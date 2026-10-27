using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Web.Script.Serialization;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// JSON with what .NET Framework has built in (no extra libraries next to the exe): objects are
    /// Dictionary&lt;string, object&gt;, arrays object[], numbers int/long/decimal. Written back indented, keys in their order.
    /// </summary>
    public static class Json
    {
        private static JavaScriptSerializer Serializer() => new JavaScriptSerializer { MaxJsonLength = int.MaxValue, RecursionLimit = 256 };

        public static object Parse(string text) => Serializer().DeserializeObject(text);

        public static Dictionary<string, object> ParseObject(string text) =>
            Parse(text) as Dictionary<string, object> ?? throw new FormatException("JSON: очікувався об'єкт");

        public static Dictionary<string, object> Obj(object o) => o as Dictionary<string, object>;

        public static Dictionary<string, object> Obj(Dictionary<string, object> d, string key) =>
            d != null && d.TryGetValue(key, out object v) ? v as Dictionary<string, object> : null;

        public static IEnumerable<object> Arr(Dictionary<string, object> d, string key) =>
            d != null && d.TryGetValue(key, out object v) && v is IEnumerable e && !(v is string) ? e.Cast<object>() : Enumerable.Empty<object>();

        public static string Str(Dictionary<string, object> d, string key, string fallback = null) =>
            d != null && d.TryGetValue(key, out object v) && v != null ? Convert.ToString(v, CultureInfo.InvariantCulture) : fallback;

        public static long Long(Dictionary<string, object> d, string key, long fallback = 0) =>
            d != null && d.TryGetValue(key, out object v) && v != null && !(v is string) ? Convert.ToInt64(v, CultureInfo.InvariantCulture) : fallback;

        public static bool Bool(Dictionary<string, object> d, string key, bool fallback = false) =>
            d != null && d.TryGetValue(key, out object v) && v is bool b ? b : fallback;

        public static string Write(object value)
        {
            var sb = new StringBuilder();
            WriteValue(sb, value, 0);
            sb.Append('\n');
            return sb.ToString();
        }

        private static void WriteValue(StringBuilder sb, object v, int depth)
        {
            switch (v)
            {
                case null:
                    sb.Append("null");
                    break;
                case string s:
                    WriteString(sb, s);
                    break;
                case bool b:
                    sb.Append(b ? "true" : "false");
                    break;
                case IDictionary<string, object> d:
                    if (d.Count == 0)
                    {
                        sb.Append("{}");
                        break;
                    }
                    sb.Append("{\n");
                    int i = 0;
                    foreach (var kv in d)
                    {
                        Indent(sb, depth + 1);
                        WriteString(sb, kv.Key);
                        sb.Append(": ");
                        WriteValue(sb, kv.Value, depth + 1);
                        sb.Append(++i < d.Count ? ",\n" : "\n");
                    }
                    Indent(sb, depth);
                    sb.Append('}');
                    break;
                case IEnumerable e:
                    var items = e.Cast<object>().ToList();
                    if (items.Count == 0)
                    {
                        sb.Append("[]");
                        break;
                    }
                    sb.Append("[\n");
                    for (int k = 0; k < items.Count; k++)
                    {
                        Indent(sb, depth + 1);
                        WriteValue(sb, items[k], depth + 1);
                        sb.Append(k + 1 < items.Count ? ",\n" : "\n");
                    }
                    Indent(sb, depth);
                    sb.Append(']');
                    break;
                case IFormattable f:
                    sb.Append(f.ToString(null, CultureInfo.InvariantCulture));
                    break;
                default:
                    WriteString(sb, v.ToString());
                    break;
            }
        }

        private static void Indent(StringBuilder sb, int depth) => sb.Append(' ', depth * 2);

        private static void WriteString(StringBuilder sb, string s)
        {
            sb.Append('"');
            foreach (char c in s)
            {
                switch (c)
                {
                    case '"': sb.Append("\\\""); break;
                    case '\\': sb.Append("\\\\"); break;
                    case '\n': sb.Append("\\n"); break;
                    case '\r': sb.Append("\\r"); break;
                    case '\t': sb.Append("\\t"); break;
                    default:
                        if (c < 0x20) sb.Append("\\u").Append(((int)c).ToString("x4"));
                        else sb.Append(c);
                        break;
                }
            }
            sb.Append('"');
        }
    }
}
