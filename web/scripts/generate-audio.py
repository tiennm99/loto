#!/usr/bin/env python3
"""Generate Vietnamese audio clips (1-90 + Chờ + Kinh) for every edge-tts
Vietnamese voice. Output written to static/audio/{voiceId}/ and shipped
with the app — runtime never calls TTS.

Run once on a dev machine:

    pip install edge-tts
    python3 scripts/generate-audio.py
"""
import asyncio
import json
import os
import re
import sys

OUT_ROOT = os.path.join(os.path.dirname(__file__), "..", "static", "audio")

ONES = [
    "không", "một", "hai", "ba", "bốn",
    "năm", "sáu", "bảy", "tám", "chín",
]


def number_to_vietnamese(n: int) -> str:
    """Mirror of src/lib/vietnamese-number.js for build-time use."""
    if n < 10:
        return ONES[n]
    if n == 10:
        return "mười"
    if n < 20:
        u = n - 10
        return "mười lăm" if u == 5 else f"mười {ONES[u]}"
    t, u = divmod(n, 10)
    tens = f"{ONES[t]} mươi"
    if u == 0:
        return tens
    if u == 1:
        return f"{tens} mốt"
    if u == 5:
        return f"{tens} lăm"
    return f"{tens} {ONES[u]}"


def voice_id(short_name: str) -> str:
    """vi-VN-HoaiMyNeural -> hoai-my"""
    name = short_name.split("-")[-1]                 # HoaiMyNeural
    name = re.sub(r"Neural$", "", name)              # HoaiMy
    name = re.sub(r"(?<!^)(?=[A-Z])", "-", name)     # Hoai-My
    return name.lower()                              # hoai-my


def display_label(short_name: str, gender: str) -> str:
    given = re.sub(r"Neural$", "", short_name.split("-")[-1])
    given = re.sub(r"(?<!^)(?=[A-Z])", " ", given).strip()
    gender_vi = "nữ" if gender.lower() == "female" else "nam"
    return f"{given} ({gender_vi})"


async def synth(text: str, voice: str, out: str) -> None:
    import edge_tts
    await edge_tts.Communicate(text, voice).save(out)
    print(f"  {out}  ←  \"{text}\"")


async def main() -> None:
    import edge_tts
    all_voices = await edge_tts.list_voices()
    vi_voices = [v for v in all_voices if v["Locale"].startswith("vi-")]
    if not vi_voices:
        sys.exit("No Vietnamese voices found in edge-tts.")

    seen_ids: set[str] = set()
    manifest = {"voices": []}

    for v in vi_voices:
        vid = voice_id(v["ShortName"])
        if vid in seen_ids:
            sys.exit(f"Voice id collision: {vid} (from {v['ShortName']})")
        seen_ids.add(vid)

        out_dir = os.path.join(OUT_ROOT, vid)
        os.makedirs(out_dir, exist_ok=True)
        print(f"\n→ {v['ShortName']}  →  static/audio/{vid}/")

        tasks = [
            synth(number_to_vietnamese(n), v["ShortName"], os.path.join(out_dir, f"{n}.mp3"))
            for n in range(1, 91)
        ]
        tasks.append(synth("Chờ",  v["ShortName"], os.path.join(out_dir, "cho.mp3")))
        tasks.append(synth("Kinh", v["ShortName"], os.path.join(out_dir, "kinh.mp3")))
        await asyncio.gather(*tasks)

        manifest["voices"].append({
            "id": vid,
            "edgeName": v["ShortName"],
            "label": display_label(v["ShortName"], v["Gender"]),
            "gender": v["Gender"].lower(),
        })

    manifest_path = os.path.join(OUT_ROOT, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"\nWrote manifest with {len(manifest['voices'])} voice(s) → {manifest_path}")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except ImportError:
        sys.exit("Install dep first: pip install edge-tts")
