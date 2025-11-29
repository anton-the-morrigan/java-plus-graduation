package ru.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.mapper.AnalyzerMapper;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.avro.ActionWeights;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@RequiredArgsConstructor
public class UserActionHandler {
    private final UserActionRepository userActionRepository;

    public UserAction processUserAction(UserActionAvro userAction) {
        UserAction userActionFromBD = findUserActionByEventIdAndUserId(userAction.getEventId(), userAction.getUserId());
        UserAction userActionFromAvro = AnalyzerMapper.toActionFromAvro(userAction);
        if (userActionFromBD != null) {
            if (ActionWeights.WEIGHTS.get(userAction.getActionType()) >= ActionWeights.WEIGHTS.get(userActionFromBD.getActionType())) {
                userActionFromBD.setActionType(userAction.getActionType());
                userActionFromBD.setTimestamp(userAction.getTimestamp());
                userActionFromBD = userActionRepository.save(userActionFromBD);
            }
        } else {
            userActionFromBD = userActionRepository.save(userActionFromAvro);
        }
        return userActionFromBD;
    }

    public UserAction findUserActionByEventIdAndUserId(Long eventId,  Long userId) {
        return userActionRepository.findByEventIdAndUserId(eventId, userId);
    }
}
