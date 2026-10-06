# 07 简历 PDF 前端导出

点击导航栏的下载按钮，浏览器直接产出一份 A4 PDF。整条链路都在前端：把 `#resume-content` 截成一张长图，按左右两栏各自切块装箱，再逐块贴进 jsPDF。服务端不参与排版——后端虽然引了 PDFBox（`backend/pom.xml:80-83`），但那是给 PDF 简历导入解析文本用的（第 05 篇），和导出无关。

这条路线的固有代价要先说清楚：产出的是「图片 PDF」，文字不可选中、不可检索、复制到外部也拿不到内容。换来的是所见即所得——网页什么样，PDF 就什么样，不需要维护第二套排版模板。

## 依赖与调用入口

`frontend/src/utils/pdf.js:1-4` 引的两个包：

```js
// 使用 html2canvas-pro：官方 html2canvas 1.x 不支持 color-mix()/oklch() 等现代
// CSS 颜色函数（Element Plus 及本项目主题大量使用），会直接抛错导致导出失败
import html2canvas from 'html2canvas-pro'
import { jsPDF } from 'jspdf'
```

`color-mix(in srgb, …)` 在项目里用得很重，例如 `.section-block` 的背景和边框（`styles/theme.css:224-225`）、顶部导航的半透明底（`views/home/ResumeHome.vue:366`）。用官方 html2canvas 1.3.x 在这里必挂，所以 `frontend/package.json:17` 装的是社区 fork `html2canvas-pro@^1.6.7`。

入口只有一个按钮和一次函数调用：

- `ResumeHome.vue:21-29`：`:disabled="exporting || loading"`，加载中不允许点（此时内容还没渲染完，截出来是空骨架）。
- `ResumeHome.vue:228-239`：置 `exporting`，`await exportResumePdf(document.getElementById('resume-content'))`，成功 `ElMessage.success`，失败打印原始异常并提示「PDF 导出失败，请重试」，`finally` 复位。

函数签名 `exportResumePdf(element, fileName = '个人简历')`（`pdf.js:22`），首行 `if (!element) return`（`:23`）。文件名只有函数签名一处定义，调用处不再重复传参，导出的永远是 `个人简历.pdf`。

## 导出态：先改版式，再截图

截图前必须把页面切成「适合打印的版式」，靠给根元素加一个类完成：

```js
const root = document.documentElement
root.classList.add('pdf-exporting')   // pdf.js:31-32
```

`finally` 里无条件移除（`:77-79`），异常路径也不会留脏状态。样式覆盖分两处：

| 位置 | 作用 |
|------|------|
| `theme.css:141-146` | `.pdf-exporting .reveal` 强制 `opacity: 1; transform: none`，视口外没触发入场动画的板块也必须可见 |
| `theme.css:148-149` | `body { overflow: hidden !important }`，按 1080px 定宽渲染时右侧不会截到滚动条黑边 |
| `theme.css:154-160` | `#resume-content img` 全部不显示，只有 `.hero img.avatar-img` 重新 `display: block`——头像留，作品封面不留 |
| `theme.css:164-166` | `#contact`、`#portfolio` 整块隐藏，属于网页引流/交互区 |
| `theme.css:170-198` | 卡片 padding、标题字号、图标尺寸、技术标签全面收紧，压页数 |
| `ResumeHome.vue:768-796` | `.resume-main` 宽度写死 1080px + `max-width: none`；栅格改 `290px minmax(0, 1fr)` + `gap: 10px`；侧栏 `position: static`；`.page-footer` 隐藏 |

`ResumeHome.vue:779-782` 那条 `position: static` 是关键：网页态侧栏是 `sticky; top: 74px`（`:566-568`），html2canvas 遍历 DOM 计算坐标时吸顶元素会按「滚动后位置」参与布局，长图里左栏内容会错位甚至重复。导出态退回普通流才安全。

`theme.css:154-160` 的注释还专门解释了头像为什么能留：第二条选择器特异性必须高于第一条（含 id + 两个类）。这类靠特异性次序实现的行为，改样式时很容易踩坏。

## 等待与截图参数

`pdf.js:26-28` 与 `:39-41` 一共四道等待：

```js
await document.fonts?.ready
window.scrollTo(0, 0)
await new Promise((r) => setTimeout(r, 60))     // 类刚加上，等一帧排版
...
await new Promise((r) =>
  requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(r, 80)))
)                                                // 两帧 + 短延时，确保重排完成
```

双 `requestAnimationFrame` 是常见写法：第一帧回调里再注册一帧，能保证「本次样式变更后的布局已经计算完」再往后走一步。没有等图片 `onload`——头像在首屏就加载过了，`<img>` 的 `fetchpriority="high"`（`HeroSection.vue:12`）也是为这个；如果哪天头像改成懒加载，导出就会拿到空白头像，需要补等待。

截图配置逐项写在 `:45-52`：

```js
canvas = await html2canvas(element, {
  useCORS: true,
  allowTaint: false,
  backgroundColor: bgColor,     // getComputedStyle(document.body).backgroundColor || '#f5f7fb'
  scale: 2,
  scrollY: 0,
  windowWidth: element.scrollWidth
})
```

- `scale: 2`：按 2 倍设备像素渲染，文字边缘才不至于糊。这也决定了后面 `scale = canvas.width / rootWidthPx` 大约是 2。
- `allowTaint: false` 配 `useCORS: true`：跨域且没带 CORS 头的图片不允许画进画布，宁可不出图也不产出「被污染、无法 `toDataURL`」的脏画布。头像走的是同域 `/uploads/**`，不受影响。
- `scrollY: 0`：告诉 html2canvas 按「页面在顶部」来算坐标，前面 `scrollTo(0, 0)` 是为了让真实布局也一致。
- `windowWidth: element.scrollWidth`：给内部媒体查询一个等于内容宽度的视口宽度，避免导出瞬间页面按当前窗口宽度重排。
- 背景色取 body 的计算色而不是写死，三套主题下底色不同（`#f5f7fb` / `#0b1120` / `#f3faf6`），写死会让深色主题的 PDF 出现大片白色边。

没有用到 `onclone`、`logging`、`width/height` 这些参数，导出态完全靠真实 DOM 上的 class 实现。

## 三套单位换算

这份代码里最容易看错的是「px」有三种含义，各自换算一次：

```js
const scale = canvas.width / rootWidthPx                        // :87 画布像素 / CSS 像素
const mmPerPx = (pageWidth - 2 * PAGE_MARGIN_MM) / rootWidthPx   // :88 CSS 像素 / 毫米
const pageHeightPx = ((297 - 2 * PAGE_MARGIN_MM) * rootWidthPx) / (210 - 2 * PAGE_MARGIN_MM)  // :59-60
const pageHeightMm = pageHeight - 2 * PAGE_MARGIN_MM             // :104
paginateColumn(col.slices, pageHeightMm / mmPerPx)               // :105-107 → 装箱用的 CSS px 页高
```

`rootWidthPx` 是导出态下 `#resume-content` 的 CSS 宽度，也就是 `:54-55` 那一刻读到的 `getBoundingClientRect().width`（≈1080）。`:85-86` 的注释提醒：`pdf-exporting` 类在 `finally` 里已经移除，页面退回响应式宽度，之后再读 `element` 的实时宽度就错了，所以要在截图时把 `rootWidthPx` 存进外层变量。

`pageHeightPx` 用 A4 内容区（210−2×7 mm 宽、297−2×7 mm 高）按等比反推「截图里一页该有多高」，用于切片高度判断；`pageHeightMm / mmPerPx` 是同一个量的另一种写法，用于装箱。`PAGE_MARGIN_MM = 7`（`:136`）在声明位置上位于函数之后，靠 `const` 的暂时性死区规则——实际运行没问题，因为 `exportResumePdf` 调用时模块已经求值完。

## 栏内切块：collectColumnSlices

`pdf.js:143-200`。左右两栏分别调用（`:62-76`），坐标全部相对栏顶（`rectOf`，`:146-149`）。

先取「板块」：

```js
const blocks = Array.from(columnEl.querySelectorAll('.hero, .section-block'))
  .map((el) => ({ el, ...rectOf(el) }))
  .filter((b) => b.bottom > 0 && b.top < contentHeight)
  .sort((a, b) => a.top - b.top)
```

`filter` 是防御 DOM 里存在零高节点，`sort` 保证按视觉顺序处理（不依赖 querySelectorAll 的文档顺序也行，但两栏容器是 flex 排布时 order 会影响视觉顺序，显式排序更稳）。

再取「卡」，七个选择器（`:156-157`）：

| 选择器 | 来源组件 |
|--------|----------|
| `.el-timeline-item` | `ExperienceSection.vue:29`（多条经历） |
| `.single-item` | `ExperienceSection.vue:8`（单条经历分支，不走时间轴） |
| `.edu-card` | `EducationSection.vue:5` |
| `.skill-group` | `SkillsSection.vue:5` |
| `.honor-card` | `HonorsSection.vue:8` |
| `.portfolio-card` | `PortfolioSection.vue:5` |
| `.contact-grid` | `ContactSection.vue:4` |

这七个类名就是「可分割单元」的白名单。板块内部结构改了、类名换了，分页会静默退化成整块处理，所以这份列表和组件模板是硬耦合。

三种情况：

1. **整块不拆**：`cards.length === 0`（评价板块、Hero）或者板块高度已经超过 `maxBlockHeight * 2`（`:166`）。后者是止损——一个高到两页都装不下的板块，拆标题+卡片只是自找麻烦，直接整体交下一页，再由装箱阶段硬切。
2. **标题单独成块**：`:173-177` 把 `.section-title` 的区域作为 `{ top: block.top, bottom: titleBottom, isTitle: true }` 收集，卡片从标题之后开始。这样标题不会带着整摞卡片一起换页，页底大片留白的概率显著下降。
3. **同行卡片合并**：`:180-187` 按 `Math.abs(card.top - row._rowTop) < 2` 判定同一行，把栅格里的并排卡片并成一个切片。技能分组、荣誉网格、作品网格都是多列布局，不合并的话一张卡换页、同排另一张留在上一页，视觉上像错位。2px 容差容忍不同卡片首行基线的细微差异。

最后一步是无缝化（`:190-199`）：所有切片按 top 排序后，把每个切片的 `bottom` 推到下一个切片的 `top`，最后一个推到栏底，第一个的 `top` 收敛到 0。这样切片之间不留空隙，贴到 PDF 上不会出现「内容被抹掉一条」。末尾 `filter((s) => s.bottom - s.top > 1)` 丢掉 1px 及以下的碎块。

两栏的水平取样区域在 `:62-76`：左栏 `wPx` 取到「右栏的左边缘」而不是只取左栏宽度，即把 18px 栏间距一并划进左栏像素。`:66` 的注释说明了原因——否则中缝会露出 PDF 白底。这里对 `leftEl` / `rightEl` 没有判空，两个类名由 `ResumeHome.vue:68` 和 `:92` 恒定提供，属于隐式契约：模板上把某个栏容器改成别的类名，导出会直接抛 `TypeError`。

## 装箱：paginateColumn

```js
const usableHeight = pageHeightPx - 8     // :210
const pages = [[]]
let cursor = 0
```

8px 是安全余量，`:208-209` 的解释：CSS px→mm→JPEG 像素的换算存在 1 到几 px 的舍入误差，正好贴到页底时末行卡片可能被裁一刀。

三种放置逻辑（`:214-257`）：

- **块高不超过可用页高，且当前页还装得下**：直接追加到当前页，条目记 `{ slice, y }`，`cursor` 累加。
- **块高不超过可用页高，但当前页剩余空间不够**：先另起一页再放。这里对标题块多一层判断（`:218-230`）——若是标题块、且「标题 + 下一张卡片」整体能塞进一整页、但当前页剩余放不下这两者，就整体换页，避免标题孤悬页底；普通块走 `:231-234` 的常规换页。
- **单切片高过整页**：内部硬切（`:241-256`），按 `usableHeight - cursor` 逐段切，记 `{ slice, y, chunkTop, chunkH }`。`cursor > 0` 时先另起一页，每切完一段若仍有剩余就再另起一页，保证同页不会有两段内容重叠。

绘制时 `item.chunkTop ?? item.slice.top`、`item.chunkH ?? item.slice.bottom - item.slice.top`（`:119-120`）——硬切条目用各自区间，普通条目用整块区间，走同一段代码。

## 出图

```js
const totalPages = Math.max(paginated[0].length, paginated[1].length)   // :108
for (let pageIdx = 0; pageIdx < totalPages; pageIdx++) {
  if (pageIdx > 0) pdf.addPage()
  columns.forEach((col, colIdx) => {
    const items = paginated[colIdx][pageIdx]
    if (!items) return                        // :115 该栏这页没内容就跳过
    ...
    pdf.addImage(drawSlice(col, topPx, topPx + heightPx), 'JPEG',
      PAGE_MARGIN_MM + col.xPx * mmPerPx,
      PAGE_MARGIN_MM + item.y * mmPerPx,
      col.wPx * mmPerPx, heightPx * mmPerPx)
  })
}
```

总页数取两栏较大值，所以左栏短、右栏长时，第二页之后左栏自然留空（`:114-115`）。第 1 页由 `new jsPDF('p', 'mm', 'a4')`（`:81`）自带，从第 2 页起 `addPage()`。

`drawSlice`（`:91-101`）在离屏 canvas 上做一次 `drawImage` 裁剪，再 `toDataURL('image/jpeg', 0.92)`。每个切片单独编码，意味着块越多、编码次数越多；一份内容较多的简历导出会明显卡一下，这属于方案的固定成本。选 JPEG 而不是 PNG：照片类内容体积差一个量级，且 PDF 里不需要透明通道。

## 已知边界

- 作品集和联系板块在导出态被 `display: none`（`theme.css:164-166`），PDF 永远不含这两块；作品封面图片同样被过滤，只保留头像。想要完整作品集只能给网页链接。
- 双栏装箱是各自独立的，左右两栏的「第 2 页」在视觉上不对齐是正常结果——同一页的左右内容在网页里可能相邻，装箱后属于不同页高度起点。
- 文件名固定为「个人简历.pdf」，多版本导出后本地文件会重名，靠浏览器自动加的 `(1)` 后缀区分；`exportResumePdf` 的第二个参数是留给版本名的接口，目前没有调用方使用（`ResumeHome.vue:231`）。
- 导出过程没有进度提示，只有按钮禁用态；截图 + 编码是主线程同步长任务，页面会短暂无响应。
- 一切依赖真实 DOM 测量：字体没加载完、图片被浏览器拦截、窗口宽度极窄导致 1080px 定宽仍生效，都可能改变结果。四道等待就是为这类时序问题兜底，不是为「图片加载」兜底。

## 可复用点

- **导出态用「真实 DOM + 一个根 class」，不用 `onclone`**：`pdf-exporting` 加在 `document.documentElement`（`pdf.js:31-32`），`finally` 无条件移除（`:77-79`）。好处是所有打印样式都写进正经的 CSS 层（`theme.css:141-198`、`ResumeHome.vue:768-796`），能在浏览器 DevTools 里直接给根元素挂类预览，不用维护一份只存在于内存副本里的克隆样式；异常路径不会留脏状态。代价是截图期间页面真的在重排，用户看得到一瞬版式跳动。用 `onclone` 的另一半代价是 `getComputedStyle` 的取值时机和真实 DOM 不一致，排查更难。
- **截图库的选型由样式里用到的颜色函数决定**：`color-mix(in srgb, …)` 用在卡片底色与边框（`theme.css:224-225`）和吸顶导航的半透明底（`ResumeHome.vue:366`），官方 html2canvas 1.x 直接抛错，所以依赖换成社区 fork（`package.json:17`）。换项目时先 grep 一遍 `color-mix(` / `oklch(` / `lab(`，命中就必须用支持现代颜色的分支；没命中就别多引一个 fork。同类"库能力被样式写法卡住"的还有 `backdrop-filter`——截图里不会还原模糊，导出态得靠纯色兜。
- **改动样式前先存住测量值**：`rootWidthPx` 在加了 `pdf-exporting` 的那一刻读进外层变量（`pdf.js:54-55`），后续所有换算只认这个数（`:87-88`）。类一移除页面就退回响应式宽度，再读 `getBoundingClientRect()` 拿到的是另一个值，页高、坐标、贴图的 scale 全部串位。凡是"截图 / 导出 / 打印之后还要用测量结果"的代码都适用同一条：测量与使用之间隔着重排，就必须快照，不能按需实时读。
- **长图分页的四步通用做法**：切片白名单（七个类名，`pdf.js:156-157`）→ 标题独立成块（`:173-177`）→ 同行卡片按 2px 容差合并（`:180-187`）→ 切片 `bottom` 推到下一个 `top` 无缝化（`:190-199`）。可复用的是后两步的思路：并排元素要么整行换页要么整行留在上一页，否则视觉上像错位；切片首尾相接才不会在 PDF 上出现"被抹掉一条"。装箱侧再留 `usableHeight - 8` 的安全余量吸收 px→mm→JPEG 的舍入误差（`:208-210`）。这份列表和组件类名是硬耦合，板块类名一改分页会静默退化成整块处理——加新板块时白名单必须同步。
- **等排版稳定的固定序列**：`document.fonts?.ready` → `scrollTo(0, 0)` → `setTimeout 60ms` → 双 `requestAnimationFrame` + 80ms（`pdf.js:26-28,39-41`）。双 rAF 的作用是在"本次样式变更后的布局已计算完"之后再往后走一步，单帧不够；中文字体异步加载，`fonts.ready` 少了就会截到行高变化中的中间态。同样的序列在第 06 篇的溢出测量里也出现（`PortfolioSection.vue:88-122` 的 `nextTick` / `fonts.ready` / resize 防抖），凡是"样式改完立刻量 DOM"的场景都建议直接抄这一串，不要凭感觉只等一个 `setTimeout`。
