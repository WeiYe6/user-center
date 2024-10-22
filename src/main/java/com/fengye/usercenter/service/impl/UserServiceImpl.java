package com.fengye.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fengye.usercenter.constant.UserConstant;
import com.fengye.usercenter.common.ErrorCode;
import com.fengye.usercenter.exception.BusinessException;
import com.fengye.usercenter.mapper.UserMapper;
import com.fengye.usercenter.model.domain.User;
import com.fengye.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户服务实现类
 *
 * @author 曾伟业
 */

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper , User> implements UserService {

    @Resource
    private UserMapper userMapper;

    /*
    加盐---->混淆密码:只要别人不知道你的盐是什么，就永远解不了
     */
    private static final  String SALT = "yupi";

    /*
    用户登录状态键---->提取到常量类中了
     */
    //private static final String USER_LOGIN_STATE = "userLoginState";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode) {
        //1.校验 非空  同时判断 是否为 空、”“、null
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            //todo 修改为自定义异常
            throw new BusinessException(ErrorCode.NULL_ERROR,"参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        if (planetCode.length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号过长");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~! @#￥%……&* ()——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户有违规字符");
        }
        //密码和校验密码相同
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入密码不一样");
        }

        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已存在该账户");
        }

        //星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode",planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已存在该星球编号");
        }

        //2.对密码进行加密(MD5)
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3.向数据库插入用户数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"插入数据失败");
        }

        Long userId = user.getId();
        if (userId == null){ //因为id为Long 但是我们的返回值为 long ---会出现拆装箱 如果拆箱失败则返回 null
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return userId;
    }


    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验 :  同时判断 是否为 空、”“、null
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名或者密码输入错误");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~! @#￥%……&* ()——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名有违规字符");
        }


        //2.对密码进行加密(MD5)-校验登录账号
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询用户是否存在 以及 和数据库中的密文密码进行校验
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在时
        if (user == null){
            log.info("user login failed, userAccount or passWord error");
            throw new BusinessException(ErrorCode.NULL_ERROR,"用户名或者密码输入错误");
        }


        //3.用户脱敏
        User safetyUser = getSafetyUser(user);

        //4.记录用户的登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,safetyUser);

        return safetyUser;

    }

    /**
     * 用户脱敏
     * @param originUser 需要脱敏的用户
     * @return 脱敏后的用户
     */
    @Override
    public User getSafetyUser(User originUser){
        if (originUser == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"该用户不存在");
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setAge(originUser.getAge());
        safetyUser.setIntroduction(originUser.getIntroduction());
        return safetyUser;
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public boolean updateUser(User newuser, HttpServletRequest request) {
        QueryWrapper<User> queryWrapper = null;
        long count;
        User olduser = userMapper.selectOne(new QueryWrapper<User>().eq("id", newuser.getId()));
        //如果没有改动过账户则不需要进行下面的判断
        if (!newuser.getUserAccount().equals(olduser.getUserAccount())) {
            //账户不能重复
            queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount",newuser.getUserAccount());
            count = userMapper.selectCount(queryWrapper);
            if (count > 0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"已存在该账户");
            }
        }
        //如果没有改动过星球编号则不需要进行下面的判断
        if (!newuser.getPlanetCode().equals(olduser.getPlanetCode())) {
            //星球编号不能重复
            queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("planetCode", newuser.getPlanetCode());
            count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已存在该星球编号");
            }
        }
        userMapper.updateById(newuser);
        return true;
    }

    @Override
    public boolean addUser(User user) {
        //1.校验 非空  同时判断 是否为 空、”“、null
        if (StringUtils.isAnyBlank(user.getUserAccount(),user.getPlanetCode())){
            throw new BusinessException(ErrorCode.NULL_ERROR,"参数为空");
        }
        if (user.getUserAccount().length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户过短");
        }

        if (user.getPlanetCode().length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球编号过长");
        }

        //账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~! @#￥%……&* ()——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(user.getUserAccount());
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账户有违规字符");
        }

        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",user.getUserAccount());
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已存在该账户");
        }

        //星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode",user.getPlanetCode());
        count = userMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"已存在该星球编号");
        }

        //设置默认密码 123456
        user.setUserPassword("12345678");

        //2.对密码进行加密(MD5)
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + user.getUserPassword()).getBytes());
        user.setUserPassword(encryptPassword);//用加密密码覆盖上面的明文密码

        //3.向数据库插入用户数据
        boolean saveResult = this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"插入数据失败");
        }
        return true;
    }
}
