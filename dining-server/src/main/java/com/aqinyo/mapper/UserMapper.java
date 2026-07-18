package com.aqinyo.mapper;

import com.aqinyo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    // 根据openid查询用户是否为新用户
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    // 插入完成后需要返回主键id,所以需要利用配置文件进行插入并返回id
    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long userId);

    Integer countByMap(Map<String, Object> map);

}
