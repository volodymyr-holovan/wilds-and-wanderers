"""Ukrainian for Hybrid Aquatic (03.11): the mod ships a uk_ua with the English text copied in, so most of its fish,
corals, stones and diving gear showed in English. Vanilla terms follow Minecraft's own uk_ua (corals: «Блок … корала»,
«Віяло з … коралів»; «Яйце виклику …»; «Осідальне зілля …»). Painting authors stay as they are (names).

Terms: Brinestone - розсольний камінь, Shorestone - прибережний камінь, Chimneystone - димарний камінь, Schist -
кристалічний сланець, Raft - пліт, Hatxolotl - аксолотль-капелюх, Sirenian - сирена (дюгонь, ламантин), Shell Beast -
Мушляний звір, Karkinos/Karcinogen/Karcinoma - Каркінос/Карциноген/Карцинома.
"""
import os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import i18n  # noqa: E402

# coral kinds: (adjective nominative masc., genitive sg., genitive pl.)
CORALS = {
    "bamboo": ("бамбуковий", "бамбукового", "бамбукових"), "brain": ("мозковий", "мозкового", "мозкових"),
    "bubble": ("бульбашковий", "бульбашкового", "бульбашкових"), "button": ("ґудзиковий", "ґудзикового", "ґудзикових"),
    "fire": ("вогняний", "вогняного", "вогняних"), "horn": ("роговий", "рогового", "рогових"),
    "leaf": ("листовий", "листового", "листових"), "lophelia": ("лофелієвий", "лофелієвого", "лофелієвих"),
    "rose": ("трояндовий", "трояндового", "трояндових"), "sun": ("сонячний", "сонячного", "сонячних"),
    "thorn": ("терновий", "тернового", "тернових"), "tube": ("трубчастий", "трубчастого", "трубчастих"),
    "zigzag": ("зигзагоподібний", "зигзагоподібного", "зигзагоподібних"),
}
STATES = {"": ("", "", ""), "bleached_": ("вибілений ", "вибіленого ", "вибілених "), "dead_": ("мертвий ", "мертвого ", "мертвих ")}


def corals():
    out = {}
    for c, (nom, gen, pl) in CORALS.items():
        for pre, (snom, sgen, spl) in STATES.items():
            base = f"block.hybrid_aquatic.{pre}{c}_coral"
            out[base] = (snom + nom + " корал").capitalize()
            out[base + "_block"] = f"Блок {sgen}{gen} корала"
            out[base + "_fan"] = f"Віяло з {spl}{pl} коралів"
    return out


# (name, genitive for "Яйце виклику …")
MOBS = {
    "argonaut": ("Аргонавт", "аргонавта"), "beakling": ("Дзьобанчик", "дзьобанчика"), "blobfish": ("Риба-крапля", "риби-краплі"),
    "cavitation_bubble": ("Кавітаційна бульбашка", "кавітаційної бульбашки"), "cichlid": ("Цихліда", "цихліди"),
    "colossal_squid": ("Колосальний кальмар", "колосального кальмара"), "comb_jelly": ("Реброплав", "реброплава"),
    "corydora": ("Коридорас", "коридораса"), "depth_charge": ("Глибинна бомба", "глибинної бомби"), "dugong": ("Дюгонь", "дюгоня"),
    "fangtooth": ("Шаблезуб", "шаблезуба"), "garden_eel": ("Садовий вугор", "садового вугра"),
    "giant_squid": ("Гігантський кальмар", "гігантського кальмара"), "goblin_shark": ("Акула-домовик", "акули-домовика"),
    "hagfish": ("Міксина", "міксини"), "hatchetfish": ("Риба-сокира", "риби-сокири"), "manatee": ("Ламантин", "ламантина"),
    "ocean_sunfish": ("Риба-місяць", "риби-місяця"), "orca": ("Косатка", "косатки"), "pleco": ("Плекостомус", "плекостомуса"),
    "scalyfoot_snail": ("Лускатоногий равлик", "лускатоногого равлика"), "shell_beast": ("Мушляний звір", "Мушляного звіра"),
    "shiner": ("Блискітка", "блискітки"), "sixgill_shark": ("Шестизяброва акула", "шестизябрової акули"),
    "sleeper_shark": ("Полярна акула", "полярної акули"), "slickhead": ("Гладкоголов", "гладкоголова"),
    "small_tnt": ("Малий динаміт", "малого динаміту"), "starfish_projectile": ("Кинута морська зірка", "кинутої морської зірки"),
    "trevally": ("Каранкс", "каранкса"), "tripod_fish": ("Риба-тринога", "риби-триноги"), "trout": ("Форель", "форелі"),
    "viperfish": ("Хауліод", "хауліода"),
}

T = {
    "advancements.hybrid_aquatic.argonaut.description": "Збудуйте «Аргонавта»",
    "advancements.hybrid_aquatic.argonaut.title": "У цього човна є плавці",
    "advancements.hybrid_aquatic.conduit.description": "Створіть провідник",
    "advancements.hybrid_aquatic.conduit.title": "Краще за зябра",
    "advancements.hybrid_aquatic.coral_chunk.description": "Обстрижіть краба-декоратора, щоб отримати шматок корала",
    "advancements.hybrid_aquatic.coral_chunk.title": "Це не вівця",
    "advancements.hybrid_aquatic.coral_tools.description": "Зробіть інструмент зі шматків корала",
    "advancements.hybrid_aquatic.coral_tools.title": "Кому потрібне Лагодження?",
    "advancements.hybrid_aquatic.depth_charge.description": "Створіть глибинну бомбу",
    "advancements.hybrid_aquatic.depth_charge.title": "Втома від копання?",
    "advancements.hybrid_aquatic.diving_upgrade.description": "Знайдіть ковальський шаблон водолазного костюма",
    "advancements.hybrid_aquatic.diving_upgrade.title": "Скарби з узбережжя",
    "advancements.hybrid_aquatic.diving_weight.description": "Створіть водолазний вантаж",
    "advancements.hybrid_aquatic.diving_weight.title": "Найшвидше на дно",
    "advancements.hybrid_aquatic.enter_water.title": "Hybrid Aquatic",
    "advancements.hybrid_aquatic.get_clam.description": "Нагодуйте дюгоня морським салатом, і він дасть молюска",
    "advancements.hybrid_aquatic.get_clam.title": "Щасливий, як молюск",
    "advancements.hybrid_aquatic.glowing_diving_suit.description": "Зробіть свій водолазний костюм сяйним",
    "advancements.hybrid_aquatic.glowing_diving_suit.title": "Світло в безодні",
    "advancements.hybrid_aquatic.kill_sirenian.description": "Вони ж під загрозою зникнення",
    "advancements.hybrid_aquatic.kill_sirenian.title": "Мисливець на морських корів",
    "advancements.hybrid_aquatic.nautilus_shell.description": "Здобудьте мушлю навтилуса",
    "advancements.hybrid_aquatic.nautilus_shell.title": "Тут хтось колись жив",
    "advancements.hybrid_aquatic.ominous_conch.description": "Виміняйте в рака-самітника мушлі на Зловісну мушлю",
    "advancements.hybrid_aquatic.ominous_conch.title": "Якщо прислухатися…",
    "advancements.hybrid_aquatic.plant_clam.description": "Виростіть власних молюсків під водою",
    "advancements.hybrid_aquatic.plant_clam.title": "Мушляна корпорація",
    "advancements.hybrid_aquatic.reinforced_diving_suit.description": "Посильте своє водолазне спорядження",
    "advancements.hybrid_aquatic.reinforced_diving_suit.title": "Ще глибше",
    "advancements.hybrid_aquatic.seashell_tools.description": "Зробіть інструмент із мушель навтилуса",
    "advancements.hybrid_aquatic.seashell_tools.title": "Мушля-перемушля!",
    "advancements.hybrid_aquatic.shell_beast.description": "Переможіть Мушляного звіра",
    "advancements.hybrid_aquatic.shell_beast.title": "Мушляний шок",
    "advancements.hybrid_aquatic.sulfur.description": "Знайдіть сірку в сірчаній печері",
    "advancements.hybrid_aquatic.sulfur.title": "Це не світлопил",
    "advancements.hybrid_aquatic.trident.description": "Здобудьте тризубець",
    "advancements.hybrid_aquatic.trident.title": "Сам Посейдон тремтить!",
    "advancements.hybrid_aquatic.turtle_scute.description": "Здобудьте щиток черепахи",
    "advancements.hybrid_aquatic.turtle_scute.title": "Як швидко вони ростуть!",
    "advancements.hybrid_aquatic.turtle_set.description": "Створіть частину черепахової броні",
    "advancements.hybrid_aquatic.turtle_set.title": "Кавабанга!",
    "block.hybrid_aquatic.aerated_sand": "Пористий пісок",
    "block.hybrid_aquatic.barnacle_shorestone": "Прибережний камінь із вусоногими",
    "block.hybrid_aquatic.bell_buoy": "Буй із дзвоном",
    "block.hybrid_aquatic.bone_fence": "Кістяний паркан",
    "block.hybrid_aquatic.bone_slab": "Кістяна плита",
    "block.hybrid_aquatic.bone_stairs": "Кістяні сходи",
    "block.hybrid_aquatic.bone_wall": "Кістяна стіна",
    "block.hybrid_aquatic.bone_worms": "Кісткові черв'яки",
    "block.hybrid_aquatic.bubble_geyser": "Бульбашковий гейзер",
    "block.hybrid_aquatic.clams": "Молюск",
    "block.hybrid_aquatic.coralstone": "Кораловий камінь",
    "block.hybrid_aquatic.crystalline_sulfur": "Кристалічна сірка",
    "block.hybrid_aquatic.delesseria": "Делесерія",
    "block.hybrid_aquatic.delesseria_plant": "Стебло делесерії",
    "block.hybrid_aquatic.depth_charge": "Глибинна бомба",
    "block.hybrid_aquatic.giant_thermal_vent": "Велике термальне джерело",
    "block.hybrid_aquatic.glass_sponge": "Скляна губка",
    "block.hybrid_aquatic.grassy_sand": "Порослий травою пісок",
    "block.hybrid_aquatic.hagslime_block": "Блок слизу міксини",
    "block.hybrid_aquatic.harp_sponge": "Губка-арфа",
    "block.hybrid_aquatic.marine_snow": "Морський сніг",
    "block.hybrid_aquatic.message_in_a_bottle.change_hint": "Клацніть ПКМ по ній у світі, щоб змінити вигляд",
    "block.hybrid_aquatic.message_in_a_bottle.variant.bottle": "Пляшка",
    "block.hybrid_aquatic.message_in_a_bottle.variant.jar": "Банка",
    "block.hybrid_aquatic.message_in_a_bottle.variant.longneck": "Довгошия пляшка",
    "block.hybrid_aquatic.message_in_a_bottle.variant.potion": "Пляшечка з-під зілля",
    "block.hybrid_aquatic.message_in_a_bottle.variant.wine": "Винна пляшка",
    "block.hybrid_aquatic.mussels": "Мідія",
    "block.hybrid_aquatic.oyster_block": "Устриця",
    "block.hybrid_aquatic.ping_pong_sponge": "Губка «пінг-понг»",
    "block.hybrid_aquatic.short_red_algae": "Низькі червоні водорості",
    "block.hybrid_aquatic.suspicious_red_sand": "Підозрілий червоний пісок",
    "block.hybrid_aquatic.water_hyacinth": "Водяний гіацинт",
    "block.hybrid_aquatic.white_sand": "Білий пісок",
    "block.hybrid_aquatic.wild_mussels": "Дикі мідії",
    "item.hybrid_aquatic.argonaut": "«Аргонавт»",
    "item.hybrid_aquatic.blobfish": "Риба-крапля",
    "item.hybrid_aquatic.blue_hatxolotl": "Синій аксолотль-капелюх",
    "item.hybrid_aquatic.brine_bucket": "Відро з розсолом",
    "item.hybrid_aquatic.brown_hatxolotl": "Коричневий аксолотль-капелюх",
    "item.hybrid_aquatic.comically_large_nautilus_shell": "Смішно велика мушля навтилуса",
    "item.hybrid_aquatic.cooked_clam": "Смажений молюск",
    "item.hybrid_aquatic.cooked_crab": "Смажений краб",
    "item.hybrid_aquatic.cooked_mussel": "Смажена мідія",
    "item.hybrid_aquatic.cyan_hatxolotl": "Бірюзовий аксолотль-капелюх",
    "item.hybrid_aquatic.diving_armor_upgrade_template": "Ковальський шаблон",
    "item.hybrid_aquatic.diving_weight": "Водолазний вантаж",
    "item.hybrid_aquatic.diving_weight.description": "Досить важкий",
    "item.hybrid_aquatic.fish_food": "Корм для риб",
    "item.hybrid_aquatic.glowing_diving_boots": "Сяйні водолазні чоботи",
    "item.hybrid_aquatic.glowing_diving_helmet": "Сяйний водолазний шолом",
    "item.hybrid_aquatic.glowing_diving_leggings": "Сяйні водолазні наголінники",
    "item.hybrid_aquatic.glowing_diving_suit": "Сяйний водолазний костюм",
    "item.hybrid_aquatic.gold_hatxolotl": "Золотий аксолотль-капелюх",
    "item.hybrid_aquatic.hagfish": "Міксина",
    "item.hybrid_aquatic.hagslime": "Слиз міксини",
    "item.hybrid_aquatic.hook.description_tide": "Встановлюється на рибальському столі",
    "item.hybrid_aquatic.ocean_sunfish": "Риба-місяць",
    "item.hybrid_aquatic.ominous_conch": "Зловісна мушля",
    "item.hybrid_aquatic.ominous_conch.function": "Кличе Мушляного звіра",
    "item.hybrid_aquatic.pink_hatxolotl": "Рожевий аксолотль-капелюх",
    "item.hybrid_aquatic.pleco": "Плекостомус",
    "item.hybrid_aquatic.reinforced_diving_boots": "Посилені водолазні чоботи",
    "item.hybrid_aquatic.reinforced_diving_helmet": "Посилений водолазний шолом",
    "item.hybrid_aquatic.reinforced_diving_leggings": "Посилені водолазні наголінники",
    "item.hybrid_aquatic.reinforced_diving_suit": "Посилений водолазний костюм",
    "item.hybrid_aquatic.sirenian_beef": "Сире м'ясо сирени",
    "item.hybrid_aquatic.sirenian_steak": "Стейк із сирени",
    "item.hybrid_aquatic.starfish": "Морська зірка",
    "item.hybrid_aquatic.striped_eel_scarf": "Шарф зі смугастого вугра",
    "item.hybrid_aquatic.trevally": "Каранкс",
    "item.hybrid_aquatic.trout": "Форель",
    "item.minecraft.lingering_potion.effect.corrosion": "Осідальне зілля корозії",
    "item.minecraft.lingering_potion.effect.glowing": "Осідальне зілля світіння",
    "item.minecraft.lingering_potion.effect.major_luck": "Осідальне зілля великого везіння",
    "item.minecraft.lingering_potion.effect.minor_luck": "Осідальне зілля малого везіння",
    "item.minecraft.lingering_potion.effect.thalassophobia": "Осідальне зілля таласофобії",
    "item.minecraft.lingering_potion.effect.thorns": "Осідальне зілля шипів",
    "journal.description.hybrid_aquatic.blobfish": "Глибоководна риба з м'яким драглистим тілом, пристосована до величезного тиску.",
    "journal.description.hybrid_aquatic.cichlid": "Прісноводна риба, улюблениця акваріумістів за кмітливість і яскраві візерунки.",
    "journal.description.hybrid_aquatic.clownfish": "Яскрава рифова риба, що часто живе серед актиній; прославилася завдяки дитячому мультфільму.",
    "journal.description.hybrid_aquatic.coelacanth": "Глибоководна риба, яку довго вважали вимерлою, — тепер її звуть живою копалиною.",
    "journal.description.hybrid_aquatic.damselfish": "Дуже поширена рифова риба найрізноманітніших форм і кольорів, часто пливе великими зграями.",
    "journal.description.hybrid_aquatic.dragonfish": "Глибоководна риба з довгою сяйною приманкою на щелепі, якою вона вабить здобич.",
    "journal.description.hybrid_aquatic.flashlight_fish": "Нічна глибоководна риба зі сяйними органами під очима: ними вона перемовляється й вабить здобич.",
    "journal.description.hybrid_aquatic.flying_fish": "Риба відкритого океану, що на великих плавцях ширяє над водою.",
    "journal.description.hybrid_aquatic.hagfish": "Давній змієподібний падальник; коли їй загрожують, виділяє багато слизу.",
    "journal.description.hybrid_aquatic.herring": "Невелика рибка-наживка, що збирається у величезні зграї.",
    "journal.description.hybrid_aquatic.john_dory": "Хижа морська риба з великою темною плямою на боці й щелепами, що далеко висуваються.",
    "journal.description.hybrid_aquatic.lionfish": "Хижа рифова риба з отруйними колючками; у деяких морях — небажаний прибулець.",
    "journal.description.hybrid_aquatic.mahi": "Хижа риба відкритих вод, відома яскравим забарвленням.",
    "journal.description.hybrid_aquatic.needlefish": "Тонка хижа риба з довгим дзьобом, повним гострих зубів; тримається біля поверхні.",
    "journal.description.hybrid_aquatic.oarfish": "Довга глибоководна риба, що нібито з'являється перед землетрусами; від неї пішли легенди про морських змій.",
    "journal.description.hybrid_aquatic.ocean_sunfish": "Найбільша кісткова риба у світі, відома тим, що не зважає на біль.",
    "journal.description.hybrid_aquatic.opah": "Велика риба відкритого океану — одна з небагатьох теплокровних риб.",
    "journal.description.hybrid_aquatic.parrotfish": "Важлива рифова риба з дзьобом, як у папуги: гризе корали й перетворює їх на пісок.",
    "journal.description.hybrid_aquatic.pleco": "Прісноводний сом у панцирі: їсть водорості й чіпляється до поверхонь ротом-присоском.",
    "journal.description.hybrid_aquatic.ratfish": "Глибоководна родичка акул із великими очима, довгим хвостом і незвичним виглядом.",
    "journal.description.hybrid_aquatic.seahorse": "Невелика рифова рибка, що плаває стоячи; має закручений хвіст, а ікру виношує самець.",
    "journal.description.hybrid_aquatic.sheepshead_wrasse": "Велика риба ламінарієвих лісів: їсть морських їжаків і цим береже ламінарію.",
    "journal.description.hybrid_aquatic.stingray": "Велика кругла риба, що пливе понад дном; на кінці хвоста має отруйний шип.",
    "journal.description.hybrid_aquatic.tetra": "Невелика прісноводна рибка, відома яскравим кольором і життям у зграї.",
    "journal.description.hybrid_aquatic.trevally": "Зграйна риба, що любить супроводжувати великих тварин.",
    "journal.description.hybrid_aquatic.trout": "Прісноводна риба річок і озер, цінована за швидкість і вміння плисти проти течії.",
    "painting.hybrid_aquatic.asexual_pride_flag.title": "Асексуальна гордість",
    "painting.hybrid_aquatic.below.title": "У глибині",
    "painting.hybrid_aquatic.big_lure.title": "Велика приманка",
    "painting.hybrid_aquatic.bigeye.title": "Великоокий",
    "painting.hybrid_aquatic.bisexual_pride_flag.title": "Бісексуальна гордість",
    "painting.hybrid_aquatic.bold_and_brash.title": "Сміливо й зухвало",
    "painting.hybrid_aquatic.conch_street.title": "Вулиця Мушлі",
    "painting.hybrid_aquatic.creatures_of_the_deep.title": "Мешканці глибин",
    "painting.hybrid_aquatic.gay_pride_flag.title": "Гей-гордість",
    "painting.hybrid_aquatic.great_lake.title": "Велике озеро",
    "painting.hybrid_aquatic.jam.title": "Варення",
    "painting.hybrid_aquatic.jaws.title": "Щелепи",
    "painting.hybrid_aquatic.jolly_roger.title": "Веселий Роджер",
    "painting.hybrid_aquatic.king_of_herring.title": "Король оселедців",
    "painting.hybrid_aquatic.lesbian_pride_flag.title": "Лесбійська гордість",
    "painting.hybrid_aquatic.lighthouse.title": "Маяк",
    "painting.hybrid_aquatic.melon.title": "Кавун",
    "painting.hybrid_aquatic.mooncatcher.title": "Ловець місяця",
    "painting.hybrid_aquatic.nonbinary_pride_flag.title": "Небінарна гордість",
    "painting.hybrid_aquatic.palestine_flag.title": "Палестина",
    "painting.hybrid_aquatic.preserver.title": "Життя",
    "painting.hybrid_aquatic.pressed_bull_kelp.title": "Засушена бичача ламінарія",
    "painting.hybrid_aquatic.pressed_kelp.title": "Засушена ламінарія",
    "painting.hybrid_aquatic.pride_flag.title": "Гордість",
    "painting.hybrid_aquatic.riverfish.title": "Річкова риба",
    "painting.hybrid_aquatic.sunset.title": "Захід сонця",
    "painting.hybrid_aquatic.suntide.title": "Сонячний приплив",
    "painting.hybrid_aquatic.swirling.title": "Вир",
    "painting.hybrid_aquatic.the_watchful.title": "Пильний",
    "painting.hybrid_aquatic.trans_pride_flag.title": "Транс-гордість",
    "subtitles.hybrid_aquatic.entity.dugong.ambient": "Дюгонь щебече",
    "subtitles.hybrid_aquatic.entity.dugong.die": "Дюгонь гине",
    "subtitles.hybrid_aquatic.entity.dugong.hurt": "Дюгонь поранений",
    "subtitles.hybrid_aquatic.entity.dugong.splash": "Дюгонь хлюпається",
    "subtitles.hybrid_aquatic.entity.dugong.swim": "Дюгонь пливе",
    "subtitles.hybrid_aquatic.entity.hypnautilus.ambient": "Гіпнавтилус кружляє",
    "subtitles.hybrid_aquatic.entity.hypnautilus.die": "Гіпнавтилус гине",
    "subtitles.hybrid_aquatic.entity.hypnautilus.hurt": "Гіпнавтилус поранений",
    "subtitles.hybrid_aquatic.entity.karcinogen.ambient": "Карциноген клацає",
    "subtitles.hybrid_aquatic.entity.karcinogen.die": "Карциноген гине",
    "subtitles.hybrid_aquatic.entity.karcinogen.hurt": "Карциноген поранений",
    "subtitles.hybrid_aquatic.entity.karcinoma.ambient": "Карцинома клацає",
    "subtitles.hybrid_aquatic.entity.karcinoma.die": "Карцинома гине",
    "subtitles.hybrid_aquatic.entity.karcinoma.hurt": "Карцинома поранена",
    "subtitles.hybrid_aquatic.entity.karkinos.ambient": "Каркінос клацає",
    "subtitles.hybrid_aquatic.entity.karkinos.die": "Каркінос гине",
    "subtitles.hybrid_aquatic.entity.karkinos.hurt": "Каркінос поранений",
    "subtitles.hybrid_aquatic.entity.manatee.ambient": "Ламантин пирхає",
    "subtitles.hybrid_aquatic.entity.manatee.die": "Ламантин гине",
    "subtitles.hybrid_aquatic.entity.manatee.hurt": "Ламантин поранений",
    "subtitles.hybrid_aquatic.entity.manatee.splash": "Ламантин хлюпається",
    "subtitles.hybrid_aquatic.entity.manatee.swim": "Ламантин пливе",
    "subtitles.hybrid_aquatic.entity.shell_beast.ambient": "Мушляний звір клацає",
    "subtitles.hybrid_aquatic.entity.shell_beast.die": "Мушляний звір гине",
    "subtitles.hybrid_aquatic.entity.shell_beast.hurt": "Мушляний звір поранений",
    "subtitles.hybrid_aquatic.entity.shell_beast.roar": "Мушляний звір реве",
    "subtitles.hybrid_aquatic.entity.shell_beast.shoot": "Мушляний звір стріляє",
    "subtitles.hybrid_aquatic.entity.sirenian.eat": "Сирена їсть",
    "subtitles.hybrid_aquatic.item.ominous_conch.blows": "Зловісна мушля звучить",
    "tooltip.hybrid_aquatic.argonaut.glowing": "Сяйний",
    "tooltip.hybrid_aquatic.argonaut.sail": "Вітрила: %s",
    "tooltip.hybrid_aquatic.argonaut.shell": "Мушля: %s",
    "tooltip.hybrid_aquatic.ominous_conch.unused": "З глибини мушлі кличе океан…",
    "tooltip.hybrid_aquatic.ominous_conch.used": "Мушля мовчить",
}

STONES = {"orange_brinestone": ("помаранчевий розсольний камінь", "помаранчевого розсольного каменю"),
          "red_brinestone": ("червоний розсольний камінь", "червоного розсольного каменю"),
          "yellow_brinestone": ("жовтий розсольний камінь", "жовтого розсольного каменю"),
          "chimneystone": ("димарний камінь", "димарного каменю"), "schist": ("кристалічний сланець", "кристалічного сланцю"),
          "shorestone": ("прибережний камінь", "прибережного каменю"), "white_sandstone": ("білий пісковик", "білого пісковику")}
WOODS = {"oak": "Дубовий", "spruce": "Ялиновий", "birch": "Березовий", "jungle": "Тропічний", "acacia": "Акацієвий",
         "dark_oak": "Темнодубовий", "mangrove": "Мангровий", "cherry": "Вишневий", "driftwood": "Пліт із плавника"}


def generated():
    out = corals()
    for m, (name, gen) in MOBS.items():
        out[f"entity.hybrid_aquatic.{m}"] = name
        out[f"item.hybrid_aquatic.{m}_spawn_egg"] = f"Яйце виклику {gen}"
    for s, (nom, gen) in STONES.items():
        b = f"block.hybrid_aquatic.{s}"
        out[b] = nom.capitalize()
        out[b + "_slab"] = f"Плита з {gen}"
        out[b + "_stairs"] = f"Сходи з {gen}"
        out[b + "_wall"] = f"Стіна з {gen}"
        out[b + "_bricks"] = f"Цегла з {gen}"
        out[b + "_brick_slab"] = f"Плита з цегли з {gen}"
        out[b + "_brick_stairs"] = f"Сходи з цегли з {gen}"
        out[b + "_brick_wall"] = f"Стіна з цегли з {gen}"
        out[f"block.hybrid_aquatic.polished_{s}"] = f"Полірований {nom}"
        out[f"block.hybrid_aquatic.polished_{s}_slab"] = f"Плита з полірованого {gen}"
        out[f"block.hybrid_aquatic.polished_{s}_stairs"] = f"Сходи з полірованого {gen}"
        out[f"block.hybrid_aquatic.chiseled_{s}"] = f"Різьблений {nom}"
        out[f"block.hybrid_aquatic.cut_{s}"] = f"Тесаний {nom}"
        out[f"block.hybrid_aquatic.cut_{s}_slab"] = f"Плита з тесаного {gen}"
        out[f"block.hybrid_aquatic.smooth_{s}"] = f"Гладкий {nom}"
        out[f"block.hybrid_aquatic.smooth_{s}_slab"] = f"Плита з гладкого {gen}"
        out[f"block.hybrid_aquatic.smooth_{s}_stairs"] = f"Сходи з гладкого {gen}"
    for w, adj in WOODS.items():
        out[f"block.hybrid_aquatic.{w}_raft"] = adj if w == "driftwood" else f"{adj} пліт"
    for shark in ("basking", "bull", "frilled", "great_white", "hammerhead", "thresher", "tiger", "whale"):
        out[f"block.hybrid_aquatic.{shark}_shark_plushie.description"] = "Можна носити як капелюх!"
    for tool in ("axe", "blade", "hoe", "pickaxe", "shovel"):
        out[f"item.hybrid_aquatic.coral_{tool}.description"] = "Лагодиться сам під водою"
        out[f"item.hybrid_aquatic.seashell_{tool}.description"] = "Швидше копає під водою"
    return out


if __name__ == "__main__":
    allt = generated()
    allt.update(T)
    todo = i18n.load_lang(open(sys.argv[1], "rb").read()) if len(sys.argv) > 1 else None
    if todo is not None:  # report what the todo list still has untranslated (names of painting authors stay)
        left = [k for k in todo.get("hybrid_aquatic", {}) if k not in allt and not k.endswith(".author")]
        print("left:", len(left), left[:40])
    unknown = i18n.store("hybrid_aquatic", allt)
    print("stored", len(allt) - len(unknown), "unknown keys:", len(unknown), unknown[:10])
