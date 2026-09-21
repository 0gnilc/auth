package com.gnilc.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 将指定 {@link Long} 字段输出为 JSON 数字，用于覆盖全局 Long 字符串序列化规则。
 */
public final class LongNumberSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeNumber(value);
    }
}
