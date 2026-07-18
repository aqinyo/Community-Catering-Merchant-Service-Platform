package com.aqinyo.service;

import com.aqinyo.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    // 查询当前登录用户的所有地址信息
    List<AddressBook> list(AddressBook addressBook);

    void save(AddressBook addressBook);

    AddressBook getById(Long id);

    void update(AddressBook addressBook);

    void setDefault(AddressBook addressBook);

    void deleteById(Long id);

}
