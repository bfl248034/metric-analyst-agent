package com.metric.analyst.agent.agents;

import com.metric.analyst.agent.config.SkillPromptLoader;
import com.metric.analyst.agent.service.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 增强版数据分析智能体 - 使用 Skills Prompt
 * 
 * 特点：
 * 1. 加载 docs/skills/ 下的 Markdown 描述
 * 2. 将 Skills 描述注入 System Prompt
 * 3. 大模型更好地理解系统能力
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedMetricAnalystAgent {

    private final IntentClassificationService intentService;
    private final IndicatorRecognitionService recognitionService;
    private final DimensionNormalizationService dimensionService;
    private final DataQueryService dataQueryService;
    private final ChatModel chatModel;
    private final SkillPromptLoader skillPromptLoader;

    /**
     * 处理用户输入 - 使用增强的 System Prompt
     */
    public AgentResponse handle(String userInput, ConversationContext context) {
        log.info("EnhancedAgent handling: {}", userInput);

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
                // 使用增强的 System Prompt 生成引导回复
                String guidance = generateGuidanceWithPrompt(userInput, recognition);
                return AgentResponse.builder()
                    .type(ResponseType.GUIDANCE)
                    .content(guidance)
                    .build();
            }

            // Stage 2-4: 维度标准化、数据查询、结果加工
            // ... (与原版相同)

            // Stage 5: 使用增强的 System Prompt 生成洞察
            String insight = generateEnhancedInsight(
                recognition.getIndicator(),
                recognition.getDimensions(),
                // query result
            );

            return AgentResponse.builder()
                .type(ResponseType.DATA)
                .content(insight)
                .build();

        } catch (Exception e) {
            log.error("EnhancedAgent handle failed", e);
            return AgentResponse.builder()
                .type(ResponseType.ERROR)
                .content("处理出错：" + e.getMessage())
                .build();
        }
    }

    /**
     * 使用增强的 System Prompt 生成引导回复
     */
    private String generateGuidanceWithPrompt(String userInput, 
                                               IndicatorRecognitionService.RecognitionResult recognition) {
        
        // 获取加载的 System Prompt（包含所有 Skills 描述）
        String systemPrompt = skillPromptLoader.getSystemPrompt();
        
        // 构建用户提示
        String userPrompt = String.format("""
            用户输入: %s
            
            识别结果: 未匹配到具体指标
            原因: %s
            
            请根据你的能力范围，生成友好的引导回复：
            1. 说明无法识别的原因
            2. 告诉用户你能查询哪些指标
            3. 给出3-5个具体的查询示例
            
            回复要求:
            - 语气友好、专业
            - 使用表情符号增加亲和力
            - 控制在200字以内
            """,
            userInput,
            recognition.getMessage()
        );
        
        // 组合 System Prompt + User Prompt
        String fullPrompt = systemPrompt + "\n\n---\n\n" + userPrompt;
        
        try {
            return chatModel.call(fullPrompt);
        } catch (Exception e) {
            log.warn("LLM guidance generation failed, using template", e);
            return generateFallbackGuidance();
        }
    }

    /**
     * 使用增强的 System Prompt 生成洞察
     */
    private String generateEnhancedInsight(Indicator indicator,
                                            Map<String, Object> dimensions,
                                            Object queryResult) {
        
        // 获取 System Prompt
        String systemPrompt = skillPromptLoader.getSystemPrompt();
        
        // 构建针对性的 User Prompt
        String userPrompt = String.format("""
            请基于以下数据生成分析回复：
            
            **指标**: %s
            **单位**: %s
            **维度**: %s
            **数据**: %s
            
            回复要求:
            1. 先说核心数据（最新值）
            2. 补充同比/环比信息（如有）
            3. 提及排名情况（如有）
            4. 描述趋势（上升/下降/平稳）
            5. 控制在150字以内
            6. 用自然、口语化的方式表达
            """,
            indicator.getIndicatorName(),
            indicator.getUnit(),
            dimensions,
            queryResult
        );
        
        String fullPrompt = systemPrompt + "\n\n---\n\n" + userPrompt;
        
        try {
            return chatModel.call(fullPrompt);
        } catch (Exception e) {
            log.warn("LLM insight generation failed, using template", e);
            return generateFallbackInsight(indicator, queryResult);
        }
    }

    /**
     * 使用特定 Skills 的精简 Prompt 处理
     * 
     * 适用场景：已知用户意图，只需要特定 Skills
     */
    public AgentResponse handleWithTargetedSkills(String userInput, 
                                                   List<String> targetSkills) {
        
        // 构建针对性的 Prompt（只包含需要的 Skills）
        String targetedPrompt = skillPromptLoader.buildTargetedPrompt(targetSkills);
        
        log.debug("Using targeted prompt with skills: {}", targetSkills);
        
        // 使用精简 Prompt 调用大模型
        String userPrompt = String.format("""
            用户输入: %s
            
            请根据上述 Skills 描述，判断如何处理此请求。
            如果可以处理，输出处理步骤和需要的工具调用。
            如果无法处理，说明原因并引导用户。
            
            以JSON格式输出：
            {
              "canHandle": true/false,
              "reason": "判断原因",
              "steps": ["步骤1", "步骤2", ...],
              "tools": [{"name": "工具名", "params": {}}]
            }
            """,
            userInput
        );
        
        String fullPrompt = targetedPrompt + "\n\n---\n\n" + userPrompt;
        
        try {
            String response = chatModel.call(fullPrompt);
            // 解析响应，执行对应逻辑
            log.debug("Targeted prompt response: {}", response);
            // ... 解析并执行
            
            return AgentResponse.builder()
                .type(ResponseType.DATA)
                .content(response)
                .build();
        } catch (Exception e) {
            log.error("Targeted skill handling failed", e);
            return AgentResponse.builder()
                .type(ResponseType.ERROR)
                .content("处理失败：" + e.getMessage())
                .build();
        }
    }

    /**
     * 降级引导回复
     */
    private String generateFallbackGuidance() {
        return """
            抱歉，我没有理解您想查询什么指标。
            
            目前我可以帮您查询：
            📈 招聘就业（岗位数量、平均薪酬、招聘企业）
            🏢 市场主体（新增、注销、在营企业）
            💡 知识产权（专利申请）
            🛒 政府采购（金额、数量、均价）
            
            💬 试试这样问：
            • "北京招聘薪资"
            • "各省份企业新增排名"
            • "近6个月专利趋势"
            """;
    }

    /**
     * 降级洞察回复
     */
    private String generateFallbackInsight(Indicator indicator, Object queryResult) {
        return String.format("%s最新数据已为您查询到，具体数值请查看详情。",
            indicator.getIndicatorName());
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
        CHITCHAT,
        DATA,
        GUIDANCE,
        ERROR
    }

    @Data
    @Builder
    public static class ConversationContext {
        private String sessionId;
        private String lastIndicatorId;
        private Map<String, Object> attributes;
    }
}
