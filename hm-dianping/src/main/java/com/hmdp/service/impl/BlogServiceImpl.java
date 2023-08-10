package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.RedisConstant;
import com.hmdp.constant.SystemConstants;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.log.LogApi;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.BeanCopyUtils;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
//@LogApi
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
  @Resource
  private IUserService userService;
  @Resource
  private BlogMapper blogMapper;
  @Resource
  private FollowMapper followMapper;
  @Resource
  private RedisCache redisCache;


  //@PostConstruct
  //private void init(){
  //  initBlogLikeData();
  //}
  //private void initBlogLikeData() {
  //  List<Blog> blogList = list();
  //  Iterator<Blog> iterator = blogList.iterator();
  //  while (iterator.hasNext()) {
  //    Blog blog = iterator.next();
  //    String likeKey = String.join("", RedisConstant.BLOG_LIKE_COUNT, blog.getId().toString());
  //    Long number = redisCache.getRedisTemplate().opsForZSet().zCard(likeKey);
  //    if (Objects.isNull(number) || number <= 0) {
  //      redisCache.getRedisTemplate().opsForZSet().add(likeKey, -1, -1);
  //      // 同步数据库中的数据
  //    }
  //  }
  //}


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
   * 更新点赞
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
    Double socre = redisCache.getSocreZSet(likeKey, userId);
    if (Objects.nonNull(socre)){
      // 点赞了就取消点赞
      int res = blogMapper.likeDesc(noteId);
      if (res > 0){
        Long remove = redisCache.deleteCacheZSet(likeKey, userId);
        if (Objects.nonNull(remove) && remove > 0){
          success = true;
        }
      }
    }else{
      // 没点赞就修改数据库，然后保存到redis
      int res = blogMapper.likeIncrease(noteId);
      if (res > 0){
        Boolean add = redisCache.addCacheZSet(likeKey, userId, System.currentTimeMillis());
        if (Objects.nonNull(add) && add) {
          success = true;
        }
      }
    }
    return BooleanUtil.isTrue(success);
  }

  /**
   * 获取前4名，最新点赞的用户
   *
   * @param noteId 注意id
   * @return {@link List}<{@link UserDTO}>
   */
  @Override
  public List<UserDTO> getBlogLikes(Long noteId) {
    //String s = "10";
    //long l = Long.parseLong(s);

    List<UserDTO> userDTOList = null;
    String likeKey = String.join("", RedisConstant.BLOG_LIKE_COUNT, noteId.toString());
    Set<Long> top5 = redisCache.getRangeZSet(likeKey, 0, 4);
    if (Objects.nonNull(top5) && top5.size() != 0) {
      List<Long> userIdList = new ArrayList<>(top5);
      LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
      String idString = StrUtil.join(",", userIdList);
      lqw.in(User::getId, userIdList).last("FIELD(id," + idString +")");

      List<User> userList = userService.listByIds(userIdList);
      userDTOList = BeanCopyUtils.copyBeanList(userList, UserDTO.class);
    }
    return userDTOList;
  }

  /**
   * 保存博客，同时保存blogId到redis
   *
   * @param blog 博客
   * @return {@link Blog}
   */
  @Override
  public Blog saveBlog(Blog blog) {
    // 获取登录用户
    UserDTO user = UserHolder.getUser();
    blog.setUserId(user.getId());
    // 保存探店博文
    if (Objects.isNull(blog.getContent())){
      blog.setContent("该用户觉得菜品不错，默认给了好评");
    }
    boolean success = save(blog);
    if (!success){
      return null;
    }

    // 这个是关注了谁，不是谁关注了我
    LambdaQueryWrapper<Follow> lqw = new LambdaQueryWrapper<>();
    lqw.eq(Follow::getFollowUserId, user.getId().toString());
    List<Follow> followList = followMapper.selectList(lqw);
    List<Long> idList = followList.stream().map(Follow::getUserId).collect(Collectors.toList());
    Iterator<Long> iterator = idList.iterator();
    while (iterator.hasNext()){
      Long userId = iterator.next();
      String feedKey = String.join("", RedisConstant.FEED_USERS, userId.toString());
      // 推送前需要在redis中构建推送blog的数据
      redisCache.addCacheZSet(feedKey, blog.getId(), System.currentTimeMillis());
    }
    return blog;
  }


  private boolean blogIsLiked(Long blogId){
    UserDTO user = UserHolder.getUser();
    if (Objects.isNull(user)){
      return false;
    }
    Long userId = user.getId();
    String likeKey = String.join("", RedisConstant.BLOG_LIKE_COUNT, blogId.toString());
    Double socre = redisCache.getSocreZSet(likeKey, userId);
    return Objects.nonNull(socre);
  }


  private void queryBlogById(Blog blog) {
    Long userId = blog.getUserId();
    User user = userService.getById(userId);
    blog.setName(user.getNickName());
    blog.setIcon(user.getIcon());
  }


  /**
   * 查询博客遵循
   *
   * @param max    起始score
   * @param offset 偏移
   * @return {@link List}<{@link Blog}>
   */
  @Override
  public ScrollResult queryBlogOfFollow(Long max, Long offset) {
    Long userId = UserHolder.getUser().getId();
    String feedKey = String.join("", RedisConstant.FEED_USERS, userId.toString());
    Long min = 0L, count = 3L;
    // 1.获取分页数据
    Set<ZSetOperations.TypedTuple<Integer>> feedDataSet = redisCache.getRangeByScoreWithScores(
      feedKey, max, min, offset.longValue(), count);
    if (Objects.isNull(feedDataSet) || feedDataSet.isEmpty()) {
      return null;

    }
    // 2.获取返回信息
    List<Blog> blogList = new ArrayList<>(feedDataSet.size());
    long minTime = 0, offsetNew = 1;
    for (ZSetOperations.TypedTuple<Integer> tuple: feedDataSet) {
      // 3.获取博文的具体信息（用 listById 的话，需要添加FIELD字段）
      Long value = tuple.getValue().longValue();
      Blog blog = getBlogById(value);
      blogList.add(blog);
      // 先判断相等，再赋值
      long score = tuple.getScore().longValue();
      if (score == minTime){
        offsetNew ++;
      }
      minTime = score;
    }
    //while (iterator.hasNext()){
    //  ZSetOperations.TypedTuple<Long> tuple = iterator.next();
    //  // 3.获取博文的具体信息（用 listById 的话，需要添加FIELD字段）
    //  Integer value = Math.toIntExact(tuple.getValue());
    //  Blog blog = getBlogById(value.longValue());
    //  blogList.add(blog);
    //  // 先判断相等，再赋值
    //  long score = tuple.getScore().longValue();
    //  if (score == minTime){
    //    offsetNew ++;
    //  }
    //  minTime = score;
    //}

    ScrollResult scrollResult = new ScrollResult(blogList, minTime, offsetNew);
    return scrollResult;
  }


}
