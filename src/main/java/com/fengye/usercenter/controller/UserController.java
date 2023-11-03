package com.fengye.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fengye.usercenter.common.BaseResponse;
import com.fengye.usercenter.common.ErrorCode;
import com.fengye.usercenter.common.ResultUtils;
import com.fengye.usercenter.constant.UserConstant;
import com.fengye.usercenter.exception.BusinessException;
import com.fengye.usercenter.model.domain.User;
import com.fengye.usercenter.model.domain.request.UserLoginRequest;
import com.fengye.usercenter.model.domain.request.UserQueryRequest;
import com.fengye.usercenter.model.domain.request.UserRegisterRequest;
import com.fengye.usercenter.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author 曾伟业
 * @RequestBody 注解用在方法的参数上，表示将 HTTP 请求体中的数据绑定到 UserRegisterRequest 类型的参数中。
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        //return new BaseResponse<>(0,result,"ok");
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    //获取当前用户登录态，信息接口
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        //如果 Session 中存在指定名称的属性，则返回其对应的属性值，否则返回 null。
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        //todo 校验用户是否合法（有没有被封号之类的）
        User user = userService.getById(userId);//如果数据库中查询不到该用户对应的id---返回null
        if (user == null) {
            return new BaseResponse<>(ErrorCode.NULL_ERROR);
        }
        User safetyUser = userService.getSafetyUser(user);//返回脱敏后的用户信息
        return ResultUtils.success(safetyUser);
    }

    //个人设置更新 (更新个人账户信息)
    @PutMapping("/update")
    public BaseResponse<Boolean> updateCurrentUser(@RequestBody User user, HttpServletRequest request){
        User currentUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (currentUser == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (currentUser.getId() <= 0 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        user.setId(currentUser.getId());//由于前端没有传来用户id，所以我们手动添加userId，不然后面的更新就失败了
        boolean b = userService.updateById(user);
        return ResultUtils.success(b);
    }

    //鉴权（管理员才能使用的功能）
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        //仅管理员可查询
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User userQuery = new User();
        if (userQueryRequest != null){
            BeanUtils.copyProperties(userQueryRequest, userQuery);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNoneBlank(userQuery.getUsername())) {
            queryWrapper.like("username", userQuery.getUsername());
        }
        if (userQuery.getGender() != null) {
            queryWrapper.like("gender", userQuery.getGender());
        }
        if (userQuery.getUserRole() != null) {
            queryWrapper.like("userRole", userQuery.getUserRole());
        }
        if (userQuery.getUserStatus() != null) {
            queryWrapper.like("userStatus", userQuery.getUserStatus());
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

/*    //鉴权（管理员才能使用的功能）
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        //仅管理员可查询
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNoneBlank(username)) {
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }*/

    //管理员删除用户
    @PostMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteUser(@PathVariable("id") long id, HttpServletRequest request) {
        //仅管理员可删除
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    //管理员修改用户
    @PutMapping("/update/{id}")
    public BaseResponse<Boolean> updateUser(@PathVariable("id") long id, @RequestBody User newuser, HttpServletRequest request) {
        //仅管理员可操作
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.updateUser(newuser, request);
        return ResultUtils.success(b);
    }

    //管理员添加用户
    @PostMapping("/add")
    public BaseResponse<Boolean> addUser(@RequestBody User user, HttpServletRequest request){
        //仅管理员可操作
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.addUser(user);
        return ResultUtils.success(b);
    }



    /**
     * 判断是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == UserConstant.ADMIN_ROLE;
    }
}
