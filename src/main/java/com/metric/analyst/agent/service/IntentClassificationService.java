package com.metric.analyst.agent.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 意图分类服务 - 识别闲聊 vs 数据查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassificationService {

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
        "1+1等于几", "你会唱歌吗", "推荐一部电影", "讲个故事"
    );

    @Getter
    public enum IntentType {
        GREETING,      // 打招呼
        IDENTITY,      // 询问身份
        OFF_TOPIC,     // 无关闲聊
        DATA_QUERY,    // 数据查询
        UNKNOWN        // 未知
    }

    /**
     * 分类用户意图
     */
    public IntentType classify(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentType.UNKNOWN;
        }

        String normalized = userInput.trim().toLowerCase();

        // 1. 精确匹配
        if (GREETINGS.contains(normalized)) {
            return IntentType.GREETING;
        }
        if (IDENTITY_QUERIES.contains(normalized)) {
            return IntentType.IDENTITY;
        }
        if (OFF_TOPIC.contains(normalized)) {
            return IntentType.OFF_TOPIC;
        }

        // 2. 包含匹配（短输入）
        if (normalized.length() <= 10) {
            for (String greeting : GREETINGS) {
                if (normalized.contains(greeting)) {
                    return IntentType.GREETING;
                }
            }
        }

        // 3. 默认为数据查询意图
        return IntentType.DATA_QUERY;
    }

    /**
     * 生成闲聊回复
     */
    public String generateChitchatReply(IntentType intent) {
        return switch (intent) {
            case GREETING -> """
                你好！我是**Metric Analyst（数据分析智能体）**，您的专业数据助手。
                
                我可以帮您查询以下领域的数据：
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
                我是**Metric Analyst（数据分析智能体）**，专门解答各类经济指标问题。
                
                我擅长：
                📊 查询各类经济指标（招聘、企业、专利、采购）
                📈 分析趋势变化（同比、环比、排名）
                🏆 多地区对比（各省份、各市排名）
                
                您可以这样问我：
                • "北京本科招聘薪资是多少？"
                • "各省份近半年企业新增排名"
                • "深圳专利申请趋势怎么样？"
                """;

            case OFF_TOPIC -> """
                哈哈，这个我还真不太擅长～我是专门做数据分析的，不如我们来聊聊招聘市场或者企业数据？
                
                试试问我：
                • "北京近6个月招聘趋势"
                • "各省份企业新增排名"
                • "专利申请者数量"
                """;

            default -> "您好！有什么可以帮您的吗？";
        };
    }
}
