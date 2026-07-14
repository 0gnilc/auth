package com.gnilc.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Serializes a {@link Long} as a JSON number even when the global Long serializer writes strings.
 */
public final class LongNumberSerializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeNumber(value);
    }
}
