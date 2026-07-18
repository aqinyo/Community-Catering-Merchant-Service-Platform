package com.aqinyo.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*  属性配置类: 仅负责"封装"dev.yml配置文件传过来的数据 --> 方便其他类创建这个类对象时可调用 封装好的aliOss数据  */

@Component
@ConfigurationProperties(prefix = "aqinyo.alioss")    // 加前缀限定: 有aqinyo.alioss的才加载出来
@Data // 封装     (简去了原本写封装时的Getter和Setter等方法)
public class AliOssProperties {

    // 与dev.yml的配置文件中的 aliOss的数据 对应上
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
