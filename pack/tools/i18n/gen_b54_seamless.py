"""Seamless Ores (03.11): its ores in other stones, «Granite Diamond Ore» -> «Діамантова руда (граніт)», for the ores and
stones this pack has; the rest of its 1600 combinations (mods the pack does not have) never appear and stay English."""
import json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import i18n  # noqa: E402

STONES = {
    "Granite": "граніт", "Diorite": "діорит", "Andesite": "андезит", "Dripstone": "натічний камінь", "Limestone": "вапняк",
    "Tuff": "туф", "Calcite": "кальцит", "Basalt": "базальт", "Smooth Basalt": "гладкий базальт", "Blackstone": "чорний камінь",
    "Deep Blackstone": "глибинний чорний камінь", "Scoria": "шлак", "Scorchia": "темний шлак", "Asurine": "азурин",
    "Ochrum": "охрум", "Crimsite": "кримзит", "Veridium": "веридій", "Marble": "мармур", "Schist": "сланець",
    "Rhyolite": "ріоліт", "Gabbro": "габро", "Bauxite": "боксит", "Galena": "галеніт", "Cinnabar": "кіновар",
    "Bloodstone": "кров'яний камінь", "Bluestone": "синій камінь", "Darkstone": "темний камінь", "Asphalt": "асфальт",
    "Reefstone": "рифовий камінь", "Lignite": "буре вугілля", "Viridite": "виридит", "Blunite": "блуніт",
    "Black Argillite": "чорний аргіліт",
}
ORES = {
    "Coal": "Вугільна", "Iron": "Залізна", "Copper": "Мідна", "Gold": "Золота", "Nether Gold": "Незерська золота",
    "Lapis Lazuli": "Лазуритова", "Diamond": "Діамантова", "Emerald": "Смарагдова", "Redstone": "Редстоунова",
    "Quartz": "Кварцова", "Zinc": "Цинкова", "Osmium": "Осмієва", "Tin": "Олов'яна", "Lead": "Свинцева",
    "Uranium": "Уранова", "Fluorite": "Флюоритова", "Dense Osmium": "Густа осмієва", "Dense Tin": "Густа олов'яна",
    "Dense Lead": "Густа свинцева", "Dense Uranium": "Густа уранова", "Dense Fluorite": "Густа флюоритова",
    "Silver": "Срібна", "Iceandfire Silver": "Срібна", "Sapphire": "Сапфірова", "Iceandfire Sapphire": "Сапфірова",
    "Ruby": "Рубінова", "Nickel": "Нікелева", "Aluminum": "Алюмінієва", "Platinum": "Платинова", "Salt": "Соляна",
}


def main():
    src = json.load(open(os.path.join(i18n.SRC, "seamlessores.json"), encoding="utf-8"))
    out = {}
    for k, v in src.items():
        if not (k.startswith("block.seamlessores.") and v.endswith(" Ore")):
            continue
        name = v[:-4]
        for stone in sorted(STONES, key=len, reverse=True):
            if name.startswith(stone + " ") and name[len(stone) + 1:] in ORES:
                out[k] = f"{ORES[name[len(stone) + 1:]]} руда ({STONES[stone]})"
                break
    out["itemGroup.seamlessores.ores"] = "Seamless Ores: звичайні камені"
    out["itemGroup.seamlessores.modded_stones"] = "Seamless Ores: камені модів"
    unknown = i18n.store("seamlessores", out)
    print(f"seamlessores: {len(out)} names, unknown keys {len(unknown)}")


if __name__ == "__main__":
    main()
