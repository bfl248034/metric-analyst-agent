package com.metric.analyst.agent.hooks;

import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 闲聊识别 Hook
 * 
 * 功能：
 * 1. 识别用户闲聊意图（打招呼、问身份、无关话题）
 * 2. 快速生成友好回复，不调用大模型（节省 Token）
 * 3. 引导用户回到数据查询主题
 * 
 * 基于 Spring AI Alibaba MessagesModelHook 实现
 */
@Slf4j
@Component
public class ChitchatHook extends MessagesModelHook {

    // 打招呼关键词
    private static final Set<String> GREETINGS = Set.of(
        "你好", "您好", "嗨", "哈喽", "hello", "hi",
        "在吗", "在吗？", "有人吗", "早上好", "下午好", "晚上好"
    );

    // 询问身份关键词
    private static final Set<String> IDENTITY_QUERIES = Set.of(
        "你是谁", "你是什么", "你叫什么名字", "你能做什么",
        "你是干嘛的", "介绍一下自己", "你有什么用", "你的功能"
    );

    // 无关问题关键词
    private static final Set<String> OFF_TOPIC = Set.of(
        "讲个笑话", "天气怎么样", "今天几号", "帮我写代码",
        "1+1等于几", "你会唱歌吗", "推荐一部电影"
    );

    @Override
    public String getName() {
        return "chitchat_detector";
    }

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        // 提取最后一条用户消息
        String userInput = extractLastUserInput(messages);
        if (userInput == null || userInput.isEmpty()) {
            return new AgentCommand(messages);
        }

        // 闲聊检测
        ChitchatType type = detectChitchat(userInput);
        if (type != null) {
            String reply = generateReply(type);
            log.info("Chitchat detected ({}), returning direct reply", type);
            
            // 直接返回回复，跳过模型调用
            List<Message> response = List.of(new AssistantMessage(reply));
            return new AgentCommand(response, UpdatePolicy.REPLACE);
        }

        // 非闲聊，正常处理
        return new AgentCommand(messages);
    }

    /**
     * 检测是否为闲聊
     */
    private ChitchatType detectChitchat(String input) {
        String normalized = input.trim().toLowerCase();

        // 精确匹配
        if (GREETINGS.contains(normalized)) {
            return ChitchatType.GREETING;
        }
        if (IDENTITY_QUERIES.contains(normalized)) {
            return ChitchatType.IDENTITY;
        }
        if (OFF_TOPIC.contains(normalized)) {
            return ChitchatType.OFF_TOPIC;
        }

        // 包含匹配（短输入）
        if (normalized.length() <= 10) {
            for (String greeting : GREETINGS) {
                if (normalized.contains(greeting)) {
                    return ChitchatType.GREETING;
                }
            }
        }

        return null;
    }

    /**
     * 生成回复
     */
    private String generateReply(ChitchatType type) {
        return switch (type) {
            case GREETING -> """
                你好！我是 **Metric Analyst（数据分析智能体）**，你的专业数据助手。
                
                我可以帮你查询以下领域的数据：
                📊 招聘就业（岗位数量、平均薪酬、招聘企业）
                🏢 市场主体（新增、注销、在营企业）
                💡 知识产权（专利申请）
                🛒 政府采购（金额、数量、均价）
                
                💬 试试这样问：
                • "北京招聘薪资怎么样？"
                • "各省份企业新增排名"
                • "近6个月专利趋势"
                """;

            case IDENTITY -> """
                我是 **Metric Analyst（数据分析智能体）**，专门解答各类经济指标问题。
                
                我擅长：
                📊 查询各类经济指标（招聘、企业、专利、采购）
                📈 分析趋势变化（同比、环比、排名）
                🏆 多地区对比（各省份、各市排名）
                📝 生成数据分析报告
                
                你可以这样问我：
                • "北京本科招聘薪资是多少？"
                • "各省份近半年企业新增排名"
                • "生成招聘市场分析报告"
                """;

            case OFF_TOPIC -> """
                哈哈，这个我还真不太擅长～我是专门做数据分析的，不如我们来聊聊招聘市场或者企业数据？
                
                试试问我：
                • "北京近6个月招聘趋势"
                • "各省份企业新增排名"
                • "对比北京和上海的薪资"
                """;
        };
    }

    /**
     * 提取最后一条用户输入
     */
    private String extractLastUserInput(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage userMsg) {
                return userMsg.getText();
            }
        }
        return null;
    }

    /**
     * 闲聊类型
     */
    private enum ChitchatType {
        GREETING,   // 打招呼
        IDENTITY,   // 询问身份
        OFF_TOPIC   // 无关话题
    }
}
