"""Quest chapter «Create: додатки» (11 quests): електрика, зачарування машинами, кухня й деталі Create, заводський стиль. Read by gen_quests.py."""
CHAPTER = {
    "key": "create_addons",
    "title": "&6Create: додатки",
    "subtitle": "Електрика, зачарування машинами, кухня, деталі й заводський стиль",
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
        # --- added 20.11: Slice & Dice, Create: Connected, Create: Central Kitchen, Create Deco
        {
            "key": "slicer", "title": "Різак", "icon": "sliceanddice:slicer", "deps": ["create:press", "kitchen:sharp"],
            "desc": [
                "Slice & Dice автоматизує &eдошку для нарізання&r Farmer's Delight. Дайте &aрізаку&r інструмент (ПКМ ножем чи сокирою) — і він ріже все, що лежить під ним на депо, конвеєрі чи в чаші.",
                "З Create: Central Kitchen &aмеханічні руки&r ще й беруть порції святкових страв і пирогів самі."
            ],
            "tasks": ["item:sliceanddice:slicer"], "rewards": ["xp:60"], "skill": 40,
        },
        {
            "key": "sprinkler", "title": "Дощувач", "icon": "sliceanddice:sprinkler", "deps": ["slicer"],
            "desc": [
                "&aДощувач&r ставлять під трубу Create: він розбризкує рідину на ділянку внизу. Вода зволожує грядки, а різні рідини по-різному діють і на істот поруч.",
                "Рідке добриво з того ж мода підживлює посіви."
            ],
            "tasks": ["item:sliceanddice:sprinkler"], "rewards": ["xp:40"],
        },
        {
            "key": "gearbox", "title": "Латунний редуктор", "icon": "create_connected:brass_gearbox", "deps": ["create:mixer"],
            "desc": [
                "Create: Connected додає дрібні, але корисні кінетичні деталі. У &aлатунного редуктора&r напрям обертання кожної з чотирьох сторін окремо перемикається ключем.",
                "&aВідцентрова муфта&r з'єднує вали, лише коли обертання досить швидке."
            ],
            "tasks": ["item:create_connected:brass_gearbox"], "rewards": ["xp:40"],
        },
        {
            "key": "battery", "title": "Кінетична батарея", "icon": "create_connected:kinetic_battery", "deps": ["gearbox"],
            "desc": [
                "&aКінетична батарея&r запасає навантажувальну здатність: без сигналу редстоуну вона заряджається від обертання спереду, з сигналом — сама крутить механізми.",
                "Так вітряк чи водяне колесо працюють і тоді, коли вітру чи течії бракує. Рівень заряду показує компаратор."
            ],
            "tasks": ["item:create_connected:kinetic_battery"], "rewards": ["xp:80"], "skill": 50,
        },
        {
            "key": "silo", "title": "Силос", "icon": "create_connected:item_silo", "deps": ["gearbox"],
            "desc": [
                "&aСилос&r — це вертикальне сховище Create: росте вгору, як вежа, і займає мало місця на землі."
            ],
            "tasks": ["item:create_connected:item_silo"], "rewards": ["xp:40"],
        },
        {
            "key": "deco", "title": "Заводський стиль", "icon": "createdeco:industrial_iron_ingot", "deps": ["create:press"],
            "desc": [
                "Create Deco дає будівлям заводу власний вигляд: місточки з поручнями, ґрати, лампи в клітках, вантажні контейнери й вісім видів цегли.",
                "Частину з цього роблять із &aпромислового заліза&r."
            ],
            "tasks": ["item:createdeco:industrial_iron_ingot*4", "item:createdeco:andesite_catwalk*4"], "rewards": ["xp:40"],
        },
    ],
}
