#!/usr/bin/env python3
"""Collect candidate photos and native-speaker recordings from Wikimedia (runs in CI).

Images: lead photo of an English Wikipedia article + top Commons search hits, freely licensed only.
Audio: Lingua Libre recordings of Turkish words (File:LL-Q256 (tur)-<speaker>-<word>.wav).

Output (in --out):
  images/<concept>_<k>.jpg, images.json   candidates to review (k = 0,1,2)
  audio/<key>.mp3, audio.json              normalised mono MP3 recordings
"""
import argparse, io, json, os, re, subprocess, sys, time, urllib.parse, urllib.request
from collections import Counter

sys.path.insert(0, os.path.dirname(__file__))
from manifest import IMAGES, pronunciation_words  # noqa: E402

UA = "TurkceKolayAssetBot/0.2 (https://github.com/atefmatar2025-gif/TurkceKolay; educational app)"
COMMONS = "https://commons.wikimedia.org/w/api.php"
ENWIKI = "https://en.wikipedia.org/w/api.php"
FREE = re.compile(r"^(cc0|public domain|pd|cc by(-sa)?( \d\.\d)?|cc-by(-sa)?-\d\.\d|attribution|fal)", re.I)


def get(url, params=None, binary=False, tries=4):
    if params:
        url = url + "?" + urllib.parse.urlencode({**params, "format": "json", "formatversion": "2"})
    for attempt in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            time.sleep(0.15)
            return data if binary else json.loads(data)
        except Exception as e:  # noqa: BLE001
            print("  retry", attempt, url[:120], e, file=sys.stderr)
            time.sleep(2 + attempt * 3)
    return None


def tr_lower(s):
    return s.replace("I", "ı").replace("İ", "i").lower()


def strip_html(s):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", s or "")).strip()


def imageinfo(titles, width=None):
    out = {}
    titles = list(dict.fromkeys(titles))
    for i in range(0, len(titles), 40):
        params = {"action": "query", "prop": "imageinfo", "titles": "|".join(titles[i:i + 40]),
                  "iiprop": "url|extmetadata|mime|size"}
        if width:
            params["iiurlwidth"] = width
        d = get(COMMONS, params)
        for p in (d or {}).get("query", {}).get("pages", []):
            ii = (p.get("imageinfo") or [None])[0]
            if not ii or p.get("missing"):
                continue
            meta = ii.get("extmetadata", {})
            lic = strip_html(meta.get("LicenseShortName", {}).get("value"))
            out[p["title"]] = {
                "title": p["title"],
                "url": ii.get("thumburl") or ii.get("url"),
                "original": ii.get("url"),
                "mime": ii.get("mime"),
                "width": ii.get("width"), "height": ii.get("height"),
                "license": lic,
                "licenseUrl": strip_html(meta.get("LicenseUrl", {}).get("value")),
                "artist": strip_html(meta.get("Artist", {}).get("value"))[:200],
                "page": ii.get("descriptionurl"),
                "free": bool(FREE.match(lic or "")),
            }
    return out


def fetch_images(out):
    os.makedirs(os.path.join(out, "images"), exist_ok=True)
    from PIL import Image

    # 1. Wikipedia lead images
    wiki_titles = {c: w for c, (w, _) in IMAGES.items() if w}
    lead = {}
    names = list(set(wiki_titles.values()))
    for i in range(0, len(names), 40):
        d = get(ENWIKI, {"action": "query", "prop": "pageimages", "piprop": "name", "redirects": 1,
                         "titles": "|".join(names[i:i + 40])})
        q = (d or {}).get("query", {})
        redirect = {r["from"]: r["to"] for r in q.get("redirects", [])}
        norm = {n["from"]: n["to"] for n in q.get("normalized", [])}
        found = {p["title"]: p.get("pageimage") for p in q.get("pages", [])}
        for n in names[i:i + 40]:
            t = redirect.get(norm.get(n, n), norm.get(n, n))
            if found.get(t):
                lead[n] = "File:" + found[t]

    # 2. Commons search hits
    hits = {}
    for c, (_, query) in IMAGES.items():
        if not query:
            continue
        d = get(COMMONS, {"action": "query", "list": "search", "srnamespace": 6, "srlimit": 8,
                          "srsearch": f"{query} filetype:bitmap"})
        hits[c] = [r["title"] for r in (d or {}).get("query", {}).get("search", [])]

    # 3. Metadata, licence filter, download
    wanted = []
    for c, (w, _) in IMAGES.items():
        if w and w in lead:
            wanted.append(lead[w])
        wanted += hits.get(c, [])
    info = imageinfo(wanted, width=480)

    result = {}
    for c, (w, _) in IMAGES.items():
        cands = ([lead[w]] if w and w in lead else []) + hits.get(c, [])
        chosen = []
        for t in cands:
            m = info.get(t)
            if not m or not m["free"] or m["mime"] not in ("image/jpeg", "image/png", "image/webp"):
                continue
            if any(x["title"] == t for x in chosen):
                continue
            if (m["width"] or 0) < 300:
                continue
            chosen.append(m)
            if len(chosen) == 3:
                break
        result[c] = []
        for k, m in enumerate(chosen):
            data = get(m["url"], binary=True)
            if not data:
                continue
            try:
                im = Image.open(io.BytesIO(data)).convert("RGB")
                im.thumbnail((480, 480))
                im.save(os.path.join(out, "images", f"{c}_{k}.jpg"), quality=85)
                result[c].append({**m, "k": k})
            except Exception as e:  # noqa: BLE001
                print("  bad image", c, e, file=sys.stderr)
        print(f"{c}: {len(result[c])} candidates", flush=True)
    with open(os.path.join(out, "images.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)


LL = re.compile(r"^File:LL-Q256 \(tur\)-(.+?)-(.+)\.(wav|ogg|flac)$")


def fetch_audio(out, content):
    os.makedirs(os.path.join(out, "audio"), exist_ok=True)
    vocab = json.load(open(os.path.join(content, "vocabulary.json"), encoding="utf-8"))["items"]
    letters = json.load(open(os.path.join(content, "alphabet.json"), encoding="utf-8"))["letters"]
    words = pronunciation_words(vocab, letters)

    matches = {}  # key -> [(speaker, title)]
    for key, word in words.items():
        d = get(COMMONS, {"action": "query", "list": "search", "srnamespace": 6, "srlimit": 50,
                          "srsearch": f'intitle:"LL-Q256 (tur)" intitle:"{word}"'})
        found = []
        for r in (d or {}).get("query", {}).get("search", []):
            m = LL.match(r["title"])
            if m and tr_lower(m.group(2)).strip() == tr_lower(word).strip():
                found.append((m.group(1), r["title"]))
        matches[key] = found
    speakers = Counter(s for f in matches.values() for s, _ in f)
    print("speakers:", speakers.most_common(8), flush=True)

    titles = []
    pick = {}
    for key, found in matches.items():
        if found:
            best = max(found, key=lambda st: speakers[st[0]])
            pick[key] = best
            titles.append(best[1])
    info = imageinfo(titles)
    result = {}
    for key, (speaker, title) in pick.items():
        m = info.get(title)
        if not m or not m["free"]:
            continue
        data = get(m["original"], binary=True)
        if not data:
            continue
        src = os.path.join(out, "tmp_audio")
        with open(src, "wb") as f:
            f.write(data)
        dst = os.path.join(out, "audio", f"{key}.mp3")
        af = ("silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
              "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
              "loudnorm=I=-16:TP=-1.5:LRA=11,adelay=150,apad=pad_dur=0.3")
        r = subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", src, "-af", af,
                            "-ac", "1", "-ar", "44100", "-b:a", "64k", dst])
        if r.returncode == 0:
            result[key] = {"word": words[key], "speaker": speaker, **m}
        os.remove(src)
    print(f"audio: {len(result)} of {len(words)} words have native recordings", flush=True)
    with open(os.path.join(out, "audio.json"), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--content", required=True)
    a = ap.parse_args()
    fetch_audio(a.out, a.content)
    fetch_images(a.out)
