package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer postId;

    private Integer userId;

    private String title;
    private String userName;
    private String icon;
    private String content;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Integer view;

    private Integer liked;
    private List<String> images;

    private Integer favorite;

    private boolean userIsLiked ;
    private boolean userIsFavorited ;

}