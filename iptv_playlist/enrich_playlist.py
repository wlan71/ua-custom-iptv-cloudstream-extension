#!/usr/bin/env python3
"""
Reads an IPTV playlist (nStream XML or M3U/M3U8), adds logos from the iptv-org
database, and writes the result next to the source file with "+logo" in the name.

All original attributes, groups, categories, and extra lines are preserved as-is.
Only tvg-logo is added or updated.

Usage:
    python3 enrich_playlist.py <playlist_file>

Examples:
    python3 enrich_playlist.py playlist-ua+xx.m3u8
    python3 enrich_playlist.py ~/Downloads/nStream.xml
"""

import xml.etree.ElementTree as ET
import json
import re
import urllib.request
import sys
import os

CHANNELS_URL = "https://iptv-org.github.io/api/channels.json"
LOGOS_URL    = "https://iptv-org.github.io/api/logos.json"


def fetch(url):
    print(f"  Downloading {url} ...")
    with urllib.request.urlopen(url, timeout=30) as r:
        return json.loads(r.read().decode())


def normalize(name):
    name = re.sub(r'\s+(UHD|FHD\+?|HD|SD|4K)(\s+\d+)?', '', name, flags=re.IGNORECASE)
    name = re.sub(r'\s+\+\d+', '', name)
    name = re.sub(r'\s+\(.*?\)', '', name)
    name = re.sub(r'\s+(UK|US|RU|UA|DE|FR|NL|AL|PL|IT|ES|AT|BE|CZ|HU|RO|BG|TR|IN|AU|CA|AR|BR|SE|NO|DK|FI|GR|IL|SA|AE|JP|KR|CN)$', '', name, flags=re.IGNORECASE)
    name = name.replace('-', ' ').replace('!', '')
    name = re.sub(r'\s+', ' ', name).strip().lower()
    return name


def find_logo(channel_name, logo_map):
    key = normalize(channel_name)
    if key in logo_map:
        return logo_map[key]
    for k, v in logo_map.items():
        if len(k) >= 4 and k in key:
            return v
    return None


def build_logo_map(channels, logos):
    id_to_logo = {
        l["channel"]: l["url"]
        for l in logos
        if l.get("url") and l.get("in_use", False)
    }
    logo_map = {}
    for ch in channels:
        logo = id_to_logo.get(ch.get("id", ""))
        if not logo:
            continue
        logo_map[normalize(ch.get("name", ""))] = logo
        for alt in ch.get("alt_names", []):
            logo_map[normalize(alt)] = logo
    return logo_map


def inject_logo(extinf_line, logo):
    """Insert or replace tvg-logo in an #EXTINF line, keeping everything else."""
    if re.search(r'tvg-logo="[^"]*"', extinf_line):
        return re.sub(r'tvg-logo="[^"]*"', f'tvg-logo="{logo}"', extinf_line)
    # insert right after the duration number (#EXTINF:0 or #EXTINF:-1 etc.)
    return re.sub(r'(#EXTINF:-?\d+)', rf'\1 tvg-logo="{logo}"', extinf_line, count=1)


def enrich_m3u(input_path, output_path, logo_map):
    out_lines = []
    found = 0
    not_found = []
    pending_extinf = None
    pending_extgrp = None
    total = 0

    with open(input_path, encoding="utf-8", errors="replace") as f:
        for line in f:
            stripped = line.rstrip("\n\r")
            s = stripped.strip()

            if s.startswith("#EXTINF"):
                pending_extinf = stripped
            elif s.startswith("#EXTGRP"):
                # buffer #EXTGRP — output it after #EXTINF (preserving original order)
                pending_extgrp = stripped
            elif s and not s.startswith("#"):
                # URL line
                if pending_extinf is not None:
                    total += 1
                    comma_idx = pending_extinf.rfind(",")
                    title = pending_extinf[comma_idx + 1:].strip() if comma_idx != -1 else ""

                    existing_logo = re.search(r'tvg-logo="([^"]*)"', pending_extinf)
                    existing_logo = existing_logo.group(1).strip() if existing_logo else ""

                    logo = existing_logo or find_logo(title, logo_map) or ""
                    if logo:
                        found += 1
                        pending_extinf = inject_logo(pending_extinf, logo)
                    else:
                        not_found.append(title)

                    out_lines.append(pending_extinf)
                    if pending_extgrp is not None:
                        out_lines.append(pending_extgrp)
                    pending_extinf = None
                    pending_extgrp = None

                out_lines.append(stripped)
            else:
                # other # lines or blank — flush any pending buffers first
                if pending_extinf is not None:
                    out_lines.append(pending_extinf)
                    pending_extinf = None
                if pending_extgrp is not None:
                    out_lines.append(pending_extgrp)
                    pending_extgrp = None
                out_lines.append(stripped)

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(out_lines))

    return total, found, not_found


def enrich_xml(input_path, output_path, logo_map):
    tree = ET.parse(input_path)
    root = tree.getroot()
    found = 0
    not_found = []
    total = 0

    lines = ["#EXTM3U"]
    for ch in root.findall("channel"):
        title = ch.findtext("title", "").strip()
        url   = ch.findtext("stream_url", "").strip()
        if not title or not url:
            continue
        total += 1
        logo = find_logo(title, logo_map) or ""
        if logo:
            found += 1
        else:
            not_found.append(title)
        lines.append(f'#EXTINF:-1 tvg-logo="{logo}",{title}')
        lines.append(url)

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    return total, found, not_found


def detect_format(path):
    ext = os.path.splitext(path)[1].lower()
    if ext == ".xml":
        return "xml"
    if ext in (".m3u", ".m3u8"):
        return "m3u"
    with open(path, encoding="utf-8", errors="replace") as f:
        first = f.read(16).strip()
    return "xml" if first.startswith("<") else "m3u"


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 enrich_playlist.py <playlist_file>")
        print("Example: python3 enrich_playlist.py playlist-ua+xx.m3u8")
        sys.exit(1)

    input_file = os.path.expanduser(sys.argv[1])
    base, ext = os.path.splitext(input_file)
    output_file = base + "+logo" + ext

    print("=== enrich_playlist.py ===\n")

    if not os.path.exists(input_file):
        print(f"File not found: {input_file}")
        sys.exit(1)

    fmt = detect_format(input_file)
    fmt_label = "nStream XML" if fmt == "xml" else "M3U/M3U8"
    print(f"[1/4] Reading {input_file} ({fmt_label}) ...")

    # 2. Download logo database
    print("\n[2/4] Downloading iptv-org logo database ...")
    try:
        iptv_channels = fetch(CHANNELS_URL)
        iptv_logos    = fetch(LOGOS_URL)
    except Exception as e:
        print(f"  Error: {e}")
        sys.exit(1)

    # 3. Build name → logo map
    print("\n[3/4] Building logo map ...")
    logo_map = build_logo_map(iptv_channels, iptv_logos)
    print(f"      Logos in database: {len(logo_map)}")

    # 4. Enrich
    print(f"\n[4/4] Writing {os.path.basename(output_file)} ...")
    if fmt == "xml":
        total, found, not_found = enrich_xml(input_file, output_file, logo_map)
    else:
        total, found, not_found = enrich_m3u(input_file, output_file, logo_map)

    print(f"\n  ✓ Logos found: {found}/{total}")
    if not_found:
        print(f"  ✗ No logo ({len(not_found)} channels):")
        for n in not_found[:20]:
            print(f"      - {n}")
        if len(not_found) > 20:
            print(f"      ... and {len(not_found)-20} more")

    print(f"\n  Saved: {output_file}")


if __name__ == "__main__":
    main()
