package com.dreamarchive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 梦境档案馆 - 主启动类
 * @author 张怀民
 */
@SpringBootApplication
@MapperScan("com.dreamarchive.mapper")
@EnableScheduling
public class DreamArchiveApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DreamArchiveApplication.class, args);
        System.out.println("========================================");
        System.out.println("🌙 梦境档案馆系统启动成功！");
        System.out.println("📍 访问地址: http://localhost:8080/api");
        System.out.println("📖 API文档: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}
