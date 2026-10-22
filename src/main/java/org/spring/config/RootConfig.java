package org.spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {"org.spring.service", "org.spring.seeder"})
@Import({JpaConfig.class, SecurityConfig.class})
public class RootConfig {
}
