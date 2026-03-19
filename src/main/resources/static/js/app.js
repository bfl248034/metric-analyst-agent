// 应用状态
const state = {
    sessionId: generateSessionId(),
    messages: [],
    isLoading: false,
    theme: localStorage.getItem('theme') || 'light'
};

// DOM 元素
const elements = {
    messagesContainer: document.getElementById('messagesContainer'),
    messageInput: document.getElementById('messageInput'),
    sendBtn: document.getElementById('sendBtn'),
    loadingOverlay: document.getElementById('loadingOverlay'),
    sessionId: document.getElementById('sessionId')
};

// 初始化
function init() {
    applyTheme();
    elements.sessionId.textContent = `Session: ${state.sessionId}`;
    elements.messageInput.focus();
    
    // 检查后端状态
    checkHealth();
}

// 生成会话ID
function generateSessionId() {
    return 'session_' + Date.now().toString(36) + Math.random().toString(36).substr(2);
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

// 检查后端健康状态
async function checkHealth() {
    try {
        const response = await fetch('/api/health');
        const data = await response.json();
        console.log('Backend status:', data.status);
    } catch (error) {
        console.error('Backend health check failed:', error);
        showError('无法连接到后端服务，请检查服务是否运行');
    }
}

// 发送消息
async function sendMessage() {
    const text = elements.messageInput.value.trim();
    if (!text || state.isLoading) return;

    // 添加用户消息
    addMessage({
        type: 'user',
        content: text,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    });

    // 清空输入
    elements.messageInput.value = '';
    autoResize(elements.messageInput);

    // 显示加载状态
    setLoading(true);
    showTypingIndicator();

    try {
        // 调用后端API
        const response = await fetch(`/api/chat?input=${encodeURIComponent(text)}&sessionId=${state.sessionId}`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        
        // 移除打字指示器
        removeTypingIndicator();
        
        // 添加AI回复
        addMessage({
            type: 'assistant',
            content: formatResponse(data.content, data.type),
            time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
            rawData: data
        });

    } catch (error) {
        removeTypingIndicator();
        console.error('Send message error:', error);
        
        addMessage({
            type: 'assistant',
            content: '抱歉，服务暂时不可用，请稍后重试。',
            time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
            isError: true
        });
    } finally {
        setLoading(false);
    }
}

// 格式化响应内容
function formatResponse(content, type) {
    // 如果是数据响应，可以添加特殊格式
    if (type === 'DATA') {
        return content;
    }
    
    // 转换换行符为HTML
    return content.replace(/\n/g, '<br>');
}

// 添加消息到界面
function addMessage(message) {
    const messageEl = document.createElement('div');
    messageEl.className = `message ${message.type}`;
    
    const avatar = message.type === 'user' ? '👤' : '🤖';
    const author = message.type === 'user' ? '我' : 'Metric Analyst';
    
    messageEl.innerHTML = `
        <div class="message-avatar">
            <span>${avatar}</span>
        </div>
        <div class="message-content">
            <div class="message-header">
                <span class="message-author">${author}</span>
                <span class="message-time">${message.time}</span>
            </div>
            <div class="message-body ${message.isError ? 'error' : ''}">
                ${message.content}
            </div>
        </div>
    `;
    
    elements.messagesContainer.appendChild(messageEl);
    scrollToBottom();
    
    // 保存到状态
    state.messages.push(message);
}

// 显示打字指示器
function showTypingIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'message assistant typing';
    indicator.id = 'typingIndicator';
    
    indicator.innerHTML = `
        <div class="message-avatar">
            <span>🤖</span>
        </div>
        <div class="message-content">
            <div class="message-header">
                <span class="message-author">Metric Analyst</span>
                <span class="message-time">正在输入...</span>
            </div>
            <div class="message-body">
                <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>
    `;
    
    elements.messagesContainer.appendChild(indicator);
    scrollToBottom();
}

// 移除打字指示器
function removeTypingIndicator() {
    const indicator = document.getElementById('typingIndicator');
    if (indicator) {
        indicator.remove();
    }
}

// 设置加载状态
function setLoading(loading) {
    state.isLoading = loading;
    elements.sendBtn.disabled = loading;
    
    if (loading) {
        elements.loadingOverlay.classList.add('active');
    } else {
        elements.loadingOverlay.classList.remove('active');
    }
}

// 滚动到底部
function scrollToBottom() {
    elements.messagesContainer.scrollTop = elements.messagesContainer.scrollHeight;
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

// 发送示例问题
function sendExample(text) {
    elements.messageInput.value = text;
    autoResize(elements.messageInput);
    sendMessage();
}

// 开始新对话
function startNewChat() {
    // 生成新会话ID
    state.sessionId = generateSessionId();
    state.messages = [];
    
    // 清空消息区域
    elements.messagesContainer.innerHTML = '';
    elements.sessionId.textContent = `Session: ${state.sessionId}`;
    
    // 重新显示欢迎消息
    location.reload();
}

// 清空对话
function clearChat() {
    if (confirm('确定要清空当前对话吗？')) {
        elements.messagesContainer.innerHTML = '';
        state.messages = [];
        
        // 重新显示欢迎消息
        location.reload();
    }
}

// 显示错误提示
function showError(message) {
    const errorEl = document.createElement('div');
    errorEl.className = 'error-toast';
    errorEl.textContent = message;
    errorEl.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #ef4444;
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(errorEl);
    
    setTimeout(() => {
        errorEl.remove();
    }, 5000);
}

// 添加CSS动画
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    .message-body.error {
        background: #fef2f2 !important;
        border: 1px solid #fecaca;
        color: #991b1b !important;
    }
`;
document.head.appendChild(style);

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', init);

// 导出函数供HTML调用
window.sendMessage = sendMessage;
window.handleKeyDown = handleKeyDown;
window.autoResize = autoResize;
window.sendExample = sendExample;
window.startNewChat = startNewChat;
window.clearChat = clearChat;
window.toggleTheme = toggleTheme;
