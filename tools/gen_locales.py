#!/usr/bin/env python3
"""Transcribe the iOS Localizable.strings translations into Android strings.xml files.

Mechanical transform only — values are copied verbatim from each .lproj file (plan §8.2 forbids
machine translation). The iOS keys are the literal English display text, so the English source file
provides the key -> Android-resource-name mapping.
"""
import os
import re
import sys

IOS = "/Users/armstrongllc/Desktop/BLE/nRFThingy52/nRFThingy52/Utilities"
AND = "/Users/armstrongllc/Desktop/BLE/nRFThingy52Android/app/src/main/res"

# iOS English key -> Android resource name. Order here is the order written to file.
MAPPING = [
    ("Nearby Devices", "nearby_devices"),
    ("Scanning...", "scanning"),
    ("Unknown Device", "unknown_device"),
    ("CAN'T SEE YOUR THINGY?", "cant_see_your_thingy"),
    ("1. Make sure it's switched on.", "empty_state_step_1"),
    ("Toggle the switch next to the micro USB port to switch it on.", "empty_state_step_1_detail"),
    # Corrected 2026-07-28: the Thingy:52 has no coin cell — it is a rechargeable 1440 mAh Li-Po
    # charged over USB. The old wording came from the Nordic sample app both projects descend from.
    # The replacement deliberately makes no claim about where the battery is; don't reintroduce one.
    ("2. Make sure the battery is charged.", "empty_state_step_2"),
    ("If not, connect it to a PC or a charger using a micro USB cable. The dev kit has a built-in rechargeable battery.", "empty_state_step_2_detail"),
    ("ON", "on"),
    ("OFF", "off"),
    ("PRESSED", "pressed"),
    ("RELEASED", "released"),
    ("Reading...", "reading"),
    ("DISCONNECTED", "disconnected"),
    ("LED", "led"),
    ("Button", "button"),
    ("Toggling the switch will cause the LED on the Thingy to turn ON or OFF.", "led_section_footer"),
    ("Pressing and releasing the button on the Thingy will update the state here.", "button_section_footer"),
    ("Environment", "environment"),
    ("Temperature", "temperature"),
    ("Humidity", "humidity"),
    ("Pressure", "pressure"),
    ("Air Quality", "air_quality"),
    ("Live sensor readings streamed from the Thingy.", "environment_section_footer"),
    ("Motion", "motion"),
    ("Orientation", "orientation"),
    # Orientation *values*, localized 2026-07-28 (plan §10 item 13). On iOS these live in
    # ThingyOrientation.labelKey and the English sentence is the lookup key, per that app's
    # convention; the Android resource names below are ours to choose, only the values are shared.
    ("Portrait", "orientation_portrait"),
    ("Landscape", "orientation_landscape"),
    ("Portrait (upside down)", "orientation_portrait_upside_down"),
    ("Landscape (upside down)", "orientation_landscape_upside_down"),
    ("Steps", "steps"),
    ("Heading", "heading"),
    ("Last Tap", "last_tap"),
    ("Orientation, steps, heading, and taps reported by the Thingy's motion sensors.", "motion_section_footer"),
]

# iOS .lproj -> Android resource qualifier. zh-Hans uses the modern BCP-47 form rather than the
# legacy zh-rCN, so it also matches zh-Hans-SG etc. (plan §8.1 leaves the choice open).
LOCALES = {
    # ja and zh-Hant were added on the iOS side during the 2026-07-26 translation pass — they are not
    # in the plan's original §8.1 list of 16.
    "ja": "values-ja",
    "zh-Hant": "values-b+zh+Hant",
    "de": "values-de",
    "es": "values-es",
    "fi": "values-fi",
    "fr": "values-fr",
    "it": "values-it",
    "ko": "values-ko",
    "mr": "values-mr",
    "nb": "values-nb",
    "pl": "values-pl",
    "pt-BR": "values-pt-rBR",
    "ro": "values-ro",
    "ru": "values-ru",
    "uk": "values-uk",
    "vi": "values-vi",
    "zh-Hans": "values-b+zh+Hans",
}

# "key" = "value";  — values may contain escaped quotes.
ENTRY = re.compile(r'^\s*"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', re.M)


def unescape(s: str) -> str:
    return s.replace('\\"', '"').replace("\\n", "\n").replace("\\\\", "\\")


def parse(path: str) -> dict:
    with open(path, encoding="utf-8") as f:
        text = f.read()
    return {unescape(k): unescape(v) for k, v in ENTRY.findall(text)}


def xml_escape(value: str) -> str:
    """Escape for an Android string resource value."""
    v = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    v = v.replace("'", "\\'").replace('"', '\\"')
    if v.startswith("@") or v.startswith("?"):
        v = "\\" + v
    return v


def main() -> int:
    english = parse(os.path.join(IOS, "en.lproj", "Localizable.strings"))
    missing_en = [k for k, _ in MAPPING if k not in english]
    if missing_en:
        print(f"ERROR: keys absent from en.lproj: {missing_en}")
        return 1

    problems = []
    for lproj, qualifier in sorted(LOCALES.items()):
        src = os.path.join(IOS, f"{lproj}.lproj", "Localizable.strings")
        if not os.path.exists(src):
            problems.append(f"{lproj}: source missing")
            continue
        table = parse(src)

        translated = sum(1 for k, _ in MAPPING if table.get(k) != english[k])
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            f"<!-- {lproj}: transcribed verbatim from the iOS {lproj}.lproj/Localizable.strings",
            "     (plan §8.2). Not machine-translated.",
            "",
            f"     COVERAGE: {translated} of {len(MAPPING)} values differ from English. Any that match are genuine",
            "     translations that coincide with the English word — \"LED\" is LED in most languages —",
            "     not untranslated placeholders. (That was not true before 2026-07-26: 24 strings were",
            "     English placeholders upstream until the iOS translation pass landed. See plan",
            "     §10 item 12.)",
            "",
            "     Strings with no iOS counterpart (app_name, scanner_title, and the cd_* content",
            "     descriptions) intentionally stay in values/ and fall back to English. -->",
            "<resources>",
        ]
        untranslated = []
        for key, name in MAPPING:
            if key not in table:
                problems.append(f"{lproj}: missing key {key!r}")
                continue
            value = table[key]
            if value == english[key]:
                untranslated.append(name)
            lines.append(f'    <string name="{name}">{xml_escape(value)}</string>')
        lines.append("</resources>")

        outdir = os.path.join(AND, qualifier)
        os.makedirs(outdir, exist_ok=True)
        with open(os.path.join(outdir, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")

        note = f"  (same-as-English: {', '.join(untranslated)})" if untranslated else ""
        print(f"wrote {qualifier}/strings.xml  {len(MAPPING)} strings{note}")

    if problems:
        print("\nPROBLEMS:")
        for p in problems:
            print("  -", p)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
