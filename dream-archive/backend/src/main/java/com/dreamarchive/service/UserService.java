package com.dreamarchive.service;

import com.dreamarchive.entity.User;
import com.dreamarchive.common.PageResult;

/**
 * 用户Service
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    User register(String username, String password, String email);
    
    /**
     * 用户登录
     */
    String login(String username, String password);
    
    /**
     * 根据ID获取用户信息
     */
    User getUserById(Long id);
    
    /**
     * 根据用户名获取用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 更新用户信息
     */
    boolean updateUser(User user);
    
    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 获取用户列表（分页）
     */
    PageResult<User> getUserList(int pageNum, int pageSize);
    
    /**
     * 更新用户状态
     */
    boolean updateUserStatus(Long userId, Integer status);
}
