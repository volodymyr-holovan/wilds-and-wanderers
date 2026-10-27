"""Batch 38: Naturalist (creatures, items, configuration, subtitles). Subtitles are built from the animal names."""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "i18n", "batches", "b38_naturalist.json")

ANIMALS = {  # id -> (name, grammatical gender m/f/n/pl for the verbs)
    "anglerfish": ("Вудильник", "m"), "ant": ("Мураха", "f"), "black_bear": ("Чорний ведмідь", "m"), "blobfish": ("Риба-крапля", "f"),
    "butterfly": ("Метелик", "m"), "clam": ("Молюск", "m"), "crab": ("Краб", "m"), "deer": ("Олень", "m"), "dragonfly": ("Бабка", "f"),
    "capybara": ("Капібара", "f"), "hedgehog": ("Їжак", "m"), "elephant": ("Слон", "m"), "firefly": ("Світлячок", "m"),
    "giant_isopod": ("Велетенська рівнонога", "f"), "giraffe": ("Жирафа", "f"), "great_white_shark": ("Велика біла акула", "f"),
    "hippo": ("Бегемот", "m"), "jellyfish": ("Медуза", "f"), "komodo_dragon": ("Комодський варан", "m"), "lion": ("Лев", "m"),
    "lizard": ("Ящірка", "f"), "mammoth": ("Мамонт", "m"), "mole": ("Кріт", "m"), "ostrich": ("Страус", "m"), "piranha": ("Піранья", "f"),
    "rat": ("Щур", "m"), "ray": ("Скат", "m"), "rhino": ("Носоріг", "m"), "scorpion": ("Скорпіон", "m"), "snail": ("Равлик", "m"),
    "snake": ("Змія", "f"), "tiger": ("Тигр", "m"), "tortoise": ("Черепаха", "f"), "turkey": ("Індик", "m"), "whale": ("Кит", "m"),
}
# sound -> (masculine, feminine)
VERBS = {
    "hurt": ("отримує шкоду", "отримує шкоду"), "death": ("гине", "гине"), "swim": ("пливе", "пливе"), "flop": ("б'ється на суші", "б'ється на суші"),
    "idle": ("булькає", "булькає"), "ambient": None, "sleep": ("хропе", "хропе"), "eat": ("їсть", "їсть"), "attack": ("нападає", "нападає"),
}
AMBIENT = {"ant": "цокотить", "black_bear": "бурчить", "butterfly": "пурхає", "clam": "булькає", "crab": "клацає", "capybara": "пищить",
           "hedgehog": "пирхає", "firefly": "дзижчить", "great_white_shark": "гарчить", "komodo_dragon": "шипить", "lizard": "цвірінчить",
           "mammoth": "сурмить", "mole": "пищить", "rat": "пищить", "scorpion": "цокотить", "snail": "хлюпає", "snake": "повзе",
           "tiger": "гарчить", "tortoise": "сопе", "turkey": "белькоче", "whale": "співає"}
SPECIAL = {
    "naturalist.subtitles.entity.anglerfish.idle": "Вудильник булькає",
    "naturalist.subtitles.entity.black_bear.sniff": "Чорний ведмідь нюхає",
    "naturalist.subtitles.entity.clam.launch": "Молюск клацає стулками",
    "naturalist.subtitles.entity.crab.pincer": "Краб клацає клешнею",
    "naturalist.subtitles.entity.hedgehog.hide": "Їжак згортається",
    "naturalist.subtitles.entity.hedgehog.unhide": "Їжак розгортається",
    "naturalist.subtitles.entity.giant_isopod.roll": "Велетенська рівнонога згортається",
    "naturalist.subtitles.entity.great_white_shark.attack": "Велика біла акула кусає",
    "naturalist.subtitles.entity.jellyfish.idle": "Медуза пульсує",
    "naturalist.subtitles.entity.komodo_dragon.attack": "Комодський варан кусає",
    "naturalist.subtitles.entity.mole.dig_down": "Кріт заривається",
    "naturalist.subtitles.entity.mole.dig_up": "Кріт вилазить",
    "naturalist.subtitles.entity.mole.peek": "Кріт визирає",
    "naturalist.subtitles.entity.ostrich.attack": "Страус б'є ногою",
    "naturalist.subtitles.entity.ostrich.bury": "Страус ховає голову",
    "naturalist.subtitles.entity.piranha.idle": "Піранья булькає",
    "naturalist.subtitles.entity.rat.swim": "Щур пливе",
    "naturalist.subtitles.entity.ray.idle": "Скат булькає",
    "naturalist.subtitles.entity.scorpion.attack": "Скорпіон жалить",
    "naturalist.subtitles.entity.tiger.attack": "Тигр ричить",
    "naturalist.subtitles.entity.tiger.prey": "Тигр пирхає",
    "naturalist.subtitles.entity.tortoise.hide": "Черепаха ховається в панцир",
    "naturalist.subtitles.entity.tortoise.shell_block": "Панцир черепахи блокує удар",
    "naturalist.subtitles.entity.tortoise.thud": "Черепаха гупає",
    "naturalist.subtitles.entity.turkey.peck": "Індик дзьобає",
    "naturalist.subtitles.entity.whale.blowhole": "Кит пускає фонтан",
}

T = {
    "_comment.advancements": " ДОСЯГНЕННЯ ", "_comment.blocks": " БЛОКИ ", "_comment.entities": " ІСТОТИ ",
    "_comment.effects": " ЗІЛЛЯ / ЕФЕКТИ ", "_comment.items": " ПРЕДМЕТИ ", "_comment.tooltips": " ПІДКАЗКИ ",
    "_comment.configuration": " НАЛАШТУВАННЯ ", "_comment.whistle_knapsack": " СВИСТОК / РЮКЗАЧОК ", "_comment.subtitles": " СУБТИТРИ ІСТОТ ",
    "block.naturalist.ant_hill": "Мурашник", "block.naturalist.blue_starfish": "Синя морська зірка",
    "block.naturalist.orange_starfish": "Помаранчева морська зірка", "block.naturalist.purple_starfish": "Фіолетова морська зірка",
    "block.naturalist.red_starfish": "Червона морська зірка", "block.naturalist.snail_shell": "Мушля равлика",
    "death.attack.naturalist.hedgehog_throw": "%1$s загинув, коли в нього кинули їжака: %2$s",
    "entity.naturalist.anglerfish": "Вудильник", "entity.naturalist.ant": "Мураха", "entity.naturalist.bird": "Птах",
    "entity.naturalist.black_bear": "Чорний ведмідь", "entity.naturalist.blobfish": "Риба-крапля", "entity.naturalist.carried_food": "Їжа в лапах",
    "entity.naturalist.capybara": "Капібара", "entity.naturalist.clam": "Молюск", "entity.naturalist.crab": "Краб",
    "entity.naturalist.desert_scorpion": "Пустельний скорпіон", "entity.naturalist.dirt_trail": "Земляний слід",
    "entity.naturalist.giant_isopod": "Велетенська рівнонога", "entity.naturalist.great_white_shark": "Велика біла акула",
    "entity.naturalist.hedgehog": "Їжак", "entity.naturalist.jellyfish": "Медуза", "entity.naturalist.jungle_scorpion": "Тропічний скорпіон",
    "entity.naturalist.komodo_dragon": "Комодський варан", "entity.naturalist.mammoth": "Мамонт", "entity.naturalist.mole": "Кріт",
    "entity.naturalist.piranha": "Піранья", "entity.naturalist.rat": "Щур", "entity.naturalist.ray": "Скат",
    "entity.naturalist.starfish": "Морська зірка", "entity.naturalist.tiger": "Тигр", "entity.naturalist.turkey": "Індик",
    "entity.naturalist.whale": "Кит",
    "effect.naturalist.antivenom": "Протиотрута",
    "item.minecraft.lingering_potion.effect.antivenom": "Тривке зілля протиотрути", "item.minecraft.potion.effect.antivenom": "Зілля протиотрути",
    "item.minecraft.splash_potion.effect.antivenom": "Вибухове зілля протиотрути", "item.minecraft.tipped_arrow.effect.antivenom": "Стріла протиотрути",
    "item.naturalist.anglerfish": "Вудильник", "item.naturalist.anglerfish_bucket": "Відро з вудильником",
    "item.naturalist.ant": "Мураха", "item.naturalist.blobfish": "Риба-крапля", "item.naturalist.blobfish_bucket": "Відро з рибою-краплею",
    "item.naturalist.bushmeat": "Дичина", "item.naturalist.dragonfly": "Бабка", "item.naturalist.firefly": "Світлячок",
    "item.naturalist.bee": "Бджола", "item.naturalist.silverfish": "Срібна рибка", "item.naturalist.endermite": "Ендерміт",
    "item.naturalist.clam_meat": "М'ясо молюска", "item.naturalist.cooked_anglerfish": "Смажений вудильник",
    "item.naturalist.cooked_blobfish": "Смажена риба-крапля", "item.naturalist.cooked_bushmeat": "Смажена дичина",
    "item.naturalist.cooked_clam_meat": "Смажене м'ясо молюска", "item.naturalist.cooked_crab_meat": "Смажене м'ясо краба",
    "item.naturalist.cooked_mammoth_meat": "Смажене м'ясо мамонта", "item.naturalist.cooked_piranha": "Смажена піранья",
    "item.naturalist.crab": "Краб", "item.naturalist.crab_meat": "М'ясо краба", "item.naturalist.duck_bucket": "Відро з каченям",
    "item.naturalist.fat": "Жир", "item.naturalist.fur": "Хутро", "item.naturalist.giant_isopod_bucket": "Відро з велетенською рівноногою",
    "item.naturalist.hide": "Шкура", "item.naturalist.hedgehog": "Їжак", "item.naturalist.jellyfish_bucket": "Відро з медузою",
    "item.naturalist.knapsack": "Рюкзачок для звірят", "item.naturalist.mammoth_meat": "Сире м'ясо мамонта", "item.naturalist.morsel": "Ласий шматок",
    "item.naturalist.music_disc_death_by_hogs": "Платівка", "item.naturalist.music_disc_wild_ones": "Платівка",
    "jukebox_song.naturalist.death_by_hogs": "Ninni - Death by Hogs", "jukebox_song.naturalist.wild_ones": "Chips the Cat - Wild Ones",
    "painting.naturalist.bear_necessities.author": ".inzaenity", "painting.naturalist.bear_necessities.title": "Ведмежі потреби",
    "item.naturalist.piranha": "Піранья", "item.naturalist.piranha_bucket": "Відро з піраньєю", "item.naturalist.queen_ant": "Мурашина королева",
    "item.naturalist.rat": "Щур", "item.naturalist.ray_bucket": "Відро зі скатом", "item.naturalist.scorpion": "Скорпіон",
    "item.naturalist.scorpion_poison_gland": "Отруйна залоза скорпіона", "item.naturalist.snail": "Равлик",
    "item.naturalist.starfish_bucket": "Відро з морською зіркою", "item.naturalist.tooth": "Зуб", "item.naturalist.whistle": "Свисток",
    "tooltip.naturalist.anglerfish_glow": "Світний", "tooltip.naturalist.anglerfish_red": "Червоний",
    "tooltip.naturalist.bass_small": "Малий", "tooltip.naturalist.bass_medium": "Середній", "tooltip.naturalist.bass_large": "Великий",
    "tooltip.naturalist.crab_blue": "Синій", "tooltip.naturalist.crab_brown": "Бурий", "tooltip.naturalist.crab_orange": "Помаранчевий",
    "tooltip.naturalist.crab_red": "Червоний", "tooltip.naturalist.crab_yellow": "Жовтий",
    "tooltip.naturalist.giant_isopod_blue": "Синя", "tooltip.naturalist.giant_isopod_brown": "Бура",
    "tooltip.naturalist.jade_green_swallowtail": "Нефритовий махаон", "tooltip.naturalist.purple_emperor": "Райдужниця велика",
    "tooltip.naturalist.hedgehog_brown": "Бурий", "tooltip.naturalist.hedgehog_dark": "Темний", "tooltip.naturalist.hedgehog_white": "Білий",
    "tooltip.naturalist.rat_black": "Чорний", "tooltip.naturalist.rat_brown": "Бурий", "tooltip.naturalist.rat_white": "Білий",
    "tooltip.naturalist.ray_eagle_ray": "Орляк", "tooltip.naturalist.ray_mobula_ray": "Мобула", "tooltip.naturalist.ray_stingray": "Хвостокол",
    "tooltip.naturalist.red_admiral": "Адмірал",
    "naturalist.configuration.aquatic_mobs": "Водні істоти", "naturalist.configuration.behavior": "Поведінка",
    "naturalist.configuration.bird_head_slow_falling": "Повільне падіння з птахом", "naturalist.configuration.bugs": "Комахи",
    "naturalist.configuration.disable_mobs": "Вимкнення істот", "naturalist.configuration.glow_goop_tooltip": "Підказка світного слизу",
    "naturalist.configuration.land_mobs": "Наземні істоти", "naturalist.configuration.parrot_flight": "Політ папуги",
    "naturalist.configuration.remove_all_bugs": "Прибрати всіх комах", "naturalist.configuration.snail_crushing": "Равликів можна розчавити",
    "naturalist.whistle.message": "%1$s тепер: %2$s.", "naturalist.whistle.following": "йде за вами", "naturalist.whistle.wandering": "гуляє",
    "naturalist.subtitles.item.whistle": "Свисток свистить", "naturalist.subtitles.item.knapsack.pickup": "Рюкзачок ловить звірятко",
    "naturalist.subtitles.item.knapsack.place": "Рюкзачок випускає звірятко",
    "tag.item.minecraft.meat": "М'ясо", "tag.item.naturalist.eggs": "Яйця", "tag.item.naturalist.shears": "Ножиці",
    "tag.item.naturalist.bear_furs": "Ведмеже хутро", "tag.item.naturalist.crab_food": "Їжа крабів", "tag.item.naturalist.rat_food": "Їжа щурів",
    "tag.item.naturalist.giant_isopod_food": "Їжа велетенських рівноногих",
}
REMOVE = {"alligator": "алігатора", "anglerfish": "вудильника", "ant": "мурах", "bass": "окуня", "bear": "ведмедя", "bird": "птахів",
          "black_bear": "чорного ведмедя", "blobfish": "рибу-краплю", "boar": "кабана", "butterfly": "метеликів", "capybara": "капібару",
          "catfish": "сома", "clam": "молюска", "crab": "краба", "deer": "оленя", "desert_scorpion": "пустельного скорпіона",
          "dragonfly": "бабок", "duck": "качок", "elephant": "слона", "firefly": "світлячків", "forest_fox": "лісову лисицю",
          "forest_rabbit": "лісового кролика", "giant_isopod": "велетенську рівноногу", "giraffe": "жирафу",
          "great_white_shark": "велику білу акулу", "hedgehog": "їжака", "hippo": "бегемота", "jellyfish": "медуз",
          "jungle_scorpion": "тропічного скорпіона", "komodo_dragon": "комодського варана", "lion": "лева", "lizard": "ящірок",
          "mammoth": "мамонта", "mole": "крота", "ostrich": "страуса", "piranha": "піранью", "rat": "щурів", "ray": "скатів",
          "rhino": "носорога", "snail": "равликів", "snake": "змій", "starfish": "морських зірок", "tiger": "тигра",
          "tortoise": "черепаху", "turkey": "індика", "vulture": "грифа", "whale": "кита", "zebra": "зебру"}
for k, v in REMOVE.items():
    T[f"naturalist.configuration.{k}_removed"] = "Прибрати " + v
FOOD = {"alligator": "алігаторів", "bird": "птахів", "boar": "кабанів", "capybara": "капібар", "duck": "качок", "giraffe": "жираф",
        "great_white_shark": "великих білих акул", "hedgehog": "їжаків", "komodo_dragon": "комодських варанів", "lion": "левів",
        "ostrich": "страусів", "tiger": "тигрів", "turkey": "індиків"}
for k, v in FOOD.items():
    T[f"tag.item.naturalist.{k}_food_items"] = "Їжа " + v
for k, v in {"bear": "ведмедів", "lizard": "ящірок", "snake": "змій", "tortoise": "черепах"}.items():
    T[f"tag.item.naturalist.{k}_tempt_items"] = "Приманка для " + v
T["tag.item.naturalist.snake_tame_items"] = "Приручення змій"
EGGS = {"anglerfish": "вудильника", "ant": "мурахи", "bird": "птаха", "black_bear": "чорного ведмедя", "blobfish": "риби-краплі",
        "capybara": "капібари", "clam": "молюска", "crab": "краба", "desert_scorpion": "пустельного скорпіона",
        "giant_isopod": "велетенської рівноногої", "great_white_shark": "великої білої акули", "hedgehog": "їжака", "jellyfish": "медузи",
        "jungle_scorpion": "тропічного скорпіона", "komodo_dragon": "комодського варана", "mammoth": "мамонта", "mole": "крота",
        "piranha": "піраньї", "rat": "щура", "ray": "ската", "starfish": "морської зірки", "tiger": "тигра", "turkey": "індика", "whale": "кита"}
for k, v in EGGS.items():
    T[f"item.naturalist.{k}_spawn_egg"] = "Яйце виклику " + v

src = json.load(open(os.path.join(HERE, "..", "..", "i18n", "src", "naturalist.json"), encoding="utf-8"))
for key in src:
    if key.startswith("naturalist.subtitles.entity.") and key not in T:
        if key in SPECIAL:
            T[key] = SPECIAL[key]; continue
        _, _, _, animal, sound = key.split(".")
        name, g = ANIMALS[animal]
        verb = AMBIENT.get(animal, "подає голос") if sound == "ambient" else VERBS[sound][0 if g == "m" else 1]
        T[key] = f"{name} {verb}"
missing = [k for k in src if k not in T]
if missing:
    raise SystemExit("untranslated: " + ", ".join(missing))
json.dump({"naturalist": {k: T[k] for k in src}}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("written", len(src))
