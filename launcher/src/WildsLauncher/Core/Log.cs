using System;
using System.IO;
using System.Text;

namespace Wilds.Launcher.Core
{
    /// <summary>A plain text log next to the program's data; with --headless it is also printed to the console.</summary>
    public static class Log
    {
        private static readonly object Gate = new object();
        private static string file;

        public static bool ToConsole { get; set; }

        public static string FilePath => file;

        public static void Init(string dir)
        {
            Directory.CreateDirectory(dir);
            file = Path.Combine(dir, "launcher.log");
            try
            {
                // one previous session is kept for bug reports, older ones go
                if (File.Exists(file) && new FileInfo(file).Length > 2 * 1024 * 1024)
                {
                    string old = Path.Combine(dir, "launcher.old.log");
                    if (File.Exists(old)) File.Delete(old);
                    File.Move(file, old);
                }
            }
            catch (IOException)
            {
            }
        }

        public static void Info(string message) => Write("INFO", message);

        public static void Warn(string message) => Write("WARN", message);

        public static void Error(string message, Exception e = null) => Write("ERROR", e == null ? message : message + Environment.NewLine + e);

        private static void Write(string level, string message)
        {
            string line = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] [{level}] {message}";
            lock (Gate)
            {
                if (ToConsole) Console.WriteLine(line);
                if (file == null) return;
                try
                {
                    File.AppendAllText(file, line + Environment.NewLine, Encoding.UTF8);
                }
                catch (IOException)
                {
                }
            }
        }
    }
}
