using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace Wilds.Launcher.Core
{
    /// <summary>The player's choices in this program: the graphics preset and the memory for the game.</summary>
    public sealed class Settings
    {
        public string Preset;
        /// <summary>0: chosen from the computer's memory</summary>
        public int MemoryGb;

        public static Settings Load(string file)
        {
            var s = new Settings();
            try
            {
                if (!File.Exists(file)) return s;
                var d = Json.ParseObject(File.ReadAllText(file, Encoding.UTF8));
                s.Preset = Json.Str(d, "preset");
                s.MemoryGb = (int)Json.Long(d, "memory_gb");
            }
            catch (Exception e)
            {
                Log.Warn("Налаштування не прочитано, беру стандартні: " + e.Message);
            }
            return s;
        }

        public void Save(string file)
        {
            var d = new Dictionary<string, object> { ["preset"] = Preset, ["memory_gb"] = MemoryGb };
            Files.WriteAtomic(file, Json.Write(d));
        }
    }

    /// <summary>
    /// What this program put into the game folder (path, hash, size and time of each file) and which preset it applied.
    /// The hashes are reused while a file's size and time are unchanged, so a start does not reread a gigabyte of mods.
    /// </summary>
    public sealed class InstallState
    {
        public sealed class Known
        {
            public string Sha1;
            public long Size;
            public long Time;
        }

        public string PackVersion;
        public string AppliedPreset;
        public string AppliedMemory;
        public Dictionary<string, Known> Files = new Dictionary<string, Known>(StringComparer.OrdinalIgnoreCase);

        public bool Installed => PackVersion != null;

        public static InstallState Load(string file)
        {
            var s = new InstallState();
            try
            {
                if (!File.Exists(file)) return s;
                var d = Json.ParseObject(File.ReadAllText(file, Encoding.UTF8));
                s.PackVersion = Json.Str(d, "pack_version");
                s.AppliedPreset = Json.Str(d, "applied_preset");
                s.AppliedMemory = Json.Str(d, "applied_memory");
                var files = Json.Obj(d, "files");
                if (files != null)
                {
                    foreach (var kv in files)
                    {
                        var f = Json.Obj(kv.Value);
                        if (f == null) continue;
                        s.Files[kv.Key] = new Known { Sha1 = Json.Str(f, "sha1"), Size = Json.Long(f, "size"), Time = Json.Long(f, "time") };
                    }
                }
            }
            catch (Exception e)
            {
                Log.Warn("Стан встановлення не прочитано, перевірю всі файли: " + e.Message);
            }
            return s;
        }

        public void Save(string file)
        {
            var files = new SortedDictionary<string, object>(StringComparer.OrdinalIgnoreCase);
            foreach (var kv in Files)
                files[kv.Key] = new Dictionary<string, object> { ["sha1"] = kv.Value.Sha1, ["size"] = kv.Value.Size, ["time"] = kv.Value.Time };
            var d = new Dictionary<string, object>
            {
                ["pack_version"] = PackVersion,
                ["applied_preset"] = AppliedPreset,
                ["applied_memory"] = AppliedMemory,
                ["files"] = files,
            };
            Directory.CreateDirectory(Path.GetDirectoryName(file));
            Core.Files.WriteAtomic(file, Json.Write(d));
        }

        /// <summary>the file's hash: from the record if size and time are unchanged, else read anew (null: no file)</summary>
        public string HashOf(string relative, string full, bool deep)
        {
            var info = new FileInfo(full);
            if (!info.Exists) return null;
            long time = info.LastWriteTimeUtc.Ticks;
            if (!deep && Files.TryGetValue(relative, out var k) && k.Size == info.Length && k.Time == time && k.Sha1 != null) return k.Sha1;
            string sha = Net.Sha1Of(full);
            Files[relative] = new Known { Sha1 = sha, Size = info.Length, Time = time };
            return sha;
        }

        public void Record(string relative, string full)
        {
            var info = new FileInfo(full);
            Files[relative] = new Known { Sha1 = Net.Sha1Of(full), Size = info.Length, Time = info.LastWriteTimeUtc.Ticks };
        }
    }

    public static class Files
    {
        /// <summary>write through a temporary file, so a crash never leaves half a file</summary>
        public static void WriteAtomic(string file, string text)
        {
            Directory.CreateDirectory(Path.GetDirectoryName(file));
            string tmp = file + ".tmp";
            File.WriteAllText(tmp, text, new UTF8Encoding(false));
            if (File.Exists(file)) File.Replace(tmp, file, null);
            else File.Move(tmp, file);
        }

        /// <summary>moves a file or folder the pack no longer wants into _removed_by_pack (never deletes the player's things)</summary>
        public static void MoveAside(string full, string gameDir, string removedDir)
        {
            if (!File.Exists(full) && !Directory.Exists(full)) return;
            string rel = full.Substring(gameDir.TrimEnd(Path.DirectorySeparatorChar).Length + 1);
            string target = Path.Combine(removedDir, rel);
            for (int i = 1; File.Exists(target) || Directory.Exists(target); i++) target = Path.Combine(removedDir, rel + "." + i);
            Directory.CreateDirectory(Path.GetDirectoryName(target));
            if (Directory.Exists(full)) Directory.Move(full, target);
            else File.Move(full, target);
            Log.Info("Відкладено (більше не потрібно збірці): " + rel);
        }
    }
}
