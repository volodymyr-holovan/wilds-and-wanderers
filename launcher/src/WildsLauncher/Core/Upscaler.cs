using System.IO;
using System.Text;
using System.Text.RegularExpressions;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// DLSS / FSR / XeSS through the Super Resolution mod, installed only when the player ticks it. The mod needs its own
    /// OpenGL 4.3+ context, so NeoForge's early loading window must not create one: on its first start the mod switches
    /// earlyWindowControl off in config/fml.toml and closes the game asking for a restart. The program does that before
    /// the game starts, so the first start just works; unticked, the NeoForge default (on) comes back.
    /// </summary>
    public static class Upscaler
    {
        public static void ApplyEarlyWindow(Paths p, bool upscaler)
        {
            string file = p.InGame("config/fml.toml");
            string want = upscaler ? "false" : "true";
            try
            {
                if (!File.Exists(file))
                {
                    if (!upscaler) return;   // NeoForge writes its defaults on the first start
                    Directory.CreateDirectory(Path.GetDirectoryName(file));
                    Files.WriteAtomic(file, "earlyWindowControl = false\n");
                    return;
                }
                string text = File.ReadAllText(file, Encoding.UTF8);
                var re = new Regex(@"(?m)^(\s*earlyWindowControl\s*=\s*)(true|false)\s*$");
                string next = re.IsMatch(text) ? re.Replace(text, "${1}" + want) : text.TrimEnd() + "\nearlyWindowControl = " + want + "\n";
                if (next != text) Files.WriteAtomic(file, next);
            }
            catch (IOException e)
            {
                Log.Warn("fml.toml не змінено: " + e.Message);
            }
        }
    }
}
