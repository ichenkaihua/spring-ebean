package com.chenkaihua.springebean.controller;

import com.chenkaihua.springebean.entity.User;
import com.chenkaihua.springebean.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by chenkaihua on 15-10-29.
 */
@RestController
@RequestMapping("users")
public class UserController {


    @Autowired
    UserService userService;

    @RequestMapping("save")
    public User sava(@RequestParam(defaultValue = "name", required = false) String name,
                     @RequestParam(defaultValue = "password", required = false) String password) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        userService.save(user);
        return user;
    }

    /**
     * 这是个会产生exception的方法，因此数据库会回滚，说明spring和ebean事物配置成功
     * @param name
     * @param password
     * @return
     */
    @RequestMapping("saveE")
    public User saveE(@RequestParam(defaultValue = "name", required = false) String name,
                      @RequestParam(defaultValue = "password", required = false) String password) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        userService.saveOnThrowException(user);
        return user;


    }



    @RequestMapping("")
    public List<User> users() {
        return userService.users();
    }


}
