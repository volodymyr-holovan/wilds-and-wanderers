CHAPTER = {
    "key": "nether_end",
    "title": "&cНезер &7і &5Енд",
    "subtitle": "Новий Незер, рибалка в лаві, дракон і зовнішні острови",
    "group": "Пригоди",
    "icon": "minecraft:crying_obsidian",
    "quests": [
        # --- the Nether ---
        {
            "key": "portal", "title": "Брама в Незер", "icon": "minecraft:obsidian", "shape": "gear", "size": 1.5,
            "desc": [
                "Незер у цьому світі переписано: вісім нових біомів &6Incendium&r, маєтки й катакомби &6Eternal Nether&r, перебудовані фортеці, живі лавові моря й фрукти в кожному лісі.",
                "",
                "Складіть рамку з &aобсидіану&r 4×5 (кути можна пропустити) і підпаліть її кресалом. Обсидіан дає вода, вилита на лаву; добувають його алмазним кайлом.",
                "",
                "&6Перед дорогою:&r золота броня (хоча б одна частина) — пігліни не нападуть; вогнестійкість, блоки й кресало, щоб запалити портал назад."
            ],
            "tasks": ["dim:minecraft:the_nether"], "rewards": ["item:minecraft:golden_helmet", "item:minecraft:fire_charge*4", "xp:50"], "skill": 80,
        },
        {
            "key": "new_biomes", "title": "Вісім нових біомів", "icon": "minecraft:crimson_nylium", "deps": ["portal"],
            "desc": [
                "Знайдіть попелясті пустища й кварцові рівнини — два з восьми біомів Incendium. Кожен небезпечний по-своєму:",
                " • &7Попелясті пустища&r — тут стоїть Заборонений замок;",
                " • &eПекельні дюни&r — пустельні іфрити;",
                " • &fКварцові рівнини&r — застигла лава, Санктум і Кварцова кухня;",
                " • &aОтруйне звалище&r — радіація й отруйні слизні;",
                " • &6Вулканічні дельти&r — давні уламки просто на поверхні.",
                "",
                "Біом показує мінімапа й F3; вікі (O) має статтю про кожен."
            ],
            "guide": {"find": "Компас природи (Nature's Compass) знайде біом за назвою.", "danger": 3},
            "tasks": ["biome:incendium:ash_barrens", "biome:incendium:quartz_flats"],
            "rewards": ["loot:wilds:reward/explorer", "xp:80"], "skill": 80,
        },
        {
            "key": "fortress", "title": "Фортеця Незеру", "icon": "minecraft:nether_bricks", "deps": ["portal"],
            "desc": [
                "Фортеці Незеру перебудовано (YUNG's Better Nether Fortresses): більші, з мостами, залами й в'язницями. Тут живуть &cіфрити&r і &8візер-скелети&r.",
                "",
                "Вам потрібні &aстрижні іфрита&r: з них варять зілля (варильна стійка) і роблять &aоко Енду&r."
            ],
            "guide": {"where": "темні цегляні мости над лавою; часто між біомами.", "danger": 3,
                      "gear": "щит, лук, вогнестійкість", "find": "Компас дослідника → «fortress».",
                      "tip": "візер-скелети накладають іссушення — тримайте молоко."},
            "tasks": ["item:minecraft:blaze_rod*6"], "rewards": ["item:minecraft:brewing_stand", "item:minecraft:nether_wart*8", "xp:100"], "skill": 100,
        },
        {
            "key": "castle", "title": "Заборонений замок", "icon": "minecraft:gilded_blackstone", "deps": ["new_biomes"],
            "desc": [
                "Найбільша споруда Incendium — фортеця піглінів посеред попелястих пустищ. Лицарі, лучники, алхіміки, піромани й розвідники; у холодильній кімнаті чекає &bКрижаний клинок&r.",
                "",
                "Зброя Incendium — особливі версії звичайних предметів із власними властивостями: читайте підказки."
            ],
            "guide": {"where": "лише в біомі «Попелясті пустища».", "danger": 4, "gear": "алмазна броня, щит, золото",
                      "find": "Компас дослідника → «forbidden_castle»."},
            "tasks": ["struct:incendium:forbidden_castle"], "rewards": ["loot:wilds:reward/boss", "xp:250"], "skill": 150,
        },
        {
            "key": "sanctum", "title": "Санктум", "icon": "minecraft:quartz_pillar", "deps": ["new_biomes"],
            "desc": [
                "Кварцовий притулок розбійників у кварцових рівнинах: сховище, вартові й ілюзіоністи. З ілюзіоністів часто падає &aСувій повернення&r."
            ],
            "guide": {"where": "біом «Кварцові рівнини».", "danger": 4, "find": "Компас дослідника → «sanctum»."},
            "tasks": ["struct:incendium:sanctum"], "rewards": ["loot:wilds:reward/explorer", "xp:200"], "skill": 120,
        },
        {
            "key": "inferno", "title": "Ширяюче пекло", "icon": "minecraft:nether_star", "deps": ["castle", "sanctum"],
            "desc": [
                "Бос Incendium. Принесіть &aзірку Незеру&r на &cПекельний вівтар&r — і з полум'я постане Ширяюче пекло.",
                "",
                "Нагорода — &6Пекельні крила&r, «Вогняна буря» чи один зі щитів. Зірку Незеру дає візер."
            ],
            "guide": {"danger": 5, "gear": "незеритова броня, вогнестійкість, зцілення", "find": "Компас дослідника → «infernal_altar».",
                      "tip": "бийтеся там, де є куди відступити від вогню."},
            "tasks": ["struct:incendium:infernal_altar", "item:minecraft:nether_star"], "rewards": ["loot:wilds:reward/boss", "xp:500"], "skill": 300,
        },
        {
            "key": "manor", "title": "Маєток піглінів", "icon": "eternalnether:netherite_bell", "deps": ["portal"],
            "desc": [
                "У багряному лісі стоїть маєток &6Eternal Nether&r. Його стережуть пігліни-мисливці зі щитами й абордажними шаблями, а в підвалах сидять полонені.",
                "",
                "Визвольте &aполоненого пігліна&r: потрібні золото й &aнезеритовий дзвін&r. Вдячний піглін дарує &6позолочений незеритовий щит&r — він ніколи не ламається."
            ],
            "guide": {"where": "багряний ліс.", "danger": 3, "find": "Компас дослідника → «piglin_manor»."},
            "tasks": ["struct:eternalnether:piglin_manor", "item:eternalnether:gilded_netherite_shield"], "rewards": ["loot:wilds:reward/fantasy", "xp:200"], "skill": 120,
        },
        {
            "key": "citadel", "title": "Химерна цитадель", "icon": "eternalnether:warped_ender_pearl", "deps": ["portal"],
            "desc": [
                "Вежа в химерному лісі, де живуть &3химерні ендермени&r — вони нападають, щойно побачать вас.",
                "",
                "Химерного ендермена можна очистити: зрізайте з нього нарости &aножицями&r, поки він не стане звичайним. Принесіть &aхимерну перлину Енду&r."
            ],
            "guide": {"where": "химерний ліс.", "danger": 4, "gear": "гарбуз на голову, щоб не злити ендерменів поглядом", "find": "Компас дослідника → «citadel»."},
            "tasks": ["struct:eternalnether:citadel", "item:eternalnether:warped_ender_pearl"], "rewards": ["loot:wilds:reward/explorer", "xp:200"], "skill": 120,
        },
        {
            "key": "catacomb", "title": "Катакомби візера", "icon": "eternalnether:withered_bone", "deps": ["fortress"],
            "desc": [
                "Величезна руїна у формі візера. Візер-скелети-лицарі, корпори, мари й вексі. У «ребрах» сховано скарби, серед них — платівка «Вальс візера».",
                "",
                "Переможіть &8візер-скелета лицаря&r."
            ],
            "guide": {"danger": 4, "gear": "молоко проти іссушення, сокира", "find": "Компас дослідника → «catacomb»."},
            "tasks": ["kill:eternalnether:wither_skeleton_knight"], "rewards": ["loot:wilds:reward/boss", "xp:250"], "skill": 150,
        },
        {
            "key": "lava_rod", "title": "Рибалка в лаві", "icon": "netherdepthsupgrade:lava_fishing_rod", "deps": ["fortress"],
            "desc": [
                "Лавові моря Незеру живі: дев'ять видів риб. Для лави потрібна &aлавова вудка&r — з незеритових уламків, стрижнів іфрита й ланцюгів.",
                "",
                "Улов корисний: блейзриба дає порох іфрита, світлосардина — світлопил, обсидіанова риба — обсидіан, а з риби-ока, ножиць і пороху іфрита виходить &aоко Енду&r."
            ],
            "tasks": ["item:netherdepthsupgrade:lava_fishing_rod", "item:netherdepthsupgrade:eyeball_fish"], "rewards": ["item:minecraft:blaze_powder*6", "loot:wilds:reward/sea", "xp:150"], "skill": 100,
        },
        {
            "key": "fruit", "title": "Фрукти Незеру", "icon": "netherexotics:kiwano", "deps": ["portal"],
            "desc": [
                "У кожному лісі Незеру росте свій фрукт: &3ківано&r (химерний ліс, вогнестійкість), &cпітая&r (багряний ліс, знімає візер), &6рамбутан&r (пустища, регенерація), &5жаботикаба&r (базальтові дельти, знімає ефекти), &eрука Будди&r (пісок душ, нічний зір).",
                "",
                "Ножем на дошці ківано ріжеться на скибки й дає шкірку — з неї броня й знаряддя, які самі переплавляють здобуте."
            ],
            "tasks": ["item:netherexotics:kiwano*3", "item:netherexotics:rambutan*3"], "rewards": ["item:farmersdelight:cutting_board", "xp:80"], "skill": 60,
        },
        {
            "key": "eyes", "title": "Очі Енду", "icon": "minecraft:ender_eye", "deps": ["fortress"],
            "desc": [
                "Око Енду — порох іфрита і перлина Енду (або око риби-ока з лавових морів). Киньте його — воно полетить до найближчої &aфортеці Енду&r під землею.",
                "",
                "Для порталу в Енд потрібно до дванадцяти очей: частина рамок уже може мати око. Візьміть запас — очі інколи розбиваються в польоті."
            ],
            "guide": {"find": "Око Енду показує напрям; фортеці перебудовано (YUNG's Better Strongholds), портал — у глибині.", "danger": 3},
            "tasks": ["item:minecraft:ender_eye*12"], "rewards": ["loot:wilds:reward/explorer", "xp:150"], "skill": 120,
        },
        # --- the End ---
        {
            "key": "end", "title": "Енд", "icon": "minecraft:end_stone", "deps": ["eyes"], "shape": "gear", "size": 1.5,
            "desc": [
                "Острів дракона перебудовано (YUNG's Better End Island): у центрі — &5вежа з дзвоном&r. Дракон прилітає не одразу, а коли ви наближаєтеся до вежі, тож є час роздивитися.",
                "",
                "&6Візьміть:&r лук і багато стріл, блоки, гарбуз на голову (від ендерменів), відро води, зілля повільного падіння."
            ],
            "tasks": ["dim:minecraft:the_end"], "rewards": ["item:minecraft:arrow*64", "item:minecraft:carved_pumpkin", "xp:100"], "skill": 100,
        },
        {
            "key": "dragon", "title": "Дракон Енду", "icon": "minecraft:dragon_head", "deps": ["end"], "size": 1.5,
            "desc": [
                "Розбийте кристали на колонах — вони лікують дракона, — і перемагайте. Дракона можна викликати знову: поставте чотири &aкристали Енду&r на блоки корінної породи в чотирьох крилах вежі.",
                "",
                "Після перемоги відкриються шлюзи до зовнішніх островів."
            ],
            "guide": {"danger": 5, "gear": "лук із «Силою», алмазна броня, зцілення", "tip": "кристали в клітках розбивайте зблизька."},
            "tasks": ["kill:minecraft:ender_dragon"], "rewards": ["loot:wilds:reward/boss", "xp:800"], "skill": 400,
        },
        {
            "key": "outer_end", "title": "Зовнішні острови", "icon": "enderscape:celestial_chanterelle", "deps": ["dragon"],
            "desc": [
                "За шлюзом починається &6Enderscape&r: &bнебесні гаї&r велетенських грибів, &7серпанкові ліси&r, &8осквернені пустища&r і &dмагнієві скелі&r, що притягують і відштовхують метал.",
                "",
                "Знайдіть небесний гай — там пливуть повітрям &aдрейфуни&r. Клацніть по дрейфуну порожньою пляшкою, щоб набрати &aдрейфового желе&r."
            ],
            "tasks": ["biome:enderscape:celestial_grove", "item:enderscape:drift_jelly_bottle"], "rewards": ["loot:wilds:reward/explorer", "xp:150"], "skill": 100,
        },
        {
            "key": "nebulite", "title": "Небуліт і шадолін", "icon": "enderscape:nebulite", "deps": ["outer_end"],
            "desc": [
                "Дві руди Енду: &dнебуліт&r — енергія для знарядь Енду (з уламків складається небуліт) і &8шадолін&r — метал, що плавиться в злитки.",
                "",
                "Жорна Create і збагачувач Mekanism дають з руди більше, а Ars Nouveau заряджає уламки небуліту в джерельні самоцвіти."
            ],
            "tasks": ["item:enderscape:nebulite*2", "item:enderscape:shadoline_ingot*6"], "rewards": ["loot:wilds:reward/tech", "xp:200"], "skill": 120,
        },
        {
            "key": "attractor", "title": "Магнієвий притягувач", "icon": "enderscape:magnia_attractor", "deps": ["nebulite"],
            "desc": [
                "Паросток притягувальної й паросток відштовхувальної магнії з магнієвих скель плюс злитки шадоліну — і ви тягнете предмети здалеку. Працює на небуліті."
            ],
            "tasks": ["item:enderscape:magnia_attractor"], "rewards": ["item:enderscape:nebulite_shards*8", "xp:150"], "skill": 80,
        },
        {
            "key": "end_city", "title": "Ключ від сховища", "icon": "enderscape:end_city_key", "deps": ["dragon"],
            "desc": [
                "Міста Енду перебудовано: у них стоять &5випробувальні породжувачі Енду&r. Переможіть їхніх істот — випаде &aключ від міста Енду&r. Він відмикає &5сховище Енду&r: дзеркала, небуліт, платівки й шаблон оздоблення «Стазис».",
                "",
                "На кораблі міста — окреме сховище з &aелітрами&r."
            ],
            "guide": {"danger": 4, "gear": "відро води й повільне падіння: шалкери підкидають у повітря", "find": "Компас дослідника → «end_city»."},
            "tasks": ["item:enderscape:end_city_key"], "rewards": ["loot:wilds:reward/explorer", "xp:250"], "skill": 150,
        },
        {
            "key": "mirror", "title": "Дзеркало", "icon": "enderscape:mirror", "deps": ["end_city", "nebulite"],
            "desc": [
                "&aДзеркало&r зі сховища міста Енду переносить до &aмагнетиту&r: клацніть дзеркалом по магнетиту, заправте його небулітом — і повертайтеся додому будь-коли.",
                "",
                "Зачарування «Міжвимірність» дозволяє переноситися навіть з іншого виміру."
            ],
            "tasks": ["item:enderscape:mirror"], "rewards": ["item:minecraft:lodestone", "item:enderscape:nebulite*2", "xp:200"], "skill": 120,
        },
        {
            "key": "elytra", "title": "Крила", "icon": "minecraft:elytra", "deps": ["end_city"], "size": 1.5,
            "desc": [
                "Елітри — найкращий спосіб подорожувати. Феєрверки дають тягу, «Відновлення» лагодить їх досвідом.",
                "",
                "Enderscape додає елітрам чари: &aВідскок&r (затисніть стрибок у польоті) і &aСвітлова швидкість&r (далі на одному розгоні)."
            ],
            "tasks": ["item:minecraft:elytra"], "rewards": ["item:minecraft:firework_rocket*32", "xp:300"], "skill": 200,
        },
    ],
}
