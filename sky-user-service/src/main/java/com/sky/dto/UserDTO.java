package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserDTO implements Serializable {

    private Integer userId;          // 主键（注意：原表为 bigint(20) UNSIGNED，对应 Java 类型为 Long）
    private String phoneNumber;   // 手机号码
    private String password;      // 密码（加密存储）
    private String username;      // 昵称
    private String email;         // 邮箱
    private String role;          // 角色（admin/user）
    private String icon;
    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;


}

