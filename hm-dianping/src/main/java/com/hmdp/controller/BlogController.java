package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.log.LogApi;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.constant.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Zhi
 * @since 2021-12-22
 */
//@LogApi
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;



    /**
     * 通过用户id查询博客
     *
     * @param current 当前
     * @param id      id
     * @return {@link Result}
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
      @RequestParam(value = "current", defaultValue = "1") Integer current,
      @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
          .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }


    @GetMapping("{blogId}")
    public Result queryBlogById(@Min(1) @PathVariable Long blogId){
        Blog blog = blogService.getBlogById(blogId);
        if (Objects.isNull(blog)){
            return Result.fail(AppHttpCodeEnum.QUERY_ERROR);
        }
        return Result.ok(blog);
    }

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        Blog b = blogService.saveBlog(blog);
        if (Objects.isNull(blog)){
            return Result.fail(AppHttpCodeEnum.INSTER_ERROR);
        }
        // 返回id
        return Result.ok(b.getId());
    }

    @PutMapping("/like/{noteId}")
    public Result likeBlog(@PathVariable("noteId") Long noteId) {
        //// 修改点赞数量
        //blogService.update()
        //        .setSql("liked = liked + 1").eq("id", id).update();
        boolean res = blogService.updateLikeCount(noteId);
        if (res){
            return Result.ok();
        }
        Result result = Result.fail(AppHttpCodeEnum.QUERY_ERROR);
        return result;
    }

    @GetMapping("/likes/{noteId}")
    public Result getBlogLikes(@PathVariable("noteId") Long noteId) {
        List<UserDTO> userList = blogService.getBlogLikes(noteId);
        if (Objects.nonNull(userList)){
            return Result.ok(userList);
        }
        Result result = Result.fail(AppHttpCodeEnum.QUERY_ERROR);
        return result;
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<Blog> records = blogService.queryHotBlog(current);
        return Result.ok(records);
    }


    @GetMapping("/of/follow")
    public Result queryBlogofFollow(@Min(1L) @RequestParam("lastId")Long max,
                                    @NotNull @RequestParam(value = "offset", defaultValue="0")Long offset) {
        ScrollResult scrollResult = blogService.queryBlogOfFollow(max, offset);
        return Result.ok(scrollResult);
    }

}
