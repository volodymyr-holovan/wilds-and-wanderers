# Технічна архітектура

## 1. Чому зміни в самому TF, а не міксин чи аддон

| Варіант | За | Проти |
|---|---|---|
| **Зміни в сирцях TF (обрано)** | Доступ до всього, що TF уже має: сутність `plateau_boss`, її спавнер, `BaseTFBoss` (смуга, скриня, «підкорено»), споруди замку, керована поява мобів, генератори даних TF. Перекласти, теги, досягнення TF генеруються штатно | Треба перезбирати TF і стежити за його оновленнями |
| Міксини з `wilds_core` | Не чіпаємо jar TF | Нового боса міксином не додати: потрібні нові сутності, блоки, рендери й генеровані дані TF. Десятки міксинів у приватні місця TF ламаються з кожним оновленням TF |
| Окремий аддон | Незалежна збірка | `plateau_boss` і його спавнер уже зареєстровані TF, тож аддон мав би або дублювати боса, або все одно лізти в TF. Помилку в генерації замку (§4) аддон не виправить |

Бінарний jar ніде не правиться: jar щоразу збирається з сирців TeamTwilight (LGPL 2.1) і наших патчів.

## 2. Як збирається

```
mods-src/twilightforest-completion/
  build.py                 клон TF -> коміт 008085c -> git am patches/*.patch -> gradle build -> --install
  patches/0001-*.patch     виправлення залежності Curios (коміт 9125ee8 самої TeamTwilight; лише build.gradle)
  patches/0002-*.patch     завершення замку, перша версія
  patches/0003-*.patch     власні моделі, анімації, виправлення після аудиту
  tools/make_models.py     моделі (Java-геометрія між маркерами) і текстури + шар світіння
  tools/make_textures.py   текстури скіпетра й снаряда
```

```bash
python mods-src/twilightforest-completion/build.py --install
```

- **База** — `008085c660f1f9fc9aad6376ea0dc8080b3c0629` (гілка `1.21.1`, 22.03.2026). Саме з неї зібрано офіційний 4.8.3345: час коміту збігається з часом збірки в `MANIFEST.MF`. Перевірено: перезібраний без патчів jar має побайтово ті самі `.class`, що й jar із CurseForge.
- **Версія мода** лишається `4.8.3345` (`-PCIRevision=4.8.3345`), тож моди, що залежать від TF, бачать ту саму версію.
- **Файл** — `wilds_twilightforest-1.21.1-4.8.3345-w1.jar`. Префікс `wilds_` означає власний мод збірки: `build_neo.py` його не видаляє, а `release_client.py` віддає гравцям з GitHub Release. Офіційний TF у `docs/mods.json` має `"where": "replaced"`: запис лишається для історії, але не завантажується й не роздається.
- **Потрібно:** Git, JDK 21 (Gradle сам завантажує його в `~/.gradle/jdks`; без JDK 21 скрипт не стартує), інтернет для першої збірки (~800 МБ залежностей).

## 3. Що змінено в TF (класи)

| Файл | Що |
|---|---|
| `entity/boss/PlateauBoss.java` | Рунний володар: фази, вартові, кола, стрибки, прислужники, скидання бою, збереження, жести (події 70–72), поворот корпусу до цілі, ремонт NaN |
| `entity/boss/ForgeHarbinger.java` | Передвісник горна: таран, оглушення, жар, підмога; стан для клієнта (`ANIM_STATE`), подія 73 |
| `entity/boss/RuneWard.java` | рунний вартовий |
| `entity/boss/CastleBossRules.java` | спільні правила чесного бою: ліміт 30, ×0,25 без гравця, відстань і арена, скидання за ігровим часом, сеанс сервера, бонус оглушення без `/kill`, ремонт NaN |
| `entity/RovingCube.java`, `ai/goal/Cube*Goal.java` | символ із восьми клітинок, пошук раз на 2 с, ловіння талісманом, перестрибування на тому ж поверсі |
| `entity/monster/HarbingerCube.java` | ближній удар, атрибути, зона удару |
| `entity/projectile/RuneBolt.java` | рунний снаряд (бос і скіпетр) |
| `entity/projectile/CubeOfAnnihilation.java` | лише тег замку, без блоків із вмістом, `BreakEvent` |
| `item/RuneScepterItem.java` | Рунний скіпетр |
| `block/entity/spawner/FinalBossSpawnerBlockEntity.java` | спавнер оживає, коли гравець в альтанці (не в донжоні під нею) |
| `block/entity/spawner/ForgeHarbingerSpawnerBlockEntity.java`, `enums/BossVariant.java` | спавнер кузні |
| `world/.../FinalCastleDungeonExitComponent.java` | **виправлення TF**: `minZ` → `minX` (§4) |
| `world/.../FinalCastleDungeonForgeRoomComponent.java` | спавнер замість таблички |
| `world/.../FinalCastleDungeonRoom31Component.java` | центр кімнати (для запасного спавнера) |
| `world/.../FinalCastleUpgrade.java` | оновлення замків на льоту (§5) |
| `world/.../type/FinalCastleStructure.java` | списки появи мобів |
| `events/EntityEvents.java` | натяк на плато замість «WIP»; підписка оновлення замків |
| `client/model/entity/RuneSovereignModel.java`, `ForgeHarbingerModel.java`, `RuneWardModel.java`, `DebugPose.java` | моделі й анімації; тестовий стенд поз (вмикається лише `-Dtwilightforest.debugPoses=true`) |
| `client/renderer/entity/RuneSovereignRenderer.java`, `ForgeHarbingerRenderer.java`, `RuneWardRenderer.java` | рендери з шаром світіння |
| `init/*`, `client/event/ClientRegistrationEvents.java`, `client/model/TFModelLayers.java` | реєстрація |
| `data/*` | генератори даних TF: назви, добич, досягнення, теги, моделі предметів, рецепт перезарядки, творча вкладка |

**Нові id:** сутності `forge_harbinger`, `rune_ward`, `rune_bolt`; блок `forge_harbinger_boss_spawner`; предмет `rune_scepter`; яйця `plateau_boss_spawn_egg`, `forge_harbinger_spawn_egg`; досягнення `progress_castle`, `progress_talisman`, `progress_annihilation`, `progress_final`. Жоден наявний id не змінено.

## 4. Знайдена помилка самого TF

`FinalCastleDungeonExitComponent.addChildren` у TF 4.x передає `boundingBox.minZ() + 15` як **x** сходів на другий рівень підземелля (у коді Benimatic 2016 року там `minX`). Через це другий рівень і кузня опиняються за сотні блоків від замку, далі за 8 чанків від початку структури. А структура ставить шматки лише в межах 8 чанків, тож вони ніколи не генерувались. На тестовому світі: замок на x ≈ -745, кузня на x ≈ 188…238. Після виправлення в новому замку кузня лягла приблизно за 100 блоків від початку структури й згенерувалась зі спавнером.

## 5. Старі замки (оновлення на льоту)

`FinalCastleUpgrade` знаходить тегований `interaction` під написами «WIP» у шаблоні альтанки двома шляхами:

1. подія `EntityJoinLevelEvent` — коли чанк читається з диска;
2. `ChunkEvent.Load` для чанків із посиланням на Кінцевий замок — через секунду дивиться в стовпець чанка з серединою альтанки. Це для щойно згенерованих чанків: їхні сутності приходять без події.

Сама робота виконується на наступному тіку виміру (не посеред завантаження чанка), по замку за тік:

- **альтанка:** спавнер на місці jigsaw, якщо замок не підкорено й там повітря; написи й маркер зникають, тож повторно оновлення не спрацює;
- **кузня:** стара табличка «Mini-boss 2» стає спавнером. Якщо кузня лежить поза досяжними чанками (помилка з §4), спавнер стає в центр найглибшої досяжної кімнати підземелля.

## 6. Відновлення попередньої версії (відкат)

**Повернути оригінальний TF** (світ при цьому не ламається, але див. «Що станеться зі світом»):

1. Зупинити сервер.
2. У кожній із тек `client/mods`, `server/mods`, `testserver/mods` (і на Crafty, якщо туди заливали):
   - перейменувати `wilds_twilightforest-1.21.1-4.8.3345-w1.jar` → `….jar.disabled`;
   - перейменувати `twilightforest-1.21.1-4.8.3345-universal.jar.replaced` → `twilightforest-1.21.1-4.8.3345-universal.jar`.

   Оригінали з SHA-256 лежать і в `_backup/twilightforest-4.8.3345-original/` (`SHA256SUMS.txt`, sha256 `493da16d…aba35`).
3. Повернути запис у `docs/mods.json` (`"where": "both"`, прибрати `replaced_by`) і рядок `227639: ("twilightforest", "both")` у `pack/tools/build_neo.py`. Найпростіше — `git revert` коміту завершення.
4. Квести: повернути `pack/tools/quests/chapters/c10_twilight.py` і перезапустити `gen_quests.py`. Переклади: повернути `pack/i18n/uk|fixes/twilightforest.json` і запустити `i18n.py build`. Вікі: повернути `gen_bosses.py`, `gen_guides.py`, назву споруди й перезібрати мод вікі.

**Що станеться зі світом після відкату:**

- Сутності `forge_harbinger`, `rune_ward`, `rune_bolt` і блок `forge_harbinger_boss_spawner` зникнуть (у журналі будуть попередження про невідомі id). Предмети `rune_scepter` зникнуть з інвентарів.
- `plateau_boss` і `final_boss_boss_spawner` лишаться (вони були в TF), але бос знову стане «порожнім».
- Замки, вже оновлені на льоту, лишаться без написів «WIP»: це нешкідливо.
- Досягнення, яких більше немає, гра просто ігнорує.

## 7. Виправлення, внесені після тестів (у патчі 0003)

Див. [changelog.md](changelog.md).
