"""Batch 45: Mekanism settings screens. Hand translations live in pack/i18n/mek/*.tsv (key with "~" for
"configuration.mekanism.", TAB, text); the repetitive sections (tiers, energy storage and usage of every machine, ore veins)
are built here from templates and from the Ukrainian block names Mekanism ships."""
import glob, json, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
I18N = os.path.join(HERE, "..", "..", "i18n")
OUT = os.path.join(I18N, "batches", "b45_mekanism.json")
P = "configuration.mekanism."

src = json.load(open(os.path.join(I18N, "src", "mekanism.json"), encoding="utf-8"))
ref = json.load(open(os.path.join(I18N, "ref", "mekanism.json"), encoding="utf-8"))
done = json.load(open(os.path.join(I18N, "uk", "mekanism.json"), encoding="utf-8"))
names = {**ref, **done}
T = {}
for f in sorted(glob.glob(os.path.join(I18N, "mek", "*.tsv"))):
    for line in open(f, encoding="utf-8"):
        line = line.rstrip("\n")
        if line:
            k, v = line.split("\t", 1)
            T[k.replace("~", P, 1) if k.startswith("~") else k] = v.replace("\\n", "\n")

TIER = {"basic": "Базовий", "advanced": "Покращений", "elite": "Елітний", "ultimate": "Найкращий", "creative": "Творчий"}
TIER_PL = {"basic": "базових", "advanced": "покращених", "elite": "елітних", "ultimate": "найкращих", "creative": "творчих"}
GROUP = {  # tier group -> (title, settings text, button)
    "bin": ("Сховища", "Налаштування сховищ", "Налаштувати сховища"),
    "chemical_tank": ("Баки хімікатів", "Налаштування баків хімікатів", "Налаштувати баки хімікатів"),
    "energy_cube": ("Енергокуби", "Налаштування енергокубів", "Налаштувати енергокуби"),
    "fluid_tank": ("Баки рідин", "Налаштування баків рідин", "Налаштувати баки рідин"),
    "induction": ("Індукційна матриця", "Налаштування індукційних комірок і постачальників", "Налаштувати матрицю"),
    "transmitter": ("Передавачі", "Налаштування передавачів", "Налаштувати передавачі"),
    "transmitter.chemical": ("Трубки під тиском", "Налаштування трубок під тиском", "Налаштувати трубки"),
    "transmitter.energy": ("Універсальні кабелі", "Налаштування універсальних кабелів", "Налаштувати кабелі"),
    "transmitter.fluid": ("Механічні труби", "Налаштування механічних труб", "Налаштувати труби"),
    "transmitter.heat": ("Термодинамічні провідники", "Налаштування термодинамічних провідників", "Налаштувати провідники"),
    "transmitter.item": ("Логістичні транспортери", "Налаштування логістичних транспортерів", "Налаштувати транспортери"),
}
STAT = {"storage": "місткість", "rate": "швидкість виходу", "pull": "швидкість прокачування", "capacity": "теплоємність",
        "insulation": "ізоляція", "conduction": "зворотна провідність", "speed": "швидкість перенесення"}
TIP = [  # English template with {t} = tier -> Ukrainian with {t} = plural tier adjective
    (r"Capacity of (\w+) mechanical pipes in mB\.", "Місткість {t} механічних труб у мБ."),
    (r"Capacity of (\w+) pressurized tubes in mB\.", "Місткість {t} трубок під тиском у мБ."),
    (r"Conduction value of (\w+) thermodynamic conductors\.", "Провідність {t} термодинамічних провідників."),
    (r"Five times the travel speed in m/s of (\w+) logistical transporter\. This value assumes a target tick rate of 20 ticks per second\.",
     "П'ятикратна швидкість руху (м/с) {t} логістичних транспортерів. Розраховано на 20 тактів за секунду."),
    (r"Heat capacity of (\w+) thermodynamic conductors\.", "Теплоємність {t} термодинамічних провідників."),
    (r"Insulation value of (\w+) thermodynamic conductor\.", "Ізоляція {t} термодинамічних провідників."),
    (r"Internal buffer in Joules of each (\w+) universal cable\.", "Внутрішній буфер (Дж) кожного з {t} універсальних кабелів."),
    (r"Item throughput rate of (\w+) logistical transporters in items/half second\. This value assumes a target tick rate of 20 ticks per second\.",
     "Пропускна здатність {t} логістичних транспортерів (предметів за пів секунди). Розраховано на 20 тактів за секунду."),
    (r"Maximum number of Joules (\w+) energy cubes can store\.", "Скільки Дж найбільше вміщують енергокуби ({t})."),
    (r"Maximum number of Joules (\w+) induction cells can store\.", "Скільки Дж найбільше вміщують індукційні комірки ({t})."),
    (r"Maximum number of Joules (\w+) induction providers can output or accept\.", "Скільки Дж найбільше віддають чи приймають індукційні постачальники ({t})."),
    (r"Output rate in Joules of (\w+) energy cubes\.", "Швидкість виходу (Дж) енергокубів ({t})."),
    (r"Output rate of (\w+) chemical tanks in mB\.", "Швидкість виходу баків хімікатів ({t}) у мБ."),
    (r"Output rate of (\w+) fluid tanks in mB\.", "Швидкість виходу баків рідин ({t}) у мБ."),
    (r"Pump rate of (\w+) mechanical pipes in mB/t\.", "Швидкість прокачування {t} механічних труб (мБ/т)."),
    (r"Pump rate of (\w+) pressurized tubes in mB/t\.", "Швидкість прокачування {t} трубок під тиском (мБ/т)."),
    (r"Storage size of (\w+) chemical tanks in mB\.", "Місткість баків хімікатів ({t}) у мБ."),
    (r"Storage size of (\w+) fluid tanks in mB\.", "Місткість баків рідин ({t}) у мБ."),
    (r"The number of items (\w+) bins can store\.", "Скільки предметів уміщують сховища ({t})."),
]
LABEL = {"Heat Capacity": "теплоємність", "Insulation": "ізоляція", "Inverse Conduction": "зворотна провідність",
         "Output Rate": "швидкість виходу", "Pull Rate": "швидкість прокачування", "Storage": "місткість", "Transfer Speed": "швидкість перенесення"}

ORE = {"osmium": "осмію", "tin": "олова", "uranium": "урану", "fluorite": "флюориту", "lead": "свинцю"}
ORE_TITLE = {"osmium": "Осмій", "tin": "Олово", "uranium": "Уран", "fluorite": "Флюорит", "lead": "Свинець"}
VEIN = {"small": "мала", "middle": "середня", "upper": "верхня", "large": "велика", "normal": "звичайна", "buried": "похована"}


def machine(mid):
    return names.get(f"block.mekanism.{mid}") or names.get(f"item.mekanism.{mid}")


for k, en in src.items():
    if k in done or k in T or not k.startswith(P):
        continue
    sub = k[len(P):]
    m = re.match(r"tier\.(.+?)(?:\.(basic|advanced|elite|ultimate|creative)\.(\w+))?(\.tooltip|\.button)?$", sub)
    if m:
        grp, tier, stat, suf = m.groups()
        if tier is None and grp in GROUP:
            title, tip, btn = GROUP[grp]
            T[k] = {None: title, ".tooltip": tip, ".button": btn}[suf]
            continue
        if tier and suf is None:
            label = re.sub(r"^(Basic|Advanced|Elite|Ultimate|Creative) ", "", en)
            if label in LABEL:
                T[k] = f"{TIER[tier]}: {LABEL[label]}"
                continue
        if tier and suf == ".tooltip":
            for pat, ua in TIP:
                if re.fullmatch(pat, en):
                    T[k] = ua.replace("{t}", TIER_PL[tier])
                    break
            if k in T:
                continue
    m = re.match(r"(storage|usage)\.(\w+)\.energy(\.tooltip)?$", sub)
    if m and machine(m[2]):
        kind, mid, tip = m.groups()
        name = machine(mid)
        if kind == "storage":
            T[k] = (f"Базовий запас енергії (Дж): {name}" + (". Також визначає найбільшу швидкість." if "Also defines" in en else "")) if tip else f"Запас енергії: {name}"
        else:
            T[k] = f"Енергія (Дж) на одну операцію: {name}" if tip else f"Витрата енергії: {name}"
        continue
    m = re.match(r"world\.(osmium|tin|uranium|fluorite|lead)\.(?:(small|middle|upper|large|normal|buried)\.)?(\w+)(\.tooltip|\.button)?$", sub)
    if m:
        ore, vein, field, suf = m.groups()
        o, ttl = ORE[ore], ORE_TITLE[ore]
        vn = f"{VEIN[vein]} жила {o}" if vein else ""
        VN = vn[:1].upper() + vn[1:]
        if field == "top_level":
            T[k] = (f"Налаштувати: {ttl}" if suf == ".button" else f"Генерація руди {o}" if suf == ".tooltip" else f"Генерація: {ttl}") if not vein \
                else (f"Налаштувати: {VN}" if suf == ".button" else f"Генерація: {VN}" if suf == ".tooltip" else VN)
        elif field == "generate":
            T[k] = "Генерувати" if not suf else (f"Чи додавати руду {o} у генерацію світу." if not vein else
                                                 f"Чи генерувати: {VN}. Примітка: потрібна увімкнена генерація руди {o}.")
        elif field == "discard_chance":
            T[k] = "Шанс відкидання" if not suf else f"Шанс, що блоки, відкриті повітрю, не з'являться ({VN})."
        elif field in ("max", "min"):
            word = "Верхній" if field == "max" else "Нижній"
            if suf == ".button":
                T[k] = f"Змінити: {word.lower()} якір"
            elif suf == ".tooltip":
                T[k] = f"{word} (включно) якір висоти ({VN}): {'найвищий' if field == 'max' else 'найнижчий'} рівень Y, де вона з'являється."
            else:
                T[k] = f"{word} якір"
        elif field == "max_size":
            T[k] = "Найбільший розмір" if not suf else f"Найбільше блоків у жилі ({VN})."
        elif field == "per_chunk":
            T[k] = "На чанк" if not suf else f"Шанс, що в чанку з'явиться {vn}."
        elif field == "shape":
            T[k] = "Форма розподілу" if not suf else f"Форма розподілу ({VN})."
        if k in T:
            continue

missing = [k for k in src if k not in done and k not in T]
if missing:
    print("untranslated", len(missing))
    for k in missing:
        print(" ", k.replace(P, "~"), "|", src[k])
    raise SystemExit(1)
json.dump({"mekanism": {k: v for k, v in T.items() if k in src and k not in done}}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("written", sum(1 for k in T if k in src and k not in done))
