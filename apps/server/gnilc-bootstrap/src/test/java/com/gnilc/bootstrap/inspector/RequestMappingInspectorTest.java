package com.gnilc.bootstrap.inspector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestMappingInspectorTest {

    @Test
    void expandsAndSortsEveryPathAndMethodCombination() throws Exception {
        RequestMappingInfo mapping = mapping("/items/{id}", "/items")
                .methods(RequestMethod.POST, RequestMethod.GET)
                .build();
        RequestMappingInspector inspector = inspector(mapping);
        String handler = TestController.class.getName() + "#handle";

        assertThat(inspector.inspectMappings()).containsExactly(
                new RequestMappingInspector.MappingLogEntry("GET", "/items", handler),
                new RequestMappingInspector.MappingLogEntry("GET", "/items/{id}", handler),
                new RequestMappingInspector.MappingLogEntry("POST", "/items", handler),
                new RequestMappingInspector.MappingLogEntry("POST", "/items/{id}", handler));
    }

    @Test
    void usesWildcardWhenMappingDoesNotDeclareARequestMethod() throws Exception {
        RequestMappingInfo mapping = mapping("/health").build();
        RequestMappingInspector inspector = inspector(mapping);

        assertThat(inspector.inspectMappings())
                .extracting(RequestMappingInspector.MappingLogEntry::method)
                .containsExactly("*");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void logsMappingsAndSummaryAtInfoLevel(CapturedOutput output) throws Exception {
        RequestMappingInfo mapping = mapping("/items/{id}")
                .methods(RequestMethod.GET)
                .build();
        RequestMappingInspector inspector = inspector(mapping);

        inspector.logMappings();

        assertThat(output)
                .contains("Request mapping: GET /items/{id} -> "
                        + TestController.class.getName() + "#handle")
                .contains("Discovered 1 request mappings");
    }

    private RequestMappingInfo.Builder mapping(String... paths) {
        RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
        options.setPatternParser(new PathPatternParser());
        return RequestMappingInfo.paths(paths).options(options);
    }

    private RequestMappingInspector inspector(RequestMappingInfo mapping) throws Exception {
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        Method method = TestController.class.getDeclaredMethod("handle");
        when(handlerMapping.getHandlerMethods())
                .thenReturn(Map.of(mapping, new HandlerMethod(new TestController(), method)));
        return new RequestMappingInspector(handlerMapping);
    }

    private static class TestController {
        public void handle() {
        }
    }
}
