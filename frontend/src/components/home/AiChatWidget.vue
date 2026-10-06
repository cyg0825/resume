<template>
  <div class="ai-chat-widget">
    <transition name="panel-slide">
      <div v-if="open" class="chat-panel">
        <div class="chat-header">
          <div class="header-left">
            <el-icon :size="20"><ChatLineRound /></el-icon>
            <div>
              <div class="chat-title">简历 AI 助手</div>
              <div class="chat-subtitle">基于简历内容为你解答</div>
            </div>
          </div>
          <el-icon class="close-btn" @click="closePanel"><Close /></el-icon>
        </div>

        <div ref="bodyRef" class="chat-body">
          <div v-if="!messages.length" class="welcome">
            <p>你好！我是这份简历的 AI 助手，可以向我询问<span>教育背景、实习经历、项目经验、技能特长、荣誉证书、联系方式</span>等，例如：</p>
            <div class="quick-list">
              <span v-for="q in quickQuestions" :key="q" class="quick-item" @click="askQuick(q)">
                <el-icon class="q-icon"><ChatDotRound /></el-icon>{{ q }}
              </span>
            </div>
          </div>

          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="msg-row"
            :class="msg.role"
          >
            <div v-if="msg.role === 'assistant'" class="msg-avatar avatar-ai">
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div class="bubble">
              <span v-if="msg.source === 'llm' || msg.source === 'local'" class="source-tag">
                {{ msg.source === 'llm' ? 'AI 生成' : '简历问答' }}
              </span>
              <div class="bubble-text">
                <template v-for="(line, li) in renderLines(msg.content)" :key="li">
                  <div v-if="line.type === 'heading'" class="line-heading">{{ line.text }}</div>
                  <div v-else-if="line.type === 'item'" class="line-item">
                    <span class="line-dot"></span><span>{{ line.text }}</span>
                  </div>
                  <p v-else class="line-text">{{ line.text }}</p>
                </template>
              </div>
            </div>
            <div v-if="msg.role === 'user'" class="msg-avatar avatar-user">
              <el-icon><User /></el-icon>
            </div>
          </div>

          <div v-if="loading" class="msg-row assistant">
            <div class="msg-avatar avatar-ai">
              <el-icon><ChatLineRound /></el-icon>
            </div>
            <div class="bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="input"
            placeholder="输入你的问题，回车发送"
            clearable
            :disabled="loading"
            @keydown.enter="onEnterKey"
          />
          <el-button type="primary" :loading="loading" @click="send">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </transition>

    <div
      v-if="!open"
      class="chat-fab"
      role="button"
      tabindex="0"
      aria-label="打开 AI 问答"
      title="AI 问答"
      @click="open = true"
      @keyup.enter="open = true"
    >
      <el-icon :size="26"><ChatLineRound /></el-icon>
      <span class="fab-pulse"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { aiChat } from '@/api'

const props = defineProps({
  versionId: { type: [Number, String], default: null }
})

// 首次访问默认展开聊天框；访客手动关闭一次后记住，之后不再自动弹出
const AUTO_OPEN_KEY = 'resume-ai-auto-opened'
const open = ref(localStorage.getItem(AUTO_OPEN_KEY) !== '1')

function closePanel() {
  open.value = false
  localStorage.setItem(AUTO_OPEN_KEY, '1')
}

const loading = ref(false)
const input = ref('')
const bodyRef = ref()
const messages = reactive([])

const quickQuestions = [
  '掌握哪些技能？',
  '实习期间主要做什么工作？',
  '拿过哪些荣誉证书？',
  '什么时候毕业？怎么联系？'
]

// 会话 ID 持久化，用于服务端串联多轮对话
const getSessionId = () => {
  let sid = localStorage.getItem('resume-ai-sid')
  if (!sid) {
    sid = `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem('resume-ai-sid', sid)
  }
  return sid
}

// 将回答按行结构化为 标题 / 列表项 / 段落，避免大段文字难读
function renderLines(content) {
  return String(content || '')
    .split('\n')
    .map((raw) => {
      const text = raw.trim()
      if (!text) return null
      const heading = text.match(/^【(.+?)】\s*$/)
      if (heading) return { type: 'heading', text: heading[1] }
      const item = text.match(/^[-•·*]\s*(.+)$/)
      if (item) return { type: 'item', text: item[1] }
      return { type: 'text', text }
    })
    .filter(Boolean)
}

// 中文输入法选词时按回车不应发送
function onEnterKey(e) {
  if (e.isComposing || e.keyCode === 229) return
  send()
}

function askQuick(q) {
  input.value = q
  send()
}

async function send() {
  const question = input.value.trim()
  if (!question || loading.value) return

  messages.push({ role: 'user', content: question })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const res = await aiChat({
      sessionId: getSessionId(),
      question,
      versionId: props.versionId || null
    })
    messages.push({
      role: 'assistant',
      content: res.data.answer,
      source: res.data.source
    })
  } catch (e) {
    messages.push({
      role: 'assistant',
      content: '抱歉，问答服务暂时不可用，请稍后再试。',
      source: 'error'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}
</script>

<style scoped>
.ai-chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
}

.chat-fab {
  position: relative;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.25);
  transition: transform 0.25s ease;
}

.chat-fab:hover {
  transform: scale(1.08);
}

.fab-pulse {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: var(--color-primary);
  opacity: 0.35;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.35; }
  70% { transform: scale(1.5); opacity: 0; }
  100% { transform: scale(1.5); opacity: 0; }
}

.chat-panel {
  position: absolute;
  right: 0;
  bottom: 72px;
  width: 390px;
  max-width: calc(100vw - 32px);
  height: 580px;
  max-height: 72vh;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 小屏：面板相对悬浮球左移，使左右边距对称（各约 16px） */
@media (max-width: 480px) {
  .chat-panel {
    right: -8px;
    max-width: calc(100vw - 32px);
  }
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.28s ease;
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.96);
}

.chat-header {
  background: var(--hero-gradient);
  color: #fff;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-title {
  font-size: 15px;
  font-weight: 700;
}

.chat-subtitle {
  font-size: 11px;
  opacity: 0.85;
}

.close-btn {
  cursor: pointer;
  font-size: 18px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--color-bg);
}

.welcome {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 15px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text-secondary);
}

.welcome p {
  margin: 0 0 12px;
}

.welcome span {
  color: var(--color-primary);
  font-weight: 600;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 12px;
  border-radius: 9px;
  background: var(--tag-bg);
  color: var(--tag-text);
  font-size: 12.5px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.quick-item:hover {
  transform: translateX(3px);
  box-shadow: var(--shadow-card);
}

.q-icon {
  flex-shrink: 0;
  font-size: 13px;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 14px;
}

.msg-row.user {
  flex-direction: row;
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
}

.avatar-ai {
  background: linear-gradient(135deg, var(--color-primary-dark), var(--color-primary-light));
}

.avatar-user {
  background: linear-gradient(135deg, #64748b, #94a3b8);
}

.bubble {
  position: relative;
  max-width: calc(100% - 46px);
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.75;
  word-break: break-word;
}

.msg-row.user .bubble {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-row.assistant .bubble {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: 4px;
}

.source-tag {
  display: inline-block;
  font-size: 10px;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--tag-bg);
  color: var(--tag-text);
  margin-bottom: 6px;
}

.bubble-text {
  white-space: normal;
}

.line-heading {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-primary);
  margin: 6px 0 4px;
}

.line-heading:first-child {
  margin-top: 0;
}

.line-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 3px 0;
}

.line-dot {
  flex-shrink: 0;
  width: 5px;
  height: 5px;
  margin-top: 9px;
  border-radius: 50%;
  background: var(--color-primary);
}

.msg-row.user .line-dot {
  background: rgba(255, 255, 255, 0.8);
}

.line-text {
  margin: 4px 0;
}

.line-text:first-child {
  margin-top: 0;
}

.line-text:last-child {
  margin-bottom: 0;
}

.typing {
  display: flex;
  gap: 5px;
  align-items: center;
}

.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-text-secondary);
  animation: blink 1.2s infinite;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 60%, 100% { opacity: 0.25; }
  30% { opacity: 1; }
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}
</style>
