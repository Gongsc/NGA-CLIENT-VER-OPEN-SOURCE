# Compose 与现代 Android 迁移

## 平台与构建

- 仅支持 Android 12 及以上（minSdk 31）。Android 11 及更早版本不再支持安装。
- compileSdk / targetSdk：37（Android 17）。
- Android Gradle Plugin 9.4.1，Gradle Wrapper 9.6.0，JDK 17。
- Kotlin 2.4.21，使用 AGP 内置 Kotlin 和同版本 Compose Compiler 插件。
- Compose BOM 2026.09.00、Material 3；SDK 版本统一定义于根目录 build.gradle。
- 仓库配置集中于 settings.gradle，仅使用 HTTPS。
- 不再使用 kotlin-android-extensions、ButterKnife、ARouter 编译器、Jetifier、旧 Support 库、旧滑动返回库及 floatingactionmenu.aar。
- Java 资源判断使用普通条件语句，采用现代默认的非 final、非传递 R 类。
- 发布签名由发布环境配置，仓库不再内置签名密码或机器相关的证书路径。

## 已迁移界面

- 首页：Compose 导航抽屉、分类标签、自适应版块网格、账号切换、收藏前移/后移/删除。
- 版块目录：Compose 搜索筛选、加载与失败重试、空状态，ViewModel 持有目录数据。
- 搜索：主题/版块/用户、正文选项、当前版块筛选、用户名/UID、历史重排与删除。
- 主题列表、搜索结果和帖子收藏：Compose 列表、下拉刷新、分页、删除确认、标题样式、版块菜单与发帖入口。
- 登录：Compose 导航、进度与错误提示、官方网页登录、Cookie 校验、取消与成功结果区分。
- 编辑器：Compose 标题与正文、格式选区、完整表情分组、主题分类、系统附件选择、上传与提交状态、提交确认及未提交内容退出保护。
- 帖子详情：Compose Pager、楼层列表、操作菜单、页码与楼层跳转、引用及编辑、收藏与缓存；正文使用 LocalWebView 互操作。
- 贴条：Compose 对话框、长度校验、匿名选项、提交反馈与楼层列表刷新。
- 关于：Compose 应用信息、许可证、更新与贡献者信息。
- 字号设置：Compose Slider 和预览，沿用已有配置键。
- 设置子页面：关键词屏蔽（添加、去重、启用开关、确认删除）、用户黑名单（移除、清空确认）、账号管理（切换当前账号、上移下移、查看资料、确认移除、添加账号），作为设置页内的子页面，由 `SettingsListsViewModel` 读写。
- 浏览历史：主题列表菜单中的浏览历史改为 `TopicHistoryActivity`，与首页“历史”共用 `HomeHistoryScreen`（按日分组、长按删除、清空确认）。
- 回复通知：`RecentNotificationActivity`（系统通知栏“被喷提醒”打开的页面）改为 Compose，与首页“消息”共用 `HomeInboxViewModel` 和 `ReplyNotificationCard`；显示通知类型、时间与未读标记，按时间从新到旧排列，下拉刷新，清空前确认。
- 子版块：主题列表菜单中的子版块改为 `SubBoardActivity`，由 `SubBoardViewModel` 管理显示开关；请求经 `ForumActionClient`/`NgaWriteClient` 提交到当前论坛域名，在 `AppScope` 中执行，开关状态以服务端结果为准，请求进行中显示进度且不能重复点击。未使用的版块分类分页（`BoardCategoryFragment`、`BoardPagerAdapter`）与 `LauncherSubActivity` 已删除。

Compose 页面通过 ComposeView / setContent 接入现有 Activity、Fragment 和业务层；Fragment 中的 composition 跟随 View 生命周期销毁。

登录认证网页与帖子正文保留 WebView，通过 AndroidView 接入 Compose。详情分页采用 Compose Pager，并通过 AndroidFragment 管理单页请求及生命周期。用户资料、私信与缓存管理仍保留原有 View 实现。原登录、编辑器及楼层列表 XML、列表适配器和格式工具面板已移除。

## 系统行为

- 统一边到边窗口；Compose Scaffold 处理系统栏，保留的 View 页面应用系统栏、刘海和键盘 Insets。
- 返回操作接入 OnBackPressedDispatcher；不再劫持整屏滑动手势。
- 通知使用不可变 PendingIntent，并处理 Android 13 及以上的通知权限。
- 保存图片和导出缓存使用 MediaStore，不再声明或请求 WRITE_EXTERNAL_STORAGE，也不启用旧外部存储模式。
- 附件和缓存导入使用系统文档选择器。既有附件上传通过 ContentResolver 读取 URI。
- 页面跳转使用显式 Intent 映射，保留原有路由参数与登录拦截行为。

## 本地验证

安装 JDK 17、Android SDK Platform 37 和 Build Tools 36，配置 ANDROID_HOME 或本机 local.properties 后执行：

```sh
./gradlew :nga_phone_base_3.0:assembleDebug \
  :nga_phone_base_3.0:testDebugUnitTest \
  :nga_phone_base_3.0:lintDebug
```

调试包位于 nga_phone_base_3.0/build/outputs/apk/debug/。

单元测试共 19 项，覆盖筛选特殊字符、大小写与空白、历史去重与重排、合集唯一键、导航映射、登录 Cookie 解析、域名校验、编辑器选区处理及页码与楼层边界。Android 12 模拟器上的 8 项设备交互测试覆盖提交确认、退出保护、编辑内容重建恢复、登录取消、分页、引用、离线详情宿主及 HTML 高度测量。Lint 保持错误阻断；SARIF 导出因 AGP 9.4.1 报告器发生数组越界而关闭，HTML、文本与 XML 报告仍保留。

设备测试使用本地回调和离线数据验证交互，不提交真实帖子或回复。小屏横屏、1.5 倍字体与关闭系统动画配置下的 4 项核心交互测试亦已通过。Android 17 模拟器仍存在启动挂起问题。发布前仍需验证真实认证、服务端提交、上传与完整系统行为，并覆盖 Android 12 与 Android 17、深浅色、较大字体和横屏。

## 官方参考

- https://developer.android.com/compose
- https://developer.android.com/develop/ui/compose/migrate/strategy
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://developer.android.com/build/releases/agp-9-4-0-release-notes
