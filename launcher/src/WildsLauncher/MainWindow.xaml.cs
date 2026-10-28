using System;
using System.ComponentModel;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using Wilds.Launcher.Core;

namespace Wilds.Launcher
{
    public partial class MainWindow : Window
    {
        private readonly Engine engine;
        private readonly CancellationTokenSource cancel = new CancellationTokenSource();
        private bool busy, ready;

        public MainWindow()
        {
            InitializeComponent();
            engine = new Engine(App.Paths, App.Source);
            Loaded += async (s, e) => await Start();
        }

        private async Task Start()
        {
            VersionText.Text = "Програма " + SelfUpdate.Current.ToString(3);
            SetBusy(true);
            ShowStatus("Перевірка збірки…");
            try
            {
                await engine.Load(cancel.Token);
                if (!engine.Offline && !App.Has("--updated") && SelfUpdate.IsNewer(engine.Config))
                {
                    ShowStatus("Оновлення програми до версії " + engine.Config.UpdaterVersion + "…");
                    await SelfUpdate.Run(engine.Config, engine.Net, App.Args, cancel.Token);
                    Application.Current.Shutdown();
                    return;
                }
                Fill();
                await Refresh();
            }
            catch (OperationCanceledException)
            {
            }
            catch (Exception e)
            {
                Fail(e);
            }
            finally
            {
                SetBusy(false);
            }
            string shot = App.Option("--screenshot");
            if (shot != null)
            {
                await Task.Delay(500);
                SaveScreenshot(shot);
                Close();
            }
        }

        private void SaveScreenshot(string file)
        {
            var root = (FrameworkElement)Content;
            var dpi = VisualTreeHelper.GetDpi(this);
            var bmp = new System.Windows.Media.Imaging.RenderTargetBitmap((int)(root.ActualWidth * dpi.DpiScaleX), (int)(root.ActualHeight * dpi.DpiScaleY),
                dpi.PixelsPerInchX, dpi.PixelsPerInchY, PixelFormats.Pbgra32);
            bmp.Render(root);
            var png = new System.Windows.Media.Imaging.PngBitmapEncoder();
            png.Frames.Add(System.Windows.Media.Imaging.BitmapFrame.Create(bmp));
            using (var f = System.IO.File.Create(file)) png.Save(f);
        }

        /// <summary>the parts of the window that come from the pack: news, presets, server, memory</summary>
        private void Fill()
        {
            PackTitle.Text = engine.Config.PackName;
            News.ItemsSource = engine.Config.News;
            NewsCard.Visibility = engine.Config.News.Count > 0 ? Visibility.Visible : Visibility.Collapsed;
            VersionText.Text = $"Програма {SelfUpdate.Current.ToString(3)} · збірка {engine.Manifest.Version}" + (engine.Offline ? " · без зв'язку з GitHub" : "");

            PresetList.Children.Clear();
            var current = engine.CurrentPreset;
            foreach (var preset in engine.Presets.Presets)
            {
                var text = new StackPanel();
                text.Children.Add(new TextBlock { Text = preset.Name, FontSize = 15, FontWeight = FontWeights.SemiBold });
                text.Children.Add(new TextBlock
                {
                    Text = preset.Description, FontSize = 12, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 2, 0, 0),
                    Foreground = (Brush)FindResource("Muted"),
                });
                var card = new RadioButton { Style = (Style)FindResource("PresetCard"), Content = text, GroupName = "preset", Tag = preset.Id, IsChecked = preset.Id == current.Id };
                card.Checked += OnPresetChecked;
                PresetList.Children.Add(card);
            }

            var server = engine.Config.Servers.Count > 0 ? engine.Config.Servers[0] : null;
            ServerButton.Visibility = server == null ? Visibility.Collapsed : Visibility.Visible;
            if (server != null)
            {
                var line = new TextBlock();
                line.Inlines.Add(new System.Windows.Documents.Run("Сервер  ") { Foreground = (Brush)FindResource("Faint") });
                line.Inlines.Add(new System.Windows.Documents.Run(server.Address) { Foreground = (Brush)FindResource("Text") });
                ServerButton.Content = line;
            }

            ShowFolder();
            int total = GameStarter.TotalMemoryGb(), recommended = GameStarter.RecommendedMemoryGb(total);
            MemorySlider.Maximum = Math.Max(4, Math.Min(16, total - 2));
            MemorySlider.Minimum = Math.Min(3, MemorySlider.Maximum);
            MemorySlider.Value = engine.MemoryGb;
            MemoryHint.Text = $"На комп'ютері {total} ГБ. Рекомендовано {recommended} ГБ." +
                              (total < 8 ? " Збірці тісно на такому комп'ютері: закрийте браузер перед грою." : "");
            UpdateMemoryText();
        }

        /// <summary>what the button says: install, update, apply, or that all is in place</summary>
        private async Task Refresh()
        {
            ShowStatus("Перевірка файлів…");
            var plan = await Task.Run(() => engine.Plan(false));
            bool firstTime = !engine.State.Installed;
            bool work = engine.NeedsWork(plan);
            PlayButton.Content = firstTime ? "ВСТАНОВИТИ" : !plan.Empty || engine.State.PackVersion != engine.Manifest.Version ? "ОНОВИТИ"
                : work ? "ЗАСТОСУВАТИ" : "ГОТОВО ✓";
            if (firstTime)
                ShowStatus($"Збірку ще не встановлено. Буде завантажено ~{PackSync.Mb(plan.DownloadBytes)} модів, NeoForge і Minecraft {engine.Manifest.Minecraft}. " +
                           "Якщо в теці модів уже щось лежить, її буде збережено під іншою назвою.");
            else if (!plan.Empty || engine.State.PackVersion != engine.Manifest.Version)
                ShowStatus($"Є оновлення збірки {engine.Manifest.Version}" + (plan.Empty ? "." : $": {plan}" + (plan.DownloadBytes > 0 ? $", ~{PackSync.Mb(plan.DownloadBytes)}." : ".")) +
                           " Натисніть «Оновити».");
            else if (work)
                ShowStatus("Змінено пресет графіки чи пам'ять. Натисніть «Застосувати».");
            else if (engine.Offline)
                ShowStatus("Немає зв'язку з GitHub: оновлення перевірити не вдалося. Встановлена збірка працює як є.");
            else
                ShowStatus(Ready());
            Bar.IsIndeterminate = false;
            Bar.Value = work ? 0 : 1;
            ready = true;
        }

        /// <summary>what the player does next: no launcher is opened, the version is in theirs</summary>
        private string Ready() =>
            $"Усе встановлено й актуально. Відкрийте свій лаунчер Minecraft і оберіть версію «{SharedMinecraft.VersionId}» " +
            $"(в офіційному — установку «{engine.Config.PackName}»). Сервер уже є в списку серверів гри. " +
            $"У TLauncher та подібних виставте в налаштуваннях лаунчера пам'ять {engine.MemoryGb} ГБ.";

        private async void OnPlay(object sender, RoutedEventArgs e) => await Run(false);

        private async void OnVerify(object sender, RoutedEventArgs e) => await Run(true);

        private async Task Run(bool deep)
        {
            if (busy || !ready) return;
            SetBusy(true);
            var progress = new Progress<Status>(s =>
            {
                ShowStatus(s.Text);
                Bar.IsIndeterminate = s.Fraction < 0;
                if (s.Fraction >= 0) Bar.Value = s.Fraction;
            });
            try
            {
                await Task.Run(() => engine.Prepare(deep, App.Icon(), progress, cancel.Token));
                PlayButton.Content = "ГОТОВО ✓";
                Bar.IsIndeterminate = false;
                Bar.Value = 1;
                string archived = engine.ArchivedMods == null ? "" : $" Моди, що лежали в теці mods раніше, збережено в теці {engine.ArchivedMods}.";
                ShowStatus((deep ? "Усі файли перевірено й справні. " : "Готово! ") + Ready() + archived);
            }
            catch (OperationCanceledException)
            {
            }
            catch (Exception ex)
            {
                Fail(ex);
            }
            finally
            {
                SetBusy(false);
            }
        }

        private void OnPresetChecked(object sender, RoutedEventArgs e)
        {
            if (!(sender is RadioButton r) || !(r.Tag is string id) || engine.Settings.Preset == id) return;
            engine.Settings.Preset = id;
            engine.SaveSettings();
            if (ready && !busy && engine.State.Installed)
            {
                PlayButton.Content = engine.State.AppliedPreset == id ? "ГОТОВО ✓" : "ЗАСТОСУВАТИ";
                ShowStatus("Натисніть «Застосувати», щоб увімкнути пресет «" + engine.CurrentPreset.Name + "». Окремі налаштування можна змінити в самій грі — вони збережуться.");
            }
        }

        private void OnMemoryChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (engine == null || !IsLoaded) return;
            int gb = (int)Math.Round(MemorySlider.Value);
            engine.Settings.MemoryGb = gb == GameStarter.RecommendedMemoryGb(GameStarter.TotalMemoryGb()) ? 0 : gb;
            engine.SaveSettings();
            UpdateMemoryText();
            if (ready && !busy && engine.State.Installed && engine.State.AppliedMemory != engine.MemoryGb.ToString()) PlayButton.Content = "ЗАСТОСУВАТИ";
        }

        private void UpdateMemoryText() => MemoryText.Text = (int)Math.Round(MemorySlider.Value) + " ГБ";

        private void OnCopyServer(object sender, RoutedEventArgs e)
        {
            if (engine.Config == null || engine.Config.Servers.Count == 0) return;
            try
            {
                Clipboard.SetText(engine.Config.Servers[0].Address);
                ShowStatus("Адресу сервера скопійовано. Він уже є в списку серверів гри.");
            }
            catch (System.Runtime.InteropServices.COMException)
            {
            }
        }

        private void OnOpenFolder(object sender, RoutedEventArgs e) => GameStarter.OpenFolder(engine.Paths.GameDir);

        // ---------------------------------------------------------------- the Minecraft folder

        private static bool SameFolder(string a, string b) =>
            string.Equals(Path.GetFullPath(a).TrimEnd('\\'), Path.GetFullPath(b).TrimEnd('\\'), StringComparison.OrdinalIgnoreCase);

        private bool StandardFolder => SameFolder(engine.Paths.MinecraftDir, Paths.StandardMinecraftDir());

        private void ShowFolder()
        {
            FolderText.Text = engine.Paths.MinecraftDir + (StandardFolder ? "  (стандартна)" : "");
            FolderText.ToolTip = engine.Paths.MinecraftDir;
            ResetFolderButton.Visibility = StandardFolder ? Visibility.Collapsed : Visibility.Visible;
        }

        private static bool LooksLikeMinecraft(string dir) =>
            Directory.Exists(Path.Combine(dir, "versions")) || Directory.Exists(Path.Combine(dir, "saves"))
            || File.Exists(Path.Combine(dir, "launcher_profiles.json")) || File.Exists(Path.Combine(dir, "options.txt"));

        private void OnChangeFolder(object sender, RoutedEventArgs e)
        {
            if (busy) return;
            using (var dlg = new System.Windows.Forms.FolderBrowserDialog
                   {
                       Description = "Оберіть теку Minecraft, з якою працює ваш лаунчер (у ній зазвичай є теки versions і saves).",
                       SelectedPath = engine.Paths.MinecraftDir,
                       ShowNewFolderButton = true,
                   })
            {
                if (dlg.ShowDialog() != System.Windows.Forms.DialogResult.OK) return;
                string dir = dlg.SelectedPath;
                // the folder above .minecraft was picked: the game folder is the one inside
                string inner = Path.Combine(dir, ".minecraft");
                if (!LooksLikeMinecraft(dir) && LooksLikeMinecraft(inner)) dir = inner;
                if (!LooksLikeMinecraft(dir) && MessageBox.Show(this, $"У теці «{dir}» не видно Minecraft (немає тек versions і saves).\n\n" +
                                                                      "Однаково встановлювати збірку сюди?", "Wilds & Wanderers",
                        MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes)
                    return;
                SwitchFolder(dir);
            }
        }

        private void OnResetFolder(object sender, RoutedEventArgs e)
        {
            if (!busy) SwitchFolder(Paths.StandardMinecraftDir());
        }

        /// <summary>the program starts anew with the other folder (everything it knows is per folder)</summary>
        private void SwitchFolder(string dir)
        {
            dir = Path.GetFullPath(dir);
            if (SameFolder(dir, engine.Paths.MinecraftDir)) return;
            if (engine.State.Installed && MessageBox.Show(this, $"Збірку, встановлену в «{engine.Paths.MinecraftDir}», при цьому не буде прибрано.\n" +
                                                                "Щоб прибрати її, спершу натисніть «Видалити збірку».\n\nПерейти до іншої теки?",
                    "Wilds & Wanderers", MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes)
                return;
            engine.Settings.MinecraftDir = SameFolder(dir, Paths.StandardMinecraftDir()) ? null : dir;
            engine.SaveSettings();
            Log.Info("Тека Minecraft: " + dir);
            var keep = new System.Collections.Generic.List<string>();
            for (int i = 0; i < App.Args.Length; i++)
            {
                if (App.Args[i] == "--minecraft-dir") i++;
                else if (App.Args[i] != "--updated") keep.Add(App.Args[i]);
            }
            System.Diagnostics.Process.Start(System.Reflection.Assembly.GetExecutingAssembly().Location,
                string.Join(" ", keep.ConvertAll(a => "\"" + a + "\"")));
            Application.Current.Shutdown();
        }

        // ---------------------------------------------------------------- removing the pack

        private async void OnUninstall(object sender, RoutedEventArgs e)
        {
            if (busy || !ready) return;
            UninstallPlan plan;
            try
            {
                plan = engine.PlanUninstall();
            }
            catch (Exception ex)
            {
                Fail(ex);
                return;
            }
            string back = plan.RestoreArchive == null ? "" : $"\n\nМоди, що були в теці mods до збірки, повернуться (з теки {Path.GetFileName(plan.RestoreArchive)}).";
            string text = $"Видалити збірку Wilds & Wanderers з теки «{engine.Paths.MinecraftDir}»?\n\n" +
                          $"Буде видалено ({PackSync.Mb(plan.Bytes)}):\n" +
                          "• моди й налаштування збірки та все, що створили її моди (кеші, мапа, дані модів);\n" +
                          $"• версію «{SharedMinecraft.VersionId}»" + (plan.RemovesNeoForge ? " і NeoForge" : "") + ", установку в офіційному лаунчері;\n" +
                          "• сервер збірки зі списку серверів і тимчасові файли програми.\n\n" +
                          "Залишаться: ваші світи, знімки екрана, схеми, налаштування гри й ресурспаки." + back;
            if (MessageBox.Show(this, text, "Видалення збірки", MessageBoxButton.YesNo, MessageBoxImage.Warning, MessageBoxResult.No) != MessageBoxResult.Yes) return;
            SetBusy(true);
            ShowStatus("Видалення збірки…");
            try
            {
                await Task.Run(() => engine.Uninstall(plan));
                PlayButton.Content = "ВСТАНОВИТИ";
                Bar.IsIndeterminate = false;
                Bar.Value = 0;
                ShowStatus($"Збірку видалено, звільнено {PackSync.Mb(plan.Bytes)}. Ваші світи й налаштування на місці." +
                           (plan.RestoreArchive != null ? " Попередні моди повернуто в теку mods." : "") +
                           " Встановити збірку знову можна кнопкою «Встановити».");
            }
            catch (Exception ex)
            {
                Fail(ex);
            }
            finally
            {
                SetBusy(false);
            }
        }

        private void OnMinimize(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;

        private void OnClose(object sender, RoutedEventArgs e) => Close();

        protected override void OnClosing(CancelEventArgs e)
        {
            if (busy && MessageBox.Show(this, "Зараз іде завантаження. Перервати й закрити? Наступного разу програма докачає решту.",
                    "Wilds & Wanderers", MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes)
            {
                e.Cancel = true;
                return;
            }
            cancel.Cancel();
            base.OnClosing(e);
        }

        protected override void OnClosed(EventArgs e)
        {
            engine.Dispose();
            base.OnClosed(e);
        }

        private void SetBusy(bool on)
        {
            busy = on;
            PlayButton.IsEnabled = !on && ready;
            VerifyButton.IsEnabled = !on && ready;
            UninstallButton.IsEnabled = !on && ready && engine.State.Installed;
            FolderButton.IsEnabled = ResetFolderButton.IsEnabled = !on;
            foreach (var c in PresetList.Children)
                if (c is RadioButton r) r.IsEnabled = !on;
            MemorySlider.IsEnabled = !on;
            if (on) Bar.IsIndeterminate = true;
        }

        private void ShowStatus(string text)
        {
            StatusText.Foreground = (Brush)FindResource("Text");
            StatusText.Text = text;
        }

        private void Fail(Exception e)
        {
            Log.Error("Помилка", e);
            Bar.IsIndeterminate = false;
            Bar.Value = 0;
            StatusText.Foreground = (Brush)FindResource("Danger");
            string reason = e is System.Net.Http.HttpRequestException || e.InnerException is System.Net.Http.HttpRequestException
                ? "Немає зв'язку із сервером завантаження. Перевірте інтернет і спробуйте ще раз."
                : e.Message;
            StatusText.Text = reason + "  (журнал: " + Log.FilePath + ")";
        }
    }
}
