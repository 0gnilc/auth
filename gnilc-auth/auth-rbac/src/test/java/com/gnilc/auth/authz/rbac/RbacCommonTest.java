package com.gnilc.auth.authz.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gnilc.auth.authz.rbac.common.base.Preconditions;
import com.gnilc.auth.authz.rbac.common.utils.BeanCopyUtils;
import com.gnilc.auth.authz.rbac.common.utils.PageParams;
import com.gnilc.auth.authz.rbac.common.utils.PageResult;
import com.gnilc.auth.authz.rbac.common.utils.R;
import com.gnilc.auth.authz.rbac.exception.IllegalConditionException;
import com.gnilc.auth.authz.rbac.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RbacCommonTest {
    @Test
    void responseEnvelopeSeparatesBusinessCodeFromPayload() {
        R<String> success = R.success("created");
        R<Void> error = R.error(10001, "bad input");

        assertThat(success.getCode()).isZero();
        assertThat(success.getData()).isEqualTo("created");
        assertThat(error.getCode()).isEqualTo(10001);
        assertThat(error.getError()).isEqualTo("bad input");
        assertThatThrownBy(() -> R.error(400, "transport status is not a business code"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageParametersNormalizeInvalidValuesAndResultsCalculatePages() {
        PageParams params = new PageParams();
        params.setCurrentPage(0L);
        params.setPageSize(-1L);

        IPage<String> page = params.getPage();
        PageResult<String> result = new PageResult<>(List.of("a", "b"), 11, 5, 2);

        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(result.getTotalPage()).isEqualTo(3);
        assertThat(result.getCurrentPage()).isEqualTo(2);
    }

    @Test
    void beanCopyUpdatesOnlyNonNullProperties() {
        Mutable source = new Mutable(null, "new");
        Mutable target = new Mutable("keep", "old");

        BeanCopyUtils.copyNonNullProperties(source, target);

        assertThat(target.getFirst()).isEqualTo("keep");
        assertThat(target.getSecond()).isEqualTo("new");
    }

    @Test
    void domainPreconditionsUseDistinctExceptionTypes() {
        assertThatThrownBy(() -> Preconditions.checkArgument(false, "argument"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("argument");
        assertThatThrownBy(() -> Preconditions.checkCondition(false, "state"))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("state");
    }

    public static final class Mutable {
        private String first;
        private String second;

        Mutable(String first, String second) {
            this.first = first;
            this.second = second;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(String second) {
            this.second = second;
        }
    }
}
