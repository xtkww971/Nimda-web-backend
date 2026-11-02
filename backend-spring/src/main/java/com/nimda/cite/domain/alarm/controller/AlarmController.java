package com.nimda.cite.domain.alarm.controller;

import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.domain.alarm.service.AlarmService;
import com.nimda.cite.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping(UrlNames.alarmUrl)
public class AlarmController {
    private final AlarmService alarmService;

    @GetMapping(UrlNames.subscribeUrl)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return alarmService.subscribe(userId);
    }
}
