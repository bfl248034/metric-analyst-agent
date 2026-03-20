package com.metric.analyst.agent.service;

import com.metric.analyst.agent.entity.Indicator;

import java.util.List;
import java.util.Optional;

/**
 * 指标定位器接口 - 用于从文本中识别指标
 * 与具体实现解耦，避免循环依赖
 */
public interface IndicatorLocator {
    
    /**
     * 根据文本识别指标
     * @param text 用户输入文本
     * @return 识别结果
     */
    RecognitionResult recognize(String text);
    
    /**
     * 根据名称查找指标
     * @param name 指标名称或别名
     * @return 指标实体
     */
    Optional<Indicator> findByName(String name);
    
    /**
     * 全文搜索指标
     * @param keyword 关键词
     * @return 指标列表
     */
    List<Indicator> searchByKeyword(String keyword);
    
    /**
     * 识别结果
     */
    class RecognitionResult {
        private final boolean matched;
        private final Indicator indicator;
        private final String matchedName;
        private final String message;
        
        private RecognitionResult(boolean matched, Indicator indicator, String matchedName, String message) {
            this.matched = matched;
            this.indicator = indicator;
            this.matchedName = matchedName;
            this.message = message;
        }
        
        public static RecognitionResult success(Indicator indicator, String matchedName) {
            return new RecognitionResult(true, indicator, matchedName, null);
        }
        
        public static RecognitionResult fail(String message) {
            return new RecognitionResult(false, null, null, message);
        }
        
        public boolean isMatched() {
            return matched;
        }
        
        public Indicator getIndicator() {
            return indicator;
        }
        
        public String getMatchedName() {
            return matchedName;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
