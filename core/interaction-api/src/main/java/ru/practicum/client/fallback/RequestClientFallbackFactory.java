package ru.practicum.client.fallback;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.BadRequestException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RequestClientFallbackFactory implements FallbackFactory<RequestClient> {

    @Override
    public RequestClient create(Throwable cause) {
        if (cause instanceof FeignException.FeignClientException fe) {
            System.out.println(fe.getClass());
            throw fe;
        }

        return new RequestClient() {
            @Override
            public List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId) {
                return List.of();
            }

            @Override
            public EventRequestStatusUpdateResult updateRequests(Long eventId, Long userId, EventRequestStatusUpdateRequest updateRequest) {
                throw new BadRequestException("Ошибка в request-service");
            }

            @Override
            public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
                return Collections.emptyMap();
            }
        };
    }
}
