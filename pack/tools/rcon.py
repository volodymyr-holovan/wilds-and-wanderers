"""Minimal RCON client: rcon.py HOST PORT PASSWORD "command" ["command" ...]

Long responses arrive split into several packets, so after each command all packets are collected
until the socket stays quiet for a moment. (Vanilla RCON drops the connection on some tricks such as an
extra marker packet, so only plain command packets are sent.) Each command is retried on a fresh
connection if the server closes it.
"""
import socket, struct, sys, time


def packet(rid, ptype, body):
    data = struct.pack("<ii", rid, ptype) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(data)) + data


def recv_exact(sock, n):
    data = b""
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise ConnectionError("connection closed")
        data += chunk
    return data


def recv(sock):
    size = struct.unpack("<i", recv_exact(sock, 4))[0]
    data = recv_exact(sock, size)
    rid, ptype = struct.unpack("<ii", data[:8])
    return rid, data[8:-2]


def run(host, port, pw, cmd):
    s = socket.create_connection((host, port), timeout=60)
    try:
        s.sendall(packet(1, 3, pw))
        if recv(s)[0] == -1:
            sys.exit("auth failed")
        s.sendall(packet(2, 2, cmd))
        body = recv(s)[1]            # the first part may take long (slow commands)
        s.settimeout(0.4)
        try:
            while True:
                body += recv(s)[1]
        except (socket.timeout, TimeoutError):
            pass
        return body
    finally:
        s.close()


host, port, pw = sys.argv[1], int(sys.argv[2]), sys.argv[3]
for cmd in sys.argv[4:]:
    for attempt in range(3):
        try:
            body = run(host, port, pw, cmd)
            break
        except (ConnectionError, OSError) as e:
            body = f"[rcon error: {e}]".encode()
            time.sleep(1)
    sys.stdout.buffer.write(f"> {cmd}\n".encode() + body + b"\n")
