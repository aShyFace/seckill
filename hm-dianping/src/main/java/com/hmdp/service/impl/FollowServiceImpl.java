package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.FollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.BeanCopyUtils;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.UTFDataFormatException;
import java.util.*;

/**
 * (Follow)表服务实现类
 *
 * @author makejava
 * @since 2023-08-09 20:01:05
 */
@Service("followService")
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisCache redisCache;
    @Resource
    private IUserService userService;

    /**
     * 更新关注状态
     *
     * @param followUserId 遵循用户id
     * @param isFollow     是遵循
     * @return boolean
     */
    @Override
    public boolean updateFollow(Long followUserId, Boolean isFollow) {
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            return false;
        }

        boolean res = false;
        String followKey = String.join("", RedisConstant.FOLLOW_USERS, user.getId().toString());
        if (isFollow){
            Follow follow = new Follow(user.getId(), followUserId);
            save(follow);
            Long add = redisCache.addCacheSet(followKey, followUserId);
            res = true && add > 0;
        }else{
            LambdaQueryWrapper<Follow> lqw = new LambdaQueryWrapper<>();
            lqw.eq(Follow::getFollowUserId, followUserId).eq(Follow::getUserId, user.getId().toString());
            int change = followMapper.delete(lqw);
            Long remove = redisCache.deleteCacheSet(followKey, followUserId);
            res = change > 0 && remove > 0;
        }
        return res;
    }

    @Override
    public boolean isFollow(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            return false;
        }

        LambdaQueryWrapper<Follow> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Follow::getFollowUserId, followUserId).eq(Follow::getUserId, user.getId().toString());
        Integer count = followMapper.selectCount(lqw);
        return count > 0;
    }

    /**
     * 查询共同关注
     *
     * @param followUserId 遵循用户id
     * @return {@link List}<{@link UserDTO}>
     */
    @Override
    public List<UserDTO> queryCommonFollow(Long followUserId) {
        UserDTO user = UserHolder.getUser();
        if (Objects.isNull(user)){
            return null;
        }

        String followKey = String.join("", RedisConstant.FOLLOW_USERS, followUserId.toString());
        String selfFollowKey = String.join("", RedisConstant.FOLLOW_USERS, user.getId().toString());
        Set intersect = redisCache.intersectCacheSet(followKey, selfFollowKey);
        if (Objects.isNull(intersect) || intersect.isEmpty()){
            return new ArrayList<UserDTO>();
        }
        List<Long> commonUserId = new ArrayList<Long>(intersect);
        List<User> userList = userService.listByIds(commonUserId);
        List<UserDTO> userDTOList = BeanCopyUtils.copyBeanList(userList, UserDTO.class);
        return userDTOList;
    }

}

