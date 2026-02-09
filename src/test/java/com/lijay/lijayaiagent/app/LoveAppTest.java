package com.lijay.lijayaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class LoveAppTest {
    @Resource
    private LoveApp loveApp;

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员lijay";
        String answer = loveApp.doChat(message, chatId);
//        第二轮
//        message = "我想知道苏州有什么地方比较好玩";
//        answer = loveApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
//        //第三轮
//        message = "我叫什么？刚刚跟你说过的，我最近想去哪里玩？";
//        answer = loveApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
    }
}