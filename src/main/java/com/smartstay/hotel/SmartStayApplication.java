package com.smartstay.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class SmartStayApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SmartStayApplication.class, args);
        String port = context.getEnvironment().getProperty("local.server.port",
                context.getEnvironment().getProperty("server.port", "8080"));
        System.out.println("========================================");
        System.out.println("酒店管理系统启动成功!");
        System.out.println("访问地址: http://localhost:" + port);
        System.out.println("========================================");
    }
}
