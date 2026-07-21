package com.gnilc.system.i18n.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.system.i18n.entity.bo.I18nBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态国际化 Mapper。
 */
@Mapper
public interface I18nDao extends BaseMapper<I18nBo> {
}
