using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace Wilds.Launcher.Core
{
    public sealed class Status
    {
        public string Text;
        /// <summary>0..1, or below 0 when the length of the step is unknown</summary>
        public double Fraction = -1;

        public Status(string text, double fraction = -1)
        {
            Text = text;
            Fraction = fraction;
        }
    }

    /// <summary>What an update has to do: the files to fetch and the files to move aside.</summary>
    public sealed class SyncPlan
    {
        public readonly List<FileEntry> Mods = new List<FileEntry>();
        public readonly List<FileEntry> Shaders = new List<FileEntry>();
        public readonly List<FileEntry> Overrides = new List<FileEntry>();
        public readonly List<FileEntry> Initial = new List<FileEntry>();
        public readonly List<string> MoveAside = new List<string>();

        public bool Empty => Mods.Count + Shaders.Count + Overrides.Count + Initial.Count + MoveAside.Count == 0;

        public long DownloadBytes => Mods.Concat(Shaders).Sum(f => f.Size);

        public override string ToString() =>
            $"{Mods.Count} модів, {Shaders.Count} шейдерів, {Overrides.Count + Initial.Count} файлів налаштувань, {MoveAside.Count} відкласти";
    }

    /// <summary>Brings the game folder to what the manifest says: mods, configs, shader packs. Nothing else is touched.</summary>
    public static class PackSync
    {
        private const string Mods = "mods/";
        private const string ShaderPacks = "shaderpacks/";

        public static SyncPlan Plan(Manifest m, Paths p, InstallState st, bool deep)
        {
            var plan = new SyncPlan();
            var wanted = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (var f in m.Mods)
            {
                string rel = Mods + f.Path;
                wanted.Add(rel);
                if (st.HashOf(rel, p.InGame(rel), deep) != f.Sha1) plan.Mods.Add(f);
            }
            foreach (var f in m.Shaders)
            {
                string rel = ShaderPacks + f.Path;
                wanted.Add(rel);
                if (st.HashOf(rel, p.InGame(rel), deep) != f.Sha1) plan.Shaders.Add(f);
            }
            foreach (var f in m.Overrides)
            {
                wanted.Add(f.Path);
                if (st.HashOf(f.Path, p.InGame(f.Path), deep) != f.Sha1) plan.Overrides.Add(f);
            }
            // the player's own settings: placed once, then theirs
            foreach (var f in m.Initial)
            {
                if (!File.Exists(p.InGame(f.Path))) plan.Initial.Add(f);
            }

            var aside = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (string rel in m.Removed)
            {
                string full = p.InGame(rel);
                if (File.Exists(full) || Directory.Exists(full)) aside.Add(full);
            }
            // what an earlier version of the pack installed and this one no longer has
            foreach (string rel in st.Files.Keys.ToList())
            {
                if (wanted.Contains(rel)) continue;
                string full = p.InGame(rel);
                if (File.Exists(full)) aside.Add(full);
                else st.Files.Remove(rel);
            }
            // mods the pack does not have (an old copy, another version of one of its mods): the server would refuse them
            string modsDir = p.InGame("mods");
            if (Directory.Exists(modsDir))
            {
                foreach (string jar in Directory.GetFiles(modsDir, "*.jar"))
                {
                    if (!wanted.Contains(Mods + Path.GetFileName(jar))) aside.Add(jar);
                }
            }
            plan.MoveAside.AddRange(aside.OrderBy(s => s, StringComparer.OrdinalIgnoreCase));
            return plan;
        }

        public static async Task Apply(SyncPlan plan, Manifest m, Paths p, InstallState st, Net net, IProgress<Status> progress, CancellationToken ct)
        {
            foreach (string full in plan.MoveAside)
            {
                Files.MoveAside(full, p.GameDir, p.RemovedDir);
                string rel = full.Substring(p.GameDir.TrimEnd(Path.DirectorySeparatorChar).Length + 1).Replace('\\', '/');
                st.Files.Remove(rel);
            }

            var downloads = plan.Mods.Select(f => (f, rel: Mods + f.Path)).Concat(plan.Shaders.Select(f => (f, rel: ShaderPacks + f.Path))).ToList();
            if (downloads.Count > 0)
            {
                long total = Math.Max(1, downloads.Sum(d => d.f.Size)), done = 0;
                int files = 0;
                var gate = new SemaphoreSlim(4);
                var lastReport = DateTime.MinValue;
                void Report()
                {
                    if ((DateTime.UtcNow - lastReport).TotalMilliseconds < 150) return;
                    lastReport = DateTime.UtcNow;
                    long d = Interlocked.Read(ref done);
                    progress?.Report(new Status($"Завантаження модів: {Volatile.Read(ref files)} з {downloads.Count} · {Mb(d)} з {Mb(total)}",
                        Math.Min(1.0, (double)d / total)));
                }
                var tasks = downloads.Select(async x =>
                {
                    await gate.WaitAsync(ct).ConfigureAwait(false);
                    try
                    {
                        string full = p.InGame(x.rel);
                        await net.Download(x.f.Url, full, x.f.Sha1, null, n =>
                        {
                            Interlocked.Add(ref done, n);
                            Report();
                        }, ct).ConfigureAwait(false);
                        lock (st) st.Record(x.rel, full);
                        Interlocked.Increment(ref files);
                        Report();
                    }
                    finally
                    {
                        gate.Release();
                    }
                }).ToList();
                await Task.WhenAll(tasks).ConfigureAwait(false);
                Log.Info($"Завантажено {downloads.Count} файлів, {Mb(total)}");
            }

            if (plan.Overrides.Count + plan.Initial.Count > 0)
            {
                progress?.Report(new Status("Завантаження налаштувань збірки…"));
                string zip = Path.Combine(p.CacheDir, "overrides-" + m.Version + ".zip");
                if (!File.Exists(zip) || Net.Sha1Of(zip) != m.OverridesZip.Sha1)
                    await net.Download(m.OverridesZip.Url, zip, m.OverridesZip.Sha1, null, null, ct).ConfigureAwait(false);
                progress?.Report(new Status("Розкладання налаштувань…"));
                using (var archive = ZipFile.OpenRead(zip))
                {
                    foreach (var f in plan.Overrides)
                    {
                        if (plan.Overrides.Count <= 20) Log.Info("Налаштування збірки: " + f.Path);
                        Extract(archive, "overrides/" + f.Path, f, p, st, true);
                    }
                    foreach (var f in plan.Initial) Extract(archive, "initial/" + f.Path, f, p, st, false);
                }
                Log.Info($"Налаштувань оновлено: {plan.Overrides.Count}, нових особистих: {plan.Initial.Count}");
                // older archives are no longer needed
                foreach (string old in Directory.GetFiles(p.CacheDir, "overrides-*.zip").Where(z => !string.Equals(z, zip, StringComparison.OrdinalIgnoreCase)))
                {
                    try
                    {
                        File.Delete(old);
                    }
                    catch (IOException)
                    {
                    }
                }
            }
        }

        private static void Extract(ZipArchive archive, string entryName, FileEntry f, Paths p, InstallState st, bool record)
        {
            var entry = archive.GetEntry(entryName) ?? throw new InvalidDataException("В архіві налаштувань немає " + entryName);
            string full = p.InGame(f.Path);
            Directory.CreateDirectory(Path.GetDirectoryName(full));
            string tmp = full + ".part";
            entry.ExtractToFile(tmp, true);
            if (Net.Sha1Of(tmp) != f.Sha1)
            {
                File.Delete(tmp);
                throw new HashMismatchException(f.Path);
            }
            if (File.Exists(full)) File.Delete(full);
            File.Move(tmp, full);
            if (record) st.Record(f.Path, full);
        }

        public static string Mb(long bytes) => (bytes / 1048576.0).ToString(bytes < 10 * 1048576 ? "0.0" : "0", System.Globalization.CultureInfo.InvariantCulture) + " МБ";
    }
}
