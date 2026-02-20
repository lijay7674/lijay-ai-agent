package com.lijay.lijayaiagent.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 多数据源配置类
 * - primary: MySQL 数据源，用于业务数据（MyBatis-Plus）
 * - vector: PostgreSQL 数据源，用于向量存储（pgvector）
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL 数据源属性配置
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * MySQL 主数据源 - 用于业务数据（MyBatis-Plus 使用）
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return primaryDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    /**
     * PostgreSQL 数据源属性配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.vector")
    public DataSourceProperties vectorDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * PostgreSQL 向量存储数据源 - 用于 pgvector
     */
    @Bean
    public DataSource vectorDataSource() {
        return vectorDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }
}
