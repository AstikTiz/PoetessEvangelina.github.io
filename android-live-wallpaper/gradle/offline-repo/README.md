# Offline Gradle/Android plugin cache

Эта папка зарезервирована под **локальный Maven-репозиторий** для случаев, когда сборочная среда не может выйти в Google Maven/Maven Central.

Важно: один скачанный `*.jar` Android Gradle Plugin сюда класть бесполезно. Gradle должен видеть Maven-структуру с `*.pom`/metadata и все транзитивные зависимости плагина.

Минимальная идея структуры выглядит так:

```text
gradle/offline-repo/
├── com/android/application/com.android.application.gradle.plugin/8.7.3/...
├── com/android/tools/build/gradle/8.7.3/...
├── com/android/tools/build/aapt2/.../...
└── ...другие зависимости Android Gradle Plugin...
```

Практически проще не собирать эту папку вручную, а использовать один из вариантов:

1. На машине с интернетом собрать проект один раз, затем перенести в офлайн-среду весь `~/.gradle/caches` и Android SDK (`ANDROID_HOME`/`ANDROID_SDK_ROOT`). После этого запускать `gradle --offline :app:assembleDebug`.
2. Если нужен именно локальный Maven-репозиторий внутри проекта, подготовить его через Gradle/Maven/Nexus/Artifactory на машине с интернетом и положить сюда уже готовую Maven-структуру, а не отдельные jar-файлы.

Бинарные зависимости обычно не коммитят в Git: они большие, быстро устаревают и дублируют официальные Maven-репозитории.
