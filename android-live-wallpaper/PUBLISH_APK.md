# Как опубликовать APK в GitHub Releases

Этот репозиторий уже содержит workflow, который должен собрать Android APK и прикрепить его к GitHub Release. Ошибка `404` у прямой ссылки означает только одно: release-файл ещё не был создан или workflow ещё не завершился успешно.

## Вариант 1 — автоматически через GitHub Actions

1. Откройте репозиторий на GitHub.
2. Перейдите во вкладку **Actions**.
3. Выберите workflow **Build Android live-wallpaper APK**.
4. Нажмите **Run workflow**.
5. Выберите ветку, где лежат последние изменения.
6. Нажмите зелёную кнопку **Run workflow** и дождитесь успешной сборки.
7. После зелёной галочки workflow создаст или перезапишет release `android-live-wallpaper-latest`.
8. APK будет прикреплён к release как файл `zakoulok-waifu-wallpaper-debug.apk`.

После этого должна заработать прямая ссылка:

```text
https://github.com/PoetessEvangelina/PoetessEvangelina.github.io/releases/download/android-live-wallpaper-latest/zakoulok-waifu-wallpaper-debug.apk
```

Если ссылка всё ещё отдаёт `404`, проверьте:

- workflow действительно завершился зелёной галочкой;
- в логе шага **Publish stable debug APK release** нет ошибки прав доступа;
- в разделе **Releases** появился release `android-live-wallpaper-latest`;
- внутри release есть asset `zakoulok-waifu-wallpaper-debug.apk`.

## Вариант 2 — вручную загрузить APK в Release

Если вы собрали APK в Android Studio, App Builder или на другой машине:

1. Откройте вкладку **Releases** в GitHub-репозитории.
2. Нажмите **Draft a new release** или откройте существующий release `android-live-wallpaper-latest` и нажмите **Edit**.
3. В поле tag укажите `android-live-wallpaper-latest`.
4. В title можно указать `Zakoulok Waifu Wallpaper APK`.
5. Прикрепите APK-файл в блок assets.
6. Назовите файл именно `zakoulok-waifu-wallpaper-debug.apk`, если хотите сохранить текущую прямую ссылку без изменений.
7. Нажмите **Publish release** или **Update release**.

## Что можно передать в App Builder

Я не могу приложить готовый APK прямо из этого контейнера, потому что здесь нет Android SDK/Android Gradle Plugin для финальной сборки. Но workflow дополнительно упаковывает исходники в artifact `zakoulok-waifu-wallpaper-app-builder-source.zip`; его можно скачать со страницы запуска GitHub Actions и загрузить в App Builder.

Также можно вручную передать в App Builder исходники Android-проекта:

```text
android-live-wallpaper/
```

Минимальный набор для App Builder:

```text
android-live-wallpaper/settings.gradle
android-live-wallpaper/build.gradle
android-live-wallpaper/app/build.gradle
android-live-wallpaper/app/src/main/AndroidManifest.xml
android-live-wallpaper/app/src/main/java/com/zakoulok/wallpaper/
android-live-wallpaper/app/src/main/res/
```

Если App Builder просит готовый Gradle-проект, загрузите папку `android-live-wallpaper` целиком. Если он просит только исходники Android app module, загрузите папку `android-live-wallpaper/app` и файлы Gradle рядом.

Если вы скачиваете artifact `zakoulok-waifu-wallpaper-app-builder-source.zip`, распаковывать его обычно не нужно: большинство App Builder-сервисов принимают zip-архив проекта. Если сервис не принимает zip, распакуйте архив и выберите получившуюся папку проекта.

## Что прислать обратно для улучшения качества

Лучший вариант — прислать обратно одно из двух:

- готовый APK, если App Builder смог его собрать;
- архив проекта после правок App Builder, если он изменил Gradle-файлы, ресурсы или Java-код.

После этого можно будет проверить структуру, поправить визуальную часть, улучшить Canvas-отрисовку девушки и обновить инструкции/ссылки под новый файл.
