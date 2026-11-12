"""Builds the documentation pages from README.md / README.ru.md / CHANGELOG.md, so the README stays the one
source: docs/wiki/<lang>/<page>.md (plain pages, no H1: titles come from the outside), then

  --github <dir>   a GitHub wiki checkout: Home.md, <Page>.md, <Page>-ru.md, _Sidebar.md, _Footer.md
  --site <dir>     the Farmstead docs site content dir (game/neoessentials/docs/site/content):
                   footprints/ and en/footprints/ with their _meta.json

    python -I tools/build_wiki.py [--github <dir>] [--site <dir>]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = "https://raw.githubusercontent.com/shtanko-michael/farmstead-footprints/main/"
REPO = "https://github.com/shtanko-michael/farmstead-footprints"

# page slug -> README sections (by position, so both languages split the same way)
# README section order: 0 see, 1 features, 2 own prints, 3 downloads, 4 settings, 5 commands, 6 update check,
# 7 compatibility, 8 faq, 9 planned, 10 building, 11 license
PAGES = [
    ("", [0, 1]),
    ("installation", [3, 6]),
    ("settings", [4]),
    ("own-prints", [2]),
    ("commands", [5]),
    ("faq", [7, 8, 9]),
    ("building", [10, 11]),
]

TITLES = {
    "en": {"": ("Footprints", "Overview"), "installation": ("Installation and versions", None),
           "settings": ("Settings", None), "own-prints": ("Your own prints", None), "commands": ("Commands", None),
           "faq": ("Compatibility and FAQ", None), "building": ("Building from source", None),
           "changelog": ("Changelog", None)},
    "ru": {"": ("Footprints", "Обзор"), "installation": ("Установка и версии", None),
           "settings": ("Настройки", None), "own-prints": ("Свои следы", None), "commands": ("Команды", None),
           "faq": ("Совместимость и FAQ", None), "building": ("Сборка из исходников", None),
           "changelog": ("История версий", None)},
}
LEDES = {
    "en": {"": "Realistic footprints for players and every mob, client-side, on Fabric, Forge and NeoForge from 1.20.1 to 26.3.",
           "installation": "Which jar for which game, what else you need (nothing), and the once-per-start update check.",
           "settings": "Every key of config/farmstead_footprints-client.toml, the presets and the settings screen.",
           "own-prints": "PNG pictures as prints, random variants, per-mob profiles and what resource packs and mods can add.",
           "commands": "The /footprints client commands.",
           "faq": "Shaders, other footprint mods, modded blocks and mobs, and common questions.",
           "building": "Building the 22 jars and the license.",
           "changelog": "What changed in each release."},
    "ru": {"": "Реалистичные следы игроков и всех мобов, только клиент, Fabric, Forge и NeoForge от 1.20.1 до 26.3.",
           "installation": "Какой jar для какой игры, что ещё нужно (ничего) и проверка обновлений при запуске.",
           "settings": "Все ключи config/farmstead_footprints-client.toml, пресеты и экран настроек.",
           "own-prints": "PNG-картинки как следы, случайные варианты, профили мобов и что могут добавить ресурспаки и моды.",
           "commands": "Клиентские команды /footprints.",
           "faq": "Шейдеры, другие моды следов, модовые блоки и мобы, частые вопросы.",
           "building": "Сборка 22 jar и лицензия.",
           "changelog": "Что менялось в каждом релизе."},
}
CTA = {
    "en": '<p class="cta-row"><a class="cta" href="https://modrinth.com/mod/farmstead-footprints">⬇ Modrinth</a> '
          '<a class="cta cta-soft" href="https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints">CurseForge</a> '
          f'<a class="cta cta-soft" href="{REPO}">GitHub</a></p>',
    "ru": '<p class="cta-row"><a class="cta" href="https://modrinth.com/mod/farmstead-footprints">⬇ Modrinth</a> '
          '<a class="cta cta-soft" href="https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints">CurseForge</a> '
          f'<a class="cta cta-soft" href="{REPO}">GitHub</a></p>',
}
CHANGELOG_RU_NOTE = "Список изменений ведётся на английском, как в репозитории.\n\n"


def sections(text):
    parts = re.split(r"(?m)^(## .*)$", text)
    intro = parts[0]
    out = []
    for i in range(1, len(parts), 2):
        out.append((parts[i][3:].strip(), parts[i + 1].strip()))
    return intro, out


def absolute(md):
    # relative media and file links -> absolute, the pages live outside the repo
    md = re.sub(r"\]\((docs/[^)]+)\)", lambda m: f"]({RAW}{m.group(1)})", md)
    md = re.sub(r'src="(docs/[^"]+)"', lambda m: f'src="{RAW}{m.group(1)}"', md)
    md = re.sub(r"\]\(((?:LICENSE|CHANGELOG\.md|README[^)]*|listing/[^)]+))\)", lambda m: f"]({REPO}/blob/main/{m.group(1)})", md)
    return md


def build_pages():
    pages = {}
    for lang, readme in (("en", "README.md"), ("ru", "README.ru.md")):
        intro, secs = sections((ROOT / readme).read_text(encoding="utf-8"))
        intro_text = "\n".join(l for l in intro.splitlines()
                               if not l.startswith(("# ", "[![", "<p align", "<img", "![")) and "README" not in l).strip()
        for slug, idx in PAGES:
            body = []
            if slug == "":
                body.append(CTA[lang])
                body.append(intro_text)
            for i in idx:
                title, text = secs[i]
                if slug in ("settings", "own-prints", "commands") and len(idx) == 1:
                    body.append(text)
                else:
                    body.append(f"## {title}\n\n{text}")
            pages[(lang, slug)] = absolute("\n\n".join(b for b in body if b).strip() + "\n")
        log = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
        log = re.sub(r"(?s)^# Changelog\s*", "", log)
        pages[(lang, "changelog")] = (CHANGELOG_RU_NOTE if lang == "ru" else "") + log.strip() + "\n"
    return pages


def write_local(pages):
    for (lang, slug), text in pages.items():
        p = ROOT / "docs" / "wiki" / lang / f"{slug or 'index'}.md"
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(text, encoding="utf-8")


def github_name(lang, slug):
    title = TITLES["en"][slug][0] if slug else "Home"
    name = title.replace(" ", "-")
    return name if lang == "en" else name + "-ru"


def write_github(pages, out):
    out = Path(out)
    order = [s for s, _ in PAGES] + ["changelog"]
    for (lang, slug), text in pages.items():
        title, _ = TITLES[lang][slug]
        head = f"> {LEDES[lang][slug]}\n\n"
        if slug == "":
            other = "[Русская версия](Home-ru)" if lang == "en" else "[English version](Home)"
            head = f"{other}\n\n" + head
        (out / f"{github_name(lang, slug)}.md").write_text(head + re.sub(r'<p class="cta-row">.*?</p>\n*', "", text), encoding="utf-8")
    side = ["**Footprints**", ""]
    for slug in order:
        side.append(f"- [{TITLES['en'][slug][1] or TITLES['en'][slug][0]}]({github_name('en', slug)})")
    side += ["", "**По-русски**", ""]
    for slug in order:
        side.append(f"- [{TITLES['ru'][slug][1] or TITLES['ru'][slug][0]}]({github_name('ru', slug)})")
    (out / "_Sidebar.md").write_text("\n".join(side) + "\n", encoding="utf-8")
    (out / "_Footer.md").write_text(
        "Made by [Farmstead](https://farmsteadminecraft.online) · [Modrinth](https://modrinth.com/mod/farmstead-footprints) · "
        "[CurseForge](https://www.curseforge.com/minecraft/mc-mods/farmstead-footprints) · "
        f"[Source]({REPO}) · [Issues]({REPO}/issues)\n", encoding="utf-8")


KEYWORDS = ["Footprints", "farmstead_footprints", "/footprints", "footprint", "следы"]


def write_site(pages, content):
    content = Path(content)
    for lang, base in (("ru", content / "footprints"), ("en", content / "en" / "footprints")):
        base.mkdir(parents=True, exist_ok=True)
        groups = {"ru": ["Начало", "Справочник", "Помощь"], "en": ["Start", "Reference", "Help"]}[lang]
        group_of = {"": 0, "installation": 0, "settings": 1, "own-prints": 1, "commands": 1, "faq": 2, "building": 2, "changelog": 2}
        meta = {"groups": groups, "pages": []}
        for slug, _ in PAGES + [("changelog", None)]:
            title, nav = TITLES[lang][slug]
            entry = {"slug": slug, "title": title if slug else "Footprints", "group": groups[group_of[slug]],
                     "lede": LEDES[lang][slug], "keywords": KEYWORDS + [title]}
            if slug == "":
                entry["source"] = "index.md"
                entry["navTitle"] = nav
            if slug == "settings":
                entry["wide"] = True
            meta["pages"].append(entry)
            (base / f"{slug or 'index'}.md").write_text(pages[(lang, slug)], encoding="utf-8")
        if lang == "ru":
            (base / "_meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        else:
            (base / "_meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    pages = build_pages()
    write_local(pages)
    args = sys.argv[1:]
    if "--github" in args:
        write_github(pages, args[args.index("--github") + 1])
    if "--site" in args:
        write_site(pages, args[args.index("--site") + 1])
    print(f"{len(pages)} pages")
