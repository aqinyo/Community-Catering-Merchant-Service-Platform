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
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //类型: 1、菜品分类 2、套餐分类
    private Integer type;

    //分类名称
    private String name;

    //顺序
    private Integer sort;

    /*   往上都是DTO类和自己(实体类)有交集部分 --> 可用直接"属性拷贝"转换为自己的实体类 / 然后下面是自己实体类没有的属性 --> 要手动赋值   */

    //分类状态 0：标识禁用 1：表示启用
    private Integer status;

    /*   下面虽然也是需要手动赋值的内容,但是很多实体类都有这部分(公共字段),所以统一做AOP增强即可 --> 避免代码冗余   */

    //创建时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;

    //创建人
    private Long createUser;

    //修改人
    private Long updateUser;
}
