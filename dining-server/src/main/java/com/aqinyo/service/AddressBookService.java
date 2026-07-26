package com.aqinyo.service;

import com.aqinyo.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    // 查询当前登录用户的所有地址信息
    List<AddressBook> list(AddressBook addressBook);

    // 新增地址
    void save(AddressBook addressBook);

    // 根据id查询地址
    AddressBook getById(Long id);

    // 修改地址
    void update(AddressBook addressBook);

    // 设置默认地址
    void setDefault(AddressBook addressBook);

    // 删除地址
    void deleteById(Long id);

}
