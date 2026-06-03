package com.gnilc.authz.rbac.annotation;

import com.gnilc.authz.web.annotation.EnableWebAccessControl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableWebAccessControl
@ComponentScan({"com.gnilc.authz.rbac"})
@MapperScan({"com.gnilc.authz.rbac.dao"})
public class WebRbacAccessControlConfiguration {

}
