package com.aqinyo.config;

import com.aqinyo.properties.AliOssProperties;
import com.aqinyo.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*   配置类: 用于创建 ALiOssUtil工具类对象 --> 利用AliOssProperties类封装好的aliOss的数据 --> 作为new这个Util工具类对象时的参数值   */

@Configuration
@Slf4j
public class AliOssConfiguration {

    @Bean   // 设置为bean,当项目启动了,就能调用这个方法,取到方法返回出来的对象
    @ConditionalOnMissingBean   // 当没有这种bean再创建 (相当于做个判断而已)
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){    // 形参这里的类就是"封装好配置文件值的类",用AliOssProperties类对象get里面封装好的"aliOss的数据"出来
        log.info("正在创建阿里云工具类对象: {}", aliOssProperties);

        // 创建ALiOssUtil工具类对象
        return new AliOssUtil(aliOssProperties.getEndpoint(),   //TODO:使用完3个月免费额度,记得删掉阿里云的bucket
                              aliOssProperties.getAccessKeyId(),
                              aliOssProperties.getAccessKeySecret(),
                              aliOssProperties.getBucketName());
    }

}
