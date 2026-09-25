# 功能六：PDF 简历导出

## 功能概述

首页右上角"导出 PDF"按钮，一键把当前简历的双栏排版导出成 A4 多页 PDF。

核心技术：**html2canvas 把 DOM 截成长图 → jsPDF 把长图按 A4 高度切片 → 逐页写入 PDF**。

---

## 一、为什么不用后端生成 PDF

后端生成 PDF（如 iText）的问题：
- 需要重新定义一套布局样式（CSS 里写的排版在后端 PDF 库不认）
- 图片、字体加载是另一套逻辑
- 维护成本高——改了网页样式就得同步改 PDF 模板

前端 DOM 截图的好处：所见即所得，网页长什么样 PDF 就什么样。

---

## 二、html2canvas-pro vs 原版 html2canvas

原版 html2canvas 的问题：不支持 `color-mix()`、`oklch()` 等现代 CSS 颜色函数。Element Plus 和本项目主题大量用了这些函数，原版会直接抛错。

所以用了社区 fork 的 `html2canvas-pro`，它修复了这些兼容性问题。

---

## 三、导出流程

**文件**：`frontend/src/utils/pdf.js`

```
1. 等待字体和图片渲染完成
   document.fonts?.ready  // 等自定义字体加载完
   window.scrollTo(0, 0)  // 回到页顶，保证截图从顶部开始

2. 进入导出态
   document.documentElement.classList.add('pdf-exporting')
   // CSS 里 .pdf-exporting 会：
   //   - 强制所有 v-reveal 元素显示（不等待 IntersectionObserver）
   //   - 把简历容器固定到 1080px 宽度（A4 比例换算）
   //   - 隐藏页脚的操作按钮和版权信息
   //   - 两帧 + 80ms 延时，让浏览器完成布局重排

3. 整页截图
   canvas = await html2canvas(element, {
     useCORS: true,       // 允许跨域图片
     backgroundColor: bg, // 背景色防止透明区域
     scale: 2,            // 2 倍分辨率 → PDF 清晰
     windowWidth: 1080    // 强制 1080px 宽
   })

4. 计算 A4 页对应的 CSS px 高度
   // A4: 210×297mm，扣掉上下各 7mm 页边距
   // 内容区宽 196mm，高 283mm
   // CSS px 高度 = canvas 实际宽 * 内容区高 / 内容区宽

5. 收集左栏和右栏的"不可分割单元"
   collectColumnSlices()
   
6. 两栏各自独立按页装箱
   paginateColumn() → 每页要画哪些切片

7. 创建 jsPDF 对象，逐页写入
   pdf = new jsPDF('p', 'mm', 'a4')
   for each page:
     pdf.addPage()
     // 左栏画左半部分，右栏画右半部分
     columns.forEach(col => pdf.addImage(drawSlice(...), ...))

8. 下载
   pdf.save(fileName + '.pdf')
```

---

## 四、切片收集（collectColumnSlices）

**为什么不能直接把长图按 297mm 高度一刀切**：卡片可能被切到中间，比如一个工作经历的卡片正好跨越了页边界，切到一半就断了。

所以收集"不可分割单元"的逻辑：

```
1. 找到栏内所有 .section-block（板块）和 .hero（头像区）
2. 对每个板块：
   a. 板块内部找可以拆的卡片：.el-timeline-item, .edu-card, .skill-group 等
   b. 没有可拆卡片（比如个人评价板块就是一整块）→ 整个板块作为一个切片
   c. 板块整体比一页还高 → 整个板块作为一个切片（后面 paginateColumn 会在板块内部硬切）
   d. 常规板块 → 拆成：
      - 板块标题（.section-title）作为独立切片
      - 板块内的卡片，同行的网格卡片合并成一个切片（top 差 < 2px 视为同行）
3. 切片之间的留白并到前一个切片底部，保证拼接无缝
```

这样做的效果：工作经历的时间线项、技能分组、教育卡片都是完整出现在某一页的标题下面。

---

## 五、分页装箱（paginateColumn）

**算法**：类似"首次适应"装箱问题，但更简单——只需要在当前页放不下整个单元时换页。

```
for 每个切片:
  if 切片高度 ≤ 当前页剩余高度:
    if 是板块标题 && 下一张卡片会放不进当前页但能放进新页:
      整体移到下一页（防止标题孤悬页底）
    装入当前页，更新 cursor
  else（切片本身比一页还高）:
    在当前页装能装的部分
    剩余部分新页装
    循环直到这个切片全部装完
```

**板块标题孤悬处理**：
```
光标在页底还剩一点空间，正好能放下标题，但放不下标题+下一张卡片
→ 整个板块（标题+卡片）移到下一页
```
这样不会出现"第 2 页末尾孤零零挂着个'工作经历'标题，内容全在第 3 页"的难看布局。

---

## 六、双栏同步

左栏和右栏各自独立分页后：

```
总页数 = max(左栏页数, 右栏页数)
for pageIdx = 0 to 总页数-1:
  pdf.addPage()
  左栏画第 pageIdx 页的切片（如果左栏还有内容）
  右栏画第 pageIdx 页的切片（如果右栏还有内容）
```

这样两栏在同一页上并排显示。如果左栏 3 页、右栏 5 页，总共有 5 页，前 3 页左右都有内容，后 2 页只有右栏有内容。

---

## 七、drawSlice：从长图裁栏内区域

长图 canvas 的坐标系是从 `(0, 0)` 到 `(canvas.width, canvas.height)`。要裁左栏某个切片：

```
sx = 栏左边界 * scale          // scale = canvas.width / 1080
sy = 切片顶部 * scale
sw = 栏宽度 * scale
sh = 切片高度 * scale

sliceCanvas = 新canvas
sliceCanvas.getContext('2d').drawImage(
  原canvas,
  sx, sy, sw, sh,           // 源区域（从长图里裁）
  0, 0, sw, sh              // 目标区域（画到新 canvas）
)
JPEG base64 = sliceCanvas.toDataURL('image/jpeg', 0.92)
```

然后 `pdf.addImage(base64, 'JPEG', xMm, yMm, wMm, hMm)` 把这张图贴到 PDF 的指定位置。

---

## 八、为什么用 JPEG 而不是 PNG

PDF 里每页有很多切片图。PNG 无损但文件大（每页 500KB+）；JPEG 质量 0.92 时肉眼看不出区别，但每页只有约 100KB。

---

## 九、导出态 CSS 的必要性

```css
.pdf-exporting {
  /* 强制 1080px 固定宽，对应 A4 比例 */
  --page-width: 1080px;
  
  /* 禁用入场动画，所有元素立即显示 */
  .reveal:not(.reveal-in) { opacity: 1; transform: none; }
  
  /* 隐藏不导出的元素 */
  .no-pdf-export { display: none !important; }
  
  /* 把双栏 grid 的 gap 设为 0，导出后再恢复 */
}
```

导出完成后立即 `classList.remove('pdf-exporting')`，页面恢复正常浏览态。

---

## 十、知识点总结

| 知识点 | 说明 |
|--------|------|
| html2canvas | 用浏览器渲染引擎（不是 Canvas API）把 DOM 绘到 canvas 上。现代 CSS 兼容需要用 pro fork |
| jsPDF | 生成 PDF 的纯前端库，addPage/addImage/addText 链式 API，mm 单位 |
| scale 参数 | 截图 2 倍或 3 倍分辨率，保证在高 DPI 屏幕和 PDF 里清晰 |
| Document.fonts.ready | 等所有 @font-face 字体加载完再截图，否则截图里是系统默认字体 |
| requestAnimationFrame 双帧等待 | 确保布局重排完成。浏览器的 CSS 改变不会同步反映到 layout，至少等两帧 |
| 切片装箱 | 避免卡片被页边界截断。类似装箱算法，首次适应 + 板块标题孤悬处理 |
| 板块标题孤悬 | 标题单独出现在页底、内容在下一页会很丑，检测到就整体下移 |
| 双栏独立分页后合并 | 两栏的"不可分割单元"是独立的，各自分页后同一页的切片并排画 |
| CSS 运行时类 | 导出态加一个类，CSS 里用选择器匹配。完成后移除，页面立即恢复 |
| A4 比例换算 | 210:297 = paperW:paperH。用页面宽度反推每页 CSS px 高度 |