package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.entity.Indicator;
import com.metric.analyst.agent.service.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据分析智能体 - 完整实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricAnalystAgent {

    private final IntentClassificationService intentService;
    private final IndicatorRecognitionService recognitionService;
    private final DimensionNormalizationService dimensionService;
    private final DataQueryService dataQueryService;
    private final ChatModel chatModel;

    // 领域到指标的映射
    private static final Map<String, String[]> DOMAIN_INDICATORS = new HashMap<>();
    static {
        DOMAIN_INDICATORS.put("招聘就业", new String[]{"招聘岗位平均薪酬", "招聘岗位数量", "招聘市场主体数量"});
        DOMAIN_INDICATORS.put("市场主体", new String[]{"新增企业数量", "注销企业数量", "在营企业数量"});
        DOMAIN_INDICATORS.put("知识产权", new String[]{"专利申请数量"});
        DOMAIN_INDICATORS.put("政府采购", new String[]{"政府采购金额", "政府采购数量", "政府采购平均价格"});
    }

    /**
     * 处理用户输入
     */
    public AgentResponse handle(String userInput, ConversationContext context) {
        log.info("Handling user input: {}", userInput);

        try {
            // Stage 0: 意图分类（闲聊识别）
            IntentClassificationService.IntentType intent = intentService.classify(userInput);
            if (intent != IntentClassificationService.IntentType.DATA_QUERY) {
                return AgentResponse.builder()
                    .type(ResponseType.CHITCHAT)
                    .content(intentService.generateChitchatReply(intent))
                    .build();
            }

            // Stage 1: 指标识别
            IndicatorRecognitionService.RecognitionResult recognition = 
                recognitionService.recognize(userInput);

            if (!recognition.isMatched()) {
                return handleNoMatch(recognition, userInput);
            }

            // Stage 2: 维度标准化
            DimensionNormalizationService.NormalizedDimensions dimensions = 
                dimensionService.normalize(
                    recognition.getIndicator().getIndicatorId(),
                    recognition.getIndicator().getTableId(),
                    recognition.getDimensions()
                );

            // Stage 3: 数据查询
            DataQueryService.QueryResult queryResult = dataQueryService.query(
                recognition.getIndicator().getTableId(),
                dimensions
            );

            if (!queryResult.isSuccess()) {
                return AgentResponse.builder()
                    .type(ResponseType.ERROR)
                    .content("查询失败：" + queryResult.getMessage())
                    .build();
            }

            // Stage 4: 洞察生成
            String insight = generateInsight(
                recognition.getIndicator(),
                dimensions,
                queryResult
            );

            return AgentResponse.builder()
                .type(ResponseType.DATA)
                .content(insight)
                .data(queryResult)
                .build();

        } catch (Exception e) {
            log.error("Agent handle failed", e);
            return AgentResponse.builder()
                .type(ResponseType.ERROR)
                .content("处理出错：" + e.getMessage())
                .build();
        }
    }

    /**
     * 处理指标无匹配 - 三层降级
     * 1. 领域推荐：分析意图领域，推荐相似指标
     * 2. 澄清询问：列出Top 3候选让用户选择
     * 3. 全局引导：展示所有可查询领域
     */
    private AgentResponse handleNoMatch(IndicatorRecognitionService.RecognitionResult recognition, String userInput) {
        StringBuilder reply = new StringBuilder();

        switch (recognition.getFallbackType()) {
            case NO_MATCH -> {
                // 第一层降级：领域推荐
                String inferredDomain = inferDomain(userInput);
                if (inferredDomain != null && DOMAIN_INDICATORS.containsKey(inferredDomain)) {
                    reply.append(String.format("""
                        抱歉，我没有找到"%s"相关的具体指标。
                        
                        但根据您的描述，您可能想查询**%s**方面的数据：
                        
                        """, extractKeyWords(userInput), inferredDomain));
                    
                    String[] indicators = DOMAIN_INDICATORS.get(inferredDomain);
                    for (String ind : indicators) {
                        reply.append(String.format("📊 **%s**\n", ind));
                    }
                    
                    reply.append("""
                        
                        💬 您可以这样问：
                        • "北京" + 上述指标名称
                        • "各省份" + 上述指标名称 + "排名"
                        """);
                } else {
                    // 第三层降级：全局引导
                    reply.append("""
                        抱歉，我没有找到与您查询相关的指标。
                        
                        目前我可以帮您查询以下领域的数据：
                        
                        📈 **招聘就业**
                           • 招聘岗位数量、平均薪酬、招聘企业数
                        
                        🏢 **市场主体**
                           • 新增企业、注销企业、在营企业数量
                        
                        💡 **知识产权**
                           • 专利申请数量
                        
                        🛒 **政府采购**
                           • 采购金额、数量、均价
                        
                        💬 **试试这样问：**
                        • "北京招聘薪资"
                        • "各省份企业新增排名"
                        • "近6个月专利趋势"
                        """);
                }
            }
            case AMBIGUOUS -> {
                // 第二层降级：澄清询问
                reply.append("您是想查询以下哪个指标？\n\n");
                
                List<IndicatorRecognitionService.IndicatorCandidate> candidates = recognition.getCandidates();
                if (candidates != null && !candidates.isEmpty()) {
                    for (int i = 0; i < Math.min(3, candidates.size()); i++) {
                        Indicator ind = candidates.get(i).getIndicator();
                        reply.append(String.format("%d. **%s**\n   %s\n\n", 
                            i + 1, 
                            ind.getIndicatorName(),
                            ind.getRemark() != null ? ind.getRemark() : ind.getDomain() + " - " + ind.getSubdomain()));
                    }
                    reply.append("请回复数字 1、2 或 3 选择您想查询的指标。");
                } else {
                    reply.append("抱歉，我无法确定您想查询的指标，请尝试更具体的描述。");
                }
            }
            case ERROR -> {
                reply.append("抱歉，查询出现了问题。请稍后重试，或换个方式提问。");
            }
        }

        return AgentResponse.builder()
            .type(ResponseType.GUIDANCE)
            .content(reply.toString())
            .build();
    }

    /**
     * 推断用户意图领域
     */
    private String inferDomain(String userInput) {
        String lower = userInput.toLowerCase();
        
        // 招聘就业关键词
        if (lower.contains("招聘") || lower.contains("岗位") || lower.contains("薪资") || 
            lower.contains("工资") || lower.contains("薪酬") || lower.contains("求职")) {
            return "招聘就业";
        }
        
        // 市场主体关键词
        if (lower.contains("企业") || lower.contains("公司") || lower.contains("注册") || 
            lower.contains("注销") || lower.contains("在营") || lower.contains("市场主体")) {
            return "市场主体";
        }
        
        // 知识产权关键词
        if (lower.contains("专利") || lower.contains("商标") || lower.contains("版权") || 
            lower.contains("知识产权") || lower.contains("创新")) {
            return "知识产权";
        }
        
        // 政府采购关键词
        if (lower.contains("采购") || lower.contains("招标") || lower.contains("投标") || 
            lower.contains("政府") || lower.contains("采购")) {
            return "政府采购";
        }
        
        return null;
    }

    /**
     * 提取用户输入中的关键词
     */
    private String extractKeyWords(String userInput) {
        // 简单实现：返回前10个字符加省略号
        if (userInput.length() <= 10) {
            return userInput;
        }
        return userInput.substring(0, 10) + "...";
    }

    /**
     * 生成洞察
     */
    private String generateInsight(Indicator indicator,
                                   DimensionNormalizationService.NormalizedDimensions dimensions,
                                   DataQueryService.QueryResult queryResult) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下数据，生成简洁的分析回复（100字以内）：\n\n");
        prompt.append("指标：").append(indicator.getIndicatorName()).append("\n");
        prompt.append("单位：").append(indicator.getUnit()).append("\n");
        
        if (queryResult.getLatestValue() != null) {
            prompt.append("最新值：").append(queryResult.getLatestValue()).append("\n");
        }
        if (queryResult.getLatestYoy() != null) {
            prompt.append("同比：").append(queryResult.getLatestYoy()).append("%\n");
        }
        if (queryResult.getRanking() != null && !queryResult.getRanking().isEmpty()) {
            prompt.append("排名：第").append(queryResult.getRanking().get(0).getRank()).append("\n");
        }
        if (queryResult.getTrend() != null) {
            prompt.append("趋势：").append(queryResult.getTrend().getTrendType()).append("\n");
        }

        prompt.append("\n要求：\n");
        prompt.append("1. 先说最新值和同比\n");
        prompt.append("2. 提及排名（如有）\n");
        prompt.append("3. 描述趋势\n");
        prompt.append("4. 语言简洁自然\n");

        try {
            return chatModel.call(prompt.toString());
        } catch (Exception e) {
            log.warn("Insight generation failed, using template", e);
            // 降级：使用模板
            return String.format("%s最新为%s%s，同比%s%%，呈%s趋势。",
                indicator.getIndicatorName(),
                queryResult.getLatestValue(),
                indicator.getUnit(),
                queryResult.getLatestYoy(),
                queryResult.getTrend() != null ? queryResult.getTrend().getTrendType() : "平稳"
            );
        }
    }

    // DTO
    @Data
    @Builder
    public static class AgentResponse {
        private ResponseType type;
        private String content;
        private Object data;
    }

    public enum ResponseType {
        CHITCHAT,   // 闲聊回复
        DATA,       // 数据回复
        GUIDANCE,   // 引导回复
        ERROR       // 错误
    }

    @Data
    @Builder
    public static class ConversationContext {
        private String sessionId;
        private String lastIndicatorId;
        private Map<String, Object> attributes;
    }
}
