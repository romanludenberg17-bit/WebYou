# WebYou

WebYou — минималистичный Android-браузер в духе Material 3 / Material Expressive с органическими «волнами», pill-контролами и четырьмя режимами темы: **система, светлая, тёмная, AMOLED**.

## Что уже есть в MVP

- стартовая страница Google;
- адресная строка + поиск;
- вкладки с превью страниц;
- инкогнито-вкладки;
- встроенное меню браузера;
- предпросмотр страниц;
- блокировка рекламных/трекерных доменов через `shouldInterceptRequest`;
- перевод страницы через Google Translate;
- HTTP/HTTPS proxy override для WebView;
- экран DNS / Proxy;
- кастомизация акцентного цвета;
- светлая / тёмная / AMOLED / системная тема;
- настройка количества и глубины декоративных волн;
- GitHub Actions для сборки APK.

## Важный момент про DNS

WebView не даёт приложению просто заменить системный DNS одним флагом. Поэтому в MVP экран DNS уже подготовлен, а реальный сетевой DoH/VPN-слой оставлен отдельным этапом. Proxy override при поддерживаемом WebView работает непосредственно для WebView-трафика.

## Сборка локально

Требования:

- Android Studio с JDK 17;
- Android SDK 36;
- Gradle 9.6.

```bash
gradle assembleDebug
gradle assembleRelease
```

APK появится в:

```text
app/build/outputs/apk/debug/
app/build/outputs/apk/release/
```

## Сборка на GitHub

репозиторий уже содержит workflow `.github/workflows/build.yml`.

при push в `main` или `master` GitHub Actions собирает debug и release APK и публикует их как artifacts.

при создании тега вида `v0.1.0` workflow также создаёт GitHub Release.

### production signing

для настоящего публичного релиза лучше добавить собственный Android keystore в GitHub Secrets и подписывать `release`-вариант этим ключом. сейчас release-сборка намеренно не содержит приватного ключа.

## Архитектура

UI: Jetpack Compose + Material 3.

Web engine: Android WebView + AndroidX WebKit 1.18.

сетка дизайна специально сделана вокруг асимметричных rounded/pill форм и мягких волновых фонов, чтобы приблизить интерфейс к согласованному концепту WebYou.
