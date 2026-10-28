package com.axity.course.retailorderservice.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.axity.course.retailorderservice.model")
@EnableJpaRepositories("com.axity.course.retailorderservice.repository")
public class PersistenceConfig {
}
