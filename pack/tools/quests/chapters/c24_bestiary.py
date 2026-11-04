"""Звіринець: Alex's Mobs and Friends & Foes, the animals worth meeting and what they give."""
CHAPTER = {
    "key": "bestiary",
    "title": "&aЗвіринець",
    "subtitle": "Дивовижні звірі світу",
    "group": "Пригоди",
    "icon": "alexsmobs:animal_dictionary",
    "quests": [
        {
            "key": "dictionary", "title": "Книга звірів", "icon": "alexsmobs:animal_dictionary", "size": 1.5, "shape": "hexagon",
            "subtitle": "Усе про тварин Alex's Mobs",
            "desc": [
                "&aЕнциклопедія тварин&r розповідає про кожну тварину мода: де живе, що їсть, чим корисна. Її крафтять із книги, зеленого барвника й будь-якої здобичі з тварин мода.",
                "А ще все це є у вікі (&aO&r) — наведіть приціл на тварину й відкрийте вікі."
            ],
            "tasks": ["item:alexsmobs:animal_dictionary"], "rewards": ["xp:30"], "skill": 30,
        },
        {
            "key": "banana", "title": "Банани з джунглів", "icon": "alexsmobs:banana", "deps": ["dictionary"],
            "desc": [
                "Із листя джунглів зрідка падають &aбанани&r. Їх люблять мавпи: капуцина можна приручити, і він кидатиме дротики у ворогів.",
                "Обережно зі шкіркою: на ній ковзають."
            ],
            "tasks": ["item:alexsmobs:banana*4"], "rewards": ["xp:30"], "skill": 20,
        },
        {
            "key": "roadrunner", "title": "Бігун пустелі", "icon": "alexsmobs:roadrunner_feather", "deps": ["dictionary"],
            "desc": [
                "Дорожні бігуни живуть у пустелях і скидають &aпір'я&r. З нього шиють &aчоботи бігуна&r: у них ви швидше бігаєте по піску."
            ],
            "tasks": ["item:alexsmobs:roadrunner_feather*3"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "bison", "title": "Хутро бізона", "icon": "alexsmobs:bison_fur", "deps": ["dictionary"],
            "desc": [
                "Бізони пасуться на засніжених рівнинах. Постригти їх ножицями — отримаєте &aхутро&r на теплі килими й блоки."
            ],
            "tasks": ["item:alexsmobs:bison_fur*4"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "moose", "title": "Лосині роги", "icon": "alexsmobs:moose_antler", "deps": ["bison"],
            "desc": [
                "Лосі в тайзі взимку скидають &aроги&r. Два роги, залізний злиток і нитки — і в вас лосиний шолом."
            ],
            "tasks": ["item:alexsmobs:moose_antler*2"], "rewards": ["xp:50"], "skill": 40,
        },
        {
            "key": "crocodile", "title": "Крокодил", "icon": "alexsmobs:crocodile_scute", "deps": ["banana"],
            "desc": [
                "Крокодили ховаються в мангрових болотах і річках пустель. Підходьте обережно: вони хапають і тягнуть під воду.",
                "З семи &aщитків&r виходить крокодилячий нагрудник."
            ],
            "tasks": ["kill:alexsmobs:crocodile", "item:alexsmobs:crocodile_scute*2"], "rewards": ["loot:wilds:reward/explorer", "xp:80"], "skill": 60,
        },
        {
            "key": "grizzly", "title": "Ведмідь грізлі", "icon": "alexsmobs:bear_fur", "deps": ["bison"],
            "desc": [
                "Грізлі живуть у лісах і люблять мед і лосося. Розлючений ведмідь дуже небезпечний.",
                "Ведмедя можна приручити: годуйте його лососем, коли він не сердиться."
            ],
            "tasks": ["item:alexsmobs:bear_fur"], "rewards": ["xp:60"], "skill": 50,
        },
        {
            "key": "shark", "title": "Акулячий зуб", "icon": "alexsmobs:shark_tooth", "deps": ["dictionary"],
            "desc": [
                "Молотоголові й інші акули полюють у теплих морях. Їхні &aзуби&r йдуть на гострі акулячі стріли."
            ],
            "tasks": ["item:alexsmobs:shark_tooth*3"], "rewards": ["item:minecraft:arrow*32", "xp:60"], "skill": 40,
        },
        {
            "key": "mimic", "title": "Восьминіг-мімік", "icon": "alexsmobs:mimic_octopus_bucket", "deps": ["shark"],
            "desc": [
                "Восьминоги-міміки змінюють колір і вигляд. Злийте одного у відро — і він житиме у вашому акваріумі."
            ],
            "tasks": ["item:alexsmobs:mimic_octopus_bucket"], "rewards": ["xp:60"], "skill": 40,
        },
        {
            "key": "void_worm", "title": "Пустотний черв", "icon": "alexsmobs:void_worm_eye", "deps": ["mimic", "crocodile"], "size": 1.5, "shape": "gear",
            "subtitle": "Найбільша тварина Енду",
            "desc": [
                "Велетенський &cпустотний черв&r живе в безодні Енду. Він живе в безодні під островами; &aефігія пустотного черва&r допомагає його покликати.",
                "Він прокладає портали й кидається на вас з-під острова. Беріть крила або елітри."
            ],
            "tasks": ["kill:alexsmobs:void_worm"], "rewards": ["loot:wilds:reward/boss", "xp:400"], "skill": 200,
        },
        {
            "key": "crab", "title": "Крабова клешня", "icon": "friendsandfoes:crab_claw", "deps": ["dictionary"],
            "desc": [
                "Краби (Friends & Foes) бігають мангровими пляжами. З їхньої &aклешні&r варять зілля, що подовжує досяжність рук."
            ],
            "tasks": ["item:friendsandfoes:crab_claw"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "penguin", "title": "Пінгвіни", "icon": "friendsandfoes:penguin_feather", "deps": ["crab"],
            "desc": [
                "Пінгвіни гуляють біля холодних морів. Не кривдьте їх — а &aпір'я&r вони скидають самі."
            ],
            "tasks": ["item:friendsandfoes:penguin_feather*2"], "rewards": ["xp:40"], "skill": 30,
        },
        {
            "key": "totems", "title": "Тотеми ілладжерів", "icon": "friendsandfoes:totem_of_freezing", "deps": ["crab"],
            "desc": [
                "Нові ілладжери Friends & Foes носять рідкісні тотеми. Вони спрацьовують, коли здоров'я падає нижче половини: &aтотем заморожування&r скує ворогів кригою, &aтотем ілюзії&r створить ваших двійників.",
                "Шукайте їх у мешканців особняків і серед рейдерів."
            ],
            "tasks": ["item:friendsandfoes:totem_of_freezing"], "rewards": ["loot:wilds:reward/boss", "xp:120"], "skill": 80,
        },
        {
            "key": "wildfire", "title": "Вогняна корона", "icon": "friendsandfoes:wildfire_crown_fragment", "deps": ["totems"],
            "desc": [
                "&cДика пожежа&r — королева іфритів Незеру, рідкісна й дуже сильна. З її &aуламків&r складається &aвогняна корона&r — шолом королеви вогню."
            ],
            "tasks": ["item:friendsandfoes:wildfire_crown_fragment"], "rewards": ["loot:wilds:reward/boss", "xp:150"], "skill": 100,
        },
    ],
}
