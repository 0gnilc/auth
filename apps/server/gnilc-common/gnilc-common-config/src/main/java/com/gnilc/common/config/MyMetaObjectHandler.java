package com.gnilc.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** 按实体填充声明写入 UTC 时间点；严格填充保留调用方已经提供的值。 */
@Component("myMetaObjectHandler")
@ConditionalOnMissingBean(MetaObjectHandler.class)
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Instant::now, Instant.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Instant::now, Instant.class);
    }
}
