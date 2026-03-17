package com.dreamarchive.mapper;

import com.dreamarchive.entity.UserAvatar;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAvatarMapper {

    @Update("UPDATE user_avatar SET is_current = 0 WHERE user_id = #{userId} AND is_current = 1")
    int clearCurrentByUserId(Long userId);

    @Insert("INSERT INTO user_avatar(user_id, avatar_url, is_current) VALUES(#{userId}, #{avatarUrl}, #{isCurrent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAvatar avatar);
}
