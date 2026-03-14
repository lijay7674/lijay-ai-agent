package com.lijay.lijayaiagent.agent;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class LijayManusTest {
    @Autowired
    private  LijayManus lijayManus;

    @Test
    void testRun() {
        String userPrompt = """
                我的对象在伤害静安区，请帮我找到附近5公里内合适的约会地点，
                并结合一些网络图片制定一份详细的计划，
                最后以pdf格式返回""";
        String result = lijayManus.run(userPrompt);
        Assertions.assertNotNull(result);
    }

}