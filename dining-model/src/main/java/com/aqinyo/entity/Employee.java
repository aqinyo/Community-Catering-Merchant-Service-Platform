package com.aqinyo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

    /*   往上都是DTO类和自己(实体类)有交集部分 --> 可用直接"属性拷贝"转换为自己的实体类 / 然后下面是自己实体类没有的属性 --> 要手动赋值   */

    private Integer status;

    private String password;

    /*   下面虽然也是需要手动赋值的内容,但是很多实体类都有这部分(公共字段),所以统一做AOP增强即可 --> 避免代码冗余   */
    // 格式:@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 格式:@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
