# Джерела

Кожне джерело має клас достовірності:

- **А — першоджерело.** Код, ресурси й історія комітів самого Twilight Forest, а також слова членів TeamTwilight (позначка `MEMBER` на GitHub).
- **Б — офіційна вікі мода.** Вікі на сайті автора, benimatic.com/tfwiki. Її пишуть спільнота й розробники, тож вона здебільшого точна, але подекуди застаріла.
- **В — вторинні джерела.** Сторонні вікі й огляди. Беремо лише для довідки, перевіряємо за А.
- **Г — фанатські проєкти.** Чужі моди-доповнення. Нічого не переносимо, лише порівнюємо.

| # | Джерело | Клас | Що звідти взято |
|---|---|---|---|
| S1 | Репозиторій [TeamTwilight/twilightforest](https://github.com/TeamTwilight/twilightforest), гілка `1.21.1`, коміт `008085c` (22.03.2026, «fix #2578») | А | База збірки. Час коміту (22:52 UTC) збігається з `Implementation-Timestamp` у jar збірки (22:54 UTC), версія `4.8.3345` — це `CIRevision` збірки CI |
| S2 | Коміт `15d26a3` Benimatic, 04.03.2016, «Commit with files from in-development version 2.3.8» | А | Початковий задум замку. Таблички «Final Boss Here / You win!», «Mini-boss 2 / Gives talisman», «Parkour area 1 / Unique monster?», «Parkour area 2 / mini-boss 1». Предмет `cubeTalisman` і логіка `doesTalismanHaveCube` у `ItemTFCubeOfAnnihilation` |
| S3 | `FinalCastleBossGazeboComponent.java`, шаблон `structure/final_castle/temp/gazebo.nbt` (S1) | А | Альтанка 21×13×21 із фіолетового силового поля. Усередині — jigsaw `twilightforest:final_castle/final_boss` (до нього так і не підключили жодного пулу) і написи «Final Castle WIP» |
| S4 | `PlateauBoss.java`, `FinalBossSpawnerBlockEntity.java`, `ClientRegistrationEvents.java` (S1) | А | Фінальний бос зареєстрований як `twilightforest:plateau_boss`, має спавнер `final_boss_boss_spawner`, але не має поведінки, атрибутів і моделі (`NoopRenderer`) |
| S5 | `FinalCastleStructure.buildFinalCastleConfig` (S1) | А | Закоментовані списки мобів замку: Кобольди, Адепти, Куби-передвісники, Енде́рмени, Броньовані велетні, Іфрити в кузні. Примітка `TODO Re-enable mob spawns when proper castle mobs are created` |
| S6 | `CubeOfAnnihilation.java`, тег `annihilation_inclusions` (S1) | А | Куб знищення стирає саме блоки замку, силові поля, мертвий камінь і терни. Предмет і талісман стоять у тегу `twilightforest:wip` |
| S7 | `RovingCube.java`, `CubeMoveToRedstoneSymbolsGoal.java`, `CubeCenterOnSymbolGoal.java` (S1) | А | Задум Benimatic: Бродячий куб шукає «символ» із червоного пилу й стає в його центр. Перевірка символу вимагає пилу з усіх шести боків, тож символ неможливо побудувати. Механіку так і не довели до кінця |
| S8 | `TFAdvancementGenerator.java` (S1) | А | Закоментовані досягнення `progress_thorns` і `progress_castle`. Досягнення `progression_end` з текстом «далі — робота в процесі» |
| S9 | Тікет [#291](https://github.com/TeamTwilight/twilightforest/issues/291), відповіді Tamaized і Killer-Demon | А | «The Final Castle + the final boss are both in the design document». Замок майже не змінювався з 1.7.10 |
| S10 | Тікет [#1873](https://github.com/TeamTwilight/twilightforest/issues/1873), відповідь Tamaized (MEMBER), 2023 | А | «The boss is already fully planned out». Задум розробників існує, але не опублікований |
| S11 | Тікет [#1669](https://github.com/TeamTwilight/twilightforest/issues/1669), Tamaized (MEMBER) | А | «We have plans for the castle already» |
| S12 | Тікет [#1457](https://github.com/TeamTwilight/twilightforest/issues/1457), Drullkus (MEMBER), 2022 | А | Підтвердження: фінального боса немає |
| S13 | Тікет [#2822](https://github.com/TeamTwilight/twilightforest/issues/2822), Drullkus, 09.2026 | А | Написи в альтанці не прибираються ударом: відома вада |
| S14 | Вікі: [Final Castle](http://benimatic.com/tfwiki/index.php/Final_Castle) | Б | Історія версій: 3.0.0 — замок; 3.10.1013 — тимчасовий «Final Boss» (Кобольд, 1024 здоров'я); 4.0.475 — його прибрали; 4.2.1428 — моби більше не з'являються |
| S15 | Вікі: [Final Plateau](http://benimatic.com/tfwiki/index.php/Final_Plateau), [Thornlands](http://benimatic.com/tfwiki/index.php/Thornlands) | Б | Кислотний дощ, Лампа Попелу, терни. 4.3.1735 — повідомлення «замок ще не готовий» |
| S16 | Вікі: [Adherent](http://benimatic.com/tfwiki/index.php/Adherent), [Harbinger Cube](http://benimatic.com/tfwiki/index.php/Harbinger_Cube), [Roving Cube](http://benimatic.com/tfwiki/index.php/Roving_Cube), [Castle Door](http://benimatic.com/tfwiki/index.php/Castle_Door) | Б | Стан мобів замку: усі NYI («not yet implemented»). Двері замку чотирьох кольорів |
| S17 | Вікі: [April Fools 2022](http://benimatic.com/tfwiki/index.php/Change_Log/April_Fools_2022) | Б | Жартівливий «фінальний бос» — перефарбований Дракон Краю. Розробники кепкують із ідеї «фінальний бос — дракон». Офіційного письмового лору немає: «it is all implied» |
| S18 | `LICENSE` (LGPL 2.1), `ASSET_LICENSE` (CC BY-NC-SA 4.0) у S1; `license` у `neoforge.mods.toml` jar збірки | А | Умови зміни й поширення |
| S19 | [FTB Wiki: Final Castle](https://ftb.fandom.com/wiki/Final_Castle), [TV Tropes](https://www.tvtropes.org/pmwiki/pmwiki.php/VideoGame/TheTwilightForest), [FTB help: Highland Bosses](https://help.ftb.team/help/en-gb/19-twilight-forest/70-highland-bosses) | В | Лише підтвердження загальновідомого: замок недороблений, фінального боса немає |
| S20 | CurseForge: [Twilight Forest Final Boss](https://www.curseforge.com/minecraft/mc-mods/twilight-forest-final-boss), [Remake](https://www.curseforge.com/minecraft/mc-mods/twilight-forest-final-boss-remake), [GitHub pSUNSET/TwilightForestFinalBoss](https://github.com/pSUNSET/TwilightForestFinalBoss), [The King of Twilight](https://www.curseforge.com/minecraft/mc-mods/the-king-of-twilight), [Twilight Forest: Finale](https://www.curseforge.com/minecraft/mc-mods/twilight-forest-finale) | Г | Див. [fan-projects.md](fan-projects.md) |

## Чого немає в джерелах

- Справжній задум TeamTwilight щодо фінального боса **не опубліковано** (S10, S11). Тому все, що тут зроблено для боса, — **фанатське завершення збірки Wilds & Wanderers, а не відтворення офіційного задуму**. Так це й позначено в грі та документації.
- Інтерв'ю чи допису Benimatic про задум фінального боса знайти не вдалося. Пошук ішов в інтернеті, у тікетах GitHub і у вікі на сайті автора. Єдине першоджерело його задуму — код і таблички 2016 року (S2).
