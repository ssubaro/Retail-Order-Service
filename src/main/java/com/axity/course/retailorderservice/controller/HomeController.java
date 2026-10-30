package com.axity.course.retailorderservice.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.axity.course.retailorderservice.dto.WelcomeResponseDto;

/**
 * Página de bienvenida en la raíz. Además de orientar a quien abre
 * http://localhost:8080, indica qué instancia respondió: en Kubernetes el
 * hostname del contenedor es el nombre del Pod, así que al recargar se ve cómo
 * el LoadBalancer reparte las peticiones entre réplicas.
 */
@RestController
public class HomeController {

    private final String serviceName;
    private final String version;
    private final String environment;

    public HomeController(
            @Value("${spring.application.name}") String serviceName,
            @Value("${APP_VERSION:local}") String version,
            @Value("${APP_ENVIRONMENT:local}") String environment) {
        this.serviceName = serviceName;
        this.version = version;
        this.environment = environment;
    }

    @GetMapping("/")
    public WelcomeResponseDto welcome() {
        String pod;
        String podIp;
        try {
            InetAddress host = InetAddress.getLocalHost();
            pod = host.getHostName();
            podIp = host.getHostAddress();
        } catch (UnknownHostException e) {
            pod = "desconocido";
            podIp = "desconocida";
        }

        Map<String, String> links = new LinkedHashMap<>();
        links.put("swagger", "/swagger-ui/index.html");
        links.put("products", "/api/products");
        links.put("orders", "/api/orders");
        links.put("auditLogs", "/api/audit-logs");
        links.put("health", "/actuator/health");

        return new WelcomeResponseDto(serviceName, version, environment, pod, podIp, Instant.now(), links);
    }
}
