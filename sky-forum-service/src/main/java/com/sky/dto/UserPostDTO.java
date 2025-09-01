package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPostDTO {
    private Integer userId;
    // 当前页码
    private int page;
    // 每页显示记录数
    private int pageSize;
    private String sortType;
}
