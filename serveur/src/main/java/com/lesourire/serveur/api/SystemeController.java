package com.lesourire.serveur.api;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Point public permettant au client de vérifier que le serveur répond. */
@RestController
@RequestMapping("/api/systeme")
public class SystemeController {

    @Value("${lesourire.version:0.1.0}")
    private String version;

    @GetMapping("/statut")
    public Map<String, String> statut() {
        return Map.of(
                "application", "Le Sourire - Serveur",
                "version", version,
                "heureServeur", LocalDateTime.now().toString());
    }
}
