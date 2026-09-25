# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先拷贝 pom 利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 上传文件持久化目录（通过 volume 挂载）
RUN mkdir -p /app/uploads

COPY --from=builder /build/target/resume-backend.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
