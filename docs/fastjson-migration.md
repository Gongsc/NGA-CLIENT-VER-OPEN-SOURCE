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

`nga_phone_base_3.0/src/test/java/sp/phone/json/` 下的三个测试类记录了 fastjson 的实际行为，数据均为合成数据：

- `FastjsonParsingCharacterizationTest`：宽松输入、拒绝的输入、空文档、数字类型、键顺序、序列化转义。
- `FastjsonCoercionCharacterizationTest`：11 个取值方法对 29 种原始值的转换结果表。
- `FastjsonBindingCharacterizationTest`：实体绑定规则，以及本地持久化格式的写出与读取。

替换时这些测试先改为面向新实现运行，全部通过后才迁移业务代码。

### 需要保留的行为

**解析**

- 接受：前导零数字（`000123` → `123`）、无引号键、单引号字符串、末尾注释、多余逗号、字符串内原始换行与制表符、`\x41` 转义、重复键（后者覆盖）、值为 `undefined` 的键（丢弃）。
- 拒绝：文档后的多余文本（注释除外）、`+1`、`.5`、`NaN`、`TRUE`；`parseObject` 遇到数组、`parseArray` 遇到对象都会抛出异常。
- `""`、空白与 `null` 文档在 `parseObject`/`parseArray`/`parse` 中返回 `null`。
- 数字：整数为 `Integer`，超出范围为 `Long`、`BigInteger`；小数为 `BigDecimal`，保留原文（`1.50` 取字符串仍是 `1.50`）；指数形式为 `Double`，`1e3` 序列化为 `1000`。

**类型转换（摘要，完整内容见转换表）**

- `getIntValue`/`getLongValue`/`getBooleanValue` 遇到缺失、`null`、`""` 时返回默认值；`getInteger`/`getLong`/`getBoolean` 返回 `null`。
- `" "`、`"1.5"`、`"abc"` 转整数时抛出异常；小数 `1.9` 转整数截断为 `1`；`3000000000` 转 `int` 时溢出为负数。
- 布尔值：`"1"`、`"true"`、`true` 为真；整数 `2`、`12` 为假；`1.9` 和 `"Y"`、`"T"` 抛出异常。
- 对对象或数组调用 `getString` 返回 JSON 文本；值为 JSON 文本的字符串，`getJSONObject`/`getJSONArray` 会解析；`""` 返回 `null`；数字调用 `getJSONObject` 抛出异常。

**序列化**

- `null` 值的键被省略；中文、`/`、`<`、`>`、`&` 不转义；控制字符与 U+2028 写作 `\uXXXX`。
- 实体按属性名字母顺序输出，同时包含公有字段。`User` 因此同时写出 `userId` 和 `mUserId` 等两组键。

**键顺序**

- 默认 `JSONObject` 基于 `HashMap`，不保证顺序；`Feature.OrderedField` 与 `new JSONObject(true)` 保持插入顺序。新实现统一保持插入顺序，这是原行为的超集，不视为差异。

**实体绑定的两条规则**

- `parseObject(text, Class)`/`parseArray(text, Class)` 宽松匹配键名，忽略大小写与下划线（`user_id`、`userid` 都能匹配 `userId`），并按上面的规则转换类型。
- `JSONObject.toJavaObject` 只精确匹配属性名。`ThreadPageInfo` 读取 `__T` 时 `authorid`、`postdate`、`topic_misc` 等不会绑定；`ThreadRowInfo` 的 `from_client`、`isanonymous`、`comment` 也不会绑定，由转换代码另行读取。
- 绑定基于 setter：只有 getter 的属性（如 `mirrorBoard`）读入时忽略；公有字段（如 `hotReplies`）也会绑定。
- `toJavaObject` 中 `""` 或非数字字符串转为 `int` 属性时抛出异常；对象字段遇到 `""` 得到 `null`；`Map` 字段遇到数组时抛出异常。当前一行数据异常会导致整页转换失败，替换时先保持一致，是否放宽另行决定。

## 替换方案

采用 Gson 作为解析器，在 `lib_common` 中提供兼容层：

- `NgaJson`：`parseObject`、`parseArray`、`parse`、`toJsonString`、`parseObject(text, Class)`、`parseArray(text, Class)`、`toJavaObject`。
- `NgaJsonObject`、`NgaJsonArray`：基于 `LinkedHashMap`/`ArrayList`，方法名与返回值和 fastjson 对齐，类型转换规则集中实现并由转换表约束。
- 解析使用 Gson `JsonReader` 的宽松模式，自行构建树，以保留数字原文和键顺序。Gson 宽松模式与 fastjson 的差异（前导零、`\x` 转义、`undefined`、末尾多余文本、连续逗号）在兼容层补齐，以特征测试为准。
- 实体绑定与序列化按 fastjson 的 JavaBean 规则（setter/getter 与公有字段）自行实现，分宽松与精确两种匹配，不使用 Gson 基于字段的反射绑定，避免 `mTid` 这类字段名改变已保存的数据格式。

未选方案：kotlinx.serialization 需要编译插件，对 Java 调用方不友好；Moshi 的树形结构将数字统一转为 `Double`，大编号会丢失精度；`org.json` 在 JVM 单元测试中不可用，类型转换接口也较弱。

## 实施步骤

每一步单独提交，全部单元测试通过后再进行下一步。

1. 编写特征测试，记录 fastjson 当前行为（已完成）。
2. 加入 Gson 与兼容层，让特征测试同时针对兼容层运行并全部通过。
3. 迁移本地持久化（D）与实体绑定（C），补充“旧版本写入的数据可被读取”的测试。
4. 迁移其他接口的树形取值（B）。
5. 迁移主题加载链路（A），运行全部单元测试，并在设备上运行 `IncompleteTopicTest`、`WebViewRoutingTest`。
6. 删除 fastjson 依赖，确认代码中不再出现 `com.alibaba.fastjson`；删除只针对 fastjson 的测试分支。

第 5 步完成后，建议使用测试账号实际浏览主题，覆盖普通楼层、热点回复、附件、投票与匿名楼层。
