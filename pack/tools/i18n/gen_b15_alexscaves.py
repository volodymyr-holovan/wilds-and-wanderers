"""Generates i18n/batches/b15_alexscaves.json (Candy Cavity update of Alex's Caves)."""
import json, os, re

HERE = os.path.dirname(__file__)
SRC = json.load(open(os.path.join(HERE, "..", "..", "i18n", "src", "alexscaves.json"), encoding="utf-8"))
OUT = os.path.join(HERE, "..", "..", "i18n", "batches", "b15_alexscaves.json")

COL_M = {"white": "Білий", "orange": "Помаранчевий", "magenta": "Пурпуровий", "light_blue": "Блакитний", "yellow": "Жовтий", "lime": "Лаймовий",
         "pink": "Рожевий", "gray": "Сірий", "light_gray": "Світло-сірий", "cyan": "Бірюзовий", "purple": "Фіолетовий", "blue": "Синій",
         "brown": "Коричневий", "green": "Зелений", "red": "Червоний", "black": "Чорний"}


def fem(adj):
    return adj[:-2] + ("я" if adj.endswith("ій") else "а")


def neut(adj):
    return adj[:-2] + ("є" if adj.endswith("ій") else "е")


NAMES = {"sweetish_fish": "Солодка рибка", "caniac": "Цукроманіяк", "gumbeeper": "Жуйкоплюй", "candicorn": "Цукрооріг", "gum_worm": "Жуйкочерв",
         "caramel_cube": "Карамельний куб", "gummy_bear": "Мармеладний ведмідь", "licowitch": "Лакрична відьма", "gingerbread_man": "Пряничний чоловічок"}
EGG = {"sweetish_fish": "солодкої рибки", "caniac": "цукроманіяка", "gumbeeper": "жуйкоплюя", "candicorn": "цукрооріга", "gum_worm": "жуйкочерва",
       "caramel_cube": "карамельного куба", "gummy_bear": "мармеладного ведмедя", "licowitch": "лакричної відьми", "gingerbread_man": "пряничного чоловічка"}

T = {
    "attribute.name.alexscaves.swim_speed": "Швидкість плавання", "attribute.name.alexscaves.entity_gravity": "Тяжіння",
    "itemGroup.alexscaves.candy_cavity": "Цукеркова порожнина", "block.alexscaves.hologram_projector.scale": "Розмір голограми: %sx",
    "block.alexscaves.ambersol_light": "Світло амберсолу", "block.alexscaves.potted_underweed": "Підземна трава в горщику",
    "block.alexscaves.potted_thornwood_branch": "Гілка тернового дерева в горщику", "block.alexscaves.block_of_chocolate": "Блок шоколаду",
    "block.alexscaves.block_of_polished_chocolate": "Блок полірованого шоколаду", "block.alexscaves.block_of_chiseled_chocolate": "Блок різьбленого шоколаду",
    "block.alexscaves.block_of_frosted_chocolate": "Блок глазурованого шоколаду", "block.alexscaves.block_of_frosting": "Блок глазурі",
    "block.alexscaves.block_of_vanilla_frosting": "Блок ванільної глазурі", "block.alexscaves.block_of_chocolate_frosting": "Блок шоколадної глазурі",
    "block.alexscaves.sweet_puff": "Солодкий пуф", "block.alexscaves.cake_layer": "Корж", "block.alexscaves.dough_block": "Блок тіста",
    "block.alexscaves.cookie_block": "Блок печива", "block.alexscaves.wafer_cookie_block": "Блок вафельного печива",
    "block.alexscaves.wafer_cookie_stairs": "Сходи з вафельного печива", "block.alexscaves.wafer_cookie_slab": "Плита з вафельного печива",
    "block.alexscaves.wafer_cookie_wall": "Стіна з вафельного печива", "block.alexscaves.licoroot": "Лакричний корінь",
    "block.alexscaves.licoroot_vine": "Лоза лакричного кореня", "block.alexscaves.licoroot_sprout": "Паросток лакричного кореня",
    "block.alexscaves.small_peppermint": "Мала м'ятна цукерка", "block.alexscaves.large_peppermint": "Велика м'ятна цукерка",
    "block.alexscaves.vanilla_ice_cream": "Ванільне морозиво", "block.alexscaves.chocolate_ice_cream": "Шоколадне морозиво",
    "block.alexscaves.sweetberry_ice_cream": "Ягідне морозиво", "block.alexscaves.sundae": "Санді", "block.alexscaves.sprinkles": "Посипка",
    "block.alexscaves.giant_sweetberry": "Велетенська солодка ягода", "block.alexscaves.candy_cane": "Карамельна тростина",
    "block.alexscaves.candy_cane_block": "Блок карамельної тростини", "block.alexscaves.chiseled_candy_cane_block": "Різьблений блок карамельної тростини",
    "block.alexscaves.stripped_candy_cane_block": "Обтесаний блок карамельної тростини", "block.alexscaves.candy_cane_pole": "Стовп карамельної тростини",
    "block.alexscaves.stripped_candy_cane_pole": "Обтесаний стовп карамельної тростини", "block.alexscaves.lollipop_bunch": "Гроно льодяників",
    "block.alexscaves.frostmint": "Морозна м'ята", "block.alexscaves.sugar_glass": "Цукрове скло", "block.alexscaves.purple_soda": "Фіолетова газованка",
    "block.alexscaves.sundrop": "Сонячна крапля", "block.alexscaves.gobthumper": "Тупотун", "block.alexscaves.conversion_crucible": "Тигель перетворення",
    "block.alexscaves.gingerbread_block": "Пряниковий блок", "block.alexscaves.gingerbread_stairs": "Пряникові сходи",
    "block.alexscaves.gingerbread_slab": "Пряникова плита", "block.alexscaves.gingerbread_wall": "Пряникова стіна",
    "block.alexscaves.gingerbread_door": "Пряникові двері", "block.alexscaves.gingerbarrel": "Пряникова бочка",
    "block.alexscaves.frosted_gingerbread_block": "Глазурований пряниковий блок", "block.alexscaves.frosted_gingerbread_stairs": "Глазуровані пряникові сходи",
    "block.alexscaves.frosted_gingerbread_slab": "Глазурована пряникова плита", "block.alexscaves.frosted_gingerbread_wall": "Глазурована пряникова стіна",
    "block.alexscaves.frosted_gingerbread_door": "Глазуровані пряникові двері", "block.alexscaves.gingerbread_bricks": "Пряникова цегла",
    "block.alexscaves.gingerbread_brick_stairs": "Сходи з пряникової цегли", "block.alexscaves.gingerbread_brick_slab": "Плита з пряникової цегли",
    "block.alexscaves.gingerbread_brick_wall": "Стіна з пряникової цегли", "block.alexscaves.frosted_gingerbread_bricks": "Глазурована пряникова цегла",
    "block.alexscaves.frosted_gingerbread_brick_stairs": "Сходи з глазурованої пряникової цегли",
    "block.alexscaves.frosted_gingerbread_brick_slab": "Плита з глазурованої пряникової цегли",
    "block.alexscaves.frosted_gingerbread_brick_wall": "Стіна з глазурованої пряникової цегли", "block.alexscaves.confection_oven": "Кондитерська піч",
    "item.alexscaves.icon_item": "Значок досягнення", "item.alexscaves.effect_item": "Значок ефекту",
    "item.alexscaves.purple_soda_bucket": "Відро фіолетової газованки", "item.alexscaves.purple_soda_bottle": "Пляшка фіолетової газованки",
    "item.alexscaves.hot_chocolate_bottle": "Пляшка гарячого шоколаду", "item.alexscaves.vanilla_ice_cream_scoop": "Кулька ванільного морозива",
    "item.alexscaves.chocolate_ice_cream_scoop": "Кулька шоколадного морозива", "item.alexscaves.sweetberry_ice_cream_scoop": "Кулька ягідного морозива",
    "item.alexscaves.sundae": "Санді", "item.alexscaves.sharpened_candy_cane": "Загострена карамельна тростина",
    "item.alexscaves.peppermint_powder": "М'ятна пудра", "item.alexscaves.rainbounce_boots": "Веселкострибні чоботи",
    "item.alexscaves.gumball_pile": "Купка жуйок", "item.alexscaves.shot_gum": "Жуйкостріл", "item.alexscaves.caramel": "Карамель",
    "item.alexscaves.caramel_apple": "Яблуко в карамелі", "item.alexscaves.candy_cane_hook": "Гак з карамельної тростини",
    "item.alexscaves.sweet_tooth": "Солодкий зуб", "item.alexscaves.radiant_essence": "Сяйлива есенція",
    "item.alexscaves.licowitch_radiant_essence": "Сяйлива есенція",
    "item.alexscaves.licowitch_radiant_essence.desc": "Придивившись, ви розумієте, що замовили цю есенцію на Temu",
    "item.alexscaves.sack_of_sating": "Мішок ситості", "item.alexscaves.sugar_staff": "Цукровий посох", "item.alexscaves.sugar_staff_inventory": "Цукровий посох",
    "item.alexscaves.gingerbread_crumbs": "Пряникові крихти", "item.alexscaves.gingerbread_helmet": "Пряниковий шолом",
    "item.alexscaves.gingerbread_chestplate": "Пряниковий нагрудник", "item.alexscaves.gingerbread_leggings": "Пряникові наголінники",
    "item.alexscaves.gingerbread_boots": "Пряникові чоботи", "item.alexscaves.purple_soda_bottle_rocket": "Ракета з пляшки газованки",
    "item.alexscaves.frostmint_spear": "Спис з морозної м'яти", "item.alexscaves.frostmint_spear_inventory": "Спис з морозної м'яти",
    "item.alexscaves.disc_fragment_tasty": "Фрагмент платівки", "item.alexscaves.disc_fragment_tasty.desc": "Платівка — Tasty",
    "item.alexscaves.music_disc_tasty": "Платівка", "item.alexscaves.music_disc_tasty.desc": "Ninni — Tasty",
    "item.alexscaves.alex_meal": "Обід Алекса", "item.alexscaves.alex_meal.desc": "Ти справді так живеш?",
    "item.alexscaves.biome_treat": "Біомні ласощі", "item.alexscaves.biome_treat.desc": "Без прив'язки",
    "item.alexscaves.jelly_bean": "Желейна цукерка", "item.alexscaves.jelly_bean.desc": "Смак: %s",
    "rarity.alexscaves.nuclear.name": "Ядерний", "rarity.alexscaves.demonic.name": "Демонічний", "rarity.alexscaves.rainbow.name": "Веселковий",
    "entity.alexscaves.depth_charge": "Глибинна бомба", "entity.alexscaves.thrown_ice_cream_scoop": "Кулька морозива",
    "entity.alexscaves.gumball": "Жуйка", "entity.alexscaves.gum_worm_segment": "Жуйкочерв", "entity.alexscaves.melted_caramel": "Розтоплена карамель",
    "entity.alexscaves.spinning_peppermint": "Крутна м'ятна цукерка", "entity.alexscaves.sugar_staff_hex": "Чари цукрового посоха",
    "entity.alexscaves.falling_frostmint": "Падуча морозна м'ята", "entity.alexscaves.candy_cane_hook": "Гак з карамельної тростини",
    "entity.alexscaves.soda_bottle_rocket": "Ракета з пляшки газованки", "entity.alexscaves.frostmint_spear": "Спис з морозної м'яти",
    "fluid.alexscaves.purple_soda": "Фіолетова газованка", "fluid_type.alexscaves.purple_soda": "Фіолетова газованка",
    "death.attack.gumball_0": "%s розкусив жуйку", "death.attack.gumball_1": "%s забито жуйками",
    "death.attack.gumball_0.entity": "%s змусили розкусити жуйку — це зробив %s", "death.attack.gumball_1.entity": "%s забито жуйками від %s",
    "death.attack.intentional_game_design_0": "%s відкрив задуманий дизайн гри",
    "death.attack.intentional_game_design_1": "%s спробував те, чого не слід було",
    "death.attack.intentional_game_design_0.entity": "%s відкрив задуманий дизайн гри завдяки %s",
    "death.attack.intentional_game_design_1.entity": "%s спробував те, чого не слід було, через %s",
    "effect.alexscaves.sugar_rush": "Цукровий шал",
    "item.minecraft.potion.effect.sugar_rush": "Зілля цукрового шалу", "item.minecraft.potion.effect.long_sugar_rush": "Зілля цукрового шалу",
    "item.minecraft.splash_potion.effect.sugar_rush": "Вибухове зілля цукрового шалу",
    "item.minecraft.splash_potion.effect.long_sugar_rush": "Вибухове зілля цукрового шалу",
    "item.minecraft.tipped_arrow.effect.sugar_rush": "Стріла цукрового шалу", "item.minecraft.tipped_arrow.effect.long_sugar_rush": "Стріла цукрового шалу",
    "item.minecraft.lingering_potion.effect.sugar_rush": "Осідальне зілля цукрового шалу",
    "item.minecraft.lingering_potion.effect.long_sugar_rush": "Осідальне зілля цукрового шалу",
    "item.minecraft.potion.effect.strong_hunger": "Зілля голоду", "item.minecraft.splash_potion.effect.strong_hunger": "Вибухове зілля голоду",
    "item.minecraft.tipped_arrow.effect.strong_hunger": "Стріла голоду", "item.minecraft.lingering_potion.effect.strong_hunger": "Осідальне зілля голоду",
    "item.alexscaves.polarity_armor_trim_smithing_template": "Ковальський шаблон: оздоба «Полярність»",
    "biome.alexscaves.candy_cavity": "Цукеркова порожнина", "book.alexscaves.candy_cavity": "Цукеркова порожнина",
    "advancements.alexscaves.discover_candy_cavity.title": "Чиста уява",
    "advancements.alexscaves.discover_candy_cavity.desc": "Потрапте в Цукеркову порожнину",
    "advancements.alexscaves.walk_on_rock_candy.title": "Дорога з жовтої цегли",
    "advancements.alexscaves.walk_on_rock_candy.desc": "Пройдіться дорогою з льодяників",
    "advancements.alexscaves.eat_block_of_chocolate.title": "Шоко-блок",
    "advancements.alexscaves.eat_block_of_chocolate.desc": "З'їжте цілий блок шоколаду... і чому ви досі голодні?",
    "advancements.alexscaves.enter_purple_soda.title": "Газоване купання",
    "advancements.alexscaves.enter_purple_soda.desc": "Гарненько поплавайте в річці фіолетової газованки",
    "advancements.alexscaves.sweetish_fish_bucket.title": "Мармеладна юшка",
    "advancements.alexscaves.sweetish_fish_bucket.desc": "Спіймайте солодку рибку у відро",
    "advancements.alexscaves.gelatin.title": "А де Гупі?", "advancements.alexscaves.gelatin.desc": "Зваріть желатин із солодкої рибки",
    "advancements.alexscaves.gummy_rings.title": "Колекція рятувальних кіл",
    "advancements.alexscaves.gummy_rings.desc": "Зберіть жувальні кільця всіх п'яти кольорів",
    "advancements.alexscaves.eat_candy_cane.title": "Святкова пора", "advancements.alexscaves.eat_candy_cane.desc": "З'їжте карамельну тростину",
    "advancements.alexscaves.defeat_caniac.title": "Цукроманіяка більше нема",
    "advancements.alexscaves.defeat_caniac.desc": "Здолайте цукроманіяка — кістяне чудовисько з чистої м'яти",
    "advancements.alexscaves.rainbounce_boots.title": "Без слизу не обслуговуємо",
    "advancements.alexscaves.rainbounce_boots.desc": "Скрафтіть веселкострибні чоботи й стрибайте, де заманеться",
    "advancements.alexscaves.frostmint_explosion.title": "Вибухове поєднання",
    "advancements.alexscaves.frostmint_explosion.desc": "Опиніться поруч, коли морозна м'ята падає у фіолетову газованку",
    "advancements.alexscaves.frostmint_spear.title": "М'ятний спис",
    "advancements.alexscaves.frostmint_spear.desc": "Скрафтіть спис з морозної м'яти й карамельних тростин, що заморожує кількох ворогів одним ударом",
    "advancements.alexscaves.candy_cane_hook.title": "Підсів на гачок",
    "advancements.alexscaves.candy_cane_hook.desc": "Скрафтіть гак з карамельної тростини, щоб легко пересуватися вгору й униз",
    "advancements.alexscaves.defeat_gumbeeper.title": "Я знайшов щілину для монет",
    "advancements.alexscaves.defeat_gumbeeper.desc": "Здолайте жуйкоплюя, поки він не вибухнув стрибучими жуйками",
    "advancements.alexscaves.shot_gum.title": "А жуйки в мене скінчилися",
    "advancements.alexscaves.shot_gum.desc": "Скрафтіть жуйкостріл — зброю, що стріляє двома стрибучими жуйками водночас",
    "advancements.alexscaves.caramel.title": "Розгорнута цукерка",
    "advancements.alexscaves.caramel.desc": "Здобудьте карамель, здолавши карамельний куб",
    "advancements.alexscaves.caramel_apple.title": "День на ярмарку", "advancements.alexscaves.caramel_apple.desc": "Скрафтіть яблуко в карамелі",
    "advancements.alexscaves.tame_candicorn.title": "Веселки й цукрооріжки",
    "advancements.alexscaves.tame_candicorn.desc": "Приручіть цукрооріга, нагодувавши його яблуком у карамелі",
    "advancements.alexscaves.sundae.title": "Ліниве недільне санді", "advancements.alexscaves.sundae.desc": "Скрафтіть санді. Тільки не захоплюйтеся",
    "advancements.alexscaves.feed_gummy_bear.title": "Мармеладний ведмідь, що ходить і говорить",
    "advancements.alexscaves.feed_gummy_bear.desc": "Напоїть мармеладного ведмедя зіллям і подивіться, як він впадає в сплячку",
    "advancements.alexscaves.jelly_bean.title": "Червоний смак — не моє",
    "advancements.alexscaves.jelly_bean.desc": "Отримайте желейну цукерку від мармеладного ведмедя, що після сплячки чухається об лакричний корінь",
    "advancements.alexscaves.gobthumper.title": "Ходи без ритму",
    "advancements.alexscaves.gobthumper.desc": "Скрафтіть тупотун — блок, що приваблює увагу величезної істоти...",
    "advancements.alexscaves.ride_gum_worm.title": "Сила десерту",
    "advancements.alexscaves.ride_gum_worm.desc": "Двома гаками з карамельної тростини водночас учепіться за жуйкочерва й керуйте велетнем",
    "advancements.alexscaves.defeat_gum_worm.title": "(Корична) спеція має текти",
    "advancements.alexscaves.defeat_gum_worm.desc": "Переможіть жуйкочерва в бою",
    "advancements.alexscaves.sack_of_sating.title": "Дивний предмет",
    "advancements.alexscaves.sack_of_sating.desc": "Скрафтіть мішок ситості, щоб завжди бути ситим",
    "advancements.alexscaves.sugar_rush.title": "Солодке-солодке прискорення",
    "advancements.alexscaves.sugar_rush.desc": "Отримайте цукровий шал, з'ївши забагато солодощів",
    "advancements.alexscaves.licowitch_tower.title": "Темна (шоколадна) вежа",
    "advancements.alexscaves.licowitch_tower.desc": "Знайдіть вежу лакричної відьми на вершині величезного лакричного кореня",
    "advancements.alexscaves.radiant_essence.title": "Таємний інгредієнт",
    "advancements.alexscaves.radiant_essence.desc": "Здобудьте сяйливу есенцію, здолавши лакричну відьму",
    "advancements.alexscaves.conversion_crucible.title": "Біомні реактори",
    "advancements.alexscaves.conversion_crucible.desc": "Здобудьте тигель перетворення",
    "advancements.alexscaves.convert_biome.title": "Мамо, дивись, без WorldEdit!",
    "advancements.alexscaves.convert_biome.desc": "Перетворіть один біом на інший тиглем перетворення",
    "advancements.alexscaves.convert_nether_biome.title": "З поверненням, реакторе Незеру",
    "advancements.alexscaves.convert_nether_biome.desc": "Створіть біом Незеру у Звичайному світі тиглем перетворення",
    "advancements.alexscaves.gingerbread_town.title": "Пряникове містечко", "advancements.alexscaves.gingerbread_town.desc": "Знайдіть пряникове містечко",
    "advancements.alexscaves.gingerbread_crumbs.title": "Ви чудовисько",
    "advancements.alexscaves.gingerbread_crumbs.desc": "Здобудьте пряникові крихти, поранивши пряничних чоловічків",
    "advancements.alexscaves.confection_oven.title": "Крихітна битва в блоці",
    "advancements.alexscaves.confection_oven.desc": "Скрафтіть кондитерську піч — блок, що створює маленьке військо, яке атакує все довкола",
    "advancements.alexscaves.gingerbread_armor.title": "Біжи, біжи щодуху",
    "advancements.alexscaves.gingerbread_armor.desc": "Здобудьте всі чотири частини пряникової броні від пряничних чоловічків, що збирають крадені цукерки в пряникових бочках",
    "enchantment.alexscaves.targeted_ricochet": "Прицільний рикошет",
    "enchantment.alexscaves.targeted_ricochet.desc": "Жуйки відскакують до найближчої цілі, влучивши в стіну чи істоту",
    "enchantment.alexscaves.triple_split": "Потрійний розкол",
    "enchantment.alexscaves.triple_split.desc": "Після першої цілі жуйка розпадається ще на три",
    "enchantment.alexscaves.bouncy_ball": "Стрибучий м'ячик", "enchantment.alexscaves.bouncy_ball.desc": "Жуйки відскакують ще двічі за кожен рівень",
    "enchantment.alexscaves.explosive_flavor": "Вибуховий смак",
    "enchantment.alexscaves.explosive_flavor.desc": "Жуйкостріл випускає одну повільну вибухову жуйку",
    "enchantment.alexscaves.far_flung": "Далекий кидок", "enchantment.alexscaves.far_flung.desc": "Під час замаху гак летить ще далі",
    "enchantment.alexscaves.sharp_cane": "Гостра тростина",
    "enchantment.alexscaves.sharp_cane.desc": "Гак завдає три одиниці шкоди за рівень, учепившись у ціль",
    "enchantment.alexscaves.straight_hook": "Прямий гак",
    "enchantment.alexscaves.straight_hook.desc": "Гак летить прямо вперед, тяжіння на нього не діє",
    "enchantment.alexscaves.spell_lasting": "Тривалі чари",
    "enchantment.alexscaves.spell_lasting.desc": "Чари тривають на три секунди довше за кожен рівень",
    "enchantment.alexscaves.peppermint_punting": "М'ятний удар",
    "enchantment.alexscaves.peppermint_punting.desc": "М'ятні цукерки відскакують прямо вперед, а не навколо нападника",
    "enchantment.alexscaves.humungous_hex": "Величезні чари",
    "enchantment.alexscaves.humungous_hex.desc": "Чари ширші на цілий блок за кожен рівень",
    "enchantment.alexscaves.multiple_mint": "Багато м'яти",
    "enchantment.alexscaves.multiple_mint.desc": "За кожен рівень з'являється ще одна м'ятна цукерка",
    "enchantment.alexscaves.seekcandy": "Цукерка-шукачка",
    "enchantment.alexscaves.seekcandy.desc": "М'ятні цукерки посоха летять до будь-якої істоти, на яку дивиться нападник",
}
SOUNDS = {"step": "Кроки", "place": "Блок розміщено", "break": "Блок зламано", "breaking": "Блок ламається"}
VERBS = {
    "purple_soda_idle": "Фіолетова газованка булькає", "purple_soda_submerge": "Фіолетова газованка огортає",
    "purple_soda_unsubmerge": "Фіолетова газованка відступає", "purple_soda_swim": "Фіолетова газованка хлюпає",
    "conversion_crucible_add": "Тигель перетворення булькає", "conversion_crucible_activate": "Тигель перетворення вмикається",
    "conversion_crucible_convert": "Тигель перетворення змінює біом", "gobthumper_thump": "Тупотун тупотить",
    "luxtructosaurus_snort": "Люкструктозавр пирхає", "sweetish_fish_hurt": "Солодка рибка хлюпає", "sweetish_fish_flop": "Солодка рибка підстрибує",
    "caniac_idle": "Цукроманіяк бурчить", "caniac_hurt": "Цукроманіяк деренчить", "caniac_death": "Цукроманіяк кришиться",
    "caniac_attack": "Цукроманіяк кидається", "caniac_swing": "Цукроманіяк замахується", "gumbeeper_death": "Жуйкоплюй розбивається",
    "gumbeeper_explode": "Жуйкоплюй вибухає", "gumbeeper_hurt": "Жуйкоплюй сичить", "candicorn_idle": "Цукрооріг ірже",
    "candicorn_hurt": "Цукрооріг стогне", "candicorn_death": "Цукрооріг верещить", "candicorn_step": "Цукрооріг цокає",
    "candicorn_gallop": "Цукрооріг скаче", "candicorn_charge_start": "Цукрооріг кидається в атаку", "candicorn_charge_loop": "Цукрооріг кидається в атаку",
    "gum_worm_idle": "Жуйкочерв реве", "gum_worm_hurt": "Жуйкочерв сичить", "gum_worm_death": "Жуйкочерв кричить", "gum_worm_attack": "Жуйкочерв кидається",
    "gum_worm_dig_loop": "Жуйкочерв риє", "gum_worm_dig_stop": "Жуйкочерв вилазить", "caramel_cube_attack": "Карамельний куб гупає",
    "caramel_cube_small": "Карамельний куб стрибає", "caramel_cube_big": "Карамельний куб стрибає", "gummy_bear_idle": "Мармеладний ведмідь гарчить",
    "gummy_bear_hurt": "Мармеладний ведмідь стогне", "gummy_bear_death": "Мармеладний ведмідь плаче", "gummy_bear_snore": "Мармеладний ведмідь хропе",
    "gummy_bear_eat": "Мармеладний ведмідь жує", "gummy_bear_step": "Мармеладний ведмідь підстрибує", "licowitch_idle": "Лакрична відьма регоче",
    "licowitch_hurt": "Лакрична відьма крекче", "licowitch_death": "Лакрична відьма кричить", "licowitch_cast_hex": "Лакрична відьма чаклує",
    "licowitch_cast_peppermint": "Лакрична відьма кидає м'ятні цукерки", "licowitch_cast_summon": "Лакрична відьма викликає чудовисько",
    "licowitch_celebrate": "Лакрична відьма радіє", "gingerbread_man_idle": "Пряничний чоловічок белькоче",
    "gingerbread_man_hurt": "Пряничний чоловічок кричить", "gingerbread_man_death": "Пряничний чоловічок кришиться",
    "gingerbread_man_attack": "Пряничний чоловічок переможно вигукує", "gingerbread_man_talks": "Пряничний чоловічок пліткує",
    "gumball_launch": "Жуйка вилітає", "gumball_hit": "Жуйка розбивається", "gumball_bounce": "Жуйка відскакує", "shotgum_shoot": "Жуйкостріл стріляє",
    "shotgum_reload": "Жуйкостріл перезаряджається", "shotgum_empty": "Жуйкостріл порожній", "candy_cane_hook_launch": "Гак вилітає",
    "candy_cane_hook_reel": "Гак підтягується", "rainbounce_boots_bounce": "Веселкострибні чоботи підстрибують",
    "sugar_staff_cast_peppermint": "Цукровий посох кидає м'ятну цукерку", "sugar_staff_cast_hex": "Цукровий посох чаклує",
    "frostmint_spear_throw": "Спис з морозної м'яти кинуто", "frostmint_spear_hit": "Спис з морозної м'яти влучає",
    "sugar_rush_enter": "Цукровий шал починається", "sugar_rush_exit": "Цукровий шал минає", "sugar_rush_loop": "Цукровий шал мерехтить",
}

out = dict(T)
for k in SRC:
    if k in out:
        continue
    m = re.match(r"block\.alexscaves\.rock_candy_([a-z_]+)$", k)
    if m:
        out[k] = f"{COL_M[m.group(1)]} льодяник"; continue
    m = re.match(r"block\.alexscaves\.gummy_ring_([a-z_]+)$", k)
    if m:
        out[k] = f"{neut(COL_M[m.group(1)])} жувальне кільце"; continue
    m = re.match(r"item\.alexscaves\.gelatin_([a-z_]+)$", k)
    if m:
        out[k] = f"{COL_M[m.group(1)]} желатин"; continue
    m = re.match(r"item\.alexscaves\.sweetish_fish_([a-z]+)_bucket$", k)
    if m:
        out[k] = "Відро із солодкою рибкою"; continue
    m = re.match(r"item\.alexscaves\.sweetish_fish_([a-z]+)$", k)
    if m:
        out[k] = f"{fem(COL_M[m.group(1)])} солодка рибка"; continue
    m = re.match(r"item\.alexscaves\.sweetish_fish_bucket\.desc_([a-z]+)$", k)
    if m:
        out[k] = fem(COL_M[m.group(1)]); continue
    m = re.match(r"item\.alexscaves\.spawn_egg_([a-z_]+)$", k)
    if m and m.group(1) in EGG:
        out[k] = "Яйце виклику " + EGG[m.group(1)]; continue
    m = re.match(r"entity\.alexscaves\.([a-z_]+)$", k)
    if m and m.group(1) in NAMES:
        out[k] = NAMES[m.group(1)]; continue
    m = re.match(r"alexscaves\.sound\.subtitle\.[a-z]+_candy_([a-z]+)$", k)
    if m:
        out[k] = SOUNDS[m.group(1)]; continue
    m = re.match(r"alexscaves\.sound\.subtitle\.([a-z_]+)$", k)
    if m and m.group(1) in VERBS:
        out[k] = VERBS[m.group(1)]
out = {k: v for k, v in out.items() if k in SRC}
missing = [k for k in SRC if k not in out]
json.dump({"alexscaves": out}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(out), "translated;", len(missing), "missing:", missing)
