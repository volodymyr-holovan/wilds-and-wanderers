using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace Wilds.Launcher.Core
{
    /// <summary>A file the pack installs: where it goes in the game folder, its hash, where to get it.</summary>
    public sealed class FileEntry
    {
        public string Path;
        public string Sha1;
        public string Sha256;
        public long Size;
        public string Url;

        internal static FileEntry From(object o, string pathKey = "path")
        {
            var d = Json.Obj(o) ?? throw new InvalidDataException("Пошкоджений запис файлу в маніфесті");
            return new FileEntry
            {
                Path = Json.Str(d, pathKey),
                Sha1 = Json.Str(d, "sha1")?.ToLowerInvariant(),
                Sha256 = Json.Str(d, "sha256")?.ToLowerInvariant(),
                Size = Json.Long(d, "size"),
                Url = Json.Str(d, "url"),
            };
        }
    }

    public sealed class ServerEntry
    {
        public string Name;
        public string Address;
    }

    /// <summary>launcher/config.json in the repository: what the owner edits by hand (servers, news, latest program).</summary>
    public sealed class PackConfig
    {
        public string PackName = "Wilds & Wanderers";
        public List<ServerEntry> Servers = new List<ServerEntry>();
        public List<string> News = new List<string>();
        public string UpdaterVersion;
        public string UpdaterUrl;
        public string UpdaterSha256;
        public int MinimumMemoryGb = 4;

        public static PackConfig Parse(string json)
        {
            var d = Json.ParseObject(json);
            var c = new PackConfig { PackName = Json.Str(d, "pack", "Wilds & Wanderers"), MinimumMemoryGb = (int)Json.Long(d, "minimum_memory_gb", 4) };
            foreach (var s in Json.Arr(d, "servers").Select(Json.Obj).Where(s => s != null))
            {
                string address = Json.Str(s, "address");
                if (!string.IsNullOrWhiteSpace(address)) c.Servers.Add(new ServerEntry { Name = Json.Str(s, "name", c.PackName), Address = address.Trim() });
            }
            c.News.AddRange(Json.Arr(d, "news").Select(n => n as string).Where(n => !string.IsNullOrWhiteSpace(n)));
            var u = Json.Obj(d, "updater");
            c.UpdaterVersion = Json.Str(u, "version");
            c.UpdaterUrl = Json.Str(u, "url");
            c.UpdaterSha256 = Json.Str(u, "sha256")?.ToLowerInvariant();
            return c;
        }
    }

    /// <summary>launcher/manifest.json, written by pack/tools/release_client.py for every release of the pack.</summary>
    public sealed class Manifest
    {
        public string Version;
        public string Minecraft;
        public string NeoForgeVersion;
        public FileEntry NeoForgeInstaller;
        public List<FileEntry> Mods = new List<FileEntry>();
        public FileEntry OverridesZip;
        public List<FileEntry> Overrides = new List<FileEntry>();
        public List<FileEntry> Initial = new List<FileEntry>();
        public List<FileEntry> Shaders = new List<FileEntry>();
        public List<string> Removed = new List<string>();

        public string VersionId => "neoforge-" + NeoForgeVersion;

        public static Manifest Parse(string json)
        {
            var d = Json.ParseObject(json);
            var m = new Manifest
            {
                Version = Json.Str(d, "version"),
                Minecraft = Json.Str(d, "minecraft"),
            };
            var nf = Json.Obj(d, "neoforge");
            m.NeoForgeVersion = Json.Str(nf, "version");
            m.NeoForgeInstaller = FileEntry.From(Json.Obj(nf, "installer"), "file");
            m.Mods.AddRange(Json.Arr(d, "mods").Select(o => FileEntry.From(o, "file")));
            var files = Json.Obj(d, "files");
            m.OverridesZip = FileEntry.From(Json.Obj(files, "zip"), "file");
            m.Overrides.AddRange(Json.Arr(files, "overrides").Select(o => FileEntry.From(o)));
            m.Initial.AddRange(Json.Arr(files, "initial").Select(o => FileEntry.From(o)));
            m.Shaders.AddRange(Json.Arr(d, "shaders").Select(o => FileEntry.From(o, "file")));
            m.Removed.AddRange(Json.Arr(d, "removed").Select(o => o as string).Where(s => !string.IsNullOrWhiteSpace(s)));
            m.Validate();
            return m;
        }

        private void Validate()
        {
            if (string.IsNullOrEmpty(Version) || string.IsNullOrEmpty(NeoForgeVersion) || NeoForgeInstaller?.Url == null || OverridesZip?.Url == null)
                throw new InvalidDataException("Маніфест збірки неповний");
            foreach (var f in Mods.Concat(Shaders))
            {
                if (string.IsNullOrEmpty(f.Path) || f.Path.IndexOfAny(new[] { '/', '\\', ':' }) >= 0 || string.IsNullOrEmpty(f.Url) || string.IsNullOrEmpty(f.Sha1))
                    throw new InvalidDataException("Неправильний запис у маніфесті: " + f.Path);
            }
            foreach (var f in Overrides.Concat(Initial))
            {
                if (string.IsNullOrEmpty(f.Path) || string.IsNullOrEmpty(f.Sha1)) throw new InvalidDataException("Неправильний запис у маніфесті: " + f.Path);
            }
        }
    }

    public sealed class DistantHorizonsSettings
    {
        public bool Enabled;
        public int Radius = 64;
        public string Quality = "LOW";
        public int Threads = 1;
        public string Ratio = "0.5";
    }

    public sealed class Preset
    {
        public string Id;
        public string Name;
        public string Description;
        public Dictionary<string, string> Options = new Dictionary<string, string>();
        public string Shader;
        public DistantHorizonsSettings DistantHorizons = new DistantHorizonsSettings();
    }

    /// <summary>launcher/presets.json: the four graphics presets.</summary>
    public sealed class PresetSet
    {
        public string Default = "minimal";
        public List<Preset> Presets = new List<Preset>();

        public Preset Find(string id) => Presets.FirstOrDefault(p => p.Id == id) ?? Presets.FirstOrDefault(p => p.Id == Default) ?? Presets.FirstOrDefault();

        public static PresetSet Parse(string json)
        {
            var d = Json.ParseObject(json);
            var set = new PresetSet { Default = Json.Str(d, "default", "minimal") };
            foreach (var p in Json.Arr(d, "presets").Select(Json.Obj).Where(p => p != null))
            {
                var preset = new Preset
                {
                    Id = Json.Str(p, "id"),
                    Name = Json.Str(p, "name"),
                    Description = Json.Str(p, "description", ""),
                    Shader = Json.Str(p, "shader"),
                };
                var options = Json.Obj(p, "options");
                if (options != null)
                    foreach (var kv in options) preset.Options[kv.Key] = Convert.ToString(kv.Value, System.Globalization.CultureInfo.InvariantCulture);
                var dh = Json.Obj(p, "distant_horizons");
                if (dh != null)
                {
                    preset.DistantHorizons = new DistantHorizonsSettings
                    {
                        Enabled = Json.Bool(dh, "enabled"),
                        Radius = (int)Json.Long(dh, "radius", 64),
                        Quality = Json.Str(dh, "quality", "LOW"),
                        Threads = (int)Json.Long(dh, "threads", 1),
                        Ratio = Json.Str(dh, "ratio", "0.5"),
                    };
                }
                if (!string.IsNullOrEmpty(preset.Id)) set.Presets.Add(preset);
            }
            if (set.Presets.Count == 0) throw new InvalidDataException("Немає пресетів графіки");
            return set;
        }
    }
}
