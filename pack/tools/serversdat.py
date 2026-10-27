"""Write a minimal uncompressed NBT servers.dat with one server entry."""
import struct, sys


def tag_string(name, value):
    n, v = name.encode(), value.encode()
    return b"\x08" + struct.pack(">H", len(n)) + n + struct.pack(">H", len(v)) + v


def tag_byte(name, value):
    n = name.encode()
    return b"\x01" + struct.pack(">H", len(n)) + n + struct.pack(">b", value)


def servers_dat(entries):
    body = b""
    for name, ip in entries:
        # hidden=0, acceptTextures=1 so the server resource pack (Polymer etc.) is accepted
        body += tag_string("name", name) + tag_string("ip", ip) + tag_byte("hidden", 0) + tag_byte("acceptTextures", 1) + b"\x00"
    lst = b"\x09" + struct.pack(">H", 7) + b"servers" + b"\x0a" + struct.pack(">i", len(entries)) + body
    return b"\x0a" + struct.pack(">H", 0) + lst + b"\x00"


if __name__ == "__main__":
    # usage: serversdat.py OUT NAME1 IP1 [NAME2 IP2 ...]
    out, rest = sys.argv[1], sys.argv[2:]
    open(out, "wb").write(servers_dat(list(zip(rest[::2], rest[1::2]))))
