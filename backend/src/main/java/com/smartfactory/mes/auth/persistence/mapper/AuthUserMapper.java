package com.smartfactory.mes.auth.persistence.mapper;

import com.smartfactory.mes.auth.domain.AppUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthUserMapper {

    @Select("""
            SELECT user_id, username, email, password_hash, display_name, created_at, updated_at
            FROM app_users
            WHERE LOWER(email) = LOWER(#{email})
            """)
    AppUser selectByEmail(@Param("email") String email);

    @Select("""
            SELECT user_id, username, email, password_hash, display_name, created_at, updated_at
            FROM app_users
            WHERE LOWER(username) = LOWER(#{username})
            """)
    AppUser selectByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO app_users (
                username, email, password_hash, display_name, created_at, updated_at
            ) VALUES (
                #{username}, #{email}, #{passwordHash}, #{displayName}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    void insert(AppUser user);
}
