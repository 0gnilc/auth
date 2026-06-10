package com.gnilc.authz.rbac.annotation;

import com.gnilc.authz.web.annotation.EnableWebAuthorization;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@EnableWebAuthorization
@Import(RbacAccessIdentityResolverConfiguration.class)
@ComponentScan({"com.gnilc.authz.rbac"})
@MapperScan({"com.gnilc.authz.rbac.dao"})
public class WebRbacAuthorizationConfiguration {

}
