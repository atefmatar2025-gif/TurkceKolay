#!/usr/bin/env python3
"""One-off generator for the bundled curriculum JSON. Output is the source of truth in the repo."""
import json, os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "app/src/main/assets/content")
IMAGES_DIR = os.path.join(ROOT, "app/src/main/assets/images/vocab")
sys.path.insert(0, os.path.join(ROOT, "tools/assets"))
from manifest import LETTER_EXAMPLES  # noqa: E402

VERSION = 2


def img(concept, emoji):
    """A reviewed photo/illustration when one exists, otherwise the emoji fallback."""
    if concept and os.path.exists(os.path.join(IMAGES_DIR, concept + ".webp")):
        return "images/vocab/" + concept + ".webp"
    return "emoji:" + emoji

# ---------------------------------------------------------------- alphabet
# id, upper, letter-name (spoken by TTS), Arabic approximation, example tr, example ar, emoji, special, note
L = [
 ("l_a","A","a","قريب من الفتحة المفتوحة (آ قصيرة)","Arı","نحلة","🐝",False,None),
 ("l_b","B","be","مثل الباء","Balık","سمكة","🐟",False,None),
 ("l_c","C","ce","مثل الجيم الفصيحة (j في الإنجليزية) — وليس مثل حرف c في الإنجليزية","Ceket","سترة","🧥",False,"تنبيه: C في التركية تُنطق «ج» دائمًا."),
 ("l_ch","Ç","çe","مثل «تش» (ch في الإنجليزية)","Çay","شاي","🍵",True,"لاحظ الذيل الصغير تحت الحرف: C = ج ، Ç = تش."),
 ("l_d","D","de","مثل الدال","Dondurma","بوظة (آيس كريم)","🍦",False,None),
 ("l_e","E","e","صوت بين الفتحة والكسرة (e في كلمة bed)","Ekmek","خبز","🍞",False,None),
 ("l_f","F","fe","مثل الفاء","Fil","فيل","🐘",False,None),
 ("l_g","G","ge","مثل الجيم المصرية (g في الإنجليزية)","Gemi","سفينة","🚢",False,None),
 ("l_gh","Ğ","yumuşak ge","لا يُنطق كحرف مستقل؛ يُطيل الصوت المتحرك الذي قبله","Dağ","جبل","⛰️",True,"Ğ لا تأتي في أول الكلمة أبدًا. في «Dağ» نمدّ صوت a قليلًا: «داا»."),
 ("l_h","H","he","مثل الهاء","Havuç","جزرة","🥕",False,None),
 ("l_i_dotless","I","ı","صوت قصير لا يوجد في العربية: بين الكسرة والضمة، والشفتان غير مستديرتين","Irmak","نهر","🏞️",True,"حرف I بلا نقطة، وصغيره ı بلا نقطة أيضًا. لا تخلطه مع İ / i."),
 ("l_i","İ","i","مثل الكسرة الطويلة «إي»","İnek","بقرة","🐄",True,"حرف İ بنقطة حتى في الحرف الكبير، وصغيره i. النقطة تغيّر المعنى: kız (بنت) غير kiz."),
 ("l_j","J","je","مثل j في الفرنسية (جيم معطّشة كما في garage)","Jöle","هلام (جيلي)","🍮",False,None),
 ("l_k","K","ke","مثل الكاف","Kedi","قطة","🐱",False,None),
 ("l_l","L","le","مثل اللام","Limon","ليمون","🍋",False,None),
 ("l_m","M","me","مثل الميم","Muz","موز","🍌",False,None),
 ("l_n","N","ne","مثل النون","Nane","نعناع","🌿",False,None),
 ("l_o","O","o","«أو» قصيرة والشفتان مستديرتان","Okul","مدرسة","🏫",False,None),
 ("l_oe","Ö","ö","لا يوجد في العربية: اللسان كما في e والشفتان مستديرتان كما في o","Ördek","بطة","🦆",True,"النقطتان فوق O تغيّران الصوت والمعنى: on (عشرة) غير ön (أمام)."),
 ("l_p","P","pe","مثل p في الإنجليزية (ليس الباء)","Peynir","جبن","🧀",False,"P غير B: أخرج نفَسًا خفيفًا مع P."),
 ("l_r","R","re","مثل الراء لكن أخف","Radyo","راديو","📻",False,None),
 ("l_s","S","se","مثل السين","Su","ماء","💧",False,None),
 ("l_sh","Ş","şe","مثل الشين","Şapka","قبعة","👒",True,"S = س ، Ş = ش. الذيل الصغير هو الفرق."),
 ("l_t","T","te","مثل التاء","Top","كرة","⚽",False,None),
 ("l_u","U","u","«أُو» قصيرة (ضمة) والشفتان مستديرتان","Uçak","طائرة","✈️",False,None),
 ("l_ue","Ü","ü","لا يوجد في العربية: اللسان كما في i والشفتان مستديرتان","Üzüm","عنب","🍇",True,"U غير Ü: un (طحين) غير ün (شهرة)."),
 ("l_v","V","ve","مثل v في الإنجليزية (ليس الفاء)","Vapur","عبّارة","⛴️",False,None),
 ("l_y","Y","ye","مثل الياء في «يد»","Yumurta","بيضة","🥚",False,None),
 ("l_z","Z","ze","مثل الزاي","Zürafa","زرافة","🦒",False,None),
]
assert len(L) == 29
alphabet = {"contentVersion": VERSION, "letters": [
    {"id": i, "upper": u, "name": n, "hintAr": h, "exampleTr": et, "exampleAr": ea,
     "image": img(LETTER_EXAMPLES.get(i), em), "isSpecial": sp, "note": note}
    for (i, u, n, h, et, ea, em, sp, note) in L]}

# ---------------------------------------------------------------- categories
CATS = [
 # id, ar, tr, emoji, group
 ("first_words","أول 30 كلمة","İlk Kelimeler","⭐","FIRST_WORDS"),
 ("expressions","عبارات أساسية","Temel İfadeler","💬","EXPRESSIONS"),
 ("family","العائلة","Aile","👨‍👩‍👧","EVERYDAY"),
 ("food","الطعام","Yiyecekler","🍲","EVERYDAY"),
 ("fruits","الفواكه","Meyveler","🍓","EVERYDAY"),
 ("vegetables","الخضروات","Sebzeler","🥕","EVERYDAY"),
 ("drinks","المشروبات","İçecekler","🧃","EVERYDAY"),
 ("animals","الحيوانات","Hayvanlar","🐾","EVERYDAY"),
 ("colors","الألوان","Renkler","🎨","EVERYDAY"),
 ("numbers","الأرقام","Sayılar","🔢","EVERYDAY"),
 ("body","أعضاء الجسم","Vücut","🧍","EVERYDAY"),
 ("clothes","الملابس","Giysiler","👕","EVERYDAY"),
 ("home","البيت","Ev","🏡","EVERYDAY"),
 ("furniture","الأثاث","Mobilya","🛋️","EVERYDAY"),
 ("transport","المواصلات","Ulaşım","🚌","EVERYDAY"),
 ("places","الأماكن","Yerler","📍","EVERYDAY"),
 ("time","الوقت","Zaman","⏰","EVERYDAY"),
 ("days","أيام الأسبوع","Günler","📆","EVERYDAY"),
 ("weather","الطقس","Hava Durumu","🌦️","EVERYDAY"),
 ("school","المدرسة","Okul","🎒","EVERYDAY"),
 ("work","العمل","İş","💼","EVERYDAY"),
 ("verbs","أفعال شائعة","Fiiller","🏃","EVERYDAY"),
 ("a1_intro","التعريف بالنفس","Tanışma","🙋","SENTENCES"),
 ("a1_questions","أسئلة بسيطة","Sorular","❓","SENTENCES"),
 ("a1_people","وصف الأشخاص","İnsanlar","🧑","SENTENCES"),
 ("a1_food","طلب الطعام","Yemek Siparişi","🍽️","SENTENCES"),
 ("a1_shopping","التسوق","Alışveriş","🛍️","SENTENCES"),
 ("a1_directions","السؤال عن الطريق","Yol Tarifi","🧭","SENTENCES"),
 ("a1_routine","روتيني اليومي","Günlük Rutin","🗓️","SENTENCES"),
 ("a1_negation","النفي والسؤال","Olumsuz ve Soru","🙅","SENTENCES"),
 ("a2_past","أحداث مضت","Geçmiş","🕰️","SENTENCES"),
 ("a2_future","خطط المستقبل","Gelecek Planları","🔮","SENTENCES"),
 ("a2_appointments","المواعيد","Randevular","📅","SENTENCES"),
 ("a2_travel","السفر","Seyahat","🧳","SENTENCES"),
]

# id-suffix, tr, ar, emoji, exTr, exAr, note
W = {}
W["first_words"] = [
 ("merhaba","Merhaba","مرحبًا","👋","Merhaba, ben Ali.","مرحبًا، أنا علي.",None),
 ("evet","Evet","نعم","✅","Evet, anladım.","نعم، فهمت.",None),
 ("hayir","Hayır","لا","❌","Hayır, teşekkürler.","لا، شكرًا.",None),
 ("su","Su","ماء","💧","Su içiyorum.","أنا أشرب الماء.",None),
 ("ekmek","Ekmek","خبز","🍞","Bu ekmek taze.","هذا الخبز طازج.",None),
 ("elma","Elma","تفاحة","🍎","Bu bir elma.","هذه تفاحة.",None),
 ("ev","Ev","منزل","🏠","Bu benim evim.","هذا منزلي.",None),
 ("kitap","Kitap","كتاب","📖","Bu bir kitap.","هذا كتاب.",None),
 ("anne","Anne","أم","👩","Annem evde.","أمي في المنزل.",None),
 ("baba","Baba","أب","👨","Babam çalışıyor.","أبي يعمل.",None),
 ("cay","Çay","شاي","🍵","Çay sıcak.","الشاي ساخن.",None),
 ("kahve","Kahve","قهوة","☕","Bir kahve, lütfen.","قهوة واحدة، من فضلك.",None),
 ("sut","Süt","حليب","🥛","Süt soğuk.","الحليب بارد.",None),
 ("araba","Araba","سيارة","🚗","Araba büyük.","السيارة كبيرة.",None),
 ("kedi","Kedi","قطة","🐱","Kedi küçük.","القطة صغيرة.",None),
 ("kopek","Köpek","كلب","🐶","Köpek bahçede.","الكلب في الحديقة.",None),
 ("okul","Okul","مدرسة","🏫","Okul yakın.","المدرسة قريبة.",None),
 ("kapi","Kapı","باب","🚪","Kapı açık.","الباب مفتوح.",None),
 ("sandalye","Sandalye","كرسي","🪑","Sandalye boş.","الكرسي فارغ.",None),
 ("telefon","Telefon","هاتف","📱","Telefonum nerede?","أين هاتفي؟",None),
 ("para","Para","مال (نقود)","💵","Param yok.","ليس لدي مال.",None),
 ("gunes","Güneş","شمس","☀️","Güneş parlıyor.","الشمس مشرقة.",None),
 ("ay","Ay","قمر","🌙","Ay çok güzel.","القمر جميل جدًا.","«Ay» تعني أيضًا «شهر»."),
 ("cocuk","Çocuk","طفل","🧒","Çocuk oynuyor.","الطفل يلعب.",None),
 ("arkadas","Arkadaş","صديق","🧑‍🤝‍🧑","O benim arkadaşım.","هو (أو هي) صديقي.","«O» تعني هو أو هي؛ التركية لا تفرّق بين المذكر والمؤنث."),
 ("yemek","Yemek","طعام","🍽️","Yemek hazır.","الطعام جاهز.","«Yemek» تعني أيضًا الفعل «يأكل»."),
 ("goz","Göz","عين","👁️","Bu benim gözüm.","هذه عيني.",None),
 ("el","El","يد","✋","Elim soğuk.","يدي باردة.",None),
 ("canta","Çanta","حقيبة","👜","Çanta yeşil.","الحقيبة خضراء.",None),
 ("iyi","İyi","جيد","👍","Çok iyi!","جيد جدًا!",None),
]
W["expressions"] = [
 ("gunaydin","Günaydın","صباح الخير","🌅","Günaydın, anne!","صباح الخير يا أمي!",None),
 ("iyi_aksamlar","İyi akşamlar","مساء الخير","🌆","İyi akşamlar, hoş geldiniz.","مساء الخير، أهلًا بكم.",None),
 ("hos_geldiniz","Hoş geldiniz","أهلًا وسهلًا","🤗","Hoş geldiniz! — Hoş bulduk!","أهلًا وسهلًا! — أهلًا بك (الرد المعتاد)!","الرد التقليدي على «Hoş geldiniz» هو «Hoş bulduk»."),
 ("hosca_kal","Hoşça kal","مع السلامة","👋","Hoşça kal, yarın görüşürüz.","مع السلامة، نلتقي غدًا.","يقولها الشخص الذي يغادر."),
 ("gorusuruz","Görüşürüz","إلى اللقاء","🙋","Görüşürüz, iyi günler!","إلى اللقاء، يومًا سعيدًا!",None),
 ("tesekkur","Teşekkür ederim","شكرًا لك","🙏","Çok teşekkür ederim.","شكرًا جزيلًا.",None),
 ("lutfen","Lütfen","من فضلك","🤲","Su, lütfen.","ماء، من فضلك.",None),
 ("affedersiniz","Affedersiniz","المعذرة","🙇","Affedersiniz, banyo nerede?","المعذرة، أين الحمام؟","تُستعمل لجذب الانتباه أو للاعتذار البسيط."),
 ("ozur_dilerim","Özür dilerim","أنا آسف","😔","Özür dilerim, geç kaldım.","أنا آسف، تأخرت.",None),
 ("nasilsiniz","Nasılsınız?","كيف حالك؟","😊","Merhaba, nasılsınız?","مرحبًا، كيف حالك؟","صيغة مهذبة. مع الأصدقاء نقول «Nasılsın?»."),
 ("iyiyim","İyiyim, teşekkürler","أنا بخير، شكرًا","😄","İyiyim, teşekkürler. Siz?","أنا بخير، شكرًا. وأنت؟",None),
 ("benim_adim","Benim adım…","اسمي…","🪪","Benim adım Ahmet.","اسمي أحمد.",None),
 ("ben_den","Ben … -denim","أنا من …","🌍","Ben Suriye'denim.","أنا من سوريا.","اللاحقة تتغير حسب الكلمة: -den / -dan / -ten / -tan."),
 ("anlamadim","Anlamadım","لم أفهم","🤷","Özür dilerim, anlamadım.","آسف، لم أفهم.",None),
 ("tekrar","Tekrar eder misiniz?","هل يمكنك أن تعيد؟","🔁","Anlamadım. Tekrar eder misiniz?","لم أفهم. هل يمكنك أن تعيد؟",None),
 ("yavas","Lütfen yavaş konuşun","تكلّم ببطء من فضلك","🐢","Lütfen yavaş konuşun, yeni öğreniyorum.","تكلّم ببطء من فضلك، أنا أتعلّم حديثًا.",None),
]
W["family"] = [
 ("aile","Aile","عائلة","👨‍👩‍👧","Bu benim ailem.","هذه عائلتي.",None),
 ("kardes","Kardeş","أخ أو أخت","👫","Bir kardeşim var.","لدي أخ واحد (أو أخت).","«Kardeş» تُقال للأخ وللأخت."),
 ("dede","Dede","جد","👴","Dedem yaşlı.","جدي كبير في السن.",None),
 ("buyukanne","Büyükanne","جدة","👵","Büyükannem yemek yapıyor.","جدتي تطبخ.",None),
 ("ogul","Oğul","ابن","👦","Oğlum okulda.","ابني في المدرسة.",None),
 ("kiz","Kız","بنت (ابنة)","👧","Kızım kitap okuyor.","ابنتي تقرأ كتابًا.",None),
 ("es","Eş","زوج أو زوجة","💑","Eşim doktor.","زوجي (أو زوجتي) طبيب.",None),
 ("bebek","Bebek","طفل رضيع","👶","Bebek uyuyor.","الرضيع نائم.",None),
]
W["food"] = [
 ("peynir","Peynir","جبن","🧀","Peynir lezzetli.","الجبن لذيذ.",None),
 ("yumurta","Yumurta","بيضة","🥚","Kahvaltıda yumurta yiyorum.","آكل البيض في الفطور.",None),
 ("pilav","Pilav","أرز مطبوخ","🍚","Pilav sıcak.","الأرز ساخن.",None),
 ("corba","Çorba","شوربة","🍲","Çorba çok güzel.","الشوربة طيبة جدًا.",None),
 ("et","Et","لحم","🥩","Et pahalı.","اللحم غالٍ.",None),
 ("tavuk","Tavuk","دجاج","🍗","Tavuk yiyorum.","آكل الدجاج.",None),
 ("balik","Balık","سمك","🐟","Balık taze.","السمك طازج.",None),
 ("bal","Bal","عسل","🍯","Bal tatlı.","العسل حلو.",None),
]
W["fruits"] = [
 ("muz","Muz","موز","🍌","Muz sarı.","الموز أصفر.",None),
 ("portakal","Portakal","برتقال","🍊","Portakal suyu içiyorum.","أشرب عصير البرتقال.",None),
 ("uzum","Üzüm","عنب","🍇","Üzüm tatlı.","العنب حلو.",None),
 ("cilek","Çilek","فراولة","🍓","Çilek kırmızı.","الفراولة حمراء.",None),
 ("karpuz","Karpuz","بطيخ","🍉","Karpuz büyük.","البطيخة كبيرة.",None),
 ("armut","Armut","إجاص (كمثرى)","🍐","Armut yeşil.","الإجاصة خضراء.",None),
 ("limon","Limon","ليمون","🍋","Limon ekşi.","الليمون حامض.",None),
]
W["vegetables"] = [
 ("domates","Domates","طماطم","🍅","Domates kırmızı.","الطماطم حمراء.",None),
 ("salatalik","Salatalık","خيار","🥒","Salatalık taze.","الخيار طازج.",None),
 ("patates","Patates","بطاطا","🥔","Patates kızartması seviyorum.","أحب البطاطا المقلية.",None),
 ("sogan","Soğan","بصل","🧅","Soğan doğruyorum.","أقطّع البصل.",None),
 ("havuc","Havuç","جزر","🥕","Havuç turuncu.","الجزر برتقالي اللون.",None),
 ("biber","Biber","فلفل","🫑","Biber yeşil.","الفلفل أخضر.",None),
 ("patlican","Patlıcan","باذنجان","🍆","Patlıcan mor.","الباذنجان بنفسجي.",None),
 ("sarimsak","Sarımsak","ثوم","🧄","Sarımsak kokulu.","الثوم رائحته قوية.",None),
]
W["drinks"] = [
 ("meyve_suyu","Meyve suyu","عصير فواكه","🧃","Meyve suyu soğuk.","العصير بارد.",None),
 ("gazli_icecek","Gazlı içecek","مشروب غازي","🥤","Gazlı içecek istemiyorum.","لا أريد مشروبًا غازيًا.",None),
 ("ayran","Ayran","لبن عيران","🫗","Ayran çok serinletici.","العيران منعش جدًا.",None),
 ("bitki_cayi","Bitki çayı","شاي أعشاب","🫖","Akşam bitki çayı içiyorum.","أشرب شاي الأعشاب مساءً.",None),
 ("limonata","Limonata","عصير ليمون","🍹","Bir limonata, lütfen.","عصير ليمون واحد، من فضلك.",None),
]
W["animals"] = [
 ("kus","Kuş","طائر","🐦","Kuş uçuyor.","الطائر يطير.",None),
 ("at","At","حصان","🐴","At hızlı koşuyor.","الحصان يركض بسرعة.",None),
 ("inek","İnek","بقرة","🐄","İnek süt verir.","البقرة تعطي الحليب.",None),
 ("tavsan","Tavşan","أرنب","🐰","Tavşan havuç yiyor.","الأرنب يأكل الجزر.",None),
 ("aslan","Aslan","أسد","🦁","Aslan güçlü.","الأسد قوي.",None),
 ("maymun","Maymun","قرد","🐒","Maymun ağaçta.","القرد على الشجرة.",None),
 ("koyun","Koyun","خروف","🐑","Koyun beyaz.","الخروف أبيض.",None),
 ("fil","Fil","فيل","🐘","Fil çok büyük.","الفيل كبير جدًا.",None),
]
W["colors"] = [
 ("kirmizi","Kırmızı","أحمر","🔴","Elma kırmızı.","التفاحة حمراء.",None),
 ("mavi","Mavi","أزرق","🔵","Gökyüzü mavi.","السماء زرقاء.",None),
 ("yesil","Yeşil","أخضر","🟢","Çimen yeşil.","العشب أخضر.",None),
 ("sari","Sarı","أصفر","🟡","Güneş sarı.","الشمس صفراء.",None),
 ("siyah","Siyah","أسود","⚫","Kedim siyah.","قطتي سوداء.",None),
 ("beyaz","Beyaz","أبيض","⚪","Süt beyaz.","الحليب أبيض.",None),
 ("turuncu","Turuncu","برتقالي","🟠","Portakal turuncu.","البرتقالة برتقالية اللون.",None),
 ("mor","Mor","بنفسجي","🟣","Çantam mor.","حقيبتي بنفسجية.",None),
]
NUM_NOTE = "بعد العدد يبقى الاسم مفردًا في التركية."
W["numbers"] = [
 ("bir","Bir","واحد","1️⃣","Bir elma.","تفاحة واحدة.",None),
 ("iki","İki","اثنان","2️⃣","İki kedi.","قطتان.",NUM_NOTE),
 ("uc","Üç","ثلاثة","3️⃣","Üç kitap.","ثلاثة كتب.",NUM_NOTE),
 ("dort","Dört","أربعة","4️⃣","Dört araba.","أربع سيارات.",NUM_NOTE),
 ("bes","Beş","خمسة","5️⃣","Beş çocuk.","خمسة أطفال.",NUM_NOTE),
 ("alti","Altı","ستة","6️⃣","Altı yumurta.","ست بيضات.",NUM_NOTE),
 ("yedi","Yedi","سبعة","7️⃣","Yedi gün.","سبعة أيام.",NUM_NOTE),
 ("sekiz","Sekiz","ثمانية","8️⃣","Sekiz saat.","ثماني ساعات.",NUM_NOTE),
 ("dokuz","Dokuz","تسعة","9️⃣","Dokuz kuş.","تسعة طيور.",NUM_NOTE),
 ("on","On","عشرة","🔟","On parmak.","عشرة أصابع.",NUM_NOTE),
]
W["body"] = [
 ("agiz","Ağız","فم","👄","Ağzını aç.","افتح فمك.",None),
 ("burun","Burun","أنف","👃","Burnum akıyor.","أنفي يسيل.",None),
 ("kulak","Kulak","أذن","👂","Kulağım ağrıyor.","أذني تؤلمني.",None),
 ("ayak","Ayak","قدم","🦶","Ayağım üşüyor.","قدمي باردة.",None),
 ("dis","Diş","سنّ","🦷","Dişlerimi fırçalıyorum.","أنظّف أسناني بالفرشاة.",None),
 ("sac","Saç","شعر","💇","Saçım uzun.","شعري طويل.",None),
 ("kol","Kol","ذراع","💪","Kolum ağrıyor.","ذراعي تؤلمني.",None),
]
W["clothes"] = [
 ("tisort","Tişört","تي شيرت","👕","Tişörtüm mavi.","قميصي أزرق.",None),
 ("pantolon","Pantolon","بنطال","👖","Pantolon uzun.","البنطال طويل.",None),
 ("elbise","Elbise","فستان","👗","Elbise çok güzel.","الفستان جميل جدًا.",None),
 ("ayakkabi","Ayakkabı","حذاء","👟","Ayakkabılarım yeni.","حذائي جديد.",None),
 ("sapka","Şapka","قبعة","👒","Şapkanı tak.","البس قبعتك.",None),
 ("ceket","Ceket","سترة","🧥","Ceket sıcak tutar.","السترة تُدفئ.",None),
 ("corap","Çorap","جورب","🧦","Çoraplar nerede?","أين الجوارب؟",None),
 ("eldiven","Eldiven","قفاز","🧤","Eldiven giyiyorum.","ألبس القفازات.",None),
]
W["home"] = [
 ("mutfak","Mutfak","مطبخ","🍳","Mutfak temiz.","المطبخ نظيف.",None),
 ("banyo","Banyo","حمّام","🛁","Banyo nerede?","أين الحمام؟",None),
 ("pencere","Pencere","نافذة","🪟","Pencereyi aç.","افتح النافذة.",None),
 ("anahtar","Anahtar","مفتاح","🔑","Anahtarım yok.","ليس معي مفتاحي.",None),
 ("bahce","Bahçe","حديقة","🌳","Bahçede çiçekler var.","توجد أزهار في الحديقة.",None),
 ("merdiven","Merdiven","درج","🪜","Merdivenden çıkıyorum.","أصعد الدرج.",None),
 ("duvar","Duvar","جدار","🧱","Duvar beyaz.","الجدار أبيض.",None),
]
W["furniture"] = [
 ("koltuk","Koltuk","أريكة","🛋️","Koltuk rahat.","الأريكة مريحة.",None),
 ("yatak","Yatak","سرير","🛏️","Yatak büyük.","السرير كبير.",None),
 ("dolap","Dolap","خزانة","🗄️","Dolap dolu.","الخزانة ممتلئة.",None),
 ("lamba","Lamba","مصباح","💡","Lambayı kapat.","أطفئ المصباح.",None),
 ("ayna","Ayna","مرآة","🪞","Aynaya bakıyorum.","أنظر في المرآة.",None),
 ("televizyon","Televizyon","تلفاز","📺","Televizyon izliyorum.","أشاهد التلفاز.",None),
]
W["transport"] = [
 ("otobus","Otobüs","حافلة","🚌","Otobüs geliyor.","الحافلة قادمة.",None),
 ("tren","Tren","قطار","🚆","Tren hızlı.","القطار سريع.",None),
 ("ucak","Uçak","طائرة","✈️","Uçak havada.","الطائرة في الجو.",None),
 ("bisiklet","Bisiklet","دراجة","🚲","Bisiklete biniyorum.","أركب الدراجة.",None),
 ("taksi","Taksi","سيارة أجرة","🚕","Taksi çağırır mısınız?","هل يمكنك طلب سيارة أجرة؟",None),
 ("gemi","Gemi","سفينة","🚢","Gemi denizde.","السفينة في البحر.",None),
 ("metro","Metro","مترو","🚇","Metroyla gidiyorum.","أذهب بالمترو.",None),
]
W["places"] = [
 ("hastane","Hastane","مستشفى","🏥","Hastane nerede?","أين المستشفى؟",None),
 ("market","Market","بقالة","🛒","Marketten ekmek alıyorum.","أشتري الخبز من البقالة.",None),
 ("eczane","Eczane","صيدلية","💊","Eczane açık mı?","هل الصيدلية مفتوحة؟",None),
 ("cami","Cami","مسجد","🕌","Cami çok güzel.","المسجد جميل جدًا.",None),
 ("banka","Banka","بنك","🏦","Banka kapalı.","البنك مغلق.",None),
 ("otel","Otel","فندق","🏨","Otel denize yakın.","الفندق قريب من البحر.",None),
 ("havalimani","Havalimanı","مطار","🛫","Havalimanına gidiyorum.","أنا ذاهب إلى المطار.",None),
 ("restoran","Restoran","مطعم","🍴","Restoran kalabalık.","المطعم مزدحم.",None),
]
W["time"] = [
 ("saat","Saat","ساعة","⏰","Saat kaç?","كم الساعة؟",None),
 ("dakika","Dakika","دقيقة","⏱️","Beş dakika bekle.","انتظر خمس دقائق.",None),
 ("sabah","Sabah","صباح","🌄","Sabah kahve içiyorum.","أشرب القهوة صباحًا.",None),
 ("ogle","Öğle","ظهر","🌞","Öğle yemeği hazır.","الغداء جاهز.",None),
 ("aksam","Akşam","مساء","🌇","Akşam evdeyim.","أنا في البيت مساءً.",None),
 ("gece","Gece","ليل","🌃","Gece uyuyorum.","أنام في الليل.",None),
 ("bugun","Bugün","اليوم","📅","Bugün cuma.","اليوم الجمعة.",None),
 ("yarin","Yarın","غدًا","🗓️","Yarın görüşürüz.","نلتقي غدًا.",None),
]
W["days"] = [
 ("pazartesi","Pazartesi","الإثنين","1️⃣","Pazartesi işe gidiyorum.","يوم الإثنين أذهب إلى العمل.","الأسبوع التركي يبدأ بيوم الإثنين."),
 ("sali","Salı","الثلاثاء","2️⃣","Salı günü dersim var.","لدي درس يوم الثلاثاء.",None),
 ("carsamba","Çarşamba","الأربعاء","3️⃣","Çarşamba boşum.","أنا متفرّغ يوم الأربعاء.",None),
 ("persembe","Perşembe","الخميس","4️⃣","Perşembe annemi arıyorum.","يوم الخميس أتصل بأمي.",None),
 ("cuma","Cuma","الجمعة","5️⃣","Cuma günü camiye gidiyoruz.","يوم الجمعة نذهب إلى المسجد.",None),
 ("cumartesi","Cumartesi","السبت","6️⃣","Cumartesi tatil.","السبت عطلة.",None),
 ("pazar","Pazar","الأحد","7️⃣","Pazar günü dinleniyorum.","أرتاح يوم الأحد.","«Pazar» تعني أيضًا «سوق شعبي»."),
]
W["weather"] = [
 ("gunesli","Güneşli","مشمس","🌤️","Bugün hava güneşli.","الجو مشمس اليوم.",None),
 ("yagmur","Yağmur","مطر","🌧️","Yağmur yağıyor.","المطر يهطل.",None),
 ("kar","Kar","ثلج","❄️","Kar yağıyor.","الثلج يتساقط.",None),
 ("ruzgar","Rüzgâr","ريح","💨","Rüzgâr çok güçlü.","الريح قوية جدًا.",None),
 ("bulut","Bulut","غيمة","☁️","Gökyüzünde bulut var.","في السماء غيمة.",None),
 ("sicak","Sıcak","حار","🥵","Hava sıcak.","الجو حار.",None),
 ("soguk","Soğuk","بارد","🥶","Hava soğuk.","الجو بارد.",None),
 ("semsiye","Şemsiye","مظلة","☂️","Şemsiyeni al.","خذ مظلتك.",None),
]
W["school"] = [
 ("ogretmen","Öğretmen","معلّم","🧑‍🏫","Öğretmen sınıfta.","المعلّم في الصف.",None),
 ("ogrenci","Öğrenci","طالب","🧑‍🎓","Ben öğrenciyim.","أنا طالب.",None),
 ("kalem","Kalem","قلم","✏️","Kalemin var mı?","هل لديك قلم؟",None),
 ("defter","Defter","دفتر","📓","Defterim yeni.","دفتري جديد.",None),
 ("ders","Ders","درس","📚","Ders başlıyor.","الدرس يبدأ.",None),
 ("makas","Makas","مقص","✂️","Makas keskin.","المقص حاد.",None),
 ("cetvel","Cetvel","مسطرة","📏","Cetvel uzun.","المسطرة طويلة.",None),
]
W["work"] = [
 ("is","İş","عمل","💼","İşe gidiyorum.","أنا ذاهب إلى العمل.",None),
 ("doktor","Doktor","طبيب","🧑‍⚕️","Doktor hastanede.","الطبيب في المستشفى.",None),
 ("asci","Aşçı","طبّاخ","🧑‍🍳","Aşçı yemek yapıyor.","الطبّاخ يطبخ.",None),
 ("polis","Polis","شرطي","👮","Polis yardım ediyor.","الشرطي يساعد.",None),
 ("muhendis","Mühendis","مهندس","👷","Babam mühendis.","أبي مهندس.",None),
 ("ciftci","Çiftçi","مزارع","🧑‍🌾","Çiftçi tarlada çalışıyor.","المزارع يعمل في الحقل.",None),
 ("toplanti","Toplantı","اجتماع","🤝","Toplantı saat üçte.","الاجتماع في الساعة الثالثة.",None),
 ("ofis","Ofis","مكتب (مكان العمل)","🏢","Ofis büyük.","المكتب كبير.",None),
]
VERB_NOTE = "الفعل هنا بصيغة المصدر وينتهي بـ -mak أو -mek."
W["verbs"] = [
 ("uyumak","Uyumak","ينام","😴","Erken uyuyorum.","أنام مبكرًا.",VERB_NOTE),
 ("kosmak","Koşmak","يركض","🏃","Parkta koşuyorum.","أركض في الحديقة.",VERB_NOTE),
 ("yazmak","Yazmak","يكتب","✍️","Mektup yazıyorum.","أكتب رسالة.",VERB_NOTE),
 ("okumak","Okumak","يقرأ","📖","Kitap okuyorum.","أقرأ كتابًا.",VERB_NOTE),
 ("konusmak","Konuşmak","يتكلّم","🗣️","Türkçe konuşuyorum.","أتكلّم التركية.",VERB_NOTE),
 ("dinlemek","Dinlemek","يستمع","🎧","Müzik dinliyorum.","أستمع إلى الموسيقى.",VERB_NOTE),
 ("gitmek","Gitmek","يذهب","🚶","Eve gidiyorum.","أذهب إلى البيت.",VERB_NOTE),
 ("icmek","İçmek","يشرب","🚰","Su içiyorum.","أشرب الماء.",VERB_NOTE),
]

# Sentences: id-suffix, tr, ar, emoji, note
S = {}
S["a1_intro"] = [
 ("adim_ali","Benim adım Ali.","اسمي علي.","🙋",None),
 ("suriyeli","Ben Suriyeliyim.","أنا سوري.","🌍","اللاحقة -li تعني «من بلد…»."),
 ("yirmi_yas","Ben yirmi yaşındayım.","عمري عشرون سنة.","🎂",None),
 ("memnun","Tanıştığımıza memnun oldum.","سررت بالتعرف إليك.","🤝",None),
 ("adiniz_ne","Sizin adınız ne?","ما اسمك؟","🪪",None),
]
S["a1_questions"] = [
 ("bu_ne","Bu ne?","ما هذا؟","🤔",None),
 ("nerede","Nerede?","أين؟","📍",None),
 ("ne_zaman","Ne zaman?","متى؟","🕑",None),
 ("kim","Kim?","من؟","🧐",None),
 ("kac_lira","Kaç lira?","بكم ليرة؟","💰",None),
 ("neden","Neden?","لماذا؟","❔",None),
]
S["a1_people"] = [
 ("uzun_boylu","O uzun boylu.","هو (أو هي) طويل القامة.","🧍",None),
 ("cok_mutlu","O çok mutlu.","هو (أو هي) سعيد جدًا.","😊",None),
 ("kadin_genc","Kadın genç.","المرأة شابة.","👩‍🦰",None),
 ("adam_yorgun","Adam yorgun.","الرجل متعب.","😩",None),
 ("cocuk_kucuk","Çocuk küçük.","الطفل صغير.","🧒",None),
]
S["a1_food"] = [
 ("menu","Menüyü alabilir miyim?","هل يمكنني رؤية قائمة الطعام؟","📋",None),
 ("bir_cay","Bir çay, lütfen.","شاي واحد، من فضلك.","🍵",None),
 ("hesap","Hesap, lütfen.","الحساب، من فضلك.","🧾",None),
 ("lezzetli","Çok lezzetli!","لذيذ جدًا!","😋",None),
 ("su_var_mi","Su var mı?","هل يوجد ماء؟","💧",None),
 ("afiyet","Afiyet olsun!","بالهناء والشفاء!","🍽️",None),
]
S["a1_shopping"] = [
 ("ne_kadar","Bu ne kadar?","كم سعر هذا؟","🏷️",None),
 ("pahali","Çok pahalı.","غالٍ جدًا.","💸",None),
 ("ucuz","Daha ucuz var mı?","هل يوجد أرخص؟","🪙",None),
 ("kartla","Kartla ödeyebilir miyim?","هل يمكنني الدفع بالبطاقة؟","💳",None),
 ("aliyorum","Bunu alıyorum.","سآخذ هذا.","🛍️",None),
 ("poset","Poşet istiyorum.","أريد كيسًا.","🛒",None),
]
S["a1_directions"] = [
 ("tuvalet","Tuvalet nerede?","أين المرحاض؟","🚻",None),
 ("duz","Düz gidin.","اذهب إلى الأمام مباشرة.","⬆️",None),
 ("saga","Sağa dönün.","انعطف يمينًا.","➡️",None),
 ("sola","Sola dönün.","انعطف يسارًا.","⬅️",None),
 ("yakin_mi","Yakın mı?","هل هو قريب؟","🚶",None),
 ("uzak_degil","Uzak değil.","ليس بعيدًا.","🗺️",None),
]
S["a1_routine"] = [
 ("kalkiyorum","Sabah yedide kalkıyorum.","أستيقظ في السابعة صباحًا.","⏰",None),
 ("kahvalti","Kahvaltı yapıyorum.","أتناول الفطور.","🥐",None),
 ("otobusle","İşe otobüsle gidiyorum.","أذهب إلى العمل بالحافلة.","🚌",None),
 ("pisiriyorum","Akşam yemek pişiriyorum.","أطبخ في المساء.","🍳",None),
 ("yatiyorum","Saat on birde yatıyorum.","أنام في الساعة الحادية عشرة.","🛌",None),
]
S["a1_negation"] = [
 ("yorgun_degilim","Ben yorgun değilim.","أنا لست متعبًا.","💪","«değil» تنفي الصفات والأسماء."),
 ("icmiyorum","Kahve içmiyorum.","لا أشرب القهوة.","🚫","«-mi-» داخل الفعل تنفيه."),
 ("anliyor_musun","Anlıyor musun?","هل تفهم؟","💭","«mu / mi / mı / mü» تحوّل الجملة إلى سؤال."),
 ("anliyorum","Evet, anlıyorum.","نعم، أفهم.","👍",None),
 ("bilmiyorum","Hayır, bilmiyorum.","لا، لا أعرف.","🤷",None),
 ("turkce_biliyor","Türkçe biliyor musunuz?","هل تعرف التركية؟","🇹🇷",None),
]
S["a2_past"] = [
 ("sinema","Dün sinemaya gittim.","ذهبت إلى السينما أمس.","🎬","«-di / -dı» علامة الماضي."),
 ("istanbul","Geçen hafta İstanbul'daydım.","كنت في إسطنبول الأسبوع الماضي.","🌉",None),
 ("aksam_yemegi","Akşam yemeği yedik.","تناولنا العشاء.","🥘",None),
 ("bitirdim","Kitabı bitirdim.","أنهيت الكتاب.","📗",None),
 ("ne_yaptin","Ne yaptın?","ماذا فعلت؟","❓",None),
]
S["a2_future"] = [
 ("ankara","Yarın Ankara'ya gideceğim.","سأذهب إلى أنقرة غدًا.","🚆","«-ecek / -acak» علامة المستقبل."),
 ("dinlenecegim","Hafta sonu dinleneceğim.","سأرتاح في عطلة نهاية الأسبوع.","🏖️",None),
 ("ogrenecegim","Türkçe öğreneceğim.","سأتعلّم التركية.","📚",None),
 ("geleceksin","Ne zaman geleceksin?","متى ستأتي؟","🕑",None),
 ("tatil","Gelecek yıl tatile çıkacağız.","سنذهب في إجازة السنة القادمة.","✈️",None),
]
S["a2_appointments"] = [
 ("randevu","Randevu almak istiyorum.","أريد أن أحجز موعدًا.","📅",None),
 ("uygun","Saat üçte uygun musunuz?","هل أنت متفرّغ في الساعة الثالثة؟","🕒",None),
 ("ertelendi","Toplantı ertelendi.","تأجّل الاجتماع.","⏳",None),
 ("doktora","Doktora gitmem lazım.","يجب أن أذهب إلى الطبيب.","🩺",None),
 ("gec_kalacagim","Biraz geç kalacağım.","سأتأخر قليلًا.","⌛",None),
]
S["a2_travel"] = [
 ("bilet","Bir bilet istiyorum.","أريد تذكرة واحدة.","🎫",None),
 ("tren_kacta","Tren kaçta kalkıyor?","متى يغادر القطار؟","🚉",None),
 ("rezervasyon","Otelde rezervasyonum var.","لدي حجز في الفندق.","🏨",None),
 ("pasaport","Pasaportum burada.","جواز سفري هنا.","🛂",None),
 ("havalimani_uzak","Havalimanı ne kadar uzak?","كم يبعد المطار؟","🛫",None),
]

KIND = {"FIRST_WORDS": "WORD", "EXPRESSIONS": "PHRASE", "EVERYDAY": "WORD"}
cat_group = {c[0]: c[4] for c in CATS}
items, ids = [], set()
def add(item):
    assert item["id"] not in ids, item["id"]
    ids.add(item["id"]); items.append(item)

for cat, rows in W.items():
    for (sid, tr, ar, em, extr, exar, note) in rows:
        add({"id": f"w_{sid}", "kind": KIND[cat_group[cat]], "tr": tr, "ar": ar, "categoryId": cat,
             "image": img(sid, em), "exampleTr": extr, "exampleAr": exar, "note": note,
             "difficulty": 1 if cat_group[cat] != "EVERYDAY" else 2})
for cat, rows in S.items():
    for (sid, tr, ar, em, note) in rows:
        add({"id": f"s_{sid}", "kind": "SENTENCE", "tr": tr, "ar": ar, "categoryId": cat,
             "image": "emoji:" + em, "exampleTr": None, "exampleAr": None, "note": note,
             "difficulty": 3 if cat.startswith("a1") else 4})

vocabulary = {"contentVersion": VERSION,
              "categories": [{"id": c, "titleAr": a, "titleTr": t, "image": "emoji:" + e, "group": g, "sortOrder": i}
                             for i, (c, a, t, e, g) in enumerate(CATS)],
              "items": [i for i in items if i["kind"] != "SENTENCE"]}
sentences = {"contentVersion": VERSION, "items": [i for i in items if i["kind"] == "SENTENCE"]}

# ---------------------------------------------------------------- conversations
def turn(tr, ar, right, wrongs, hint):
    return {"tr": tr, "ar": ar, "hintAr": hint,
            "choices": [{"tr": right[0], "ar": right[1], "correct": True}] +
                       [{"tr": w[0], "ar": w[1], "correct": False} for w in wrongs]}
conversations = {"contentVersion": VERSION, "conversations": [
 {"id": "c_greeting", "titleAr": "التعارف", "image": "emoji:🤝", "turns": [
   turn("Merhaba! Nasılsınız?", "مرحبًا! كيف حالك؟", ("İyiyim, teşekkürler. Siz nasılsınız?", "أنا بخير، شكرًا. وأنت؟"),
        [("Hoşça kalın.", "مع السلامة."), ("Bir çay, lütfen.", "شاي واحد، من فضلك.")], "يسألك عن حالك."),
   turn("Ben de iyiyim. Adınız ne?", "وأنا بخير أيضًا. ما اسمك؟", ("Benim adım Ahmet.", "اسمي أحمد."),
        [("Saat üç.", "الساعة الثالثة."), ("Evet, pahalı.", "نعم، غالٍ.")], "يسألك عن اسمك."),
   turn("Memnun oldum Ahmet. Nerelisiniz?", "تشرفت يا أحمد. من أين أنت؟", ("Ben Suriyeliyim.", "أنا سوري."),
        [("Ben yorgunum.", "أنا متعب."), ("Banyo nerede?", "أين الحمام؟")], "يسألك من أي بلد أنت."),
   turn("Hoş geldiniz!", "أهلًا وسهلًا!", ("Hoş bulduk!", "أهلًا بك (الرد المعتاد)!"),
        [("Afiyet olsun!", "بالهناء والشفاء!"), ("Hayır.", "لا.")], "هناك رد تقليدي على الترحيب."),
 ]},
 {"id": "c_cafe", "titleAr": "في المقهى", "image": "emoji:☕", "turns": [
   turn("Hoş geldiniz! Ne alırsınız?", "أهلًا بك! ماذا تطلب؟", ("Bir çay, lütfen.", "شاي واحد، من فضلك."),
        [("Benim adım Ali.", "اسمي علي."), ("Görüşürüz.", "إلى اللقاء.")], "النادل يسأل عن طلبك."),
   turn("Şekerli mi?", "بالسكر؟", ("Evet, şekerli lütfen.", "نعم، بالسكر من فضلك."),
        [("Sağa dönün.", "انعطف يمينًا."), ("Ben öğrenciyim.", "أنا طالب.")], "يسأل إن كنت تريد السكر."),
   turn("Başka bir şey?", "شيء آخر؟", ("Hayır, teşekkürler. Hesap, lütfen.", "لا، شكرًا. الحساب من فضلك."),
        [("Kar yağıyor.", "الثلج يتساقط."), ("Merhaba.", "مرحبًا.")], "يسأل إن كنت تريد شيئًا آخر."),
   turn("Toplam otuz lira.", "المجموع ثلاثون ليرة.", ("Buyurun.", "تفضّل."),
        [("Gece uyuyorum.", "أنام في الليل."), ("Yarın görüşürüz.", "نلتقي غدًا.")], "أخبرك بالسعر. ماذا تقول وأنت تعطيه المال؟"),
 ]},
 {"id": "c_directions", "titleAr": "السؤال عن الطريق", "image": "emoji:🧭", "turns": [
   turn("Buyurun, yardım edebilir miyim?", "تفضّل، هل أستطيع مساعدتك؟", ("Affedersiniz, eczane nerede?", "المعذرة، أين الصيدلية؟"),
        [("Çok lezzetli!", "لذيذ جدًا!"), ("Bugün cuma.", "اليوم الجمعة.")], "اسأل عن مكان الصيدلية."),
   turn("Düz gidin, sonra sağa dönün.", "اذهب مباشرة، ثم انعطف يمينًا.", ("Uzak mı?", "هل هي بعيدة؟"),
        [("Afiyet olsun.", "بالهناء."), ("Ben yirmi yaşındayım.", "عمري عشرون سنة.")], "اسأل عن المسافة."),
   turn("Hayır, çok yakın. Beş dakika.", "لا، قريبة جدًا. خمس دقائق.", ("Çok teşekkür ederim!", "شكرًا جزيلًا!"),
        [("Hesap, lütfen.", "الحساب من فضلك."), ("Kahve içmiyorum.", "لا أشرب القهوة.")], "اشكره."),
   turn("Rica ederim. İyi günler!", "عفوًا. يومًا سعيدًا!", ("İyi günler!", "يومًا سعيدًا!"),
        [("Bir bilet istiyorum.", "أريد تذكرة."), ("Pazartesi.", "الإثنين.")], "ردّ على التحية."),
 ]},
 {"id": "c_shopping", "titleAr": "في السوق", "image": "emoji:🛒", "turns": [
   turn("Merhaba, buyurun.", "مرحبًا، تفضّل.", ("Merhaba, bu elma ne kadar?", "مرحبًا، كم سعر هذا التفاح؟"),
        [("İyi akşamlar, hesap.", "مساء الخير، الحساب."), ("Yarın görüşürüz.", "نلتقي غدًا.")], "اسأل عن سعر التفاح."),
   turn("Kilosu kırk lira.", "الكيلو بأربعين ليرة.", ("Biraz pahalı. Daha ucuz var mı?", "غالٍ قليلًا. هل يوجد أرخص؟"),
        [("Benim adım Ali.", "اسمي علي."), ("Kar yağıyor.", "الثلج يتساقط.")], "السعر مرتفع قليلًا."),
   turn("Bu armut daha ucuz, otuz lira.", "هذا الإجاص أرخص، ثلاثون ليرة.", ("Tamam, bir kilo alıyorum.", "حسنًا، سآخذ كيلو واحدًا."),
        [("Sola dönün.", "انعطف يسارًا."), ("Hoş bulduk.", "أهلًا بك.")], "وافق على الشراء."),
   turn("Kartla mı, nakit mi?", "بالبطاقة أم نقدًا؟", ("Kartla ödeyebilir miyim?", "هل يمكنني الدفع بالبطاقة؟"),
        [("Ben öğrenciyim.", "أنا طالب."), ("Gece.", "ليل.")], "يسأل عن طريقة الدفع."),
 ]},
]}

# ---------------------------------------------------------------- curriculum
def wid(cat): return [f"w_{r[0]}" for r in W[cat]]
def sid(cat): return [f"s_{r[0]}" for r in S[cat]]
def lesson(id, title, desc, typ, minutes, items=None, conv=None):
    return {"id": id, "title": title, "description": desc, "type": typ, "estimatedMinutes": minutes,
            "items": items or [], "conversationId": conv}

fw = wid("first_words")
ex = wid("expressions")
cat_title = {c[0]: (c[1], c[3]) for c in CATS}
everyday_lessons = []
for c in [c[0] for c in CATS if c[4] == "EVERYDAY"]:
    ws = wid(c)
    chunks = [ws] if len(ws) <= 8 else [ws[:5], ws[5:]]
    for n, ch in enumerate(chunks):
        suffix = "" if len(chunks) == 1 else f" ({n+1})"
        everyday_lessons.append(lesson(f"ed_{c}_{n+1}", cat_title[c][0] + suffix, "كلمات مصوّرة", "VOCABULARY", 5, ch))

curriculum = {"contentVersion": VERSION, "levels": [
 {"id": "lvl_0", "code": "L0", "title": "المستوى 0 — البداية", "description": "الحروف وأول الكلمات والعبارات", "sortOrder": 0, "isUnlocked": True, "modules": [
   {"id": "m_alphabet", "title": "الحروف التركية", "description": "29 حرفًا مع الصوت والصورة", "image": "emoji:🔤", "estimatedMinutes": 30, "lessons": [
     lesson("alpha_0", "استمع إلى كل الحروف", "الخطوة الأولى: 29 حرفًا بالصوت والصورة", "ALPHABET_INTRO", 6,
            [l[0] for l in L]),
     lesson("alpha_1", "A B C Ç D E", "أول ستة حروف", "ALPHABET", 5, ["l_a","l_b","l_c","l_ch","l_d","l_e"]),
     lesson("alpha_2", "F G Ğ H I İ", "انتبه لحرفي I و İ", "ALPHABET", 5, ["l_f","l_g","l_gh","l_h","l_i_dotless","l_i"]),
     lesson("alpha_3", "J K L M N O Ö", "سبعة حروف", "ALPHABET", 5, ["l_j","l_k","l_l","l_m","l_n","l_o","l_oe"]),
     lesson("alpha_4", "P R S Ş T U", "ستة حروف", "ALPHABET", 5, ["l_p","l_r","l_s","l_sh","l_t","l_u"]),
     lesson("alpha_5", "Ü V Y Z", "آخر الحروف", "ALPHABET", 4, ["l_ue","l_v","l_y","l_z"]),
     lesson("alpha_special", "الحروف المميزة", "Ç Ş Ğ I İ Ö Ü", "ALPHABET", 6, ["l_ch","l_sh","l_gh","l_i_dotless","l_i","l_oe","l_ue"]),
   ]},
   {"id": "m_first_words", "title": "أول 30 كلمة", "description": "أهم الكلمات للبداية", "image": "emoji:⭐", "estimatedMinutes": 25, "lessons": [
     lesson(f"fw_{i+1}", f"الكلمات {i*6+1}–{i*6+6}", "ست كلمات مصوّرة", "VOCABULARY", 5, fw[i*6:i*6+6]) for i in range(5)
   ]},
   {"id": "m_expressions", "title": "عبارات أساسية", "description": "التحية والشكر والمساعدة", "image": "emoji:💬", "estimatedMinutes": 20, "lessons": [
     lesson("ex_1", "التحية والوداع", "مرحبًا ومع السلامة", "EXPRESSIONS", 5, ex[0:5]),
     lesson("ex_2", "كلمات مهذبة", "شكرًا ومن فضلك", "EXPRESSIONS", 5, ex[5:9]),
     lesson("ex_3", "عن نفسي", "كيف حالك؟ واسمي…", "EXPRESSIONS", 5, ex[9:13]),
     lesson("ex_4", "عندما لا أفهم", "اطلب الإعادة", "EXPRESSIONS", 5, ex[13:16]),
   ]},
   {"id": "m_everyday", "title": "مفردات الحياة اليومية", "description": "20 مجموعة مصوّرة", "image": "emoji:🏡", "estimatedMinutes": 110,
    "lessons": everyday_lessons},
 ]},
 {"id": "lvl_a1", "code": "A1", "title": "المستوى A1 — مبتدئ", "description": "جمل بسيطة لمواقف يومية", "sortOrder": 1, "isUnlocked": False, "modules": [
   {"id": "m_a1_situations", "title": "مواقف يومية", "description": "جمل قصيرة ومفيدة", "image": "emoji:🗣️", "estimatedMinutes": 45, "lessons": [
     lesson(f"les_{c}", cat_title[c][0], "جمل قصيرة", "SENTENCES", 6, sid(c)) for c in
     ["a1_intro","a1_questions","a1_people","a1_food","a1_shopping","a1_directions","a1_routine","a1_negation"]
   ]},
   {"id": "m_a1_conversations", "title": "محادثات قصيرة", "description": "اختر الرد المناسب", "image": "emoji:💬", "estimatedMinutes": 20, "lessons": [
     lesson("conv_greeting", "التعارف", "محادثة", "CONVERSATION", 4, conv="c_greeting"),
     lesson("conv_cafe", "في المقهى", "محادثة", "CONVERSATION", 4, conv="c_cafe"),
     lesson("conv_directions", "السؤال عن الطريق", "محادثة", "CONVERSATION", 4, conv="c_directions"),
     lesson("conv_shopping", "في السوق", "محادثة", "CONVERSATION", 4, conv="c_shopping"),
   ]},
 ]},
 {"id": "lvl_a2", "code": "A2", "title": "المستوى A2 — أساسي", "description": "الماضي والمستقبل والمواعيد والسفر", "sortOrder": 2, "isUnlocked": False, "modules": [
   {"id": "m_a2_life", "title": "حياتي وخططي", "description": "جمل أطول قليلًا", "image": "emoji:🧳", "estimatedMinutes": 30, "lessons": [
     lesson(f"les_{c}", cat_title[c][0], "جمل", "SENTENCES", 7, sid(c)) for c in ["a2_past","a2_future","a2_appointments","a2_travel"]
   ]},
 ]},
 {"id": "lvl_b1", "code": "B1", "title": "المستوى B1 — متوسط", "description": "قريبًا في تحديث قادم", "sortOrder": 3, "isUnlocked": False, "modules": []},
]}

os.makedirs(OUT, exist_ok=True)
for name, data in [("alphabet", alphabet), ("vocabulary", vocabulary), ("sentences", sentences), ("conversations", conversations), ("lessons", curriculum)]:
    with open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
print("items", len(items), "letters", len(L), "lessons", sum(len(m["lessons"]) for lv in curriculum["levels"] for m in lv["modules"]))
