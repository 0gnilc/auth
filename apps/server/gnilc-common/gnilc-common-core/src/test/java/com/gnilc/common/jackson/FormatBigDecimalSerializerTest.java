package com.gnilc.common.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormatBigDecimalSerializerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void formatsAnnotatedDecimalsAsExactStringsWithAtLeastTwoPlaces()
            throws Exception {
        DecimalValues values = new DecimalValues(
                new BigDecimal("100.000000"),
                new BigDecimal("100.100000"),
                new BigDecimal("100.123400"),
                new BigDecimal("100.123456"));

        assertThat(objectMapper.writeValueAsString(values)).isEqualTo(
                "{\"whole\":\"100.00\",\"oneFraction\":\"100.10\","
                        + "\"trimmed\":\"100.1234\","
                        + "\"exact\":\"100.123456\"}");
    }

    @Test
    void supportsPropertySpecificMinimumScaleForDecimalStrings()
            throws Exception {
        ConfiguredDecimals values = new ConfiguredDecimals(
                new BigDecimal("1E+3"),
                new BigDecimal("12.340000"));

        assertThat(objectMapper.writeValueAsString(values))
                .isEqualTo("{\"fourPlaces\":\"1000.0000\","
                        + "\"rate\":\"12.34\"}");
    }

    @Test
    void leavesUnannotatedDecimalsUnderTheDefaultJacksonContract()
            throws Exception {
        assertThat(objectMapper.writeValueAsString(
                new UnformattedDecimal(new BigDecimal("100.000000"))))
                .isEqualTo("{\"value\":100.000000}");
    }

    private record DecimalValues(
            @FormatBigDecimal BigDecimal whole,
            @FormatBigDecimal BigDecimal oneFraction,
            @FormatBigDecimal BigDecimal trimmed,
            @FormatBigDecimal BigDecimal exact) {
    }

    private record ConfiguredDecimals(
            @FormatBigDecimal(minScale = 4) BigDecimal fourPlaces,
            @FormatBigDecimal BigDecimal rate) {
    }

    private record UnformattedDecimal(BigDecimal value) {
    }
}
