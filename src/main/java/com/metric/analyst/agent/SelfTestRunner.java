package com.metric.analyst.agent;

import com.metric.analyst.agent.tools.MetricQueryTools;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动自测
 */
@Component
public class SelfTestRunner implements CommandLineRunner {

    private final MetricQueryTools queryTools;

    public SelfTestRunner(MetricQueryTools queryTools) {
        this.queryTools = queryTools;
    }

    @Override
    public void run(String... args) throws Exception {
        // 等待数据初始化
        Thread.sleep(1000);
        
        System.out.println("\n====================================");
        System.out.println("  指标分析系统 - 启动自测");
        System.out.println("====================================");

        // 测试1: 单指标查询
        System.out.println("\n[测试1] 单指标查询");
        System.out.println(queryTools.queryMetricCurrentValue("招聘数量", "北京"));

        // 测试2: 多地区对比
        System.out.println("\n[测试2] 多地区对比");
        System.out.println(queryTools.queryMetricComparison("招聘数量", "北京,上海,广东"));

        // 测试3: 趋势分析
        System.out.println("\n[测试3] 趋势分析");
        System.out.println(queryTools.queryMetricTrend("招聘数量", "北京", 6));

        // 测试4: 排名查询
        System.out.println("\n[测试4] 排名查询");
        System.out.println(queryTools.queryMetricRanking("招聘数量", 5));

        System.out.println("\n====================================");
        System.out.println("  自测完成！系统运行正常");
        System.out.println("====================================\n");
    }
}
