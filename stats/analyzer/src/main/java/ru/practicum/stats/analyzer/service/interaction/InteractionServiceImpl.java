package ru.practicum.stats.analyzer.service.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.analyzer.dal.entity.Interaction;
import ru.practicum.stats.analyzer.dal.mapper.InteractionMapper;
import ru.practicum.stats.analyzer.dal.repository.InteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {
    private final InteractionRepository repository;

    @Override
    @Transactional
    public void handleInteraction(UserActionAvro avro) {
        Interaction interaction = InteractionMapper.fromAvro(avro);

        Optional<Interaction> optionalOld = repository.findByUserIdAndEventId(interaction.getUserId(), interaction.getEventId());
        if (optionalOld.isEmpty()) {
            saveNewInteraction(interaction);
        } else {
            updateInteraction(interaction, optionalOld.get());
        }
    }

    private void saveNewInteraction(Interaction interaction) {
        repository.save(interaction);
    }

    private void updateInteraction(Interaction newInteraction, Interaction forUpdate) {
        if (forUpdate.getRating() >= newInteraction.getRating()) {
            return;
        }
        forUpdate.setRating(newInteraction.getRating());
        repository.save(forUpdate);
    }
}
