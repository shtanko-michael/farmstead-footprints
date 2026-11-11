"""Imprint translations: one table, twelve lang files, and the completeness check.

    python -I tools/lang.py          write src/main/resources/assets/imprint/lang/*.json
    python -I tools/lang.py --check  fail on a missing language, key or placeholder mismatch

en_us and ru_ru are the references; every other language must carry every key with the same
%s / %d placeholders. Terms: "print/footprint" (not "track"), "preset", "Imprint" stays as is.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/imprint/lang"
LANGS = ["en_us", "ru_ru", "zh_cn", "pt_br", "es_es", "de_de", "fr_fr", "pl_pl", "uk_ua", "ja_jp", "ko_kr", "tr_tr"]

T = {
    "config.imprint.title": {
        "en_us": "Imprint settings", "ru_ru": "Настройки Imprint", "zh_cn": "Imprint 设置", "pt_br": "Configurações do Imprint",
        "es_es": "Ajustes de Imprint", "de_de": "Imprint-Einstellungen", "fr_fr": "Paramètres d'Imprint", "pl_pl": "Ustawienia Imprint",
        "uk_ua": "Налаштування Imprint", "ja_jp": "Imprint の設定", "ko_kr": "Imprint 설정", "tr_tr": "Imprint ayarları"},
    "config.imprint.preset": {
        "en_us": "Preset", "ru_ru": "Пресет", "zh_cn": "预设", "pt_br": "Predefinição", "es_es": "Preajuste", "de_de": "Voreinstellung",
        "fr_fr": "Préréglage", "pl_pl": "Ustawienie wstępne", "uk_ua": "Пресет", "ja_jp": "プリセット", "ko_kr": "프리셋", "tr_tr": "Ön ayar"},
    "config.imprint.preset.tooltip": {
        "en_us": "Sets every value below at once. Values you change afterwards are kept.",
        "ru_ru": "Задаёт все значения ниже разом. То, что вы поменяете потом, сохранится.",
        "zh_cn": "一次性设置下方所有数值。之后手动修改的数值会被保留。",
        "pt_br": "Define todos os valores abaixo de uma vez. Valores alterados depois são mantidos.",
        "es_es": "Establece todos los valores de abajo a la vez. Los que cambies después se conservan.",
        "de_de": "Setzt alle Werte unten auf einmal. Danach geänderte Werte bleiben erhalten.",
        "fr_fr": "Règle toutes les valeurs ci-dessous d'un coup. Celles modifiées ensuite sont conservées.",
        "pl_pl": "Ustawia naraz wszystkie wartości poniżej. Zmienione później wartości zostają.",
        "uk_ua": "Задає всі значення нижче одразу. Те, що ви зміните потім, збережеться.",
        "ja_jp": "下の値をまとめて設定します。後から変更した値はそのまま残ります。",
        "ko_kr": "아래 값을 한 번에 설정합니다. 이후에 바꾼 값은 유지됩니다.",
        "tr_tr": "Aşağıdaki tüm değerleri tek seferde ayarlar. Sonradan değiştirdiğiniz değerler korunur."},
    "config.imprint.preset.default": {
        "en_us": "Default", "ru_ru": "По умолчанию", "zh_cn": "默认", "pt_br": "Padrão", "es_es": "Predeterminado", "de_de": "Standard",
        "fr_fr": "Par défaut", "pl_pl": "Domyślne", "uk_ua": "За замовчуванням", "ja_jp": "標準", "ko_kr": "기본", "tr_tr": "Varsayılan"},
    "config.imprint.preset.vanilla_plus": {
        "en_us": "Vanilla+", "ru_ru": "Ваниль+", "zh_cn": "原版+", "pt_br": "Vanilla+", "es_es": "Vanilla+", "de_de": "Vanilla+",
        "fr_fr": "Vanilla+", "pl_pl": "Vanilla+", "uk_ua": "Ваніль+", "ja_jp": "バニラ+", "ko_kr": "바닐라+", "tr_tr": "Vanilla+"},
    "config.imprint.preset.cinematic": {
        "en_us": "Cinematic", "ru_ru": "Кинематограф", "zh_cn": "电影感", "pt_br": "Cinematográfico", "es_es": "Cinemático",
        "de_de": "Filmisch", "fr_fr": "Cinématique", "pl_pl": "Filmowe", "uk_ua": "Кінематограф", "ja_jp": "シネマティック",
        "ko_kr": "시네마틱", "tr_tr": "Sinematik"},
    "config.imprint.preset.performance": {
        "en_us": "Performance", "ru_ru": "Производительность", "zh_cn": "性能优先", "pt_br": "Desempenho", "es_es": "Rendimiento",
        "de_de": "Leistung", "fr_fr": "Performances", "pl_pl": "Wydajność", "uk_ua": "Продуктивність", "ja_jp": "パフォーマンス",
        "ko_kr": "성능", "tr_tr": "Performans"},
    "config.imprint.general.enabled": {
        "en_us": "Enabled", "ru_ru": "Включено", "zh_cn": "启用", "pt_br": "Ativado", "es_es": "Activado", "de_de": "Aktiviert",
        "fr_fr": "Activé", "pl_pl": "Włączone", "uk_ua": "Увімкнено", "ja_jp": "有効", "ko_kr": "사용", "tr_tr": "Etkin"},
    "config.imprint.general.enabled.tooltip": {
        "en_us": "Master switch. Off: no prints are made or drawn.",
        "ru_ru": "Главный выключатель. Выключено — следы не появляются и не рисуются.",
        "zh_cn": "总开关。关闭后不会产生也不会绘制脚印。",
        "pt_br": "Chave geral. Desligado: nenhuma pegada é criada ou desenhada.",
        "es_es": "Interruptor general. Apagado: no se crean ni se dibujan huellas.",
        "de_de": "Hauptschalter. Aus: Es entstehen keine Spuren und es werden keine gezeichnet.",
        "fr_fr": "Interrupteur général. Désactivé : aucune empreinte n'est créée ni affichée.",
        "pl_pl": "Główny przełącznik. Wyłączony: ślady nie powstają i nie są rysowane.",
        "uk_ua": "Головний вимикач. Вимкнено — сліди не з'являються і не малюються.",
        "ja_jp": "全体のスイッチ。オフにすると足跡は作られず、描画もされません。",
        "ko_kr": "전체 스위치입니다. 끄면 발자국이 생기지도, 그려지지도 않습니다.",
        "tr_tr": "Ana anahtar. Kapalıyken iz oluşmaz ve çizilmez."},
    "config.imprint.footprints.players": {
        "en_us": "Player prints", "ru_ru": "Следы игроков", "zh_cn": "玩家脚印", "pt_br": "Pegadas de jogadores", "es_es": "Huellas de jugadores",
        "de_de": "Spuren von Spielern", "fr_fr": "Empreintes des joueurs", "pl_pl": "Ślady graczy", "uk_ua": "Сліди гравців",
        "ja_jp": "プレイヤーの足跡", "ko_kr": "플레이어 발자국", "tr_tr": "Oyuncu izleri"},
    "config.imprint.footprints.players.tooltip": {
        "en_us": "Prints of players, yourself included.", "ru_ru": "Следы игроков, в том числе ваши.", "zh_cn": "玩家的脚印，包括你自己。",
        "pt_br": "Pegadas de jogadores, incluindo as suas.", "es_es": "Huellas de jugadores, incluidas las tuyas.",
        "de_de": "Spuren von Spielern, deine eigenen eingeschlossen.", "fr_fr": "Empreintes des joueurs, y compris les vôtres.",
        "pl_pl": "Ślady graczy, także twoje.", "uk_ua": "Сліди гравців, зокрема ваші.", "ja_jp": "プレイヤーの足跡（自分を含む）。",
        "ko_kr": "플레이어의 발자국(자신 포함).", "tr_tr": "Oyuncuların izleri, sizinkiler dahil."},
    "config.imprint.footprints.mobs": {
        "en_us": "Mob prints", "ru_ru": "Следы мобов", "zh_cn": "生物脚印", "pt_br": "Pegadas de criaturas", "es_es": "Huellas de criaturas",
        "de_de": "Spuren von Kreaturen", "fr_fr": "Empreintes des créatures", "pl_pl": "Ślady stworzeń", "uk_ua": "Сліди мобів",
        "ja_jp": "モブの足跡", "ko_kr": "몹 발자국", "tr_tr": "Yaratık izleri"},
    "config.imprint.footprints.mobs.tooltip": {
        "en_us": "Prints of animals and monsters.", "ru_ru": "Следы животных и монстров.", "zh_cn": "动物和怪物的脚印。",
        "pt_br": "Pegadas de animais e monstros.", "es_es": "Huellas de animales y monstruos.", "de_de": "Spuren von Tieren und Monstern.",
        "fr_fr": "Empreintes des animaux et des monstres.", "pl_pl": "Ślady zwierząt i potworów.", "uk_ua": "Сліди тварин і монстрів.",
        "ja_jp": "動物とモンスターの足跡。", "ko_kr": "동물과 몬스터의 발자국.", "tr_tr": "Hayvan ve canavar izleri."},
    "config.imprint.footprints.maxPrints": {
        "en_us": "Max prints", "ru_ru": "Предел следов", "zh_cn": "脚印上限", "pt_br": "Máximo de pegadas", "es_es": "Máximo de huellas",
        "de_de": "Maximale Spuren", "fr_fr": "Empreintes max.", "pl_pl": "Maks. śladów", "uk_ua": "Межа слідів", "ja_jp": "足跡の最大数",
        "ko_kr": "최대 발자국 수", "tr_tr": "En fazla iz"},
    "config.imprint.footprints.maxPrints.tooltip": {
        "en_us": "How many prints can exist at once. The oldest disappear first when the limit is reached.",
        "ru_ru": "Сколько следов может быть одновременно. Когда предел достигнут, первыми исчезают самые старые.",
        "zh_cn": "同时存在的脚印数量。达到上限时最旧的脚印先消失。",
        "pt_br": "Quantas pegadas podem existir ao mesmo tempo. Ao atingir o limite, as mais antigas somem primeiro.",
        "es_es": "Cuántas huellas pueden existir a la vez. Al llegar al límite, las más antiguas desaparecen primero.",
        "de_de": "Wie viele Spuren gleichzeitig existieren können. Ist die Grenze erreicht, verschwinden die ältesten zuerst.",
        "fr_fr": "Nombre d'empreintes pouvant exister en même temps. Au-delà, les plus anciennes disparaissent d'abord.",
        "pl_pl": "Ile śladów może istnieć jednocześnie. Po osiągnięciu limitu najstarsze znikają pierwsze.",
        "uk_ua": "Скільки слідів може бути одночасно. Коли межу досягнуто, першими зникають найстаріші.",
        "ja_jp": "同時に存在できる足跡の数。上限に達すると古いものから消えます。",
        "ko_kr": "동시에 존재할 수 있는 발자국 수입니다. 한도에 이르면 가장 오래된 것부터 사라집니다.",
        "tr_tr": "Aynı anda kaç iz olabileceği. Sınıra ulaşıldığında önce en eskiler kaybolur."},
    "config.imprint.footprints.renderDistance": {
        "en_us": "Distance", "ru_ru": "Дальность", "zh_cn": "距离", "pt_br": "Distância", "es_es": "Distancia", "de_de": "Entfernung",
        "fr_fr": "Distance", "pl_pl": "Odległość", "uk_ua": "Дальність", "ja_jp": "距離", "ko_kr": "거리", "tr_tr": "Mesafe"},
    "config.imprint.footprints.renderDistance.tooltip": {
        "en_us": "Blocks from the camera within which prints are made and drawn.",
        "ru_ru": "На каком расстоянии от камеры (в блоках) следы появляются и рисуются.",
        "zh_cn": "距离镜头多少格以内会产生并绘制脚印。",
        "pt_br": "Distância da câmera, em blocos, em que pegadas são criadas e desenhadas.",
        "es_es": "Distancia a la cámara, en bloques, en la que se crean y dibujan huellas.",
        "de_de": "Abstand zur Kamera in Blöcken, in dem Spuren entstehen und gezeichnet werden.",
        "fr_fr": "Distance à la caméra, en blocs, dans laquelle les empreintes sont créées et affichées.",
        "pl_pl": "Odległość od kamery w blokach, w której ślady powstają i są rysowane.",
        "uk_ua": "На якій відстані від камери (у блоках) сліди з'являються і малюються.",
        "ja_jp": "カメラから何ブロック以内で足跡を作り、描画するか。",
        "ko_kr": "카메라로부터 몇 블록 안에서 발자국을 만들고 그릴지 정합니다.",
        "tr_tr": "Kameradan kaç blok içinde izlerin oluşup çizileceği."},
    "config.imprint.footprints.lifetimeMultiplier": {
        "en_us": "Print lifetime", "ru_ru": "Время жизни следов", "zh_cn": "脚印持续时间", "pt_br": "Duração das pegadas",
        "es_es": "Duración de las huellas", "de_de": "Lebensdauer der Spuren", "fr_fr": "Durée des empreintes", "pl_pl": "Czas życia śladów",
        "uk_ua": "Час життя слідів", "ja_jp": "足跡の持続時間", "ko_kr": "발자국 지속 시간", "tr_tr": "İz ömrü"},
    "config.imprint.footprints.lifetimeMultiplier.tooltip": {
        "en_us": "Multiplies how long prints stay before they fade (snow 5 min, sand 3 min at 1.0).",
        "ru_ru": "Множитель того, сколько следы держатся до исчезновения (при 1.0: снег 5 мин, песок 3 мин).",
        "zh_cn": "脚印消失前停留时间的倍数（1.0 时：雪地 5 分钟，沙地 3 分钟）。",
        "pt_br": "Multiplica quanto tempo as pegadas duram antes de sumir (em 1.0: neve 5 min, areia 3 min).",
        "es_es": "Multiplica cuánto duran las huellas antes de desvanecerse (con 1.0: nieve 5 min, arena 3 min).",
        "de_de": "Multipliziert, wie lange Spuren bleiben, bevor sie verblassen (bei 1.0: Schnee 5 Min., Sand 3 Min.).",
        "fr_fr": "Multiplie la durée des empreintes avant qu'elles s'effacent (à 1.0 : neige 5 min, sable 3 min).",
        "pl_pl": "Mnoży czas, przez jaki ślady trwają, zanim znikną (przy 1.0: śnieg 5 min, piasek 3 min).",
        "uk_ua": "Множник того, скільки сліди тримаються до зникнення (при 1.0: сніг 5 хв, пісок 3 хв).",
        "ja_jp": "足跡が消えるまでの時間の倍率（1.0 で雪 5 分、砂 3 分）。",
        "ko_kr": "발자국이 사라지기 전까지 남는 시간의 배수입니다(1.0일 때 눈 5분, 모래 3분).",
        "tr_tr": "İzlerin solmadan önce ne kadar kalacağının çarpanı (1.0'da kar 5 dk, kum 3 dk)."},
    "config.imprint.footprints.opacity": {
        "en_us": "Visibility", "ru_ru": "Заметность", "zh_cn": "清晰度", "pt_br": "Visibilidade", "es_es": "Visibilidad", "de_de": "Sichtbarkeit",
        "fr_fr": "Visibilité", "pl_pl": "Widoczność", "uk_ua": "Помітність", "ja_jp": "見やすさ", "ko_kr": "선명도", "tr_tr": "Görünürlük"},
    "config.imprint.footprints.opacity.tooltip": {
        "en_us": "How strongly prints show.", "ru_ru": "Насколько сильно видны следы.", "zh_cn": "脚印显示的明显程度。",
        "pt_br": "Quão marcadas as pegadas aparecem.", "es_es": "Cuán marcadas se ven las huellas.", "de_de": "Wie deutlich Spuren zu sehen sind.",
        "fr_fr": "Intensité d'affichage des empreintes.", "pl_pl": "Jak wyraźnie widać ślady.", "uk_ua": "Наскільки сильно видно сліди.",
        "ja_jp": "足跡がどれだけはっきり見えるか。", "ko_kr": "발자국이 얼마나 뚜렷하게 보이는지.", "tr_tr": "İzlerin ne kadar belirgin görüneceği."},
    "config.imprint.footprints.entityBlacklist": {
        "en_us": "No prints for", "ru_ru": "Без следов", "zh_cn": "不留脚印的实体", "pt_br": "Sem pegadas para", "es_es": "Sin huellas para",
        "de_de": "Keine Spuren für", "fr_fr": "Pas d'empreintes pour", "pl_pl": "Bez śladów dla", "uk_ua": "Без слідів", "ja_jp": "足跡を残さない",
        "ko_kr": "발자국 제외", "tr_tr": "İz bırakmayanlar"},
    "config.imprint.footprints.entityBlacklist.tooltip": {
        "en_us": "Entity ids that never leave prints, separated by commas, e.g. minecraft:villager.",
        "ru_ru": "ID существ, которые никогда не оставляют следов, через запятую, например minecraft:villager.",
        "zh_cn": "永远不留脚印的实体 ID，用逗号分隔，例如 minecraft:villager。",
        "pt_br": "IDs de entidades que nunca deixam pegadas, separados por vírgula, ex.: minecraft:villager.",
        "es_es": "IDs de entidades que nunca dejan huellas, separados por comas, p. ej. minecraft:villager.",
        "de_de": "Entity-IDs, die nie Spuren hinterlassen, durch Kommas getrennt, z. B. minecraft:villager.",
        "fr_fr": "Identifiants d'entités qui ne laissent jamais d'empreintes, séparés par des virgules, ex. minecraft:villager.",
        "pl_pl": "ID stworzeń, które nigdy nie zostawiają śladów, oddzielone przecinkami, np. minecraft:villager.",
        "uk_ua": "ID істот, які ніколи не залишають слідів, через кому, наприклад minecraft:villager.",
        "ja_jp": "足跡を残さないエンティティ ID（カンマ区切り）。例: minecraft:villager",
        "ko_kr": "절대 발자국을 남기지 않는 엔티티 ID(쉼표로 구분). 예: minecraft:villager",
        "tr_tr": "Asla iz bırakmayan varlık kimlikleri, virgülle ayrılmış; ör. minecraft:villager."},
    "imprint.command.reloaded": {
        "en_us": "Imprint settings reloaded.", "ru_ru": "Настройки Imprint перезагружены.", "zh_cn": "Imprint 设置已重新加载。",
        "pt_br": "Configurações do Imprint recarregadas.", "es_es": "Ajustes de Imprint recargados.", "de_de": "Imprint-Einstellungen neu geladen.",
        "fr_fr": "Paramètres d'Imprint rechargés.", "pl_pl": "Ustawienia Imprint wczytane ponownie.", "uk_ua": "Налаштування Imprint перезавантажено.",
        "ja_jp": "Imprint の設定を再読み込みしました。", "ko_kr": "Imprint 설정을 다시 불러왔습니다.", "tr_tr": "Imprint ayarları yeniden yüklendi."},
    "imprint.command.problems": {
        "en_us": "Imprint settings: %s problem(s), see below. Fix the file and run /imprint reload again.",
        "ru_ru": "Настройки Imprint: проблем — %s, подробности ниже. Исправьте файл и снова выполните /imprint reload.",
        "zh_cn": "Imprint 设置：%s 个问题，详见下方。修正文件后再次执行 /imprint reload。",
        "pt_br": "Configurações do Imprint: %s problema(s), veja abaixo. Corrija o arquivo e rode /imprint reload de novo.",
        "es_es": "Ajustes de Imprint: %s problema(s), mira abajo. Corrige el archivo y ejecuta /imprint reload otra vez.",
        "de_de": "Imprint-Einstellungen: %s Problem(e), siehe unten. Datei korrigieren und /imprint reload erneut ausführen.",
        "fr_fr": "Paramètres d'Imprint : %s problème(s), voir ci-dessous. Corrigez le fichier puis relancez /imprint reload.",
        "pl_pl": "Ustawienia Imprint: problemów: %s, szczegóły poniżej. Popraw plik i ponownie wpisz /imprint reload.",
        "uk_ua": "Налаштування Imprint: проблем — %s, подробиці нижче. Виправте файл і знову виконайте /imprint reload.",
        "ja_jp": "Imprint の設定: 問題が %s 件あります（下記参照）。ファイルを直して再度 /imprint reload を実行してください。",
        "ko_kr": "Imprint 설정: 문제 %s건, 아래를 확인하세요. 파일을 고친 뒤 /imprint reload를 다시 실행하세요.",
        "tr_tr": "Imprint ayarları: %s sorun, aşağıya bakın. Dosyayı düzeltip /imprint reload komutunu yeniden çalıştırın."},
    "imprint.command.stats": {
        "en_us": "Imprint: %s prints of %s, drawn last frame: %s.",
        "ru_ru": "Imprint: следов %s из %s, нарисовано в последнем кадре: %s.",
        "zh_cn": "Imprint：脚印 %s / %s，上一帧绘制：%s。",
        "pt_br": "Imprint: %s pegadas de %s, desenhadas no último quadro: %s.",
        "es_es": "Imprint: %s huellas de %s, dibujadas en el último fotograma: %s.",
        "de_de": "Imprint: %s von %s Spuren, im letzten Bild gezeichnet: %s.",
        "fr_fr": "Imprint : %s empreintes sur %s, affichées à la dernière image : %s.",
        "pl_pl": "Imprint: śladów %s z %s, narysowano w ostatniej klatce: %s.",
        "uk_ua": "Imprint: слідів %s з %s, намальовано в останньому кадрі: %s.",
        "ja_jp": "Imprint: 足跡 %s / %s、直前のフレームで描画: %s。",
        "ko_kr": "Imprint: 발자국 %s / %s, 지난 프레임에 그림: %s.",
        "tr_tr": "Imprint: %s / %s iz, son karede çizilen: %s."},
}

PH = re.compile(r"%(?:\d+\$)?[sd]")


def write():
    OUT.mkdir(parents=True, exist_ok=True)
    for lang in LANGS:
        data = {k: v[lang] for k, v in T.items()}
        (OUT / f"{lang}.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{len(LANGS)} languages x {len(T)} keys -> {OUT}")


def check() -> int:
    errors = []
    ref = json.loads((OUT / "en_us.json").read_text(encoding="utf-8"))
    for lang in LANGS:
        path = OUT / f"{lang}.json"
        if not path.exists():
            errors.append(f"{lang}: file missing")
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        for key, text in ref.items():
            if key not in data or not str(data[key]).strip():
                errors.append(f"{lang}: missing {key}")
            elif sorted(PH.findall(text)) != sorted(PH.findall(data[key])):
                errors.append(f"{lang}: placeholders differ in {key}")
        for key in data:
            if key not in ref:
                errors.append(f"{lang}: extra key {key}")
    for e in errors:
        print("  " + e)
    print("lang check: " + ("OK" if not errors else f"{len(errors)} problem(s)"))
    return 1 if errors else 0


if __name__ == "__main__":
    if "--check" in sys.argv:
        sys.exit(check())
    write()
    sys.exit(check())
