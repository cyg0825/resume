INSERT INTO resume_version (id, version_name, description, is_default, create_time, update_time) VALUES
(1, '全栈工程师（Demo）', '默认版本，通用前端 + 后端能力', 1, '2025-01-01 10:00:00', '2025-06-01 10:00:00'),
(2, '前端工程师（Demo）', '侧重 Vue3 / React 前端方向', NULL, '2025-03-15 10:00:00', '2025-06-01 10:00:00');

INSERT INTO profile (id, version_id, name, job_title, slogan, avatar, email, phone, wechat, github, gitee, csdn, address, about, update_time) VALUES
(1, 1, '张三', '全栈工程师', '持续学习，热爱技术', '/demo/avatar.png', 'zhangsan@example.com', '13800000000', 'zhangsan_demo', 'https://github.com/demo-user', 'https://gitee.com/demo-user', 'https://blog.csdn.net/demo-user', '北京市', '5 年全栈开发经验，熟悉前后端全链路技术栈。注重代码质量与团队协作。', '2025-06-01 10:00:00'),
(2, 2, '张三', '前端工程师', '用代码创造美感', '/demo/avatar.png', 'zhangsan@example.com', '13800000000', 'zhangsan_demo', 'https://github.com/demo-user', 'https://gitee.com/demo-user', 'https://blog.csdn.net/demo-user', '北京市', '专注前端领域多年，精通 Vue3 / React 生态，有丰富的性能优化和组件库建设经验。', '2025-06-01 10:00:00');

INSERT INTO education (id, version_id, school, major, degree, start_date, end_date, description, sort) VALUES
(1, 1, '某某大学', '软件工程', '本科', '2016-09-01', '2020-06-30', '主修课程：数据结构、操作系统、计算机网络、数据库系统、软件工程、算法设计。', 1),
(2, 2, '某某大学', '软件工程', '本科', '2016-09-01', '2020-06-30', '主修课程：数据结构、操作系统、计算机图形学、人机交互设计。', 1);

INSERT INTO experience (id, version_id, type, company, position, start_date, end_date, description, tech_stack, sort) VALUES
(1, 1, 1, '某某科技有限公司', '全栈工程师', '2022-03-01', NULL, '负责公司核心 SaaS 平台的架构设计与开发，带领 5 人小组完成前后端全链路交付。主导技术栈从单体架构向微服务演进，系统吞吐量提升 3 倍。推动 CI/CD 流水线建设，部署效率提升 80%。', 'Vue3, Spring Boot, MySQL, Redis, Docker, Kubernetes, Jenkins', 1),
(2, 1, 1, '某某网络科技', '后端开发工程师', '2020-07-01', '2022-02-28', '参与电商平台后端系统开发，负责订单、支付、优惠券模块。使用 Redis 缓存热点数据，接口响应时间从 500ms 降至 80ms。', 'Spring Boot, MyBatis, MySQL, Redis, RabbitMQ', 2),
(3, 1, 2, '开源博客系统（Demo）', '个人开源项目', '2023-06-01', '2023-12-31', '基于 Vue3 + Spring Boot 的前后端分离博客系统，支持 Markdown 编辑、文章分类标签、访问统计。GitHub Star 500+。', 'Vue3, Element Plus, Spring Boot, JWT, MySQL', 1),
(4, 1, 2, '在线代码编辑器（Demo）', '课程设计项目', '2019-03-01', '2019-06-30', '支持 5 种编程语言的在线代码运行平台，基于 Monaco Editor 和 WebAssembly 实现前端代码高亮与执行。', 'React, Monaco Editor, WebAssembly, Node.js', 2),
(5, 2, 1, '某某科技有限公司', '高级前端工程师', '2022-03-01', NULL, '主导前端架构升级，将项目从 Vue2 迁移至 Vue3 + Vite，构建时间从 90 秒缩短至 15 秒。建设公司内部组件库，覆盖 60+ 常用组件，支撑 10+ 业务团队复用。', 'Vue3, Vite, TypeScript, Element Plus, Webpack', 1),
(6, 2, 2, '数据可视化大屏（Demo）', '独立开发', '2024-01-01', '2024-03-31', '基于 ECharts 的多主题数据大屏，支持实时数据更新、自适应布局、全屏展示。', 'Vue3, ECharts, SCSS, WebSocket', 1);

INSERT INTO skill (id, version_id, category, name, level, sort) VALUES
(1, 1, '前端', 'Vue3 / Vue2', 92, 1),
(2, 1, '前端', 'React', 80, 2),
(3, 1, '前端', 'TypeScript', 85, 3),
(4, 1, '前端', 'Element Plus / Ant Design', 90, 4),
(5, 1, '前端', 'Vite / Webpack', 82, 5),
(6, 1, '前端', 'Tailwind CSS', 78, 6),
(7, 1, '后端', 'Spring Boot', 90, 7),
(8, 1, '后端', 'MyBatis / JPA', 85, 8),
(9, 1, '后端', 'MySQL / PostgreSQL', 88, 9),
(10, 1, '后端', 'Redis', 82, 10),
(11, 1, '后端', 'Docker / Kubernetes', 75, 11),
(12, 1, '后端', 'RabbitMQ / Kafka', 72, 12),
(13, 2, '前端', 'Vue3', 95, 1),
(14, 2, '前端', 'React', 82, 2),
(15, 2, '前端', 'TypeScript', 88, 3),
(16, 2, '前端', 'Element Plus / Ant Design', 92, 4),
(17, 2, '前端', 'Vite / Webpack', 85, 5),
(18, 2, '前端', 'ECharts / D3.js', 80, 6),
(19, 2, '前端', 'Jest / Vitest', 75, 7);

INSERT INTO portfolio (id, owner_name, title, cover, url, description, sort) VALUES
(1, '张三', '开源博客系统（Demo）', '/demo/portfolio-blog.png', 'https://github.com/demo-user/blog', '基于 Vue3 + Spring Boot 的前后端分离博客，支持 Markdown、统计，GitHub Star 500+。', 1),
(2, '张三', '数据可视化大屏（Demo）', '/demo/portfolio-dashboard.png', 'https://github.com/demo-user/dashboard', '多主题实时数据大屏，ECharts + WebSocket。', 2),
(3, '张三', '在线面试平台（Demo）', '/demo/portfolio-interview.png', 'https://github.com/demo-user/interview', '支持视频面试、在线代码编辑的一站式面试平台。', 3),
(4, '张三', '个人简历网站（本项目 Demo）', '/demo/portfolio-resume.png', 'https://github.com/demo-user/resume', 'Vue3 + Spring Boot 3 + H2 全栈个人简历网站，多主题、AI 问答、PDF 导出。', 4);

INSERT INTO honor (id, owner_name, title, image, issuer, level, honor_date, description, sort) VALUES
(1, '张三', '校级一等奖学金', '/demo/honor-scholarship.png', '某某大学', '校级', '2017-06-01', '综合成绩排名专业前 5%', 1),
(2, '张三', 'ACM 程序设计大赛省赛三等奖', '/demo/honor-acm.png', '中国计算机学会', '省级', '2018-11-01', '作为主力队员代表学校参赛', 2),
(3, '张三', '全国大学生软件创新大赛二等奖', '/demo/honor-soft.png', '教育部高等学校计算机类专业教学指导委员会', '国家级', '2019-05-01', '作品「在线代码编辑器」获评审专家高度评价', 3),
(4, '张三', '公司年度优秀员工', '/demo/honor-employee.png', '某某科技有限公司', '公司级', '2023-12-01', '因技术贡献与团队协作获评年度优秀员工', 4);

INSERT INTO site_config (id, site_title, default_theme, ai_enabled, update_time) VALUES
(1, '张三的个人简历（Demo）', 'default', 1, '2025-06-01 10:00:00');

INSERT INTO user (id, username, password, nickname, avatar, create_time) VALUES
(1, 'admin', '$2a$10$X9jyzwybeqmCxSVPf4PrxOTTy8y9mjD5RqY9//m0gAoKcl/D4dCI.', '管理员', NULL, '2025-01-01 10:00:00');

INSERT INTO share_link (id, token, remark, version_id, expire_time, max_views, view_count, enabled, last_view_time, last_view_ip, create_time) VALUES
(1, 'abc123def456abc123def456abc123def456abc123def456abc123def456ab', '简历分享链接（演示）', 1, NULL, NULL, 0, 1, NULL, NULL, '2025-06-01 10:00:00');

INSERT INTO ai_chat_history (id, session_id, question, answer, source, create_time) VALUES
(1, 'demo-session-001', '你好，介绍一下你自己', '你好！我是张三，5 年全栈工程师经验，主要技术栈是 Vue3 + Spring Boot。可以问我关于教育背景、工作经历、技能等问题。', 'llm', '2025-06-10 09:00:00'),
(2, 'demo-session-001', '最擅长的技术是什么', '前端方面最擅长 Vue3 生态，后端方面最擅长 Spring Boot 和 MyBatis-Plus。', 'llm', '2025-06-10 09:01:00'),
(3, 'demo-session-002', '做过哪些项目', '做过开源博客系统、数据可视化大屏、在线面试平台等项目，具体可以看作品集板块。', 'llm', '2025-06-12 14:30:00'),
(4, 'demo-session-003', '怎么联系你', '可以通过邮箱 zhangsan@example.com 联系我，或者在 GitHub 上私信。', 'llm', '2025-06-15 20:00:00');

INSERT INTO visit_log (id, ip, user_agent, referer, path, session_id, visit_time) VALUES
(1, '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', NULL, '/', 'sess-001', '2025-06-10 09:00:00'),
(2, '192.168.1.101', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36', 'https://www.google.com/', '/', 'sess-002', '2025-06-10 10:30:00'),
(3, '192.168.1.102', 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15', 'https://github.com/', '/', 'sess-003', '2025-06-11 15:20:00'),
(4, '192.168.1.103', 'Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36', NULL, '/', 'sess-004', '2025-06-12 08:45:00'),
(5, '192.168.1.104', 'curl/7.68.0', NULL, '/api/profile', 'sess-005', '2025-06-13 12:00:00');