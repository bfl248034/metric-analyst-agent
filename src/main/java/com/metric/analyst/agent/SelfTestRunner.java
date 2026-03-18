package com.metric.analyst.agent;

import com.metric.analyst.agent.service.query.DataQueryService;
import com.metric.analyst.agent.service.query.InMemoryVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 自测运行器 - 启动后自动执行测试场景
 */
@Configuration
public class SelfTestRunner {

    @Bean
    public CommandLineRunner runTests(DataQueryService queryService, InMemoryVectorStore vectorStore) {
        return args -> {
            System.out.println("====================================");
            System.out.println("  开始自测 - 指标分析系统");
            System.out.println("====================================");

            // 等待数据初始化完成
            Thread.sleep(1000);

            // 测试1: 单指标查询
            System.out.println("\n[测试1] 单指标查询 - 招聘数量 @ 北京");
            var result1 = queryService.querySingleMetric("招聘数量", "北京", "最近");
            logResult(result1);

            // 测试2: 多地区对比
            System.out.println("\n[测试2] 多地区对比 - 招聘数量 @ 北京,上海,广东");
            var result2 = queryService.queryMultiRegion("招聘数量", List.of("北京", "上海", "广东"), "最近");
            logResult(result2);

            // 测试3: 趋势分析
            System.out.println("\n[测试3] 趋势分析 - 招聘数量 @ 北京 (6个月)");
            var result3 = queryService.queryTrend("招聘数量", "北京", 6);
            logResult(result3);

            // 测试4: 排名查询
            System.out.println("\n[测试4] 排名查询 - 招聘数量 TOP5");
            var result4 = queryService.queryRanking("招聘数量", "最近", 5);
            logResult(result4);

            // 测试5: 多指标多维度
            System.out.println("\n[测试5] 单指标查询 - 专利数量 @ 广东");
            var result5 = queryService.querySingleMetric("专利数量", "广东", "最近");
            logResult(result5);

            System.out.println("\n====================================");
            System.out.println("  自测完成！");
            System.out.println("====================================");
        };
    }

    private void logResult(DataQueryService.QueryResult result) {
        if (result.success()) {
            System.out.println("  结果: " + result.indicatorName() + " = " + result.value() + " " + result.unit());
            if (result.compareItems() != null) {
                result.compareItems().forEach(item -> 
                    System.out.println("    " + item.region() + ": " + item.value() + " (同比: " + item.yoy() + "%)")
                );
            }
            if (result.trendPoints() != null) {
                System.out.println("  趋势: " + result.trend());
                result.trendPoints().forEach(p -> 
                    System.out.println("    " + p.timeId() + ": " + p.value())
                );
            }
            if (result.rankItems() != null) {
                result.rankItems().forEach(item -> 
                    System.out.println("    #" + item.rank() + " " + item.region() + ": " + item.value())
                );
            }
        } else {
            System.err.println("  错误: " + result.errorMessage());
        }
    }
}
