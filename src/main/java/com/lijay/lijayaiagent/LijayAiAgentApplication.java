package com.lijay.lijayaiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lijay.lijayaiagent.mapper")
public class LijayAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LijayAiAgentApplication.class, args);
    }

}