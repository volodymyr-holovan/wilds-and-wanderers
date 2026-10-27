using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// NeoForge for the official launcher: its own installer, run headless into .minecraft. Minecraft itself (the game
    /// files) comes from Mojang's servers through that installer and the launcher; this program never carries it.
    /// </summary>
    public static class NeoForge
    {
        public static bool IsInstalled(Paths p, Manifest m)
        {
            string v = m.NeoForgeVersion;
            return File.Exists(Path.Combine(p.MinecraftDir, "versions", m.VersionId, m.VersionId + ".json"))
                   && File.Exists(Path.Combine(p.MinecraftDir, "libraries", "net", "neoforged", "neoforge", v, $"neoforge-{v}-client.jar"));
        }

        public static async Task Install(Paths p, Manifest m, Net net, IProgress<Status> progress, CancellationToken ct)
        {
            Directory.CreateDirectory(p.MinecraftDir);
            LauncherProfiles.EnsureFileExists(p.MinecraftDir);   // the installer refuses a folder without one
            string java = await Java.Find(p, net, progress, ct).ConfigureAwait(false);

            progress?.Report(new Status("Завантаження NeoForge " + m.NeoForgeVersion + "…"));
            string installer = Path.Combine(p.CacheDir, m.NeoForgeInstaller.Path ?? $"neoforge-{m.NeoForgeVersion}-installer.jar");
            if (!File.Exists(installer) || Net.Sha1Of(installer) != m.NeoForgeInstaller.Sha1)
                await net.Download(m.NeoForgeInstaller.Url, installer, m.NeoForgeInstaller.Sha1, null, null, ct).ConfigureAwait(false);

            progress?.Report(new Status("Встановлення NeoForge і Minecraft " + m.Minecraft + " (кілька хвилин)…"));
            string work = Path.Combine(p.DataDir, "neoforge-installer");
            Directory.CreateDirectory(work);
            var psi = new ProcessStartInfo(java, $"-jar \"{installer}\" --install-client \"{p.MinecraftDir}\"")
            {
                WorkingDirectory = work,
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                StandardOutputEncoding = Encoding.UTF8,
                StandardErrorEncoding = Encoding.UTF8,
            };
            var output = new StringBuilder();
            using (var proc = new Process { StartInfo = psi })
            {
                proc.OutputDataReceived += (s, e) =>
                {
                    if (e.Data == null) return;
                    lock (output) output.AppendLine(e.Data);
                    string line = e.Data.Trim();
                    if (line.StartsWith("Downloading", StringComparison.OrdinalIgnoreCase) || line.StartsWith("Processor", StringComparison.OrdinalIgnoreCase))
                        progress?.Report(new Status("NeoForge: " + Shorten(line, 90)));
                };
                proc.ErrorDataReceived += (s, e) =>
                {
                    if (e.Data != null)
                        lock (output) output.AppendLine(e.Data);
                };
                proc.Start();
                proc.BeginOutputReadLine();
                proc.BeginErrorReadLine();
                using (ct.Register(() =>
                {
                    try
                    {
                        if (!proc.HasExited) proc.Kill();
                    }
                    catch (InvalidOperationException)
                    {
                    }
                }))
                {
                    await Task.Run(() => proc.WaitForExit(), ct).ConfigureAwait(false);
                    proc.WaitForExit();
                }
                File.WriteAllText(Path.Combine(work, "last-install.log"), output.ToString(), Encoding.UTF8);
                if (proc.ExitCode != 0 || !IsInstalled(p, m))
                {
                    Log.Error("NeoForge installer:\n" + Tail(output.ToString(), 40));
                    throw new InvalidOperationException("Не вдалося встановити NeoForge (код " + proc.ExitCode + "). Подробиці — у журналі.");
                }
            }
            Log.Info("NeoForge " + m.NeoForgeVersion + " встановлено в " + p.MinecraftDir);
        }

        private static string Shorten(string s, int n) => s.Length <= n ? s : s.Substring(0, n - 1) + "…";

        private static string Tail(string s, int lines) => string.Join("\n", s.Split('\n').Reverse().Take(lines).Reverse());
    }

    /// <summary>Java 21 for the NeoForge installer: the one the official launcher already has, else Eclipse Temurin.</summary>
    public static class Java
    {
        public static async Task<string> Find(Paths p, Net net, IProgress<Status> progress, CancellationToken ct)
        {
            foreach (string candidate in Candidates(p))
            {
                if (File.Exists(candidate) && MajorVersion(candidate) >= 21)
                {
                    Log.Info("Java: " + candidate);
                    return candidate;
                }
            }
            return await DownloadTemurin(p, net, progress, ct).ConfigureAwait(false);
        }

        private static IEnumerable<string> Candidates(Paths p)
        {
            const string delta = @"runtime\java-runtime-delta\windows-x64\java-runtime-delta\bin\java.exe";
            yield return Path.Combine(p.MinecraftDir, delta);
            if (Directory.Exists(p.JavaDir))
            {
                foreach (string dir in Directory.GetDirectories(p.JavaDir)) yield return Path.Combine(dir, "bin", "java.exe");
            }
            if (p.Isolated) yield break;
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            yield return Path.Combine(local, @"Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local", delta);
            string home = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(home)) yield return Path.Combine(home, "bin", "java.exe");
            foreach (string dir in (Environment.GetEnvironmentVariable("PATH") ?? "").Split(';'))
            {
                if (dir.Trim().Length > 0) yield return Path.Combine(dir.Trim().Trim('"'), "java.exe");
            }
        }

        private static int MajorVersion(string java)
        {
            try
            {
                var psi = new ProcessStartInfo(java, "-version")
                {
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardError = true,
                    RedirectStandardOutput = true,
                };
                using (var proc = Process.Start(psi))
                {
                    string text = proc.StandardError.ReadToEnd() + proc.StandardOutput.ReadToEnd();
                    proc.WaitForExit(10000);
                    var match = Regex.Match(text, "version \"(\\d+)(?:\\.(\\d+))?");
                    if (!match.Success) return 0;
                    int major = int.Parse(match.Groups[1].Value);
                    return major == 1 && match.Groups[2].Success ? int.Parse(match.Groups[2].Value) : major;
                }
            }
            catch (Exception e) when (e is System.ComponentModel.Win32Exception || e is InvalidOperationException || e is IOException)
            {
                return 0;
            }
        }

        private static async Task<string> DownloadTemurin(Paths p, Net net, IProgress<Status> progress, CancellationToken ct)
        {
            progress?.Report(new Status("Завантаження Java 21 (Eclipse Temurin)…"));
            string api = "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse";
            var list = Json.Parse(await net.GetString(api, ct).ConfigureAwait(false)) as object[];
            var pkg = list?.Select(Json.Obj).Select(a => Json.Obj(Json.Obj(a, "binary"), "package"))
                          .FirstOrDefault(x => x != null && (Json.Str(x, "name") ?? "").EndsWith(".zip", StringComparison.OrdinalIgnoreCase))
                      ?? throw new InvalidOperationException("Не знайдено Java 21 для завантаження");
            string zip = Path.Combine(p.CacheDir, Json.Str(pkg, "name"));
            long size = Json.Long(pkg, "size");
            await net.Download(Json.Str(pkg, "link"), zip, null, Json.Str(pkg, "checksum"),
                n => progress?.Report(new Status("Завантаження Java 21…", -1)), ct).ConfigureAwait(false);
            progress?.Report(new Status("Розпакування Java 21…"));
            string target = Path.Combine(p.JavaDir, Path.GetFileNameWithoutExtension(zip));
            if (Directory.Exists(target)) Directory.Delete(target, true);
            Directory.CreateDirectory(target);
            using (var archive = ZipFile.OpenRead(zip))
            {
                // the archive has one top folder (jdk-21...-jre): its content goes straight into target
                foreach (var e in archive.Entries)
                {
                    int slash = e.FullName.IndexOf('/');
                    if (slash < 0 || slash == e.FullName.Length - 1 || e.FullName.EndsWith("/")) continue;
                    string dest = Paths.Inside(target, e.FullName.Substring(slash + 1));
                    Directory.CreateDirectory(Path.GetDirectoryName(dest));
                    e.ExtractToFile(dest, true);
                }
            }
            File.Delete(zip);
            string java = Path.Combine(target, "bin", "java.exe");
            if (!File.Exists(java)) throw new InvalidOperationException("Java розпаковано неправильно");
            Log.Info($"Java 21 завантажено ({PackSync.Mb(size)}): {java}");
            return java;
        }
    }
}
