# enrich_playlist.py

Enriches an IPTV playlist with channel logos from the [iptv-org](https://github.com/iptv-org/iptv) database and outputs a ready-to-use M3U file.

Supports **nStream XML** (`.xml`) and **M3U/M3U8** (`.m3u`, `.m3u8`) input formats.

## Requirements

- Python 3.6+
- Internet connection (downloads logo database from iptv-org)

## Usage

```bash
python3 enrich_playlist.py <input_file>
```

**Examples:**

```bash
# File in the same folder as the script
python3 enrich_playlist.py playlist.m3u8

# File in Downloads
python3 enrich_playlist.py ~/Downloads/playlist.m3u8
python3 enrich_playlist.py ~/Downloads/channels.m3u
python3 enrich_playlist.py ~/Downloads/nStream.xml
```

## Output

The enriched playlist is saved **next to the input file**, with `+logo` appended to the filename. The extension stays the same.

```
playlist.m3u8      →  playlist+logo.m3u8
channels.m3u       →  channels+logo.m3u
nStream.xml        →  nStream+logo.xml
```

Each channel entry will have a `tvg-logo` attribute filled in from the iptv-org logo database. For M3U/M3U8 input, logos already present in the source file are kept as-is; missing ones are looked up by channel name.

## How to upload to GitHub Gist (private)

GitHub Gist lets you host a single file privately with a direct download link.

1. Go to **https://gist.github.com** (sign in if needed).
2. In the **Filename** field enter the name of your file, e.g. `playlist+logo.m3u8`.
3. Paste the **contents** of the enriched file into the text area  
   (open the file in any text editor, Select All, Copy, Paste).
4. At the bottom choose **Create secret gist** — do NOT click "Create public gist".
5. On the next page click the **Raw** button (top-right of the file box).
6. Copy the URL from the browser address bar — it looks like:  
   `https://gist.githubusercontent.com/username/abc123/raw/d4e5f6a7b8c9.../playlist+logo.m3u8`

> The URL opens in a browser and shows the raw file text — that means it works correctly.

> **Note:** if you update the Gist with a new version of the file, the Raw URL will change.
> Grab the new Raw URL and update it in the CloudStream plugin settings.

To find your Gist later, go to **https://gist.github.com/wlan71** — all your Gists are listed there.

## How to use in CloudStream

1. Open the **Ukraine IPTV** plugin settings in the CloudStream app.
2. Paste the Raw Gist URL as the playlist URL.
3. Save and restart the app.

## Logo matching

Channel names are matched against the iptv-org database using normalized comparison:
- suffixes like HD, FHD, UHD, 4K, SD are stripped
- hyphens are treated as spaces (`Ukraine-1` → `Ukraine 1`)
- country codes at the end are ignored
- case-insensitive

Unmatched channels are listed at the end of the script output.
