using System;
using System.ComponentModel;
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
            else if (!plan.Empty)
                ShowStatus($"Є оновлення збірки {engine.Manifest.Version}: {plan}" + (plan.DownloadBytes > 0 ? $", ~{PackSync.Mb(plan.DownloadBytes)}." : "."));
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
