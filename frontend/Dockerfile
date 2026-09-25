# ---------- 构建阶段 ----------
FROM node:20-alpine AS builder
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# ---------- 运行阶段：Nginx 托管静态文件 ----------
FROM nginx:1.27-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
