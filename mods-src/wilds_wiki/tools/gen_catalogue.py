"""The wiki's catalogue: the categories of the home page (categories.json) and one line about every mod (mods.json).

  python gen_catalogue.py   ->  wilds_wiki/uk_ua/categories.json, mods.json
"""
import glob, importlib.util, json, os, re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "wilds_wiki", "wilds_wiki", "uk_ua")


def mod(ns):
    return "topic:wilds_wiki:mod/" + ns


def sec(title=None, **kw):
    d = {"title": title} if title else {}
    d.update(kw)
    return d


BOSSES = """minecraft:ender_dragon minecraft:wither minecraft:warden minecraft:elder_guardian
twilightforest:naga twilightforest:lich twilightforest:minoshroom twilightforest:hydra twilightforest:knight_phantom twilightforest:ur_ghast
twilightforest:alpha_yeti twilightforest:snow_queen
aether:slider aether:valkyrie_queen aether:sun_spirit
cataclysm:netherite_monstrosity cataclysm:ignis cataclysm:ender_guardian cataclysm:the_harbinger cataclysm:the_leviathan cataclysm:ancient_remnant
cataclysm:maledictus cataclysm:scylla
mowziesmobs:ferrous_wroughtnaut mowziesmobs:frostmaw mowziesmobs:umvuthi mowziesmobs:sculptor
iceandfire:fire_dragon iceandfire:ice_dragon iceandfire:lightning_dragon iceandfire:hydra iceandfire:gorgon iceandfire:cyclops iceandfire:sea_serpent
alexscaves:luxtructosaurus alexscaves:tremorzilla alexscaves:forsaken alexscaves:hullbreaker alexscaves:atlatitan
alexsmobs:void_worm alexsmobs:warped_mosco alexsmobs:farseer
irons_spellbooks:dead_king irons_spellbooks:fire_boss
ars_nouveau:wilden_boss friendsandfoes:wildfire hybrid_aquatic:karkinos upgrade_aquatic:great_thrasher""".split()

CHAPTERS = []
for f in sorted(glob.glob(os.path.join(ROOT, "pack", "tools", "quests", "chapters", "c*.py"))):
    spec = importlib.util.spec_from_file_location(os.path.basename(f)[:-3], f)
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    CHAPTERS.append((m.CHAPTER["key"], re.sub(r"&[0-9a-fk-or]", "", m.CHAPTER["title"])))

CATEGORIES = [
    dict(id="start", top=True, title="Початок гри й сервер", icon="minecraft:compass",
         note="Як користуватися вікі, команди сервера, квести, навички, гроші, спорядження.",
         keywords=["початок", "старт", "новачок", "основи"],
         sections=[sec("Перші кроки", pages=["wiki_help", "server", "friends", "quest_givers", "skills", "money"]),
                   sec("Зручності збірки", pages=["equipment", "finder", "copper_golem", "gathering", "tents", "seasons", "music", "links"]),
                   sec(children=["faq", "quests"])]),
    dict(id="faq", top=True, title="Часті питання", icon="minecraft:writable_book",
         note="Короткі відповіді: як зняти кислотний дощ, як потрапити до печер тролів, де шукати печери Alex's Caves…",
         keywords=["питання", "faq", "як", "чому", "де знайти"],
         sections=[sec(articles="faq")]),
    dict(id="walkthrough", top=True, title="Проходження: виміри й пригоди", icon="minecraft:filled_map",
         note="Покрокові гайди: Сутінковий ліс, Етер, Незер, Енд, печери, дракони, катаклізм.",
         keywords=["проходження", "гайд", "прогресія", "виміри", "пригоди"],
         sections=[sec("Виміри", articles="dimensions"),
                   sec("Пригоди у звичайному світі", articles="adventures", pages=["dragons", "world_tiers", "peoples", "seas", "colonies"]),
                   sec(children=["bosses", "quests"])]),
    dict(id="bosses", top=True, title="Боси", icon="minecraft:wither_skeleton_skull",
         note="Усі боси збірки: де шукати, як викликати, що падає.",
         keywords=["бос", "боси", "boss"],
         sections=[sec("Гайди по босах", articles="bosses"),
                   sec("Cataclysm", articles="path:guides/cataclysm/"),
                   sec("Усі боси", query="entity:boss")]),
    dict(id="mobs", top=True, title="Істоти", icon="minecraft:creeper_head",
         note="Вороги, тварини, морські мешканці, народи. На сторінці кожної — де живе й що з неї падає.",
         keywords=["моби", "істоти", "тварини", "вороги", "монстри"],
         sections=[sec("Народи й приручення", pages=["peoples", "dragons", "quest_givers"], articles="mobs"),
                   sec("Боси", children=["bosses"]),
                   sec("Вороги", query="entity:monster"), sec("Тварини й мирні істоти", query="entity:creature"),
                   sec("Водні мешканці", query="entity:water"), sec("Інші", query="entity:ambient")]),
    dict(id="world", top=True, title="Світ: біоми та споруди", icon="minecraft:grass_block",
         note="Біоми кожного виміру й споруди в них: що там живе, що лежить у скринях.",
         keywords=["біоми", "споруди", "структури", "виміри", "світ"],
         sections=[sec("Про генерацію світу", articles="world"),
                   sec("Біоми звичайного світу", query="biome:minecraft:overworld"),
                   sec("Біоми Сутінкового лісу", query="biome:twilightforest:twilight_forest"),
                   sec("Біоми Етеру", query="biome:aether:the_aether"),
                   sec("Біоми Незеру", query="biome:minecraft:the_nether"),
                   sec("Біоми Енду", query="biome:minecraft:the_end"),
                   sec("Споруди звичайного світу", query="structure:minecraft:overworld"),
                   sec("Споруди Сутінкового лісу", query="structure:twilightforest:twilight_forest"),
                   sec("Споруди Етеру", query="structure:aether:the_aether"),
                   sec("Споруди Незеру", query="structure:minecraft:the_nether"),
                   sec("Споруди Енду", query="structure:minecraft:the_end")]),
    dict(id="gear", top=True, title="Зброя, броня, інструменти", icon="minecraft:diamond_sword",
         note="Уся зброя ближнього й далекого бою, броня за частинами, щити, інструменти, самоцвіти.",
         keywords=["зброя", "броня", "інструменти", "мечі", "луки", "щити", "спорядження"],
         sections=[sec("Покращення спорядження", pages=["gems", "world_tiers", "equipment"], articles="gear"),
                   sec("Apotheosis", articles="path:guides/apotheosis/"),
                   sec("Зброя ближнього бою", query="item:melee"), sec("Сокири", query="item:axe"),
                   sec("Зброя далекого бою", query="item:ranged"), sec("Стріли й набої", query="item:ammo"), sec("Щити", query="item:shield"),
                   sec("Шоломи", query="item:armor_head"), sec("Нагрудники", query="item:armor_chest"),
                   sec("Поножі", query="item:armor_legs"), sec("Чоботи", query="item:armor_feet"),
                   sec("Кирки", query="item:pickaxe"), sec("Лопати", query="item:shovel"), sec("Мотики", query="item:hoe"),
                   sec("Інші інструменти", query="item:tool")]),
    dict(id="food", top=True, title="Їжа, зілля, ефекти", icon="minecraft:bread",
         note="Уся їжа й напої, кухня Farmer's Delight, зілля й що робить кожен ефект.",
         keywords=["їжа", "зілля", "ефекти", "кухня", "напої"],
         sections=[sec("Кухня", articles="path:guides/farmersdelight/", pages=[mod("farmersdelight"), mod("oceansdelight")]),
                   sec("Їжа", query="item:food"), sec("Зілля", query="item:potion"),
                   sec("Корисні ефекти", query="effect:beneficial"), sec("Шкідливі ефекти", query="effect:harmful"),
                   sec("Нейтральні ефекти", query="effect:neutral")]),
    dict(id="magic", top=True, title="Магія й зачарування", icon="minecraft:enchanted_book",
         note="Що робить кожне зачарування, стіл і бібліотека зачарувань, заклинання, мана.",
         keywords=["магія", "зачарування", "заклинання", "мана", "чари"],
         sections=[sec("Зачарування", articles="path:guides/enchanting/", pages=["item:minecraft:enchanted_book", mod("apothic_enchanting")]),
                   sec("Школи магії", pages=[mod("irons_spellbooks"), mod("ars_nouveau"), mod("ars_n_spells")], articles="magic"),
                   sec("Усі зачарування", query="enchantment:all")]),
    dict(id="tech", top=True, title="Механізми й техніка", icon="create:cogwheel",
         note="Create та його доповнення, Mekanism, Applied Energistics 2, кораблі й літальні машини.",
         keywords=["техніка", "механізми", "автоматизація", "машини", "енергія"],
         sections=[sec("Моди", pages=[mod("create"), mod("createaddition"), mod("create_enchantment_industry"), mod("create_dragons_plus"), mod("copycats"),
                                      mod("aeronautics"), mod("simulated"), mod("offroad"), mod("mekanism"), mod("mekanismgenerators"), mod("mekanismtools"),
                                      mod("ae2"), mod("appmek"), mod("ae2wtlib")]),
                   sec("Гайди", articles="tech")]),
    dict(id="life", top=True, title="Дім, сховища, подорожі", icon="minecraft:chest",
         note="Скрині й рюкзаки, сортування, шляхові камені, намети, кораблі, колонії.",
         keywords=["сховище", "скрині", "рюкзак", "подорожі", "дім", "транспорт"],
         sections=[sec("Сховища", pages=["copper_golem", "finder", mod("sophisticatedstorage"), mod("sophisticatedbackpacks")]),
                   sec("Подорожі", articles="path:guides/waystones/", pages=["tents", "seas", mod("waystones"), mod("smallships")]),
                   sec("Поселення", pages=["colonies", "peoples", mod("minecolonies"), mod("easy_villagers"), mod("villagersplus"), mod("guardvillagers")])]),
    dict(id="mods", top=True, title="Моди", icon="minecraft:bookshelf",
         note="Кожен мод збірки: що він додає, його істоти, предмети, біоми й гайди.",
         keywords=["моди", "мод", "mods"],
         sections=[sec(query="mods")]),
    dict(id="quests", title="Квести за розділами", icon="ftbquests:book",
         note="Підказки до кожного квесту з книги квестів (J): де, наскільки небезпечно, що взяти.",
         keywords=["квести", "завдання", "книга квестів"],
         sections=[sec(title, articles="q_" + key) for key, title in CHAPTERS]),
]

MODS = {
    "create": ("Create", "create:cogwheel", "Механізми на обертанні: вали, шестерні, конвеєри, преси, поїзди. Основа автоматизації збірки.", ["механізми", "автоматизація", "шестерня"]),
    "createaddition": ("Create Crafts & Additions", None, "Електрика для Create: генератор і мотор, що перетворюють обертання на енергію й назад.", []),
    "create_enchantment_industry": ("Create: Enchantment Industry", None, "Автоматичне зачарування й рідкий досвід для Create.", []),
    "create_dragons_plus": ("Create: Dragons Plus", None, "Доповнення Create: нові способи обробки, зокрема подихом дракона.", []),
    "copycats": ("Create: Copycats+", None, "Блоки-хамелеони: сходи, плити й панелі, що набувають вигляду будь-якого блока.", []),
    "aeronautics": ("Create Aeronautics", None, "Літальні апарати з блоків: повітряні кулі, гвинти, дирижаблі.", ["дирижабль", "літак"]),
    "simulated": ("Simulated", None, "Фізика для машин Create Aeronautics: рухомі конструкції, канати, шарніри.", []),
    "offroad": ("Offroad", None, "Наземні машини на колесах для Create Aeronautics.", []),
    "mekanism": ("Mekanism", "mekanism:steel_casing", "Техніка високого рівня: переробка руди в кілька разів вигідніше, енергія, гази, ядерний реактор.", ["техніка", "енергія"]),
    "mekanismgenerators": ("Mekanism: Generators", None, "Генератори Mekanism: сонячні, вітрові, газові, турбіна й реактори.", []),
    "mekanismtools": ("Mekanism: Tools", None, "Броня та інструменти з матеріалів Mekanism: бронза, сталь, рафінований обсидіан.", []),
    "ae2": ("Applied Energistics 2", "ae2:controller", "Цифрове сховище: усі речі в одній мережі з пошуком, автокрафтом і бездротовим доступом.", ["мережа", "автокрафт"]),
    "appmek": ("Applied Mekanistics", None, "Зберігання газів і речовин Mekanism у мережі AE2.", []),
    "ae2wtlib": ("AE2WTLib", None, "Бездротові термінали AE2 з додатковими можливостями.", []),
    "twilightforest": ("Сутінковий ліс", "twilightforest:naga_trophy", "Окремий вимір вічних сутінків з вісьмома босами, лабіринтами й замками. Проходиться по порядку.", ["twilight forest", "сутінковий", "тф"]),
    "aether": ("Етер", "aether:skyroot_sapling", "Вимір небесних островів над хмарами: три підземелля з босами, літаючі тварини, особлива зброя.", ["aether", "етер", "ефір", "небо"]),
    "alexscaves": ("Alex's Caves", "alexscaves:cave_map", "Шість величезних печерних біомів під землею: первісні з динозаврами, магнітні, токсичні, безодня, цукрові й занедбані.", ["печери", "динозаври", "alex's caves"]),
    "alexsmobs": ("Alex's Mobs", "alexsmobs:animal_dictionary", "Понад 80 нових тварин з усього світу, кожна з власною поведінкою й користю.", ["тварини", "alex's mobs"]),
    "iceandfire": ("Ice and Fire", "iceandfire:dragonegg_red", "Дракони трьох стихій, міфічні істоти (гідри, горгони, циклопи, сирени) і спорядження з них.", ["дракони", "міфи"]),
    "cataclysm": ("L_Ender's Cataclysm", "cataclysm:ignitium_ingot", "Найскладніші боси збірки у власних спорудах і легендарна зброя з них.", ["катаклізм", "боси"]),
    "mowziesmobs": ("Mowzie's Mobs", None, "Кілька дуже виразних босів і істот: Залізний кривдник, Морозна паща, Умвуті, Скульптор.", ["mowzie"]),
    "irons_spellbooks": ("Iron's Spells 'n Spellbooks", "irons_spellbooks:copper_spell_book", "Магія із заклинаннями у книгах: мана, сувої, школи вогню, льоду, блискавки, крові та інші.", ["заклинання", "мана", "магія"]),
    "ars_nouveau": ("Ars Nouveau", "ars_nouveau:novice_spell_book", "Магія, де заклинання складають із гліфів, а джерело дає силу машинам і фамільярам.", ["заклинання", "гліфи", "джерело"]),
    "ars_n_spells": ("Ars 'n' Spells", None, "З'єднує ману Ars Nouveau та Iron's Spells в одну.", []),
    "apotheosis": ("Apotheosis", "apotheosis:gem_dust", "Рідкісність спорядження, самоцвіти в гніздах, міні-боси й світові рівні складності.", ["самоцвіти", "рідкісність", "світові рівні"]),
    "apothic_enchanting": ("Apothic Enchanting", None, "Перероблений стіл зачарувань: вищі рівні, нові зачарування, бібліотека для зберігання книг.", ["зачарування"]),
    "minecolonies": ("MineColonies", "minecolonies:blockhuttownhall", "Власне місто з найнятими жителями: будівельники, шахтарі, фермери, варта.", ["колонія", "місто"]),
    "domum_ornamentum": ("Domum Ornamentum", None, "Декоративні блоки для будівель MineColonies з будь-яких матеріалів.", []),
    "structurize": ("Structurize", None, "Інструменти для схем будівель MineColonies.", []),
    "multipiston": ("Multi-Piston", None, "Поршень MineColonies, що рухає блоки на кілька клітин у будь-який бік.", []),
    "farmersdelight": ("Farmer's Delight", "farmersdelight:cooking_pot", "Кухня: казан, дошка для нарізання, нові культури й десятки страв.", ["кухня", "їжа", "готування"]),
    "oceansdelight": ("Ocean's Delight", None, "Морські страви для Farmer's Delight.", []),
    "hybrid_aquatic": ("Hybrid Aquatic", None, "Сотні видів риб, акул і морських істот, нові морські біоми й риболовля.", ["риба", "море", "акули"]),
    "upgrade_aquatic": ("Upgrade Aquatic", None, "Оновлені океани: нові корали, водорості, медузи, трешери.", ["океан"]),
    "fishofthieves": ("Fish of Thieves", None, "Риби із Sea of Thieves з рідкісними різновидами та морські пости.", ["риба"]),
    "naturalist": ("Naturalist", None, "Дикі тварини лісів і саван: ведмеді, олені, леви, слони, птахи.", ["тварини"]),
    "friendsandfoes": ("Friends & Foes", "minecraft:copper_block", "Істоти з голосувань Minecraft: мідний голем, льодовик, ілюзіоніст, мох-голем.", ["мідний голем"]),
    "goblins_tyranny": ("Goblin's Tyranny", "goblins_tyranny:coin", "Гобліни: табори, вояки, торговці й власна валюта.", ["гобліни"]),
    "kobolds": ("Kobolds", None, "Кобольди — підземний народ ковалів і торговців.", ["кобольди"]),
    "orc_hut": ("Orc Hut", None, "Хатини орків у світі.", ["орки"]),
    "guardvillagers": ("Guard Villagers", None, "Вартові в селах, яких можна споряджати й наймати.", ["вартові"]),
    "villagersplus": ("VillagersPlus", None, "Нові професії селян: алхімік, садівник, океанограф, окультист.", ["селяни"]),
    "easy_villagers": ("Easy Villagers", None, "Блоки для зручної торгівлі й розведення селян без загонів.", ["селяни", "торгівля"]),
    "bountiful": ("Bountiful", None, "Дошка оголошень у селах: завдання за нагороду.", ["завдання"]),
    "smallships": ("Small Ships", "smallships:oak_cog", "Вітрильники з гарматами й трюмом: коги, бриги, галери, драккари.", ["кораблі", "вітрильник"]),
    "waystones": ("Waystones", "waystones:waystone", "Шляхові камені для швидких подорожей між відвіданими місцями.", ["телепорт", "шляховий камінь"]),
    "sophisticatedstorage": ("Sophisticated Storage", None, "Скрині, бочки й шалкери з рівнями та покращеннями.", ["скрині", "сховище"]),
    "sophisticatedbackpacks": ("Sophisticated Backpacks", None, "Рюкзаки з покращеннями: магніт, сортування, крафт, стискання.", ["рюкзак"]),
    "sophisticatedcore": None,
    "nomadictents": ("Nomadic Tents", None, "Юрти, типі й шатра, що згортаються в предмет разом із вмістом.", ["намет", "юрта"]),
    "simplytents": ("SimplyTents", None, "Прості намети й спальники для ночівлі в дорозі.", ["намет"]),
    "comforts": ("Comforts", None, "Спальні мішки й гамаки: сон без зміни точки відродження.", ["спальник", "гамак"]),
    "sereneseasons": ("Serene Seasons", None, "Пори року: змінюються погода, колір листя й ріст урожаю.", ["пори року"]),
    "enderscape": ("Enderscape", None, "Оновлений Енд: нові біоми, рослини, істоти й матеріали.", ["енд"]),
    "eternalnether": ("Eternal Nether", None, "Нові споруди й істоти Незеру: катакомби, цитаделі, маєтки піґлінів.", ["незер"]),
    "netherdepthsupgrade": ("Nether Depths Upgrade", None, "Життя в лавових морях Незеру: риби, рослини, риболовля в лаві.", ["незер", "лава", "риба"]),
    "netherexotics": ("Nether Exotics", None, "Екзотичні плоди й рослини Незеру.", ["незер", "плоди"]),
    "seamlessores": ("Seamless Ores", None, "Руда в будь-якому камені виглядає як частина цього каменю.", []),
    "gew2": ("Ground Elements World", None, "Камінці, гілки й дрібниці на землі, що оживляють краєвид.", []),
    "lootr": ("Lootr", None, "Скрині зі здобиччю дають власний вміст кожному гравцю.", ["здобич", "скрині"]),
    "explorerscompass": ("Explorer's Compass", "explorerscompass:explorerscompass", "Компас, що шукає будь-яку споруду за назвою.", ["компас", "споруди"]),
    "naturescompass": ("Nature's Compass", "naturescompass:naturescompass", "Компас, що шукає будь-який біом за назвою.", ["компас", "біоми"]),
    "ftbquests": ("FTB Quests", None, "Книга квестів (клавіша J).", ["квести"]),
    # world generation (mostly server-only: the client cannot tell their names, so they are written here)
    "terralith": ("Terralith", None, "95 нових біомів звичайного світу й 28 споруд у них: укріплені села, застави.", ["біоми"]),
    "incendium": ("Incendium", None, "Нові біоми й споруди Незеру: Заборонений замок, Санктум та інші.", ["незер"]),
    "mvs": ("Moog's Voyager Structures", None, "130 невеликих споруд для мандрівників у звичайному світі й кілька в Незері.", ["споруди"]),
    "nova_structures": ("Dungeons and Taverns", None, "96 споруд: таверни, підземелля, крипти й застави у звичайному світі, Незері та Енді.", ["таверна", "підземелля"]),
    "ctov": ("ChoiceTheorem's Overhauled Village", None, "Оновлені села: 78 різновидів для різних біомів.", ["село"]),
    "towns_and_towers": ("Towns and Towers", None, "Села й застави розбійників у різних стилях: 60 споруд.", ["село", "застава"]),
    "dungeons_arise": ("When Dungeons Arise", None, "40 великих підземель, веж і таборів розбійників, кілька — в Енді.", ["підземелля"]),
    "dungeons_arise_seven_seas": ("When Dungeons Arise: Seven Seas", None, "Кораблі в морі: галеон, корвет корсарів, піратська джонка, яхта.", ["корабель"]),
    "mos": ("Moog's Ocean Structures", None, "26 морських споруд: руїни, закопані скарби, скелет дракона.", ["море", "руїни"]),
    "hopo": ("Hopo Better Underwater Ruins", None, "Підводні руїни, скам'янілості й місто під водою.", ["руїни"]),
    "ocean_lily_pad_village": ("Ocean Lily Pad Village", None, "Село на лататтях посеред океану.", ["село"]),
    "joshie": ("Tidal Towns", None, "Село на воді посеред океану.", ["село"]),
    "bettermineshafts": ("YUNG's Better Mineshafts", None, "Шахти в 13 стилях, кожен під свій біом.", ["шахта"]),
    "betterdungeons": ("YUNG's Better Dungeons", None, "Підземелля скелетів і павуків, малі підземелля й одне в Незері.", ["підземелля"]),
    "betterfortresses": ("YUNG's Better Nether Fortresses", None, "Оновлена незерська фортеця.", ["фортеця"]),
    "betteroceanmonuments": ("YUNG's Better Ocean Monuments", None, "Оновлений океанський монумент.", ["монумент"]),
    "betterstrongholds": ("YUNG's Better Strongholds", None, "Оновлена споруда з порталом в Енд.", ["портал енду"]),
    "patchouli": None, "guideme": None, "hapi": None, "ftblibrary": None, "irons_lib": None,
}

json.dump({"bosses": BOSSES, "categories": CATEGORIES}, open(os.path.join(OUT, "categories.json"), "w", encoding="utf-8", newline="\n"), indent=1, ensure_ascii=False)
out = {}
for ns, v in MODS.items():
    if v is None:
        out[ns] = {"hide": True}
        continue
    name, icon, note, kw = v
    out[ns] = {"name": name, "note": note, "keywords": kw}
    if icon:
        out[ns]["icon"] = icon
extra = os.path.join(HERE, "mods_text.json")   # longer texts for mod pages, written by hand
if os.path.exists(extra):
    for ns, text in json.load(open(extra, encoding="utf-8")).items():
        out.setdefault(ns, {})["text"] = text
json.dump(out, open(os.path.join(OUT, "mods.json"), "w", encoding="utf-8", newline="\n"), indent=1, ensure_ascii=False)
print(len(CATEGORIES), "categories,", len(out), "mods,", len(BOSSES), "bosses,", len(CHAPTERS), "quest chapters")
