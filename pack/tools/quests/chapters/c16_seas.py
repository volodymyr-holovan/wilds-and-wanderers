CHAPTER = {
    "key": "seas",
    "title": "&bМоря і кораблі",
    "subtitle": "Верф, гармати, затонулі міста й морські звірі",
    "group": "Пригоди",
    "icon": "smallships:cannon",
    "quests": [
        {
            "key": "dockyard", "title": "Верф", "icon": "smallships:dockyard", "size": 1.5, "shape": "hexagon",
            "desc": [
                "Кораблі в цьому світі будують на &aверфі&r. Поставте її біля глибокої води, покладіть у неї матеріали і натисніть «Збудувати» — корабель зійде на воду поруч.",
                "",
                "&eЯкі бувають:&r",
                " • &aког&r — одна щогла, до 4 гармат; любить попутний вітер;",
                " • &aгалера&r і &aдраккар&r — весла й вітрило, пливуть навіть проти вітру;",
                " • &aкаравела&r — ходить і проти вітру, і бокового;",
                " • &aбриг&r — дві щогли, 6 гармат; &aгалеон&r — три щогли, 10 гармат;",
                " • &aдау&r — швидка при боковому вітрі.",
                "Корабель можна зробити з будь-якого дерева, навіть з дерев Сутінкового лісу й Етеру."
            ],
            "guide": {
                "gear": "залізо, мотузки (поводки), шерсть, папір, бочка",
                "tip": "верф також ремонтує корабель, фарбує вітрила й ставить на них прапор."
            },
            "tasks": ["item:smallships:dockyard"], "rewards": ["loot:wilds:reward/sea", "xp:50"], "skill": 60,
        },
        {
            "key": "ship", "title": "Свій корабель", "icon": "smallships:oak_cog", "deps": ["dockyard"],
            "desc": [
                "Збудуйте перший корабель і вийдіть у море. Корабель — це ваша плавуча база: у трюмі є скрині, а на палубі поміститься друг.",
                "&eКерування:&r W/S — вітрило й швидкість, A/D — кермо. Вітер змінюється; стежте за прапорцем на щоглі."
            ],
            "guide": {
                "tip": "візьміть на корабель &aнамет&r — і ви матимете дім у будь-якій бухті."
            },
            "tasks": ["check"], "rewards": ["item:smallships:cotton_sails", "xp:80"], "skill": 80,
        },
        {
            "key": "cannons", "title": "Гармати", "icon": "smallships:cannon", "deps": ["ship"],
            "desc": [
                "Гармату ставлять на борт через верф. Набої:",
                " • &aядро&r — звичайний постріл;",
                " • &aланцюгове ядро&r — рве вітрила;",
                " • &aкартеч&r — близько, але страшно для екіпажу;",
                " • &aдрібний порох&r — +50% дальності при перезарядці.",
                "",
                "&dЗв'язок світу:&r піратські кораблі Seven Seas і нежиті пірати теж мають гармати — тепер ви на рівних."
            ],
            "tasks": ["item:smallships:cannon", "item:smallships:cannon_ball*16"], "rewards": ["loot:wilds:reward/sea", "xp:100"], "skill": 80,
        },
        {
            "key": "pirates", "title": "Піратський корабель", "icon": "minecraft:skeleton_skull", "deps": ["cannons"],
            "desc": [
                "Посеред океану дрейфують кораблі: фрегати, галеони, корвети корсарів і корабель-привид із нежиттю. Зачистіть палубу, і трюм ваш."
            ],
            "guide": {
                "where": "відкритий океан, далеко від берега.",
                "danger": 4,
                "gear": "лук, щит, зілля підводного дихання (якщо впадете за борт)",
                "find": "Компас дослідника → «undead_pirate_ship», «victory_frigate», «unicorn_galleon», «corsair_corvette».",
                "tip": "підпливайте з навітряного боку і стріляйте з гармат, поки екіпаж не вибіг на палубу."
            },
            "tasks": ["struct:dungeons_arise:undead_pirate_ship"], "rewards": ["loot:wilds:reward/sea", "loot:wilds:reward/boss", "xp:250"], "skill": 200,
        },
        {
            "key": "diving", "title": "Водолазний костюм", "icon": "hybrid_aquatic:diving_helmet", "deps": ["dockyard"],
            "desc": [
                "Hybrid Aquatic додає справжній &aводолазний костюм&r: шолом, костюм, штани й черевики. З &aважками&r ви ходите дном, як по суші; &aсвітна паличка&r освітлює глибину.",
                "Шаблон &aпосилення костюма&r трапляється у скринях."
            ],
            "guide": {"tip": "у глибокому морі живуть акули: блокуйте їхній укус щитом — так здобувають &aакулячий зуб&r."},
            "tasks": ["item:hybrid_aquatic:diving_helmet", "item:hybrid_aquatic:diving_suit"], "rewards": ["item:hybrid_aquatic:glowstick*4", "xp:100"], "skill": 80,
        },
        {
            "key": "city", "title": "Підводне місто", "icon": "minecraft:prismarine_bricks", "deps": ["diving"],
            "desc": [
                "На дні лежать руїни: &aпідводне місто&r, малі руїни, скам'янілості, &aАтлантида&r з трибуною й садами, підводні святилища."
            ],
            "guide": {
                "where": "глибокий океан; малі руїни — і в теплих морях.",
                "danger": 3,
                "gear": "водолазний костюм або зілля підводного дихання, «Підводна хода»",
                "find": "Компас дослідника → «underwater/underwater_city», «ruins_atlantis», «underwater_shrine».",
                "tip": "у руїнах вартують утоплені й вартові; беріть тризуб або арбалет."
            },
            "tasks": ["struct:hopo:underwater/underwater_city"], "rewards": ["loot:wilds:reward/sea", "xp:200"], "skill": 150,
        },
        {
            "key": "whalefall", "title": "Кістки кита", "icon": "minecraft:bone_block", "deps": ["diving"],
            "desc": [
                "&aКит-могильник&r — скелет велетенського кита на дні. Навколо нього збирається дивне життя: крабі, черви, глибоководні риби."
            ],
            "guide": {
                "where": "глибокі океани, на самому дні.",
                "danger": 2,
                "find": "Компас дослідника → «whale_fall».",
                "tip": "тут ловлять рідкісних глибоководних мешканців сачком для істот."
            },
            "tasks": ["struct:hybrid_aquatic:whale_fall"], "rewards": ["loot:wilds:reward/sea", "xp:120"], "skill": 100,
        },
        {
            "key": "monument", "title": "Океанський монумент", "icon": "minecraft:prismarine_crystals", "deps": ["city"],
            "desc": [
                "Перебудований монумент — справжнє підводне підземелля з крилами, скарбницями і трьома старшими вартовими."
            ],
            "guide": {
                "where": "глибокий океан.",
                "danger": 4,
                "gear": "молоко від втоми, зілля підводного дихання, тризуб або «Спритність у воді»",
                "find": "Компас дослідника → «ocean_monument» (Better Ocean Monuments).",
                "tip": "&aокеанська кухня&r (Ocean's Delight) готує м'ясо вартових — сушіть і смажте їх."
            },
            "tasks": ["kill:minecraft:elder_guardian"], "rewards": ["loot:wilds:reward/sea", "loot:wilds:reward/boss", "xp:300"], "skill": 200,
        },
        {
            "key": "thrasher", "title": "Молотильник", "icon": "upgrade_aquatic:thrasher_tooth", "deps": ["monument"],
            "desc": [
                "Великий морський звір Upgrade Aquatic. Він б'є звуковою хвилею і хапає здобич. Нагорода — &aзуб молотильника&r для ліхтарів і плитки."
            ],
            "guide": {
                "where": "глибокі холодні океани.",
                "danger": 4,
                "find": "пливіть кораблем над глибоким холодним океаном — він сам вас знайде.",
                "tip": "бийтеся з корабля, а не у воді."
            },
            "tasks": ["kill:upgrade_aquatic:thrasher"], "rewards": ["loot:wilds:reward/sea", "xp:250"], "skill": 150,
        },
        {
            "key": "conch", "title": "Зловісна мушля", "icon": "hybrid_aquatic:ominous_conch", "deps": ["diving"],
            "desc": [
                "&aРак-самітник&r на березі міняє черепашки на свої скарби. Найрідкісніший — &aзловісна мушля&r: «Глибокий океан кличе зсередини». Що відповість на її поклик — дізнайтеся самі."
            ],
            "guide": {
                "where": "пляжі теплих морів, біля води.",
                "danger": 5,
                "tip": "не дмухайте в мушлю без повного спорядження і друзів поруч."
            },
            "tasks": ["item:hybrid_aquatic:ominous_conch"], "rewards": ["xp:150"], "skill": 100,
        },
        {
            "key": "fish", "title": "Риби семи морів", "icon": "fishofthieves:splashtail", "deps": ["dockyard"],
            "desc": [
                "Fish of Thieves населяє води рибами з характером: &aсплештейли&r, &aпондії&r, &aайлгопери&r, &aштормові риби&r, що клюють лише в грозу, і рідкісні &aдревньолуські&r.",
                "&aГачки&r Hybrid Aquatic у другій руці пришвидшують риболовлю вдень (колючий) чи вночі (світний), а &aмагнітний&r — частіше дає скарби."
            ],
            "guide": {"tip": "у &aморській пошті&r (Компас дослідника → «seapost») можна поповнити запаси й знайти наживку."},
            "tasks": ["item:fishofthieves:splashtail", "item:fishofthieves:pondie", "item:fishofthieves:islehopper"], "rewards": ["item:hybrid_aquatic:barbed_hook", "xp:60"], "skill": 50,
        },
        {
            "key": "kitchen", "title": "Морська кухня", "icon": "oceansdelight:fugu_roll", "deps": ["fish"],
            "desc": [
                "Ocean's Delight навчає готувати морське: ролли з фугу, кільця кальмара, суп з вартового. Риба Fish of Thieves і Hybrid Aquatic підходить у рецепти як звичайна сира риба."
            ],
            "tasks": ["item:oceansdelight:fugu_roll", "item:oceansdelight:squid_rings"], "rewards": ["loot:wilds:reward/explorer", "xp:60"], "skill": 50,
        },
        {
            "key": "villages", "title": "Морські поселення", "icon": "minecraft:lily_pad", "deps": ["ship"],
            "desc": [
                "На воді теж живуть: &aселище на лататті&r з ринком і храмом, &aприпливні містечка&r на палях, рибальські хатки, маяки."
            ],
            "guide": {
                "where": "береги і мілке море; містечка на лататті — у болотах і теплих водах.",
                "danger": 1,
                "find": "Компас дослідника → «ocean_lily_pad_village:market», «village_ocean», «lighthouse».",
                "tip": "морські рибалки торгують гачками, кораблями й гарматами."
            },
            "tasks": ["struct:ocean_lily_pad_village:market"], "rewards": ["loot:wilds:reward/explorer", "xp:100"], "skill": 80,
        },
        {
            "key": "leviathan", "title": "Морські владики", "icon": "cataclysm:tidal_claws", "deps": ["monument", "bosses:leviathan"],
            "desc": [
                "Найбільші загрози морів — &aЛевіятан&r затонулого міста і &aСцилла&r з Акрополя. Їх бої описані в главі &4Великі боси&r; тут — нагорода моряка за обох."
            ],
            "tasks": ["kill:cataclysm:the_leviathan", "kill:cataclysm:scylla"], "rewards": ["loot:wilds:reward/sea", "loot:wilds:reward/boss", "xp:500"], "skill": 300,
        },
    ],
}
