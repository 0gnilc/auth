package com.gnilc.core.i18n.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.core.i18n.entity.bo.I18nMessageBo;
import com.gnilc.core.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.core.i18n.entity.dto.I18nMessageDto;
import com.gnilc.core.i18n.entity.vo.I18nMessageVo;
import com.gnilc.core.i18n.entity.vo.I18nMessageItemVo;

import java.util.Map;
import java.util.List;

/** 管理全局消息键及其各语言翻译，分类只决定管理分组和运行时包范围。 */
public interface DynamicI18nMessageService extends IService<I18nMessageBo> {

    Map<String, Object> getMessageBundle(String category);

    List<String> getSupportedCategories();

    PageResult<I18nMessageItemVo> getMessagePage(I18nMessagePageDto dto);

    I18nMessageVo getMessageValues(String messageKey);

    I18nMessageVo createMessage(I18nMessageDto dto);

    I18nMessageVo saveMessage(I18nMessageDto dto);

    void removeMessage(String messageKey);
}
