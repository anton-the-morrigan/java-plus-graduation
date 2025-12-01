package ru.practicum.stats.analyzer.service.similarity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.dal.entity.Similarity;
import ru.practicum.stats.analyzer.dal.mapper.SimilarityMapper;
import ru.practicum.stats.analyzer.dal.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityServiceImpl implements SimilarityService {
    private final SimilarityRepository repository;

    @Override
    @Transactional
    public void handleSimilarity(EventsSimilarityAvro avro) {
        log.debug("Обработка схожести событий {} и {}", avro.getEventA(), avro.getEventB());
        Similarity similarity = SimilarityMapper.fromAvro(avro);

        Optional<Similarity> forUpdateOptional = repository.findByEvent1AndEvent2(similarity.getEvent1(), similarity.getEvent2());
        if (forUpdateOptional.isEmpty()) {
            repository.save(similarity);
            log.info("Сохранен новый коэффициент схожести событий: {}", similarity);
        } else {
            Similarity forUpdate = forUpdateOptional.get();
            forUpdate.setSimilarity(similarity.getSimilarity());
            forUpdate.setTimestamp(similarity.getTimestamp());
            repository.save(forUpdate);
            log.info("Обновлен коэффициент схожести для событий {} и {}: новое значение {}, timestamp {}",
                    forUpdate.getEvent1(), forUpdate.getEvent2(), forUpdate.getSimilarity(), forUpdate.getTimestamp());
        }
    }
}
