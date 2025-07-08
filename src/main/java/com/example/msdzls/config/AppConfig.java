package com.example.msdzls.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

/**
 * Spring应用配置类
 */
@Configuration
public class AppConfig {
    /**
     * 配置事务管理器（支持嵌套事务）
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(emf);
        tm.setNestedTransactionAllowed(true); // 允许事务嵌套
        return tm;
    }

    /**
     * 配置带超时的HTTP客户端
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5秒连接超时
        factory.setReadTimeout(10000);    // 10秒读取超时
        return new RestTemplate(factory);
    }
}
