# gnilc-common-config

`gnilc-common-config` 提供项目共享的 Spring 基础设施配置。模块位于 classpath 时不会自动启用；使用方按需显式导入配置组件。

```java
@Import({
        MybatisPlusConfiguration.class,
        MyMetaObjectHandler.class,
        LongNumberJacksonConfiguration.class,
        ServletCorsConfiguration.class
})
public class ApplicationConfiguration {
}
```

`MetaObjectHandler`、`MybatisPlusInterceptor` 和 Servlet CORS Filter 使用全局唯一的默认实现，并在使用方提供相同能力时退让。Jackson customizer 可以与其他 customizer 并存；同名 `longNumberJacksonCustomizer` 会替换默认实现。
