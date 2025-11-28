package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FeignParticipationRequestController implements RequestClient  {

    private final ParticipationRequestService participationRequestService;

    @GetMapping("/confirmed")
    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        return participationRequestService.getConfirmedRequestsCount(eventIds);
    }

}
