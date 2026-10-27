using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using Microsoft.Win32;

namespace Wilds.Launcher.Core
{
    /// <summary>Starts the official Minecraft launcher (the player signs in and plays there, with the pack's installation).</summary>
    public static class GameStarter
    {
        private const string StorePackage = "Microsoft.4297127D64EC6_8wekyb3d8bbwe";

        /// <summary>true when a launcher was started; false: none is installed</summary>
        public static bool StartOfficialLauncher()
        {
            string exe = FindLegacyLauncher();
            if (exe != null)
            {
                Process.Start(new ProcessStartInfo(exe) { UseShellExecute = true, WorkingDirectory = Path.GetDirectoryName(exe) });
                Log.Info("Запущено лаунчер Minecraft: " + exe);
                return true;
            }
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            if (Directory.Exists(Path.Combine(local, "Packages", StorePackage)))
            {
                // the launcher from the Microsoft Store / Xbox app
                Process.Start(new ProcessStartInfo("explorer.exe", $"shell:AppsFolder\\{StorePackage}!Minecraft") { UseShellExecute = true });
                Log.Info("Запущено лаунчер Minecraft (Microsoft Store)");
                return true;
            }
            Log.Warn("Лаунчер Minecraft не знайдено");
            return false;
        }

        public static bool LauncherInstalled()
        {
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            return FindLegacyLauncher() != null || Directory.Exists(Path.Combine(local, "Packages", StorePackage));
        }

        private static string FindLegacyLauncher()
        {
            foreach (string key in new[] { @"SOFTWARE\WOW6432Node\Mojang\InstalledProducts\Minecraft Launcher", @"SOFTWARE\Mojang\InstalledProducts\Minecraft Launcher" })
            {
                try
                {
                    using (var k = Registry.LocalMachine.OpenSubKey(key))
                    {
                        if (k?.GetValue("InstallLocation") is string dir)
                        {
                            string exe = Path.Combine(dir, "MinecraftLauncher.exe");
                            if (File.Exists(exe)) return exe;
                        }
                    }
                }
                catch (Exception e) when (e is System.Security.SecurityException || e is UnauthorizedAccessException)
                {
                }
            }
            foreach (var folder in new[] { Environment.SpecialFolder.ProgramFilesX86, Environment.SpecialFolder.ProgramFiles })
            {
                string exe = Path.Combine(Environment.GetFolderPath(folder), "Minecraft Launcher", "MinecraftLauncher.exe");
                if (File.Exists(exe)) return exe;
            }
            return null;
        }

        public static void OpenDownloadPage() =>
            Process.Start(new ProcessStartInfo("https://www.minecraft.net/download") { UseShellExecute = true });

        public static void OpenFolder(string dir)
        {
            Directory.CreateDirectory(dir);
            Process.Start(new ProcessStartInfo("explorer.exe", "\"" + dir + "\"") { UseShellExecute = true });
        }

        // ---------------------------------------------------------------- memory

        [StructLayout(LayoutKind.Sequential)]
        private struct MemoryStatusEx
        {
            public uint Length;
            public uint MemoryLoad;
            public ulong TotalPhys;
            public ulong AvailPhys;
            public ulong TotalPageFile;
            public ulong AvailPageFile;
            public ulong TotalVirtual;
            public ulong AvailVirtual;
            public ulong AvailExtendedVirtual;
        }

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool GlobalMemoryStatusEx(ref MemoryStatusEx status);

        public static int TotalMemoryGb()
        {
            var s = new MemoryStatusEx { Length = (uint)Marshal.SizeOf(typeof(MemoryStatusEx)) };
            return GlobalMemoryStatusEx(ref s) ? (int)Math.Round(s.TotalPhys / 1073741824.0) : 8;
        }

        /// <summary>memory for the game by the computer's memory: enough for ~170 mods, room left for Windows</summary>
        public static int RecommendedMemoryGb(int total)
        {
            if (total >= 32) return 8;
            if (total >= 16) return 6;
            if (total >= 12) return 5;
            if (total >= 8) return 4;
            return Math.Max(3, total - 3);
        }
    }
}
