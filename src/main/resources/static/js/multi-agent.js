// 多智能体对话应用状态
const state = {
    sessionId: generateSessionId(),
    messages: [],
    isLoading: false,
    theme: localStorage.getItem('theme') || 'light',
    reasoningSteps: [],
    currentAgent: null,
    sidebarOpen: false,
    reasoningPanelOpen: false
};

// DOM 元素缓存
const elements = {
    messagesContainer: document.getElementById('messagesContainer'),
    messageInput: document.getElementById('messageInput'),
    sendBtn: document.getElementById('sendBtn'),
    loadingOverlay: document.getElementById('loadingOverlay'),
    loadingText: document.getElementById('loadingText'),
    sessionId: document.getElementById('sessionId'),
    messageCount: document.getElementById('messageCount'),
    reasoningPanel: document.getElementById('reasoningPanel'),
    reasoningContent: document.getElementById('reasoningContent'),
    historyList: document.getElementById('historyList'),
    skillModal: document.getElementById('skillModal'),
    skillGrid: document.getElementById('skillGrid'),
    sidebar: document.querySelector('.sidebar')
};

// 技能配置
const skills = [
    { id: 'metric-analysis', name: '指标分析', icon: '📊', desc: '单指标、对比、趋势、排名查询' },
    { id: 'data-insight', name: '数据洞察', icon: '🔍', desc: '异常检测、关联分析、预测建议' },
    { id: 'data-retrieval', name: '数据检索', icon: '🗄️', desc: '动态SQL构建、参数化查询' },
    { id: 'dimension-parse', name: '维度解析', icon: '🎯', desc: '地区、时间、学历等维度识别' },
    { id: 'generation', name: '回答生成', icon: '✍️', desc: '自然语言回答、报告生成' },
    { id: 'supervisor', name: '任务协调', icon: '🎯', desc: '意图识别、Agent路由、任务编排' }
];

// Agent 配置
const agents = {
    supervisor: { name: 'Supervisor', role: '主协调者', color: 'supervisor' },
    query: { name: 'Query Agent', role: '数据查询', color: 'query' },
    insight: { name: 'Insight Agent', role: '洞察分析', color: 'insight' },
    report: { name: 'Report Agent', role: '报告生成', color: 'report' }
};

// 初始化
function init() {
    applyTheme();
    updateSessionDisplay();
    loadHistory();
    renderSkills();
    elements.messageInput.focus();
    
    // 检查后端状态
    checkHealth();
    
    // 加载保存的历史会话
    loadSavedSessions();
}

// 生成会话ID
function generateSessionId() {
    return 'sess_' + Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
}

// 应用主题
function applyTheme() {
    document.documentElement.setAttribute('data-theme', state.theme);
}

// 切换主题
function toggleTheme() {
    state.theme = state.theme === 'light' ? 'dark' : 'light';
    localStorage.setItem('theme', state.theme);
    applyTheme();
}

// 更新会话显示
function updateSessionDisplay() {
    elements.sessionId.textContent = state.sessionId.slice(-8).toUpperCase();
    elements.messageCount.textContent = `${state.messages.length} 条消息`;
}

// 切换侧边栏
function toggleSidebar() {
    state.sidebarOpen = !state.sidebarOpen;
    elements.sidebar.classList.toggle('open', state.sidebarOpen);
}

// 切换推理面板
function toggleReasoningPanel() {
    state.reasoningPanelOpen = !state.reasoningPanelOpen;
    elements.reasoningPanel.classList.toggle('hidden', !state.reasoningPanelOpen);
}

// 检查后端健康状态
async function checkHealth() {
    try {
        const response = await fetch('/api/health');
        const data = await response.json();
        console.log('Backend status:', data.status);
        updateAgentStatus('supervisor', 'idle');
    } catch (error) {
        console.error('Backend health check failed:', error);
        updateAgentStatus('supervisor', 'error');
    }
}

// 更新 Agent 状态
function updateAgentStatus(agentId, status) {
    const statusEl = document.getElementById(`${agentId}-status`);
    if (!statusEl) return;
    
    const badge = statusEl.querySelector('.status-badge');
    statusEl.classList.toggle('active', status === 'active');
    
    const statusMap = {
        idle: { text: '待机', class: 'idle' },
        active: { text: '运行中', class: 'active' },
        working: { text: '工作中', class: 'working' },
        error: { text: '异常', class: 'idle' }
    };
    
    const config = statusMap[status] || statusMap.idle;
    badge.textContent = config.text;
    badge.className = `status-badge ${config.class}`;
}

// 加载历史记录（占位）
function loadHistory() {
    // 可以在这里加载最近的历史会话列表
}

// 加载保存的会话
function loadSavedSessions() {
    const saved = localStorage.getItem('metric_analyst_sessions');
    if (saved) {
        const sessions = JSON.parse(saved);
        renderHistoryList(sessions.slice(-5));
    }
}

// 渲染历史列表
function renderHistoryList(sessions) {
    elements.historyList.innerHTML = sessions.map(session => `
        <div class="history-item" onclick="loadSession('${session.id}')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            <span class="history-title">${session.title || '新对话'}</span>
            <span class="history-time">${formatTime(session.time)}</span>
        </div>
    `).join('');
}

// 渲染技能列表
function renderSkills() {
    elements.skillGrid.innerHTML = skills.map(skill => `
        <div class="skill-card" onclick="useSkill('${skill.id}')">
            <span class="skill-card-icon">${skill.icon}</span>
            <span class="skill-card-name">${skill.name}</span>
            <span class="skill-card-desc">${skill.desc}</span>
        </div>
    `).join('');
}

// 显示技能选择器
function showSkillSelector() {
    elements.skillModal.classList.add('active');
}

// 隐藏技能选择器
function hideSkillSelector() {
    elements.skillModal.classList.remove('active');
}

// 使用技能
function useSkill(skillId) {
    const skill = skills.find(s => s.id === skillId);
    if (skill) {
        elements.messageInput.value = `使用${skill.name}：`;
        elements.messageInput.focus();
        hideSkillSelector();
        autoResize(elements.messageInput);
    }
}

// 发送消息
async function sendMessage() {
    const text = elements.messageInput.value.trim();
    if (!text || state.isLoading) return;

    // 添加用户消息
    addUserMessage(text);
    
    // 清空输入
    elements.messageInput.value = '';
    autoResize(elements.messageInput);
    
    // 设置加载状态
    setLoading(true, 'Supervisor 正在分析意图...');
    
    // 清空推理步骤
    state.reasoningSteps = [];
    renderReasoningSteps();
    
    // 更新 Agent 状态
    updateAgentStatus('supervisor', 'active');
    
    try {
        // 模拟多智能体协作过程
        await simulateMultiAgentProcess(text);
        
        // 调用实际 API
        const response = await fetch(`/api/chat?input=${encodeURIComponent(text)}&sessionId=${state.sessionId}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        
        // 添加 AI 回复
        addAgentMessage(data);
        
        // 保存会话
        saveSession(text);
        
    } catch (error) {
        console.error('Send message error:', error);
        addErrorMessage('抱歉，服务暂时不可用，请稍后重试。');
    } finally {
        setLoading(false);
        updateAgentStatus('supervisor', 'idle');
        updateAgentStatus('query', 'idle');
        updateAgentStatus('insight', 'idle');
        updateAgentStatus('report', 'idle');
        updateSessionDisplay();
    }
}

// 模拟多智能体协作过程
async function simulateMultiAgentProcess(text) {
    // 步骤 1: Supervisor 意图识别
    await delay(500);
    addReasoningStep({
        agent: 'supervisor',
        type: 'thinking',
        title: '意图识别',
        content: `分析用户输入："${text}"`,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    });
    
    // 步骤 2: 确定路由
    await delay(600);
    const route = determineRoute(text);
    addReasoningStep({
        agent: 'supervisor',
        type: 'action',
        title: 'Agent 路由',
        content: `路由到 ${agents[route].name} 处理`,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    });
    
    updateAgentStatus('supervisor', 'idle');
    updateAgentStatus(route, 'active');
    
    // 步骤 3: 子 Agent 处理
    await delay(800);
    addReasoningStep({
        agent: route,
        type: 'thinking',
        title: '任务分解',
        content: '将查询分解为可执行步骤',
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    });
    
    // 步骤 4: 工具调用（如果需要）
    if (route === 'query') {
        await delay(700);
        addReasoningStep({
            agent: route,
            type: 'action',
            title: '工具调用',
            content: '调用 `queryMetricCurrentValue` 工具',
            time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
        });
        
        await delay(500);
        addReasoningStep({
            agent: route,
            type: 'observation',
            title: '数据获取',
            content: '成功获取指标数据',
            time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
        });
    }
    
    updateAgentStatus(route, 'idle');
}

// 确定路由
function determineRoute(text) {
    const lower = text.toLowerCase();
    if (lower.includes('分析') || lower.includes('为什么') || lower.includes('趋势')) {
        return 'insight';
    }
    if (lower.includes('报告') || lower.includes('导出')) {
        return 'report';
    }
    return 'query';
}

// 延迟函数
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// 添加推理步骤
function addReasoningStep(step) {
    state.reasoningSteps.push(step);
    renderReasoningSteps();
}

// 渲染推理步骤
function renderReasoningSteps() {
    if (state.reasoningSteps.length === 0) {
        elements.reasoningContent.innerHTML = `
            <div class="reasoning-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
                </svg>
                <p>发送消息后，这里将显示智能体的推理过程</p>
            </div>
        `;
        return;
    }
    
    elements.reasoningContent.innerHTML = state.reasoningSteps.map(step => `
        <div class="reasoning-item">
            <div class="reasoning-item-header">
                <div class="reasoning-item-icon ${step.type}">
                    ${getStepIcon(step.type)}
                </div>
                <span class="reasoning-item-title">${step.title}</span>
                <span class="reasoning-item-time">${step.time}</span>
            </div>
            <div class="reasoning-item-content">${step.content}</div>
        </div>
    `).join('');
    
    // 滚动到底部
    elements.reasoningContent.scrollTop = elements.reasoningContent.scrollHeight;
}

// 获取步骤图标
function getStepIcon(type) {
    const icons = {
        thinking: '💭',
        action: '⚡',
        observation: '👁️'
    };
    return icons[type] || '•';
}

// 添加用户消息
function addUserMessage(text) {
    const message = {
        id: Date.now(),
        type: 'user',
        content: text,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    };
    
    state.messages.push(message);
    renderMessage(message);
    updateSessionDisplay();
}

// 添加 Agent 消息
function addAgentMessage(data) {
    const message = {
        id: Date.now(),
        type: 'assistant',
        agent: data.agent || 'supervisor',
        content: data.content,
        rawData: data,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    };
    
    state.messages.push(message);
    renderMessage(message);
    updateSessionDisplay();
}

// 添加错误消息
function addErrorMessage(text) {
    const message = {
        id: Date.now(),
        type: 'error',
        content: text,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    };
    
    state.messages.push(message);
    renderMessage(message);
}

// 渲染消息
function renderMessage(message) {
    const messageEl = document.createElement('div');
    messageEl.className = 'message-group';
    messageEl.id = `msg-${message.id}`;
    
    if (message.type === 'user') {
        messageEl.innerHTML = `
            <div class="message user">
                <div class="message-avatar">
                    <div class="user-avatar">我</div>
                </div>
                <div class="message-content">
                    <div class="message-header">
                        <span class="message-author">我</span>
                        <span class="message-time">${message.time}</span>
                    </div>
                    <div class="message-body">${escapeHtml(message.content)}</div>
                </div>
            </div>
        `;
    } else if (message.type === 'assistant') {
        const agent = agents[message.agent] || agents.supervisor;
        messageEl.innerHTML = `
            <div class="message">
                <div class="message-avatar">
                    <div class="agent-avatar ${agent.color}">
                        <span>${agent.name.charAt(0)}</span>
                    </div>
                </div>
                <div class="message-content">
                    <div class="message-header">
                        <span class="message-author">${agent.name}</span>
                        <span class="message-badge ${message.agent}">${agent.role}</span>
                        <span class="message-time">${message.time}</span>
                    </div>
                    <div class="message-body">${formatContent(message.content)}</div>
                </div>
            </div>
        `;
    } else if (message.type === 'error') {
        messageEl.innerHTML = `
            <div class="message">
                <div class="message-avatar">
                    <div class="agent-avatar" style="background: #ef4444;">
                        <span>!</span>
                    </div>
                </div>
                <div class="message-content">
                    <div class="message-header">
                        <span class="message-author">系统</span>
                        <span class="message-time">${message.time}</span>
                    </div>
                    <div class="message-body" style="background: #fef2f2; border-color: #fecaca; color: #991b1b;">
                        ${escapeHtml(message.content)}
                    </div>
                </div>
            </div>
        `;
    }
    
    elements.messagesContainer.appendChild(messageEl);
    scrollToBottom();
}

// 格式化内容
function formatContent(content) {
    if (!content) return '';
    
    // 转换换行符
    let formatted = escapeHtml(content).replace(/\n/g, '<br>');
    
    // 转换代码块
    formatted = formatted.replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
    
    // 转换行内代码
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // 转换粗体
    formatted = formatted.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    
    return formatted;
}

// HTML 转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 滚动到底部
function scrollToBottom() {
    elements.messagesContainer.scrollTop = elements.messagesContainer.scrollHeight;
}

// 设置加载状态
function setLoading(loading, text = '正在处理...') {
    state.isLoading = loading;
    elements.sendBtn.disabled = loading;
    elements.loadingText.textContent = text;
    
    if (loading) {
        elements.loadingOverlay.classList.add('active');
    } else {
        elements.loadingOverlay.classList.remove('active');
    }
}

// 处理键盘事件
function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// 自动调整输入框高度
function autoResize(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
}

// 发送示例
function sendExample(text) {
    elements.messageInput.value = text;
    autoResize(elements.messageInput);
    sendMessage();
}

// 保存会话
function saveSession(firstMessage) {
    const saved = localStorage.getItem('metric_analyst_sessions');
    let sessions = saved ? JSON.parse(saved) : [];
    
    const existing = sessions.find(s => s.id === state.sessionId);
    if (!existing) {
        sessions.push({
            id: state.sessionId,
            title: firstMessage.slice(0, 20) + (firstMessage.length > 20 ? '...' : ''),
            time: Date.now()
        });
        
        // 只保留最近 20 个会话
        sessions = sessions.slice(-20);
        localStorage.setItem('metric_analyst_sessions', JSON.stringify(sessions));
        renderHistoryList(sessions);
    }
}

// 加载会话
function loadSession(sessionId) {
    // 可以在这里加载特定会话的历史消息
    console.log('Loading session:', sessionId);
}

// 开始新对话
function startNewChat() {
    // 保存当前会话
    if (state.messages.length > 0) {
        saveSession(state.messages[0]?.content || '新对话');
    }
    
    // 重置状态
    state.sessionId = generateSessionId();
    state.messages = [];
    state.reasoningSteps = [];
    
    // 清空界面
    elements.messagesContainer.innerHTML = '';
    renderReasoningSteps();
    updateSessionDisplay();
    
    // 重新加载欢迎消息
    location.reload();
}

// 清空对话
function clearChat() {
    if (confirm('确定要清空当前对话吗？')) {
        elements.messagesContainer.innerHTML = '';
        state.messages = [];
        updateSessionDisplay();
        
        // 重新加载欢迎消息
        location.reload();
    }
}

// 格式化时间
function formatTime(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
    
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
}

// 点击外部关闭侧边栏
document.addEventListener('click', (e) => {
    if (state.sidebarOpen && !elements.sidebar.contains(e.target) && !e.target.closest('.menu-btn')) {
        state.sidebarOpen = false;
        elements.sidebar.classList.remove('open');
    }
});

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', init);

// 导出全局函数
window.sendMessage = sendMessage;
window.handleKeyDown = handleKeyDown;
window.autoResize = autoResize;
window.sendExample = sendExample;
window.startNewChat = startNewChat;
window.clearChat = clearChat;
window.toggleTheme = toggleTheme;
window.toggleSidebar = toggleSidebar;
window.toggleReasoningPanel = toggleReasoningPanel;
window.showSkillSelector = showSkillSelector;
window.hideSkillSelector = hideSkillSelector;
window.useSkill = useSkill;
window.loadSession = loadSession;
