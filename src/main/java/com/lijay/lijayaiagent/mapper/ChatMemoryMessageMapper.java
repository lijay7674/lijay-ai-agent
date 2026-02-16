package com.lijay.lijayaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lijay.lijayaiagent.entity.ChatMemoryMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话记忆消息 Mapper 接口
 * 继承 MyBatis-Plus 的 BaseMapper，提供基础的 CRUD 操作
 */
@Mapper
public interface ChatMemoryMessageMapper extends BaseMapper<ChatMemoryMessage> {
}
