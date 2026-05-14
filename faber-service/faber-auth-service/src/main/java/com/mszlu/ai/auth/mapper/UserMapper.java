package com.mszlu.ai.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mszlu.ai.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
