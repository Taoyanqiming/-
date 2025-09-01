package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BusinessException;
import com.sky.mapper.PostMapper;
import com.sky.rabbitmq.MessageSender;
import com.sky.result.PageResult;
import com.sky.service.PostService;
import com.sky.utils.AliOssUtil;
import com.sky.vo.CommentsVO;
import com.sky.vo.PostVO;
import com.sky.vo.PostViewVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * 帖子服务实现类，实现了 PostService 接口中的所有方法。
 */
@Service
public class PostServiceImpl implements PostService {
    @Autowired
    AliOssUtil aliOssUtil;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private MessageSender messageSender;
    /**
     * 添加新帖子。
     * @param postDTO 包含帖子信息的数据传输对象
     */
    @Override
    public Integer addPost(PostDTO postDTO) {
        postMapper.insertPost(postDTO);
        Integer postId = postDTO.getPostId();
        return postId;
    }

    public void uploadPic(List<MultipartFile> files,Integer postId) {
        System.out.println("文件上传"+files);
        if (files != null && !files.isEmpty()) {
            if (files.size() > 9) {
                throw new BusinessException("最多只能上传9张图片");
            }
        }
        // 3. 处理图片上传（如果有图片）
        List<String> imageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    // 验证文件有效性
                    validateFile(file);

                    // 生成文件名和路径
                    String fileName = generateFileName(file);
                    String filePath = "posts/" + postId + "/" + fileName;

                    try {
                        // 上传到OSS并获取URL
                        String imageUrl = aliOssUtil.upload(file.getBytes(), filePath);
                        imageUrls.add(imageUrl);
                    } catch (Exception e) {
                        throw new BusinessException("图片上传失败，请稍后重试");
                    }
                }
            }
        }

        // 4. 更新帖子的图片URL列表（如果有图片）
        if (!imageUrls.isEmpty()) {
            // 将图片URL列表转为JSON字符串存储
            String imagesJson = JSON.toJSONString(imageUrls);
            postMapper.updatePostImages(postId, imagesJson);
        }

    }
    /**
     * 为指定帖子添加评论。
     *
     * @param commentDTO 包含评论信息的数据传输对象
     */
    @Override
    @Transactional
    public void addComment(CommentDTO commentDTO) {
        Comments comments = new Comments();
        BeanUtils.copyProperties(commentDTO, comments);
        comments.setStatus(0);
        //插入评论表
        postMapper.insertComment(comments);
        //修改帖子评论数量
        postMapper.updateComment(comments.getPostId(),+1);
        //查询帖子主人
        PostVO post= postMapper.getPostById(commentDTO.getPostId());
        MessageDTO messageDTO = MessageDTO.builder()
                .type(1)
                .receiver(post.getUserId())
                .sender(commentDTO.getUserId())
                .sourceModule("/forum")
                .sourceId(commentDTO.getPostId())
                .content(commentDTO.getContent())
                .build();
        System.out.println("messageDTO 内容：" + messageDTO);
        //加入消息队列
        messageSender.sendCommentMessage(messageDTO);
    }

    /**
     * 为指定帖子添加点赞。
     * @param likeDTO 包含点赞信息的数据传输对象
     */
    @Override
    @Transactional
    public void addLike(LikeDTO likeDTO) {
        // 判断是否已经点赞
        Likes likes = new Likes();
        if(postMapper.isLiked(likeDTO) != null){
            postMapper.deleteLiked(likeDTO);
            // 减少帖子点赞数量
            postMapper.updateLiked(likeDTO.getPostId(), -1);
        }else{
            // 用户点赞更新数据库
            BeanUtils.copyProperties(likeDTO, likes);
            postMapper.insertLike(likes);
            //更新帖子点赞数量
            postMapper.updateLiked(likes.getPostId(), 1);
            //查询帖子主人
            PostVO post= postMapper.getPostById(likeDTO.getPostId());
            MessageDTO messageDTO = MessageDTO.builder()
                    .type(2)
                    .receiver(post.getUserId())
                    .sender(likeDTO.getUserId())
                    .sourceModule("/forum")
                    .sourceId(likes.getPostId())
                    .content("您收到了一条新点赞")
                    .build();
            // 直接发送原始DTO到消息队列，类型信息可通过消息头或路由键区分
            System.out.println("messageDTO 内容：" + messageDTO);
            messageSender.sendLikeMessage(messageDTO);
        }
    }

    /**
     * 评论点赞
     */
    @Override
    @Transactional
    public void addLikeComment(LikeCommentDTO likeCommentDTO) {
        // 判断是否已经点赞
        CommentLikes commentLikes = new CommentLikes();
        if(postMapper.isCommentLiked(likeCommentDTO) != null){
            postMapper.deleteCommentLiked(likeCommentDTO);
            // 减少评论点赞数量
            postMapper.updateCommnetLiked(likeCommentDTO.getCommentId(), -1);
        }else{
            // 用户点赞更新数据库
            BeanUtils.copyProperties(likeCommentDTO, commentLikes);
            //返回自增主键
            postMapper.insertCommentLike(commentLikes);

            postMapper.updateCommnetLiked(likeCommentDTO.getCommentId(), 1);
            //查询一个评论的所有相关信息
            Comments comments=postMapper.selectComment(likeCommentDTO.getCommentId());

            MessageDTO messageDTO = MessageDTO.builder()
                    .type(2)
                    .receiver(comments.getUserId())
                    .sender(likeCommentDTO.getUserId())
                    .sourceModule("/forum")
                    .sourceId(comments.getPostId())
                    .content("您收到了一条新点赞")
                    .build();
            // 直接发送原始DTO到消息队列
            System.out.println("messageDTO 内容：" + messageDTO);
            messageSender.sendComLikeMessage(messageDTO);
        }
    }
    /**
     * 为指定帖子添加收藏。
     *
     * @param favoriteDTO 包含收藏信息的数据传输对象
     */
    @Override
    public void addFavorite(FavoriteDTO favoriteDTO) {
        Favorites favorites = new Favorites();
        if(postMapper.isFavor(favoriteDTO) != null){
            //取消收藏
            postMapper.deleteFavor(favoriteDTO);
            //修改帖子
            postMapper.incrementFavoriteCount(favoriteDTO.getPostId(),-1);
        }
        BeanUtils.copyProperties(favoriteDTO, favorites);
        postMapper.insertFavorite(favorites);
        postMapper.incrementFavoriteCount(favoriteDTO.getPostId(),1);

    }


    /**
     * 删除指定帖子。
     * @param postId 要删除的帖子的 ID
     */
    @Override
    public void deletePost(Integer postId) {
        postMapper.deletePost(postId);
    }

    /**
     * 删除指定评论。
     * @param commentId
     */
    @Override
    public void deleteComment(Integer commentId) {
        postMapper.deleteComment(commentId);
    }


    /**
     * 根据帖子 ID 分页获取评论列表。
     * @param commentPageQueryDTO 包含分页和查询条件的数据传输对象
     * @return 包含评论列表的分页结果对象
     */
    @Override
    public PageResult getCommentsByPostId(CommentPageQueryDTO commentPageQueryDTO) {
        PageHelper.startPage(commentPageQueryDTO.getPage(), commentPageQueryDTO.getPageSize());
        Page<CommentsVO> page = postMapper.pageQueryByPostId(commentPageQueryDTO);
        long total = page.getTotal();
        List<CommentsVO> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 分页获取所有帖子列表。
     * @param postPageQueryDTO 包含分页和查询条件的数据传输对象
     * @return 包含帖子列表的分页结果对象
     */
    @Override
    public PageResult getPostsByPage(PostPageQueryDTO postPageQueryDTO) {
        PageHelper.startPage(postPageQueryDTO.getPage(), postPageQueryDTO.getPageSize());
        Page<PostVO> page = postMapper.getPostsByPage(postPageQueryDTO);
        System.out.println(page);
        long total = page.getTotal();
        List<PostVO> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 获取帖子信息
     * @param postId
     * @return
     */
    public PostVO getPostById(Integer postId){
        postMapper.incrementViewCount(postId);
        return postMapper.getPostById(postId);
    }

    /**
     * 返回view前10
     * @return
     */
    @Override
    public List<PostViewVO> getTopPostsByViewToday() {
        // 获取当前时间的开始和结束时间（今天）
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1); // 今天的结束时间是明天的开始时间
        // 直接调用 Mapper 方法
        List<PostViewVO> topPosts = postMapper.getTopPostsByViewToday(todayStart, todayEnd);

        return topPosts;
    }
    @Override
    public PageResult getUserPost(UserPostDTO userPostDTO){
        PageHelper.startPage(userPostDTO.getPage(), userPostDTO.getPageSize());
        Page<PostVO> page = postMapper.pageUserPost(userPostDTO);
        long total = page.getTotal();
        List<PostVO> records = page.getResult();
        return new PageResult(total, records);
    }
    @Override
    public PageResult getUserFavor(UserFavorDTO userFavorDTO){
        PageHelper.startPage(userFavorDTO.getPage(), userFavorDTO.getPageSize());
        Page<PostVO> page = postMapper.pageUserFavor(userFavorDTO);
        long total = page.getTotal();
        List<PostVO> records = page.getResult();
        return new PageResult(total, records);
    }
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    // 允许的图片类型
    private static final String[] ALLOWED_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/bmp"
    };
    /**
     * 验证文件有效性
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new BusinessException("不支持的图片格式，仅支持jpg、png、gif、bmp");
        }
    }
    /**
     * 生成文件名
     */
    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";

        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return UUID.randomUUID() + suffix;
    }

}