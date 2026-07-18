package com.aqinyo.mapper;

import com.github.pagehelper.Page;
import com.aqinyo.annotation.AutoFill;
import com.aqinyo.dto.CategoryPageQueryDTO;
import com.aqinyo.entity.Category;
import com.aqinyo.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/*   分类管理 Mapper层   */

@Mapper
public interface CategoryMapper {

    // 插入数据
    @Insert("insert into category(type, name, sort, status, create_time, update_time, create_user, update_user)" +
            " VALUES" +
            " (#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Category category);

    // 分页查询
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    // 删除分类 (根据id)
    @Delete("delete from category where id = #{id}")
    void deleteById(Long id);

    // 修改分类 (根据id)
    @AutoFill(value = OperationType.UPDATE)
    void update(Category category);

    // 查询分类 (根据类型)
    List<Category> list(Integer type);

}
