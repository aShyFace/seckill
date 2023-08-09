package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.RedisConstant;
import com.hmdp.constant.SystemConstants;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.log.LogApi;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@LogApi
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
  @Resource
  private IUserService userService;
  @Resource
  private BlogMapper blogMapper;
  @Resource
  private RedisCache redisCache;

  @Override
  public Blog getBlogById(Long blogId) {
    Blog blog = blogMapper.selectById(blogId);
    if (Objects.nonNull(blog)){
      queryBlogById(blog);
      blog.setLike(blogIsLiked(blog.getId()));
    }
    return blog;
  }

  @Override
  public List<Blog> queryHotBlog(Integer current) {
    // 根据用户查询
    Page<Blog> page = query()
      .orderByDesc("liked")
      .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
    // 获取当前页数据
    List<Blog> records = page.getRecords();
    // 查询用户
    records.forEach(blog -> {
      queryBlogById(blog);
      blog.setLike(blogIsLiked(blog.getId()));
    });
    return records;
  }




  /**
   * 更新点赞数
   *
   * @param noteId 注意id
   * @return boolean
   */
  @Override
  public boolean updateLikeCount(Long noteId) {
    boolean success = false;
    UserDTO user = UserHolder.getUser();
    Long userId = user.getId();
    String likeKey = String.join("", RedisConstant.BLOG_LIKE_COUNT, noteId.toString());
    Double socre = redisCache.getRedisTemplate().opsForZSet().score(likeKey, userId);
    if (Objects.nonNull(socre)){
      // 点赞了就取消点赞
      int res = blogMapper.likeDesc(noteId);
      if (res > 0){
        Long remove = redisCache.getRedisTemplate().opsForZSet().remove(likeKey, userId);
        if (Objects.nonNull(remove) && remove > 0){
          success = true;
        }
      }
    }else{
      // 没点赞就修改数据库，然后保存到redis
      int res = blogMapper.likeIncrease(noteId);
      if (res > 0){
        Boolean add = redisCache.getRedisTemplate().opsForZSet().add(likeKey, userId, System.currentTimeMillis());
        if (Objects.nonNull(add) && add) {
          success = true;
        }
      }
    }
    return BooleanUtil.isTrue(success);
  }

  @Override
  public List<UserDTO> getBlogLikes(Long noteId) {
    return null;
  }

  private boolean blogIsLiked(Long blogId){
    Long userId = UserHolder.getUser().getId();
    String likeKey = String.join("", RedisConstant.BLOG_LIKE_COUNT, blogId.toString());
    Double socre = redisCache.getRedisTemplate().opsForZSet().score(likeKey, userId);
    return Objects.nonNull(socre);
  }


  private void queryBlogById(Blog blog) {
    Long userId = blog.getUserId();
    User user = userService.getById(userId);
    blog.setName(user.getNickName());
    blog.setIcon(user.getIcon());
  }


}
