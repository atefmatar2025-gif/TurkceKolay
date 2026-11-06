# مخطط قاعدة البيانات (Room، الإصدار 1)

الملف: `turkce_kolay.db`. المخطط المُصدَّر يُكتب تلقائيًا في `app/schemas/` عند البناء.

## جداول المحتوى (تُملأ من assets)

| الجدول | الحقول | ملاحظات |
|---|---|---|
| `learning_level` | id, code, title, description, sortOrder, isUnlocked | `isUnlocked` قيمة افتراضية؛ الفتح الفعلي يُحسب من التقدّم |
| `learning_module` | id, levelId→level, title, description, imagePath, sortOrder, estimatedMinutes, isUnlocked | FK مع CASCADE |
| `lesson` | id, moduleId→module, title, description, lessonType, sortOrder, estimatedMinutes, itemIds (JSON), conversationId | |
| `vocabulary_category` | id, titleArabic, titleTurkish, imagePath, sortOrder, groupName | المجموعات: ALPHABET, FIRST_WORDS, EXPRESSIONS, EVERYDAY, SENTENCES |
| `vocabulary` | id, kind, turkishWord, arabicMeaning, categoryId→category, imagePath, audioPath, audioText, exampleTurkish, exampleArabic, note, hintArabic, difficulty, sortOrder, contentVersion | الحروف والكلمات والعبارات والجمل في جدول واحد يميّزها `kind` |

## جداول المتعلّم (لا تُمس عند تحديث المحتوى)

| الجدول | الحقول |
|---|---|
| `user_profile` | id (=1), displayName, nativeLanguage, dailyGoalMinutes, quietModeEnabled, createdAt, updatedAt |
| `user_vocabulary_progress` | id, vocabularyId (فريد), learningState, repetitions, correctCount, incorrectCount, lapses, confidence, lastExerciseType, lastReviewedAt, nextReviewAt (مفهرس), updatedAt |
| `lesson_progress` | id, lessonId (فريد), status, score, completedAt, timeSpentSeconds |
| `exercise_attempt` | id, exerciseId, exerciseType, lessonId, vocabularyId, selectedAnswer, isCorrect, timeSpentSeconds, createdAt |
| `daily_activity` | id, activityDate (فريد، ISO), xpEarned, learningMinutes, learningSeconds, completedLessons, reviewedWords |
| `favorite_vocabulary` | id, vocabularyId (فريد), createdAt |

### إضافات على المواصفة الأصلية ولماذا

- `lapses` و`lastExerciseType` في التقدّم: لاكتشاف «النسيان المتكرر» وتغيير نوع السؤال.
- `kind`, `audioText`, `note`, `hintArabic`, `sortOrder` في المفردات: لدعم الحروف ونطق TTS الصحيح لأسماء الحروف.
- `learningSeconds` في النشاط اليومي: دقة أعلى من الدقائق (تُشتق منها `learningMinutes`).
- `exerciseType` و`vocabularyId` في المحاولات: للتحليل لاحقًا.

## الترحيل

`fallbackToDestructiveMigration` **غير مفعّل عمدًا**. أي تغيير في المخطط = رفع `version` + `Migration` + اختبار ترحيل.
