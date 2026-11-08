# 다국어 문자열 생성기: python3 tools/gen_strings.py app/src/main/res
# 키별로 (en, ko, fr, es, ja, zh) 순서. 문자열을 고칠 때는 이 파일을 수정 후 재생성.
# key: (en, ko, fr, es, ja, zh)
S = {
 "app_name": ("SnapQR",)*6,
 "tile_label": ("Scan QR", "QR 스캔", "Scanner un QR", "Escanear QR", "QRスキャン", "扫描二维码"),
 "shortcut_scan_short": ("Scan", "스캔", "Scanner", "Escanear", "スキャン", "扫描"),
 "shortcut_scan_long": ("Scan a QR code", "QR 코드 스캔", "Scanner un code QR", "Escanear un código QR", "QRコードをスキャン", "扫描二维码"),

 "perm_message": ("Camera access is needed\nto scan QR codes", "QR 코드를 스캔하려면\n카메라 권한이 필요해요", "L’accès à l’appareil photo est nécessaire\npour scanner les codes QR", "Se necesita acceso a la cámara\npara escanear códigos QR", "QRコードをスキャンするには\nカメラへのアクセスが必要です", "扫描二维码需要\n相机权限"),
 "perm_button": ("Allow camera", "카메라 권한 허용", "Autoriser l’appareil photo", "Permitir cámara", "カメラを許可", "允许使用相机"),

 "hint_point": ("Point at a QR code", "QR 코드를 비춰주세요", "Visez un code QR", "Apunta a un código QR", "QRコードを映してください", "将二维码放入画面"),
 "hint_detecting": ("Detecting…", "인식 중…", "Détection…", "Detectando…", "認識中…", "正在识别…"),
 "cd_gallery": ("Scan from gallery", "갤러리에서 스캔", "Scanner depuis la galerie", "Escanear desde la galería", "ギャラリーからスキャン", "从相册扫描"),
 "cd_torch": ("Flashlight", "플래시", "Lampe torche", "Linterna", "ライト", "手电筒"),
 "cd_copy": ("Copy", "복사", "Copier", "Copiar", "コピー", "复制"),
 "cd_share": ("Share", "공유", "Partager", "Compartir", "共有", "分享"),
 "cd_close": ("Close", "닫기", "Fermer", "Cerrar", "閉じる", "关闭"),
 "cd_secure": ("Secure connection (HTTPS)", "보안 연결 (HTTPS)", "Connexion sécurisée (HTTPS)", "Conexión segura (HTTPS)", "安全な接続 (HTTPS)", "安全连接 (HTTPS)"),
 "cd_not_secure": ("Not secure", "보안 연결 아님", "Non sécurisé", "No seguro", "保護されていない通信", "不安全"),

 "toast_no_qr_in_image": ("No QR code found in this image", "이미지에서 QR 코드를 찾지 못했어요", "Aucun code QR trouvé dans cette image", "No se encontró ningún código QR en la imagen", "画像にQRコードが見つかりませんでした", "未在图片中找到二维码"),
 "toast_no_app_for_scheme": ("No app can open this link (%1$s://)", "이 링크를 열 수 있는 앱이 없어요 (%1$s://)", "Aucune application ne peut ouvrir ce lien (%1$s://)", "Ninguna app puede abrir este enlace (%1$s://)", "このリンクを開けるアプリがありません (%1$s://)", "没有可打开此链接的应用 (%1$s://)"),
 "toast_no_app_for_link": ("No app can open this link", "이 링크를 열 수 있는 앱이 없어요", "Aucune application ne peut ouvrir ce lien", "Ninguna app puede abrir este enlace", "このリンクを開けるアプリがありません", "没有可打开此链接的应用"),
 "toast_invalid_link": ("Invalid app link", "잘못된 앱 링크예요", "Lien d’application non valide", "Enlace de app no válido", "無効なアプリリンクです", "无效的应用链接"),
 "toast_wifi_pw_copied": ("Password copied. Connect to “%1$s”", "비밀번호를 복사했어요. “%1$s”에 연결하세요", "Mot de passe copié. Connectez-vous à « %1$s »", "Contraseña copiada. Conéctate a “%1$s”", "パスワードをコピーしました。「%1$s」に接続してください", "密码已复制。请连接到“%1$s”"),
 "toast_copied": ("Copied", "복사했어요", "Copié", "Copiado", "コピーしました", "已复制"),
 "toast_no_app": ("No app can handle this action", "이 작업을 처리할 앱이 없어요", "Aucune application ne peut effectuer cette action", "Ninguna app puede realizar esta acción", "この操作を実行できるアプリがありません", "没有可执行此操作的应用"),

 "preview_redirect": ("Redirects %1$s → %2$s", "%1$s → %2$s 로 이동", "Redirige %1$s → %2$s", "Redirige %1$s → %2$s", "%1$s → %2$s にリダイレクト", "重定向 %1$s → %2$s"),
 "preview_failed": ("Couldn’t load preview", "미리보기를 불러오지 못했어요", "Impossible de charger l’aperçu", "No se pudo cargar la vista previa", "プレビューを読み込めませんでした", "无法加载预览"),
 "target_opens_in": ("Opens in %1$s", "%1$s 앱으로 열려요", "S’ouvre dans %1$s", "Se abre en %1$s", "%1$s で開きます", "将在 %1$s 中打开"),
 "target_not_installed": ("App not installed · Go to store", "앱이 설치되어 있지 않아요 · 스토어로 이동", "Application non installée · Aller au store", "App no instalada · Ir a la tienda", "アプリ未インストール · ストアへ", "应用未安装 · 前往商店"),

 "label_network": ("Network", "네트워크", "Réseau", "Red", "ネットワーク", "网络"),
 "label_password": ("Password", "비밀번호", "Mot de passe", "Contraseña", "パスワード", "密码"),
 "label_security": ("Security", "보안", "Sécurité", "Seguridad", "セキュリティ", "加密"),
 "security_none": ("None", "없음", "Aucune", "Ninguna", "なし", "无"),
 "label_phone_number": ("Phone", "전화번호", "Téléphone", "Teléfono", "電話番号", "电话号码"),
 "label_recipient": ("To", "받는 사람", "À", "Para", "宛先", "收件人"),
 "label_message": ("Message", "내용", "Message", "Mensaje", "本文", "内容"),
 "label_subject": ("Subject", "제목", "Objet", "Asunto", "件名", "主题"),
 "label_coordinates": ("Coordinates", "좌표", "Coordonnées", "Coordenadas", "座標", "坐标"),
 "label_name": ("Name", "이름", "Nom", "Nombre", "名前", "姓名"),
 "label_company": ("Company", "회사", "Société", "Empresa", "会社", "公司"),
 "label_phone": ("Phone", "전화", "Téléphone", "Teléfono", "電話", "电话"),
 "label_email": ("Email", "이메일", "E-mail", "Correo", "メール", "邮箱"),
 "label_web": ("Web", "웹", "Web", "Web", "Web", "网址"),
 "label_address": ("Address", "주소", "Adresse", "Dirección", "住所", "地址"),
 "label_title": ("Title", "제목", "Titre", "Título", "タイトル", "标题"),
 "label_when": ("When", "일시", "Date", "Fecha", "日時", "时间"),
 "label_location": ("Location", "장소", "Lieu", "Lugar", "場所", "地点"),
 "label_description": ("Details", "설명", "Description", "Descripción", "説明", "说明"),

 "type_play_app": ("Google Play app", "Google Play 앱", "Application Google Play", "App de Google Play", "Google Play アプリ", "Google Play 应用"),
 "type_website": ("Website", "웹사이트", "Site web", "Sitio web", "ウェブサイト", "网站"),
 "type_app_link": ("App link", "앱 링크", "Lien d’application", "Enlace de app", "アプリリンク", "应用链接"),
 "type_wifi": ("Wi-Fi",)*6,
 "type_phone": ("Phone number", "전화번호", "Numéro de téléphone", "Número de teléfono", "電話番号", "电话号码"),
 "type_sms": ("Text message", "문자", "SMS", "SMS", "SMS", "短信"),
 "type_email": ("Email", "이메일", "E-mail", "Correo electrónico", "メール", "电子邮件"),
 "type_location": ("Location", "위치", "Lieu", "Ubicación", "位置情報", "位置"),
 "type_contact": ("Contact", "연락처", "Contact", "Contacto", "連絡先", "联系人"),
 "type_event": ("Event", "일정", "Événement", "Evento", "予定", "日程"),
 "type_text": ("Text", "텍스트", "Texte", "Texto", "テキスト", "文本"),

 "action_play_store": ("View in Play Store", "Play 스토어에서 보기", "Voir sur le Play Store", "Ver en Play Store", "Play ストアで見る", "在 Play 商店中查看"),
 "action_open_in": ("Open in %1$s", "%1$s에서 열기", "Ouvrir dans %1$s", "Abrir en %1$s", "%1$s で開く", "在 %1$s 中打开"),
 "action_open": ("Open", "열기", "Ouvrir", "Abrir", "開く", "打开"),
 "action_open_app": ("Open %1$s", "%1$s 열기", "Ouvrir %1$s", "Abrir %1$s", "%1$s を開く", "打开 %1$s"),
 "action_install": ("Install from store", "스토어에서 설치", "Installer depuis le store", "Instalar desde la tienda", "ストアからインストール", "从商店安装"),
 "action_open_in_app": ("Open in app", "앱에서 열기", "Ouvrir dans l’application", "Abrir en la app", "アプリで開く", "在应用中打开"),
 "action_wifi": ("Join Wi-Fi", "Wi-Fi 연결", "Se connecter au Wi-Fi", "Conectar a Wi-Fi", "Wi-Fi に接続", "连接 Wi-Fi"),
 "action_call": ("Call", "전화 걸기", "Appeler", "Llamar", "電話をかける", "拨打电话"),
 "action_sms": ("Send message", "문자 보내기", "Envoyer un SMS", "Enviar mensaje", "SMSを送信", "发送短信"),
 "action_email": ("Write email", "메일 쓰기", "Écrire un e-mail", "Escribir correo", "メールを作成", "写邮件"),
 "action_map": ("Show on map", "지도에서 보기", "Voir sur la carte", "Ver en el mapa", "地図で見る", "在地图中查看"),
 "action_add_contact": ("Add to contacts", "연락처에 추가", "Ajouter aux contacts", "Añadir a contactos", "連絡先に追加", "添加到联系人"),
 "action_add_event": ("Add to calendar", "캘린더에 추가", "Ajouter à l’agenda", "Añadir al calendario", "カレンダーに追加", "添加到日历"),
 "action_web_search": ("Search the web", "웹에서 검색", "Rechercher sur le Web", "Buscar en la web", "ウェブで検索", "网页搜索"),
 "risk_title_danger": ("Possibly a phishing link", "피싱 위험이 있는 링크예요", "Lien d’hameçonnage possible", "Posible enlace de phishing", "フィッシングの可能性があるリンクです", "可能是钓鱼链接"),
 "risk_title_caution": ("Check before opening", "열기 전에 확인하세요", "Vérifiez avant d’ouvrir", "Revisa antes de abrir", "開く前に確認してください", "打开前请确认"),
 "risk_safe": ("No known risk signs", "알려진 위험 신호 없음", "Aucun signe de risque connu", "Sin señales de riesgo conocidas", "既知の危険サインはありません", "未发现已知风险迹象"),
 "risk_dangerous_scheme": ("Runs a script or opens a local file (%1$s:)", "스크립트를 실행하거나 기기 파일을 여는 링크예요 (%1$s:)", "Exécute un script ou ouvre un fichier local (%1$s:)", "Ejecuta un script o abre un archivo local (%1$s:)", "スクリプト実行またはローカルファイルを開くリンクです (%1$s:)", "会执行脚本或打开本地文件 (%1$s:)"),
 "risk_userinfo": ("Disguised address: “%1$s” before @ is not the real site", "위장된 주소: @ 앞의 “%1$s”는 실제 사이트가 아니에요", "Adresse déguisée : « %1$s » avant @ n’est pas le vrai site", "Dirección disfrazada: “%1$s” antes de @ no es el sitio real", "偽装アドレス：@ の前の「%1$s」は本当のサイトではありません", "伪装地址：@ 前的“%1$s”不是真实网站"),
 "risk_brand": ("Uses the name %1$s but is not an official domain", "%1$s 이름을 쓰지만 공식 도메인이 아니에요", "Utilise le nom %1$s mais n’est pas un domaine officiel", "Usa el nombre %1$s pero no es un dominio oficial", "%1$s の名前を使っていますが公式ドメインではありません", "使用了 %1$s 的名称，但不是官方域名"),
 "risk_brand_mention": ("Contains the name %1$s – make sure it’s official", "%1$s 이름이 포함돼 있어요. 공식 사이트인지 확인하세요", "Contient le nom %1$s – vérifiez qu’il est officiel", "Contiene el nombre %1$s: comprueba que sea oficial", "%1$s の名前を含みます。公式サイトか確認してください", "包含 %1$s 名称，请确认是否为官方网站"),
 "risk_lookalike": ("Spelled like %1$s but slightly different", "%1$s와 철자가 비슷하지만 달라요", "Orthographe proche de %1$s mais différente", "Se escribe parecido a %1$s pero es distinto", "%1$s と似ていますが綴りが違います", "拼写与 %1$s 相似但不同"),
 "risk_homograph": ("Uses look-alike characters: %1$s", "비슷하게 생긴 다른 문자를 사용해요: %1$s", "Utilise des caractères trompeurs : %1$s", "Usa caracteres engañosos: %1$s", "紛らわしい文字を使っています：%1$s", "使用了形似字符：%1$s"),
 "risk_apk": ("Downloads an app installer (APK)", "앱 설치 파일(APK)을 내려받아요", "Télécharge un fichier d’installation d’application (APK)", "Descarga un instalador de aplicación (APK)", "アプリのインストールファイル (APK) をダウンロードします", "会下载应用安装包 (APK)"),
 "risk_ip": ("Uses a raw IP address instead of a domain", "도메인 대신 IP 주소를 사용해요", "Utilise une adresse IP au lieu d’un domaine", "Usa una dirección IP en lugar de un dominio", "ドメインではなくIPアドレスを使っています", "使用 IP 地址而非域名"),
 "risk_shortener": ("Shortened link – the real destination is hidden", "단축 URL이라 실제 목적지가 숨겨져 있어요", "Lien raccourci – la vraie destination est masquée", "Enlace acortado: el destino real está oculto", "短縮URLのため実際のリンク先が隠れています", "短链接，真实目标被隐藏"),
 "risk_tld": ("Domain ending often used for spam (.%1$s)", "스팸에 자주 쓰이는 도메인이에요 (.%1$s)", "Extension souvent utilisée pour le spam (.%1$s)", "Terminación de dominio usada a menudo para spam (.%1$s)", "スパムによく使われるドメインです (.%1$s)", "常被用于垃圾信息的域名后缀 (.%1$s)"),
 "risk_http": ("Not encrypted (HTTP)", "암호화되지 않은 연결(HTTP)이에요", "Connexion non chiffrée (HTTP)", "Conexión sin cifrar (HTTP)", "暗号化されていない接続 (HTTP) です", "未加密连接 (HTTP)"),
 "risk_port": ("Unusual port (%1$s)", "일반적이지 않은 포트를 사용해요 (%1$s)", "Port inhabituel (%1$s)", "Puerto inusual (%1$s)", "通常と異なるポートです (%1$s)", "非常用端口 (%1$s)"),
 "confirm_title": ("Open this link?", "이 링크를 열까요?", "Ouvrir ce lien ?", "¿Abrir este enlace?", "このリンクを開きますか？", "要打开此链接吗？"),
 "confirm_message": ("This link shows signs of phishing. Never enter passwords, card numbers or verification codes.", "피싱 징후가 있는 링크예요. 비밀번호, 카드번호, 인증번호는 절대 입력하지 마세요.", "Ce lien présente des signes d’hameçonnage. Ne saisissez jamais de mot de passe, numéro de carte ou code de vérification.", "Este enlace muestra señales de phishing. Nunca introduzcas contraseñas, números de tarjeta ni códigos de verificación.", "フィッシングの兆候があるリンクです。パスワード、カード番号、認証コードは絶対に入力しないでください。", "此链接有钓鱼迹象。切勿输入密码、卡号或验证码。"),
 "action_open_anyway": ("Open anyway", "그래도 열기", "Ouvrir quand même", "Abrir de todos modos", "それでも開く", "仍然打开"),
 "action_cancel": ("Cancel", "취소", "Annuler", "Cancelar", "キャンセル", "取消"),
}
LANGS = ["", "ko", "fr", "es", "ja", "zh"]
import sys, os
res = sys.argv[1]
def esc(s):
    s = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
    s = s.replace("\\","\\\\").replace("\n","\\n")
    s = s.replace("'", "\\'").replace('"', '\\"')
    if s[:1] in "@?": s = "\\" + s
    return s
for i, lang in enumerate(LANGS):
    d = os.path.join(res, "values" + (f"-{lang}" if lang else ""))
    os.makedirs(d, exist_ok=True)
    out = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for k, v in S.items():
        val = v[i] if len(v) > 1 else v[0]
        extra = ' translatable="false"' if (k in ("app_name","type_wifi") and lang == "") else ""
        if lang and k in ("app_name","type_wifi"): continue
        out.append(f'    <string name="{k}"{extra}>{esc(val)}</string>')
    out.append('</resources>\n')
    open(os.path.join(d, "strings.xml"), "w").write("\n".join(out))
print("ok", len(S))
