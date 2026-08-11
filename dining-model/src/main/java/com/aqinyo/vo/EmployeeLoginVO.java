package com.aqinyo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description ="EmployeeLoginVO(员工登录VO)", title = "员工登录返回的数据格式")    /*  用在DTO/VO/Entity类上: 描述数据模型data (即Result中的data)   */
public class EmployeeLoginVO implements Serializable {

    @Schema(description = "主键值")
    private Long id;

    @Schema(description = "用户名")    /*  用在DTO/VO/Entity类的属性上: 描述data中每个字段的含义 (在接口文档中可看到)  */
    private String userName;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "jwt令牌")
    private String token;

}
