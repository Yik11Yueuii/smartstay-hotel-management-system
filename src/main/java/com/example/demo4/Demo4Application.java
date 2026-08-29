package com.example.demo4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Demo4Application {
    public static void main(String[] args) {
        SpringApplication.run(Demo4Application.class, args);
        System.out.println("========================================");
        System.out.println("酒店管理系统启动成功!");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("========================================");
    }
}
