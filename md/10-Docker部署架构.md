# 10 打包与容器编排

部署单元是两个容器：后端（内含 H2）和前端（Nginx 托管构建产物）。这一篇记录镜像怎么分层、配置怎么从环境变量注入、数据落在哪，以及一个必须先知道的事实——当前配置下每次重启都会重建数据库。

## H2 内嵌决定了编排形状

`docker-compose.yml` 只有 `backend` 和 `frontend` 两个服务（`:2`、`:28`），没有数据库服务，也没有 `healthcheck`。原因在第 11 篇：H2 是跑在后端 JVM 里的 jar 库，不是独立进程，JDBC URL 直接读写磁盘上的 `.mv.db` 文件。所以：

- 不需要等数据库就绪，`depends_on` 只声明后端先启动（`:33-34`），是普通列表写法，没有 `condition: service_healthy`。
- 数据持久化靠一个 named volume（`h2-data`，`:25-26`、`:38-40`）挂到 `/app/data`，配合 `H2_DATA_DIR: /app/data/resume`（`:9`）——URL 前缀落在卷里，文件名是 `resume`。
- 备份就是把这个卷里的 `.mv.db` 拷出来，单文件即全量快照。

也没有自定义 `networks` 段，两个服务落在 compose 的默认网络里，靠服务名互相解析——前端 Nginx 配置里的 `proxy_pass http://backend:8080` 用的就是这个 DNS 名。

## 后端镜像

`backend/Dockerfile` 两段式：

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder     # :2
COPY pom.xml .                                   # :5
RUN mvn -B -q dependency:go-offline              # :6
COPY src ./src
RUN mvn -B -q clean package -DskipTests          # :8

FROM eclipse-temurin:17-jre                      # :11
RUN mkdir -p /app/uploads                        # :15
COPY --from=builder /build/target/resume-backend.jar /app/app.jar   # :17
ENTRYPOINT ["java", "-jar", "/app/app.jar"]      # :20
```

三个点：

- **层缓存的切法**：先 `COPY pom.xml` 再 `dependency:go-offline`，只有 pom 变了才重装依赖；改代码只重跑 `package`。这是标准做法，但 `dependency:go-offline` 对插件依赖并非 100% 完整，偶尔仍会在 `package` 阶段重新下载。
- **jar 名固定**：`pom.xml:100` 设了 `<finalName>resume-backend</finalName>`，所以 COPY 写死文件名而不是 `*.jar`。改 finalName 会直接构建失败，两处要一起动。
- **运行镜像是 `17-jre`**：Spring Boot 只需要 JRE 就能跑，构建阶段的 JDK 留在 builder 里，运行镜像不带。

`ENTRYPOINT` 里没有 `--spring.profiles.active`，profile 完全由环境变量决定。而 `application.yml:13` 的默认值本身就是 `prod`（`${SPRING_PROFILES_ACTIVE:prod}`），也就是零参数启动就是生产配置。接口文档的开关跟着 profile 走：

- jar 直接启动或容器启动都取 `application-prod.yml:22-23` 的 `knife4j.production`，默认为 `true`，`/doc.html` 是关的；compose 里 `KNIFE4J_PRODUCTION: ${KNIFE4J_PRODUCTION:-true}`（`docker-compose.yml:20`）与这个默认值同向。
- 想看文档把 `KNIFE4J_PRODUCTION` 设为 false，或用 `SPRING_PROFILES_ACTIVE=default` 起一次——`application.yml:60` 的默认值是 false，只有非 prod profile 才会开。

## 前端镜像与 Nginx 行为

`frontend/Dockerfile`：`node:20-alpine` 构建（`:2`），`nginx:1.27-alpine` 托管（`:10`），只把 `dist/` 和 `nginx.conf` 拷进运行镜像（`:11-12`），没有 `CMD`（基础镜像自带）。Vite 产物是纯静态文件，运行期不需要 Node。

依赖安装走 lockfile：`COPY package.json package-lock.json ./`（`:4`）+ `npm ci --no-audit --no-fund`（`:5`）。`npm ci` 严格按 `package-lock.json` 装版本，同一份 lockfile 每次构建出同一棵依赖树；代价是 lockfile 与 `package.json` 不一致时直接构建失败，加了依赖得先本地跑一次 `npm install` 把 lock 提交上来。

Nginx 侧只记录会影响行为选择的几项（配置见 `frontend/nginx.conf`）：

- SPA 回退 `try_files $uri $uri/ /index.html`（`:19-21`）。项目用 hash 路由（第 06 篇），实际上访问者不会请求到 `/r/xxx` 这类真实路径，这段配置是为直接打开子路径的情况兜底。
- `/api/` 反代带四个头：`Host`、`X-Real-IP`、`X-Forwarded-For`、`X-Forwarded-Proto`，外加 `proxy_read_timeout 180s`（`:23-32`）。真实 IP 全靠这几个头，缺一个访问统计就会把所有访客算成同一个内网地址（第 09 篇）。180 秒这个数是对齐前端 AI 导入请求的超时（`api/index.js:114`）——Nginx 的默认读超时是 60 秒，大 PDF 走 AI 结构化时中间这一层会先把连接断掉，两侧限制不一致时先断的那层决定失败表现。
- `proxy_pass http://backend:8080` 不带 URI 段，路径原样透传。写法上 `http://backend:8080/`（带尾斜杠）会重写路径，本项目后端接口本身就在 `/api/**` 下，两种写法结果相同，但不带斜杠不容易出错。
- `client_max_body_size 12m`（`:15-16`）与后端 multipart 的 `max-request-size: 12MB`（`application.yml:29`）取同一个数，注释里写明了这个对应关系（`nginx.conf:15`）。超限时 Nginx 先返回 413；请求真到得了后端又超后端限制时，由 `common/GlobalExceptionHandler.java:43-47` 转成可读的 413 业务码。哪一层取值更小，错误就从哪一层返回，取值一致时前端只有一种表现。
- 上传目录 `location ^~ /uploads/`（`:34-42`）反代到后端的 `/uploads/**` 静态映射。`^~` 是必需的：`:53` 有一条正则 `location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$` 做静态资源强缓存，而 Nginx 的匹配次序是「先记下最长前缀匹配，再按配置顺序试正则，正则命中就用正则」，除非前缀带 `^~` 或 `=`。没有这个修饰符时 `/uploads/xxx.jpg` 会命中缓存正则、被当成本地文件去 `/usr/share/nginx/html` 找，结果是 404；本地直连 8080 端口看不出问题，只有走完容器链路才暴露。
- 接口文档路径单独一条正则代理（`:44-50`），覆盖 `/doc.html`、`/swagger-ui.html`、`/webjars/`、`/v3/api-docs`、`/swagger-ui/`、`/swagger-resources/`、`/favicon.ico`。这几条对应后端白名单里的同一组路径（`config/SecurityConfig.java:49-60`，第 02 篇）：Nginx 不代理则 404，后端不放行则 401，两处要一起对齐才有可用的文档入口。
- gzip 覆盖 css/js/json/svg 等类型，`gzip_min_length 1024`（`:8-13`）——小于 1KB 的文件压缩收益抵不过 CPU。
- 静态资源缓存 `expires 7d` + `Cache-Control: public, immutable`（`:52-56`）。Vite 产物文件名带 hash，内容变了名字就变，强缓存安全；不带 hash 的文件（例如手工放进去的静态图）会被缓存 7 天，改完不生效是这个配置的必然结果。

## 配置注入：只记配置项名

compose 的 `environment`（`docker-compose.yml:7-21`）里出现的项：`SPRING_PROFILES_ACTIVE`、`H2_DATA_DIR`、`H2_USER`、`H2_PASSWORD`、`JWT_SECRET`、`ADMIN_USERNAME`、`ADMIN_PASSWORD`、`AI_API_URL`、`AI_API_KEY`、`AI_MODEL`、`AI_EMBED_URL`、`AI_EMBED_MODEL`、`KNIFE4J_PRODUCTION`、`UPLOAD_DIR`，端口变量 `BACKEND_PORT`、`WEB_PORT`（`:22-23`、`:35-36`）。

后端还会读但 compose 没有列出的：`SERVER_PORT`、`H2_CONSOLE_ENABLED`、`JWT_EXPIRE`、`SHARE_TOKEN_EXPIRE`、`VISIT_SESSION_WINDOW_MINUTES`、`AI_IMPORT_MODEL`、`LOG_LEVEL`（`application.yml:7-108`、`application-prod.yml:28`）。要改这些得往 compose 里补。

这些项的**值一个都不写进笔记**。`JWT_SECRET`、`ADMIN_PASSWORD`、`AI_API_KEY` 这三项尤其注意：compose 与 `application.yml` 里给的是占位默认值（`docker-compose.yml:10-14`、`application.yml:70,82-83`），依赖默认值等于用公开在仓库里的口令起服务；实际部署要么放 `.env`（已被 `.gitignore:21-24` 忽略），要么在编排层注入。`.gitignore` 同时忽略了 `backend/data/`、`backend/uploads/`、`uploads/`、`docker-data/`（`:26-32`、`:41-42`），本地跑起来产生的数据库文件和上传文件不会误提交。

后端 8080 默认映射到宿主机（`docker-compose.yml:22-23`）是为了直接调接口、看 SQL 日志方便；容器网络内部通信走的是服务名，不需要这段映射，只留前端 80 也能跑完整链路。

## 每次重启都会重建数据库

这是这一篇最重要的一条。JDBC URL 末尾无条件带了初始化脚本（`application.yml:18`，`application-prod.yml:12` 又重复声明了一遍同样的 URL）：

```
INIT=RUNSCRIPT FROM 'classpath:schema.sql';RUNSCRIPT FROM 'classpath:data.sql'
```

而 `schema.sql:1-12` 开头是 12 条 `DROP TABLE IF EXISTS <表名> CASCADE;`，随后才是 `CREATE TABLE`；`data.sql` 用的是显式主键的 `INSERT INTO`（`:1,5,9,13,21,42,48,54,57,60,63,69`）。这三段事实合起来的意思不是「首次启动建表」，而是：

**后端进程每次启动都会先删掉全部 12 张表并重建，再灌一遍 Demo 数据。**

后果列一遍：管理员改过的密码、新建的简历版本、编辑过的内容、签发中的专属链接、全部访问日志和 AI 问答记录、上传文件对应的数据库记录——都在重启瞬间回到仓库里 `data.sql` 的状态。文件卷里的图片还在（`uploads-data` 不受影响），但指向图片的数据库记录被换成 Demo 的固定几条，页面表现为头像/证书图片 404。`data.sql:60` 也种了一条固定的演示分享链接，重启后这条链接重新可用、真实签发的那些全部消失。

任何「重启后端以生效配置」的操作在这个机制下都会清库，包括 `docker compose restart`、容器 `restart: unless-stopped` 触发的自动拉起（`:6`、`:32`）。

这个行为的源头是把「首次建库」和「每次建库」写在了同一个脚本里，H2 的 `INIT` 没有「只在首次执行」的开关。所以本项目的运维约定是：**把 `.mv.db` 当成唯一真相源，动手重启之前先备份卷**：

```bash
docker compose stop backend
docker cp resume-backend:/app/data ./h2-backup-$(date +%F)
docker compose up -d backend
```

恢复是把备份目录拷回 `/app/data` 再启动。注意顺序：必须在后端停止时拷贝，H2 文件模式下进程持有文件句柄，热拷会得到不一致的快照。

`h2-console` 默认关闭（`application.yml:21-24`，由 `H2_CONSOLE_ENABLED` 控制，路径 `/h2-console`）。这个路径不在鉴权放行列表里，末尾的 `anyRequest().denyAll()`（`config/SecurityConfig.java:82`）会把 `/h2-console` 一起拦住——即使临时把开关打开，浏览器访问控制台依然返回 401。拦住是对的：H2 的 Web 控制台只有数据库账号这一道门，放行等于把整个库挂到公网路径上。

排查「数据怎么没了」走离线路线：先停后端、把卷拷出来（文件模式下进程持有句柄，不能热读），再用本地 Maven 仓库里的 H2 jar 起一个只连这个文件的会话：

```bash
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:./h2-backup-2026-01-01/resume;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER" \
  -user sa -sql "SELECT COUNT(*) FROM visit_log"
```

URL 里那串模式参数要和后端用的保持一致，`NON_KEYWORDS=USER` 漏掉的话连 `user` 表都查不了。查出来的 `share_link.token` 属于访客凭证，不要往笔记或截图里带。

## 可复用点

- **先看数据库是不是独立进程，再决定 compose 长什么样**：H2 是 jar 库，于是编排里没有数据库服务、没有 `healthcheck`、`depends_on` 是普通列表而不是 `condition: service_healthy`（`docker-compose.yml:33-34`），持久化退化成一个 named volume 挂目录（`:25-26`、`:38-40`），备份退化成拷一个 `.mv.db` 文件。换成 MySQL/Postgres 时这三样要一起加回来：多一个服务、多一道就绪等待、备份改走 dump 而不是拷文件。个人项目选内嵌库换来的简化幅度很大，代价是数据库和后端进程生命周期绑死，重启后端等于重启数据库。
- **默认值的安全方向选「关」**：`SPRING_PROFILES_ACTIVE` 默认 `prod`（`application.yml:13`），接口文档开关在 prod 下是 `production: true`（`application-prod.yml:22-23`），compose 的兜底同向（`docker-compose.yml:20`），非 prod 才默认 false（`application.yml:60`）。零参数启动拿到的是收紧配置，想看文档要显式改一个变量。反过来配（默认开文档、prod 才关）在忘记注入环境变量时就把接口暴露面打开了。同理适用于注册开关、调试端点、CORS 全放。
- **两处依赖安装的确定性写法值得固定下来**：后端先 `COPY pom.xml` 再 `dependency:go-offline`、只有 pom 变更才重装依赖（`backend/Dockerfile:5-6`），前端 `COPY package.json package-lock.json` + `npm ci --no-audit --no-fund`（`frontend/Dockerfile:4-5`）。`npm ci` 严格按 lockfile 装，同一份 lock 每次构建出同一棵树；代价是 lockfile 与 `package.json` 不一致时直接失败，加了依赖必须先本地 `npm install` 把 lock 一起提交。运行镜像只留 JRE（`backend/Dockerfile:11`），构建期的 JDK 留在 builder 阶段。
- **跨层限制要写成同一个数并且注明来源**：`client_max_body_size 12m`（`nginx.conf:15-16`）与后端 `max-request-size: 12MB`（`application.yml:29`）取同一个值，Nginx 侧注释直接写明对应的配置项名；`proxy_read_timeout 180s`（`nginx.conf:31`）对齐前端导入请求的超时（`api/index.js:114`），而后端模型调用是 120 秒（`ResumeImportService.java:274`）。这类"链路上取值最小的那一层先报错"的三个数（体积、超时、重试）散在四个文件里，改任何一个都要顺带看另外三个；把对应关系写进注释是唯一的防漂移手段，否则表现就是"大文件报错信息随改动的层而变化"。
- **反代特有的行为必须在完整链路里验一次**：`/uploads/` 那条 `location ^~`（`nginx.conf:34-42`）是必需的——正则缓存 `location ~* \.(…|jpg|…)$`（`:53`）会先命中，把图片当本地文件去 `/usr/share/nginx/html` 找然后 404。直连后端 8080 永远复现不出来，只有走完「浏览器 → Nginx → 后端」这条链才暴露。同类的还有 `X-Real-IP` / `X-Forwarded-For`（`:28-29`）缺头时统计全塌成一个内网地址（第 09 篇），以及文档路径正则与后端白名单两处要对齐（`:44-50` ↔ `config/SecurityConfig.java:49-60`）。经验性的做法是：把"只有经过 Nginx 才成立的行为"逐条列成清单，每次改 Nginx 配置后按清单走一遍容器链路。

