package ru.practicum.stats.analyzer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.stats.analyzer.processor.InteractionProcessor;
import ru.practicum.stats.analyzer.processor.SimilarityProcessor;

@Component
@RequiredArgsConstructor
public class AnalyzeRunner implements CommandLineRunner {
    private final InteractionProcessor interactionProcessor;
    private final SimilarityProcessor similarityProcessor;

    @Override
    public void run(String... args) {
        Thread interactionProcessorThread = new Thread(interactionProcessor);
        interactionProcessorThread.setName("InteractionProc");
        interactionProcessorThread.start();

        Thread similarityProcessorThread = new Thread(similarityProcessor);
        similarityProcessorThread.setName("SimilarityProc");
        similarityProcessorThread.start();
    }
}
