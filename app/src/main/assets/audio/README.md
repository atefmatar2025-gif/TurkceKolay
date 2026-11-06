# Native recordings

Put native-speaker recordings here. The app picks them up automatically — no code change.

```
audio/
  alphabet/       l_a.mp3, l_ch.mp3, …          (letters, by id)
  vocabulary/     w_elma.mp3, w_su.mp3, …       (words)
  greetings/      w_merhaba… phrases (kind PHRASE)
  sentences/      s_bu_ne.mp3 and example sentences: <itemId>_ex.mp3
  conversations/  c_cafe_0.mp3 (turn), c_cafe_0_r0.mp3 (reply choice)
```

File name = content id + `.mp3` (or set `"audio": "file.mp3"` on the item in the content JSON).
Until a recording exists, the app falls back to Android's Turkish TTS. See docs/AUDIO_GUIDE.md.
