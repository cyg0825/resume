# 05 文件上传与 PDF 简历导入

图片上传（头像、荣誉证书、作品封面）和一条把 PDF 简历变成新简历版本的导入链路。两者共用 Spring 的 multipart 配置，但落库路径完全不同。

## 上传落盘

`service/FileStorageService` 的目录拼接是**按年月**，不是按天：

```java
// service/FileStorageService.java:67,79
String datePart = DateTimeFormatter.ofPattern("yyyy/MM").format(LocalDate.now());
Path targetDir = Paths.get(uploadDir, datePart);
```

文件名是 `UUID.randomUUID().toString().replace("-", "")` 加原扩展名（`:68`），图片被压缩过时扩展名强制改成 `.jpg`（`:77-78`）。返回给前端的相对 URL 由配置项 `app.upload.url-prefix`（值 `/uploads`）拼出来：`urlPrefix + "/" + datePart + "/" + fileName`（`:115`、`:34-35`），最终形如 `/uploads/2026/09/3f1c….jpg`。这个前缀同时被 `config/WebMvcConfig.java:25` 注册成静态资源映射，`<img>` 直接引用即可，不需要鉴权。

`UploadController.java:18` 的类注释给的示例就是 `/uploads/2026/09/xxx.png` 这种两层目录形态，和实现一致。

后缀白名单七项，`Set.of` 定义在 `FileStorageService.java:28-29`：`.jpg .jpeg .png .gif .webp .svg .bmp`。这里没有 `.pdf`——PDF 导入走的是另一个接口，不经过这个白名单。

大小限制不在 Java 代码里，`FileStorageService` 和 `UploadController` 都没有做体积判断，唯一约束是 Spring 的 `max-file-size: 10MB` / `max-request-size: 12MB`（`application.yml:28-29`）。超限抛的 `MaxUploadSizeExceededException` 有独立分支，返回 `code=413` 与一句可读提示，不会落到兜底 handler 把原始异常 message 透出（`common/GlobalExceptionHandler.java:43-47`）。

目录不可写时抛 500 并在 message 里带上 chown/chmod 的修法提示（`FileStorageService.java:85-93,105-113`）。这类提示在生产是噪音，但个人项目自运维阶段确实省事。

## 图片压缩

`util/ImageCompressUtil` 是"压到体积目标以内"的实现，参数在 `:26-34`：

| 参数 | 值 | 位置 |
|---|---|---|
| 目标体积 `MAX_BYTES` | 150 KB | `:26` |
| 最长边上限 `MAX_DIMENSION` | 1600 px | `:28` |
| 尺寸阶梯 | 1600 / 1366 / 1152 / 960 | `:30` |
| JPEG 质量阶梯 | 0.82 / 0.72 / 0.62 / 0.52 / 0.44 | `:32` |
| 参与压缩的格式 | `.jpg .jpeg .png .bmp` | `:34` |

原图已经同时满足体积与尺寸要求时返回 null，调用方按"原样保存"处理（`:64-67`）。`webp`、`gif`、`svg` 不在压缩集合里，原样落盘（`FileStorageService.java:77,99`）。

透明通道的处理值得记一笔：JPEG 不支持 alpha，`flattenWhite`（`:122-137`）把带 alpha 的图铺白底转成 `TYPE_INT_RGB` 再编码。所以一张透明背景的 PNG 徽标上传后会变成白底方图，背景丢失不可逆。

解码失败或 `ImageIO` 读不出图，同样返回 null 原样保存，不阻断上传（`:55-62`）。四轮尺寸乘五档质量跑完仍然超标时，返回所有尝试里体积最小的那个结果 `best`（`:85-87,97-98`）——宁可交一张超标图，也不要在上传链路上抛错。调用侧的注释与常量口径一致，都写 150KB（`FileStorageService.java:70`）。

## 图片指纹：一个纯运维问题

同名覆盖图片后，访客浏览器仍命中旧缓存，看不到更新。项目为此加了一整套资源指纹：

```java
// service/AssetVersionService.java 摘要
// :50,:55  只处理以 urlPrefix + "/" 开头的地址，非上传资源或文件不存在则原样返回
// :68       指纹取文件 lastModified，外加 ConcurrentHashMap 做 3000ms TTL 缓存，省磁盘 IO
// :86,:98   stripVersion：入库前剔除 ?v= 参数，其他 query 参数保留
// :74       refresh()：清空缓存并返回刷新时间戳
```

注入点是 JSON 序列化阶段：自定义注解 `common/AssetUrl`（`common/AssetUrl.java:18`，本质是 `@JacksonAnnotationsInside` + `@JsonSerialize`）标在实体的三个图片字段上——`entity/Profile.java:37`（avatar）、`entity/Portfolio.java:32`（cover）、`entity/Honor.java:36`（image）。业务代码写库和读取都不用手工拼参数。

序列化器不是 Spring Bean（由 Jackson 实例化），拿不到注入，只能通过静态上下文取：`config/SpringContextHolder.java:8,17,22` 实现 `ApplicationContextAware` 把 context 存进 static 字段，`config/AssetUrlSerializer.java:23-24` 从 holder 取 service，取不到就原样输出不报错。这是"序列化器需要 Bean"时的标准绕法，代价是多一个全局 static。

入库前一定要 `stripVersion`（`ProfileService.java:30`、`HonorService.java:53`、`PortfolioService.java:63`），否则带 `?v=` 的地址存进库，下次序列化又叠一层，脏数据会一直长。

直接改文件系统覆盖图片的场景（用面板或 scp 传同名文件）不需要重启，后台「主题与站点」页有个「一键刷新缓存」按钮，调 `POST /api/admin/cache/refresh`（`controller/CacheController.java:19,28`，返回 `refreshedAt` 与提示文案，`:31-34`）。

## PDF 简历导入

链路是：上传 PDF → 抽文本 → 大模型结构化 → 落成一个新的简历版本 → 管理员校对后手动设默认。入口 `POST /api/admin/import/pdf`，参数名 `file`（`controller/ImportController.java:20,29-31`）。控制器校验只有两条：空文件 400，文件名小写后不以 `.pdf` 结尾 400（`:32-38`）；读字节失败 500（`:41-43`）。体积仍由 multipart 的 10MB 管。

```java
// service/ResumeImportService.java:221-224  文本抽取
try (PDDocument document = Loader.loadPDF(bytes)) {
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    return stripper.getText(document);
}
```

PDFBox 3.x 用 `Loader.loadPDF(byte[])`，不再传 File，因此可以直接吃内存里的字节。`setSortByPosition(true)` 是按页面坐标排序输出，否则文本顺序跟 PDF 内部对象顺序走，双栏简历抽出来会是左右交错的两串字，大模型再也分不出哪段是技能哪段是经历。抽出来是空文本时返回 400 并提示"请先 OCR"（`:86-88`），扫描版简历在这一步就会被挡掉，没有 OCR 分支。

结构化调用（`callLlm`，`:233-298`）复用对话地址 `app.ai.api-url` 与 `app.ai.api-key`，但模型单独取 `app.ai.import-model`（`:53-60`）：抽取任务吃结构能力，不需要对话模型的响应速度。请求体 `temperature = 0.1`、`max_tokens = 8192`（`:261-269`），单请求超时 **120 秒**（`:274`），比问答的 30 秒宽得多，因为一份多页简历的抽取经常超过半分钟。`api-key` 未配置直接 500 提示尚未配置大模型（`:234-236`），导入功能对模型是硬依赖，没有降级路径。

大模型返回的 JSON 需要两步修复（`parseStructuredJson`，`:301-328`）：

1. 以 ` ``` ` 开头时，取第一个换行之后到最后一个 ` ``` ` 之间的内容（`:303-309`）。
2. 再截取**首个 `{` 到末个 `}`**（`:310-314`）。

这两步就够覆盖实际遇到的两类脏输出：markdown 围栏包裹、JSON 前后多一句说明文字。`profile` 节点不是对象时抛 500「结构化结果缺少 profile 字段」（`:317-319`），解析异常时把响应体前 300 字符写进日志再抛 500（`:324-326`）。

这里的原则是 fail fast，不做字段补齐：`importPdf` 上有 `@Transactional`（`:80-81`），任何一步失败整个版本回滚，库里不会留下半份简历。与其猜一个默认值把空缺填满，不如让管理员重传一次——补出来的默认值会被当成真实经历读进去，那才是更难发现的问题。

## 落库

新版本名是 `"PDF导入-" + MM-dd HH:mm`，description 记原文件名，`isDefault` 显式设 null（`:94-100`）。之后按块写：

| 内容 | 位置 | 备注 |
|---|---|---|
| profile | `:106-119` | 单条 |
| educations | `:123-136` | 循环单条 insert |
| 工作经历 | `:141-154` | `type = 1` |
| 项目经历 | `:159-175` | `type = 2`，company 取项目的 name、position 取 role |
| skills | `:180-191` | level 缺省 75，并按 0-100 截断 |
| honors | `:195-211` | 按抽取到的 profile.name 归档；姓名为空整段跳过 |

全部是逐条 `insert`，没有批量方法。日期解析用正则 `(\d{4})\D{0,2}(\d{1,2})?`，并把"至今/现在/目前/present/current/now"归一为 null（`:39,389-406`），所以"2020 年 7 月至今"能落成 `start_date=2020-07-01, end_date=NULL`。

返回值是一个 record（`:77-78`）：

```java
public record ImportResult(Long versionId, String versionName, Map<String, Integer> counts) {}
```

`counts` 的 key 是 education / workExperience / projectExperience / skill / honor 五个栏目名，用于前端展示"抽出了多少条"。

## 前端导入面板

入口不在侧边栏菜单里，而是 `views/admin/VersionManage.vue:7-12` 顶部的「导入 PDF 简历」按钮，弹出面板见 `:79-143`。约束：`accept=".pdf"`、`limit 1`、体积上限 `MAX_PDF_SIZE = 10 * 1024 * 1024`（`:88-103`、`:187`），和后端 multipart 限制对齐。超出 limit 时先 `clearFiles` 再 `handleStart`，实现"选新文件即替换"（`:227-235`）。

进度条在上传阶段封顶 95%，留 5% 给大模型解析（`:245-251`）——上传本身是本地到服务器的秒级动作，真正久等的是模型，不封顶的话用户看到的是"卡在 100% 不动"。请求单独设 `timeout: 180000` 并开启 `onUploadProgress`（`api/index.js:109-117`），这个数值必须大于后端 `callLlm` 的 120 秒，否则前端先超时、后端还在跑。

导入成功后展示各栏目条数（`VersionManage.vue:112-131`、`:189-195`）、刷新版本列表，并可一键预览新版本（`:254`、`:260-264`）。此时访客看到的仍是原默认版本，新内容要等管理员点「设为默认」才对外的生效，这个顺序是设计意图而不是遗漏。

## 可复用点

- **上传文件不建表记录**：目录按年月两层（`FileStorageService.java:67,79`）、文件名是去掉连字符的 UUID（`:68`），访问地址由配置前缀拼出（`:115`）并注册成静态映射（`config/WebMvcConfig.java:25`）。库里只存这个相对路径，删除、迁移、备份都是文件系统操作，不需要一张 uploads 表；随机文件名顺带解决了"公开读但不可枚举"。
- **压缩按阶梯试，最差也要交付**：最长边四档 × JPEG 质量五档逐级降（`util/ImageCompressUtil.java:28-34`），原图已达标直接返回 null 让调用方原样保存（`:64-67`），解码失败同样不阻断上传（`:55-62`），全部超标则交回尝试过的最小结果（`:85-87,97-98`）。上传链路上任何"优化型"处理都该遵守这条：优化失败不能变成用户可见的失败。顺带一个格式兼容点——转 JPEG 前要把 alpha 铺白底（`:122-137`），否则透明 PNG 会转出一张黑底图。
- **URL 加工放在序列化阶段，不在业务代码里**：自定义注解 `common/AssetUrl` 标在实体的图片字段上，序列化时统一拼指纹参数（`config/AssetUrlSerializer.java:23-24`），写库和读取两侧都没有手工拼参数的分支。配套约束是入库前必须剥掉参数（`ProfileService.java:30`、`HonorService.java:53`、`PortfolioService.java:63`），否则脏数据逐次叠加。序列化器由 Jackson 而非 Spring 实例化，取 Bean 只能走静态上下文（`config/SpringContextHolder.java:8,17,22`）——这套绕法适用于任何"字段级输出加工"，代价是多一个全局 static。
- **抽版式文档的文本要按坐标排序**：`PDFTextStripper` 的 `setSortByPosition(true)`（`service/ResumeImportService.java:221-224`）是双栏简历能不能被后续模型读懂的分界线，默认顺序跟 PDF 内部对象顺序走，两栏内容会交错成一串。同理，抽出来是空文本就当场拒绝并提示走 OCR（`:86-88`），不要把手里的空白文本交给模型。
- **给模型的结构化输出一条超时链，缺项不猜**：前端 180 秒大于抽取请求的 120 秒（`api/index.js:114` 与 `ResumeImportService.java:274`），解析只做两步清洗（剥 markdown 围栏、截首 `{` 到末 `}`，`:303-314`），关键字段缺失直接抛错并由 `@Transactional` 回滚整个版本（`:80-81,317-319`）。补默认值看起来更友好，但猜出来的经历会被当成真实内容读进去，比导入失败更难发现。
