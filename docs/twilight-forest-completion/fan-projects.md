# Фанатські продовження

Перевірено 14.11.2026 на сторінках CurseForge і GitHub. Достовірність — клас Г (див. [sources.md](sources.md)).

| Проєкт | Автор | Версії | Ліцензія | Залежності | Що додає |
|---|---|---|---|---|---|
| [Twilight Forest Final Boss](https://www.curseforge.com/minecraft/mc-mods/twilight-forest-final-boss) | D_Angry_Modder | Forge 1.20.1, останній файл 05.03.2025 | All Rights Reserved | GeckoLib, Twilight Tweaks (без нього бос не з'являється) | Бос Castle Keeper. Зроблено в MCreator. Більше не оновлюється |
| [Twilight Forest Final Boss (Remake)](https://www.curseforge.com/minecraft/mc-mods/twilight-forest-final-boss-remake), код: [pSUNSET/TwilightForestFinalBoss](https://github.com/pSUNSET/TwilightForestFinalBoss) | LShiftLess / Project-SUNSET | NeoForge 1.21.1 (v2.1.2 під TF 4.8.3345, 30.08.2026), Forge 1.20.1 | Код — MIT. Модель, анімація й текстури Castle Keeper — «усі права захищено» D_Angry_Modder (`ORIGIN_LICENSE`) | GeckoLib | Той самий Castle Keeper. Його добич іде на рецепт Куба знищення. Два досягнення |
| [The King of Twilight](https://www.curseforge.com/minecraft/mc-mods/the-king-of-twilight) | White_Shrimp | лише Forge 1.20.1 | MIT | Twilight Tweaks, Iron's Spells, GeckoLib | Бос Twilight Lord на даху замку, шість стадій заклять. Зроблено для збірки автора |
| [Twilight Forest: Finale](https://www.curseforge.com/minecraft/mc-mods/twilight-forest-finale) | shuixingshow, Vzling | лише Forge 1.20.1, 0.1.5 (11.08.2026), «у розробці» | All Rights Reserved, сирців немає | Vzling Lib | Бос Adherent High Priest у годинниковій вежі власного замку. Нові варіанти спорядження з матеріалів босів |

## Висновки

- **Сумісні зі збіркою на рівні завантажувача** лише Remake (NeoForge 1.21.1). Решта — Forge 1.20.1, тож у збірку їх не поставити.
- **Remake не можна вбудувати в TF.** Код MIT можна було б узяти, але без моделі й текстур він нічого не дає, а їхні права захищено. Поставити його можна лише окремим модом. Тоді недороблений вміст самого TF (Куби, Адепти, кузня, талісман) так і лишився б порожнім. Власник збірки обрав власну реалізацію в сирцях TF (рішення від 14.11.2026).
- **З фанатських проєктів не взято нічого: ні коду, ні ресурсів, ні механік.** Збіги в загальних ідеях («фінальний бос на даху замку») походять від самого TF: альтанку на даху заклав Benimatic у 2016 році (S2, S3).
- **Конфлікт із збіркою.** Якщо хтось колись поставить Remake поруч із нашою версією TF, обидва моди претендуватимуть на дах замку. Наш бос живе у штатному спавнері TF (`final_boss_boss_spawner`), а Remake ставить свого боса власним механізмом. Технічно моди не зламають один одного, але гравець отримає двох фінальних босів. Тому в збірці Remake **не ставимо**.
