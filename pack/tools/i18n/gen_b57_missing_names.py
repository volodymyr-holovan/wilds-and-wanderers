"""Names that show as raw keys in the game (found by the client scan, wilds_core UiTour "names", 22.11): the mods ship no
text for them at all, not even English. Small Ships' ships of other mods' woods, Simply Tents' held models, a few
items, and projectiles and helpers that appear only in death messages ("killed by entity.cataclysm.poison_dart").

  python pack/tools/i18n/gen_b57_missing_names.py
    -> pack/overrides/kubejs/assets/wilds_names/lang/{uk_ua,en_us}.json   (a lang file of its own: keys are global)
"""
import json, os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".."))
OUT = os.path.join(ROOT, "pack", "overrides", "kubejs", "assets", "wilds_names", "lang")

UK, EN = {}, {}


def put(key, uk, en):
    UK[key], EN[key] = uk, en


# Small Ships in the woods of Aether, Twilight Forest and Fish of Thieves (adjectives as the mods' own boats)
WOODS = {"aether_skyroot": ("Небесн", "Skyroot"), "twilightforest_canopy": ("Нависн", "Canopy"), "twilightforest_dark": ("Темн", "Darkwood"),
         "twilightforest_mangrove": ("Сутінково-мангров", "Twilight Mangrove"), "twilightforest_mining": ("Шахтарськ", "Minewood"),
         "twilightforest_sorting": ("Сортувальн", "Sortingwood"), "twilightforest_time": ("Часов", "Timewood"),
         "twilightforest_transformation": ("Перетворн", "Transwood"), "twilightforest_twilight_oak": ("Сутінково-дубов", "Twilight Oak"),
         "fishofthieves_coconut": ("Кокосов", "Coconut")}
SHIPS = {"cog": ("ког", "m", "Cog"), "brigg": ("бриг", "m", "Brigg"), "galley": ("галера", "f", "Galley"), "dhow": ("дау", "f", "Dhow"),
         "drakkar": ("драккар", "m", "Drakkar"), "galleon": ("галеон", "m", "Galleon"), "caravel": ("каравела", "f", "Caravel")}
for w, (stem, en_w) in WOODS.items():
    for s, (noun, g, en_s) in SHIPS.items():
        end = "ий" if g == "m" else "а"
        if stem.endswith(("Небесн", "Темн", "Нависн", "Сортувальн", "Перетворн")) and g == "m":
            end = "ий"
        put(f"item.smallships.{w}_{s}", f"{stem}{end} {noun}", f"{en_w} {en_s}")

# Simply Tents: the tents as they look in the hand
COLORS = {"white": ("білий", "White"), "orange": ("помаранчевий", "Orange"), "magenta": ("пурпуровий", "Magenta"),
          "light_blue": ("блакитний", "Light Blue"), "yellow": ("жовтий", "Yellow"), "lime_green": ("лаймовий", "Lime"),
          "pink": ("рожевий", "Pink"), "grey": ("сірий", "Gray"), "light_grey": ("світло-сірий", "Light Gray"), "cyan": ("бірюзовий", "Cyan"),
          "purple": ("фіолетовий", "Purple"), "blue": ("синій", "Blue"), "brown": ("коричневий", "Brown"), "green": ("зелений", "Green"),
          "red": ("червоний", "Red"), "black": ("чорний", "Black")}
for c, (uk, en) in COLORS.items():
    put(f"item.simplytents.{c}_tenton_hand", f"Намет ({uk})", f"{en} Tent")
    put(f"item.simplytents.half_tent_{c}_on_hand", f"Половина намету ({uk})", f"{en} Half Tent")
    put(f"item.simplytents.{c}_half_tent_on_hand", f"Половина намету ({uk})", f"{en} Half Tent")
    put(f"item.simplytents.{c}_tent_roof_on_hand", f"Дах намету ({uk})", f"{en} Tent Roof")
put("item.simplytents.brown_tent_corner_on_han", "Кут намету (коричневий)", "Brown Tent Corner")

# single items
for k, uk, en in [
    ("block.ars_nouveau.planarium_projector", "Проєктор планаріуму", "Planarium Projector"),
    ("item.cataclysm.belt_of_monstrosity", "Пояс Монстроїда", "Belt of Monstrosity"),
    ("item.cataclysm.blazing_bone", "Палаюча кістка", "Blazing Bone"),
    ("item.cataclysm.urchin_spike", "Голка морського їжака", "Urchin Spike"),
    ("item.cataclysm.blood_clot", "Згусток крові", "Blood Clot"),
    ("item.sophisticatedstorage.inaccessible_slot", "Недоступна комірка", "Inaccessible Slot"),
    ("item.apotheosis.gem", "Самоцвіт", "Gem"),
    ("item.irons_spellbooks.legendary_spell_book", "Легендарна книга заклять", "Legendary Spell Book"),
    ("item.alexsmobs.stink_ray_empty_inventory", "Смердючий промінь (порожній)", "Stink Ray (empty)"),
    ("item.mowziesmobs.logo", "Mowzie's Mobs", "Mowzie's Mobs"),
    ("item.netherexotics.nethers_exoticism_icon", "Nether's Exoticism", "Nether's Exoticism"),
    ("entity.creeperoverhaul.birch_creeper", "Березовий кріпер", "Birch Creeper"),
    ("item.creeperoverhaul.birch_creeper_spawn_egg", "Яйце виклику березового кріпера", "Birch Creeper Spawn Egg"),
    ("entity.irons_spellbooks.debug_wizard", "Чарівник налагодження", "Debug Wizard"),
]:
    put(k, uk, en)

# projectiles, effects and helpers: only death messages and the debug screen show them
ENT = {
    "alexsmobs:emu_egg": ("Яйце ему", "Emu Egg"), "alexsmobs:ice_shard": ("Крижаний уламок", "Ice Shard"),
    "mowziesmobs:sunstrike": ("Сонячний удар", "Sunstrike"), "mowziesmobs:solar_beam": ("Сонячний промінь", "Solar Beam"),
    "mowziesmobs:boulder_projectile": ("Валун", "Boulder"), "mowziesmobs:rock_sling": ("Камінь із пращі", "Rock Sling"),
    "mowziesmobs:boulder_platform": ("Кам'яна плита", "Boulder Platform"), "mowziesmobs:boulder_platform_crumbling": ("Кам'яна плита", "Boulder Platform"),
    "mowziesmobs:pillar": ("Кам'яний стовп", "Pillar"), "mowziesmobs:pillar_sculptor": ("Стовп Скульптора", "Sculptor's Pillar"),
    "mowziesmobs:pillar_piece": ("Уламок стовпа", "Pillar Piece"), "mowziesmobs:axe_attack": ("Удар сокири", "Axe Attack"),
    "mowziesmobs:ice_breath": ("Крижаний подих", "Ice Breath"), "mowziesmobs:ice_ball": ("Крижана куля", "Ice Ball"),
    "mowziesmobs:frozen_controller": ("Замороження", "Freeze"), "mowziesmobs:dart": ("Дротик", "Dart"),
    "mowziesmobs:poison_ball": ("Отруйна куля", "Poison Ball"), "mowziesmobs:super_nova": ("Наднова", "Supernova"),
    "mowziesmobs:falling_block": ("Падаючий блок", "Falling Block"), "mowziesmobs:block_swapper": ("Зміна блоків", "Block Swapper"),
    "mowziesmobs:block_swapper_tunneling": ("Зміна блоків", "Block Swapper"), "mowziesmobs:camera_shake": ("Струс", "Camera Shake"),
    "mowziesmobs:fissure": ("Розлам", "Fissure"), "mowziesmobs:fissure_piece": ("Розлам", "Fissure"), "mowziesmobs:earth_spike": ("Земляний шип", "Earth Spike"),
    "iceandfire:ghost_sword": ("Примарний меч", "Ghost Sword"), "netherexotics:moloch_egg_projectile": ("Яйце молоха", "Moloch Egg"),
    "netherexotics:rambutan_thorn_projectile": ("Колючка рамбутана", "Rambutan Thorn"), "goblins_tyranny:bomb_projectile": ("Бомба", "Bomb"),
    "goblins_tyranny:fire_bomb_projectile": ("Вогняна бомба", "Fire Bomb"), "goblins_tyranny:gaz_bomb_projectile": ("Газова бомба", "Gas Bomb"),
    "goblins_tyranny:healing_ball_projectile": ("Цілюща куля", "Healing Ball"), "goblins_tyranny:decoy_projectile": ("Приманка", "Decoy"),
    "goblins_tyranny:barrel_fireball": ("Вогняна бочка", "Barrel Fireball"), "ars_nouveau:cinder": ("Жарина", "Cinder"), "ars_nouveau:wall": ("Магічна стіна", "Wall"),
    "ars_nouveau:ice_shard": ("Крижаний уламок", "Ice Shard"), "ars_nouveau:enchanters_hook": ("Гак зачарувальника", "Enchanter's Hook"),
    "ars_nouveau:archwood_boat": ("Човен з арочного дерева", "Archwood Boat"), "naturalist:duck_egg": ("Качине яйце", "Duck Egg"),
    "sophisticatedbackpacks:everlasting_backpack_item": ("Вічний рюкзак", "Everlasting Backpack"),
    "born_in_chaos_v1:staffof_magic_arrows_projectile": ("Магічна стріла", "Magic Arrow"), "born_in_chaos_v1:staffof_blindness_projectile": ("Заряд сліпоти", "Blindness Charge"),
    "born_in_chaos_v1:pumpkin_staff_projectile": ("Гарбузовий заряд", "Pumpkin Charge"), "born_in_chaos_v1:staffof_magic_arrows_2_projectile": ("Магічна стріла", "Magic Arrow"),
    "born_in_chaos_v1:pumpkin_staff_2_projectile": ("Гарбузовий заряд", "Pumpkin Charge"), "born_in_chaos_v1:intoxicatind_bomb_projectile": ("Отруйна бомба", "Intoxicating Bomb"),
    "born_in_chaos_v1:pumpkin_pistol_projectile": ("Гарбузова куля", "Pumpkin Bullet"), "born_in_chaos_v1:pumpkin_pistol_2_projectile": ("Гарбузова куля", "Pumpkin Bullet"),
    "born_in_chaos_v1:missionary_charge": ("Заряд місіонера", "Missionary Charge"), "born_in_chaos_v1:stimulating_bombprojectile": ("Збудлива бомба", "Stimulating Bomb"),
    "born_in_chaos_v1:phantom_bomb_projectile": ("Фантомна бомба", "Phantom Bomb"), "born_in_chaos_v1:rotten_easter_egg_projectile": ("Гнила писанка", "Rotten Easter Egg"),
    "born_in_chaos_v1:spiritual_easter_egg_projectile": ("Духовна писанка", "Spiritual Easter Egg"), "born_in_chaos_v1:monstrous_easter_egg_projectile": ("Жахлива писанка", "Monstrous Easter Egg"),
    "cataclysm:abyss_mark": ("Мітка безодні", "Abyss Mark"), "cataclysm:poison_dart": ("Отруйний дротик", "Poison Dart"), "cataclysm:blazing_bone": ("Палаюча кістка", "Blazing Bone"),
    "cataclysm:urchin_spike": ("Голка морського їжака", "Urchin Spike"), "cataclysm:spark": ("Іскра", "Spark"), "cataclysm:screen_shake": ("Струс", "Screen Shake"),
    "cataclysm:sky_color": ("Колір неба", "Sky Color"), "cataclysm:wall_watcher": ("Настінний вартовий", "Wall Watcher"), "cataclysm:accretion": ("Наріст", "Accretion"),
    "cataclysm:eye_of_dungeon": ("Око підземелля", "Eye of Dungeon"), "cataclysm:the_leviathan_tongue": ("Язик Левіафана", "Leviathan's Tongue"),
    "cataclysm:player_ceraunus": ("Цераунус", "Ceraunus"), "cataclysm:brontes": ("Бронтес", "Brontes"), "cataclysm:cursed_sandstorm": ("Проклята піщана буря", "Cursed Sandstorm"),
    "smallships:ship_part": ("Частина корабля", "Ship Part"), "smallships:cannon_ball": ("Гарматне ядро", "Cannon Ball"), "smallships:chain_shot": ("Ланцюгове ядро", "Chain Shot"),
    "smallships:grape_shot": ("Картеч", "Grape Shot"), "smallships:ground_cannon": ("Гармата", "Cannon"), "irons_spellbooks:scapegoat": ("Цап-відбувайло", "Scapegoat"),
    "minecolonies:citizen": ("Мешканець колонії", "Citizen"), "minecolonies:fishhook": ("Гачок", "Fishhook"), "minecolonies:visitor": ("Відвідувач", "Visitor"),
    "minecolonies:mercenary": ("Найманець", "Mercenary"), "minecolonies:sittingentity": ("Сидіння", "Seat"), "minecolonies:mcminecart": ("Вагонетка", "Minecart"),
    "minecolonies:firearrow": ("Вогняна стріла", "Fire Arrow"), "minecolonies:mcnormalarrow": ("Стріла", "Arrow"), "minecolonies:druidpotion": ("Зілля друїда", "Druid Potion"),
    "minecolonies:spear": ("Спис", "Spear"), "undead_unleashed:firebreath": ("Вогняний подих", "Fire Breath"),
}
for e, (uk, en) in ENT.items():
    ns, p = e.split(":")
    put(f"entity.{ns}.{p}", uk, en)


def main():
    os.makedirs(OUT, exist_ok=True)
    for lang, d in (("uk_ua", UK), ("en_us", EN)):
        json.dump(d, open(os.path.join(OUT, lang + ".json"), "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1, sort_keys=True)
    print(f"{len(UK)} names written to {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
