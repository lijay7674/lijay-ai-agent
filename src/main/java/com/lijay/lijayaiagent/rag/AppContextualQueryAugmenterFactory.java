package com.lijay.lijayaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

public class AppContextualQueryAugmenterFactory {
    public static ContextualQueryAugmenter doCreate() {
        PromptTemplate emptyContextPrompt = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答关于恋爱相关的问题，别的问题没办法回答，
                有问题可以联系我，我将尽力帮助你。
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPrompt)
                .build();
    }
}
