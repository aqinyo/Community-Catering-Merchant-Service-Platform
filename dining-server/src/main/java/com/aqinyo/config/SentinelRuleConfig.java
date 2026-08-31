package com.aqinyo.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/*   编写 Sentinel 规则配置类（用 "注解 + 代码加载规则" 的方式配置Sentinel规则,实现代码级别的规则持久化 ; 再搭配 @PostConstruct 在启动时加载配置）  */

@Slf4j
@Configuration
public class SentinelRuleConfig {

    @PostConstruct  /* 服务启动时,立即初始化 Sentinel 规则  (最常用、最清晰、最推荐的做法: 一个接口对应一个规则、且只加给热点接口) */
    public void initRules() {
        log.info("【Sentinel】开始加载规则...");
        loadFlowRules();        // 加载 限流规则
        loadDegradeRules();     // 加载 熔断降级规则
        log.info("【Sentinel】规则加载完成！");
    }



    /*  "限流(流控)" 规则配置     (限流规则有: QPS限流、线程数隔离)  */
    // 把创建流控规则的方法抽取出来
    private FlowRule createFlowRule(String resource, int grade, int count) {
        FlowRule flowRule = new FlowRule();     // 创建一个限流规则对象  (参数设置的就是:限流规则的属性。这样下面的规则配置就能根据参数来设置,更加优雅灵活可读)
        flowRule.setResource(resource);         // 资源名:    必须对齐Controller中的@SentinelResource(value = "...")  /  通常: 资源名 = 接口名/方法名
        flowRule.setGrade(grade);               // 限流类型:   grade可以是QPS（每秒请求数）/ 线程数（每秒请求数）
        flowRule.setCount(count);               // 阈值:      每秒最多count个请求，超过会被限流（可根据实际情况微调）
        return flowRule;
    }

    private void loadFlowRules() {
        List<FlowRule> rules = new ArrayList<>();   // 创建一个限流规则集合，用来存所有我自定义的限流规则

        /*   规则: QPS限流  (一般用于读接口)  */
        // C端-查询菜品接口
        rules.add(createFlowRule("dishList", RuleConstant.FLOW_GRADE_QPS, 1));    // 参数: 资源名, 限流类型, 阈值
        // C端-查询套餐接口 (条件查询)
        rules.add(createFlowRule("conditionList", RuleConstant.FLOW_GRADE_QPS, 1)); // QPS改为1,主要是为了测试验证,实际中可以根据实际情况调高阈值
        // C端-查询套餐接口 (根据套餐id查询包含的菜品)
        rules.add(createFlowRule("setMealDishList", RuleConstant.FLOW_GRADE_QPS, 100));
        // C端-查询分类接口
        rules.add(createFlowRule("categoryList", RuleConstant.FLOW_GRADE_QPS, 100));
        // C端-查询店铺状态接口
        rules.add(createFlowRule("getStatus", RuleConstant.FLOW_GRADE_QPS, 100));

        /*   规则: 线程数隔离  (一般用于写接口)  */
        // C端-提交订单接口
        rules.add(createFlowRule("submit", RuleConstant.FLOW_GRADE_THREAD, 20));
        // C端-订单支付接口
        rules.add(createFlowRule("payment", RuleConstant.FLOW_GRADE_THREAD, 20));

        FlowRuleManager.loadRules(rules);   // 把我们定义好的"所有限流规则"加载到Sentinel中生效
    }



    /*  "降级" 规则配置     (降级规则有: 慢调用比例、异常比例、异常数)  */
    private void loadDegradeRules() {
        List<DegradeRule> degradeRules = new ArrayList<>();

        /*   规则: 异常比例 熔断降级   */
        // C端-订单支付接口  (即异常比例超过20%时熔断5秒。因为只有订单支付才涉及外部支付通道,所以需要降级。而上面的高频读取接口则不需要降级,只需要限流即可)
        DegradeRule payOrderRule = new DegradeRule();
        payOrderRule.setResource("payment");
        payOrderRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);      // 异常比例
        payOrderRule.setCount(0.2);                                             // 异常比例阈值 20%
        payOrderRule.setTimeWindow(5);                                          // 熔断时长 5秒
        payOrderRule.setMinRequestAmount(10);                                   // 最小请求数: 少于10次不触发熔断
        degradeRules.add(payOrderRule);

        DegradeRuleManager.loadRules(degradeRules);    // 把我们定义好的"所有熔断降级规则"加载到Sentinel中生效
    }

}
