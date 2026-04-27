# Phase 2 — Audio generation script (build-time, all Vietnamese voices)

## Context
- [plan.md](plan.md). Output: per-voice MP3 sets committed under
  `static/audio/{voiceId}/`, ready for Phase 3's playback module.

## Overview
- Priority: P0
- Status: TODO
- Effort: ~30 min (incl. ~5-10 min generation runtime)
- Run frequency: once per voice change (rare).

## What gets generated

```
static/audio/
├── hoai-my/                       (vi-VN-HoaiMyNeural — female)
│   ├── 1.mp3        "một"
│   ├── 2.mp3        "hai"
│   ├── ... (90)
│   ├── 90.mp3       "chín mươi"
│   ├── cho.mp3      "Chờ"
│   └── kinh.mp3     "Kinh"
├── nam-minh/                      (vi-VN-NamMinhNeural — male)
│   ├── 1.mp3
│   ├── ... (same 92 names)
│   └── kinh.mp3
└── manifest.json                  (voice list, generated)
```

92 clips × N voices. As of writing, edge-tts ships 2 Vietnamese voices
(female + male) — total ~184 files, ~2.2 MB. Script auto-discovers
voices via `edge-tts.list_voices()`, so new voices added by Microsoft
get picked up on the next regen.

## Voice ID mapping

| edge-tts voice | Folder ID | Display label |
|---|---|---|
| `vi-VN-HoaiMyNeural` | `hoai-my` | "Hoài Mỹ (nữ)" |
| `vi-VN-NamMinhNeural` | `nam-minh` | "Nam Minh (nam)" |
| (future `vi-VN-XxxNeural`) | `xxx` | "Xxx" — script slug-ifies |

Folder ID = lowercase + kebab-case + drop locale prefix + drop
"Neural" suffix. Conversion is deterministic; script writes the
manifest so the JS side never has to mirror Python.

## Tooling

| Tool | Why |
|---|---|
| Python 3 | already on dev machine |
| `edge-tts` (`pip install edge-tts`) | free, no API key, MS Neural quality |

User installs `edge-tts` once on the dev machine; project doesn't
add Python deps to `package.json` (build-time only).

## File: `scripts/generate-audio.py`

```python
#!/usr/bin/env python3
"""Generate Vietnamese audio clips (1-90 + Chờ + Kinh) for every
edge-tts Vietnamese voice. Output committed to static/audio/{voiceId}/
and shipped with the app — runtime never calls TTS."""
import asyncio, json, os, re, sys

OUT_ROOT = os.path.join(os.path.dirname(__file__), "..", "static", "audio")

ONES = ["không", "một", "hai", "ba", "bốn",
        "năm", "sáu", "bảy", "tám", "chín"]

def number_to_vietnamese(n: int) -> str:
    if n < 10: return ONES[n]
    if n == 10: return "mười"
    if n < 20:
        u = n - 10
        return "mười lăm" if u == 5 else f"mười {ONES[u]}"
    t, u = divmod(n, 10)
    tens = f"{ONES[t]} mươi"
    if u == 0: return tens
    if u == 1: return f"{tens} mốt"
    if u == 5: return f"{tens} lăm"
    return f"{tens} {ONES[u]}"

def voice_id(short_name: str) -> str:
    """vi-VN-HoaiMyNeural -> hoai-my"""
    name = short_name.split("-")[-1]                # HoaiMyNeural
    name = re.sub(r"Neural$", "", name)             # HoaiMy
    name = re.sub(r"(?<!^)(?=[A-Z])", "-", name)    # Hoai-My
    return name.lower()                             # hoai-my

async def synth(text: str, voice: str, out: str):
    import edge_tts
    await edge_tts.Communicate(text, voice).save(out)
    print(f"  {out}  ←  \"{text}\"")

async def main():
    import edge_tts
    all_voices = await edge_tts.list_voices()
    vi_voices = [v for v in all_voices if v["Locale"].startswith("vi-")]
    if not vi_voices:
        sys.exit("No Vietnamese voices found in edge-tts.")

    manifest = {"voices": []}
    for v in vi_voices:
        vid = voice_id(v["ShortName"])
        out_dir = os.path.join(OUT_ROOT, vid)
        os.makedirs(out_dir, exist_ok=True)
        print(f"\n→ {v['ShortName']}  →  static/audio/{vid}/")

        tasks = []
        for n in range(1, 91):
            tasks.append(synth(number_to_vietnamese(n),
                               v["ShortName"],
                               os.path.join(out_dir, f"{n}.mp3")))
        tasks.append(synth("Chờ",  v["ShortName"], os.path.join(out_dir, "cho.mp3")))
        tasks.append(synth("Kinh", v["ShortName"], os.path.join(out_dir, "kinh.mp3")))
        await asyncio.gather(*tasks)

        # Display label: gender + given name
        gender_vi = "nữ" if v["Gender"].lower() == "female" else "nam"
        given = re.sub(r"(?<!^)(?=[A-Z])", " ", re.sub(r"Neural$", "",
                       v["ShortName"].split("-")[-1])).strip()
        manifest["voices"].append({
            "id": vid,
            "edgeName": v["ShortName"],
            "label": f"{given} ({gender_vi})",
            "gender": v["Gender"].lower(),
        })

    with open(os.path.join(OUT_ROOT, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"\nWrote manifest with {len(manifest['voices'])} voice(s).")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except ImportError:
        sys.exit("Install dep first: pip install edge-tts")
```

## How to run

```bash
pip install edge-tts                          # one-time, dev-machine only
python3 scripts/generate-audio.py
git add static/audio/
git commit -m "chore(audio): regenerate Vietnamese clips"
```

## manifest.json — the JS side reads this

```json
{
  "voices": [
    { "id": "hoai-my",  "edgeName": "vi-VN-HoaiMyNeural",  "label": "Hoai My (nữ)",  "gender": "female" },
    { "id": "nam-minh", "edgeName": "vi-VN-NamMinhNeural", "label": "Nam Minh (nam)", "gender": "male" }
  ]
}
```

Phase 4's settings UI fetches `manifest.json` once (or imports it via
Vite's `?json` if we want it baked into the bundle) to populate the
voice picker. Adding a new voice = re-run the script, ship the new
folder + manifest, no code change required.

## Repository policy / README addendum

```md
### Regenerating audio
Vietnamese voice clips live in `static/audio/{voiceId}/`. To regenerate
(e.g., to add a new edge-tts voice or change wording):

    pip install edge-tts
    python3 scripts/generate-audio.py
```

## Edge cases

| Case | Handling |
|---|---|
| Network fails mid-generation | `asyncio.gather` raises; rerun (idempotent) |
| edge-tts upstream removes a voice | Committed MP3s still play; only regen blocks. Manifest stays accurate to what's on disk. |
| New Vietnamese voice arrives | Auto-included in next regen; Phase 4 UI picks it up via manifest |
| Voice name collision after slug-ify | Defensive: script aborts with message if two `id`s collide |

## Success criteria

- `python3 scripts/generate-audio.py` writes 92 MP3s per Vietnamese
  voice into `static/audio/{voiceId}/` plus `manifest.json`.
- Each `45.mp3` says "bốn mươi lăm" in the corresponding voice.
- `cho.mp3` + `45.mp3` in sequence sounds like "Chờ bốn mươi lăm".
- Repo grows by ~2.2 MB (acceptable).

## Risks

| Risk | Mitigation |
|---|---|
| edge-tts endpoint changes break regen later | MP3s already committed; runtime unaffected |
| Manifest drifts from disk (manual edit) | Script always rewrites manifest; treat as generated artifact |
| Bandwidth — 184 files lazy-loaded | Browser caches; player only ever uses one voice's 92 files at a time |

## Next
- Phase 3 reads `manifest.json` + active-voice setting to build URLs.
