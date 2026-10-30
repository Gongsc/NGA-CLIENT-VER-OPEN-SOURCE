# NGA Android 客户端

NGA 论坛（bbs.nga.cn）的开源 Android 客户端，提供版块浏览、主题阅读、搜索、论坛收藏、短消息、账号管理与发帖等功能。界面已全部基于 Jetpack Compose 与 Material 3 重新实现，构建工具链升级到 AGP 9.4.1 与 Kotlin 2.4.21。

当前版本仅支持 **Android 12（API 31）及以上系统**。

## 目录

- [界面预览](#界面预览)
- [功能与页面](#功能与页面)
- [开发环境](#开发环境)
- [获取与构建](#获取与构建)
- [测试与静态检查](#测试与静态检查)
- [项目结构与架构](#项目结构与架构)
- [功能说明](#功能说明)
- [版本记录](#版本记录)
- [参考资料](#参考资料)
- [开源许可](#开源许可)

## 界面预览

<table>
  <tr>
    <td align="center"><img src="docs/previews/implemented/compact-home.png" width="200" alt="首页：底部导航与版块收藏"></td>
    <td align="center"><img src="docs/previews/implemented/topic-post-expanded-light.png" width="200" alt="主题列表：浮起卡片与热帖回复数"></td>
    <td align="center"><img src="docs/previews/implemented/reader-floating-light.png" width="200" alt="主题阅读：楼层卡片与悬浮翻页"></td>
    <td align="center"><img src="docs/previews/implemented/appearance-dark.png" width="200" alt="外观与布局：四套配色与深色模式"></td>
  </tr>
  <tr>
    <td align="center">首页</td>
    <td align="center">主题列表</td>
    <td align="center">主题阅读</td>
    <td align="center">配色与深色模式</td>
  </tr>
</table>

截图由设备测试在 Android 16 实机上使用离线示例数据生成，不包含真实账号内容。

## 功能与页面

全部页面与对话框均使用 Compose 绘制，应用模块不包含 XML 布局。

| 页面 | 主要内容 |
| --- | --- |
| 首页 | 底部导航（首页、历史、消息、我的），选中项图标凸出底栏并高亮；分类标签、自适应版块网格、论坛收藏同步与管理 |
| 我的 | 账号头像与切换、添加账号，我的主题、回复、收藏与缓存，论坛工具，外观、设置与关于入口 |
| 版块目录与搜索 | 版块筛选与重试；主题、版块及用户搜索，正文搜索，用户名与 UID 切换，搜索历史 |
| 主题列表 | 下拉刷新、分页加载、版块收藏、精华区、24 小时热帖、版头、子版块与发帖入口 |
| 帖子详情 | Compose 分页、楼层卡片、热点回复、页码与楼层跳转、引用、楼层操作，普通回复原生排版，富文本经 WebView |
| 发帖、回复与编辑 | 标题与正文、格式与表情工具、附件上传、提交确认、退出提示，重建后保留编辑内容 |
| 登录 | 官方网页登录、加载与错误状态、登录结果校验与取消处理 |
| 短消息与回复通知 | 会话列表与账号切换、会话内容、写信与回复；通知列表、未读标记与清空确认 |
| 用户资料 | 基本信息、金钱、声望、管理版块、签名；禁言、激活状态与“状态”（buff）按网页规则显示 |
| 修改签名与头像 | 签名可清空；头像可填写图片地址，或从相册选择后截取（缩放、拖动、调整截取框，可选正方形）再上传 |
| 设置 | 分类设置、关键词屏蔽、用户黑名单、账号管理、浏览历史、子版块显示开关 |
| 外观与布局 | 四套配色、深色模式、版块密度；字体与阅读大小带实时预览 |
| 其他 | 我的缓存与离线阅读、图片浏览（翻页、缩放、保存）、网页浏览、关于与检查更新，以及添加版面、举报、投票等对话框 |

详细迁移范围见 [Compose 迁移说明](docs/compose-migration.md)。

## 开发环境

| 配置项 | 版本或要求 |
| --- | --- |
| 最低系统版本 | Android 12，API 31 |
| 编译 SDK / 目标 SDK | Android 17，API 37 |
| JDK | 17 |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.8.2 |
| Kotlin / Compose Compiler 插件 | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Android SDK Build Tools | 36.0.0 |

Kotlin 编译采用 AGP 内置支持，Compose Compiler 使用独立插件配置。SDK 版本与公共依赖版本集中定义于根目录 `build.gradle`，依赖仓库统一配置于 `settings.gradle`，仅使用 Google 与 Maven Central。应用不接入第三方统计或崩溃上报服务。

## 获取与构建

### 环境配置

1. 获取本仓库，并使用支持当前工具链的 Android Studio 打开项目根目录。
2. 将 Gradle JDK 配置为 JDK 17。
3. 安装 Android SDK Platform 37 与 Android SDK Build Tools 36.0.0。
4. 通过 `ANDROID_HOME` 环境变量或根目录 `local.properties` 文件配置本机 SDK 路径：

```properties
sdk.dir=/path/to/android-sdk
```

本机路径配置与签名凭据不应提交至版本控制。

### 调试构建

```sh
./gradlew :nga_phone_base_3.0:assembleDebug
```

Windows 环境使用 `gradlew.bat`。调试版的包名为 `gov.anzong.androidnga.debug`，桌面名称为“NGA测试版”，可与正式版同时安装，数据互不影响。APK 输出至 `nga_phone_base_3.0/build/outputs/apk/debug/`。

### 发布构建

仓库不包含发布证书与签名密码。发布构建通过环境变量 `NGA_KEYSTORE_FILE`、`NGA_KEYSTORE_PASSWORD`、`NGA_KEY_ALIAS` 与 `NGA_KEY_PASSWORD` 读取签名配置；未设置时生成未签名的 `nga_phone_base_3.0-release-unsigned.apk`，不得作为正式安装包分发。

### 自动发布

推送 `v4.3.2` 格式的版本标签后，[发布工作流](.github/workflows/release.yml)会执行单元测试、构建签名发布包，检查安装包包含各模块的关键类，并创建对应的 GitHub Release。附件为 `NGA-<版本号>.apk` 与 `SHA256SUMS.txt`，发布说明取自 `VersionUpgradeTips` 中当前版本代码的更新说明。标签带 `-` 后缀（如 `v4.3.1-beta.1`）时创建预发布，不会出现在应用的检查更新结果中。标签与 `appVersionName` 不一致、缺少签名 Secrets 或缺少关键类时，工作流直接失败，不创建发布。

首次使用前，需在仓库 Settings → Secrets and variables → Actions 中添加：

| Secret | 内容 |
| --- | --- |
| `NGA_KEYSTORE_BASE64` | 发布证书文件的 Base64 编码 |
| `NGA_KEYSTORE_PASSWORD` | 证书库密码 |
| `NGA_KEY_ALIAS` | 密钥别名 |
| `NGA_KEY_PASSWORD` | 密钥密码 |

证书文件可用 `base64 -i release.jks | pbcopy`（macOS）或 `base64 -w0 release.jks`（Linux）转换。后续版本必须使用同一证书签名，否则无法覆盖安装。

发布步骤：

1. 在根目录 `build.gradle` 中更新 `appVersionName` 与 `appVersionCode`（`主版本×1000 + 次版本×100 + 修订号`），并在 `VersionUpgradeTips` 中添加对应的 `TIPS_<版本代码>` 更新说明。
2. 提交并推送后，创建与 `appVersionName` 一致的标签并推送：`git tag v4.3.2 && git push origin v4.3.2`。

应用“关于 → 检查更新”读取本仓库的最新正式发布，按数字比较发布标签与 `appVersionName`，并显示发布说明。库模块的 release 构建不开启混淆，避免删除模块代码（4.1.0 与 4.2.0 曾因此启动闪退，已在 4.2.1 修复）。

## 测试与静态检查

```sh
# 调试构建、单元测试与 Lint（Lint 错误会中断构建）
./gradlew :nga_phone_base_3.0:assembleDebug \
  :nga_phone_base_3.0:testDebugUnitTest \
  :nga_phone_base_3.0:lintDebug

# 设备交互测试（需连接 Android 12 及以上设备或模拟器）
./gradlew :nga_phone_base_3.0:connectedDebugAndroidTest
```

`connected*` 任务结束后会卸载应用；在实机上可改用 `installDebug`、`installDebugAndroidTest` 后通过 `adb shell am instrument -w gov.anzong.androidnga.debug.test/androidx.test.runner.AndroidJUnitRunner` 运行。

| 报告 | 路径 |
| --- | --- |
| 单元测试 | `nga_phone_base_3.0/build/reports/tests/testDebugUnitTest/index.html` |
| Lint | `nga_phone_base_3.0/build/reports/lint-results-debug.html` |
| 设备测试 | `nga_phone_base_3.0/build/reports/androidTests/connected/debug/index.html` |

**当前结果（2026 年 10 月 30 日）**：调试构建、295 项单元测试通过，Lint 无错误与警告，104 项设备交互测试在 Android 16 实机上全部通过。此前 93 项设备测试已在 Android 12 模拟器上通过；小屏横屏、1.5 倍字体与关闭系统动画配置下的核心交互测试亦已通过。由于当前 AGP 的 SARIF 报告器存在数组越界问题，暂时关闭 SARIF 导出。Android 17 设备测试尚未完成（模拟器启动挂起）；登录、发帖、上传、点赞、收藏、举报、签名与头像等服务端操作无法自动化测试，发布前需用测试账号人工检查。

测试只使用合成或离线数据，不提交真实帖子与回复：

- **单元测试**：各页面 ViewModel、论坛提交协议（合成响应）、主题响应格式兼容与截断检测、正文解码（列表、折叠、表情、图片域名）、链接识别、论坛收藏与账号隔离、资料状态规则、头像截取几何、检查更新与版块目录请求、`NgaJson` 的兼容格式等。
- **设备测试**：各 Compose 页面与对话框的交互及重建恢复、发帖与退出确认、登录取消、分页与楼层跳转、长正文高度测量、整页主题换色、底栏显隐、字体设置预览、离线缓存阅读等。

## 项目结构与架构

```text
.
├── nga_phone_base_3.0/   # 应用模块（唯一启用 Compose 的模块）
├── lib_core/            # 正文解码：论坛代码转 HTML、表情与图片地址
├── lib_common/          # 公共组件、工具、设置键与 NgaJson
├── lib_network/         # 网络回调接口
├── docs/                # 重构与迁移文档
├── build.gradle         # 构建插件、SDK 与公共版本配置
└── settings.gradle      # 模块声明与依赖仓库配置
```

- **包与语言**：代码均位于 `gov.anzong.androidnga.*`（原 `sp.phone.*` 已并入：`account` 账号、`settings` 设置、`network` 网络请求与提交、`mvvm` ViewModel 与数据、`ui` 界面、`task` 后台任务、`util` 工具）。除 Room 实体与 DAO（`User`、`UserDao`、`AppDatabase`）及 Glide 模块需要 Java 注解处理器而保留 Java 外，主代码均为 Kotlin。
- **页面**：每个页面是一个 Activity，直接通过 `setContent` 显示 Compose 界面；整页 Fragment 已全部移除，只有详情分页的 `ArticleListFragment`（由 `AndroidFragment` 嵌入）与对话框仍是 Fragment。帖子正文、签名、短消息正文、投票、网页浏览与登录页为通过 `AndroidView` 嵌入的 WebView。Compose 界面与主题位于 `gov.anzong.androidnga.ui.compose`。
- **业务逻辑**：由 ViewModel 承担，经 `SavedStateHandle` 读取页面参数；读取请求随页面结束取消，论坛提交在应用级作用域执行，离开页面也不会中途放弃；首次加载只执行一次，页面重建不重复请求。
- **网络**：读取请求为 Retrofit 挂起接口 `NgaApi`，异步统一使用 Kotlin 协程；论坛写操作统一经 `NgaWriteClient` 提交，见[发帖与论坛操作](#发帖与论坛操作)。
- **导航**：`AppNavigator` 以显式 `Intent` 映射页面，保留既有参数与登录保护规则；需要结果时使用 Activity Result API。

## 功能说明

### 首页与导航

首页底栏包含“首页”“历史”“消息”“我的”。底栏高 56 dp，选中项的图标放大为主题色圆形并凸出底栏顶部，名称加粗变色，切换时以动画过渡；凸出区域外不拦截点击。列表向下滚动时底栏隐藏，反向滚动时恢复。消息页签显示回复通知的未读数。

“我的”页包含账号头部（头像、昵称、UID，点击进入个人资料，可切换或添加账号）、账号相关入口（我的主题、我的回复、收藏夹、我的缓存）、论坛工具（查找版块、按编号添加版块、通过链接打开、搜索用户）以及外观与布局、设置、关于。未登录时显示默认头像与登录按钮。修改头像后，底栏与“我的”页的账号头像随之更新；账号头像以个人资料为准，不会被帖子中的旧头像覆盖。

设置页分为阅读与浏览、主题列表、消息通知、发帖、账号与屏蔽、网络与存储。关键词屏蔽、用户黑名单与账号管理是设置页内的子页面，删除与清空前均需确认；账号列表按保存顺序持久化，移除或调整其他账号时当前账号保持不变。原有设置键值保持兼容。

### 外观与阅读

“外观与布局”提供松绿、雾蓝、暖砂和石墨四种配色，以及浅色、深色、跟随系统三种模式。主题覆盖页面背景、内容区域、分隔线、底栏及正文网页。版块列表提供标准与紧凑两种密度，默认紧凑。

“字体与阅读大小”顶部为实时预览（主题列表的一行与一个楼层，含头像、正文与表情），下方按“主题列表”（标题字号）、“帖子正文”（正文大小、表情大小）、“楼层”（头像大小）分组调整，每项有数值、加减按钮与滑块，可一键恢复默认。正文网页使用移动端视口，1 CSS 像素等于 1 dp，预览与帖子中的实际大小一致。

普通回复与富文本共用正文缩放设置；楼层与热点回复使用 14 dp 圆角卡片，随主题变化。主题首页存在热点回复时，在主楼与正常楼层之间显示“热点回复”分区，热点不参与楼层计数与分页。无特殊格式的回复使用 Compose 原生文本排版，正文网页容器支持复用与高度缓存，见[主题滚动性能记录](docs/topic-scrolling-performance.md)。

### 正文解码与图片

论坛代码由 `lib_core` 转换为 HTML。列表（`[list]`、`[*]`）与折叠（`[collapse]`）的开闭标签按配对转换，支持嵌套与跨行，未配对的折叠标签按原文显示，不会破坏其后内容。

`*.nga.178.com` 图片域名已无法访问。正文、附件、短消息与签名中的相对图片地址及旧附件域名统一转换为 `https://img.nga.cn`，音视频同样使用该域名；表情、版块图标等 `ngabbs` 路径使用 `https://img4.nga.cn`。旧表情按路径匹配内置图片。外部图片域名保持原值，关闭图片加载时显示占位图。

### 主题加载与链接处理

主题详情依次尝试 JSON、普通网页、XML 与备用 XML，单次加载最多四次请求，各来源归一化后进入统一的校验、楼层转换与缓存流程；截断的响应会被识别并跳过。登录、权限与网络错误直接在应用内显示，保留当前页面并提供重新加载，不会自动打开浏览器、关闭页面或切换账号。正文中的 NGA 主题、回复与版块链接按实际域名与路径识别，保留分页、作者筛选与楼层定位参数。详见[主题加载修复说明](docs/topic-opening-fix.md)。

### 版块收藏同步

首页“我的收藏”显示当前账号的论坛收藏。登录后自动读取，进入首页时检查同步状态（相邻成功请求间隔 60 秒），也可手动同步。收藏、取消收藏与按编号添加版块直接提交至论坛，经论坛确认后才更新首页与缓存；处理中禁止重复提交。缓存按账号隔离，切换账号时旧请求不会覆盖当前数据。详见[版块收藏同步说明](docs/forum-favorites-sync.md)。

### 发帖与论坛操作

所有论坛写操作经 `NgaWriteClient` 提交：UTF-8 表单附带 `access_uid`/`access_token`，使用 SNGA 的 User-Agent，不跟随重定向、不自动重试；结果按论坛返回的提示判断，无法确认时提示刷新查看，不当作成功，避免重复提交。

- 发帖、回复、引用、编辑及贴条参照 SNGA 协议：先请求 `post.php` 预检取得 `auth` 再提交；回复与编辑只按主题编号提交。发帖表情插入网页版表情代码（如 `[s:ac:茶]`）。
- 短消息经 `MessagePostClient` 提交；头像、签名、投票、点赞、收藏、举报、清空提醒与子版块显示开关经 `ForumActionClient` 提交。失败或结果不明时提示原因。
- 头像上传与网页“设置头像”一致：取得上传地址与校验码，上传截取后的图片（不超过 180×255，GIF 可上传原图），再设为头像；上传地址只接受论坛域名。

### 登录

登录使用 NGA 官方网页完成认证，应用根据 WebView 实际视口高度调整嵌套登录框架，避免部分设备只显示灰色背景；加载失败时显示错误与重试。完成登录时从 Cookie 读取账号信息，信息不完整时提示继续登录。

### 平台适配

- 边到边窗口，处理系统栏、屏幕开孔及软键盘 Insets；返回操作接入 `OnBackPressedDispatcher`。
- 通知使用不可变 `PendingIntent`，并处理 Android 13 及以上的通知权限。
- 图片保存与缓存导出使用 `MediaStore`，附件选择与缓存导入使用系统文档选择器，不申请存储权限。

## 版本记录

- **4.3.2**：主代码与三个库模块由 Java 转为 Kotlin（仅保留 Room 与 Glide 注解处理所需的 4 个 Java 文件），删除无引用的旧代码；`sp.phone.*` 并入 `gov.anzong.androidnga.*`；修复调整账号顺序后当前账号跳到第一个、短消息列表遇到异常条目时卡住、主题按发帖时间排序可能出错。Lint 警告清零：菜单文字移入字符串资源，删除无用资源与图标，版块图标改为静态映射，适配主题图标；单元测试全部改为 Kotlin；OkHttp 升级至 5.5.1，Glide 升级至 5.0.9，Gradle Wrapper 升级至 9.8.2。
- **4.3.1**：所有页面与对话框改为 Compose，整页 Fragment 合并进 Activity；用户资料的禁言与激活状态按网页规则判断，buff 单独显示为“状态”；头像支持从相册截取后上传，修改后首页头像同步更新；签名可清空；点赞、收藏、举报、签名与清空提醒改经 `NgaWriteClient` 提交，失败时提示原因；投票提交时在对话框内显示进度；首页底栏缩小并突出当前页签；字体与阅读大小加入实时预览，头像大小设置实际生效；修复列表与折叠标签冲突。代码方面移除剩余的 `HttpURLConnection`，删除无引用的资源与遗留 TODO，Lint 警告由 173 项降至 86 项。
- **4.3.0**：Presenter 全部迁移至 ViewModel；发帖、回复、短消息、头像与投票改用新的提交方式；设置子页面、浏览历史、回复通知与子版块改为 Compose；修复账号删除与排序、版块图标与音视频加载等问题；移除 Bugly 与友盟统计。
- **4.2.1**：修复 4.1.0、4.2.0 正式版因库模块被混淆而启动闪退。
- **4.2.0**：fastjson 替换为自有的 `NgaJson`（本地数据格式保持兼容，见 [fastjson 替换说明](docs/fastjson-migration.md)），RxJava 与 `AsyncTask` 替换为 Kotlin 协程（见 [RxJava 替换说明](docs/rxjava-migration.md)），Retrofit、OkHttp、Glide 升级，图片浏览页改用 Compose，移除 Guava、commons-io、PhotoView 等依赖。

## 参考资料

- [MNGA 主题请求与响应兼容实现](https://github.com/BugenZhao/MNGA/blob/main/logic/service/src/fetch.rs)
- [Jetpack Compose 官方文档](https://developer.android.com/compose)
- [Compose 渐进式迁移策略](https://developer.android.com/develop/ui/compose/migrate/strategy)
- [AGP 内置 Kotlin 迁移指南](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Android Gradle Plugin 9.4 发布说明](https://developer.android.com/build/releases/agp-9-4-0-release-notes)

## 开源许可

本项目采用 GNU General Public License v2，具体条款见 [LICENSE](LICENSE)。第三方依赖的许可要求应分别遵循其对应的许可文件。
