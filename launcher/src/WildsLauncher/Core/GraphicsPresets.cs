using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// Applies a graphics preset to the game's own setting files: options.txt (Minecraft and Sodium), Distant Horizons
    /// and Iris (shaders). Only the keys a preset names change; everything else the player set stays.
    /// </summary>
    public static class GraphicsPresets
    {
        public static void Apply(Paths p, Preset preset)
        {
            SetOptions(p.InGame("options.txt"), preset.Options);
            var dh = preset.DistantHorizons;
            var toml = new TomlFile(p.InGame("config/DistantHorizons.toml"));
            toml.Set("client.advanced.debugging", "rendererMode", dh.Enabled ? "\"DEFAULT\"" : "\"DISABLED\"");
            toml.Set("client.advanced.autoUpdater", "enableAutoUpdater", "false");   // the pack decides the mod's version
            if (dh.Enabled)
            {
                toml.Set("client.advanced.graphics.quality", "lodChunkRenderDistanceRadius", dh.Radius.ToString());
                toml.Set("client.advanced.graphics.quality", "horizontalQuality", "\"" + dh.Quality + "\"");
                toml.Set("client.advanced.graphics.quality", "verticalQuality", "\"" + dh.Quality + "\"");
                toml.Set("common.multiThreading", "numberOfThreads", dh.Threads.ToString());
                toml.Set("common.multiThreading", "threadRunTimeRatio", "\"" + dh.Ratio + "\"");
            }
            toml.Save();
            var iris = new Dictionary<string, string> { ["enableShaders"] = string.IsNullOrEmpty(preset.Shader) ? "false" : "true" };
            if (!string.IsNullOrEmpty(preset.Shader)) iris["shaderPack"] = preset.Shader;
            SetProperties(p.InGame("config/iris.properties"), iris);
            Log.Info($"Пресет графіки «{preset.Name}» застосовано");
        }

        private static void SetOptions(string file, IDictionary<string, string> values)
        {
            var lines = File.Exists(file) ? File.ReadAllLines(file, Encoding.UTF8).ToList() : new List<string>();
            var left = new Dictionary<string, string>(values);
            for (int i = 0; i < lines.Count; i++)
            {
                int colon = lines[i].IndexOf(':');
                if (colon <= 0) continue;
                string key = lines[i].Substring(0, colon);
                if (left.TryGetValue(key, out string v))
                {
                    lines[i] = key + ":" + v;
                    left.Remove(key);
                }
            }
            lines.AddRange(left.Select(kv => kv.Key + ":" + kv.Value));
            Files.WriteAtomic(file, string.Join("\n", lines) + "\n");
        }

        private static void SetProperties(string file, IDictionary<string, string> values)
        {
            var lines = File.Exists(file) ? File.ReadAllLines(file, Encoding.UTF8).ToList() : new List<string>();
            var left = new Dictionary<string, string>(values);
            for (int i = 0; i < lines.Count; i++)
            {
                if (lines[i].TrimStart().StartsWith("#")) continue;
                int eq = lines[i].IndexOf('=');
                if (eq <= 0) continue;
                string key = lines[i].Substring(0, eq).Trim();
                if (left.TryGetValue(key, out string v))
                {
                    lines[i] = key + "=" + v;
                    left.Remove(key);
                }
            }
            lines.AddRange(left.Select(kv => kv.Key + "=" + kv.Value));
            Files.WriteAtomic(file, string.Join("\n", lines) + "\n");
        }

        /// <summary>
        /// Distant Horizons' TOML as text: values are replaced in place under their [section], so its comments and every
        /// other option stay; a missing key or section is added (the mod fills in the rest on its next start).
        /// </summary>
        private sealed class TomlFile
        {
            private readonly string file;
            private readonly List<string> lines;

            public TomlFile(string file)
            {
                this.file = file;
                lines = File.Exists(file) ? File.ReadAllLines(file, Encoding.UTF8).ToList() : new List<string>();
            }

            public void Set(string section, string key, string value)
            {
                int header = lines.FindIndex(l => l.Trim() == "[" + section + "]");
                if (header < 0)
                {
                    if (lines.Count > 0 && lines[lines.Count - 1].Trim().Length > 0) lines.Add("");
                    lines.Add("[" + section + "]");
                    lines.Add("\t" + key + " = " + value);
                    return;
                }
                string indent = lines[header].Substring(0, lines[header].Length - lines[header].TrimStart().Length) + "\t";
                for (int i = header + 1; i < lines.Count; i++)
                {
                    string t = lines[i].Trim();
                    if (t.StartsWith("[")) break;
                    if (t.StartsWith(key + " =") || t.StartsWith(key + "="))
                    {
                        string own = lines[i].Substring(0, lines[i].Length - lines[i].TrimStart().Length);
                        lines[i] = own + key + " = " + value;
                        return;
                    }
                }
                lines.Insert(header + 1, indent + key + " = " + value);
            }

            public void Save() => Files.WriteAtomic(file, string.Join("\n", lines) + "\n");
        }
    }
}
