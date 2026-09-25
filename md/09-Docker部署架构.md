# 功能九：Docker 部署架构

## 一、为什么用 Docker Compose

- 一键部署：`docker compose up -d` 拉起后端（内嵌 H2） + 前端（Nginx）两个容器
- 环境一致：开发、测试、生产都是同一套镜像，没有"在我机器上能跑"的问题
- 零外部依赖：H2 内嵌在后端进程里，不需要单独起数据库容器，部署更轻
- 方便扩容：以后想多后端实例，改 compose 文件就行

---

## 二、docker-compose.yml 结构

**文件**：`docker-compose.yml`

```yaml
services:
  backend:
    build:
      context: ./backend
    container_name: resume-backend
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      H2_DATA_DIR: /app/data/resume
      H2_USER: ${H2_USER:-sa}
      H2_PASSWORD: ${H2_PASSWORD:-}
      JWT_SECRET: ${JWT_SECRET:-change-me-in-production}
      ADMIN_USERNAME: ${ADMIN_USERNAME:-admin}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-admin123}
      AI_API_URL: ${AI_API_URL:-}
      AI_API_KEY: ${AI_API_KEY:-}
      AI_MODEL: ${AI_MODEL:-}
      KNIFE4J_PRODUCTION: ${KNIFE4J_PRODUCTION:-false}
      UPLOAD_DIR: /app/uploads
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - uploads-data:/app/uploads
      - h2-data:/app/data

  frontend:
    build:
      context: ./frontend
    container_name: resume-frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "${WEB_PORT:-80}:80"

volumes:
  uploads-data:        # 上传文件数据卷，持久化头像/证书图片
  h2-data:             # H2 数据库文件卷，持久化 resume.mv.db
```

### 关键设计

**为什么没有数据库服务**：

H2 是**内嵌数据库**——它不是一个独立进程，而是后端 Java 进程里的一个库。JDBC URL `jdbc:h2:file:/app/data/resume` 直接在指定目录读写 `.mv.db` 文件。所以：
- 不需要启动额外的数据库容器
- 不需要 healthcheck 等待数据库就绪（H2 和 Spring Boot 一起启动）
- 数据持久化用一个 Docker Named Volume 挂到 `/app/data` 目录就行
- 备份 = 复制这个 volume 里的文件

**端口映射**：
- 后端 8080 → 宿主机 8080（方便直接调试 API）
- 前端 80 → 宿主机 80

生产环境只需要暴露前端 80，后端可以不对外映射，只在 Docker 内部网络通信。

**数据卷**：
- `h2-data` 挂到 `/app/data`：存 H2 的 `resume.mv.db`、`resume.trace.db` 文件
- `uploads-data` 挂到 `/app/uploads`：存用户上传的头像、证书图片

H2 自动初始化机制：JDBC URL 带 `INIT=RUNSCRIPT FROM 'classpath:schema.sql'\;RUNSCRIPT FROM 'classpath:data.sql'`，**只在 `.mv.db` 文件不存在时执行一次**。已有数据库文件时完全跳过脚本，不会清空运行时数据。

---

## 三、后端 Dockerfile

**文件**：`backend/Dockerfile`

多阶段构建：

```dockerfile
# 阶段 1：Maven 构建
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B          # 先装依赖，利用缓存
COPY src ./src
RUN mvn package -DskipTests -B          # 打 jar

# 阶段 2：运行
FROM eclipse-temurin:17-jre-alpine       # 只装 JRE，镜像小很多
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

**为什么多阶段**：Maven 构建环境有几百 MB（JDK + Maven + 依赖），运行时只需要 JRE + jar。两个阶段分开后最终镜像从 ~800MB 缩减到 ~170MB。

**知识点**：`dependency:go-offline` 在 COPY src 之前执行。Docker 构建时 COPY pom.xml 变了才会重新装依赖，src 变了只重新 package，利用了层缓存。这是 Dockerfile 的最佳实践。

**JRE Alpine**：`eclipse-temurin:17-jre-alpine` 基于 Alpine Linux，只有 ~170MB。JDK full image 要 500MB+。

**H2 的好处**：后端镜像里不需要任何数据库客户端或预安装，H2 的 jar 已经打进了 Spring Boot fat jar。

---

## 四、前端 Dockerfile

**文件**：`frontend/Dockerfile`

```dockerfile
# 阶段 1：npm build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com    # 用国内镜像快
COPY . .
RUN npm run build    # Vite build 输出到 dist/

# 阶段 2：Nginx serving
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

Vite build 后输出纯静态文件（HTML + JS + CSS），不需要 Node 运行时。用 Nginx 托管。

---

## 五、Nginx 配置

**文件**：`frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # SPA 路由 fallback：所有路径返回 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到后端容器
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 上传文件反向代理到后端
    location /uploads/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Knife4j 接口文档（生产环境关闭）
    location ~ ^/(doc\.html|webjars/|v3/api-docs) {
        proxy_pass http://backend:8080;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 关键配置解释

**SPA fallback**：`try_files $uri $uri/ /index.html`。浏览器请求 `/r/abc123` 时，Nginx 找不到这个文件，回退到 `/index.html`，让 Vue Router 来处理路由。

**proxy_pass 尾部 `/` 的区别**：
```
proxy_pass http://backend:8080/api/;   # URL 重写：/api/xxx → http://backend:8080/xxx
proxy_pass http://backend:8080;         # 保留原路径：/api/xxx → http://backend:8080/api/xxx
```

本项目后端接口全在 `/api/**`，前端 `/api/xxx` 直接透传到后端 `/api/xxx`，所以第二种写法也对。

**X-Real-IP / X-Forwarded-For**：后端要知道真实访客 IP，必须从这些 header 取。Nginx 反代后 `request.getRemoteAddr()` 拿到的是 Nginx 容器内部 IP。

**静态资源缓存**：JS/CSS/图片设置 `expires 7d` + `immutable`，浏览器 7 天内不会重新请求。Vite 构建时给 hash 文件名（如 `index-abcd1234.js`），内容变了 hash 也变，浏览器就会请求新文件。

---

## 六、环境变量（.env 文件）

```
# H2（有默认值，开发环境可以不配）
H2_USER=sa
H2_PASSWORD=

# 应用配置
JWT_SECRET=your-64-char-secret-key-here
ADMIN_PASSWORD=change-me-please

# 大模型配置（OpenAI 兼容接口，不配置则使用本地关键词问答）
AI_API_URL=https://your-api-endpoint.com/v1/chat/completions
AI_API_KEY=your-api-key-here
AI_MODEL=your-model-name

# 端口
WEB_PORT=80
BACKEND_PORT=8080
```

`.env` 文件加入 `.gitignore`，不进版本库。H2 用户默认 `sa`，密码默认为空，不需要改。

---

## 七、安全最佳实践

### 1. JWT 密钥长度

HS256 签名的密钥至少 32 字节。本项目通过环境变量注入，不硬编码。

### 2. 生产禁用 Swagger UI

设置 `KNIFE4J_PRODUCTION=true` 即可屏蔽 `/doc.html` 等文档资源。

### 3. 后端端口不对外暴露

Docker Compose 里后端 8080 端口可以删掉映射，只在容器网络内。前端 Nginx 反代访问。

### 4. HTTPS 终结

Nginx 可以配 SSL 证书（Let's Encrypt 免费），常见运维工具都支持一键申请。

### 5. H2 数据备份

H2 整个数据库就是一个 `.mv.db` 文件：
```bash
# 备份
docker compose cp resume-backend:/app/data ./h2-backup

# 恢复
docker compose cp ./h2-backup/. resume-backend:/app/data/
```

---

## 八、知识点总结

| 知识点 | 说明 |
|--------|------|
| H2 内嵌数据库 | 不是独立进程，作为 jar 库运行在后端 JVM 里。零配置、零外部依赖 |
| H2 文件模式 | `jdbc:h2:file:/path/to/db` 数据持久化在磁盘文件，重启不丢失 |
| H2 MySQL 兼容模式 | `MODE=MySQL` 保留反引号、AUTO_INCREMENT、LIMIT/OFFSET 等语法 |
| H2 INIT 参数 | `INIT=RUNSCRIPT FROM 'classpath:schema.sql'` 只在 `.mv.db` 不存在时执行 |
| Named Volume vs Bind Mount | Docker 管理 vs 宿主机目录映射。H2 数据用 Named Volume 更稳妥 |
| 多阶段构建 | 构建环境和运行环境分离，大幅减小最终镜像体积 |
| 层缓存 | Dockerfile 里先 COPY pom.xml 再 RUN mvn，利用依赖缓存 |
| Alpine Linux | 极小的发行版，最终后端镜像 ~170MB（JRE Alpine + fat jar） |
| try_files | Nginx 实现 SPA fallback 的标准配置 |
| proxy_pass URL 重写 | 尾部带 `/` 和不带的区别 |
| X-Real-IP | Nginx 把真实访客 IP 透传给后端的标准 header |
| 静态资源 + hash 文件名 | Vite 给 JS/CSS 加 hash，配合 immutable 强缓存 |
| Spring Profile | `SPRING_PROFILES_ACTIVE=prod` 加载 application-prod.yml 覆盖公共配置 |