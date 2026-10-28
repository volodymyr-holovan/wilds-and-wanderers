using System;
using System.IO;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// Where things live. Everything the game needs goes into the standard .minecraft: NeoForge and the pack's own version
    /// into versions/ and libraries/, mods and configs into the game folder, which is .minecraft itself, so the version
    /// works in any launcher that reads .minecraft (the official one, TLauncher, Legacy Launcher...), most of which cannot
    /// give a version a folder of its own. This program keeps its settings, downloads and log in LocalAppData.
    /// </summary>
    public sealed class Paths
    {
        public string MinecraftDir { get; private set; }
        public string GameDir { get; private set; }
        public string DataDir { get; private set; }
        /// <summary>where version 1.0 of this program put the game (its own folder): worlds and mods are moved over from it</summary>
        public string LegacyGameDir { get; private set; }
        /// <summary>a test root: nothing outside it is looked at (no Java of the system, no launcher of the system)</summary>
        public bool Isolated { get; private set; }

        public string CacheDir => Path.Combine(DataDir, "cache");
        public string JavaDir => Path.Combine(DataDir, "java");
        public string SettingsFile => Path.Combine(DataDir, "settings.json");
        public string StateFile => Path.Combine(GameDir, ".wilds", "state.json");
        public string RemovedDir => Path.Combine(GameDir, "_removed_by_pack");

        public static Paths Default()
        {
            string roaming = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            return new Paths
            {
                MinecraftDir = Path.Combine(roaming, ".minecraft"),
                GameDir = Path.Combine(roaming, ".minecraft"),
                DataDir = Path.Combine(local, "WildsAndWanderers"),
                LegacyGameDir = Path.Combine(roaming, ".wilds-and-wanderers"),
            };
        }

        public static Paths ForRoot(string root)
        {
            root = Path.GetFullPath(root);
            return new Paths
            {
                MinecraftDir = Path.Combine(root, ".minecraft"),
                GameDir = Path.Combine(root, ".minecraft"),
                DataDir = Path.Combine(root, "data"),
                LegacyGameDir = Path.Combine(root, "game"),
                Isolated = true,
            };
        }

        public Paths WithGameDir(string dir)
        {
            var p = (Paths)MemberwiseClone();
            p.GameDir = Path.GetFullPath(dir);
            return p;
        }

        /// <summary>a path from the manifest inside the game folder; anything that would leave it is refused</summary>
        public string InGame(string relative) => Inside(GameDir, relative);

        public static string Inside(string root, string relative)
        {
            if (string.IsNullOrWhiteSpace(relative) || Path.IsPathRooted(relative) || relative.Contains(":"))
                throw new InvalidDataException("Неприпустимий шлях у маніфесті: " + relative);
            string full = Path.GetFullPath(Path.Combine(root, relative.Replace('/', Path.DirectorySeparatorChar)));
            string baseDir = Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
            if (!full.StartsWith(baseDir, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Шлях виходить за межі теки гри: " + relative);
            return full;
        }
    }
}
