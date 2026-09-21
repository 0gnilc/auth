package com.gnilc.core.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.core.admin.entity.bo.AdminBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台管理员 Mapper。
 */
@Mapper
public interface AdminDao extends BaseMapper<AdminBo> {
}
