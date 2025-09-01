package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Details {

    private Integer userId;
    private LocalDate birthday;   // 出生日期（日期类型，对应数据库 date 字段）
    private Integer sex;          // 性别（0:未知, 1:男, 2:女，对应数据库 tinyint(1)）
    private String realName;      // 真实姓名
    private String idCard;        // 身份证号码
    private String address;       // 联系地址
    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
