"""Picture and pronunciation sources for the curriculum.

IMAGES: concept -> (English Wikipedia article whose lead photo depicts it, Commons search query).
Either may be None. fetch_assets.py collects up to 3 candidates per concept; a human picks one
(tools/assets/choices.json) after looking at the contact sheets.

Concepts not listed here (numbers, colors, days of the week, today/tomorrow) get generated
pictures from make_generated.py, because a photo cannot show them unambiguously.
"""

IMAGES = {
    # --- first words
    "merhaba": ("Wave (gesture)", "person waving hand hello"),
    "evet": (None, "thumbs up hand gesture"),
    "hayir": (None, "hand stop gesture no"),
    "su": (None, "glass of water"),
    "ekmek": ("Bread", "loaf of bread"),
    "elma": ("Apple", "red apple fruit"),
    "ev": ("House", "detached house"),
    "kitap": ("Book", "open book"),
    "anne": (None, "mother holding child"),
    "baba": (None, "father with son"),
    "cay": ("Turkish tea", "Turkish tea glass"),
    "kahve": ("Turkish coffee", "Turkish coffee cup"),
    "sut": ("Milk", "glass of milk"),
    "araba": ("Car", "car automobile side view"),
    "kedi": ("Cat", "domestic cat"),
    "kopek": ("Dog", "dog portrait"),
    "okul": ("School", "school building"),
    "kapi": ("Door", "wooden door"),
    "sandalye": ("Chair", "wooden chair"),
    "telefon": ("Smartphone", "smartphone"),
    "para": ("Turkish lira", "Turkish lira banknotes"),
    "gunes": (None, "sun in blue sky"),
    "ay": ("Moon", "full moon"),
    "cocuk": ("Child", "child playing"),
    "arkadas": ("Friendship", "friends together"),
    "yemek": ("Meal", "plate of food dinner"),
    "goz": ("Eye", "human eye close up"),
    "el": ("Hand", "human hand palm"),
    "canta": ("Handbag", "handbag"),
    "iyi": (None, "thumbs up smiling"),
    # --- expressions
    "gunaydin": ("Sunrise", "sunrise morning"),
    "iyi_aksamlar": ("Dusk", "evening city lights"),
    "hos_geldiniz": (None, "welcome doormat"),
    "hosca_kal": (None, "waving goodbye"),
    "gorusuruz": ("Handshake", "handshake"),
    "tesekkur": (None, "hand on heart thank you"),
    "lutfen": (None, "please gesture hands together"),
    "affedersiniz": (None, "raising hand question"),
    "ozur_dilerim": ("Apology", "apology gesture"),
    "nasilsiniz": ("Conversation", "two people talking"),
    "iyiyim": ("Smile", "smiling man"),
    "benim_adim": ("Name tag", "name tag sticker hello my name is"),
    "ben_den": ("Passport", "passport"),
    "anlamadim": ("Confusion", "confused person"),
    "tekrar": (None, "hand behind ear listening"),
    "yavas": ("Snail", "snail"),
    # --- family
    "aile": ("Family", "family portrait"),
    "kardes": ("Sibling", "brother and sister children"),
    "dede": ("Grandparent", "grandfather portrait"),
    "buyukanne": (None, "grandmother portrait"),
    "ogul": (None, "father and son"),
    "kiz": (None, "mother and daughter"),
    "es": ("Wedding", "married couple"),
    "bebek": ("Infant", "baby"),
    # --- food
    "peynir": ("Cheese", "white cheese"),
    "yumurta": ("Egg as food", "chicken eggs"),
    "pilav": ("Pilaf", "rice pilaf"),
    "corba": ("Lentil soup", "mercimek çorbası"),
    "et": ("Meat", "raw meat"),
    "tavuk": ("Roast chicken", "roast chicken"),
    "balik": ("Fish", "fresh fish"),
    "bal": ("Honey", "honey jar"),
    # --- fruits
    "muz": ("Banana", "bananas"),
    "portakal": ("Orange (fruit)", "orange fruit"),
    "uzum": ("Grape", "bunch of grapes"),
    "cilek": ("Strawberry", "strawberries"),
    "karpuz": ("Watermelon", "watermelon"),
    "armut": ("Pear", "pear fruit"),
    "limon": ("Lemon", "lemon fruit"),
    # --- vegetables
    "domates": ("Tomato", "tomatoes"),
    "salatalik": ("Cucumber", "cucumber"),
    "patates": ("Potato", "potatoes"),
    "sogan": ("Onion", "onions"),
    "havuc": ("Carrot", "carrots"),
    "biber": ("Bell pepper", "green pepper"),
    "patlican": ("Eggplant", "eggplant"),
    "sarimsak": ("Garlic", "garlic bulb"),
    # --- drinks
    "meyve_suyu": ("Juice", "glass of orange juice"),
    "gazli_icecek": ("Soft drink", "soda can"),
    "ayran": ("Ayran", "ayran"),
    "bitki_cayi": ("Herbal tea", "herbal tea cup"),
    "limonata": ("Lemonade", "lemonade glass"),
    # --- animals
    "kus": ("House sparrow", "small bird"),
    "at": ("Horse", "horse"),
    "inek": ("Cattle", "cow"),
    "tavsan": ("Rabbit", "rabbit"),
    "aslan": ("Lion", "lion"),
    "maymun": ("Monkey", "monkey"),
    "koyun": ("Sheep", "sheep"),
    "fil": ("Elephant", "elephant"),
    # --- body
    "agiz": ("Mouth", "human mouth smile"),
    "burun": ("Human nose", "human nose"),
    "kulak": ("Ear", "human ear"),
    "ayak": ("Foot", "human foot"),
    "dis": ("Tooth", "human teeth"),
    "sac": ("Hair", "long hair woman back"),
    "kol": ("Arm", "human arm"),
    # --- clothes
    "tisort": ("T-shirt", "t-shirt"),
    "pantolon": ("Trousers", "trousers"),
    "elbise": ("Dress", "dress"),
    "ayakkabi": ("Sneakers", "shoes"),
    "sapka": ("Hat", "sun hat"),
    "ceket": ("Jacket", "jacket"),
    "corap": ("Sock", "socks"),
    "eldiven": ("Glove", "winter gloves"),
    # --- home
    "mutfak": ("Kitchen", "kitchen interior"),
    "banyo": ("Bathroom", "bathroom interior"),
    "pencere": ("Window", "window"),
    "anahtar": ("Key (lock)", "house key"),
    "bahce": ("Garden", "garden flowers"),
    "merdiven": ("Stairs", "staircase"),
    "duvar": ("Brick", "brick wall"),
    # --- furniture
    "koltuk": ("Couch", "sofa"),
    "yatak": ("Bed", "bed bedroom"),
    "dolap": ("Wardrobe", "wardrobe"),
    "lamba": ("Lamp", "table lamp"),
    "ayna": ("Mirror", "mirror"),
    "televizyon": ("Television set", "television"),
    # --- transport
    "otobus": ("Bus", "city bus"),
    "tren": ("Train", "passenger train"),
    "ucak": ("Airplane", "airliner"),
    "bisiklet": ("Bicycle", "bicycle"),
    "taksi": ("Taxicab", "Istanbul taxi"),
    "gemi": ("Ship", "ship sea"),
    "metro": ("Istanbul Metro", "metro train station"),
    # --- places
    "hastane": ("Hospital", "hospital building"),
    "market": ("Supermarket", "supermarket aisle"),
    "eczane": ("Pharmacy (shop)", "pharmacy"),
    "cami": ("Mosque", "mosque Istanbul"),
    "banka": ("Bank", "bank building"),
    "otel": ("Hotel", "hotel building"),
    "havalimani": ("Airport", "airport terminal"),
    "restoran": ("Restaurant", "restaurant interior"),
    # --- time
    "saat": ("Clock", "wall clock"),
    "dakika": ("Stopwatch", "stopwatch"),
    "sabah": ("Morning", "morning sunrise"),
    "ogle": ("Noon", "midday sun"),
    "aksam": ("Sunset", "sunset"),
    "gece": ("Night", "night sky city"),
    # --- weather
    "gunesli": ("Sunshine", "sunny day"),
    "yagmur": ("Rain", "rain"),
    "kar": ("Snow", "snowfall"),
    "ruzgar": ("Wind", "windy trees"),
    "bulut": ("Cloud", "cumulus clouds"),
    "sicak": ("Heat wave", "hot sun thermometer"),
    "soguk": ("Cold", "person cold winter"),
    "semsiye": ("Umbrella", "umbrella"),
    # --- school
    "ogretmen": ("Teacher", "teacher classroom"),
    "ogrenci": ("Student", "student studying"),
    "kalem": ("Pencil", "pencil"),
    "defter": ("Notebook", "notebook"),
    "ders": ("Classroom", "classroom"),
    "makas": ("Scissors", "scissors"),
    "cetvel": ("Ruler", "ruler"),
    # --- work
    "is": ("Office", "people working office"),
    "doktor": ("Physician", "doctor stethoscope"),
    "asci": ("Chef", "chef cooking"),
    "polis": ("Police officer", "police officer"),
    "muhendis": ("Engineer", "engineer hard hat"),
    "ciftci": ("Farmer", "farmer field"),
    "toplanti": ("Meeting", "business meeting"),
    "ofis": ("Office", "office desk"),
    # --- verbs
    "uyumak": ("Sleep", "sleeping person"),
    "kosmak": ("Running", "running person"),
    "yazmak": ("Handwriting", "hand writing pen paper"),
    "okumak": ("Reading", "person reading book"),
    "konusmak": ("Conversation", "people talking"),
    "dinlemek": ("Headphones", "listening to music headphones"),
    "gitmek": ("Walking", "person walking street"),
    "icmek": ("Drinking", "drinking water glass"),
    # --- extra words used only as alphabet examples
    "ari": ("Western honey bee", "honey bee"),
    "dondurma": ("Ice cream", "ice cream cone"),
    "dag": ("Mountain", "mountain"),
    "irmak": ("River", "river"),
    "jole": ("Gelatin dessert", "jelly dessert"),
    "nane": ("Mentha", "mint leaves"),
    "ordek": ("Mallard", "duck"),
    "radyo": ("Radio receiver", "old radio"),
    "top": ("Football (ball)", "soccer ball"),
    "vapur": ("Ferries in Istanbul", "Istanbul ferry"),
    "zurafa": ("Giraffe", "giraffe"),
}

# Alphabet letter id -> concept used for its example picture
LETTER_EXAMPLES = {
    "l_a": "ari", "l_b": "balik", "l_c": "ceket", "l_ch": "cay", "l_d": "dondurma", "l_e": "ekmek",
    "l_f": "fil", "l_g": "gemi", "l_gh": "dag", "l_h": "havuc", "l_i_dotless": "irmak", "l_i": "inek",
    "l_j": "jole", "l_k": "kedi", "l_l": "limon", "l_m": "muz", "l_n": "nane", "l_o": "okul",
    "l_oe": "ordek", "l_p": "peynir", "l_r": "radyo", "l_s": "su", "l_t": "top", "l_u": "ucak",
    "l_sh": "sapka", "l_ue": "uzum", "l_v": "vapur", "l_y": "yumurta", "l_z": "zurafa",
}

# Words to look for among native-speaker recordings (Lingua Libre, Turkish).
# key -> exact Turkish word as it would appear in the recording's file name.
def pronunciation_words(vocabulary_items, letters):
    words = {}
    for it in vocabulary_items:
        if it["kind"] in ("WORD", "PHRASE"):
            words[it["id"]] = it["tr"].rstrip("?…").strip()
    for l in letters:
        words[l["id"] + "_ex"] = l["exampleTr"]
    return words
