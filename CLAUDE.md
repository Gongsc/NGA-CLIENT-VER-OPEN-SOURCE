# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Open-source Android client for the NGA forum (bbs.nga.cn). The UI is mid-migration from Views to Jetpack Compose + Material 3; Compose and View code coexist. Project docs, README, and commit messages are written in Chinese — keep new docs/commits in Chinese to match.

Toolchain: JDK 17, AGP 9.4.1 (built-in Kotlin, no `kotlin-android` plugin), Kotlin/Compose Compiler 2.4.21, Gradle 9.6.0, minSdk 31, compile/targetSdk 37, Build Tools 36.0.0. SDK levels and shared dependency versions live in `project.ext` of the root `build.gradle`; repositories are centralized in `settings.gradle` (`FAIL_ON_PROJECT_REPOS` — don't add repos in module build files).

## Commands

```sh
# Debug build (APK → nga_phone_base_3.0/build/outputs/apk/debug/)
./gradlew :nga_phone_base_3.0:assembleDebug

# Full local check: build + unit tests + lint (lint aborts on error)
./gradlew :nga_phone_base_3.0:assembleDebug :nga_phone_base_3.0:testDebugUnitTest :nga_phone_base_3.0:lintDebug

# Single unit test class / method
./gradlew :nga_phone_base_3.0:testDebugUnitTest --tests 'sp.phone.IncompleteTopicTest'
./gradlew :nga_phone_base_3.0:testDebugUnitTest --tests 'gov.anzong.androidnga.arouter.AppNavigatorTest.someMethod'

# Instrumented tests (needs an Android 12+ device/emulator)
./gradlew :nga_phone_base_3.0:connectedDebugAndroidTest
./gradlew :nga_phone_base_3.0:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fqcn>
```

Reports: `nga_phone_base_3.0/build/reports/tests/testDebugUnitTest/`, `.../reports/lint-results-debug.html`, `.../reports/androidTests/connected/debug/`. SARIF lint export is intentionally disabled (AGP 9.4.1 reporter crashes). Release signing is not in the repo: `assembleRelease` signs only when the `NGA_KEYSTORE_*`/`NGA_KEY_*` env vars are set. Pushing a `vX.Y.Z` tag that matches `appVersionName` runs `.github/workflows/release.yml`, which builds a signed APK from repository secrets and publishes a GitHub Release (the in-app update check reads that release).

Tests live almost entirely in the app module (`nga_phone_base_3.0/src/test` and `src/androidTest`), including tests for `lib_core` code. Tests use synthetic/offline data only — never commit real account data or tests that post to the live forum.

## Module layout

- `nga_phone_base_3.0` — the application (`gov.anzong.androidnga`); depends on all libs. The only module with Compose enabled.
- `lib_core` — content decoding: `HtmlConvertFactory` and `core/decode` turn NGA post markup (BBCode-ish) into HTML for the article WebView, image URL normalization, etc. Has `compileOnly` on `lib_common`.
- `lib_common` — shared base classes, utils, RxJava helpers, `PreferenceKey` (setting keys — keep existing keys compatible), and `base/json` (`NgaJson`, the in-house JSON parser/binder that replaced fastjson; its lenient parsing, coercion and stored-data formats are pinned by tests in `sp/phone/json` — see `docs/fastjson-migration.md` before changing it).
- `lib_network` — thin HTTP callback interfaces.
- `lib_cloud` — cloud/update integration.

## App architecture

Two package roots in the app module, reflecting the project's history:

- `sp.phone.*` — legacy core: Retrofit + RxJava2 networking (`http/retrofit/RetrofitHelper`, `RetrofitService`), MVP (`mvp/contract`, `presenter`, `model`, `model/convert` for response → entity conversion), legacy fragments/adapters, user/account management (`common/UserManagerImpl`), WebView helpers.
- `gov.anzong.androidnga.*` — newer code: Activities (`activity/`), Room DB (`db/`), MVVM ViewModels (`mvvm/`), and all Compose UI in `ui/compose/` (`NgaTheme`, screens such as `HomeScreen`, `ArticleReaderScreen`, `PostEditorScreen`, `SettingsScreen`).

Key patterns:

- **Compose interop**: Compose screens are hosted by existing Activities/Fragments via `setContent` / `ComposeView`; business logic stays in the existing presenters/models/ViewModels. Topic detail uses a Compose Pager whose pages are `AndroidFragment`s that own per-page requests and lifecycle. Post bodies and the login page remain WebViews embedded via `AndroidView`. Simple replies without rich content render as native Compose text (`PlainArticleText`); anything with quotes/emoticons/attachments/signatures goes through the WebView.
- **Navigation**: `arouter/AppNavigator` is a hand-written explicit-Intent router (ARouter itself was removed). Route strings in `ARouterConstants` map to Activity classes in `AppNavigator.destination()`; `ACTIVITY_NEED_LOGIN` lists routes gated by login. Add new destinations there.
- **Theming**: user-selectable palettes (4) × light/dark/system, defined in `AppearanceSettings` / `NgaTheme`. Theme colors must propagate to WebView article content and remaining View screens, not just Compose.
- **Topic loading**: detail pages try sources in order JSON → plain web page (parsed with jsoup) → XML → backup XML, at most one request each, normalizing all of them into the common `__T`/`__R`/`__U` structure before conversion and caching. Truncated responses are detected and skipped; auth/forum/network errors are surfaced in-app and must not fall through to other sources, switch accounts, or open a browser. See `docs/topic-opening-fix.md` before touching this path.
- **Forum favorites** on the home screen are server-side and cached per account; account switches must not leak another account's cache or let stale requests overwrite current state (`docs/forum-favorites-sync.md`).

Removed and should not be reintroduced: fastjson (use `NgaJson`), Kotlin Android Extensions, ButterKnife, ARouter, Jetifier, legacy Support libraries, `WRITE_EXTERNAL_STORAGE` (use MediaStore / system document picker), full-screen swipe-back (use `OnBackPressedDispatcher`). Windows are edge-to-edge; handle insets.

## Further docs

`docs/compose-migration.md` (migration scope), `docs/interface-redesign.md`, `docs/topic-scrolling-performance.md` (WebView reuse/height caching in the reader list), `docs/topic-opening-fix.md`, `docs/forum-favorites-sync.md`, `docs/fastjson-migration.md`.
