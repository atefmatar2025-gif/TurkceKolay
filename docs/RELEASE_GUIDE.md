# دليل الإصدار

## نسخة تجريبية (Debug APK)

كل دفع إلى `main` يبني `app-debug.apk` على GitHub Actions:
**Actions ← أحدث تشغيل ← Artifacts ← TurkceKolay-debug-apk**. نزّله على الهاتف وثبّته (اسمح بالتثبيت من مصادر غير معروفة).

محليًا (مع Android Studio أو Android SDK):

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## نسخة نهائية موقّعة (Release APK)

1. أنشئ مفتاح توقيع مرة واحدة واحفظه خارج المستودع:
   ```bash
   keytool -genkey -v -keystore turkcekolay.jks -keyalg RSA -keysize 4096 -validity 10000 -alias turkcekolay
   ```
2. أضف إلى `~/.gradle/gradle.properties` (وليس إلى المستودع):
   ```
   TK_KEYSTORE=/path/turkcekolay.jks
   TK_KEYSTORE_PASSWORD=...
   TK_KEY_ALIAS=turkcekolay
   TK_KEY_PASSWORD=...
   ```
3. أضف `signingConfigs` في `app/build.gradle.kts` تقرأ هذه القيم (أو استخدم Android Studio ← Generate Signed APK).
4. `./gradlew assembleRelease`
5. للـCI: ضع المفتاح بصيغة base64 وكلمات المرور في GitHub Secrets، ولا تضعها في الكود أبدًا.

**لا تفقد ملف المفتاح:** بدونه لا يمكن تحديث التطبيق المثبّت.

## تفعيل المعلّم الذكي

1. انشر `backend/supabase/functions/tutor` (التعليمات في رأس الملف). المفتاح السري يبقى في أسرار Supabase فقط.
2. ابنِ التطبيق مع رابط الخادم العام:
   ```bash
   ./gradlew assembleRelease -PtutorBackendUrl=https://<project>.functions.supabase.co/
   ```
   الرابط عنوان عام وليس سرًا. بدون الرابط يعمل التطبيق كاملًا ويعرض المعلّم «غير مفعّل».

## قبل النشر

- ارفع `versionCode` و`versionName`.
- نفّذ خطة الاختبار اليدوية في `TEST_PLAN.md`.
- لا تَعِد بشهادة رسمية؛ CEFR مرجع للمنهج فقط.
