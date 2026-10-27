"""Generates i18n/batches/b20_dragonsplus.json: Create: Dragons Plus (liquid dyes in many colours + texts)."""
import json, os, re

HERE = os.path.dirname(__file__)
I18N = os.path.join(HERE, "..", "..", "i18n")
SRC = json.load(open(os.path.join(I18N, "src", "create_dragons_plus.json"), encoding="utf-8"))
OUT = os.path.join(I18N, "batches", "b20_dragonsplus.json")

COL = {"black": "чорний", "blue": "синій", "brown": "коричневий", "cyan": "бірюзовий", "gray": "сірий", "green": "зелений",
       "light_blue": "блакитний", "light_gray": "світло-сірий", "lime": "лаймовий", "magenta": "пурпуровий", "orange": "помаранчевий",
       "pink": "рожевий", "purple": "фіолетовий", "red": "червоний", "white": "білий", "yellow": "жовтий",
       "amber": "бурштиновий", "aqua": "колір морської хвилі", "beige": "бежевий", "coral": "кораловий", "forest": "лісовий",
       "ginger": "імбирний", "indigo": "індиго", "maroon": "бордовий", "mint": "м'ятний", "navy": "темно-синій", "olive": "оливковий",
       "rose": "трояндовий", "slate": "сланцевий", "tan": "пісочний", "teal": "синьо-зелений", "verdant": "соковито-зелений",
       "aquamarine": "аквамариновий", "bubblegum": "жуйковий", "cherenkov": "черенковський", "conifer": "хвойний",
       "fluorescent": "флуоресцентний", "honey": "медовий", "icy_blue": "крижано-блакитний", "lavender": "лавандовий",
       "peach": "персиковий", "persimmon": "хурмовий", "spring_green": "весняно-зелений", "ultramarine": "ультрамариновий",
       "wine": "винний", "bleached": "вибілювальний"}
SRCMOD = {"dye_depot": " (Dye Depot)", "dyenamics": " (Dyenamics)", "arts_and_crafts": " (Arts and Crafts)"}


def gen(adj):
    if " " in adj or adj == "індиго":          # indeclinable phrases
        return adj
    return adj[:-2] + ("ього" if adj.endswith("ій") else "ого")


def parse(name):
    """'dyenamics_icy_blue' -> (colour key, mod suffix)."""
    for pre, suf in SRCMOD.items():
        if name.startswith(pre + "_"):
            return name[len(pre) + 1:], suf
    return name, ""


def nom(color):
    adj = COL[color]
    if adj == "колір морської хвилі":
        return "Барвник кольору морської хвилі"
    if adj == "індиго":
        return "Барвник індиго"
    return f"{adj[0].upper()}{adj[1:]} барвник"


def gen_dye(color):
    adj = COL[color]
    if adj == "колір морської хвилі":
        return "барвника кольору морської хвилі"
    if adj == "індиго":
        return "барвника індиго"
    return f"{gen(adj)} барвника"


out = {}
for k in SRC:
    m = re.match(r"(block|fluid)\.create_dragons_plus\.([a-z_]+)_dye$", k)
    if m:
        c, suf = parse(m.group(2)); out[k] = "Рідкий " + nom(c)[0].lower() + nom(c)[1:] + suf; continue
    m = re.match(r"item\.create_dragons_plus\.([a-z_]+)_dye_bucket$", k)
    if m:
        c, suf = parse(m.group(1)); out[k] = "Відро " + gen_dye(c) + suf; continue
    m = re.match(r"tag\.fluid\.c\.dyes\.([a-z_]+)$", k)
    if m:
        c, _ = parse(m.group(1)); out[k] = nom(c); continue
    m = re.match(r"tag\.item\.c\.buckets\.dye\.([a-z_]+)$", k)
    if m:
        c, _ = parse(m.group(1)); out[k] = "Відра " + gen_dye(c)
out.update({
    "block.create_dragons_plus.dragon_breath": "Дихання дракона", "fluid.create_dragons_plus.dragon_breath": "Дихання дракона",
    "block.create_dragons_plus.dragon_breath_cauldron": "Казан з диханням дракона", "item.create_dragons_plus.dragon_breath_bucket": "Відро дихання дракона",
    "block.create_dragons_plus.fluid_hatch": "Рідинний люк",
    "block.create_dragons_plus.fluid_hatch.tooltip.summary": "_Легко робить_ ваш _резервуар_ доступним для _контейнерів з рідиною_.",
    "block.create_dragons_plus.fluid_hatch.tooltip.condition1": "ПКМ",
    "block.create_dragons_plus.fluid_hatch.tooltip.behaviour1": "_Виливає_ _рідину_ з _предмета в руці_ в _резервуар_, на якому стоїть. Якщо предмет не можна спорожнити, натомість _наповнює_ його з _резервуара_.",
    "block.create_dragons_plus.fluid_hatch.tooltip.condition2": "Shift + ПКМ",
    "block.create_dragons_plus.fluid_hatch.tooltip.behaviour2": "Спершу _наповнює_ ваш _контейнер_ з _резервуара_. Якщо його не можна наповнити, рідина _виливається_ в _резервуар_.",
    "block.create_dragons_plus.fragile_fluid_tank": "Крихкий рідинний резервуар",
    "block.create_dragons_plus.levitite_fragile_fluid_tank": "Крихкий левітитовий рідинний резервуар",
    "create.item_attributes.create_dragons_plus.aether_enchantable": "можна масово зачарувати",
    "create.item_attributes.create_dragons_plus.aether_enchantable.inverted": "не можна масово зачарувати",
    "create.item_attributes.create_dragons_plus.endable": "можна обробити Ендом", "create.item_attributes.create_dragons_plus.endable.inverted": "не можна обробити Ендом",
    "create.item_attributes.create_dragons_plus.freezable": "можна заморозити", "create.item_attributes.create_dragons_plus.freezable.inverted": "не можна заморозити",
    "create.item_attributes.create_dragons_plus.sandable": "можна відшліфувати", "create.item_attributes.create_dragons_plus.sandable.inverted": "не можна відшліфувати",
    "create.item_attributes.create_dragons_plus.stainable": "можна пофарбувати", "create.item_attributes.create_dragons_plus.stainable.inverted": "не можна пофарбувати",
    "create_dragons_plus.fragile_fluid_tank.effect.dragon_breath": "Шкодить живим істотам поблизу диханням дракона",
    "create_dragons_plus.fragile_fluid_tank.effect.dye": "Фарбує істот і сумісні блоки поблизу",
    "create_dragons_plus.fragile_fluid_tank.effect.explosive": "Вибухає при ударі", "create_dragons_plus.fragile_fluid_tank.effect.header": "Ефект удару",
    "create_dragons_plus.fragile_fluid_tank.effect.lava": "Підпалює блоки, поширює вогонь і обпікає істот у радіусі",
    "create_dragons_plus.fragile_fluid_tank.effect.milk": "Знімає ефекти зілль у радіусі",
    "create_dragons_plus.fragile_fluid_tank.effect.none": "Без особливого ефекту",
    "create_dragons_plus.fragile_fluid_tank.effect.potion": "Накладає ефекти зілля в радіусі",
    "create_dragons_plus.fragile_fluid_tank.effect.tea": "Дає квапливість живим істотам поблизу",
    "create_dragons_plus.fragile_fluid_tank.effect.water": "Гасить вогонь і зволожує чи охолоджує придатні блоки",
    "create_dragons_plus.fragile_fluid_tank.effect.infused_dragon_breath": "Розбирає предмети поблизу на землі й може розібрати вдягнені",
    "create_dragons_plus.ponder.air_current_interact_with_block.header": "Повітряні потоки масової обробки з симульованих механізмів",
    "create_dragons_plus.ponder.air_current_interact_with_block.text_1": "Повітряний потік масової обробки з симульованого механізму може діяти на блоки",
    "create_dragons_plus.ponder.air_current_interact_with_block.text_2": "«Потік масової виплавки поширює вогонь» типово вимкнено в налаштуваннях, щоб новачки випадково не спалили свої будинки",
    "create_dragons_plus.ponder.bulk_coloring.header": "Масове фарбування",
    "create_dragons_plus.ponder.bulk_coloring.text_1": "Потік повітря крізь рідкий барвник створює установку фарбування",
    "create_dragons_plus.ponder.bulk_coloring.text_2": "Як масова виплавка й копчення, масове фарбування автоматично працює з деякими рецептами крафту з барвниками",
    "create_dragons_plus.ponder.bulk_coloring.text_3": "Потік фарбування діє й на істот, змінюючи їхній колір.",
    "create_dragons_plus.ponder.bulk_coloring.text_4": "Вдягнена броня теж фарбується.",
    "create_dragons_plus.ponder.bulk_enchanting.header": "Масове зачарування",
    "create_dragons_plus.ponder.bulk_enchanting.text_1": "Потік повітря крізь золоту хмарину Етеру створює установку масового зачарування",
    "create_dragons_plus.ponder.bulk_enchanting.text_2": "Масове зачарування обробляє рецепти вівтаря Етеру вітрогоном",
    "create_dragons_plus.ponder.bulk_enchanting.text_3": "Деякі рецепти лагодять спорядження, тож вхідний і вихідний предмет можуть бути однакові",
    "create_dragons_plus.ponder.bulk_enchanting.text_4": "Той самий потік може висиджувати яйця моа, як обробку предметів",
    "create_dragons_plus.ponder.bulk_ending.header": "Масова обробка Ендом",
    "create_dragons_plus.ponder.bulk_ending.text_1": "Потік повітря крізь голову дракона чи дихання дракона створює установку обробки Ендом",
    "create_dragons_plus.ponder.bulk_freezing.header": "Масове заморожування",
    "create_dragons_plus.ponder.bulk_freezing.text_1": "Потік повітря крізь сипкий сніг створює установку заморожування",
    "create_dragons_plus.ponder.bulk_sanding.header": "Масове шліфування",
    "create_dragons_plus.ponder.bulk_sanding.text_1": "Потік повітря крізь каталізатори шліфування (наприклад, жвавоґрунт) створює установку шліфування",
    "create_dragons_plus.ponder.bulk_sanding.text_2": "Як масова виплавка й копчення, масове шліфування автоматично працює з рецептами наждачного паперу",
    "create_dragons_plus.ponder.dragon_breath_fluid.header": "Рідке дихання дракона",
    "create_dragons_plus.ponder.dragon_breath_fluid.text_1": "Дихання дракона можна набирати в казан",
    "create_dragons_plus.ponder.dragon_breath_fluid.text_2": "Коли текуче дихання дракона зустрічає лаву, утворюється камінь Енду",
    "create_dragons_plus.ponder.dye_fluids.header": "Рідкий барвник",
    "create_dragons_plus.ponder.dye_fluids.text_1": "Змішавши барвник з водою, отримаєте рідкий барвник",
    "create_dragons_plus.ponder.dye_fluids.text_2": "Змішування з нагрівом повертає барвник з рідини",
    "create_dragons_plus.ponder.dye_fluids.text_3": "Предмети й живі істоти, що торкаються рідкого барвника, фарбуються",
    "create_dragons_plus.ponder.dye_fluids.text_4": "Коли рідкий барвник зустрічає лаву, утворюється бетон відповідного кольору",
    "create_dragons_plus.ponder.fragile_fluid_tank.header": "Крихкий рідинний резервуар",
    "create_dragons_plus.ponder.fragile_fluid_tank.text_1": "Ці резервуари крихкі: вони розбиваються від удару",
    "create_dragons_plus.ponder.fragile_fluid_tank.text_2": "Коли крихкий резервуар розбивається, рідина діє на все довкола",
    "create_dragons_plus.ponder.group.no_scene": "Для цього пункту немає сцени Ponder", "create_dragons_plus.ponder.group.open": "ЛКМ — переглянути цей пункт",
    "create_dragons_plus.ponder.group.position": "%s / %s", "create_dragons_plus.ponder.group.scroll": "Коліщатко — вибрати пункт",
    "item.create_dragons_plus.blaze_upgrade_smithing_template": "Ковальський шаблон",
    "item.create_dragons_plus.smithing_template.blaze_upgrade.additions_slot_description": "Додайте робочі блоки для пломеня",
    "item.create_dragons_plus.smithing_template.blaze_upgrade.applies_to": "Пальник пломеня",
    "item.create_dragons_plus.smithing_template.blaze_upgrade.base_slot_description": "Додайте пальник пломеня",
    "item.create_dragons_plus.smithing_template.blaze_upgrade.ingredients": "Робочі блоки для пломеня",
    "itemGroup.create_dragons_plus.base": "Create: Dragons Plus", "pack.create_dragons_plus.runtime": "Create: Dragons Plus",
    "pack.create_dragons_plus.runtime.description": "Ресурси Create: Dragons Plus, створені під час роботи",
    "recipe.create_dragons_plus.aether_fan_enchanting": "Масове зачарування",
    "recipe.create_dragons_plus.aether_fan_enchanting.fan": "Вітрогін за золотою хмариною", "recipe.create_dragons_plus.aether_fan_enchanting.repairing": "Лагодить міцність предмета",
    "recipe.create_dragons_plus.aether_fan_incubation": "Масове висиджування", "recipe.create_dragons_plus.aether_fan_incubation.entity": "З'являється: %s",
    "recipe.create_dragons_plus.aether_fan_incubation.fan": "Вітрогін за золотою хмариною",
    "recipe.create_dragons_plus.automated_coloring": "Автоматичне фарбування", "recipe.create_dragons_plus.fan_coloring": "Масове фарбування",
    "recipe.create_dragons_plus.fan_coloring.fan": "Вітрогін за рідким барвником", "recipe.create_dragons_plus.fan_ending": "Масова обробка Ендом",
    "recipe.create_dragons_plus.fan_ending.fan": "Вітрогін за головою дракона", "recipe.create_dragons_plus.fan_freezing": "Масове заморожування",
    "recipe.create_dragons_plus.fan_freezing.fan": "Вітрогін за сипким снігом", "recipe.create_dragons_plus.fan_sanding": "Масове шліфування",
    "recipe.create_dragons_plus.fan_sanding.fan": "Вітрогін за жвавоґрунтом",
    "tag.block.create_dragons_plus.fan_processing_catalysts.ending": "Каталізатори обробки Ендом",
    "tag.block.create_dragons_plus.fan_processing_catalysts.sanding": "Каталізатори шліфування",
    "tag.block.create_dragons_plus.not_applicable_for_polishing": "Не для автоматичного рецепта шліфування блоків",
    "tag.block.create_dragons_plus.passive_block_freezers": "Пасивні заморожувачі блоків", "tag.fluid.c.dragon_breath": "Дихання дракона",
    "tag.fluid.c.dyes": "Барвники", "tag.fluid.create_dragons_plus.fan_processing_catalysts.ending": "Каталізатори обробки Ендом",
    "tag.item.c.buckets.dragon_breath": "Відра дихання дракона", "tag.item.c.buckets.dye": "Відра барвника",
    "tag.item.create_dragons_plus.not_applicable_for_coloring": "Не для автоматичного рецепта фарбування",
    "upgrade.create_dragons_plus.blaze_upgrade": "Покращення пломеня",
})
out = {k: v for k, v in out.items() if k in SRC}
missing = [k for k in SRC if k not in out]
json.dump({"create_dragons_plus": out}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(out), "translated;", len(missing), "missing:", missing)
