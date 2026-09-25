# 个人简历网站

基于 **Vue 3 + Spring Boot 3 + H2** 的全栈个人简历网站，内嵌 H2 数据库、JWT 鉴权管理后台、多主题切换、访客统计、AI 智能问答、简历多版本。

## 功能

**基础功能**
- 前台单页简历（头像/教育/工作项目/技能/作品集），响应式双栏排版
- PDF 一键导出（html2canvas + jsPDF，A4 自动分页）
- 后台 JWT 登录，简历各模块增删改查，图片上传本地存储
- 访客留言查看与删除

**扩展功能**
| 功能 | 说明 |
|------|------|
| 多主题切换 | 内置商务蓝 / 暗夜科技 / 清新绿 3 套主题，CSS 变量一键换肤 |
| 访客访问统计 | 记录真实 IP / UA / 来源 / 路径 / 时间，后台 ECharts 仪表盘可视化 |
| AI 智能问答 | 简历内容作为知识库，对接 OpenAI 兼容大模型；未配置时自动降级为本地关键词问答 |
| 简历版本管理 | 创建多套独立简历，支持克隆、重命名、删除、设默认，前台 `?versionId=x` 预览 |
| PDF 简历导入 | 上传 PDF → 后端抽取文本 → 大模型结构化 → 生成新版本校对后一键上线 |

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router 4 + ECharts + html2canvas + jsPDF |
| 后端 | Spring Boot 3.2 + Spring Security + JJWT 0.12 + MyBatis-Plus 3.5 + H2 2.2 + Lombok |
| 部署 | Docker Compose（后端 JRE + 前端 Nginx，零外部数据库依赖） |

## 目录结构

```
Resume
├── backend/                 Spring Boot 后端（Java 17 + Maven）
│   ├── src/main/java/       源码（controller/service/entity/mapper/config/security/init）
│   ├── src/main/resources/
│   │   ├── schema.sql       H2 建表脚本（首次启动自动执行）
│   │   ├── data.sql         初始简历示例数据
│   │   └── application.yml
│   └── Dockerfile
├── frontend/                Vue 3 前端
│   ├── src/                 源码（api/components/views/router/store）
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

## 快速开始

### 本地开发

```bash
# 后端（H2 内嵌数据库自动就绪，无需额外准备）
cd backend && mvn spring-boot:run      # http://localhost:8080

# 前端
cd frontend && npm install && npm run dev   # http://localhost:5173
```

默认管理员：**admin / admin123**（登录后请尽快修改）

> 清空数据重来：停后端 → 删除 `backend/data/` → 重启即可重建数据库

### Docker Compose

```bash
docker compose up -d --build
```

### 启用大模型问答

在 `application.yml` 或环境变量中配置（支持任意 Chat Completions 兼容接口）：

```yaml
app:
  ai:
    api-url: https://your-api-endpoint.com/v1/chat/completions
    api-key: your-api-key-here
    model: your-model-name
```

不配置 `api-key` 时系统自动使用内置本地关键词问答，功能依然可用。

## 数据库（H2）

内嵌文件模式，MySQL 兼容语法，零外部依赖：

| 特性 | 说明 |
|------|------|
| 运行模式 | `jdbc:h2:file:./data/resume`，数据持久化在磁盘 |
| 自动初始化 | 首次启动执行 `schema.sql` 建表 + `data.sql` 灌初始数据 |
| 重置 | 停后端 → 删 `data/` → 重启 |
| 备份 | 停后端 → 复制 `data/resume.mv.db` 即可，单文件完整快照 |

## 详细文档

`md/` 目录下按功能拆分了 10 篇技术笔记，覆盖架构、认证、版本管理、AI 问答、访问统计、数据库设计、Docker 部署等。