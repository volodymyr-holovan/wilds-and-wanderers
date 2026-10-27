"""Batch 40: Small Ships (ships of every wood, the dockyard screen, cannons). Ship names are built from wood x type."""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "i18n", "src", "smallships.json")
OUT = os.path.join(HERE, "..", "..", "i18n", "batches", "b40_smallships.json")

# ship -> (Ukrainian noun, gender: m/f)
SHIPS = {"cog": ("ког", "m"), "galley": ("галера", "f"), "war_galley": ("бойова галера", "f"), "rowboat": ("веслувальний човен", "m"),
         "drakkar": ("драккар", "m"), "dhow": ("дау", "f"), "brigg": ("бриг", "m"), "caravel": ("каравела", "f"), "galleon": ("галеон", "m")}
WOODS = {  # wood -> (masculine, feminine adjective)
    "oak": ("Дубовий", "Дубова"), "spruce": ("Ялиновий", "Ялинова"), "birch": ("Березовий", "Березова"), "jungle": ("Тропічний", "Тропічна"),
    "acacia": ("Акацієвий", "Акацієва"), "dark_oak": ("Темнодубовий", "Темнодубова"), "mangrove": ("Мангровий", "Мангрова"),
    "cherry": ("Вишневий", "Вишнева"), "bamboo": ("Бамбуковий", "Бамбукова"), "warped": ("Викривлений", "Викривлена"),
    "crimson": ("Багряний", "Багряна"),
}
T = {
    "category.smallships": "Small Ships", "smallships.creative_mode_tab": "Small Ships",
    "key.smallships.ship_sail": "Поставити / згорнути вітрило", "key.smallships.higher_ship_sail": "Більше вітрила",
    "key.smallships.lower_ship_sail": "Менше вітрила", "key.smallships.ship_inventory": "Трюм корабля",
    "death.attack.cannon_ball": "%s загинув від гарматного пострілу.", "death.attack.cannon_ball.player": "%s загинув від гармати гравця %s.",
    "death.attack.ship_hit": "%s збив корабель.", "death.attack.ship_hit.player": "%s збив кораблем гравець %s.",
    "item.smallships.sail": "Вітрило", "item.smallships.cannon": "Корабельна гармата", "item.smallships.cannon_ball": "Гарматне ядро",
    "item.smallships.iron_scantlings": "Залізні шпангоути", "item.smallships.copper_plating": "Мідна обшивка",
    "item.smallships.cotton_sails": "Бавовняні вітрила",
    "tooltip.plane.damage": "Пошкодження: %s %%", "tooltip.plane.throttle": "Тяга: %s %%", "tooltip.plane.speed": "Швидкість: %s",
    "key.cannon_shoot": "Постріл з гармати", "key.ship_right": "Кермо праворуч", "key.ship_left": "Кермо ліворуч",
    "key.ship_backward": "Корабель назад", "key.ship_forward": "Корабель уперед",
    "subtitles.smallships.sail_move": "Вітрило рухається", "subtitles.smallships.sail_pull": "Вітрило напинається",
    "subtitles.smallships.cannon_shot": "Гармата стріляє", "subtitles.smallships.ship_hit": "Корабель отримує удар",
    "container.dockyard": "Верф", "block.smallships.dockyard": "Верф",
    "item.smallships.chained_shot": "Ланцюгове ядро", "item.smallships.grape_shot": "Картеч", "item.smallships.fine_grain_powder": "Дрібний порох",
    "item.smallships.chained_shot.tooltip": "На 25% менші дальність і шкода, але сильно рве вітрила",
    "item.smallships.grape_shot.tooltip": "Мала дальність, нищівна для екіпажу",
    "item.smallships.fine_grain_powder.tooltip": "Витрачається при перезарядці: +50% дальності",
    "gui.smallships.cannon_aim": "Приціл гармати: %s° / поворот: %s°",
    "gui.smallships.dockyard.build": "Збудувати корабель", "gui.smallships.dockyard.ship_type": "Корабель: %s",
    "gui.smallships.dockyard.wood_type": "Дерево: %s", "gui.smallships.dockyard.working": "Працюємо...",
    "gui.smallships.dockyard.missing_materials": "Бракує матеріалів!",
    "gui.smallships.dockyard.no_water_spot": "Поруч немає вільної води, досить великої для цього корабля!",
    "gui.smallships.dockyard.water_too_shallow": "Замілко — цьому кораблю потрібна глибина %s блоків!",
    "gui.smallships.dockyard.no_ship": "У радіусі 16 блоків немає корабля!",
    "gui.smallships.dockyard.modify_hint": "Знайдено найближчий корабель — покращення нижче",
    "gui.smallships.dockyard.cost": "Ціна: %s× %s", "gui.smallships.dockyard.click_install": "Клацніть, щоб поставити",
    "gui.smallships.dockyard.click_remove": "Клацніть, щоб зняти", "gui.smallships.dockyard.shield_slot": "Місце для щита %s (%s)",
    "gui.smallships.dockyard.shield": "Повісити щит", "gui.smallships.dockyard.shield_hint": "Займає наступне вільне місце, починаючи з носа",
    "gui.smallships.dockyard.no_ship_type": "Немає доступних кораблів",
    "gui.smallships.upgrade.iron_scantlings.description": "+%s%% міцності", "gui.smallships.upgrade.copper_plating.description": "+%s%% маневреності",
    "gui.smallships.upgrade.cotton_sails.description": "+%s%% швидкості", "gui.smallships.upgrade.iron_scantlings": "Залізні шпангоути",
    "gui.smallships.upgrade.cotton_sails": "Бавовняні вітрила", "gui.smallships.upgrade.copper_plating": "Мідна обшивка",
    "gui.smallships.ship.detach_hint": "Затисніть, щоб зняти",
    "key.smallships.cannon_aim": "Прицілитися гарматами (тримати)", "key.smallships.cannon_barrel_up": "Підняти ствол гармати",
    "key.smallships.cannon_barrel_down": "Опустити ствол гармати", "gui.smallships.dockyard.cannon_slot": "Місце для гармати %s (%s)",
    "gui.smallships.dockyard.starboard": "Правий борт", "gui.smallships.dockyard.port": "Лівий борт",
    "gui.smallships.dockyard.click_mount": "Клацніть, щоб поставити (1 гармата)",
    "gui.smallships.dockyard.ship_busy": "Над цим кораблем уже працює інша верф!",
    "commands.smallships.wind.set_direction": "Напрям вітру: %s° (північ = 0°, за годинниковою стрілкою)",
    "gui.smallships.dockyard.click_dye": "Клацніть, щоб пофарбувати вітрила (8 с роботи)",
    "gui.smallships.dockyard.click_banner": "Клацніть, щоб повісити цей прапор (8 с роботи)",
    "gui.smallships.upgrade.installed": "Встановлено", "gui.smallships.upgrade.not_installed": "Не встановлено",
    "gui.smallships.dockyard.repair": "Полагодити корабель", "gui.smallships.dockyard.repair_cost": "Ціна:",
    "commands.smallships.wind.set_strength": "Сила вітру: %s",
    "commands.smallships.wind.info": "Вітер: напрям %s° (північ = 0°, за годинниковою стрілкою), сила %s",
    "chat.smallships.update.available": "Вийшла нова версія Small Ships: %s (встановлено: %s)",
    "chat.smallships.update.download": "Завантажити оновлення %s", "chat.smallships.update.here": "тут",
    "tips.smallships.sail_toggle": "Поставити / згорнути вітрило", "tips.smallships.sail_adjust": "Більше / менше вітрила",
    "tips.smallships.row": "Веслувати", "tips.smallships.cannon_aim": "Прицілитися (тримати)", "tips.smallships.cannon_ammo": "Змінити набої",
    "tips.smallships.cannon_shoot": "Вогонь", "tips.smallships.inventory": "Трюм корабля", "tips.smallships.zoom": "Наблизити камеру",
    "tips.smallships.mouse_wheel": "Коліщатко миші",
    "gui.smallships.dockyard.stat.wind": "Вітер", "gui.smallships.dockyard.stat.head_wind": "Зустрічний", "gui.smallships.dockyard.stat.side_wind": "Боковий",
    "gui.smallships.dockyard.stat.tail_wind": "Попутний", "gui.smallships.dockyard.stat.oars": "Весла (від найбільшого)",
    "gui.smallships.dockyard.stat.penalties": "Поточні штрафи", "gui.smallships.dockyard.stat.cargo": "Вантаж",
    "gui.smallships.dockyard.stat.cannons": "Гармати", "gui.smallships.dockyard.stat.biome": "Біом",
    "gui.smallships.dockyard.stat.max_speed": "Найбільша швидкість", "gui.smallships.dockyard.tab.build": "Будування",
    "gui.smallships.dockyard.tab.modify": "Зміни", "gui.smallships.dockyard.tab.no_ship": "У радіусі 16 блоків корабля не знайдено",
    "gui.smallships.dockyard.section.upgrades": "Покращення", "gui.smallships.dockyard.section.cannons": "Гармати",
    "gui.smallships.dockyard.section.style": "Вигляд", "gui.smallships.dockyard.section.materials": "Матеріали",
    "gui.smallships.dockyard.more_materials": "ще +%s…", "gui.smallships.dockyard.stat.attributes": "Характеристики",
    "gui.smallships.dockyard.stat.special": "Особливості", "gui.smallships.dockyard.stat.turning": "Поворотність",
    "gui.smallships.dockyard.stat.hull_hp": "Міцність корпусу", "gui.smallships.dockyard.stat.sails_hp": "Міцність вітрил",
    "gui.smallships.dockyard.stat.shields": "Щити", "gui.smallships.dockyard.stat.shields_dmg": "Захист щитів",
    "gui.smallships.dockyard.stat.shields_max": "Найбільший захист", "gui.smallships.dockyard.stat.crew": "Екіпаж",
    "gui.smallships.dockyard.stat.penalty_cargo": "Штраф за вантаж", "gui.smallships.dockyard.stat.penalty_cannons": "Штраф за гармати",
    "gui.smallships.dockyard.stat.penalty_biome": "Біом", "gui.smallships.dockyard.apply": "Застосувати",
    "gui.smallships.dockyard.total_cost": "Усього:", "gui.smallships.dockyard.refund": "Повернення: %s %s",
    "gui.smallships.dockyard.no_damage": "Без пошкоджень", "gui.smallships.dockyard.repair_hull": "Полагодити корпус",
    "gui.smallships.dockyard.repair_sails": "Полагодити вітрила", "gui.smallships.dockyard.ship_name": "Назва корабля",
    "gui.smallships.dockyard.sail_color": "Вітрила: %s", "gui.smallships.dockyard.banner": "Прапор на щоглі",
    "gui.smallships.dockyard.sail_banner": "Прапор на вітрилі", "gui.smallships.dockyard.duration": "Час: %s",
    "gui.smallships.dockyard.stat.no_cargo_penalty": "Без штрафу за вантаж", "gui.smallships.dockyard.stat.no_cannon_penalty": "Без штрафу за гармати",
    "gui.smallships.dockyard.stat.no_biome_penalty": "Без штрафу за біом", "gui.smallships.dockyard.stat.ice_breaker": "Криголам",
    "gui.smallships.dockyard.stat.ramming": "Таран без пошкоджень",
    "gui.smallships.dockyard.about.smallships.cog": "Ког — одномачтове вітрильне судно, до чотирьох гармат, по дві з кожного борту. Попутний вітер дає йому сильну надбавку до швидкості, боковий — невелику. У холодних водах іде швидше.",
    "gui.smallships.dockyard.about.smallships.brigg": "Бриг — двомачтове вітрильне судно, до шести гармат, по три з кожного борту. Попутний вітер дає сильну надбавку до швидкості, боковий — помірну. У холодних водах іде швидше.",
    "gui.smallships.dockyard.about.smallships.galley": "Галера — одномачтове судно з вітрилом і веслами, до двох гармат, по одній з кожного борту. Зі згорнутим вітрилом на веслах іде майже на повній швидкості за будь-якого вітру. Під вітрилом боковий вітер дає сильну надбавку. Може таранити інші кораблі без шкоди для свого корпусу. У теплих водах іде швидше.",
    "gui.smallships.dockyard.about.smallships.drakkar": "Драккар — одномачтове судно з вітрилом і веслами. Зі згорнутим вітрилом на веслах іде приблизно на три чверті швидкості за будь-якого вітру. Під вітрилом попутний вітер дає сильну надбавку, боковий — невелику. Уздовж бортів можна повісити до десяти щитів, по п'ять з кожного боку, і кожен зменшує шкоду кораблю. Ламає звичайний лід. У холодних водах іде швидше.",
    "gui.smallships.dockyard.about.smallships.galleon": "Галеон — тримачтове вітрильне судно, до десяти гармат, по п'ять з кожного борту. Зберігає повну швидкість навіть з усіма гарматами. Попутний вітер дає сильну надбавку. Швидкість однакова в усіх біомах.",
    "gui.smallships.dockyard.about.smallships.caravel": "Каравела — двомачтове вітрильне судно, до чотирьох гармат, по дві з кожного борту. Може йти проти вітру: і зустрічний, і боковий вітер дають надбавку до швидкості. Швидкість однакова в усіх біомах.",
    "gui.smallships.dockyard.about.smallships.dhow": "Дау — двомачтове вітрильне судно, до чотирьох гармат, по дві з кожного борту. Боковий вітер дає надбавку до швидкості. Зберігає повну швидкість навіть з повним трюмом. У теплих водах іде швидше.",
}
for ship, (noun, g) in SHIPS.items():
    T[f"entity.smallships.{ship}"] = noun[0].upper() + noun[1:]
    for wood, (m, f) in WOODS.items():
        adj = m if g == "m" else f
        T[f"item.smallships.{wood}_{ship}"] = f"{adj} {noun}"
src = json.load(open(SRC, encoding="utf-8"))
missing = [k for k in src if k not in T]
if missing:
    raise SystemExit("untranslated: " + ", ".join(missing))
json.dump({"smallships": {k: T[k] for k in src}}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("written", len(src))
