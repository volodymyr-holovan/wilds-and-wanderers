using System;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;

namespace Wilds.Launcher.Core
{
    public sealed class HashMismatchException : Exception
    {
        public HashMismatchException(string what) : base("Файл пошкоджено під час завантаження: " + what) { }
    }

    /// <summary>
    /// Downloads over HTTPS from the hosts the pack uses, each file checked against the hash the manifest gives before
    /// it replaces anything; a broken or tampered download never reaches the game folder.
    /// </summary>
    public sealed class Net : IDisposable
    {
        private static readonly string[] Hosts =
        {
            "raw.githubusercontent.com", "github.com", "objects.githubusercontent.com", "release-assets.githubusercontent.com",
            "cdn.modrinth.com", "mediafilez.forgecdn.net", "edge.forgecdn.net", "maven.neoforged.net", "api.adoptium.net",
        };

        private readonly HttpClient http;

        /// <summary>file: links (a local test source) are allowed only when the source itself is a folder</summary>
        public bool AllowLocalFiles { get; set; }

        static Net()
        {
            ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls12 | SecurityProtocolType.Tls13;
            ServicePointManager.DefaultConnectionLimit = 16;
        }

        public Net()
        {
            var handler = new HttpClientHandler { AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate };
            http = new HttpClient(handler) { Timeout = TimeSpan.FromMinutes(30) };
            http.DefaultRequestHeaders.UserAgent.ParseAdd("WildsAndWanderers/" + typeof(Net).Assembly.GetName().Version.ToString(3)
                                                           + " (+https://github.com/volodymyr-holovan/wilds-and-wanderers)");
        }

        private Uri Check(string url)
        {
            var uri = new Uri(url);
            if (uri.IsFile)
            {
                if (AllowLocalFiles) return uri;
                throw new InvalidDataException("Локальні файли як джерело не дозволені: " + url);
            }
            if (uri.Scheme != Uri.UriSchemeHttps || !Hosts.Contains(uri.Host, StringComparer.OrdinalIgnoreCase))
                throw new InvalidDataException("Недозволене джерело завантаження: " + uri.Host);
            return uri;
        }

        public async Task<string> GetString(string url, CancellationToken ct)
        {
            var uri = Check(url);
            if (uri.IsFile) return File.ReadAllText(uri.LocalPath);
            using (var r = await http.GetAsync(uri, ct).ConfigureAwait(false))
            {
                r.EnsureSuccessStatusCode();
                Check(r.RequestMessage.RequestUri.ToString());
                return await r.Content.ReadAsStringAsync().ConfigureAwait(false);
            }
        }

        /// <summary>
        /// Downloads to dest (through dest.part), checked against sha1 or sha256 (whichever is given). Retries three
        /// times; progress reports the bytes of this attempt.
        /// </summary>
        public async Task Download(string url, string dest, string sha1, string sha256, Action<long> onBytes, CancellationToken ct)
        {
            if (string.IsNullOrEmpty(sha1) && string.IsNullOrEmpty(sha256))
                throw new InvalidDataException("Немає контрольної суми для " + Path.GetFileName(dest));
            Directory.CreateDirectory(Path.GetDirectoryName(dest));
            string part = dest + ".part";
            for (int attempt = 1; ; attempt++)
            {
                long counted = 0;
                try
                {
                    var uri = Check(url);
                    using (HashAlgorithm hash = string.IsNullOrEmpty(sha256) ? (HashAlgorithm)SHA1.Create() : SHA256.Create())
                    {
                        using (var output = new FileStream(part, FileMode.Create, FileAccess.Write, FileShare.None, 1 << 16, true))
                        {
                            if (uri.IsFile)
                            {
                                using (var input = File.OpenRead(uri.LocalPath))
                                    counted = await Copy(input, output, hash, onBytes, ct).ConfigureAwait(false);
                            }
                            else
                            {
                                using (var r = await http.GetAsync(uri, HttpCompletionOption.ResponseHeadersRead, ct).ConfigureAwait(false))
                                {
                                    r.EnsureSuccessStatusCode();
                                    Check(r.RequestMessage.RequestUri.ToString());
                                    using (var input = await r.Content.ReadAsStreamAsync().ConfigureAwait(false))
                                        counted = await Copy(input, output, hash, onBytes, ct).ConfigureAwait(false);
                                }
                            }
                        }
                        hash.TransformFinalBlock(new byte[0], 0, 0);
                        string got = Hex(hash.Hash), want = (string.IsNullOrEmpty(sha256) ? sha1 : sha256).ToLowerInvariant();
                        if (got != want) throw new HashMismatchException(Path.GetFileName(dest));
                    }
                    if (File.Exists(dest)) File.Delete(dest);
                    File.Move(part, dest);
                    return;
                }
                catch (Exception e) when (attempt < 3 && !ct.IsCancellationRequested && (e is IOException || e is HttpRequestException
                                                                                           || e is HashMismatchException || e is TaskCanceledException))
                {
                    onBytes?.Invoke(-counted);
                    Log.Warn($"Повторюю завантаження {Path.GetFileName(dest)} ({attempt}/3): {e.Message}");
                    TryDelete(part);
                    await Task.Delay(1500 * attempt, ct).ConfigureAwait(false);
                }
                catch
                {
                    onBytes?.Invoke(-counted);
                    TryDelete(part);
                    throw;
                }
            }
        }

        private static async Task<long> Copy(Stream input, Stream output, HashAlgorithm hash, Action<long> onBytes, CancellationToken ct)
        {
            var buffer = new byte[1 << 16];
            long total = 0;
            int n;
            while ((n = await input.ReadAsync(buffer, 0, buffer.Length, ct).ConfigureAwait(false)) > 0)
            {
                hash.TransformBlock(buffer, 0, n, null, 0);
                await output.WriteAsync(buffer, 0, n, ct).ConfigureAwait(false);
                total += n;
                onBytes?.Invoke(n);
            }
            return total;
        }

        public static string Sha1Of(string path)
        {
            using (var s = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read, 1 << 16))
            using (var h = SHA1.Create())
                return Hex(h.ComputeHash(s));
        }

        public static string Sha256Of(string path)
        {
            using (var s = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read, 1 << 16))
            using (var h = SHA256.Create())
                return Hex(h.ComputeHash(s));
        }

        public static string Hex(byte[] bytes) => string.Concat(bytes.Select(b => b.ToString("x2")));

        private static void TryDelete(string path)
        {
            try
            {
                if (File.Exists(path)) File.Delete(path);
            }
            catch (IOException)
            {
            }
        }

        public void Dispose() => http.Dispose();
    }
}
