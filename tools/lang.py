"""Footprints translations: one table, twelve lang files, and the completeness check.

    python -I tools/lang.py          write src/main/resources/assets/farmstead_footprints/lang/*.json
    python -I tools/lang.py --check  fail on a missing language, key or placeholder mismatch

en_us and ru_ru are the references; every other language must carry every key with the same
%s / %d placeholders. Terms: "print/footprint" (not "track"), "preset", "Footprints" stays as is.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/farmstead_footprints/lang"
LANGS = ["en_us", "ru_ru", "zh_cn", "pt_br", "es_es", "de_de", "fr_fr", "pl_pl", "uk_ua", "ja_jp", "ko_kr", "tr_tr"]

T = {
    "config.farmstead_footprints.title": {
        "en_us": "Footprints settings", "ru_ru": "Настройки Footprints", "zh_cn": "Footprints 设置", "pt_br": "Configurações do Footprints",
        "es_es": "Ajustes de Footprints", "de_de": "Footprints-Einstellungen", "fr_fr": "Paramètres de Footprints", "pl_pl": "Ustawienia Footprints",
        "uk_ua": "Налаштування Footprints", "ja_jp": "Footprints の設定", "ko_kr": "Footprints 설정", "tr_tr": "Footprints ayarları"},
    "config.farmstead_footprints.preset": {
        "en_us": "Preset", "ru_ru": "Пресет", "zh_cn": "预设", "pt_br": "Predefinição", "es_es": "Preajuste", "de_de": "Voreinstellung",
        "fr_fr": "Préréglage", "pl_pl": "Ustawienie wstępne", "uk_ua": "Пресет", "ja_jp": "プリセット", "ko_kr": "프리셋", "tr_tr": "Ön ayar"},
    "config.farmstead_footprints.preset.tooltip": {
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
    "config.farmstead_footprints.preset.default": {
        "en_us": "Default", "ru_ru": "По умолчанию", "zh_cn": "默认", "pt_br": "Padrão", "es_es": "Predeterminado", "de_de": "Standard",
        "fr_fr": "Par défaut", "pl_pl": "Domyślne", "uk_ua": "За замовчуванням", "ja_jp": "標準", "ko_kr": "기본", "tr_tr": "Varsayılan"},
    "config.farmstead_footprints.preset.vanilla_plus": {
        "en_us": "Vanilla+", "ru_ru": "Ваниль+", "zh_cn": "原版+", "pt_br": "Vanilla+", "es_es": "Vanilla+", "de_de": "Vanilla+",
        "fr_fr": "Vanilla+", "pl_pl": "Vanilla+", "uk_ua": "Ваніль+", "ja_jp": "バニラ+", "ko_kr": "바닐라+", "tr_tr": "Vanilla+"},
    "config.farmstead_footprints.preset.cinematic": {
        "en_us": "Cinematic", "ru_ru": "Кинематограф", "zh_cn": "电影感", "pt_br": "Cinematográfico", "es_es": "Cinemático",
        "de_de": "Filmisch", "fr_fr": "Cinématique", "pl_pl": "Filmowe", "uk_ua": "Кінематограф", "ja_jp": "シネマティック",
        "ko_kr": "시네마틱", "tr_tr": "Sinematik"},
    "config.farmstead_footprints.preset.performance": {
        "en_us": "Performance", "ru_ru": "Производительность", "zh_cn": "性能优先", "pt_br": "Desempenho", "es_es": "Rendimiento",
        "de_de": "Leistung", "fr_fr": "Performances", "pl_pl": "Wydajność", "uk_ua": "Продуктивність", "ja_jp": "パフォーマンス",
        "ko_kr": "성능", "tr_tr": "Performans"},
    "config.farmstead_footprints.general.enabled": {
        "en_us": "Enabled", "ru_ru": "Включено", "zh_cn": "启用", "pt_br": "Ativado", "es_es": "Activado", "de_de": "Aktiviert",
        "fr_fr": "Activé", "pl_pl": "Włączone", "uk_ua": "Увімкнено", "ja_jp": "有効", "ko_kr": "사용", "tr_tr": "Etkin"},
    "config.farmstead_footprints.general.enabled.tooltip": {
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
    "config.farmstead_footprints.general.checkUpdates": {
        "en_us": "Check for updates", "ru_ru": "Проверять обновления", "zh_cn": "检查更新",
        "pt_br": "Verificar atualizações", "es_es": "Buscar actualizaciones", "de_de": "Nach Updates suchen",
        "fr_fr": "Rechercher les mises à jour", "pl_pl": "Sprawdzaj aktualizacje", "uk_ua": "Перевіряти оновлення",
        "ja_jp": "更新を確認", "ko_kr": "업데이트 확인", "tr_tr": "Güncellemeleri denetle"},
    "config.farmstead_footprints.general.checkUpdates.tooltip": {
        "en_us": "Once at start-up, ask Modrinth whether a newer Footprints is out and say so in the log. Sends nothing about you.",
        "ru_ru": "Один раз при запуске спросить Modrinth, не вышла ли новая версия Footprints, и написать об этом в лог. О вас ничего не отправляется.",
        "zh_cn": "启动时向 Modrinth 查询一次是否有新版 Footprints，并写入日志。不会发送任何关于你的信息。",
        "pt_br": "Ao iniciar, pergunta uma vez ao Modrinth se há uma versão nova do Footprints e avisa no log. Nada sobre você é enviado.",
        "es_es": "Al arrancar, pregunta una vez a Modrinth si hay una versión nueva de Footprints y lo indica en el registro. No envía nada sobre ti.",
        "de_de": "Fragt beim Start einmal bei Modrinth nach einer neueren Footprints-Version und meldet sie im Log. Über dich wird nichts gesendet.",
        "fr_fr": "Au démarrage, demande une fois à Modrinth si une nouvelle version de Footprints est sortie et l'indique dans le journal. Rien sur vous n'est envoyé.",
        "pl_pl": "Przy starcie raz pyta Modrinth, czy jest nowsza wersja Footprints, i zapisuje to w logu. Nic o tobie nie jest wysyłane.",
        "uk_ua": "Один раз під час запуску запитати Modrinth, чи вийшла нова версія Footprints, і написати про це в лог. Про вас нічого не надсилається.",
        "ja_jp": "起動時に一度だけ Modrinth に Footprints の新しいバージョンがあるか確認し、ログに書き出します。あなたに関する情報は送信しません。",
        "ko_kr": "시작할 때 한 번 Modrinth에 Footprints 새 버전이 있는지 확인하고 로그에 남깁니다. 사용자에 대한 정보는 보내지 않습니다.",
        "tr_tr": "Açılışta bir kez Modrinth'e yeni bir Footprints sürümü olup olmadığını sorar ve günlüğe yazar. Hakkınızda hiçbir şey gönderilmez."},
    "config.farmstead_footprints.footprints.players": {
        "en_us": "Player prints", "ru_ru": "Следы игроков", "zh_cn": "玩家脚印", "pt_br": "Pegadas de jogadores", "es_es": "Huellas de jugadores",
        "de_de": "Spuren von Spielern", "fr_fr": "Empreintes des joueurs", "pl_pl": "Ślady graczy", "uk_ua": "Сліди гравців",
        "ja_jp": "プレイヤーの足跡", "ko_kr": "플레이어 발자국", "tr_tr": "Oyuncu izleri"},
    "config.farmstead_footprints.footprints.players.tooltip": {
        "en_us": "Prints of players, yourself included.", "ru_ru": "Следы игроков, в том числе ваши.", "zh_cn": "玩家的脚印，包括你自己。",
        "pt_br": "Pegadas de jogadores, incluindo as suas.", "es_es": "Huellas de jugadores, incluidas las tuyas.",
        "de_de": "Spuren von Spielern, deine eigenen eingeschlossen.", "fr_fr": "Empreintes des joueurs, y compris les vôtres.",
        "pl_pl": "Ślady graczy, także twoje.", "uk_ua": "Сліди гравців, зокрема ваші.", "ja_jp": "プレイヤーの足跡（自分を含む）。",
        "ko_kr": "플레이어의 발자국(자신 포함).", "tr_tr": "Oyuncuların izleri, sizinkiler dahil."},
    "config.farmstead_footprints.footprints.mobs": {
        "en_us": "Mob prints", "ru_ru": "Следы мобов", "zh_cn": "生物脚印", "pt_br": "Pegadas de criaturas", "es_es": "Huellas de criaturas",
        "de_de": "Spuren von Kreaturen", "fr_fr": "Empreintes des créatures", "pl_pl": "Ślady stworzeń", "uk_ua": "Сліди мобів",
        "ja_jp": "モブの足跡", "ko_kr": "몹 발자국", "tr_tr": "Yaratık izleri"},
    "config.farmstead_footprints.footprints.mobs.tooltip": {
        "en_us": "Prints of animals and monsters.", "ru_ru": "Следы животных и монстров.", "zh_cn": "动物和怪物的脚印。",
        "pt_br": "Pegadas de animais e monstros.", "es_es": "Huellas de animales y monstruos.", "de_de": "Spuren von Tieren und Monstern.",
        "fr_fr": "Empreintes des animaux et des monstres.", "pl_pl": "Ślady zwierząt i potworów.", "uk_ua": "Сліди тварин і монстрів.",
        "ja_jp": "動物とモンスターの足跡。", "ko_kr": "동물과 몬스터의 발자국.", "tr_tr": "Hayvan ve canavar izleri."},
    "config.farmstead_footprints.footprints.maxPrints": {
        "en_us": "Max prints", "ru_ru": "Предел следов", "zh_cn": "脚印上限", "pt_br": "Máximo de pegadas", "es_es": "Máximo de huellas",
        "de_de": "Maximale Spuren", "fr_fr": "Empreintes max.", "pl_pl": "Maks. śladów", "uk_ua": "Межа слідів", "ja_jp": "足跡の最大数",
        "ko_kr": "최대 발자국 수", "tr_tr": "En fazla iz"},
    "config.farmstead_footprints.footprints.maxPrints.tooltip": {
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
    "config.farmstead_footprints.footprints.renderDistance": {
        "en_us": "Distance", "ru_ru": "Дальность", "zh_cn": "距离", "pt_br": "Distância", "es_es": "Distancia", "de_de": "Entfernung",
        "fr_fr": "Distance", "pl_pl": "Odległość", "uk_ua": "Дальність", "ja_jp": "距離", "ko_kr": "거리", "tr_tr": "Mesafe"},
    "config.farmstead_footprints.footprints.renderDistance.tooltip": {
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
    "config.farmstead_footprints.footprints.lifetimeMultiplier": {
        "en_us": "Print lifetime", "ru_ru": "Время жизни следов", "zh_cn": "脚印持续时间", "pt_br": "Duração das pegadas",
        "es_es": "Duración de las huellas", "de_de": "Lebensdauer der Spuren", "fr_fr": "Durée des empreintes", "pl_pl": "Czas życia śladów",
        "uk_ua": "Час життя слідів", "ja_jp": "足跡の持続時間", "ko_kr": "발자국 지속 시간", "tr_tr": "İz ömrü"},
    "config.farmstead_footprints.footprints.lifetimeMultiplier.tooltip": {
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
    "config.farmstead_footprints.footprints.opacity": {
        "en_us": "Visibility", "ru_ru": "Заметность", "zh_cn": "清晰度", "pt_br": "Visibilidade", "es_es": "Visibilidad", "de_de": "Sichtbarkeit",
        "fr_fr": "Visibilité", "pl_pl": "Widoczność", "uk_ua": "Помітність", "ja_jp": "見やすさ", "ko_kr": "선명도", "tr_tr": "Görünürlük"},
    "config.farmstead_footprints.footprints.opacity.tooltip": {
        "en_us": "How strongly prints show.", "ru_ru": "Насколько сильно видны следы.", "zh_cn": "脚印显示的明显程度。",
        "pt_br": "Quão marcadas as pegadas aparecem.", "es_es": "Cuán marcadas se ven las huellas.", "de_de": "Wie deutlich Spuren zu sehen sind.",
        "fr_fr": "Intensité d'affichage des empreintes.", "pl_pl": "Jak wyraźnie widać ślady.", "uk_ua": "Наскільки сильно видно сліди.",
        "ja_jp": "足跡がどれだけはっきり見えるか。", "ko_kr": "발자국이 얼마나 뚜렷하게 보이는지.", "tr_tr": "İzlerin ne kadar belirgin görüneceği."},
    "config.farmstead_footprints.footprints.entityBlacklist": {
        "en_us": "No prints for", "ru_ru": "Без следов", "zh_cn": "不留脚印的实体", "pt_br": "Sem pegadas para", "es_es": "Sin huellas para",
        "de_de": "Keine Spuren für", "fr_fr": "Pas d'empreintes pour", "pl_pl": "Bez śladów dla", "uk_ua": "Без слідів", "ja_jp": "足跡を残さない",
        "ko_kr": "발자국 제외", "tr_tr": "İz bırakmayanlar"},
    "config.farmstead_footprints.footprints.entityBlacklist.tooltip": {
        "en_us": "Entities that never leave prints, by id or #tag, separated by commas. Flying and footless mobs are listed by default.",
        "ru_ru": "Существа, которые никогда не оставляют следов: ID или #тег через запятую. Летающие и безногие мобы уже в списке.",
        "zh_cn": "永远不留脚印的实体，填写 ID 或 #标签，用逗号分隔。默认已列出会飞和没有脚的生物。",
        "pt_br": "Entidades que nunca deixam pegadas, por ID ou #tag, separadas por vírgula. Mobs voadores e sem pés já vêm na lista.",
        "es_es": "Entidades que nunca dejan huellas, por ID o #etiqueta, separadas por comas. Los mobs voladores y sin patas ya están en la lista.",
        "de_de": "Wesen, die nie Spuren hinterlassen, per ID oder #Tag, durch Kommas getrennt. Fliegende und fußlose Mobs stehen schon drin.",
        "fr_fr": "Entités qui ne laissent jamais d'empreintes, par identifiant ou #tag, séparées par des virgules. Les créatures volantes ou sans pattes y sont déjà.",
        "pl_pl": "Stworzenia, które nigdy nie zostawiają śladów: ID lub #tag, oddzielone przecinkami. Latające i beznogie moby są już na liście.",
        "uk_ua": "Істоти, які ніколи не залишають слідів: ID або #тег через кому. Літаючі й безногі моби вже в списку.",
        "ja_jp": "足跡を残さないエンティティ（ID または #タグ、カンマ区切り）。飛ぶモブと足のないモブは最初から入っています。",
        "ko_kr": "발자국을 남기지 않는 엔티티(ID 또는 #태그, 쉼표로 구분). 나는 몹과 발 없는 몹은 기본으로 들어 있습니다.",
        "tr_tr": "Asla iz bırakmayan varlıklar: kimlik veya #etiket, virgülle ayrılmış. Uçan ve ayaksız moblar varsayılan olarak listede."},
    "config.farmstead_footprints.footprints.sneaking": {
        "en_us": "Sneaking", "ru_ru": "Крадучись", "zh_cn": "潜行时", "pt_br": "Agachado", "es_es": "Agachado", "de_de": "Schleichen",
        "fr_fr": "Accroupi", "pl_pl": "Skradanie", "uk_ua": "Крадькома", "ja_jp": "スニーク中", "ko_kr": "웅크리기", "tr_tr": "Sinsice"},
    "config.farmstead_footprints.footprints.sneaking.tooltip": {
        "en_us": "Sneaking feet leave shallow prints, or none at all (stealth).",
        "ru_ru": "Крадущийся оставляет мелкие следы или не оставляет их вовсе (скрытность).",
        "zh_cn": "潜行时留下浅脚印，或者完全不留（隐蔽）。",
        "pt_br": "Quem anda agachado deixa pegadas rasas, ou nenhuma (furtividade).",
        "es_es": "Al ir agachado se dejan huellas poco profundas, o ninguna (sigilo).",
        "de_de": "Beim Schleichen entstehen flache Spuren oder gar keine (Tarnung).",
        "fr_fr": "En étant accroupi, on laisse des empreintes peu profondes, ou aucune (discrétion).",
        "pl_pl": "Skradanie zostawia płytkie ślady albo żadnych (ukrycie).",
        "uk_ua": "Хто крадеться, лишає мілкі сліди або не лишає їх зовсім (скритність).",
        "ja_jp": "スニーク中は浅い足跡を残すか、まったく残しません（隠密）。",
        "ko_kr": "웅크리고 걸으면 얕은 발자국을 남기거나 아예 남기지 않습니다(은신).",
        "tr_tr": "Sinsice yürürken sığ iz bırakılır ya da hiç bırakılmaz (gizlilik)."},
    "config.farmstead_footprints.footprints.sneaking.shallow": {
        "en_us": "Shallow prints", "ru_ru": "Мелкие следы", "zh_cn": "浅脚印", "pt_br": "Pegadas rasas", "es_es": "Huellas leves",
        "de_de": "Flache Spuren", "fr_fr": "Empreintes légères", "pl_pl": "Płytkie ślady", "uk_ua": "Мілкі сліди", "ja_jp": "浅い足跡",
        "ko_kr": "얕은 발자국", "tr_tr": "Sığ izler"},
    "config.farmstead_footprints.footprints.sneaking.none": {
        "en_us": "No prints", "ru_ru": "Без следов", "zh_cn": "不留脚印", "pt_br": "Sem pegadas", "es_es": "Sin huellas",
        "de_de": "Keine Spuren", "fr_fr": "Aucune empreinte", "pl_pl": "Bez śladów", "uk_ua": "Без слідів", "ja_jp": "足跡なし",
        "ko_kr": "발자국 없음", "tr_tr": "İz yok"},
    "config.farmstead_footprints.footprints.invisible": {
        "en_us": "Invisible leave prints", "ru_ru": "Следы невидимых", "zh_cn": "隐身者留脚印", "pt_br": "Invisíveis deixam pegadas",
        "es_es": "Invisibles dejan huellas", "de_de": "Unsichtbare hinterlassen Spuren", "fr_fr": "Empreintes des invisibles",
        "pl_pl": "Niewidzialni zostawiają ślady", "uk_ua": "Сліди невидимих", "ja_jp": "透明でも足跡", "ko_kr": "투명 상태 발자국",
        "tr_tr": "Görünmezler iz bırakır"},
    "config.farmstead_footprints.footprints.invisible.tooltip": {
        "en_us": "Invisible players and mobs leave prints too.",
        "ru_ru": "Невидимые игроки и мобы тоже оставляют следы.",
        "zh_cn": "隐身的玩家和生物也会留下脚印。",
        "pt_br": "Jogadores e mobs invisíveis também deixam pegadas.",
        "es_es": "Los jugadores y mobs invisibles también dejan huellas.",
        "de_de": "Auch unsichtbare Spieler und Mobs hinterlassen Spuren.",
        "fr_fr": "Les joueurs et créatures invisibles laissent aussi des empreintes.",
        "pl_pl": "Niewidzialni gracze i moby też zostawiają ślady.",
        "uk_ua": "Невидимі гравці й моби теж залишають сліди.",
        "ja_jp": "透明なプレイヤーやモブも足跡を残します。",
        "ko_kr": "투명한 플레이어와 몹도 발자국을 남깁니다.",
        "tr_tr": "Görünmez oyuncular ve moblar da iz bırakır."},
    "config.farmstead_footprints.footprints.profiles": {
        "en_us": "Prints per mob", "ru_ru": "Следы по мобам", "zh_cn": "按生物设置脚印", "pt_br": "Pegadas por mob", "es_es": "Huellas por mob",
        "de_de": "Spuren je Mob", "fr_fr": "Empreintes par créature", "pl_pl": "Ślady dla mobów", "uk_ua": "Сліди за мобами",
        "ja_jp": "モブごとの足跡", "ko_kr": "몹별 발자국", "tr_tr": "Moba göre izler"},
    "config.farmstead_footprints.footprints.profiles.tooltip": {
        "en_us": "Your own rules per mob or #tag, separated by commas, e.g. minecraft:fox = shape:paw size:0.8. Shapes: boot, bare, paw, cat, hoof, cloven, bird, big, bear, or your own picture from config/farmstead_footprints/textures.",
        "ru_ru": "Свои правила для моба или #тега через запятую, например minecraft:fox = shape:paw size:0.8. Формы: boot, bare, paw, cat, hoof, cloven, bird, big, bear или своя картинка из config/farmstead_footprints/textures.",
        "zh_cn": "按生物或 #标签自定义，用逗号分隔，例如 minecraft:fox = shape:paw size:0.8。形状：boot、bare、paw、cat、hoof、cloven、bird、big、bear，或 config/farmstead_footprints/textures 里你自己的图片。",
        "pt_br": "Suas regras por mob ou #tag, separadas por vírgula, ex.: minecraft:fox = shape:paw size:0.8. Formas: boot, bare, paw, cat, hoof, cloven, bird, big, bear ou sua própria imagem em config/farmstead_footprints/textures.",
        "es_es": "Tus reglas por mob o #etiqueta, separadas por comas, p. ej. minecraft:fox = shape:paw size:0.8. Formas: boot, bare, paw, cat, hoof, cloven, bird, big, bear o tu propia imagen de config/farmstead_footprints/textures.",
        "de_de": "Eigene Regeln je Mob oder #Tag, durch Kommas getrennt, z. B. minecraft:fox = shape:paw size:0.8. Formen: boot, bare, paw, cat, hoof, cloven, bird, big, bear oder ein eigenes Bild aus config/farmstead_footprints/textures.",
        "fr_fr": "Vos règles par créature ou #tag, séparées par des virgules, ex. minecraft:fox = shape:paw size:0.8. Formes : boot, bare, paw, cat, hoof, cloven, bird, big, bear, ou votre image dans config/farmstead_footprints/textures.",
        "pl_pl": "Własne reguły dla moba lub #tagu, oddzielone przecinkami, np. minecraft:fox = shape:paw size:0.8. Kształty: boot, bare, paw, cat, hoof, cloven, bird, big, bear albo własny obrazek z config/farmstead_footprints/textures.",
        "uk_ua": "Свої правила для моба або #тегу через кому, наприклад minecraft:fox = shape:paw size:0.8. Форми: boot, bare, paw, cat, hoof, cloven, bird, big, bear або власна картинка з config/farmstead_footprints/textures.",
        "ja_jp": "モブまたは #タグごとの独自ルール（カンマ区切り）。例: minecraft:fox = shape:paw size:0.8。形: boot, bare, paw, cat, hoof, cloven, bird, big, bear、または config/farmstead_footprints/textures に置いた自分の画像。",
        "ko_kr": "몹 또는 #태그별 규칙(쉼표로 구분). 예: minecraft:fox = shape:paw size:0.8. 모양: boot, bare, paw, cat, hoof, cloven, bird, big, bear 또는 config/farmstead_footprints/textures에 넣은 내 그림.",
        "tr_tr": "Mob veya #etikete göre kendi kuralların, virgülle ayrılmış; ör. minecraft:fox = shape:paw size:0.8. Şekiller: boot, bare, paw, cat, hoof, cloven, bird, big, bear veya config/farmstead_footprints/textures içindeki kendi resmin."},
    "config.farmstead_footprints.effects.wetPrints": {
        "en_us": "Wet prints",
        "ru_ru": "Мокрые следы",
        "zh_cn": "湿脚印",
        "pt_br": "Pegadas molhadas",
        "es_es": "Huellas mojadas",
        "de_de": "Nasse Spuren",
        "fr_fr": "Empreintes mouillées",
        "pl_pl": "Mokre ślady",
        "uk_ua": "Мокрі сліди",
        "ja_jp": "濡れた足跡",
        "ko_kr": "젖은 발자국",
        "tr_tr": "Islak izler"},
    "config.farmstead_footprints.effects.wetPrints.tooltip": {
        "en_us": "After water or rain, feet leave damp prints on hard ground too (stone, planks) that dry in half a minute.",
        "ru_ru": "После воды или дождя ноги оставляют влажные следы и на твёрдом полу (камень, доски); они высыхают за полминуты.",
        "zh_cn": "从水里出来或淋雨后，在坚硬的地面（石头、木板）上也会留下湿脚印，半分钟内变干。",
        "pt_br": "Depois da água ou da chuva, os pés deixam pegadas úmidas até em chão duro (pedra, tábuas), que secam em meio minuto.",
        "es_es": "Tras el agua o la lluvia, los pies dejan huellas húmedas también en suelo duro (piedra, tablones) que se secan en medio minuto.",
        "de_de": "Nach Wasser oder Regen hinterlassen Füße auch auf hartem Boden (Stein, Bretter) feuchte Spuren, die in einer halben Minute trocknen.",
        "fr_fr": "Après l'eau ou la pluie, les pieds laissent aussi des empreintes humides sur sol dur (pierre, planches), sèches en une demi-minute.",
        "pl_pl": "Po wodzie lub deszczu stopy zostawiają wilgotne ślady także na twardym podłożu (kamień, deski), które schną w pół minuty.",
        "uk_ua": "Після води чи дощу ноги лишають вологі сліди й на твердій підлозі (камінь, дошки); вони висихають за пів хвилини.",
        "ja_jp": "水から出たり雨に当たったりした後は、硬い床（石や板）にも湿った足跡が付き、30 秒ほどで乾きます。",
        "ko_kr": "물에서 나오거나 비를 맞은 뒤에는 단단한 바닥(돌, 판자)에도 젖은 발자국이 남고 30초 안에 마릅니다.",
        "tr_tr": "Sudan ya da yağmurdan sonra ayaklar sert zeminde de (taş, tahta) yarım dakikada kuruyan ıslak izler bırakır."},
    "config.farmstead_footprints.effects.wetSeconds": {
        "en_us": "Feet stay wet",
        "ru_ru": "Ноги мокрые",
        "zh_cn": "脚保持湿润",
        "pt_br": "Pés molhados por",
        "es_es": "Pies mojados durante",
        "de_de": "Füße bleiben nass",
        "fr_fr": "Pieds mouillés pendant",
        "pl_pl": "Stopy mokre przez",
        "uk_ua": "Ноги мокрі",
        "ja_jp": "足が濡れている時間",
        "ko_kr": "발이 젖어 있는 시간",
        "tr_tr": "Ayaklar ıslak kalır"},
    "config.farmstead_footprints.effects.wetSeconds.tooltip": {
        "en_us": "How long feet stay wet after leaving water or rain, in seconds.",
        "ru_ru": "Сколько секунд ноги остаются мокрыми после воды или дождя.",
        "zh_cn": "离开水或雨后，脚保持湿润的秒数。",
        "pt_br": "Por quantos segundos os pés ficam molhados depois da água ou da chuva.",
        "es_es": "Cuántos segundos siguen mojados los pies tras el agua o la lluvia.",
        "de_de": "Wie viele Sekunden die Füße nach Wasser oder Regen nass bleiben.",
        "fr_fr": "Combien de secondes les pieds restent mouillés après l'eau ou la pluie.",
        "pl_pl": "Ile sekund stopy pozostają mokre po wodzie lub deszczu.",
        "uk_ua": "Скільки секунд ноги лишаються мокрими після води чи дощу.",
        "ja_jp": "水や雨のあと足が濡れている秒数。",
        "ko_kr": "물이나 비를 맞은 뒤 발이 젖어 있는 시간(초).",
        "tr_tr": "Sudan ya da yağmurdan sonra ayakların kaç saniye ıslak kalacağı."},
    "config.farmstead_footprints.effects.rainWear": {
        "en_us": "Rain wears prints",
        "ru_ru": "Дождь стирает следы",
        "zh_cn": "雨水冲刷脚印",
        "pt_br": "Chuva apaga pegadas",
        "es_es": "La lluvia borra huellas",
        "de_de": "Regen verwischt Spuren",
        "fr_fr": "La pluie efface",
        "pl_pl": "Deszcz zaciera ślady",
        "uk_ua": "Дощ стирає сліди",
        "ja_jp": "雨で足跡が消える",
        "ko_kr": "비가 발자국을 지움",
        "tr_tr": "Yağmur izleri siler"},
    "config.farmstead_footprints.effects.rainWear.tooltip": {
        "en_us": "Prints under open sky age this many times faster while it rains or snows. 1: weather does nothing.",
        "ru_ru": "Во время дождя или снегопада следы под открытым небом стареют во столько раз быстрее. 1 — погода не влияет.",
        "zh_cn": "下雨或下雪时，露天的脚印以这个倍数更快消失。1：天气没有影响。",
        "pt_br": "Com chuva ou neve, pegadas a céu aberto envelhecem tantas vezes mais rápido. 1: o clima não afeta.",
        "es_es": "Con lluvia o nieve, las huellas a cielo abierto envejecen tantas veces más rápido. 1: el tiempo no influye.",
        "de_de": "Bei Regen oder Schneefall altern Spuren unter freiem Himmel so viel schneller. 1: Wetter wirkt nicht.",
        "fr_fr": "Sous la pluie ou la neige, les empreintes à ciel ouvert vieillissent autant de fois plus vite. 1 : la météo n'agit pas.",
        "pl_pl": "W deszczu lub śniegu ślady pod gołym niebem starzeją się tyle razy szybciej. 1: pogoda nie działa.",
        "uk_ua": "Під час дощу чи снігопаду сліди під відкритим небом старіють у стільки разів швидше. 1 — погода не впливає.",
        "ja_jp": "雨や雪の間、屋外の足跡はこの倍率で早く消えます。1：天気の影響なし。",
        "ko_kr": "비나 눈이 오는 동안 하늘 아래 발자국은 이 배수만큼 빨리 사라집니다. 1: 날씨 영향 없음.",
        "tr_tr": "Yağmur ya da kar yağarken açık havadaki izler bu kadar kat hızlı eskir. 1: hava etkilemez."},
    "config.farmstead_footprints.effects.dust": {
        "en_us": "Dust under feet",
        "ru_ru": "Пыль из-под ног",
        "zh_cn": "脚下扬尘",
        "pt_br": "Poeira sob os pés",
        "es_es": "Polvo bajo los pies",
        "de_de": "Staub unter den Füßen",
        "fr_fr": "Poussière sous les pieds",
        "pl_pl": "Pył spod stóp",
        "uk_ua": "Пил з-під ніг",
        "ja_jp": "足元の土ぼこり",
        "ko_kr": "발밑 먼지",
        "tr_tr": "Ayak altından toz"},
    "config.farmstead_footprints.effects.dust.tooltip": {
        "en_us": "A puff of snow, sand or dust from under the feet on loose ground.",
        "ru_ru": "Облачко снега, песка или пыли из-под ног на рыхлом грунте.",
        "zh_cn": "在松软的地面上，脚下扬起一点雪、沙子或尘土。",
        "pt_br": "Uma nuvenzinha de neve, areia ou poeira sob os pés em chão solto.",
        "es_es": "Una nubecilla de nieve, arena o polvo bajo los pies en suelo suelto.",
        "de_de": "Ein Wölkchen Schnee, Sand oder Staub unter den Füßen auf lockerem Boden.",
        "fr_fr": "Un petit nuage de neige, de sable ou de poussière sous les pieds sur sol meuble.",
        "pl_pl": "Obłoczek śniegu, piasku lub pyłu spod stóp na sypkim podłożu.",
        "uk_ua": "Хмарка снігу, піску чи пилу з-під ніг на пухкому ґрунті.",
        "ja_jp": "柔らかい地面では足元から雪や砂、土ぼこりが少し舞います。",
        "ko_kr": "푸석한 땅에서는 발밑에서 눈, 모래, 먼지가 조금 일어납니다.",
        "tr_tr": "Gevşek zeminde ayak altından biraz kar, kum ya da toz kalkar."},
    "config.farmstead_footprints.effects.dust.off": {
        "en_us": "Off",
        "ru_ru": "Выкл.",
        "zh_cn": "关闭",
        "pt_br": "Desligado",
        "es_es": "Desactivado",
        "de_de": "Aus",
        "fr_fr": "Désactivé",
        "pl_pl": "Wyłączone",
        "uk_ua": "Вимк.",
        "ja_jp": "オフ",
        "ko_kr": "끄기",
        "tr_tr": "Kapalı"},
    "config.farmstead_footprints.effects.dust.players": {
        "en_us": "Players",
        "ru_ru": "Только игроки",
        "zh_cn": "仅玩家",
        "pt_br": "Jogadores",
        "es_es": "Jugadores",
        "de_de": "Spieler",
        "fr_fr": "Joueurs",
        "pl_pl": "Gracze",
        "uk_ua": "Лише гравці",
        "ja_jp": "プレイヤーのみ",
        "ko_kr": "플레이어만",
        "tr_tr": "Oyuncular"},
    "config.farmstead_footprints.effects.dust.all": {
        "en_us": "Everyone",
        "ru_ru": "Все",
        "zh_cn": "全部",
        "pt_br": "Todos",
        "es_es": "Todos",
        "de_de": "Alle",
        "fr_fr": "Tous",
        "pl_pl": "Wszyscy",
        "uk_ua": "Усі",
        "ja_jp": "すべて",
        "ko_kr": "모두",
        "tr_tr": "Herkes"},
    "config.farmstead_footprints.surfaces.hardnessGate": {
        "en_us": "Soft up to hardness", "ru_ru": "Мягкие до прочности", "zh_cn": "硬度上限", "pt_br": "Macio até dureza", "es_es": "Blando hasta dureza",
        "de_de": "Weich bis Härte", "fr_fr": "Mou jusqu'à la dureté", "pl_pl": "Miękkie do twardości", "uk_ua": "М'які до міцності",
        "ja_jp": "柔らかさの上限（硬度）", "ko_kr": "부드러움 기준 경도", "tr_tr": "Sertliğe kadar yumuşak"},
    "config.farmstead_footprints.surfaces.hardnessGate.tooltip": {
        "en_us": "Any block softer than this takes prints, modded ones too (sand 0.5, grass and gravel 0.6, wool 0.8). 0: only the listed blocks.",
        "ru_ru": "Любой блок мягче этого значения принимает следы, модовые тоже (песок 0,5, трава и гравий 0,6, шерсть 0,8). 0 — только блоки из списков.",
        "zh_cn": "硬度低于此值的方块都会留下脚印，包括模组方块（沙子 0.5，草方块和沙砾 0.6，羊毛 0.8）。0：只用列表里的方块。",
        "pt_br": "Qualquer bloco mais macio que isto recebe pegadas, inclusive de mods (areia 0,5, grama e cascalho 0,6, lã 0,8). 0: só os blocos das listas.",
        "es_es": "Cualquier bloque más blando que esto recibe huellas, también los de mods (arena 0,5, hierba y grava 0,6, lana 0,8). 0: solo los bloques de las listas.",
        "de_de": "Jeder Block, der weicher ist, nimmt Spuren an, auch aus Mods (Sand 0,5, Gras und Kies 0,6, Wolle 0,8). 0: nur die Blöcke aus den Listen.",
        "fr_fr": "Tout bloc plus mou que cette valeur prend des empreintes, ceux des mods aussi (sable 0,5, herbe et gravier 0,6, laine 0,8). 0 : seulement les blocs des listes.",
        "pl_pl": "Każdy blok miększy od tej wartości przyjmuje ślady, także z modów (piasek 0,5, trawa i żwir 0,6, wełna 0,8). 0: tylko bloki z list.",
        "uk_ua": "Будь-який блок, м'якший за це значення, приймає сліди, модові теж (пісок 0,5, трава й гравій 0,6, вовна 0,8). 0 — лише блоки зі списків.",
        "ja_jp": "これより柔らかいブロックには足跡が付きます。MOD のブロックも対象（砂 0.5、草と砂利 0.6、羊毛 0.8）。0：リストのブロックのみ。",
        "ko_kr": "이보다 무른 블록에는 발자국이 남습니다. 모드 블록도 포함(모래 0.5, 잔디와 자갈 0.6, 양털 0.8). 0: 목록의 블록만.",
        "tr_tr": "Bundan yumuşak her blok iz alır, modlarınkiler de (kum 0,5, çim ve çakıl 0,6, yün 0,8). 0: yalnızca listedeki bloklar."},
    "config.farmstead_footprints.surfaces.include": {
        "en_us": "Always on", "ru_ru": "Всегда на", "zh_cn": "始终留脚印", "pt_br": "Sempre em", "es_es": "Siempre en", "de_de": "Immer auf",
        "fr_fr": "Toujours sur", "pl_pl": "Zawsze na", "uk_ua": "Завжди на", "ja_jp": "常に足跡", "ko_kr": "항상 남김", "tr_tr": "Her zaman"},
    "config.farmstead_footprints.surfaces.include.tooltip": {
        "en_us": "Blocks that always take prints, by id or #tag, separated by commas.",
        "ru_ru": "Блоки, которые всегда принимают следы: ID или #тег через запятую.",
        "zh_cn": "始终留下脚印的方块，填写 ID 或 #标签，用逗号分隔。",
        "pt_br": "Blocos que sempre recebem pegadas, por ID ou #tag, separados por vírgula.",
        "es_es": "Bloques que siempre reciben huellas, por ID o #etiqueta, separados por comas.",
        "de_de": "Blöcke, die immer Spuren annehmen, per ID oder #Tag, durch Kommas getrennt.",
        "fr_fr": "Blocs qui prennent toujours des empreintes, par identifiant ou #tag, séparés par des virgules.",
        "pl_pl": "Bloki, które zawsze przyjmują ślady: ID lub #tag, oddzielone przecinkami.",
        "uk_ua": "Блоки, які завжди приймають сліди: ID або #тег через кому.",
        "ja_jp": "常に足跡が付くブロック（ID または #タグ、カンマ区切り）。",
        "ko_kr": "항상 발자국이 남는 블록(ID 또는 #태그, 쉼표로 구분).",
        "tr_tr": "Her zaman iz alan bloklar: kimlik veya #etiket, virgülle ayrılmış."},
    "config.farmstead_footprints.surfaces.exclude": {
        "en_us": "Never on", "ru_ru": "Никогда на", "zh_cn": "从不留脚印", "pt_br": "Nunca em", "es_es": "Nunca en", "de_de": "Nie auf",
        "fr_fr": "Jamais sur", "pl_pl": "Nigdy na", "uk_ua": "Ніколи на", "ja_jp": "足跡なし", "ko_kr": "남기지 않음", "tr_tr": "Asla"},
    "config.farmstead_footprints.surfaces.exclude.tooltip": {
        "en_us": "Blocks that never take prints, by id or #tag, separated by commas; wins over everything else.",
        "ru_ru": "Блоки, которые никогда не принимают следов: ID или #тег через запятую; важнее всех остальных правил.",
        "zh_cn": "从不留下脚印的方块，填写 ID 或 #标签，用逗号分隔；优先于其他所有规则。",
        "pt_br": "Blocos que nunca recebem pegadas, por ID ou #tag, separados por vírgula; vale mais que todo o resto.",
        "es_es": "Bloques que nunca reciben huellas, por ID o #etiqueta, separados por comas; manda sobre todo lo demás.",
        "de_de": "Blöcke, die nie Spuren annehmen, per ID oder #Tag, durch Kommas getrennt; hat Vorrang vor allem anderen.",
        "fr_fr": "Blocs qui ne prennent jamais d'empreintes, par identifiant ou #tag, séparés par des virgules ; prime sur tout le reste.",
        "pl_pl": "Bloki, które nigdy nie przyjmują śladów: ID lub #tag, oddzielone przecinkami; ważniejsze od wszystkiego innego.",
        "uk_ua": "Блоки, які ніколи не приймають слідів: ID або #тег через кому; важливіше за всі інші правила.",
        "ja_jp": "足跡が付かないブロック（ID または #タグ、カンマ区切り）。他のどの設定より優先されます。",
        "ko_kr": "발자국이 남지 않는 블록(ID 또는 #태그, 쉼표로 구분). 다른 모든 규칙보다 우선합니다.",
        "tr_tr": "Asla iz almayan bloklar: kimlik veya #etiket, virgülle ayrılmış; diğer her şeyden önce gelir."},
    "farmstead_footprints.command.reloaded": {
        "en_us": "Footprints settings reloaded.", "ru_ru": "Настройки Footprints перезагружены.", "zh_cn": "Footprints 设置已重新加载。",
        "pt_br": "Configurações do Footprints recarregadas.", "es_es": "Ajustes de Footprints recargados.", "de_de": "Footprints-Einstellungen neu geladen.",
        "fr_fr": "Paramètres de Footprints rechargés.", "pl_pl": "Ustawienia Footprints wczytane ponownie.", "uk_ua": "Налаштування Footprints перезавантажено.",
        "ja_jp": "Footprints の設定を再読み込みしました。", "ko_kr": "Footprints 설정을 다시 불러왔습니다.", "tr_tr": "Footprints ayarları yeniden yüklendi."},
    "farmstead_footprints.command.problems": {
        "en_us": "Footprints settings: %s problem(s), see below. Fix the file and run /footprints reload again.",
        "ru_ru": "Настройки Footprints: проблем — %s, подробности ниже. Исправьте файл и снова выполните /footprints reload.",
        "zh_cn": "Footprints 设置：%s 个问题，详见下方。修正文件后再次执行 /footprints reload。",
        "pt_br": "Configurações do Footprints: %s problema(s), veja abaixo. Corrija o arquivo e rode /footprints reload de novo.",
        "es_es": "Ajustes de Footprints: %s problema(s), mira abajo. Corrige el archivo y ejecuta /footprints reload otra vez.",
        "de_de": "Footprints-Einstellungen: %s Problem(e), siehe unten. Datei korrigieren und /footprints reload erneut ausführen.",
        "fr_fr": "Paramètres de Footprints : %s problème(s), voir ci-dessous. Corrigez le fichier puis relancez /footprints reload.",
        "pl_pl": "Ustawienia Footprints: problemów: %s, szczegóły poniżej. Popraw plik i ponownie wpisz /footprints reload.",
        "uk_ua": "Налаштування Footprints: проблем — %s, подробиці нижче. Виправте файл і знову виконайте /footprints reload.",
        "ja_jp": "Footprints の設定: 問題が %s 件あります（下記参照）。ファイルを直して再度 /footprints reload を実行してください。",
        "ko_kr": "Footprints 설정: 문제 %s건, 아래를 확인하세요. 파일을 고친 뒤 /footprints reload를 다시 실행하세요.",
        "tr_tr": "Footprints ayarları: %s sorun, aşağıya bakın. Dosyayı düzeltip /footprints reload komutunu yeniden çalıştırın."},
    "farmstead_footprints.command.stats": {
        "en_us": "Footprints: %s prints of %s, drawn last frame: %s.",
        "ru_ru": "Footprints: следов %s из %s, нарисовано в последнем кадре: %s.",
        "zh_cn": "Footprints：脚印 %s / %s，上一帧绘制：%s。",
        "pt_br": "Footprints: %s pegadas de %s, desenhadas no último quadro: %s.",
        "es_es": "Footprints: %s huellas de %s, dibujadas en el último fotograma: %s.",
        "de_de": "Footprints: %s von %s Spuren, im letzten Bild gezeichnet: %s.",
        "fr_fr": "Footprints : %s empreintes sur %s, affichées à la dernière image : %s.",
        "pl_pl": "Footprints: śladów %s z %s, narysowano w ostatniej klatce: %s.",
        "uk_ua": "Footprints: слідів %s з %s, намальовано в останньому кадрі: %s.",
        "ja_jp": "Footprints: 足跡 %s / %s、直前のフレームで描画: %s。",
        "ko_kr": "Footprints: 발자국 %s / %s, 지난 프레임에 그림: %s.",
        "tr_tr": "Footprints: %s / %s iz, son karede çizilen: %s."},
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
