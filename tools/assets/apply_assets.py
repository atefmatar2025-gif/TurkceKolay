#!/usr/bin/env python3
"""Installs reviewed photos and native recordings into the app, with attribution.

Usage:
  apply_assets.py --audio <candidates dir with audio/ and audio.json>
  apply_assets.py --images <final dir with images/<concept>.jpg and images.json>

Writes:
  app/src/main/assets/audio/<folder>/<id>.mp3
  app/src/main/assets/images/vocab/<concept>.webp
  app/src/main/assets/credits.json      (shown in the app: Settings → مصادر الصور والأصوات)
  app/src/main/assets/images/ATTRIBUTION.md
"""
import argparse, json, os, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "app/src/main/assets")
CREDITS = os.path.join(ASSETS, "credits.json")


def load_credits():
    if os.path.exists(CREDITS):
        return json.load(open(CREDITS, encoding="utf-8"))
    return {"images": [], "audio": []}


def save_credits(c):
    c["images"].sort(key=lambda x: x["item"])
    c["audio"].sort(key=lambda x: x["item"])
    with open(CREDITS, "w", encoding="utf-8") as f:
        json.dump(c, f, ensure_ascii=False, indent=1)
    lines = ["# Image and audio credits", "",
             "Photos: Wikimedia Commons. Recordings: Lingua Libre (Wikimedia Commons).",
             "Each file keeps its original licence; resized/re-encoded copies are shared under the same licence.", ""]
    for kind in ("images", "audio"):
        lines.append(f"## {kind}")
        for e in c[kind]:
            lines.append(f"- **{e['item']}** — [{e['title']}]({e['page']}) · {e['author'] or 'unknown'} · {e['license']}")
        lines.append("")
    with open(os.path.join(ASSETS, "images/ATTRIBUTION.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def entry(item, m):
    return {"item": item, "title": m["title"].removeprefix("File:"), "author": m.get("artist", ""),
            "license": m.get("license", ""), "licenseUrl": m.get("licenseUrl", ""), "page": m.get("page", "")}


def apply_audio(src):
    meta = json.load(open(os.path.join(src, "audio.json"), encoding="utf-8"))
    vocab = json.load(open(os.path.join(ASSETS, "content/vocabulary.json"), encoding="utf-8"))["items"]
    kind = {i["id"]: i["kind"] for i in vocab}
    credits = load_credits()
    credits["audio"] = []
    for key, m in meta.items():
        if key.endswith("_ex"):
            folder = "sentences"          # letters: AudioRef("<letterId>_ex", "sentences")
        elif kind.get(key) == "PHRASE":
            folder = "greetings"
        else:
            folder = "vocabulary"
        dst = os.path.join(ASSETS, "audio", folder)
        os.makedirs(dst, exist_ok=True)
        shutil.copy(os.path.join(src, "audio", key + ".mp3"), os.path.join(dst, key + ".mp3"))
        credits["audio"].append({**entry(key, m), "speaker": m.get("speaker", "")})
    save_credits(credits)
    print("audio installed:", len(meta))


def apply_images(src):
    from PIL import Image
    meta = json.load(open(os.path.join(src, "images.json"), encoding="utf-8"))
    out = os.path.join(ASSETS, "images/vocab")
    os.makedirs(out, exist_ok=True)
    credits = load_credits()
    credits["images"] = []
    for concept, m in meta.items():
        im = Image.open(os.path.join(src, "images", concept + ".jpg")).convert("RGB")
        im.thumbnail((720, 720))
        im.save(os.path.join(out, concept + ".webp"), quality=74, method=6)
        credits["images"].append(entry(concept, m))
    save_credits(credits)
    print("images installed:", len(meta))


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--audio")
    ap.add_argument("--images")
    a = ap.parse_args()
    if a.audio:
        apply_audio(a.audio)
    if a.images:
        apply_images(a.images)
