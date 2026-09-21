package com.gnilc.common.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.gnilc.common.utils.DecimalUtils;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 将十进制数按原有有效精度输出为 JSON 字符串，不使用指数记法，也不执行业务舍入。 */
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = FormatBigDecimalSerializer.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT
})
public @interface FormatBigDecimal {
    /** JSON 字符串最少保留的小数位数；不足时补零，默认两位，已有更多有效位时完整保留。 */
    int minScale() default DecimalUtils.DEFAULT_DECIMAL_PLACES;

}
