using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;

namespace Wilds.Launcher.Core
{
    /// <summary>
    /// The game's server list (servers.dat, Minecraft's NBT format). The pack's servers are added at the top when they
    /// are missing; the player's own entries, names and order are kept.
    /// </summary>
    public static class ServersDat
    {
        public static void Ensure(string file, IReadOnlyList<ServerEntry> servers)
        {
            if (servers.Count == 0) return;
            var root = File.Exists(file) ? Read(file) : new Nbt.Compound();
            if (!(root.Get("servers") is Nbt.List list) || (list.ElementType != Nbt.TagCompound && list.Items.Count > 0))
            {
                list = new Nbt.List(Nbt.TagCompound);
                root.Set("servers", list);
            }
            list.ElementType = Nbt.TagCompound;
            var known = new HashSet<string>(list.Items.OfType<Nbt.Compound>().Select(c => Normal(c.Get("ip") as string)), StringComparer.OrdinalIgnoreCase);
            int at = 0;
            foreach (var s in servers)
            {
                if (known.Contains(Normal(s.Address))) continue;
                var entry = new Nbt.Compound();
                entry.Set("name", s.Name);
                entry.Set("ip", s.Address);
                list.Items.Insert(at++, entry);
                Log.Info($"Сервер додано у список: {s.Name} ({s.Address})");
            }
            if (at == 0 && File.Exists(file)) return;
            Directory.CreateDirectory(Path.GetDirectoryName(file));
            string tmp = file + ".tmp";
            using (var w = new BinaryWriter(File.Create(tmp)))
                Nbt.WriteRoot(w, root);
            if (File.Exists(file)) File.Replace(tmp, file, null);
            else File.Move(tmp, file);
        }

        private static string Normal(string address)
        {
            address = (address ?? "").Trim().ToLowerInvariant();
            return address.EndsWith(":25565") ? address.Substring(0, address.Length - 6) : address;
        }

        private static Nbt.Compound Read(string file)
        {
            byte[] bytes = File.ReadAllBytes(file);
            Stream s = new MemoryStream(bytes);
            if (bytes.Length > 2 && bytes[0] == 0x1f && bytes[1] == 0x8b) s = new GZipStream(s, CompressionMode.Decompress);
            using (var r = new BinaryReader(s))
                return Nbt.ReadRoot(r);
        }
    }

    /// <summary>Just enough NBT (big-endian, named tags) to read and write the server list without losing anything.</summary>
    public static class Nbt
    {
        public const byte TagEnd = 0, TagByte = 1, TagShort = 2, TagInt = 3, TagLong = 4, TagFloat = 5, TagDouble = 6, TagByteArray = 7,
            TagString = 8, TagList = 9, TagCompound = 10, TagIntArray = 11, TagLongArray = 12;

        public sealed class Compound
        {
            public readonly List<KeyValuePair<string, object>> Entries = new List<KeyValuePair<string, object>>();

            public object Get(string name) => Entries.FirstOrDefault(e => e.Key == name).Value;

            public void Set(string name, object value)
            {
                int i = Entries.FindIndex(e => e.Key == name);
                if (i >= 0) Entries[i] = new KeyValuePair<string, object>(name, value);
                else Entries.Add(new KeyValuePair<string, object>(name, value));
            }
        }

        public sealed class List
        {
            public byte ElementType;
            public readonly List<object> Items = new List<object>();

            public List(byte type) => ElementType = type;
        }

        public static Compound ReadRoot(BinaryReader r)
        {
            byte type = r.ReadByte();
            if (type != TagCompound) throw new InvalidDataException("servers.dat: несподіваний формат");
            ReadString(r);
            return (Compound)ReadPayload(r, type);
        }

        public static void WriteRoot(BinaryWriter w, Compound root)
        {
            w.Write(TagCompound);
            WriteString(w, "");
            WritePayload(w, TagCompound, root);
        }

        private static object ReadPayload(BinaryReader r, byte type)
        {
            switch (type)
            {
                case TagByte: return r.ReadSByte();
                case TagShort: return BitConverterBE.Int16(r);
                case TagInt: return BitConverterBE.Int32(r);
                case TagLong: return BitConverterBE.Int64(r);
                case TagFloat: return BitConverter.ToSingle(BitConverter.GetBytes(BitConverterBE.Int32(r)), 0);
                case TagDouble: return BitConverter.Int64BitsToDouble(BitConverterBE.Int64(r));
                case TagByteArray: return r.ReadBytes(BitConverterBE.Int32(r));
                case TagString: return ReadString(r);
                case TagList:
                {
                    var list = new List(r.ReadByte());
                    int n = BitConverterBE.Int32(r);
                    for (int i = 0; i < n; i++) list.Items.Add(ReadPayload(r, list.ElementType));
                    return list;
                }
                case TagCompound:
                {
                    var c = new Compound();
                    for (byte t; (t = r.ReadByte()) != TagEnd;)
                    {
                        string name = ReadString(r);
                        c.Entries.Add(new KeyValuePair<string, object>(name, ReadPayload(r, t)));
                    }
                    return c;
                }
                case TagIntArray:
                {
                    var a = new int[BitConverterBE.Int32(r)];
                    for (int i = 0; i < a.Length; i++) a[i] = BitConverterBE.Int32(r);
                    return a;
                }
                case TagLongArray:
                {
                    var a = new long[BitConverterBE.Int32(r)];
                    for (int i = 0; i < a.Length; i++) a[i] = BitConverterBE.Int64(r);
                    return a;
                }
                default: throw new InvalidDataException("servers.dat: невідомий тип " + type);
            }
        }

        private static byte TypeOf(object v)
        {
            switch (v)
            {
                case sbyte _: return TagByte;
                case short _: return TagShort;
                case int _: return TagInt;
                case long _: return TagLong;
                case float _: return TagFloat;
                case double _: return TagDouble;
                case byte[] _: return TagByteArray;
                case string _: return TagString;
                case List _: return TagList;
                case Compound _: return TagCompound;
                case int[] _: return TagIntArray;
                case long[] _: return TagLongArray;
                default: throw new InvalidDataException("NBT: непідтримуване значення " + v?.GetType());
            }
        }

        private static void WritePayload(BinaryWriter w, byte type, object v)
        {
            switch (type)
            {
                case TagByte: w.Write((sbyte)v); break;
                case TagShort: BitConverterBE.Write(w, (short)v); break;
                case TagInt: BitConverterBE.Write(w, (int)v); break;
                case TagLong: BitConverterBE.Write(w, (long)v); break;
                case TagFloat: BitConverterBE.Write(w, BitConverter.ToInt32(BitConverter.GetBytes((float)v), 0)); break;
                case TagDouble: BitConverterBE.Write(w, BitConverter.DoubleToInt64Bits((double)v)); break;
                case TagByteArray:
                    BitConverterBE.Write(w, ((byte[])v).Length);
                    w.Write((byte[])v);
                    break;
                case TagString: WriteString(w, (string)v); break;
                case TagList:
                {
                    var list = (List)v;
                    w.Write(list.Items.Count == 0 ? TagEnd : list.ElementType);
                    BitConverterBE.Write(w, list.Items.Count);
                    foreach (var item in list.Items) WritePayload(w, list.ElementType, item);
                    break;
                }
                case TagCompound:
                    foreach (var e in ((Compound)v).Entries)
                    {
                        byte t = TypeOf(e.Value);
                        w.Write(t);
                        WriteString(w, e.Key);
                        WritePayload(w, t, e.Value);
                    }
                    w.Write(TagEnd);
                    break;
                case TagIntArray:
                    BitConverterBE.Write(w, ((int[])v).Length);
                    foreach (int x in (int[])v) BitConverterBE.Write(w, x);
                    break;
                case TagLongArray:
                    BitConverterBE.Write(w, ((long[])v).Length);
                    foreach (long x in (long[])v) BitConverterBE.Write(w, x);
                    break;
            }
        }

        // NBT strings are "modified UTF-8"; for the names and addresses here (no NUL, no emoji) it is plain UTF-8
        private static string ReadString(BinaryReader r) => Encoding.UTF8.GetString(r.ReadBytes((ushort)BitConverterBE.Int16(r)));

        private static void WriteString(BinaryWriter w, string s)
        {
            byte[] b = Encoding.UTF8.GetBytes(s);
            BitConverterBE.Write(w, (short)b.Length);
            w.Write(b);
        }

        private static class BitConverterBE
        {
            public static short Int16(BinaryReader r) => (short)((r.ReadByte() << 8) | r.ReadByte());

            public static int Int32(BinaryReader r) => (r.ReadByte() << 24) | (r.ReadByte() << 16) | (r.ReadByte() << 8) | r.ReadByte();

            public static long Int64(BinaryReader r) => ((long)(uint)Int32(r) << 32) | (uint)Int32(r);

            public static void Write(BinaryWriter w, short v)
            {
                w.Write((byte)(v >> 8));
                w.Write((byte)v);
            }

            public static void Write(BinaryWriter w, int v)
            {
                w.Write((byte)(v >> 24));
                w.Write((byte)(v >> 16));
                w.Write((byte)(v >> 8));
                w.Write((byte)v);
            }

            public static void Write(BinaryWriter w, long v)
            {
                Write(w, (int)(v >> 32));
                Write(w, (int)v);
            }
        }
    }
}
