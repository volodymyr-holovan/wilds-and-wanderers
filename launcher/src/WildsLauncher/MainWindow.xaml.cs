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
                if (!engine.Offline && SelfUpdate.IsNewer(engine.Config))
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

        /// <summary>what the button says: install, update or play</summary>
        private async Task Refresh()
        {
            ShowStatus("Перевірка файлів…");
            var plan = await Task.Run(() => engine.Plan(false));
            bool firstTime = !engine.State.Installed;
            bool work = engine.NeedsWork(plan);
            PlayButton.Content = firstTime ? "ВСТАНОВИТИ" : work ? "ОНОВИТИ Й ГРАТИ" : "ГРАТИ";
            if (firstTime)
                ShowStatus($"Збірку ще не встановлено. Буде завантажено ~{PackSync.Mb(plan.DownloadBytes)} модів, NeoForge і Minecraft {engine.Manifest.Minecraft}.");
            else if (!plan.Empty)
                ShowStatus($"Є оновлення збірки {engine.Manifest.Version}: {plan}" + (plan.DownloadBytes > 0 ? $", ~{PackSync.Mb(plan.DownloadBytes)}." : "."));
            else if (engine.Offline)
                ShowStatus("Немає зв'язку з GitHub: оновлення перевірити не вдалося, гра запуститься з тим, що вже є.");
            else
                ShowStatus("Усе актуально. Гарної гри!");
            Bar.IsIndeterminate = false;
            Bar.Value = firstTime || work ? 0 : 1;
            ready = true;
        }

        private async void OnPlay(object sender, RoutedEventArgs e) => await Run(false, true);

        private async void OnVerify(object sender, RoutedEventArgs e) => await Run(true, false);

        private async Task Run(bool deep, bool launch)
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
                PlayButton.Content = "ГРАТИ";
                Bar.IsIndeterminate = false;
                Bar.Value = 1;
                if (!launch)
                {
                    ShowStatus("Усі файли перевірено й справні.");
                    return;
                }
                if (GameStarter.StartOfficialLauncher())
                {
                    ShowStatus("Лаунчер Minecraft відкривається. Увійдіть у свій акаунт, оберіть установку «" + engine.Config.PackName +
                               "» і натисніть «Грати». Сервер уже є в списку серверів гри.");
                    WindowState = WindowState.Minimized;
                }
                else
                {
                    ShowStatus("Збірку готово, але лаунчер Minecraft не знайдено.");
                    if (MessageBox.Show(this, "Збірку встановлено, але на комп'ютері немає офіційного лаунчера Minecraft.\n\n" +
                                              "Встановіть його з minecraft.net, увійдіть у свій акаунт — установка «" + engine.Config.PackName +
                                              "» з'явиться в ньому сама.\n\nВідкрити сторінку завантаження?",
                            "Wilds & Wanderers", MessageBoxButton.YesNo, MessageBoxImage.Information) == MessageBoxResult.Yes)
                        GameStarter.OpenDownloadPage();
                }
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
                PlayButton.Content = engine.State.AppliedPreset == id ? "ГРАТИ" : "ЗАСТОСУВАТИ Й ГРАТИ";
                ShowStatus("Пресет «" + engine.CurrentPreset.Name + "» застосується під час запуску. Окремі налаштування можна змінити в самій грі — вони збережуться.");
            }
        }

        private void OnMemoryChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (engine == null || !IsLoaded) return;
            int gb = (int)Math.Round(MemorySlider.Value);
            engine.Settings.MemoryGb = gb == GameStarter.RecommendedMemoryGb(GameStarter.TotalMemoryGb()) ? 0 : gb;
            engine.SaveSettings();
            UpdateMemoryText();
            if (ready && !busy && engine.State.Installed && engine.State.AppliedMemory != engine.MemoryGb.ToString()) PlayButton.Content = "ЗАСТОСУВАТИ Й ГРАТИ";
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
