CHAPTER = {
    "key": "create_addons",
    "title": "&6Create: додатки",
    "subtitle": "Електрика для інших модів і зачарування машинами",
    "group": "Техніка",
    "icon": "createaddition:electric_motor",
    "quests": [
        {
            "key": "wire", "title": "Прокатний стан", "icon": "createaddition:rolling_mill", "deps": ["create:press"],
            "desc": [
                "Crafts & Additions з'єднує Create з &eелектрикою&r (FE), якою живляться Mekanism, AE2 та інші машини.",
                "&aПрокатний стан&r робить з пластин дріт і стрижні."
            ],
            "tasks": ["item:createaddition:rolling_mill", "item:createaddition:copper_wire*8"], "rewards": ["xp:40"],
        },
        {
            "key": "alternator", "title": "Генератор", "icon": "createaddition:alternator", "deps": ["wire"],
            "desc": [
                "&aАльтернатор&r перетворює обертання Create на енергію FE. Так ваше водяне колесо чи паровий двигун можуть живити машини &eMekanism&r і мережу &eAE2&r.",
                "Енергію передають дроти між &aконекторами&r або кабелі Mekanism."
            ],
            "tasks": ["item:createaddition:alternator", "item:createaddition:connector*4"], "rewards": ["xp:60"], "skill": 50,
        },
        {
            "key": "motor", "title": "Електродвигун", "icon": "createaddition:electric_motor", "deps": ["wire"],
            "desc": [
                "&aЕлектродвигун&r робить навпаки: з енергії FE (наприклад, від сонячних панелей Mekanism) дає обертання для механізмів Create."
            ],
            "tasks": ["item:createaddition:electric_motor"], "rewards": ["xp:60"], "skill": 50,
        },
        {
            "key": "xp", "title": "Рідкий досвід", "icon": "create_enchantment_industry:experience_bucket", "deps": ["create:mixer"],
            "desc": [
                "Create: Enchantment Industry зберігає досвід як &eрідину&r. &aМеханічне точило&r знімає зачарування з речей і дає рідкий досвід; він збирається в баки."
            ],
            "tasks": ["item:create_enchantment_industry:mechanical_grindstone"], "rewards": ["xp:60"],
        },
        {
            "key": "enchanter", "title": "Пальник-зачарувальник", "icon": "create_enchantment_industry:blaze_enchanter", "deps": ["xp"],
            "desc": [
                "&aПальник-зачарувальник&r зачаровує предмети на конвеєрі, витрачаючи рідкий досвід — без столу зачарувань і без втрати рівнів гравця.",
                "Шаблон зачарування задає, що саме накласти."
            ],
            "tasks": ["item:create_enchantment_industry:blaze_enchanter"], "rewards": ["xp:150"], "skill": 80,
        },
    ],
}
