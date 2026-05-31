# Zakoulok Waifu Wallpaper для Android

Это нативный Android-проект живых обоев (`WallpaperService`), а не обычный экран Expo/React Native. Он рисует аниме-помощницу прямо на поверхности Live Wallpaper и может работать как фон рабочего стола/экрана блокировки.

## Один APK

Да, этот проект собирается в один APK-файл. Внутри одного APK находятся:

- экран настройки приложения;
- сервис живых обоев `WaifuWallpaperService`;
- сервис доступа к уведомлениям `WaifuNotificationListener`;
- вся визуальная часть Canvas-анимации.

После сборки debug-APK будет лежать здесь:

```text
android-live-wallpaper/app/build/outputs/apk/debug/app-debug.apk
```

Для релизной сборки можно использовать стандартную команду Android Gradle Plugin `gradle :app:assembleRelease` после настройки подписи.

## Что уже заложено

- Живые обои через `WaifuWallpaperService`.
- Анимированная аниме-девушка: дыхание/покачивание, фон со звёздами, реакция на касание.
- Верхние виджеты: время, погода, батарея, количество уведомлений.
- Погода через Open-Meteo без API-ключа; при отсутствии GPS используется запасная локация.
- Счётчик уведомлений через `NotificationListenerService` после ручного разрешения в настройках Android.
- Адаптивная отрисовка под разные размеры экрана: элементы масштабируются, верхние виджеты переходят из двух колонок в вертикальный список на узких экранах, длинные подписи обрезаются многоточием.
- Режим критически низкого заряда: при уровне батареи 5% и ниже обои отключают активную анимацию, не запускают обновление погоды, не реагируют на касания каждую секунду и показывают статичную очень грустную девушку.
- Экран настройки, который открывает установку Live Wallpaper, запрос геолокации и доступ к уведомлениям.


## Скачать APK

Страница загрузки для сайта: [`../android-apk.html`](../android-apk.html) / `https://PoetessEvangelina.github.io/android-apk.html`. Она ведёт на страницу release и workflow, чтобы пользователь не попадал сразу на `404`, если APK ещё не был опубликован.

Прямая стабильная ссылка на debug-APK после запуска GitHub Actions workflow:

```text
https://github.com/PoetessEvangelina/PoetessEvangelina.github.io/releases/download/android-live-wallpaper-latest/zakoulok-waifu-wallpaper-debug.apk
```

Если прямая ссылка отвечает `404`, это не ошибка в адресе: release `android-live-wallpaper-latest` или файл `zakoulok-waifu-wallpaper-debug.apk` ещё не создан. Нужно открыть GitHub Actions workflow **Build Android live-wallpaper APK**, запустить его вручную или дождаться запуска после push; после успешной сборки workflow перезапишет release `android-live-wallpaper-latest` и прикрепит APK.

Подробная пошаговая инструкция по публикации APK в GitHub Releases и передаче проекта в App Builder находится в [`PUBLISH_APK.md`](PUBLISH_APK.md).

## Сборка APK

```bash
cd android-live-wallpaper
gradle :app:assembleDebug
```

Готовый APK появится здесь:

```text
android-live-wallpaper/app/build/outputs/apk/debug/app-debug.apk
```


## Сборка без Android Gradle Plugin

Да, можно собрать APK **без Android Gradle Plugin** и даже без Gradle, если на машине уже установлен Android SDK с `platforms/android-XX/android.jar` и `build-tools` (`aapt2`, `d8`, `zipalign`, `apksigner`). Это обходной путь для ситуации, когда не скачивается именно Gradle-плагин, но Android SDK доступен.

Добавлен скрипт ручной сборки:

```bash
cd android-live-wallpaper
ANDROID_SDK_ROOT=/path/to/Android/Sdk ./scripts/build-with-sdk-tools.sh
```

Он вручную выполняет шаги, которые обычно делает AGP: компилирует ресурсы через `aapt2`, генерирует `R.java`, компилирует Java-код через `javac`, делает `classes.dex` через `d8`, упаковывает APK, выравнивает `zipalign` и подписывает debug-ключом через `apksigner`.

Готовый APK появится здесь:

```text
android-live-wallpaper/app/build/outputs/apk/manual/waifu-wallpaper-manual-debug.apk
```

Ограничение этого варианта: он всё равно требует Android SDK Build Tools. Совсем без Android SDK создать устанавливаемый APK нельзя, даже если функционал урезать до статичных живых обоев: Android должен скомпилировать ресурсы, связать манифест, создать dex и подписать пакет.

## Как установить как видеообои/живые обои

1. Установите APK на Android-телефон.
2. Откройте приложение **Zakoulok Waifu Wallpaper**.
3. Нажмите **Установить живые обои**.
4. Подтвердите установку в системном окне Android.
5. Для погоды нажмите **Разрешить погоду по GPS**.
6. Для уведомлений нажмите **Включить доступ к уведомлениям** и включите доступ для приложения.

## Проверка работоспособности на Android

Проект должен устанавливаться как обычный APK и регистрироваться в системном списке Live Wallpaper благодаря `android.service.wallpaper.WallpaperService` в манифесте. На реальном телефоне нужно проверить три системных сценария:

1. установка APK и появление пункта **Закоулок: аниме-помощница** в списке живых обоев;
2. выдача геолокации, после чего погода должна обновиться в верхнем виджете;
3. включение доступа к уведомлениям, после чего счётчик уведомлений должен показывать активные уведомления.

В текущем CI/контейнере нет Android SDK и заблокирован доступ к Google Maven, поэтому здесь можно проверить структуру проекта, XML и Gradle-конфигурацию, но финальную сборку APK нужно запускать в Android Studio или на машине с Android SDK и доступом к репозиториям Google.

## Важно про батарею

Когда батарея реально равна 0%, Android-устройство обычно уже выключено, поэтому приложение физически не может выполнить код. Для практического поведения «как на нуле» используется порог 5% и ниже: обои становятся статичными, отключают частые перерисовки и показывают очень грустное состояние персонажа.

## Важно про разрешения

Android не позволяет обычному приложению читать все уведомления или геолокацию без явного разрешения пользователя. Поэтому эти функции включаются через системные экраны, а сами живые обои показывают запасные данные, если разрешения не выданы.

## Если Google Maven / Maven Central недоступны

Обойти ограничение можно, но **просто скачать один файл Android Gradle Plugin и положить его в проект обычно недостаточно**. Плагин `com.android.application:8.7.3` тянет набор транзитивных зависимостей (`com.android.tools.build:gradle`, builder, manifest-merger, sdk-common и другие артефакты), поэтому Gradle нужен либо доступ к репозиториям, либо полноценный Maven-кэш.


### Про `settings.gradle.kts` и `google()`

Фрагмент Kotlin DSL вида `pluginManagement { repositories { google(); mavenCentral() } }` действительно правильный для Android Gradle Plugin, но в этом проекте уже есть эквивалентная настройка в Groovy DSL-файле `settings.gradle`: `google()`, `mavenCentral()` и `gradlePluginPortal()`. Такая настройка говорит Gradle, **где искать** плагин, но не заменяет сам доступ к репозиторию. Если сеть до Google Maven заблокирована и артефактов нет в локальном кэше, сборка всё равно упадёт на резолве `com.android.application:8.7.3`.

То есть добавление `google()` помогает только в двух случаях:

- на машине есть интернет-доступ к Google Maven;
- или нужные артефакты Android Gradle Plugin уже лежат в локальном Gradle-кэше/локальном Maven-репозитории.


### Про ссылку MvnRepository и `com.android.application.gradle.plugin`

Ссылка MvnRepository полезна как подсказка, что такой артефакт существует, но сама XML-зависимость Maven не является готовым решением для этого Gradle-проекта:

- `com.android.application:com.android.application.gradle.plugin` — это **plugin marker POM**, то есть метка, через которую Gradle находит реальную реализацию Android Gradle Plugin;
- в `build.gradle` проекта уже задан нужный plugin id: `id 'com.android.application' version '8.7.3' apply false`, поэтому добавлять XML из Maven в Android-проект не нужно;
- версия `9.3.0-alpha09` — preview/alpha-ветка, а проект сейчас закреплён на более консервативной версии `8.7.3`; простая замена версии может потребовать обновления Gradle wrapper, SDK/Build Tools и проверки совместимости;
- если скачать только POM или один jar, Gradle всё равно не получит все зависимости плагина. Для офлайн-сборки нужен полный набор артефактов в Gradle-кэше или локальном Maven-репозитории.

Если использовать именно эту ссылку как источник для офлайн-сборки, нужно скачать не только `com.android.application.gradle.plugin`, а весь dependency tree для той же версии AGP и положить его в Maven-структуру. Для текущей сборки безопаснее собирать артефакты версии `8.7.3`, потому что она уже указана в `build.gradle`.

Практичные варианты:

1. **Самый простой способ** — открыть проект в Android Studio на машине с интернетом и собрать APK. Android Studio сама скачает Gradle, Android Gradle Plugin, SDK Platform 35 и Build Tools.
2. **Собрать кэш один раз и перенести** — на машине с интернетом выполнить `gradle :app:assembleDebug`, затем перенести `~/.gradle/caches` и Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT`) в офлайн-среду. После переноса запускать сборку с флагом `--offline`.
3. **Локальный Maven-репозиторий в проекте** — можно положить зависимости в `android-live-wallpaper/gradle/offline-repo/`, но это должен быть полноценный Maven-репозиторий с POM/metadata и всеми зависимостями, а не одиночный `jar`. Заготовка, пример структуры и предупреждения лежат в `gradle/offline-repo/README.md`.
4. **Не рекомендуется коммитить бинарные зависимости** — Android Gradle Plugin и его зависимости занимают много места, зависят от версии Gradle/SDK и лучше хранить их в кэше CI, Nexus/Artifactory или скачивать при сборке.

Если вы хотите «добавить сюда файл», лучше добавлять не один `jar`, а архив подготовленного `~/.gradle/caches` вместе с Android SDK для офлайн-сборки или готовый локальный Maven-репозиторий. Если пришлёте такой архив/каталог, сборку можно будет переключить на него и проверить `gradle --offline :app:assembleDebug`.
