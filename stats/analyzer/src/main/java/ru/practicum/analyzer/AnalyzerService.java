package ru.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.analyzer.processor.EventSimilarityProcessor;
import ru.practicum.analyzer.processor.UserActionProcessor;

@SpringBootApplication
public class AnalyzerService {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerService.class, args);

        EventSimilarityProcessor eventSimilarityProcessor = context.getBean(EventSimilarityProcessor.class);
        UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);

        Thread EventSimilarityThread = new Thread(eventSimilarityProcessor);
        EventSimilarityThread.setName("event-similarity-processor");
        EventSimilarityThread.start();

        userActionProcessor.run();
    }
}
