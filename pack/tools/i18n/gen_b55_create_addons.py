"""Ukrainian for the Create addons added on 20.11: Create Deco (411 names built from a few parts, so they are composed
here with the right gender and case), Create: Connected, Slice & Dice and Create: Central Kitchen (their missing lines).

Terms follow the pack's Create and vanilla Minecraft: "Андезитовий стоп", "Латунна рамка" (Placard), "Промислове
залізне вікно", "Моховита кам'яна цегла", "Тріснута…", "Плита з … цегли", "Сходи з …", "Стіна з …".

  python gen_b55_create_addons.py      -> pack/i18n/uk/<ns>.json (then i18n.py build)
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
I18N = os.path.join(HERE, "..", "..", "i18n")

# material adjective: masculine, feminine, neuter, plural, genitive plural; "of" form for ingots, sheets and nuggets
MAT = {
    "Andesite": ("андезитовий", "андезитова", "андезитове", "андезитові", "андезитових", "андезиту"),
    "Brass": ("латунний", "латунна", "латунне", "латунні", "латунних", "латуні"),
    "Copper": ("мідний", "мідна", "мідне", "мідні", "мідних", "міді"),
    "Iron": ("залізний", "залізна", "залізне", "залізні", "залізних", "заліза"),
    "Industrial Iron": ("промисловий залізний", "промислова залізна", "промислове залізне", "промислові залізні",
                        "промислових залізних", "промислового заліза"),
    "Zinc": ("цинковий", "цинкова", "цинкове", "цинкові", "цинкових", "цинку"),
    "Gold": ("золотий", "золота", "золоте", "золоті", "золотих", "золота"),
    "Netherite": ("незеритовий", "незеритова", "незеритове", "незеритові", "незеритових", "незериту"),
}
# object nouns: (gender m/f/n/p, noun after the adjective)
OBJ = {
    "Bars": ("p", "ґрати"), "Bars Overlay": ("p", "накладні ґрати"), "Catwalk": ("m", "місток"),
    "Door": ("p", "двері"), "Mesh Fence": ("m", "сітчастий паркан"), "Sheet Metal": ("m", "листовий метал"),
    "Support": ("f", "опора"), "Support Wedge": ("m", "клин опори"), "Train Hull": ("f", "обшивка потяга"),
    "Trapdoor": ("m", "люк"), "Window": ("n", "вікно"), "Window Pane": ("f", "шибка"), "Ladder": ("f", "драбина"),
    "Coin": ("f", "монета"),
}
GENDER = {"m": 0, "f": 1, "n": 2, "p": 3}

# colour adjectives: feminine, masculine (Minecraft's dye names)
COLOR = {
    "White": ("біла", "білий"), "Orange": ("помаранчева", "помаранчевий"), "Magenta": ("пурпурова", "пурпуровий"),
    "Light Blue": ("блакитна", "блакитний"), "Yellow": ("жовта", "жовтий"), "Lime": ("лаймова", "лаймовий"),
    "Pink": ("рожева", "рожевий"), "Gray": ("сіра", "сірий"), "Light Gray": ("світло-сіра", "світло-сірий"),
    "Cyan": ("бірюзова", "бірюзовий"), "Purple": ("фіолетова", "фіолетовий"), "Blue": ("синя", "синій"),
    "Brown": ("коричнева", "коричневий"), "Green": ("зелена", "зелений"), "Red": ("червона", "червоний"),
    "Black": ("чорна", "чорний"),
}

# Create Deco's eight brick kinds: feminine nominative, feminine genitive ("цегла" is feminine)
BRICK = {
    "Blue": ("синя", "синьої"), "Dean": ("бежева", "бежевої"), "Dusk": ("сутінкова", "сутінкової"),
    "Pearl": ("перлинна", "перлинної"), "Red": ("червона", "червоної"), "Scarlet": ("багряна", "багряної"),
    "Umber": ("умброва", "умбрової"), "Verdant": ("зелена", "зеленої"),
}
SHAPE = {
    "Corner": ("кутова", "кутової"), "Cracked": ("тріснута", "тріснутої"), "Long": ("довга", "довгої"),
    "Mossy": ("моховита", "моховитої"), "Short": ("коротка", "короткої"), "Tiled": ("плиткова", "плиткової"),
}

DECAL = {
    "Creeper": "Кріпер", "Cross": "Хрест", "Electrical": "Струм", "Fire": "Вогонь", "Fire Diamond": "Ромб небезпеки",
    "Flow": "Потік", "Fluid": "Рідина", "Ice": "Лід", "No Entry": "Вхід заборонено", "Radioactive": "Радіація",
    "Skull": "Череп", "Warning": "Увага", "Up Arrow": "Стрілка вгору", "Down Arrow": "Стрілка вниз",
    "Left Arrow": "Стрілка ліворуч", "Right Arrow": "Стрілка праворуч", "Up Left Arrow": "Стрілка вгору-ліворуч",
    "Up Right Arrow": "Стрілка вгору-праворуч", "Down Left Arrow": "Стрілка вниз-ліворуч",
    "Down Right Arrow": "Стрілка вниз-праворуч",
}

FIXED = {
    "Create Deco Bricks": "Create Deco: цегла", "Create Deco Props": "Create Deco: оздоблення",
    "Andesite Alloy Sheet": "Лист андезитового стопу", "Industrial Iron Sheet": "Лист промислового заліза",
    "Zinc Sheet": "Цинковий лист", "Industrial Iron Ingot": "Злиток промислового заліза",
    "Industrial Iron Nugget": "Самородок промислового заліза", "Netherite Nugget": "Незеритовий самородок",
}

MATS = sorted(MAT, key=len, reverse=True)
COLORS = sorted(COLOR, key=len, reverse=True)
OBJS = sorted(OBJ, key=len, reverse=True)


def cap(s):
    return s[:1].upper() + s[1:]


def material_object(text):
    """'Brass Catwalk Railing', 'Locked Zinc Door', 'Green Iron Cage Lamp', 'Gold Coin Stack', 'ZincCoin Stack Block'"""
    m = re.fullmatch(r"(.+?) ?Coin Stack Block", text)
    if m and m[1] in MAT:
        return f"Блок стосу {MAT[m[1]][4]} монет"
    m = re.fullmatch(r"(.+) Coin Stack", text)
    if m and m[1] in MAT:
        return f"Стос {MAT[m[1]][4]} монет"
    m = re.fullmatch(r"(.+) Catwalk (Railing|Stairs)", text)
    if m and m[1] in MAT:
        gen = " ".join(w[:-2] + "ого" for w in MAT[m[1]][0].split())  # masculine genitive: андезитового, промислового залізного
        return ("Поручні " if m[2] == "Railing" else "Сходи ") + gen + " містка"
    m = re.fullmatch(r"Locked (.+) Door", text)
    if m and m[1] in MAT:
        return f"Замкнені {MAT[m[1]][3]} двері"
    for c in COLORS:
        if text.startswith(c + " ") and text.endswith(" Cage Lamp"):
            mat = text[len(c) + 1:-len(" Cage Lamp")]
            if mat in MAT:
                return cap(f"{COLOR[c][0]} {MAT[mat][1]} лампа в клітці")
    for mat in MATS:
        if text.startswith(mat + " "):
            rest = text[len(mat) + 1:]
            if rest in OBJ:
                g, noun = OBJ[rest]
                return cap(f"{MAT[mat][GENDER[g]]} {noun}")
    return None


def bricks(text):
    """'Mossy Dusk Brick Stairs' -> 'Сходи з моховитої сутінкової цегли'"""
    m = re.fullmatch(r"(?:(\w+) )?(\w+) (Bricks|Brick Slab|Brick Stairs|Brick Wall)", text)
    if not m or m[2] not in BRICK or (m[1] and m[1] not in SHAPE):
        return None
    nom = " ".join(x for x in ((SHAPE[m[1]][0] if m[1] else ""), BRICK[m[2]][0]) if x)
    gen = " ".join(x for x in ((SHAPE[m[1]][1] if m[1] else ""), BRICK[m[2]][1]) if x)
    z = "із" if gen[0] in "зсцчшж" else "з"  # милозвучність: «із зеленої», «з синьої»
    return {"Bricks": cap(f"{nom} цегла"), "Brick Slab": f"Плита {z} {gen} цегли", "Brick Stairs": f"Сходи {z} {gen} цегли",
            "Brick Wall": f"Стіна {z} {gen} цегли"}[m[3]]


def other(text):
    if text in FIXED:
        return FIXED[text]
    m = re.fullmatch(r"(.+) Decal", text)
    if m and m[1] in DECAL:
        return f"Наліпка «{DECAL[m[1]]}»"
    for c in COLORS:
        if text == c + " Placard":
            return cap(f"{COLOR[c][0]} латунна рамка")
        if text == c + " Shipping Container":
            return cap(f"{COLOR[c][1]} вантажний контейнер")
    return None


def createdeco(src):
    out, missing = {}, []
    for key, text in src.items():
        uk = other(text) or bricks(text) or material_object(text)
        if uk:
            out[key] = uk
        else:
            missing.append(text)
    if missing:
        raise SystemExit("createdeco: no rule for " + ", ".join(sorted(set(missing))))
    return out


SMALL = {
    "create_connected": {
        "block.create_connected.brass_gearbox.tooltip.summary": "Редуктор, у якому напрям обертання кожної з 4 сторін _окремо налаштовується ключем_.",
        "create_connected.battery.consumption": "Витрата заряду:",
        "create_connected.battery.powering_belts": "живлення стрічок",
        "create_connected.ponder.kinetic_battery.text_3": "Отримавши сигнал редстоуну, вона розряджається й створює певну потужність навантаження на сталій швидкості",
        "create_connected.ponder.kinetic_battery.text_4": "Але скільки заряду витрачається, залежить від того, скільки потужності насправді використовується",
        "create_connected.ponder.kinetic_battery.text_5": "Коли інші кінетичні джерела дають достатньо потужності, батареї не розряджаються",
        "create_connected.ponder.kinetic_battery.text_6": "Навіть якщо мережа нічого не навантажує, батареї витрачають крихту заряду, щоб рухати стрічки з предметами",
        "create_connected.ponder.kinetic_battery.text_9": "Або покажіть рівень заряду через передавач даних",
        "create_connected.ponder.kinetic_battery_automation.text_1": "У вигляді предмета батарея зберігає свій заряд",
        "create_connected.ponder.kinetic_battery_automation.text_3": "Коли батарея заряджається, клацніть по ній ПКМ іншою батареєю, щоб розрядити",
        "create_connected.ponder.kinetic_battery_chaining.text_5": "Коли одна батарея отримує сигнал редстоуну, живлення отримують усі батареї ланцюжка",
        "item.create_connected.charged_kinetic_battery.tooltip.summary": "Застаріле. Перетворіть на звичайну кінетичну батарею крафтом.",
    },
    "sliceanddice": {
        "sliceanddice.recipe.assembly.slicer": "Наріжте різаком",
        "sliceanddice.recipe.slicer": "Різак",
    },
    "create_central_kitchen": {
        "create_central_kitchen.ponder.cultural_delights_portioning.header": "Автоматизація пармеджано з баклажанами",
        "create_central_kitchen.ponder.cultural_delights_portioning.text_1": "Механічні руки беруть порції на тарілках без мисок",
        "create_central_kitchen.ponder.cultural_delights_portioning.text_2": "Остання порція лишається, доки її не замінить нова ціла страва",
        "create_central_kitchen.ponder.festive_delight_chicken.header": "Автоматична (майже) святкова курка",
        "create_central_kitchen.ponder.festive_delight_chicken.text_1": "Механічні руки беруть чотири порції на тарілках і лишають залишки",
        "create_central_kitchen.ponder.group.dragon_leg": "Драконяча ніжка",
        "create_central_kitchen.ponder.group.eggplant_parmesan": "Страви пармеджано з баклажанами",
        "create_central_kitchen.ponder.group.festive_chicken": "Святкова курка",
        "create_central_kitchen.ponder.group.pancakes": "Стоси млинців",
        "create_central_kitchen.ponder.group.portionable_foods": "Страви й пироги",
        "create_central_kitchen.ponder.hearth_and_harvest_cask.header": "Автоматизація діжок Hearth and Harvest",
        "create_central_kitchen.ponder.hearth_and_harvest_cask.text_1": "Пакунки приймаються, лише коли в них рівно один повний рецепт діжки",
        "create_central_kitchen.ponder.hearth_and_harvest_cask.text_2": "Панель рецепта просить одну сіль і три окремі шматки сирого м'яса.",
        "create_central_kitchen.ponder.hearth_and_harvest_cask.text_3": "Пакунок розпаковується цілком, і кожен повторений інгредієнт займає свою комірку",
        "create_central_kitchen.ponder.hearth_and_harvest_cask.text_4": "Далі діжка витримує як звичайно — 1200 тіків — за своїми правилами",
        "create_central_kitchen.ponder.hearth_and_harvest_cask.text_5": "Механічні руки не чіпають інгредієнтів, що витримуються, і забирають лише готовий результат",
        "create_central_kitchen.ponder.rustic_delight_pancakes.header": "Автоматизація стосів млинців",
        "create_central_kitchen.ponder.rustic_delight_pancakes.text_1": "Механічні руки можуть брати млинці з будь-якого стосу з начинкою",
        "create_central_kitchen.ponder.rustic_delight_pancakes.text_2": "Додаються лише такі самі млинці, до стосу з дванадцяти",
    },
}


def main():
    for ns in ("createdeco", "create_connected", "sliceanddice", "create_central_kitchen"):
        src = json.load(open(os.path.join(I18N, "src", f"{ns}.json"), encoding="utf-8"))
        out = createdeco(src) if ns == "createdeco" else {k: v for k, v in SMALL[ns].items() if k in src}
        lost = sorted(set(src) - set(out))
        if lost:
            raise SystemExit(f"{ns}: untranslated {lost}")
        path = os.path.join(I18N, "uk", f"{ns}.json")
        old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
        old.update(out)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(dict(sorted(old.items())), f, ensure_ascii=False, indent=1)
            f.write("\n")
        print(ns, len(out))


if __name__ == "__main__":
    main()
