package com.aqinyo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication  // SpringBoot自动装配的核心,是一个复合注解 --> @SpringBootConfiguration + @ComponentScan + @EnableAutoConfiguration
@EnableTransactionManagement //开启注解方式的事务管理
@Slf4j  // 开启日志记录
@EnableCaching //开启缓存注解功能 (Spring Cache所需)
@EnableScheduling //开启任务调度 (Spring Task定时任务所需)
public class DiningApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiningApplication.class, args);
        log.info("Dining_service_platform(社区餐饮商户服务平台) server started...");
    }
}
