package com.labhub.CveLabhubBack.cve_lab.config;

import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class GuacamoleDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mainDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource mainDataSource(
            @Qualifier("mainDataSourceProperties") DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "guacDataSourceProperties")
    @ConfigurationProperties("spring.datasource.guacamole")
    public DataSourceProperties guacamoleDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "guacDataSource")
    public DataSource guacamoleDataSource(
            @Qualifier("guacDataSourceProperties") DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "guacJdbcTemplate")
    public JdbcTemplate guacamoleJdbcTemplate(
            @Qualifier("guacDataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }
}
