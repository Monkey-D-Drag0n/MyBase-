# MyBase

MyBase is a local-first Android tablet app for planning tasks, keeping knowledge notes, organizing learning resources, and tracking focus time.

## Features

- Home dashboard with today's tasks, focus controls, saved PDF reading positions, and recent notes.
- Planner for dated tasks, completion, priority, and a calendar view of pending days.
- Knowledge notes with body text, categories, tags, and search.
- Library for links and document references, including a system file picker and user-authorized folder browsing.
- In-app PDF reading with page navigation, bookmarks, reading position, text/region annotations, plus TXT and Markdown previews.
- Local persistence using the app's SharedPreferences-backed repositories. No account or cloud service is required.

## Build and test

Open the project in Android Studio or use the Gradle wrapper:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## License and third-party materials

Original MyBase project code is offered under the repository's MIT `LICENSE`. Third-party libraries, platform components, and artwork remain subject to their respective licenses and terms; the MIT license does not relicense them.

The Momo illustration is drawn in the project with Compose Canvas. The launcher icon includes the Android robot artwork, reproduced or modified from work by Google under the Creative Commons Attribution 3.0 license, following the [Android brand guidelines](https://developer.android.com/distribute/marketing-tools/brand-guidelines). The Android robot remains outside the project's MIT license.
