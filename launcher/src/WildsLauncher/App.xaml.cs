using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using Wilds.Launcher.Core;

namespace Wilds.Launcher
{
    /// <summary>
    /// Start-up. Without arguments: the window. For the pack's own tests:
    ///   --headless          install/update without a window, log to the console; exit code 0 = ready
    ///   --root DIR          everything (.minecraft, game folder, data) inside DIR, nothing of this computer used
    ///   --source DIR|URL    where config/manifest/presets come from (default: the GitHub repository)
    ///   --deep              re-check every file's hash
    ///   --launch            start the official launcher afterwards
    ///   --screenshot FILE   (window) save a picture of the window once it is ready, then close
    /// </summary>
    public partial class App : Application
    {
        private static Mutex single;

        public static Paths Paths { get; private set; }
        public static string Source { get; private set; }
        public static string[] Args { get; private set; }

        [DllImport("kernel32.dll")]
        private static extern bool AttachConsole(int processId);

        protected override void OnStartup(StartupEventArgs e)
        {
            Args = e.Args;
            string root = Option("--root"), gameDir = Option("--game-dir");
            Paths = root != null ? Core.Paths.ForRoot(root) : Core.Paths.Default();
            if (gameDir != null) Paths = Paths.WithGameDir(gameDir);
            Source = Option("--source");
            Log.Init(Paths.DataDir);
            Log.Info($"Wilds & Wanderers {SelfUpdate.Current} · {Environment.OSVersion} · .NET {Environment.Version}");
            SelfUpdate.CleanUp();

            DispatcherUnhandledException += (s, ex) =>
            {
                Log.Error("Неочікувана помилка", ex.Exception);
                MessageBox.Show("Сталася неочікувана помилка:\n" + ex.Exception.Message + "\n\nЖурнал: " + Log.FilePath,
                    "Wilds & Wanderers", MessageBoxButton.OK, MessageBoxImage.Error);
                ex.Handled = true;
            };
            AppDomain.CurrentDomain.UnhandledException += (s, ex) => Log.Error("Неочікувана помилка", ex.ExceptionObject as Exception);

            // one window per game folder (a stable hash of the folder names the lock, the same in every process)
            string id;
            using (var sha = System.Security.Cryptography.SHA1.Create())
                id = Net.Hex(sha.ComputeHash(System.Text.Encoding.UTF8.GetBytes(Paths.GameDir.ToLowerInvariant()))).Substring(0, 16);
            single = new Mutex(true, "Local\\WildsAndWanderers-" + id, out bool first);
            if (!first && Has("--updated"))
            {
                // started by the copy it replaces, which is closing right now
                try
                {
                    first = single.WaitOne(TimeSpan.FromSeconds(10));
                }
                catch (AbandonedMutexException)
                {
                    first = true;
                }
            }
            if (!first)
            {
                if (!Has("--headless")) MessageBox.Show("Програма вже відкрита.", "Wilds & Wanderers", MessageBoxButton.OK, MessageBoxImage.Information);
                Shutdown(2);
                return;
            }

            if (Has("--headless"))
            {
                AttachConsole(-1);
                try
                {
                    Console.OutputEncoding = System.Text.Encoding.UTF8;
                }
                catch (IOException)
                {
                }
                Log.ToConsole = true;
                int code = 1;
                try
                {
                    code = Task.Run(Headless).GetAwaiter().GetResult();
                }
                catch (Exception ex)
                {
                    Log.Error("Не вдалося", ex);
                }
                Shutdown(code);
                return;
            }
            base.OnStartup(e);
            new MainWindow().Show();
        }

        private static async Task<int> Headless()
        {
            using (var engine = new Engine(Paths, Source))
            {
                await engine.Load(CancellationToken.None);
                var reporter = new ConsoleProgress();
                await engine.Prepare(Has("--deep"), Icon(), reporter, CancellationToken.None);
                Log.Info($"Готово: збірка {engine.Manifest.Version}, пресет «{engine.CurrentPreset.Name}», пам'ять {engine.MemoryGb} ГБ");
                if (Has("--launch") && !GameStarter.StartOfficialLauncher()) return 3;
                return 0;
            }
        }

        private sealed class ConsoleProgress : IProgress<Status>
        {
            private string last;

            public void Report(Status s)
            {
                // the console gets each step once, not every byte
                string text = s.Text.Split('·')[0].Trim();
                if (text == last) return;
                last = text;
                Log.Info(s.Text);
            }
        }

        public static byte[] Icon()
        {
            try
            {
                var res = GetResourceStream(new Uri("pack://application:,,,/Assets/icon.png"));
                if (res == null) return null;
                using (var ms = new MemoryStream())
                {
                    res.Stream.CopyTo(ms);
                    return ms.ToArray();
                }
            }
            catch (IOException)
            {
                return null;
            }
        }

        public static bool Has(string flag) => Array.Exists(Args ?? new string[0], a => string.Equals(a, flag, StringComparison.OrdinalIgnoreCase));

        public static string Option(string name)
        {
            for (int i = 0; i + 1 < Args.Length; i++)
            {
                if (string.Equals(Args[i], name, StringComparison.OrdinalIgnoreCase)) return Args[i + 1];
            }
            return null;
        }
    }
}
