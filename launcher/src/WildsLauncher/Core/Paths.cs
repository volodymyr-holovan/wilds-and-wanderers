using System;
using System.IO;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// Where things live. The official launcher keeps versions and libraries in .minecraft; the pack has a game folder of
    /// its own (mods, configs, worlds, settings), which its launcher profile points to; this program keeps its settings,
    /// downloads and log in LocalAppData.
    /// </summary>
    public sealed class Paths
    {
        public string MinecraftDir { get; private set; }
        public string GameDir { get; private set; }
        public string DataDir { get; private set; }
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
                GameDir = Path.Combine(roaming, ".wilds-and-wanderers"),
                DataDir = Path.Combine(local, "WildsAndWanderers"),
            };
        }

        public static Paths ForRoot(string root)
        {
            root = Path.GetFullPath(root);
            return new Paths
            {
                MinecraftDir = Path.Combine(root, ".minecraft"),
                GameDir = Path.Combine(root, "game"),
                DataDir = Path.Combine(root, "data"),
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
