using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// The program updates itself from the release config.json names: the new exe is downloaded beside the running one,
    /// checked (SHA-256), swapped in by renaming (Windows lets a running exe be renamed) and started; the old copy is
    /// removed on the next start.
    /// </summary>
    public static class SelfUpdate
    {
        public static Version Current => Assembly.GetExecutingAssembly().GetName().Version;

        private static string Exe => Assembly.GetExecutingAssembly().Location;

        public static void CleanUp()
        {
            string old = Exe + ".old";
            for (int i = 0; i < 5 && File.Exists(old); i++)
            {
                try
                {
                    File.Delete(old);
                }
                catch (Exception e) when (e is IOException || e is UnauthorizedAccessException)
                {
                    Thread.Sleep(400);
                }
            }
        }

        public static bool IsNewer(PackConfig c) =>
            !string.IsNullOrEmpty(c.UpdaterVersion) && !string.IsNullOrEmpty(c.UpdaterUrl) && !string.IsNullOrEmpty(c.UpdaterSha256)
            && Version.TryParse(c.UpdaterVersion, out var v) && Normalize(v) > Normalize(Current);

        private static Version Normalize(Version v) => new Version(v.Major, v.Minor, Math.Max(0, v.Build));

        /// <summary>downloads and starts the new version; the caller then closes this one</summary>
        public static async Task Run(PackConfig c, Net net, string[] args, CancellationToken ct)
        {
            string exe = Exe, fresh = exe + ".new", old = exe + ".old";
            await net.Download(c.UpdaterUrl, fresh, null, c.UpdaterSha256, null, ct).ConfigureAwait(false);
            if (File.Exists(old)) File.Delete(old);
            File.Move(exe, old);
            try
            {
                File.Move(fresh, exe);
            }
            catch
            {
                File.Move(old, exe);
                throw;
            }
            Log.Info($"Програму оновлено: {Current} → {c.UpdaterVersion}");
            string quoted = string.Join(" ", args.Select(a => "\"" + a.Replace("\"", "\\\"") + "\""));
            Process.Start(new ProcessStartInfo(exe, quoted) { UseShellExecute = false });
        }
    }
}
