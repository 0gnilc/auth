package com.gnilc.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import com.gnilc.common.utils.DecimalUtils;

/** 将带格式注解的 {@link BigDecimal} 无损序列化为普通十进制字符串；舍入由所属业务完成。 */
public final class FormatBigDecimalSerializer
        extends StdSerializer<BigDecimal>
        implements ContextualSerializer {
    private final int minScale;

    public FormatBigDecimalSerializer() {
        this(DecimalUtils.DEFAULT_DECIMAL_PLACES);
    }

    private FormatBigDecimalSerializer(
            int minScale) {
        super(BigDecimal.class);
        if (minScale < 0) {
            throw new IllegalArgumentException(
                    "BigDecimal JSON minimum scale cannot be negative");
        }
        this.minScale = minScale;
    }

    @Override
    public void serialize(
            BigDecimal value,
            JsonGenerator generator,
            SerializerProvider serializers) throws IOException {
        generator.writeString(format(value));
    }

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }
        FormatBigDecimal format = property.getAnnotation(
                FormatBigDecimal.class);
        if (format == null) {
            format = property.getContextAnnotation(FormatBigDecimal.class);
        }
        return format == null
                ? this
                : new FormatBigDecimalSerializer(
                        format.minScale());
    }

    private String format(BigDecimal value) {
        int meaningfulScale = Math.max(
                value.stripTrailingZeros().scale(), 0);
        return DecimalUtils.toPlainString(value, Math.max(minScale, meaningfulScale));
    }
}
