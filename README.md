# Ukraine IPTV — CloudStream Plugin

A [CloudStream 3](https://github.com/recloudstream/cloudstream) plugin for watching IPTV channels via a custom M3U/M3U8 playlist.

---

## Features

- Loads channels from any M3U/M3U8 playlist URL
- Channels grouped by categories from the playlist
- Channel logos resolved automatically from the [iptv-org](https://github.com/iptv-org/iptv) logo database
- DRM stream support (Widevine)
- Chromecast support
- Search across all channels
- Configurable playlist URL via plugin settings

---

## Installation

1. Open CloudStream 3
2. Go to **Settings → Extensions → Add repository**
3. Enter the repository URL:
   ```
   cloudstreamrepo://raw.githubusercontent.com/wlan71/cloudstream-extensions/builds/repo.json
   ```
4. Install the **Ukraine IPTV** plugin from the repository

---

## Configuration

1. Open the plugin settings (long-press the plugin or use the gear icon)
2. Paste your M3U/M3U8 playlist URL
3. Save and restart the app

If no URL is set, the plugin falls back to the public [iptv-org Ukraine playlist](https://iptv-org.github.io/iptv/countries/ua.m3u).

---

## Playlist with logos

The included Python script [`iptv_playlist/enrich_playlist.py`](iptv_playlist/enrich_playlist.py) enriches any M3U, M3U8, or nStream XML playlist with channel logos from the iptv-org database.

```bash
python3 iptv_playlist/enrich_playlist.py ~/Downloads/playlist.m3u8
# Output: ~/Downloads/playlist+logo.m3u8
```

See [`iptv_playlist/README.md`](iptv_playlist/README.md) for full usage instructions.

---

## Requirements

- CloudStream 3 (latest version)
- A valid M3U/M3U8 playlist URL

---

## License

[![GNU GPLv3](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

Distributed under the [GNU General Public License v3](https://www.gnu.org/licenses/gpl.html).

---

## Disclaimer

This plugin does not host any content. It loads streams from URLs provided in your playlist. Users are solely responsible for the content they access and must comply with their local laws.
