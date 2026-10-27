"""Generates i18n/batches/b08_tents.json: Nomadic Tents names are a size x tent-type grid."""
import json, os
B = os.path.join(os.path.dirname(__file__), "..", "..", "i18n", "batches", "b08_tents.json")
SIZES = ["tiny", "small", "medium", "large", "giant", "mega"]
# tent type -> (name, gender of noun for adjective agreement: f/n/m)
TYPES = {"yurt": ("юрта", "f"), "tepee": ("тіпі", "n"), "bedouin": ("бедуїнський намет", "m"),
         "indlu": ("індлу", "f"), "shamiyana": ("шаміяна", "f")}
GEN = {"tepee": "тіпі", "yurt": "юрти", "bedouin": "бедуїнського намету", "indlu": "індлу", "shamiyana": "шаміяни"}
ADJ = {"tiny": {"f": "Крихітна", "n": "Крихітне", "m": "Крихітний"}, "small": {"f": "Мала", "n": "Мале", "m": "Малий"},
       "medium": {"f": "Середня", "n": "Середнє", "m": "Середній"}, "large": {"f": "Велика", "n": "Велике", "m": "Великий"},
       "giant": {"f": "Величезна", "n": "Величезне", "m": "Величезний"}, "mega": {"f": "Мега", "n": "Мега", "m": "Мега"}}
DOOR = {"tiny": "крихітного", "small": "малого", "medium": "середнього", "large": "великого", "giant": "величезного", "mega": "мега"}
COLORS = {"black": "Чорна", "blue": "Синя", "brown": "Коричнева", "cyan": "Бірюзова", "gray": "Сіра", "green": "Зелена",
          "light_blue": "Блакитна", "lime": "Лаймова", "magenta": "Пурпурова", "orange": "Помаранчева", "pink": "Рожева",
          "purple": "Фіолетова", "red": "Червона", "light_gray": "Світло-сіра", "white": "Біла", "yellow": "Жовта"}
TEPEE = {"channel": "канали", "creeper": "кріпер", "dreamcatcher": "ловець снів", "eagle": "орел", "golem": "голем",
         "hope": "надія", "magic": "магія", "radial": "промені", "rain": "дощ", "space": "космос", "sun": "сонце",
         "triforce": "трисила", "wedge": "клин", "zigzag": "зиґзаґ"}
d = {}
for t, (name, g) in TYPES.items():
    for s in SIZES:
        n = f"{ADJ[s][g]} {name}"
        if s == "mega":
            n = f"Мега-{name}"
        d[f"item.nomadictents.{s}_{t}"] = n
        door_size = "" if s == "mega" else DOOR[s] + " "
        d[f"block.nomadictents.{s}_{t}_door"] = (f"Вхід мега-{GEN[t]}" if s == "mega" else f"Вхід {door_size}{GEN[t]}")
    d[f"item.nomadictents.{t}_section"] = f"Секція стіни {GEN[t]}"
for c, n in COLORS.items():
    d[f"block.nomadictents.{c}_shamiyana_wall"] = f"{n} стіна шаміяни"
for k, n in TEPEE.items():
    d[f"block.nomadictents.{k}_tepee_wall"] = f"Стіна тіпі ({n})"
d.update({
    "itemGroup.nomadictents": "Nomadic Tents", "block.nomadictents.door_frame": "Каркас намету",
    "block.nomadictents.yurt_wall_frame": "Каркас юрти", "block.nomadictents.yurt_roof_frame": "Каркас даху юрти",
    "block.nomadictents.tepee_wall_frame": "Каркас тіпі", "block.nomadictents.bedouin_wall_frame": "Каркас бедуїнського намету",
    "block.nomadictents.bedouin_roof_frame": "Каркас даху бедуїнського намету", "block.nomadictents.indlu_wall_frame": "Каркас індлу",
    "block.nomadictents.shamiyana_wall_frame": "Каркас шаміяни", "block.nomadictents.yurt_roof": "Дах юрти",
    "block.nomadictents.yurt_wall": "Стіна юрти", "block.nomadictents.blank_tepee_wall": "Стіна тіпі",
    "block.nomadictents.bedouin_wall": "Стіна бедуїнського намету", "block.nomadictents.bedouin_roof": "Дах бедуїнського намету",
    "block.nomadictents.indlu_wall": "Стіна індлу", "block.nomadictents.rigid_dirt": "Утрамбована земля",
    "item.nomadictents.golden_crossbeams": "Золоті поперечки", "item.nomadictents.obsidian_crossbeams": "Обсидіанові поперечки",
    "item.nomadictents.diamond_crossbeams": "Алмазні поперечки",
    "item.nomadictents.stone_tent_shovel": "Кам'яна наметова лопата", "item.nomadictents.stone_tent_shovel.tooltip": "Розширює малі й більші намети до рівня 1",
    "item.nomadictents.iron_tent_shovel": "Залізна наметова лопата", "item.nomadictents.iron_tent_shovel.tooltip": "Розширює середні й більші намети до рівня 2",
    "item.nomadictents.golden_tent_shovel": "Золота наметова лопата", "item.nomadictents.golden_tent_shovel.tooltip": "Розширює великі й більші намети до рівня 3",
    "item.nomadictents.obsidian_tent_shovel": "Обсидіанова наметова лопата", "item.nomadictents.obsidian_tent_shovel.tooltip": "Розширює величезні й мега-намети до рівня 4",
    "item.nomadictents.diamond_tent_shovel": "Алмазна наметова лопата", "item.nomadictents.diamond_tent_shovel.tooltip": "Розширює мега-намети до рівня 5",
    "item.nomadictents.mallet": "Наметовий молоток", "item.nomadictents.mallet.tooltip": "Застосуйте до каркаса намету, щоб збудувати його",
    "item.nomadictents.golden_mallet": "Золотий наметовий молоток", "item.nomadictents.golden_mallet.tooltip": "Застосуйте до каркаса намету, щоб миттєво збудувати його",
    "item.nomadictents.tent_canvas": "Наметове полотно", "item.nomadictents.tent.tooltip": "Застосуйте, щоб поставити намет",
    "item.nomadictents.tent.tooltip.id": "ID намету: %s", "item.nomadictents.tent.tooltip.layer": "Розширення: %s / %s",
    "tent.teleport.deny": "Не можна телепортуватися всередині намету", "tent.build.deny.space": "Для намету замало місця",
    "tent.build.deny.dimension": "У цьому вимірі не можна ставити намет", "tent.build.deny.inside_tent": "Не можна ставити намет усередині намету",
    "tent.enter.deny.incomplete": "Намет не добудовано", "tent.enter.deny.not_owner": "Це не ваш намет",
    "tent.enter.deny.monsters": "Зараз не можна зайти: поруч чудовиська", "tent.remove.deny.incomplete": "Намет не добудовано",
    "tent.remove.deny.not_owner": "Це не ваш намет", "tent.remove.deny.monsters": "Зараз не можна забрати намет: поруч чудовиська",
    "hwyla.progress": "Готовність: %s"})
json.dump({"nomadictents": d}, open(B, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print(len(d))
