package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.entity.ParticipationRequest;

@Component
public class ParticipationRequestMapper {

    public ParticipationRequestDto toDto(ParticipationRequest participationRequest) {
        return new ParticipationRequestDto(
                participationRequest.getId(),
                participationRequest.getRequesterId(),
                participationRequest.getEventId(),
                participationRequest.getStatus(),
                participationRequest.getCreated()
                );
    }

}
