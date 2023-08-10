package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

  Blog getBlogById(Long blogId);

  List<Blog> queryHotBlog(Integer current);

  boolean updateLikeCount(Long noteId);

  List<UserDTO> getBlogLikes(Long noteId);

  Blog saveBlog(Blog blog);

  ScrollResult queryBlogOfFollow(Long max, Long offset);
}
