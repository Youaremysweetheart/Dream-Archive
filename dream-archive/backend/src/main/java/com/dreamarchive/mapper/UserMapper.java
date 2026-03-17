package com.dreamarchive.mapper;

import com.dreamarchive.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    /**
     * 根据用户名查询
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 根据邮箱查询
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);

    /**
     * 插入用户
     */
    @Insert("INSERT INTO user (username, password, email, role, avatar) " +
            "VALUES (#{username}, #{password}, #{email}, #{role}, #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 更新用户信息
     */
    @Update("UPDATE user SET username = #{username}, email = #{email}, " +
            "avatar = #{avatar}, role = #{role} WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE user SET email = #{email}, avatar = #{avatar} WHERE id = #{id}")
    int updateProfile(@Param("id") Long id, @Param("email") String email, @Param("avatar") String avatar);

    /**
     * 更新密码
     */
    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE user SET password = #{password} WHERE username = #{username}")
    int updatePasswordByUsername(@Param("username") String username, @Param("password") String password);

    /**
     * 更新用户状态
     */
    @Update("UPDATE user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 删除用户
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int delete(Long id);

    /**
     * 统计所有用户数量
     */
    @Select("SELECT COUNT(*) FROM user")
    int countAllUsers();

    @Select("SELECT COUNT(*) FROM user WHERE DATE(create_time) = CURDATE()")
    int countTodayUsers();

    /**
     * 统计某角色的用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE role = #{role}")
    int count(@Param("role") String role);

    @Select("SELECT COUNT(*) FROM user WHERE UPPER(role) = UPPER(#{role})")
    int countByRoleIgnoreCase(@Param("role") String role);

    /**
     * 分页查询所有用户
     */
    @Select("SELECT * FROM user ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<User> findPage(@Param("offset") int offset, @Param("size") int size);

    /**
     * 查询所有用户（用于管理后台）
     */
    @Select("SELECT * FROM user ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<User> findAll(@Param("offset") int offset, @Param("size") int size);

    @Select("<script>" +
            "SELECT * FROM user WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "<if test='role != null and role != \"\"'> AND UPPER(role) = UPPER(#{role}) </if> " +
            "<if test='status != null'> AND status = #{status} </if> " +
            "<choose> " +
            "<when test='sortBy == \"username\"'> ORDER BY username </when> " +
            "<otherwise> ORDER BY id </otherwise> " +
            "</choose> " +
            "<choose> " +
            "<when test='sortOrder == \"desc\"'> DESC </when> " +
            "<otherwise> ASC </otherwise> " +
            "</choose> " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<User> findAdminPage(@Param("keyword") String keyword,
                             @Param("role") String role,
                             @Param("status") Integer status,
                             @Param("sortBy") String sortBy,
                             @Param("sortOrder") String sortOrder,
                             @Param("offset") int offset,
                             @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM user WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "<if test='role != null and role != \"\"'> AND UPPER(role) = UPPER(#{role}) </if> " +
            "<if test='status != null'> AND status = #{status} </if> " +
            "</script>")
    int countAdminPage(@Param("keyword") String keyword,
                       @Param("role") String role,
                       @Param("status") Integer status);

    @Update("UPDATE user SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);
}
