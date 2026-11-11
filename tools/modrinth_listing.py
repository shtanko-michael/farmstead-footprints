"""Brings the Modrinth project page in line with the repository: summary, description, categories, sides, links,
license, icon and gallery, all from listing/modrinth.json (paths there are relative to the repository root).

    MODRINTH_TOKEN=... MODRINTH_ID=... python3 tools/modrinth_listing.py [--dry-run]

Runs in CI (.github/workflows/listing.yml), where the token lives as a secret. The gallery is replaced as a whole:
old images are removed, the configured ones uploaded in order. Versions are not touched (release.yml adds them).
"""
import json
import mimetypes
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://api.modrinth.com/v2"
ROOT = Path(__file__).resolve().parent.parent
DRY = "--dry-run" in sys.argv
TOKEN = os.environ.get("MODRINTH_TOKEN", "")
PROJECT = os.environ.get("MODRINTH_ID", "")
UA = "farmstead-mods/listing-sync (github.com/shtanko-michael)"


def call(method, path, body=None, data=None, content_type=None):
    url = API + path
    headers = {"Authorization": TOKEN, "User-Agent": UA}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        content_type = "application/json"
    if content_type:
        headers["Content-Type"] = content_type
    if DRY and method != "GET":
        print(f"  dry run: {method} {path} ({len(data or b'')} bytes)")
        return None
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            raw = r.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        sys.exit(f"{method} {path}: HTTP {e.code} {e.read().decode('utf-8', 'replace')[:500]}")


def main():
    if not TOKEN or not PROJECT:
        sys.exit("MODRINTH_TOKEN and MODRINTH_ID are required")
    cfg = json.loads((ROOT / "listing" / "modrinth.json").read_text(encoding="utf-8"))
    project = call("GET", f"/project/{PROJECT}")
    print(f"project {project['slug']} ({project['status']}), gallery {len(project['gallery'])} image(s)")

    patch = {
        "title": cfg["title"],
        "description": cfg["summary"],
        "body": (ROOT / cfg["body"]).read_text(encoding="utf-8"),
        "categories": cfg["categories"],
        "additional_categories": cfg.get("additional_categories", []),
        "client_side": cfg["client_side"],
        "server_side": cfg["server_side"],
        "license_id": cfg["license_id"],
        "license_url": cfg.get("license_url"),
        "source_url": cfg.get("source_url"),
        "issues_url": cfg.get("issues_url"),
        "wiki_url": cfg.get("wiki_url"),
    }
    call("PATCH", f"/project/{PROJECT}", body=patch)
    print("details and description updated")

    icon = ROOT / cfg["icon"]
    call("PATCH", f"/project/{PROJECT}/icon?ext={icon.suffix[1:]}", data=icon.read_bytes(),
         content_type=mimetypes.guess_type(icon.name)[0])
    print(f"icon {icon.name}")

    for img in project["gallery"]:
        call("DELETE", f"/project/{PROJECT}/gallery?url={urllib.parse.quote(img['url'], safe='')}")
    print(f"removed {len(project['gallery'])} old gallery image(s)")
    for i, g in enumerate(cfg["gallery"]):
        f = ROOT / g["file"]
        q = urllib.parse.urlencode({
            "ext": f.suffix[1:],
            "featured": str(bool(g.get("featured"))).lower(),
            "title": g["title"],
            "description": g.get("description", ""),
            "ordering": i,
        })
        call("POST", f"/project/{PROJECT}/gallery?{q}", data=f.read_bytes(), content_type=mimetypes.guess_type(f.name)[0])
        print(f"gallery {i + 1}: {f.name} ({f.stat().st_size // 1024} KB)")
        time.sleep(1)  # be gentle with the rate limit
    print("done")


if __name__ == "__main__":
    main()
