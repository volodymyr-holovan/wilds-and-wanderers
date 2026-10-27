"""Batch 42: Domum Ornamentum (MineColonies' decorative blocks). Names built from a material use "Kind (%s)" so the
material name never has to agree in case with the Ukrainian noun."""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "i18n", "src", "domum_ornamentum.json")
OUT = os.path.join(HERE, "..", "..", "i18n", "batches", "b42_domum.json")

COLORS = {"black": ("Чорний", "Чорна", "Чорні"), "blue": ("Синій", "Синя", "Сині"), "brown": ("Бурий", "Бура", "Бурі"),
          "cyan": ("Бірюзовий", "Бірюзова", "Бірюзові"), "gray": ("Сірий", "Сіра", "Сірі"), "green": ("Зелений", "Зелена", "Зелені"),
          "light_blue": ("Блакитний", "Блакитна", "Блакитні"), "light_gray": ("Світло-сірий", "Світло-сіра", "Світло-сірі"),
          "lime": ("Лаймовий", "Лаймова", "Лаймові"), "magenta": ("Пурпуровий", "Пурпурова", "Пурпурові"),
          "orange": ("Помаранчевий", "Помаранчева", "Помаранчеві"), "pink": ("Рожевий", "Рожева", "Рожеві"),
          "purple": ("Фіолетовий", "Фіолетова", "Фіолетові"), "red": ("Червоний", "Червона", "Червоні"),
          "white": ("Білий", "Біла", "Білі"), "yellow": ("Жовтий", "Жовта", "Жовті")}
BRICKS = {"beige": "Бежева", "brown": "Бура", "cream": "Кремова", "roan": "Рудувата", "sand": "Піщана"}
VARIANTS = {"boss": "Опуклий", "coffer": "Кесонний", "full": "Суцільний", "horizontal.bars": "Горизонтальні рейки",
            "horizontally.squiggly.striped": "Горизонтальні хвилі", "horizontally.striped": "Горизонтальні смуги",
            "moulding": "Ліпний", "port.manteau": "Вішак", "porthole": "Ілюмінатор", "roundel": "Медальйон", "slot": "Проріз",
            "vertical.bars": "Вертикальні рейки", "vertically.squiggly.striped": "Вертикальні хвилі",
            "vertically.striped": "Вертикальні смуги", "waffle": "Вафельний"}
T = {
    "block.domum_ornamentum.architectscutter": "Різак архітектора", "domum_ornamentum.architectscutter": "Різак архітектора",
    "block.domum_ornamentum.blockbarreldeco_onside": "Лежача бочка", "block.domum_ornamentum.blockbarreldeco_standing": "Стояча бочка",
    "cuttergroup.domum_ornamentum.avanilla": "Звичайні блоки", "cuttergroup.domum_ornamentum.btimberframe": "Фахверк",
    "cuttergroup.domum_ornamentum.cshingle": "Черепиця", "cuttergroup.domum_ornamentum.ddoor": "Двері",
    "cuttergroup.domum_ornamentum.etrapdoor": "Люки", "cuttergroup.domum_ornamentum.fpanel": "Панелі",
    "cuttergroup.domum_ornamentum.gpillar": "Колони", "cuttergroup.domum_ornamentum.hpaperwall": "Шибки в рамі",
    "cuttergroup.domum_ornamentum.ilight": "Світильники", "cuttergroup.domum_ornamentum.jbrick": "Цегла",
    "cuttergroup.domum_ornamentum.kpost": "Стовпи",
    "domum_ornamentum.allbrick.column.format": "Основний матеріал: %s", "domum_ornamentum.block.format": "Матеріал: %s",
    "domum_ornamentum.blockpaperwall.center.format": "  - Середина: %s", "domum_ornamentum.blockpaperwall.frame.format": "  - Рама: %s",
    "domum_ornamentum.blockpaperwall.header": "Матеріали:", "domum_ornamentum.blockpaperwall.name.format": "Шибка в рамі (%s)",
    "domum_ornamentum.blockpillar.name.format": "Кругла колона (%s)",
    "domum_ornamentum.blocktiledpaperwall.center.format": "  - Середина: %s", "domum_ornamentum.blocktiledpaperwall.frame.format": "  - Рама: %s",
    "domum_ornamentum.blocktiledpaperwall.header": "Матеріали:", "domum_ornamentum.blocktiledpaperwall.name.format": "Плиткова шибка (%s)",
    "domum_ornamentum.blockypillar.name.format": "Кубічна колона (%s)",
    "domum_ornamentum.dark_brick.name.format": "Темна цегла (%s)", "domum_ornamentum.dark_brick_stair.name.format": "Сходи з темної цегли (%s)",
    "domum_ornamentum.light_brick.name.format": "Світла цегла (%s)", "domum_ornamentum.light_brick_stair.name.format": "Сходи зі світлої цегли (%s)",
    "domum_ornamentum.desc.center": "Середина: %s", "domum_ornamentum.desc.frame": "Рама: %s", "domum_ornamentum.desc.main": "Основа: %s",
    "domum_ornamentum.desc.material": "Матеріал: %s", "domum_ornamentum.desc.onlyone": "%s", "domum_ornamentum.desc.shingle": "Черепиця: %s",
    "domum_ornamentum.desc.support": "Опора: %s",
    "domum_ornamentum.door.block.format": "Матеріал: %s", "domum_ornamentum.door.name.format": "Двері (%s)",
    "domum_ornamentum.door.type.format": "Різновид: %s", "domum_ornamentum.door.type.name.full": "Суцільні",
    "domum_ornamentum.door.type.name.port.manteau": "Вішак", "domum_ornamentum.door.type.name.vertically.striped": "Вертикальні смуги",
    "domum_ornamentum.door.type.name.waffle": "Вафельні",
    "domum_ornamentum.dynamic.frame.name.format": "Блок у рамі (%s)", "domum_ornamentum.extra.name.format": "Декоративний блок (%s)",
    "domum_ornamentum.fancydoor.center.block.format": "  - Матеріал: %s", "domum_ornamentum.fancydoor.center.header": "Середина:",
    "domum_ornamentum.fancydoor.frame.block.format": "  - Матеріал: %s", "domum_ornamentum.fancydoor.frame.header": "Рама:",
    "domum_ornamentum.fancydoor.name.format": "Вишукані двері (%s)", "domum_ornamentum.fancydoor.type.format": "Різновид: %s",
    "domum_ornamentum.fancydoor.type.name.creeper": "Кріпер", "domum_ornamentum.fancydoor.type.name.full": "Суцільні",
    "domum_ornamentum.fancytrapdoor.center.block.format": "  - Матеріал: %s", "domum_ornamentum.fancytrapdoor.center.header": "Середина:",
    "domum_ornamentum.fancytrapdoor.frame.block.format": "  - Матеріал: %s", "domum_ornamentum.fancytrapdoor.frame.header": "Рама:",
    "domum_ornamentum.fancytrapdoor.name.format": "Вишуканий люк (%s)", "domum_ornamentum.fancytrapdoor.type.format": "Різновид: %s",
    "domum_ornamentum.fancytrapdoor.type.name.creeper": "Кріпер", "domum_ornamentum.fancytrapdoor.type.name.full": "Суцільний",
    "domum_ornamentum.fence-gate.name.format": "Хвіртка (%s)", "domum_ornamentum.fence.name.format": "Паркан (%s)",
    "domum_ornamentum.group": "Група:",
    "domum_ornamentum.light.center.block.format": "  - Матеріал: %s", "domum_ornamentum.light.center.header": "Середина:",
    "domum_ornamentum.light.frame.block.format": "  - Матеріал: %s", "domum_ornamentum.light.frame.header": "Рама:",
    "domum_ornamentum.light.frame.name.format": "Світильник у рамі (%s)", "domum_ornamentum.light.frame.type.center_light": "Центральний",
    "domum_ornamentum.light.frame.type.crossed_light": "Хрестом", "domum_ornamentum.light.frame.type.fancy_light": "Вишуканий",
    "domum_ornamentum.light.frame.type.format": "  - Тип: %s", "domum_ornamentum.light.frame.type.four_light": "Чотири вікна",
    "domum_ornamentum.light.frame.type.framed_light": "У рамі", "domum_ornamentum.light.frame.type.horizontal_light": "Горизонтальний",
    "domum_ornamentum.light.frame.type.vertical_light": "Вертикальний",
    "domum_ornamentum.origin.tooltip": "Робиться на різаку архітектора",
    "domum_ornamentum.panel.block.format": "Матеріал: %s", "domum_ornamentum.panel.name.format": "Панель (%s)",
    "domum_ornamentum.panel.type.format": "Різновид: %s",
    "domum_ornamentum.pillar.column.format": "Основний матеріал: %s", "domum_ornamentum.pillar.header": "Тип:",
    "domum_ornamentum.post.block.format": "Матеріал: %s", "domum_ornamentum.post.name.format": "Стовп (%s)",
    "domum_ornamentum.post.type.format": "Різновид: %s", "domum_ornamentum.post.type.name.double": "Подвійний",
    "domum_ornamentum.post.type.name.heavy": "Масивний", "domum_ornamentum.post.type.name.pinched": "Звужений",
    "domum_ornamentum.post.type.name.plain": "Простий", "domum_ornamentum.post.type.name.quad": "Четверний",
    "domum_ornamentum.post.type.name.turned": "Точений",
    "domum_ornamentum.shingle.main.format": "Основний матеріал: %s",
    "domum_ornamentum.shingle.name.format.block.domum_ornamentum.shingle": "Черепиця (%s)",
    "domum_ornamentum.shingle.name.format.block.domum_ornamentum.shingle_flat": "Пласка черепиця (%s)",
    "domum_ornamentum.shingle.name.format.block.domum_ornamentum.shingle_flat_lower": "Пласка нижня черепиця (%s)",
    "domum_ornamentum.shingle.name.format.block.domum_ornamentum.shingle_steep": "Крута черепиця (%s)",
    "domum_ornamentum.shingle.name.format.block.domum_ornamentum.shingle_steep_lower": "Крута нижня черепиця (%s)",
    "domum_ornamentum.shingle.support.format": "Опора: %s", "domum_ornamentum.shingle_slab.cover.format": "Покриття: %s",
    "domum_ornamentum.shingle_slab.main.format": "Основний матеріал: %s", "domum_ornamentum.shingle_slab.name.format": "Плита черепиці (%s)",
    "domum_ornamentum.shingle_slab.support.format": "Опора: %s", "domum_ornamentum.slab.name.format": "Плита (%s)",
    "domum_ornamentum.squarepillar.name.format": "Квадратна колона (%s)", "domum_ornamentum.stair.name.format": "Сходи (%s)",
    "domum_ornamentum.timber.center.block.format": "  - Матеріал: %s", "domum_ornamentum.timber.center.header": "Середина:",
    "domum_ornamentum.timber.frame.block.format": "  - Матеріал: %s", "domum_ornamentum.timber.frame.header": "Рама:",
    "domum_ornamentum.timber.frame.name.format": "Фахверк (%s)", "domum_ornamentum.timber.frame.type.double_crossed": "Подвійний хрест",
    "domum_ornamentum.timber.frame.type.down_gated": "Ворітця вниз", "domum_ornamentum.timber.frame.type.format": "  - Тип: %s",
    "domum_ornamentum.timber.frame.type.framed": "У рамі", "domum_ornamentum.timber.frame.type.horizontal_plain": "Простий горизонтальний",
    "domum_ornamentum.timber.frame.type.one_crossed_lr": "Навскіс зліва направо", "domum_ornamentum.timber.frame.type.one_crossed_rl": "Навскіс справа наліво",
    "domum_ornamentum.timber.frame.type.plain": "Простий", "domum_ornamentum.timber.frame.type.side_framed": "Бічна рама",
    "domum_ornamentum.timber.frame.type.side_framed_horizontal": "Бічна горизонтальна рама", "domum_ornamentum.timber.frame.type.up_gated": "Ворітця вгору",
    "domum_ornamentum.trapdoor.block.format": "Матеріал: %s", "domum_ornamentum.trapdoor.name.format": "Люк (%s)",
    "domum_ornamentum.trapdoor.type.format": "Різновид: %s", "domum_ornamentum.variant": "Різновид:",
    "domum_ornamentum.wall.name.format": "Стіна (%s)",
    "itemGroup.domum_ornamentum.doors": "DO — Двері", "itemGroup.domum_ornamentum.extra-blocks": "DO — Декоративні блоки",
    "itemGroup.domum_ornamentum.fences": "DO — Паркани", "itemGroup.domum_ornamentum.floating-carpets": "DO — Висячі килими",
    "itemGroup.domum_ornamentum.general": "Domum Ornamentum (DO)", "itemGroup.domum_ornamentum.paperwalls": "DO — Тонкі стінки в рамі",
    "itemGroup.domum_ornamentum.posts": "DO — Стовпи", "itemGroup.domum_ornamentum.shingle_slabs": "DO — Плити черепиці",
    "itemGroup.domum_ornamentum.shingles": "DO — Черепиця", "itemGroup.domum_ornamentum.slabs": "DO — Плити",
    "itemGroup.domum_ornamentum.stairs": "DO — Сходи", "itemGroup.domum_ornamentum.timber_frames": "DO — Блоки в рамі",
    "itemGroup.domum_ornamentum.walls": "DO — Стіни",
}
for c, (m, f, pl) in COLORS.items():
    T[f"block.domum_ornamentum.{c}_floating_carpet"] = f"{m} висячий килим"
    T[f"domum_ornamentum.extra.name.format.{c}"] = f"{m} декоративний блок (%s)"
for b, adj in BRICKS.items():
    T[f"block.domum_ornamentum.{b}_bricks"] = f"{adj} цегла"
    T[f"block.domum_ornamentum.{b}_stone_bricks"] = f"{adj} кам'яна цегла"
for v, name in VARIANTS.items():
    T[f"domum_ornamentum.panel.type.name.{v}"] = name
    T[f"domum_ornamentum.trapdoor.type.name.{v}"] = name

src = json.load(open(SRC, encoding="utf-8"))
missing = [k for k in src if k not in T]
if missing:
    raise SystemExit("untranslated: " + ", ".join(missing))
json.dump({"domum_ornamentum": {k: T[k] for k in src}}, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("written", len(src))
