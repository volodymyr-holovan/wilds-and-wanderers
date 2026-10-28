using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// The pack's installation in the official launcher: the pack's version and the memory for the game (other launchers
    /// find the version in .minecraft/versions by themselves).
    /// Everything else in the file (the player's other installations, their settings) is left as it is.
    /// </summary>
    public static class LauncherProfiles
    {
        public const string Key = "wilds-and-wanderers";
        private static readonly string[] FileNames = { "launcher_profiles.json", "launcher_profiles_microsoft_store.json" };

        public static void EnsureFileExists(string minecraftDir)
        {
            foreach (string name in FileNames)
            {
                if (File.Exists(Path.Combine(minecraftDir, name))) return;
            }
            var d = new Dictionary<string, object> { ["profiles"] = new Dictionary<string, object>(), ["settings"] = new Dictionary<string, object>(), ["version"] = 3 };
            Files.WriteAtomic(Path.Combine(minecraftDir, FileNames[0]), Json.Write(d));
        }

        public static void Ensure(Paths p, Manifest m, string packName, int memoryGb, byte[] iconPng)
        {
            EnsureFileExists(p.MinecraftDir);
            foreach (string name in FileNames)
            {
                string file = Path.Combine(p.MinecraftDir, name);
                if (!File.Exists(file)) continue;
                Dictionary<string, object> root;
                try
                {
                    root = Json.ParseObject(File.ReadAllText(file, Encoding.UTF8));
                }
                catch (Exception e)
                {
                    Log.Warn($"{name} не прочитано ({e.Message}); профіль у ньому не оновлено");
                    continue;
                }
                if (!(Json.Obj(root, "profiles") is Dictionary<string, object> profiles))
                {
                    profiles = new Dictionary<string, object>();
                    root["profiles"] = profiles;
                }
                var profile = Json.Obj(profiles, Key) ?? new Dictionary<string, object>();
                string now = DateTime.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'");
                if (!profile.ContainsKey("created")) profile["created"] = now;
                profile["name"] = packName;
                profile["type"] = "custom";
                profile["lastVersionId"] = SharedMinecraft.VersionId;
                // the shared .minecraft, like every other launcher (version 1.0 of this program gave it a folder of its own)
                profile.Remove("gameDir");
                profile["javaArgs"] = JvmArgs(memoryGb);
                // most recently used: the launcher offers it first
                profile["lastUsed"] = now;
                if (iconPng != null) profile["icon"] = "data:image/png;base64," + Convert.ToBase64String(iconPng);
                profiles[Key] = profile;
                Files.WriteAtomic(file, Json.Write(root));
                Log.Info($"Профіль «{packName}» у {name}: {SharedMinecraft.VersionId}, {memoryGb} ГБ");
            }
        }

        /// <summary>G1 tuned for a big modpack: short pauses, little stutter while chunks and mods allocate</summary>
        public static string JvmArgs(int memoryGb) =>
            $"-Xmx{memoryGb}G -Xms{Math.Max(2, memoryGb / 2)}G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 " +
            "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 " +
            "-XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 " +
            "-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem " +
            "-XX:MaxTenuringThreshold=1 -Dfile.encoding=UTF-8";
    }
}
