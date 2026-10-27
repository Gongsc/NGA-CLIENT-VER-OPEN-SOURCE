# RxJava 替换为 Kotlin 协程

## 背景

项目使用 RxJava 2.1.5、rxandroid、rxlifecycle 2 与 rxbinding 2，均已停止维护。30 个业务文件引用 RxJava，其中 26 个为 Java。目标是全部改用 Kotlin 协程与 Flow，并删除上述依赖及 Retrofit 的 `adapter-rxjava2`。

## 现状

| 层 | 文件 | 说明 |
|---|---|---|
| 网络接口 | `RetrofitService.java` | 14 个方法全部返回 `Observable` |
| 主题加载 | `ArticlePageLoader.java` | 用 `defer`、`onErrorResumeNext` 按 JSON → 网页 → XML 切换来源 |
| 已是 Kotlin | `ForumFavoritesRepository`、`ForumFavorites`、`HomeInboxViewModel`、`NavigationDrawerFragment` | |
| 后台任务 | `BaseRxTask` 与 `sp/phone/task/` 下 8 个任务、`SaveImageTask` | 通过 `OnHttpCallBack` 回调 Java 调用方 |
| MVP Model | `BaseModel`，`ArticleList`、`Login`、`MessageDetail`、`TopicList`、`TopicPost`、`MessageList` 各 Model | rxlifecycle 在 Fragment DETACH 时取消请求，共 26 处 |
| 界面基础设施 | `BaseRxFragment`、`RxLifecycleProvider`、`RxBus`、`RxUtils`、两个 `BaseSubscriber` | `RxBus` 有 3 个 Fragment 通过 `registerRxBus()` 订阅，另有登录对话框直接订阅；`RxUtils` 用于点击防抖（4 处）与延迟执行（1 处） |
| 测试 | 7 个测试文件 | 用 `Observable.just` 构造假响应 |

协程库已由 Compose 与 lifecycle 间接引入（1.9.0）。Presenter 继承 `ViewModel`，可直接使用 `viewModelScope`。

## 设计

- **Java 调用方式不变**：挂起函数只能在 Kotlin 中编写。改造的任务与 Model 改为 Kotlin，对外仍以 `OnHttpCallBack` 回调，Presenter 与 Fragment 等 Java 代码基本不动。
- **挂起式接口**：新增 `NgaApi`，与 `RetrofitService` 一一对应，返回值改为挂起函数；与 `RetrofitService` 共用同一 Retrofit 实例（Cookie、User-Agent、GBK 转码等拦截器与转换器不变）。
- **回调桥**：`CallbackTasks.launchWithCallback` 在主线程启动协程，在 IO 调度器执行请求，在主线程回调成功或 `onError(throwable.getMessage())`。
- **取消语义**：协程绑定页面或 ViewModel 的生命周期。取消后不回调成功或失败，与 rxlifecycle `bindUntilEvent` 一致。
- **线程**：原 `subscribeOn(io)`/`observeOn(mainThread)` 改为 `withContext(Dispatchers.IO)` 后在主线程回调。测试中通过 `NgaDispatchers` 与 `Dispatchers.setMain` 替换调度器。
- **测试数据**：网络部分用 OkHttp 拦截器返回合成响应，不访问真实论坛。

## 实施步骤

每一步单独提交，测试全部通过后再进行下一步。

0. 基础：显式依赖 `kotlinx-coroutines-android` 与 `kotlinx-coroutines-test`；新增 `NgaApi`；新增回调桥与调度器，先补单元测试（回调线程、取消不回调、错误信息）。（已完成）
1. 已是 Kotlin 的 4 个文件改用 `viewModelScope`、`lifecycleScope` 与 Flow，同步更新 `HomeInboxViewModelTest` 与版面收藏测试。（已完成）另含订阅收藏变化的 `ForumFavoritesViewModel`；`changes` 由 `BehaviorSubject` 改为 `StateFlow`。`NavigationDrawerFragment` 仅剩 RxBus 订阅，随第 5 步处理。测试以 `PendingResponse` 替代 `PublishSubject`，主调度器换成 `UnconfinedTestDispatcher` 以保持原先的立即执行语义。
2. 主题加载链路：`ArticlePageLoader` 改为 Kotlin 挂起函数，保持来源顺序、每个来源至多请求一次及错误处理规则（见 `docs/topic-opening-fix.md`），连同 `ArticleListModel`；运行解码基准测试与设备上的 `IncompleteTopicTest`、`WebViewRoutingTest`、`HotRepliesTest`。（已完成）`ArticleListModel` 改为 Kotlin，`BaseModel` 暂时增加 `LifecycleOwner`，请求随页面销毁取消（第 4 步统一替换 rxlifecycle）。`ArticlePageLoaderTest` 改为 Kotlin，16 项用例逐条保留，并迁入 `ArticleHtmlResponseTest` 中 3 项加载用例。顺带修正 `FileUtils.write` 写入 null 时的行为，与 commons-io 一致写入空文件。设备上另运行了覆盖加载错误展示的 `ReaderHostTest`。
3. 后台任务：`BaseRxTask` 与 8 个任务、`SaveImageTask` 改为 Kotlin，保留“正在下载时不重复下载”“多张图片依次保存”等行为，每个任务补充合成响应测试。（已完成）`BaseRxTask` 无调用方，直接删除。个人资料解析原样拆入 Java 类 `ProfileParser`，只把请求部分改为 Kotlin。解析放在后台执行，解析失败与 RxJava 时一样走失败回调；原先只记录日志的任务（收藏、清空通知、点赞的请求失败）仍只记录日志。`TaskCoroutinesTest` 用 `FakeNgaApi` 覆盖举报、签名、子版块、通知、点赞。

   修正一处原有问题：`BaseSubscriber` 只在 `onSubscribe(Subscription)` 中记录订阅，而 `Observable` 调用的是 `onSubscribe(Disposable)`，因此个人资料的 `cancel()`、签名与保存图片的“进行中不重复提交”从未生效。改用 `Job` 后这些行为按原意生效。个人资料的失败提示改为取自本次异常，不再沿用上次失败遗留的文字。
4. MVP Model：`BaseModel` 改为持有协程作用域，替代 rxlifecycle；其余 5 个 Model 逐个改为 Kotlin。（已完成）`BaseModel` 只保留 `LifecycleOwner`，请求通过 `scopeOf` 随页面销毁取消。登录的验证码与登录请求沿用不带公共拦截器的独立实例（`RetrofitHelper.getAuthCodeApi`、`getDefaultApi`）；发帖相关请求沿用连接超时 5 分钟的实例。这些 Model 构造时读取论坛域名、转换时依赖 Android 环境，无法在 JVM 中测试，由设备上的 `ActivityHostTest`、`ReaderHostTest` 覆盖，其余操作需人工检查。
5. 界面基础设施：`RxBus` 改用 `SharedFlow`；点击防抖与延迟执行改为普通实现；删除 `RxLifecycleProvider`、两个 `BaseSubscriber` 及 `BaseRxFragment` 中的 Rx 部分。（已完成）`UiEventBus` 以 `SharedFlow` 投递 `RxEvent`，不保留历史事件，订阅随页面销毁结束；`BaseRxFragment` 保留 `registerRxBus()`/`accept()`，子类不变。登录对话框原先的订阅从未释放，现随对话框销毁结束。`ThrottledClickListener` 保持首次点击立即响应、1 秒内忽略后续点击；延迟隐藏加载视图改用主线程 `Handler`。`lib_common` 中无调用方的 `BaseSubscriber`、`DefaultSubsriber` 一并删除。
6. 收尾：删除 `RetrofitService` 与 `adapter-rxjava2`，删除 RxJava2、rxandroid、rxlifecycle、rxbinding 依赖；迁移剩余测试，确认代码中不再出现 `io.reactivex`。

## 风险

登录、发帖、短消息、通知、订阅版块、点赞、举报、保存图片等操作大多没有自动化测试，测试也不能访问真实论坛。第 3、4 步依靠合成响应测试；第 4 步完成后需用测试账号人工检查上述操作。
