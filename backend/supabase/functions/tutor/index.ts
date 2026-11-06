// Türkçe Kolay — AI tutor endpoint (Supabase Edge Function, Deno).
//
// The Android app sends POST { messages, mode, learnerLevel, knownWords } here.
// The provider API key lives ONLY in this function's secrets:
//   supabase secrets set AI_API_KEY=... TUTOR_MODEL=claude-sonnet-5-5
//   supabase functions deploy tutor
// Then build the app with:  ./gradlew assembleRelease -PtutorBackendUrl=https://<project>.functions.supabase.co/
//
// Swap the provider by changing callModel(); the app's AiProvider contract stays the same.

const SYSTEM_PROMPT = `
أنت «كولاي»، معلّم صبور ولطيف للغة التركية لمتعلّمين عرب مبتدئين تمامًا،
ومنهم من يجد صعوبة في الحفظ أو القراءة أو معالجة المعلومات.

القواعد:
- اكتب بالعربية الفصحى البسيطة. الجمل قصيرة. لا تستخدم مصطلحات نحوية معقّدة.
- استخدم تركية تناسب مستوى المتعلّم ({LEVEL}). فضّل هذه الكلمات المعروفة له: {KNOWN}.
- قدّم معلومة جديدة واحدة أو اثنتين فقط في كل رد.
- عند التصحيح: ابدأ بما هو صحيح، ثم صحّح بلطف، ثم أعطِ الجملة الصحيحة كاملة.
- فرّق بين الترجمة الحرفية والتعبير التركي الطبيعي عندما يكون ذلك مفيدًا.
- لا تخترع قواعد ولا معاني كلمات. إن لم تكن متأكدًا فقل ذلك بوضوح.
- اكتب التركية بحروفها الصحيحة (ç ş ğ ı İ ö ü). لا تكتب التركية بحروف عربية.
- لا تدّعِ أنك تستطيع تقييم النطق.
- الرد قصير: من 2 إلى 6 أسطر.
`;

const MODE_HINTS: Record<string, string> = {
  FREE_CHAT: "أجرِ محادثة تركية بسيطة جدًا، سطرًا واحدًا بالتركية ثم ترجمته.",
  EXPLAIN_WORD: "اشرح الكلمة: المعنى، مثال قصير، وملاحظة نطق إن لزم.",
  EXPLAIN_SENTENCE: "اشرح الجملة كلمة بكلمة باختصار ثم المعنى العام.",
  CORRECT_SENTENCE: "صحّح جملة المتعلّم بلطف واشرح الخطأ في سطر واحد.",
  QUIZ: "أعطِ 3 أسئلة اختيار قصيرة على كلماته المعروفة، والإجابات في النهاية.",
  LISTENING_PRACTICE: "اقترح جملة تركية قصيرة ليستمع إليها في التطبيق ويكرّرها.",
  SUGGEST_REVIEW: "اقترح نشاط مراجعة قصيرًا مناسبًا لمستواه.",
};

interface WireMessage { role: "user" | "assistant"; text: string }
interface TutorRequest { messages: WireMessage[]; mode: string; learnerLevel: string; knownWords: string[] }

async function callModel(system: string, messages: WireMessage[]): Promise<string> {
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": Deno.env.get("AI_API_KEY") ?? "",
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model: Deno.env.get("TUTOR_MODEL") ?? "claude-sonnet-5-5",
      max_tokens: 600,
      system,
      messages: messages.map((m) => ({ role: m.role, content: m.text })),
    }),
  });
  if (!res.ok) throw new Error(`provider ${res.status}`);
  const data = await res.json();
  return (data.content ?? []).map((c: { text?: string }) => c.text ?? "").join("").trim();
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });
  try {
    const body = (await req.json()) as TutorRequest;
    const messages = (body.messages ?? []).slice(-12).filter((m) => m.text?.trim());
    if (messages.length === 0 || messages[messages.length - 1].role !== "user") {
      return Response.json({ error: "last message must be from the learner" }, { status: 400 });
    }
    const system = SYSTEM_PROMPT
      .replace("{LEVEL}", body.learnerLevel ?? "L0")
      .replace("{KNOWN}", (body.knownWords ?? []).slice(0, 60).join("، ") || "لا شيء بعد")
      + "\n" + (MODE_HINTS[body.mode] ?? MODE_HINTS.FREE_CHAT);
    const reply = await callModel(system, messages);
    return Response.json({ reply });
  } catch (_e) {
    return Response.json({ error: "tutor unavailable" }, { status: 502 });
  }
});
