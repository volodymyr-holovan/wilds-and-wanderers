"""Copy the lang files of server-only mods into the client side of the pack (kubejs/assets/<ns>/lang).

Server-only mods (FTB Essentials, GriefLogger, SkinRestorer...) send translatable chat messages; the client can only
show them if it has the texts. en_us is the fallback for every language, uk_ua comes from the mod when it ships one
(our own Ukrainian translation is written by i18n.py build into the same folder and wins over this copy).

  python server_only_langs.py <server mods dir> <client mods dir>
"""
import io, os, re, sys, zipfile

PACK = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(PACK, "overrides", "kubejs", "assets")
UK_BUILT = os.path.join(PACK, "i18n", "uk")

server, client = sys.argv[1], sys.argv[2]
for jar in sorted(set(os.listdir(server)) - set(os.listdir(client))):
    if not jar.endswith(".jar"):
        continue
    with zipfile.ZipFile(os.path.join(server, jar)) as z:
        for n in z.namelist():
            m = re.match(r"assets/([^/]+)/lang/(en_us|uk_ua)\.json$", n)
            if not m:
                continue
            ns, lang = m.groups()
            if lang == "uk_ua" and os.path.exists(os.path.join(UK_BUILT, ns + ".json")):
                continue  # our translation is built by i18n.py
            d = os.path.join(OUT, ns, "lang")
            os.makedirs(d, exist_ok=True)
            open(os.path.join(d, lang + ".json"), "wb").write(z.read(n))
            print(f"{jar}: {ns}/{lang}")
