# 功能四：文件上传与 PDF 简历导入

## 一、文件上传（头像/作品图片）

### 后端 FileStorageService

**文件**：`backend/src/main/java/com/example/resume/service/FileStorageService.java`

#### 存储策略

文件存在本地磁盘，目录结构按日期：

```
{uploadDir}/yyyy/MM/dd/{uuid}.{后缀}
```

- `uploadDir`：配置文件里的 `${app.upload.dir}`，Docker 里映射到 volume
- 日期目录：按上传日期归档，方便清理过期文件
- UUID 文件名：避免重名覆盖，用 `UUID.randomUUID().toString()`

#### 图片压缩

上传图片时自动压缩（`ImageCompressUtil`），规则：
- 宽或高超过 1280px 等比缩到 1280px 以内
- JPEG 质量 0.85
- PNG 超过 300KB 时也压缩（PNG 压成 JPEG，后缀改成 .jpg）

**知识点**：Java 原生 API `javax.imageio.ImageIO` 读图片 → `BufferedImage` → `Graphics2D` 画缩放版本 → 用 `ImageIO.write` 以指定质量写回。质量参数通过 `ImageWriteParam` 的 `setCompressionQuality(0.85f)` 设置。

#### 返回给前端的路径

存到磁盘后返回的是**相对 URL**：`/uploads/2024/06/15/uuid.jpg`。前端直接 `<img :src="baseURL + path">` 就能显示，因为 Nginx 把 `/uploads` 路径反向代理到后端的上传目录。

#### 安全校验

- 文件大小限制：MultipartFile size 限制在配置文件里配置
- 允许的后缀：白名单 `jpg/jpeg/png/webp/gif/pdf`
- MIME type 校验：用 `ContentType` 字段（由浏览器/HTTP 层决定）

---

## 二、PDF 简历导入

### 整体流程

管理员上传一份 PDF 简历，系统自动完成：

```
1. 前端上传 PDF → 后端接口接收 MultipartFile
2. FileStorageService 存文件到 uploads 目录
3. ResumeImportService.extractPdfText() 用 PDFBox 抽 PDF 文本
4. ResumeImportService.structureWithLlm() 调大模型把纯文本结构化成 JSON
5. ResumeImportService.createNewVersion() 创建新简历版本，把 JSON 内容写入各表
6. 返回新版本 id，前端跳转到该版本的编辑页
```

### PDFBox 文本抽取

```java
try (PDDocument document = Loader.loadPDF(file.toFile())) {
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    String text = stripper.getText(document);
}
```

- `Loader.loadPDF()`：兼容 PDFBox 3.x 的静态工厂
- `setSortByPosition(true)`：按页面坐标排序输出文本，而不是 PDF 内部对象顺序。后者可能乱序（比如 PDF 里"姓"和"名"对象在页面上紧挨着但内部顺序相反）
- `try-with-resources`：自动关闭 PDDocument

**知识点**：PDF 本质是页面描述语言（类似 PS），不是带语义的文档。抽出来的文本只是视觉上看到的文字串，没有任何结构信息（哪块是工作经历、哪块是教育、字段名和值的边界都没了）。这就是为什么后面还需要大模型来"结构化"。

### 大模型结构化

**Prompt 设计**：

System Prompt：
```
你是简历结构化助手。给你一份 PDF 里抽取的纯文本简历，需要把它转换成指定的 JSON 格式。
姓名必须从文本中提取，如果提取不到就留空字符串。
只输出 JSON，不要任何解释文字。
```

User Prompt：
```
以下是简历文本：
{extract 出来的纯文本}

请按这个 JSON 格式返回：
{一份完整的 JSON Schema 示例，包含 profile、educations、experiences、skills 字段}
```

**JSON Schema** 里包含了 profile（姓名/职位/标语/自我介绍）、educations（学校/专业/年份/描述）、experiences（公司/职位/年份/描述/技术栈）、skills（分组名/条目列表）。

### 创建新版本

拿到大模型返回的 JSON 后：

```java
1. 先做 JSON 结构校验（必填字段是否存在、类型是否正确）
2. 创建 ResumeVersion 记录（默认不设为 default，管理员确认后再切换）
3. 批量 insert：
   - profileMapper.insert(profile)
   - educationMapper.insertBatch(educations)    // 批量
   - experienceMapper.insertBatch(experiences)
   - skillMapper.insertBatch(skills)
4. 如果 profile.name 不为空，还按姓名去荣誉/作品集里查有没有同名数据
   （同一个人的荣誉/作品集跨版本共享）
5. 返回 ImportResult(versionId, profileName, extractedSections, warnings)
```

### 异常处理的三层兜底

大模型返回格式不对时的处理策略：

```
第一层：JSON 解析失败 → 尝试修复（去掉前后多余文字、trim、替换转义字符）
第二层：JSON 解析成功但缺少必填字段 → 用默认值补
第三层：API 调用失败或超时 → 导入失败，让管理员手动创建版本
```

**为什么不直接让大模型输出更严格的格式**：即使 Prompt 写得很详细，7B 规模的模型也偶尔会输出 `"```json ... ```"` 这种 markdown 包裹格式，或者在 JSON 末尾多一句解释。修复逻辑就是把这些不规范的输出尽量"掰回"标准 JSON。

---

## 三、知识点总结

| 知识点 | 说明 |
|--------|------|
| MultipartFile | Spring 封装的 HTTP 文件上传对象，后端 Controller 方法参数类型 |
| Date-based upload 目录 | 按日期生成子目录，方便按天清理归档 |
| UUID 文件名 | 避免并发上传重名覆盖，UUID 天然唯一 |
| ImageIO 压缩 | Java 原生 API，BufferedImage → Graphics2D 缩放 → ImageIO.write 设质量 |
| PDFBox | 最成熟的开源 PDF 操作库，`PDFTextStripper` 抽文本，`setSortByPosition` 解决顺序问题 |
| PDF 本质 | 页面描述语言，抽的文本没有语义，需要 LLM 二次结构化 |
| Prompt Engineering | System Prompt 定义角色和约束，User Prompt 给上下文和输出格式要求 |
| JSON 结构校验 | 大模型输出可能不规范，后端必须做防御性解析和修复 |
| 批量 insert | MyBatis-Plus 的 `IService.saveBatch()` 或自定义批量 SQL，比逐条 insert 快 |
| ImportResult record | Java record 类型，不可变数据载体，适合 DTO |