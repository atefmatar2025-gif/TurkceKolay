# البنية المعمارية

```
UI (Jetpack Compose, RTL)  ──►  ViewModel (Hilt)  ──►  Domain (Kotlin نقي)  ──►  Data
                                                          │                      ├─ Room  (المحتوى + التقدّم)
                                                          │                      ├─ DataStore (التفضيلات)
                                                          │                      └─ assets/content/*.json
                                                          ├─ SpacedRepetitionScheduler
                                                          ├─ ExerciseGenerator
                                                          ├─ ExerciseSessionEngine
                                                          └─ Progression (فتح المستويات، السلسلة، الإنجازات)
Audio: AudioManager → AudioRepository (تسجيل محمّل/مخزّن/مضمّن) → RecordedAudioPlayer (Media3) | TurkishTtsEngine
AI:    AiTutorRepository → AiProvider (BackendAiProvider | NotConfiguredAiProvider) → خادمنا → مزوّد الذكاء الاصطناعي
Work:  ReviewReminderWorker (WorkManager، يوميًا 18:00 إن وُجدت مراجعة)
```

## الحزم

| الحزمة | المسؤولية |
|---|---|
| `core/designsystem` | الألوان، الخطوط، المكوّنات القابلة لإعادة الاستخدام، الشخصية «كولاي» |
| `core/navigation` | المسارات وشريط التنقل السفلي |
| `core/common` | `Clock` القابل للحقن، نطاق التطبيق |
| `data/local` | كيانات Room وDAO وقاعدة البيانات |
| `data/content` | نماذج JSON، القراءة من الأصول، `ContentSeeder`، المحوّلات |
| `data/preferences` | DataStore |
| `data/repository` | تنفيذ واجهات المستودعات |
| `domain/model` | نماذج المجال والتمارين |
| `domain/learning` | كل منطق التعلّم — بلا Android، مغطّى بالاختبارات |
| `domain/repository` | واجهات المستودعات |
| `features/*` | شاشة + ViewModel لكل ميزة |
| `audio`, `ai`, `work`, `di` | كما في المخطط |

## قرارات أساسية

1. **دون تسجيل إلزامي:** ملف شخصي محلي واحد (`user_profile.id = 1`).
2. **أوفلاين أولًا:** كل الدروس والتمارين والمراجعة تعمل دون إنترنت. الإنترنت للمعلّم الذكي فقط.
3. **فصل المحتوى عن التقدّم:** جداول المحتوى تُحدَّث بـ`@Upsert` عند ارتفاع `contentVersion`؛ جداول التقدّم لا ترتبط بمفاتيح أجنبية بالمحتوى ولا تُمس.
4. **RTL:** الواجهة كلها من اليمين لليسار بغض النظر عن لغة الجهاز. النص التركي يُعرض دائمًا LTR عبر `TurkishStyle`، وصفوف ترتيب الحروف LTR.
5. **قواعد التركية:** كل تحويل لحالة الأحرف يستخدم `Locale("tr-TR")` (I↔ı و İ↔i).
6. **التمارين:** أول محاولة فقط تحرّك جدول المراجعة. الخطأ يعيد العنصر مرة واحدة بنوع تمرين مختلف. لا خصم ولا «قلوب».
7. **الحتمية:** المولّد يأخذ `Random(seed)` والمجدول يأخذ الوقت كمُدخل، فالسلوك قابل للتكرار في الاختبارات.

## إضافة محتوى أو ميزة

- **درس/كلمات جديدة:** عدّل ملفات JSON وارفع `contentVersion` في الملفات الخمسة (انظر `CONTENT_GUIDE.md`).
- **نوع تمرين جديد:** أضف قيمة إلى `ExerciseType`، ونموذجًا في `Exercises.kt`، ومُنشئًا في `ExerciseGenerator`، وعرضًا في `ExerciseViews.kt`.
- **مزوّد ذكاء اصطناعي آخر:** نفّذ `AiProvider` أو بدّل `callModel()` في الخادم؛ الواجهة لا تتغير.
- **المحادثة الصوتية لاحقًا:** `ConversationManager` نصي ومستقل؛ تُضاف طبقة تحويل كلام إلى نص قبله وTTS بعده.

## «تعلّم من حولك» (مستقبلًا)

الواجهة `domain/learning/ObjectRecognizer` جاهزة. الشروط: طلب إذن الكاميرا عند فتح الميزة فقط، وتأكيد المتعلّم للكائن المكتشف قبل حفظه، وربط النتيجة بمعرّف من القاموس المحلي عند وجوده. لا تُفعّل قبل استقرار النواة.
