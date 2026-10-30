# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Open-source Android client for the NGA forum (bbs.nga.cn). The UI is Jetpack Compose + Material 3; the app module has no XML layouts left (WebViews and per-page `ArticleListFragment`s are embedded via `AndroidView`/`AndroidFragment`). Project docs, README, and commit messages are written in Chinese — keep new docs/commits in Chinese to match.

Toolchain: JDK 17, AGP 9.4.1 (built-in Kotlin, no `kotlin-android` plugin), Kotlin/Compose Compiler 2.4.21, Gradle 9.6.0, minSdk 31, compile/targetSdk 37, Build Tools 36.0.0. SDK levels and shared dependency versions live in `project.ext` of the root `build.gradle`; repositories are centralized in `settings.gradle` (`FAIL_ON_PROJECT_REPOS` — don't add repos in module build files).

## Commands

```sh
# Debug build (APK → nga_phone_base_3.0/build/outputs/apk/debug/)
./gradlew :nga_phone_base_3.0:assembleDebug

# Full local check: build + unit tests + lint (lint aborts on error)
./gradlew :nga_phone_base_3.0:assembleDebug :nga_phone_base_3.0:testDebugUnitTest :nga_phone_base_3.0:lintDebug

# Single unit test class / method
./gradlew :nga_phone_base_3.0:testDebugUnitTest --tests 'gov.anzong.androidnga.json.NgaJsonParsingTest'
./gradlew :nga_phone_base_3.0:testDebugUnitTest --tests 'gov.anzong.androidnga.arouter.AppNavigatorTest.someMethod'

# Instrumented tests (needs an Android 12+ device/emulator)
./gradlew :nga_phone_base_3.0:connectedDebugAndroidTest
./gradlew :nga_phone_base_3.0:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fqcn>
```

Debug builds use applicationId `gov.anzong.androidnga.debug` (label "NGA测试版") so they install alongside the release app without touching its data; the instrumentation package is `gov.anzong.androidnga.debug.test`. Prefer `installDebug` + `installDebugAndroidTest` + `adb shell am instrument` over `connected*` tasks, which uninstall afterwards.

Reports: `nga_phone_base_3.0/build/reports/tests/testDebugUnitTest/`, `.../reports/lint-results-debug.html`, `.../reports/androidTests/connected/debug/`. SARIF lint export is intentionally disabled (AGP 9.4.1 reporter crashes). Release signing is not in the repo: `assembleRelease` signs only when the `NGA_KEYSTORE_*`/`NGA_KEY_*` env vars are set. Pushing a `vX.Y.Z` tag that matches `appVersionName` runs `.github/workflows/release.yml`, which builds a signed APK from repository secrets and publishes a GitHub Release (the in-app update check reads that release).

Tests live almost entirely in the app module (`nga_phone_base_3.0/src/test` and `src/androidTest`), including tests for `lib_core` code. Tests use synthetic/offline data only — never commit real account data or tests that post to the live forum.

## Module layout

- `nga_phone_base_3.0` — the application (`gov.anzong.androidnga`); depends on all libs. The only module with Compose enabled.
- `lib_core` — content decoding: `HtmlConvertFactory` and `core/decode` turn NGA post markup (BBCode-ish) into HTML for the article WebView, image URL normalization, etc. Has `compileOnly` on `lib_common`.
- `lib_common` — shared base classes, utils, `PreferenceKey` (setting keys — keep existing keys compatible), and `base/json` (`NgaJson`, the in-house JSON parser/binder that replaced fastjson; its lenient parsing, coercion and stored-data formats are pinned by tests in `gov/anzong/androidnga/json` (unit tests) — see `docs/fastjson-migration.md` before changing it).
- `lib_network` — thin HTTP callback interfaces.

## App architecture

All app code lives under `gov.anzong.androidnga.*` (the legacy `sp.phone.*` root was merged in) and is Kotlin, except the Room entity/DAO/database (`account/User`, `db/user/UserDao`, `db/AppDatabase`) and `util/CustomGlideModule`, which stay Java because Room and Glide run as javac annotation processors (Kotlin code cannot see `GlideApp`; use `Glide.with`). Main packages: `activity/` (one Activity per page), `ui/compose` (Compose screens, `NgaTheme`), `ui/dialog`, `ui/fragment` (`ArticleListFragment`), `ui/webview` (`LocalWebView`, link routing, `ProxyBridge`), `ui/theme`, `mvvm/viewmodel`, `mvvm/model` (repositories, `BoardModel`/`BoardStore`, `entity`, `param`, `convert` for response → entity conversion), `network` (`retrofit/RetrofitHelper` + suspend API `NgaApi`, `post`, `article` topic loading, `favorites`, `bean` response beans), `account` (`UserManagerImpl`), `settings` (`PhoneConfiguration`, keyword filter), `task`, `coroutines`, `db`, `util`.

JSON beans bound by `NgaJson` are matched by getter/setter names, so when editing them keep the generated JVM accessor names (some use `@get:JvmName`, e.g. `ThreadRowInfo.isAnonymous` → `getISANONYMOUS`); the golden tests in `src/test/.../json` catch changes. When porting Java string code to Kotlin, mind that Kotlin `trim()`/`split()`/`Any?.toString()` differ from Java (use `trim { it <= ' ' }`, drop trailing empties, `!!`).

Key patterns:

- **Compose interop**: Every page is an Activity that calls `setContent` directly (no page-level Fragments remain; the only Fragments are the per-page `ArticleListFragment` and dialogs); business logic lives in ViewModels (no MVP presenters remain). ViewModels read Intent extras (or fragment arguments) through a `SavedStateHandle` constructor — an Activity that derives its parameters puts them back into `intent` before the ViewModel is first created — take requests as injectable suspend functions or repositories so they can be unit-tested with `InstantTaskExecutorRule`/`UnconfinedTestDispatcher`, run reads in `viewModelScope` and forum writes in `AppScope` (a write is never abandoned half-way), and start their first load once via `start()` so recreation does not re-request. Topic detail uses a Compose Pager whose pages are `AndroidFragment`s that own per-page requests and lifecycle. Post bodies, signatures, private-message bodies, polls, web pages and the login page remain WebViews embedded via `AndroidView`. Dialogs extend `ui/dialog/BaseDialogFragment` (a DialogFragment whose `Content()` is Compose, laid out with `NgaDialogLayout`) and return results via the Fragment Result API. Simple replies without rich content render as native Compose text (`PlainArticleText`); anything with quotes/emoticons/attachments/signatures goes through the WebView.
- **Navigation**: `arouter/AppNavigator` is a hand-written explicit-Intent router (ARouter itself was removed). Route strings in `ARouterConstants` map to Activity classes in `AppNavigator.destination()`; `ACTIVITY_NEED_LOGIN` lists routes gated by login. Add new destinations there. When a result is needed, build the Intent with `NavigationRequest.intent(context)` and launch it from a `registerForActivityResult` launcher.
- **Theming**: user-selectable palettes (4) × light/dark/system, defined in `AppearanceSettings` / `NgaTheme`. Theme colors must propagate to WebView article content and remaining View screens, not just Compose.
- **Topic loading**: detail pages try sources in order JSON → plain web page (parsed with jsoup) → XML → backup XML, at most one request each, normalizing all of them into the common `__T`/`__R`/`__U` structure before conversion and caching. Truncated responses are detected and skipped; auth/forum/network errors are surfaced in-app and must not fall through to other sources, switch accounts, or open a browser. See `docs/topic-opening-fix.md` before touching this path.
- **Coroutines**: requests are suspend functions on `NgaApi`. Java callers keep `OnHttpCallBack`; Kotlin tasks/models bridge with `launchWithCallback` (work on `NgaDispatchers.io`, callbacks on main, no callback after cancellation) in `scopeOf(lifecycleOwner)` or `AppScope` (`gov.anzong.androidnga.coroutines`). There is no `HttpURLConnection` left: forum reads go through `NgaApi` (whose `String` converter always decodes GBK; read a `ResponseBody` when the response declares another charset), writes through `NgaWriteClient`, and the GitHub update check through its own OkHttp client. `BackgroundTask.runSerially` only orders background writes of the same data (the account list). Page events are `UiEvent`s on `UiEventBus` (`SharedFlow`); subscribe with `UiEventBus.observe(lifecycleOwner, consumer)`. Tests swap `Dispatchers.setMain`/`NgaDispatchers.io` and use `FakeNgaApi`/`PendingResponse`. See `docs/rxjava-migration.md`.
- **Posting / writes**: every forum write goes through `network/post/NgaWriteClient` (UTF-8 form, `access_uid`/`access_token`, SNGA User-Agent, no redirects/retries, unrecognizable results reported as unconfirmed). New topics, replies, quotes, edits and 贴条 use `TopicPostClient` (protocol mirrored from SNGA): preflight `post.php?…&lite=xml&__inchst=UTF8` for `auth`, then submit `step=2` + `auth` as UTF-8 form with `access_uid`/`access_token`; success/failure comes from `<__MESSAGE>`/`pid`, and an unrecognizable result is reported as unconfirmed, never as success. Replies are scoped by `tid` only (the default `fid=-7` must not be sent). Private messages use `MessagePostClient`; avatar, signature, poll votes, likes, bookmarks, reports, clearing notifications and sub-board show/hide switches use `ForumActionClient` (the `LikeTask`/`BookmarkTask`/`ReportTask`/`SignPostTask` wrappers only move it off the main thread; failures and unconfirmed results are surfaced, by default as toasts) (the WebView `ProxyBridge.postURL` only accepts `__lib=vote` vote/settle). Don't reintroduce hand-built GBK request bodies.
- **Forum favorites** on the home screen are server-side and cached per account; account switches must not leak another account's cache or let stale requests overwrite current state (`docs/forum-favorites-sync.md`).

Removed and should not be reintroduced: Tencent Bugly, Umeng and the `lib_cloud` module, fastjson (use `NgaJson`), RxJava/rxandroid/rxlifecycle/rxbinding and `AsyncTask` (use coroutines), Kotlin Android Extensions, ButterKnife, ARouter, Jetifier, legacy Support libraries, `WRITE_EXTERNAL_STORAGE` (use MediaStore / system document picker), full-screen swipe-back (use `OnBackPressedDispatcher`), `startActivityForResult`/`onActivityResult` (use the Activity Result API), `ProgressDialog` (use a Material dialog with a progress indicator), ViewPager v1/`FragmentStatePagerAdapter` (use ViewPager2/`FragmentStateAdapter` or a Compose pager). Windows are edge-to-edge; handle insets.

## Further docs

`docs/compose-migration.md` (migration scope), `docs/interface-redesign.md`, `docs/topic-scrolling-performance.md` (WebView reuse/height caching in the reader list), `docs/topic-opening-fix.md`, `docs/forum-favorites-sync.md`, `docs/fastjson-migration.md`, `docs/rxjava-migration.md`.
