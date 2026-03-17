package com.dreamarchive.service.impl;

import com.dreamarchive.common.PageResult;
import com.dreamarchive.entity.User;
import com.dreamarchive.mapper.CommentMapper;
import com.dreamarchive.mapper.DreamMapper;
import com.dreamarchive.mapper.UserMapper;
import com.dreamarchive.service.UserService;
import com.dreamarchive.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DreamMapper dreamMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public User register(String username, String password, String email) {
        // 检查用户名是否存在
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setAvatar("/uploads/default-avatar.png");
        user.setRole("USER");
        
        userMapper.insert(user);
        return user;
    }
    
    @Override
    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 生成Token
        return jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }
    
    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user != null) {
            // 查询用户的梦境和评论数量
            user.setDreamCount(dreamMapper.countByUserId(id));
            user.setCommentCount(commentMapper.countByUserId(id));
        }
        return user;
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public boolean updateUser(User user) {
        return userMapper.update(user) > 0;
    }
    
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        String encodedPassword = passwordEncoder.encode(newPassword);
        return userMapper.updatePassword(userId, encodedPassword) > 0;
    }
    
    @Override
    public PageResult<User> getUserList(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<User> users = userMapper.findPage(offset, pageSize);
        long total = userMapper.countAllUsers();
        
        return new PageResult<>(total, pageNum, pageSize, users);
    }
    
    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        return userMapper.updateStatus(userId, status) > 0;
    }
}
