package com.sky.service;

import com.sky.dto.*;
import com.sky.dto.CommentDTO;
import com.sky.entity.Posts;
import com.sky.result.PageResult;
import com.sky.vo.PostVO;
import com.sky.vo.PostViewVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {
    Integer addPost(PostDTO postDTO);
    void uploadPic(List<MultipartFile> files,Integer postId);
    void addComment(CommentDTO commentDTO);

    void addLike(LikeDTO likeDTO);
    void addFavorite(FavoriteDTO favoriteDTO);
    void addLikeComment(LikeCommentDTO likeCommentDTO);

    void deletePost(Integer postId);

    void deleteComment(Integer commentId);

    /**
     * 获取帖子信息
     * @param postId
     * @return
     */
    PostVO getPostById(Integer postId);

    PageResult getCommentsByPostId(CommentPageQueryDTO commentPageQueryDTO);

    PageResult getPostsByPage(PostPageQueryDTO postPageQueryDTO);

    List<PostViewVO> getTopPostsByViewToday();


    PageResult getUserPost(UserPostDTO userPostDTO);

    PageResult getUserFavor(UserFavorDTO userFavorDTO);


}