// 使用 html2canvas-pro：官方 html2canvas 1.x 不支持 color-mix()/oklch() 等现代
// CSS 颜色函数（Element Plus 及本项目主题大量使用），会直接抛错导致导出失败
import html2canvas from 'html2canvas-pro'
import { jsPDF } from 'jspdf'

/**
 * 将简历主体导出为 A4 多页 PDF（左右双栏排版）
 *
 * 布局：页面保持网页的左右双栏结构——
 *   左栏：头像信息 / 技能 / 教育 / 联系；右栏：个人评价 / 工作 / 项目 / 荣誉 / 作品
 *
 * 分页策略（避免卡片被页边界拦腰截断）：
 * 1. 整个简历在导出态（双栏）下截成一张长图；
 * 2. 左右两栏各自收集“不可分割单元”：普通板块整体一块，
 *    超高板块拆为「板块标题 + 内部卡片行」（网格同行卡片合并为一行）；
 * 3. 两栏各自独立按页装箱，放不下整块就整体移到下一页；
 * 4. 总页数取两栏最大值，逐页把左右两栏对应页的切片画到同一页。
 *
 * @param {HTMLElement} element 要导出的简历容器（#resume-content）
 * @param {string} fileName 下载文件名（无需 .pdf）
 */
export async function exportResumePdf(element, fileName = '个人简历') {
  if (!element) return

  // 等待字体/图片渲染，并回到页顶，保证布局测量准确
  await document.fonts?.ready
  window.scrollTo(0, 0)
  await new Promise((resolve) => setTimeout(resolve, 60))

  // 导出期间：强制显示滚动入场元素、双栏排版、隐藏页脚（按钮/版权不导出）
  const root = document.documentElement
  root.classList.add('pdf-exporting')

  let canvas
  let columns
  let rootWidthPx
  try {
    // 等待两帧 + 短延时，确保布局重排完成
    await new Promise((resolve) =>
      requestAnimationFrame(() => requestAnimationFrame(() => setTimeout(resolve, 80)))
    )

    const bgColor = getComputedStyle(document.body).backgroundColor || '#f5f7fb'

    canvas = await html2canvas(element, {
      useCORS: true,
      allowTaint: false,
      backgroundColor: bgColor,
      scale: 2,
      scrollY: 0,
      windowWidth: element.scrollWidth
    })

    const rootRect = element.getBoundingClientRect()
    rootWidthPx = rootRect.width
    const leftEl = element.querySelector('.side-column')
    const rightEl = element.querySelector('.content-column')
    // A4 内容区对应的 CSS px 高度（截图满宽映射 A4 内容宽）
    const pageHeightPx =
      ((297 - 2 * PAGE_MARGIN_MM) * rootWidthPx) / (210 - 2 * PAGE_MARGIN_MM)

    columns = [
      {
        el: leftEl,
        xPx: leftEl.getBoundingClientRect().left - rootRect.left,
        // 左栏切片宽度延伸到整个栏间距，避免中缝露出 PDF 白底
        wPx: rightEl.getBoundingClientRect().left - rootRect.left,
        slices: collectColumnSlices(leftEl, pageHeightPx)
      },
      {
        el: rightEl,
        xPx: rightEl.getBoundingClientRect().left - rootRect.left,
        wPx: rightEl.getBoundingClientRect().width,
        slices: collectColumnSlices(rightEl, pageHeightPx)
      }
    ]
  } finally {
    root.classList.remove('pdf-exporting')
  }

  const pdf = new jsPDF('p', 'mm', 'a4')
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()

  // 注意：rootWidth 必须使用导出态（1080px 固定宽）下测量的值，
  // 此时 pdf-exporting 类已移除、页面恢复网页态，不能再读 element 实时宽度
  const scale = canvas.width / rootWidthPx
  const mmPerPx = (pageWidth - 2 * PAGE_MARGIN_MM) / rootWidthPx

  /** 从长图裁出栏内 [top, bottom) 区域（CSS px，坐标相对栏顶）并编码 JPEG */
  const drawSlice = (col, top, bottom) => {
    const sx = Math.round(col.xPx * scale)
    const sy = Math.round(top * scale)
    const sw = Math.max(1, Math.round(col.wPx * scale))
    const sh = Math.max(1, Math.round((bottom - top) * scale))
    const sliceCanvas = document.createElement('canvas')
    sliceCanvas.width = sw
    sliceCanvas.height = sh
    sliceCanvas.getContext('2d').drawImage(canvas, sx, sy, sw, sh, 0, 0, sw, sh)
    return sliceCanvas.toDataURL('image/jpeg', 0.92)
  }

  // 每栏各自按页装箱
  const pageHeightMm = pageHeight - 2 * PAGE_MARGIN_MM
  const paginated = columns.map((col) =>
    paginateColumn(col.slices, pageHeightMm / mmPerPx)
  )
  const totalPages = Math.max(paginated[0].length, paginated[1].length)

  for (let pageIdx = 0; pageIdx < totalPages; pageIdx++) {
    if (pageIdx > 0) pdf.addPage()

    columns.forEach((col, colIdx) => {
      const items = paginated[colIdx][pageIdx]
      if (!items) return
      const xMm = PAGE_MARGIN_MM + col.xPx * mmPerPx
      const wMm = col.wPx * mmPerPx
      for (const item of items) {
        const topPx = item.chunkTop ?? item.slice.top
        const heightPx = item.chunkH ?? item.slice.bottom - item.slice.top
        pdf.addImage(
          drawSlice(col, topPx, topPx + heightPx),
          'JPEG',
          xMm,
          PAGE_MARGIN_MM + item.y * mmPerPx,
          wMm,
          heightPx * mmPerPx
        )
      }
    })
  }

  pdf.save(`${fileName}.pdf`)
}

const PAGE_MARGIN_MM = 7

/**
 * 收集单个栏内的不可分割切片（坐标相对栏顶，单位 CSS px）
 * @param {HTMLElement} columnEl 栏容器
 * @param {number} maxBlockHeight 单元最大高度（一页内容区对应 CSS px）
 */
function collectColumnSlices(columnEl, maxBlockHeight) {
  const columnTop = columnEl.getBoundingClientRect().top
  const contentHeight = columnEl.getBoundingClientRect().height
  const rectOf = (el) => {
    const r = el.getBoundingClientRect()
    return { top: r.top - columnTop, bottom: r.bottom - columnTop }
  }

  const blocks = Array.from(columnEl.querySelectorAll('.hero, .section-block'))
    .map((el) => ({ el, ...rectOf(el) }))
    .filter((b) => b.bottom > 0 && b.top < contentHeight)
    .sort((a, b) => a.top - b.top)

  const CARD_SELECTOR =
    '.el-timeline-item, .single-item, .edu-card, .skill-group, .honor-card, .portfolio-card, .contact-grid'

  const slices = []
  for (const block of blocks) {
    const cards = Array.from(block.el.querySelectorAll(CARD_SELECTOR))
      .map((el) => rectOf(el))
      .sort((a, b) => a.top - b.top)

    // Hero 或内部没有可拆卡片的板块（如个人评价）：整体一块
    if (cards.length === 0 || block.bottom - block.top > maxBlockHeight * 2) {
      slices.push({ top: block.top, bottom: block.bottom })
      continue
    }

    // 常规板块：拆为「板块标题 + 内部卡片行」，让标题与卡片均可流式分页，
    // 减少整板换页造成的页底大片留白（网格同行卡片合并为一行）
    const title = block.el.querySelector('.section-title')
    if (title) {
      const t = rectOf(title)
      slices.push({ top: block.top, bottom: t.bottom, isTitle: true })
    }

    // 按 top 聚类成行（容差 2px），合并网格中并列的卡片
    for (const card of cards) {
      const row = slices[slices.length - 1]
      if (row && !row.isTitle && Math.abs(card.top - row._rowTop) < 2) {
        row.bottom = Math.max(row.bottom, card.bottom)
      } else {
        slices.push({ top: card.top, bottom: card.bottom, _rowTop: card.top })
      }
    }
  }

  // 把切片之间的留白并入前一个切片，保证拼接无缝、不遗漏
  slices.sort((a, b) => a.top - b.top)
  for (let i = 0; i < slices.length; i++) {
    if (i === 0) slices[i].top = Math.min(slices[i].top, 0)
    const next = slices[i + 1]
    if (next && slices[i].bottom < next.top) slices[i].bottom = next.top
    if (!next) slices[i].bottom = contentHeight
    delete slices[i]._rowTop
  }
  return slices.filter((s) => s.bottom - s.top > 1)
}

/**
 * 一栏的切片按页装箱（坐标单位 CSS px）
 * 切片整体放不下当前页时整体换页；单切片自身高于整页时才在内部硬切
 * @returns {Array<Array<{slice,y,chunkTop?,chunkH?}>>} 每页要绘制的条目
 */
function paginateColumn(slices, pageHeightPx) {
  // 安全余量：CSS px→mm→JPEG 像素对齐存在 1~数 px 误差，
  // 临界高度装入页底会导致末行卡片被裁切
  const usableHeight = pageHeightPx - 8
  const pages = [[]]
  let cursor = 0

  for (let i = 0; i < slices.length; i++) {
    const slice = slices[i]
    const height = slice.bottom - slice.top

    if (height <= usableHeight) {
      // 板块标题不孤悬页底：放不下“标题 + 下一张卡片”时整体移到下一页
      const next = slices[i + 1]
      const nextH = next ? next.bottom - next.top : 0
      if (
        slice.isTitle &&
        next &&
        cursor > 0 &&
        cursor + height + nextH > usableHeight &&
        height + nextH <= usableHeight
      ) {
        pages.push([])
        cursor = 0
      } else if (cursor > 0 && cursor + height > usableHeight) {
        pages.push([])
        cursor = 0
      }
      pages[pages.length - 1].push({ slice, y: cursor })
      cursor += height
      continue
    }

    // 保底：单切片高于整页，内部按页硬切
    let remaining = height
    let chunkTop = slice.top
    while (remaining > 0) {
      if (cursor > 0) {
        pages.push([])
        cursor = 0
      }
      const chunkH = Math.min(remaining, usableHeight - cursor)
      pages[pages.length - 1].push({ slice, y: cursor, chunkTop, chunkH })
      chunkTop += chunkH
      remaining -= chunkH
      cursor += chunkH
      if (remaining > 0) {
        pages.push([])
        cursor = 0
      }
    }
  }

  return pages
}
