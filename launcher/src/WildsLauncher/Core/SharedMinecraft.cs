using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// The pack in the shared .minecraft, the folder every launcher reads: its own version in versions/ (so it is in any
    /// launcher's list), its mods in mods/ (mods of something else that were there are archived first, the whole folder
    /// renamed, nothing deleted), and what version 1.0 of this program put into a folder of its own moved over.
    /// </summary>
    public static class SharedMinecraft
    {
        /// <summary>the version players pick in their launcher</summary>
        public const string VersionId = "Wilds-and-Wanderers";

        public static string VersionFile(Paths p) => Path.Combine(p.MinecraftDir, "versions", VersionId, VersionId + ".json");

        /// <summary>
        /// The pack's version: NeoForge's own version file under the pack's name. It inherits Minecraft 1.21.1 the same way,
        /// so any launcher that runs NeoForge runs it.
        /// </summary>
        public static void EnsureVersion(Paths p, Manifest m)
        {
            string source = Path.Combine(p.MinecraftDir, "versions", m.VersionId, m.VersionId + ".json");
            var version = Json.ParseObject(File.ReadAllText(source));
            version["id"] = VersionId;
            string text = Json.Write(version), target = VersionFile(p);
            if (File.Exists(target) && File.ReadAllText(target) == text) return;
            Files.WriteAtomic(target, text);
            Log.Info($"Версія «{VersionId}» у {Path.GetDirectoryName(target)} (NeoForge {m.NeoForgeVersion})");
        }

        /// <summary>
        /// Mods that are not the pack's (another modpack, the player's own): the whole mods folder is renamed to
        /// mods_archived_&lt;date&gt; and the pack's own jars come back from it (no new download). Returns the archive's
        /// name, or null when there was nothing foreign.
        /// </summary>
        public static string ArchiveForeignMods(Paths p, Manifest m, InstallState st)
        {
            string mods = p.InGame("mods");
            if (!Directory.Exists(mods)) return null;
            var packJars = new HashSet<string>(m.Mods.Select(f => f.Path), StringComparer.OrdinalIgnoreCase);
            var foreign = Directory.GetFiles(mods, "*.jar").Select(Path.GetFileName)
                .Where(n => !packJars.Contains(n) && !st.Files.ContainsKey("mods/" + n)).ToList();
            if (foreign.Count == 0) return null;

            string name = "mods_archived_" + DateTime.Now.ToString("yyyy-MM-dd_HH-mm");
            for (int i = 2; Directory.Exists(p.InGame(name)); i++) name = "mods_archived_" + DateTime.Now.ToString("yyyy-MM-dd_HH-mm") + "_" + i;
            string archive = p.InGame(name);
            Directory.Move(mods, archive);
            Directory.CreateDirectory(mods);
            int back = 0;
            foreach (var f in m.Mods)
            {
                string from = Path.Combine(archive, f.Path);
                if (!File.Exists(from)) continue;
                File.Move(from, Path.Combine(mods, f.Path));   // checked by hash right after, like any file of the pack
                back++;
            }
            // what the pack's mods keep there (the browser's Chromium) stays with them
            string cef = Path.Combine(archive, "mcef-libraries");
            if (Directory.Exists(cef)) Directory.Move(cef, Path.Combine(mods, "mcef-libraries"));
            Log.Info($"Чужі моди ({foreign.Count}: {string.Join(", ", foreign.Take(5))}{(foreign.Count > 5 ? "…" : "")}) збережено в {name}; " +
                     $"моди збірки повернуто ({back})");
            return name;
        }

        /// <summary>
        /// Version 1.0 of this program kept the game in a folder of its own: its worlds and screenshots move into
        /// .minecraft (a world with the same name gets a suffix), its downloaded mods are reused, and the old folder is
        /// renamed (kept, not deleted). Returns how many worlds moved; -1 when there was nothing to move.
        /// </summary>
        public static int MigrateLegacy(Paths p, Manifest m)
        {
            string old = p.LegacyGameDir;
            if (string.IsNullOrEmpty(old) || !Directory.Exists(old)) return -1;
            if (string.Equals(Path.GetFullPath(old).TrimEnd('\\'), Path.GetFullPath(p.GameDir).TrimEnd('\\'), StringComparison.OrdinalIgnoreCase)) return -1;

            int worlds = 0;
            string oldSaves = Path.Combine(old, "saves");
            if (Directory.Exists(oldSaves))
            {
                Directory.CreateDirectory(p.InGame("saves"));
                foreach (string world in Directory.GetDirectories(oldSaves))
                {
                    string name = Path.GetFileName(world), target = Path.Combine(p.InGame("saves"), name);
                    for (int i = 2; Directory.Exists(target); i++) target = Path.Combine(p.InGame("saves"), $"{name} (Wilds & Wanderers {i})");
                    Directory.Move(world, target);
                    worlds++;
                }
            }
            MoveFiles(Path.Combine(old, "screenshots"), p.InGame("screenshots"), _ => true);
            var packJars = new HashSet<string>(m.Mods.Select(f => f.Path), StringComparer.OrdinalIgnoreCase);
            MoveFiles(Path.Combine(old, "mods"), p.InGame("mods"), n => packJars.Contains(n));
            var shaders = new HashSet<string>(m.Shaders.Select(f => f.Path), StringComparer.OrdinalIgnoreCase);
            MoveFiles(Path.Combine(old, "shaderpacks"), p.InGame("shaderpacks"), n => shaders.Contains(n));

            string aside = old + ".old";
            for (int i = 2; Directory.Exists(aside); i++) aside = old + ".old" + i;
            Directory.Move(old, aside);
            Log.Info($"Перенесено з окремої теки версії 1.0: світів {worlds}; стару теку збережено як {aside}");
            return worlds;
        }

        private static void MoveFiles(string from, string to, Func<string, bool> which)
        {
            if (!Directory.Exists(from)) return;
            Directory.CreateDirectory(to);
            foreach (string file in Directory.GetFiles(from))
            {
                string name = Path.GetFileName(file), target = Path.Combine(to, name);
                if (which(name) && !File.Exists(target)) File.Move(file, target);
            }
        }
    }
}
