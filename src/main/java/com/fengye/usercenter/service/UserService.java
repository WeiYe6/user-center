package com.fengye.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fengye.usercenter.model.domain.User;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户服务
* @author 曾伟业
* &#064;description  针对表【user(用户表)】的数据库操作Service
* &#064;createDate  2023-05-08 22:36:54
 */

public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String planetCode);


    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 用户脱敏
     * @param originUser 需要脱敏的用户
     * @return 脱敏后的用户
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销-----退出登录
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 管理员修改用户
     *
     * @param newuser 修改的用户
     * @param request
     * @return
     */
    boolean updateUser(User newuser, HttpServletRequest request);

    /**
     * 管理员添加用户
     * @param user 新添的用户
     * @return
     */
    boolean addUser(User user);
}
