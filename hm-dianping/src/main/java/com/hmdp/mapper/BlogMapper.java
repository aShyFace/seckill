package com.hmdp.mapper;

import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Zhi
 * @since 2021-12-22
 */
@Mapper
public interface BlogMapper extends BaseMapper<Blog> {
  @Select("UPDATE tb_blog SET liked=liked + 1 WHERE id=#{blogId}; SELECT ROW_COUNT();")
  Integer likeIncrease(@Param("blogId") Long blogId);

  @Select("UPDATE tb_blog SET liked=liked - 1 WHERE id=#{blogId}; SELECT ROW_COUNT();")
  Integer likeDesc(@Param("blogId") Long blogId);
}
