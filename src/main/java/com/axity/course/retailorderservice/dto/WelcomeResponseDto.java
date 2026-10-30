package com.axity.course.retailorderservice.dto;

import java.time.Instant;
import java.util.Map;

public record WelcomeResponseDto(
        String service,
        String version,
        String environment,
        String pod,
        String podIp,
        Instant timestamp,
        Map<String, String> links) {
}
