using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace Wilds.Launcher.Core
{
    /// <summary>What removing the pack takes away, and what it gives back.</summary>
    public sealed class UninstallPlan
    {
        /// <summary>files and folders to remove (full paths)</summary>
        public readonly List<string> Remove = new List<string>();
        /// <summary>the mods folder that was there before the pack (archived at install): it becomes mods again</summary>
        public string RestoreArchive;
        public long Bytes;
        public bool RemovesNeoForge;
        /// <summary>what was in the folder before the pack (empty folders among these stay)</summary>
        public List<string> Preexisting = new List<string>();

        public override string ToString() => $"{Remove.Count} файлів і тек, {PackSync.Mb(Bytes)}" +
                                             (RestoreArchive != null ? ", повернути " + Path.GetFileName(RestoreArchive) : "");
    }

    /// <summary>
    /// Removes the pack and what its mods made, never the player's own things. The pack's: the files it installed (its
    /// record), the configs of its mods (named after the mod ids read from its jars), the folders its mods create
    /// (KubeJS, Distant Horizons' caches, the map's data...), its version, NeoForge when the program installed it, its
    /// profile in the official launcher, its server in the list, the program's downloads. The player's: worlds,
    /// screenshots, schematics and blueprints (removed only when empty), game settings, resource packs, other mods,
    /// and anything that was in the folder before the pack came (a snapshot taken at the first install).
    /// </summary>
    public static class Uninstaller
    {
        /// <summary>folders and files the pack's mods create next to the game's own</summary>
        private static readonly string[] PackLeftovers =
        {
            "kubejs", "defaultconfigs", "moddata", "xaero", "local", "data", "browser", "Distant_Horizons_server_data",
            "patchouli_books", "patchouli_data.json", "ponders_watched.json", "command_history.txt", "easy_npc",
            "_removed_by_pack", ".wilds", ".cache", ".mixin.out", ".monolith", ".sable",
        };

        /// <summary>the player's creations: removed only when nothing is in them</summary>
        private static readonly string[] KeepWhenUsed = { "blueprints", "schematics" };

        /// <summary>the top of the game folder and of its config/, before the pack's first install</summary>
        public static List<string> Snapshot(Paths p)
        {
            var names = new List<string>();
            if (!Directory.Exists(p.GameDir)) return names;
            names.AddRange(Directory.EnumerateFileSystemEntries(p.GameDir).Select(Path.GetFileName));
            string config = Path.Combine(p.GameDir, "config");
            if (Directory.Exists(config)) names.AddRange(Directory.EnumerateFileSystemEntries(config).Select(e => "config/" + Path.GetFileName(e)));
            return names;
        }

        public static UninstallPlan Plan(Paths p, Manifest m, InstallState st)
        {
            var plan = new UninstallPlan { RemovesNeoForge = st.NeoForgeByPack, Preexisting = st.Preexisting ?? new List<string>() };
            var pre = new HashSet<string>(st.Preexisting ?? new List<string>(), StringComparer.OrdinalIgnoreCase);
            var remove = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            void Add(string full)
            {
                if (File.Exists(full) || Directory.Exists(full)) remove.Add(full);
            }

            // what the pack installed
            foreach (string rel in st.Files.Keys) Add(p.InGame(rel));
            foreach (var f in m.Mods) Add(p.InGame("mods/" + f.Path));
            foreach (var f in m.Shaders) Add(p.InGame("shaderpacks/" + f.Path));
            // whatever else is in mods/ came with the pack too: foreign mods were archived when it came (the browser's
            // Chromium and cache, other mods' folders)
            string modsDir = p.InGame("mods");
            if (Directory.Exists(modsDir))
            {
                foreach (string entry in Directory.EnumerateFileSystemEntries(modsDir))
                {
                    string name = Path.GetFileName(entry);
                    if (st.Preexisting != null || name.StartsWith("mcef", StringComparison.OrdinalIgnoreCase)) Add(entry);
                }
            }

            // the configs of the pack's mods
            var ids = ModIds(p, m);
            if (st.NeoForgeByPack)
            {
                ids.Add("neoforge");
                ids.Add("fml");
            }
            string config = p.InGame("config");
            if (Directory.Exists(config))
            {
                foreach (string entry in Directory.EnumerateFileSystemEntries(config))
                {
                    string name = Path.GetFileName(entry);
                    if (pre.Contains("config/" + name)) continue;
                    // with a snapshot of the folder before the pack, whatever appeared in config/ since is the pack's mods'
                    // (other mods cannot run there: the pack archives them); without one, the names of the pack's mods decide
                    if (st.Preexisting != null || OfMod(name, ids)) Add(entry);
                }
            }

            // what the pack's mods create beside the game's folders
            foreach (string entry in Directory.Exists(p.GameDir) ? Directory.EnumerateFileSystemEntries(p.GameDir) : Enumerable.Empty<string>())
            {
                string name = Path.GetFileName(entry);
                if (pre.Contains(name)) continue;
                if (PackLeftovers.Contains(name, StringComparer.OrdinalIgnoreCase) || name.StartsWith("XaeroWaypoints_BACKUP", StringComparison.OrdinalIgnoreCase))
                    Add(entry);
                else if (KeepWhenUsed.Contains(name, StringComparer.OrdinalIgnoreCase) && Directory.Exists(entry)
                         && !Directory.EnumerateFiles(entry, "*", SearchOption.AllDirectories).Any())
                    Add(entry);
            }

            // the pack's version, and NeoForge when the program brought it
            Add(Path.GetDirectoryName(SharedMinecraft.VersionFile(p)));
            if (st.NeoForgeByPack)
            {
                Add(Path.Combine(p.MinecraftDir, "versions", m.VersionId));
                Add(Path.Combine(p.MinecraftDir, "libraries", "net", "neoforged", "neoforge", m.NeoForgeVersion));
            }

            // the player's earlier mods come back: the first archive is how the folder was before the pack
            var archives = Directory.Exists(p.GameDir)
                ? Directory.GetDirectories(p.GameDir, "mods_archived_*").OrderBy(d => d, StringComparer.Ordinal).ToList()
                : new List<string>();
            plan.RestoreArchive = archives.FirstOrDefault();

            // nothing outside the folders the pack lives in, ever
            foreach (string full in remove)
            {
                if (!Within(full, p.GameDir) && !Within(full, p.MinecraftDir)) continue;
                plan.Remove.Add(full);
                plan.Bytes += SizeOf(full);
            }
            plan.Remove.Sort(StringComparer.OrdinalIgnoreCase);
            return plan;
        }

        public static void Apply(UninstallPlan plan, Paths p, PackConfig c)
        {
            int failed = 0;
            foreach (string full in plan.Remove)
            {
                try
                {
                    if (Directory.Exists(full)) DeleteTree(full);
                    else if (File.Exists(full))
                    {
                        File.SetAttributes(full, FileAttributes.Normal);
                        File.Delete(full);
                    }
                }
                catch (Exception e) when (e is IOException || e is UnauthorizedAccessException)
                {
                    failed++;
                    Log.Warn("Не видалено: " + full + " — " + e.Message);
                }
            }
            try
            {
                PruneEmpty(p.InGame("config"), p.GameDir, new HashSet<string>(plan.Preexisting, StringComparer.OrdinalIgnoreCase));
            }
            catch (Exception e) when (e is IOException || e is UnauthorizedAccessException)
            {
                Log.Warn("Порожні теки config не прибрано: " + e.Message);
            }
            foreach (string dir in new[] { p.InGame("mods"), p.InGame("shaderpacks") })
            {
                if (Directory.Exists(dir) && !Directory.EnumerateFileSystemEntries(dir).Any()) Directory.Delete(dir);
            }
            string mods = p.InGame("mods");
            if (plan.RestoreArchive != null && Directory.Exists(plan.RestoreArchive))
            {
                if (!Directory.Exists(mods))
                {
                    Directory.Move(plan.RestoreArchive, mods);
                    Log.Info("Попередні моди повернуто з " + Path.GetFileName(plan.RestoreArchive));
                }
                else
                {
                    Log.Warn("У теці mods лишилися чужі файли, тож " + Path.GetFileName(plan.RestoreArchive) + " не повернуто");
                }
            }
            LauncherProfiles.Remove(p.MinecraftDir);
            ServersDat.Remove(p.InGame("servers.dat"), c.Servers);
            foreach (string dir in new[] { p.CacheDir, p.JavaDir, Path.Combine(p.DataDir, "neoforge-installer") })
            {
                try
                {
                    if (Directory.Exists(dir)) DeleteTree(dir);
                }
                catch (Exception e) when (e is IOException || e is UnauthorizedAccessException)
                {
                    Log.Warn("Не видалено: " + dir + " — " + e.Message);
                }
            }
            Log.Info($"Збірку видалено: {plan}" + (failed > 0 ? $", не вдалося видалити: {failed}" : ""));
            if (failed > 0) throw new IOException($"Не вдалося видалити {failed} файлів (можливо, їх відкрито іншою програмою). Подробиці — у журналі.");
        }

        /// <summary>
        /// the mod ids of the pack's jars (and of the mods inside them), from their neoforge.mods.toml: a mod names its
        /// config files after its id ("jei", "sodium-options.json", "Mekanism/")
        /// </summary>
        private static HashSet<string> ModIds(Paths p, Manifest m)
        {
            var ids = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (var f in m.Mods)
            {
                string jar = p.InGame("mods/" + f.Path);
                if (!File.Exists(jar)) continue;
                try
                {
                    using (var zip = ZipFile.OpenRead(jar))
                        ReadIds(zip, ids, 0);
                }
                catch (Exception e) when (e is IOException || e is InvalidDataException)
                {
                }
            }
            ids.RemoveWhere(id => id.Length < 3 || id == "minecraft" || id == "java");
            return ids;
        }

        private static readonly Regex ModId = new Regex("modId\\s*=\\s*\"([A-Za-z0-9_.-]+)\"");

        private static void ReadIds(ZipArchive zip, HashSet<string> ids, int depth)
        {
            foreach (var e in zip.Entries)
            {
                if (e.FullName == "META-INF/neoforge.mods.toml" || e.FullName == "META-INF/mods.toml")
                {
                    using (var r = new StreamReader(e.Open(), Encoding.UTF8))
                        foreach (Match match in ModId.Matches(r.ReadToEnd())) ids.Add(match.Groups[1].Value);
                }
                else if (depth == 0 && e.FullName.StartsWith("META-INF/jarjar/") && e.FullName.EndsWith(".jar"))
                {
                    using (var s = e.Open())
                    using (var ms = new MemoryStream())
                    {
                        s.CopyTo(ms);
                        ms.Position = 0;
                        try
                        {
                            using (var inner = new ZipArchive(ms, ZipArchiveMode.Read))
                                ReadIds(inner, ids, 1);
                        }
                        catch (InvalidDataException)
                        {
                        }
                    }
                }
            }
        }

        /// <summary>
        /// a config named after one of the mods: "jei", "sodium-options.json", "better-advanced-tooltips-client.toml"
        /// (id betteradvancedtooltips), "xaerohud.txt" (ids xaerominimap, xaerolib...): compared without separators, in
        /// both directions for names of 5+ letters
        /// </summary>
        private static bool OfMod(string name, HashSet<string> ids)
        {
            string stem = Letters(Path.GetFileNameWithoutExtension(name));
            if (stem.Length < 3) return false;
            foreach (string id in ids)
            {
                string i = Letters(id);
                if (stem.StartsWith(i) || (stem.Length >= 5 && i.StartsWith(stem))) return true;
            }
            return false;
        }

        private static string Letters(string s) => new string(s.ToLowerInvariant().Where(char.IsLetterOrDigit).ToArray());

        /// <summary>folders left empty in config/ after the pack's files went (never one that was there before)</summary>
        private static void PruneEmpty(string dir, string gameDir, HashSet<string> pre)
        {
            if (!Directory.Exists(dir)) return;
            foreach (string sub in Directory.GetDirectories(dir)) PruneEmpty(sub, gameDir, pre);
            string rel = dir.Substring(gameDir.TrimEnd(Path.DirectorySeparatorChar).Length + 1).Replace(Path.DirectorySeparatorChar, '/');
            if (!pre.Contains(rel) && !Directory.EnumerateFileSystemEntries(dir).Any()) Directory.Delete(dir);
        }

        private static bool Within(string full, string root)
        {
            string r = Path.GetFullPath(root).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
            return Path.GetFullPath(full).StartsWith(r, StringComparison.OrdinalIgnoreCase);
        }

        private static long SizeOf(string full)
        {
            try
            {
                if (File.Exists(full)) return new FileInfo(full).Length;
                return Directory.EnumerateFiles(full, "*", SearchOption.AllDirectories).Sum(f => new FileInfo(f).Length);
            }
            catch (Exception e) when (e is IOException || e is UnauthorizedAccessException)
            {
                return 0;
            }
        }

        /// <summary>a folder and all in it; read-only files too (a junction is unlinked, never followed)</summary>
        private static void DeleteTree(string dir)
        {
            var info = new DirectoryInfo(dir);
            if ((info.Attributes & FileAttributes.ReparsePoint) != 0)
            {
                info.Delete();
                return;
            }
            foreach (var f in info.EnumerateFiles())
            {
                f.Attributes = FileAttributes.Normal;
                f.Delete();
            }
            foreach (var d in info.EnumerateDirectories()) DeleteTree(d.FullName);
            info.Attributes = FileAttributes.Directory;
            info.Delete();
        }
    }
}
