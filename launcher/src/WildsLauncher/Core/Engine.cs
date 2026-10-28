using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// One run of the program: read what the pack wants (config, manifest, presets from the repository), compare it
    /// with the game folder, bring everything up to date, then hand over to the official launcher.
    /// </summary>
    public sealed class Engine : IDisposable
    {
        public const string DefaultSource = "https://raw.githubusercontent.com/volodymyr-holovan/wilds-and-wanderers/main/launcher/";

        private readonly string source;
        private readonly bool localSource;
        public readonly Net Net = new Net();

        public Paths Paths { get; }
        public Settings Settings { get; }
        public InstallState State { get; private set; }
        public PackConfig Config { get; private set; }
        public Manifest Manifest { get; private set; }
        public PresetSet Presets { get; private set; }
        /// <summary>the repository could not be reached: the last known copy is used, updates wait</summary>
        public bool Offline { get; private set; }

        public Engine(Paths paths, string source)
        {
            Paths = paths;
            this.source = string.IsNullOrEmpty(source) ? DefaultSource : source;
            localSource = !this.source.StartsWith("https://", StringComparison.OrdinalIgnoreCase);
            Net.AllowLocalFiles = localSource;
            Directory.CreateDirectory(paths.DataDir);
            Directory.CreateDirectory(paths.CacheDir);
            Settings = Settings.Load(paths.SettingsFile);
            State = InstallState.Load(paths.StateFile);
        }

        public async Task Load(CancellationToken ct)
        {
            string config, manifest, presets;
            try
            {
                config = await Fetch("config.json", ct).ConfigureAwait(false);
                manifest = await Fetch("manifest.json", ct).ConfigureAwait(false);
                presets = await Fetch("presets.json", ct).ConfigureAwait(false);
                Parse(config, manifest, presets);
                foreach (var (name, text) in new[] { ("config.json", config), ("manifest.json", manifest), ("presets.json", presets) })
                    Files.WriteAtomic(Path.Combine(Paths.CacheDir, name), text);
                Offline = false;
                Log.Info($"Збірка {Manifest.Version}: {Manifest.Mods.Count} модів, {Manifest.Overrides.Count} файлів налаштувань");
            }
            catch (Exception e) when (!(e is OperationCanceledException))
            {
                Log.Warn("Репозиторій недоступний: " + e.Message);
                string Cached(string n) => File.ReadAllText(Path.Combine(Paths.CacheDir, n), Encoding.UTF8);
                try
                {
                    Parse(Cached("config.json"), Cached("manifest.json"), Cached("presets.json"));
                }
                catch (Exception inner) when (inner is IOException || inner is InvalidDataException || inner is ArgumentException)
                {
                    throw new InvalidOperationException("Не вдалося зв'язатися з GitHub, а збереженої копії збірки ще немає. Перевірте інтернет.", e);
                }
                Offline = true;
            }
        }

        private void Parse(string config, string manifest, string presets)
        {
            var c = PackConfig.Parse(config);
            var m = Manifest.Parse(manifest);
            var p = PresetSet.Parse(presets);
            Config = c;
            Manifest = m;
            Presets = p;
        }

        private Task<string> Fetch(string name, CancellationToken ct)
        {
            if (localSource) return Task.FromResult(File.ReadAllText(Path.Combine(source, name), Encoding.UTF8));
            // a fresh copy, not one a cache along the way still holds
            return Net.GetString(source + name + "?t=" + DateTime.UtcNow.Ticks / TimeSpan.TicksPerMinute, ct);
        }

        public Preset CurrentPreset => Presets.Find(Settings.Preset);

        public int MemoryGb => Settings.MemoryGb > 0 ? Settings.MemoryGb : GameStarter.RecommendedMemoryGb(GameStarter.TotalMemoryGb());

        public SyncPlan Plan(bool deep) => PackSync.Plan(Manifest, Paths, State, deep, !State.Installed);

        /// <summary>after Prepare: where mods of something else went (null: there were none)</summary>
        public string ArchivedMods { get; private set; }

        /// <summary>is there anything to do before the game can start</summary>
        public bool NeedsWork(SyncPlan plan) =>
            !plan.Empty || State.PackVersion != Manifest.Version || !NeoForge.IsInstalled(Paths, Manifest) || !File.Exists(SharedMinecraft.VersionFile(Paths))
            || State.AppliedPreset != CurrentPreset.Id || State.AppliedMemory != MemoryGb.ToString();

        public async Task Prepare(bool deep, byte[] icon, IProgress<Status> progress, CancellationToken ct)
        {
            if (GameRunning())
                throw new InvalidOperationException("Гра зі збіркою зараз запущена. Закрийте її, щоб оновити файли.");
            progress?.Report(new Status(deep ? "Перевірка всіх файлів…" : "Перевірка файлів…"));
            Directory.CreateDirectory(Paths.GameDir);
            if (!Paths.Isolated || Directory.Exists(Paths.LegacyGameDir))
            {
                int worlds = SharedMinecraft.MigrateLegacy(Paths, Manifest);
                if (worlds >= 0) progress?.Report(new Status($"Перенесено з попередньої теки збірки: світів {worlds}"));
            }
            ArchivedMods = SharedMinecraft.ArchiveForeignMods(Paths, Manifest, State) ?? ArchivedMods;
            var plan = await Task.Run(() => Plan(deep), ct).ConfigureAwait(false);
            Log.Info("План оновлення: " + plan);
            try
            {
                await PackSync.Apply(plan, Manifest, Paths, State, Net, progress, ct).ConfigureAwait(false);
            }
            finally
            {
                State.Save(Paths.StateFile);   // what did arrive is remembered, a retry fetches only the rest
            }

            if (!NeoForge.IsInstalled(Paths, Manifest)) await NeoForge.Install(Paths, Manifest, Net, progress, ct).ConfigureAwait(false);
            SharedMinecraft.EnsureVersion(Paths, Manifest);

            progress?.Report(new Status("Налаштування профілю й списку серверів…"));
            LauncherProfiles.Ensure(Paths, Manifest, Config.PackName, MemoryGb, icon);
            ServersDat.Ensure(Paths.InGame("servers.dat"), Config.Servers);
            var preset = CurrentPreset;
            if (State.AppliedPreset != preset.Id)
            {
                GraphicsPresets.Apply(Paths, preset);
                State.AppliedPreset = preset.Id;
            }
            State.AppliedMemory = MemoryGb.ToString();
            State.PackVersion = Manifest.Version;
            State.Save(Paths.StateFile);
            progress?.Report(new Status("Готово", 1));
        }

        /// <summary>
        /// is the game with this pack's folder running now. The running game holds its mods open, so a mod that cannot be
        /// opened exclusively means it is (the process list cannot tell: Sodium rewrites the game's command line).
        /// </summary>
        public bool GameRunning()
        {
            string mods = Paths.InGame("mods");
            if (!Directory.Exists(mods)) return false;
            foreach (string jar in Directory.EnumerateFiles(mods, "*.jar").Take(3))
            {
                try
                {
                    using (new FileStream(jar, FileMode.Open, FileAccess.ReadWrite, FileShare.None)) { }
                }
                catch (IOException)
                {
                    return true;
                }
                catch (UnauthorizedAccessException)
                {
                }
            }
            return false;
        }

        public void SaveSettings() => Settings.Save(Paths.SettingsFile);

        public void Dispose() => Net.Dispose();
    }
}
