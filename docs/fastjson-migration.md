# fastjson 替换方案

## 背景

项目使用 `com.alibaba:fastjson:1.1.71.android`。该版本已停止维护，1.x 系列有反序列化安全问题的历史。依赖由 `lib_common` 以 `api` 方式暴露，app 模块 37 个文件直接引用。

NGA 接口返回的 JSON 并不规范，业务代码隐式依赖 fastjson 的宽松解析和类型转换。替换的核心风险不在接口调用，而在这些未写明的行为，以及本地已保存数据的格式兼容。

## 用法概况

只用到 `JSON`、`JSONObject`、`JSONArray` 与 `Feature.OrderedField`，没有注解、`TypeReference` 或自定义序列化。按键取值占绝大多数：`getString` 207 处、`getJSONObject` 64 处、`getIntValue` 42 处、`getBoolean` 37 处、`containsKey` 24 处。

| 类别 | 文件 | 风险 |
|---|---|---|
| A 主题加载链路 | `ArticleResponse`、`ArticleHtmlResponse`、`ArticleScriptData`、`ArticlePageLoader`、`ArticleConvertFactory`、`ArticleUtil`、`JsonThreadLoadTask`、`ProxyBridge` | 高 |
| B 其他接口树形取值 | `ForumNotificationFactory`、`MessageUtil`、`MessageConvertFactory`、`JsonProfileLoadTask`、`ErrorConvertFactory`、`HomeInboxViewModel`、`PostCommentTask`、`MessagePostTask`、`LikeTask`、`SearchBoardTask`、`AvatarPostActivity`、`ForumFavorites.kt` | 中 |
| C 接口响应绑定实体 | `TopicListBean`、`TopicPostBean`、`CategoryBean`、`ForumsListModel`、`ReportTask.ResultBean`、`AvatarFileUploadTask.NonameUploadResponse` | 中 |
| D 本地持久化 | 屏蔽用户 `BLACK_LIST` 与旧 `USER_LIST`、屏蔽词 `FILTER_KEYWORDS_LIST`、浏览历史 `KEY_TOPIC_HISTORY`、搜索历史、版面收藏缓存、`PreferenceUtils`、`topicInfo` Intent 参数 | 高 |

主题链路中，JSON 来源经 `ArticleResponse.decode` 预处理（去 BOM 与 `/*$js$*/`、截断 `/*error fill content`、为带前导零的 `content`/`subject`/`author` 加引号）后解析；网页与 XML 来源用 `new JSONObject(true)` 构造相同的 `__T`/`__R`/`__U` 结构。`ArticlePageLoader` 校验后执行 `toJSONString()`，由 `ArticleConvertFactory` 重新解析，缓存也保存该字符串。截断检测是独立的括号扫描，不依赖 fastjson。

## 特征测试

`nga_phone_base_3.0/src/test/java/sp/phone/json/` 下的三个测试类记录了 fastjson 的实际行为，数据均为合成数据。每个用例通过 `JsonBackend` 同时针对 fastjson 与 `NgaJson` 运行：

- `FastjsonParsingCharacterizationTest`：宽松输入、拒绝的输入、空文档、数字类型、键顺序、序列化转义。
- `FastjsonCoercionCharacterizationTest`：11 个取值方法对 29 种原始值的转换结果表。
- `FastjsonBindingCharacterizationTest`：实体绑定规则，以及本地持久化格式的写出与读取。

删除 fastjson 时移除 `JsonBackend.FASTJSON`，测试继续约束 `NgaJson`。

### 需要保留的行为

**解析**

- 接受：前导零数字（`000123` → `123`）、单引号字符串、块注释、多余与连续的逗号（`[1,,2]` → `[1,2]`）、字符串内原始换行与制表符、`\x41` 与 `\'` 转义、重复键（后者覆盖）、值为 `undefined` 的键（对象中丢弃，数组中为 `null`）。
- 无引号键：纯数字，或以字母（含中文）、下划线开头并由字母、数字、下划线组成；`$`、`-`、`.` 与数字开头的混合键名被拒绝。
- 拒绝：文档后的多余文本（块注释除外）、行注释、`+1`、`.5`、`NaN`、`TRUE`、`12abc`、未知转义；`parseObject` 遇到数组、`parseArray` 遇到对象都会抛出异常。
- `""`、空白与 `null` 文档在 `parseObject`/`parseArray`/`parse` 中返回 `null`。
- 数字：9 位以内的整数为 `Integer`，10 位起为 `Long`（即使数值在 `int` 范围内），超出 `long` 为 `BigInteger`；小数为 `BigDecimal`，保留原文（`1.50` 取字符串仍是 `1.50`）；指数形式为 `Double`，`1e3` 序列化为 `1000`。

**类型转换（摘要，完整内容见转换表）**

- `getIntValue`/`getLongValue`/`getBooleanValue` 遇到缺失、`null`、`""` 时返回默认值；`getInteger`/`getLong`/`getBoolean` 返回 `null`。
- `" "`、`"1.5"`、`"abc"` 转整数时抛出异常；小数 `1.9` 转整数截断为 `1`；`3000000000` 转 `int` 时溢出为负数。
- 布尔值：`"1"`、`"true"`、`true` 为真；整数 `2`、`12` 为假；`1.9` 和 `"Y"`、`"T"` 抛出异常。
- 对对象或数组调用 `getString` 返回 JSON 文本，浮点数返回 `Double.toString` 结果（`1e3` → `1000.0`）；值为 JSON 文本的字符串，`getJSONObject`/`getJSONArray` 会解析；`""` 返回 `null`；数字调用 `getJSONObject` 抛出异常。
- 数组按下标取值时不解析字符串元素，`getJSONObject` 遇到字符串直接失败。

**序列化**

- `null` 值的键被省略，数组中的 `null` 保留；中文、`/`、`<`、`>`、`&`、U+2029 不转义；其余控制字符、DEL 与 U+2028 写作大写的 `\uXXXX`。
- 浮点数以 `toString` 输出并去掉末尾 `.0`（`100.0` → `100`，`1e20` → `1.0E20`），`NaN` 写作 `null`。
- 实体按属性名字母顺序输出，同时包含公有字段。`User` 因此同时写出 `userId` 和 `mUserId` 等两组键。

**键顺序**

- 默认 `JSONObject` 基于 `HashMap`，不保证顺序；`Feature.OrderedField` 与 `new JSONObject(true)` 保持插入顺序。新实现统一保持插入顺序，这是原行为的超集，不视为差异。

**实体绑定的两条规则**

- `parseObject(text, Class)`/`parseArray(text, Class)` 宽松匹配键名，忽略大小写与下划线（`user_id`、`userid` 都能匹配 `userId`），并按上面的规则转换类型。
- `JSONObject.toJavaObject` 只精确匹配属性名。`ThreadPageInfo` 读取 `__T` 时 `authorid`、`postdate`、`topic_misc` 等不会绑定；`ThreadRowInfo` 的 `from_client`、`isanonymous`、`comment` 也不会绑定，由转换代码另行读取。
- 绑定基于 setter：只有 getter 的属性（如 `mirrorBoard`）读入时忽略；公有字段（如 `hotReplies`）也会绑定。
- 两条路径的类型转换不同：

  | 写入 `int` 属性的值 | 文本绑定 | `toJavaObject` |
  |---|---|---|
  | `""`、`null` | 保留默认值 | 失败 |
  | `"12"` | 12 | 12 |
  | `"abc"`、`true` | 失败 | 失败 |
  | `1.9` | 失败 | 截断为 1 |
  | `3000000000` | 失败 | 溢出为负数 |

  布尔属性同理：文本绑定遇到 `""` 保留默认值，`toJavaObject` 失败；两者对 `2` 都得到 `false`，对 `"Y"` 都失败。
- 字符串属性遇到数字、对象或数组时取其文本；对象属性遇到 `""` 得到 `null`，遇到数组失败；`Map` 属性遇到数组失败，其中值为 `""` 的实体条目保留键、值为 `null`。
- 当前一行数据异常会导致整页转换失败，替换时先保持一致，是否放宽另行决定。

### 有意不保留的差异

以下 fastjson 行为没有业务依赖，`NgaJson` 不予复现：

- 默认对象无序：`NgaJson` 统一保持插入顺序，`parseObject(text, Map.class)` 也返回有序的 `NgaJsonObject`。
- 文本绑定时，集合属性遇到对象，fastjson 会把对象包装成单元素列表或生成空实体；`NgaJson` 直接失败。
- `toJavaObject` 遇到只有读取方法的集合属性（如 `imageUrls`）时 fastjson 失败；`NgaJson` 忽略该键。
- 行注释：fastjson 以 `StringIndexOutOfBoundsException` 崩溃，`NgaJson` 抛出 `NgaJsonException`，两者都拒绝。
- 异常类型：fastjson 抛出 `JSONException`、`NumberFormatException`、`ClassCastException` 等；`NgaJson` 主要抛出 `NgaJsonException`，数字字符串解析失败时仍为 `NumberFormatException`。迁移时需逐处确认调用方捕获的异常类型。

## 替换方案

在 `lib_common` 的 `gov.anzong.androidnga.base.json` 包中自行实现兼容层，不引入第三方 JSON 库：

- `NgaJson`：`parse`、`parseObject`、`parseArray`、`parseObject(text, Class)`、`parseArray(text, Class)`、`toJavaObject`、`toJSONString`。
- `NgaJsonObject`、`NgaJsonArray`：继承 `LinkedHashMap`/`ArrayList`，取值方法的名称和转换规则沿用 fastjson，迁移时主要替换导入与类型名。
- `JsonParser`：递归下降解析器，接受范围与 fastjson 1.1 对齐，保留数字原文与键顺序。
- `JsonCasts`：取值方法与 `toJavaObject` 共用的类型转换。
- `JsonBeans`：按 setter/getter 与公有字段绑定和导出实体，分文本与精确两种模式。
- `JsonWriter`：序列化，输出格式与 fastjson 一致。

最初计划使用 Gson 解析。验证发现 Gson 2.11 宽松模式在 19 种测试输入中有 10 种与 fastjson 不同，其中多数无法在解析后补救：`[1,2,]` 得到 `[1,2,null]`；`{"a":1,}`、`,,` 与 `\x41` 直接失败；`000123`、`+1`、`NaN` 被读成字符串，与带引号的字符串无法区分；`TRUE` 被接受。实体绑定也须按 JavaBean 规则自行实现，Gson 已无用处，因此改为自行实现解析器。

未选方案：kotlinx.serialization 需要编译插件，对 Java 调用方不友好；Moshi 的树形结构将数字统一转为 `Double`，大编号会丢失精度；`org.json` 在 JVM 单元测试中不可用，类型转换接口也较弱。

## 实施步骤

每一步单独提交，全部单元测试通过后再进行下一步。

1. 编写特征测试，记录 fastjson 当前行为（已完成）。
2. 加入兼容层，让特征测试同时针对兼容层运行并全部通过（已完成）。
3. 迁移本地持久化（D）与实体绑定（C），补充“旧版本写入的数据可被读取”的测试（已完成）。`BeanBindingEquivalenceTest` 用 fastjson 与 `NgaJson` 分别绑定接口实体、内置版面列表 `assets/json/category*.json` 与本地记录，结果一致。`TopicPostModel` 与 `NavigationDrawerFragment` 中少量树形取值随文件一并迁移；`ForumFavorites.kt` 只迁移离线缓存，接口解析依赖 `ArticleResponse.businessError`，随第 5 步迁移。
4. 迁移其他接口的树形取值（B）。
5. 迁移主题加载链路（A），运行全部单元测试，并在设备上运行 `IncompleteTopicTest`、`WebViewRoutingTest`。
6. 删除 fastjson 依赖，确认代码中不再出现 `com.alibaba.fastjson`；删除只针对 fastjson 的测试分支。

第 5 步完成后，建议使用测试账号实际浏览主题，覆盖普通楼层、热点回复、附件、投票与匿名楼层。
