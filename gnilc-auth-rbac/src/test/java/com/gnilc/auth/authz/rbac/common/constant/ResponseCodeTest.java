package com.gnilc.auth.authz.rbac.common.constant;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCodeTest {

    // TestCaseId: RBAC-COMMON-001
    @Test
    void businessCodesAreUnique() {
        Map<Integer, Long> counts = Arrays.stream(ResponseCode.values())
                .collect(Collectors.groupingBy(ResponseCode::getBusinessCode, Collectors.counting()));

        assertThat(counts.values()).allMatch(count -> count == 1L);
    }

    // TestCaseId: RBAC-COMMON-002
    @Test
    void successCodeIsZeroAndErrorCodesAvoidHttpStatusRange() {
        assertThat(ResponseCode.SUCCESS.getBusinessCode()).isZero();
        Arrays.stream(ResponseCode.values())
                .filter(code -> code != ResponseCode.SUCCESS)
                .forEach(code -> {
                    assertThat(code.getBusinessCode()).isGreaterThanOrEqualTo(10_000);
                    assertThat(code.getBusinessCode() >= 100 && code.getBusinessCode() <= 599).isFalse();
                });
    }

    // TestCaseId: RBAC-COMMON-003
    @Test
    void codesMatchBusinessCatalog() {
        assertThat(ResponseCode.ERROR.getBusinessCode()).isEqualTo(10_000);
        assertThat(ResponseCode.ARGUMENT_INVALID.getBusinessCode()).isEqualTo(10_001);
        assertThat(ResponseCode.ILLEGAL_CONDITION.getBusinessCode()).isEqualTo(10_002);
        assertThat(ResponseCode.AUTHENTICATION_FAILED.getBusinessCode()).isEqualTo(20_001);
        assertThat(ResponseCode.UNAUTHORIZED.getBusinessCode()).isEqualTo(20_002);
        assertThat(ResponseCode.ACCESS_DENIED.getBusinessCode()).isEqualTo(20_003);
    }
}
