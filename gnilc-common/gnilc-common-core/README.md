# gnilc-common-core

`gnilc-common-core` 是项目的公共工具库，提供不依赖认证、授权、RBAC 等业务模块实现的基础类型和工具。

## 主要内容

- `com.gnilc.common.base`：通用前置条件检查；
- `com.gnilc.common.constant`：统一响应码枚举；
- `com.gnilc.common.exception`：参数、状态和未知错误异常；
- `com.gnilc.common.utils`：统一响应体、分页参数、分页结果和非空属性拷贝。

`R.code` 只保存调用方提供的 code，不校验非空、取值范围或是否属于业务码。HTTP 状态与响应体 code 的约束由具体应用或接口层决定。

## Maven 依赖

```xml
<dependency>
    <groupId>com.gnilc.auth</groupId>
    <artifactId>gnilc-common-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

该模块可以被业务模块直接依赖，但不能反向依赖 `gnilc-auth-core`、`gnilc-auth-rbac`、`gnilc-system` 或 `gnilc-bootstrap`。
