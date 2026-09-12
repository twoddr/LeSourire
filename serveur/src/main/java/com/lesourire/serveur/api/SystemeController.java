package com.lesourire.serveur.api;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Point public permettant au client de vérifier que le serveur répond. */
@RestController
@RequestMapping("/api/systeme")
public class SystemeController {

    private final String version;

    public SystemeController(ObjectProvider<BuildProperties> buildProperties) {
        // Version issue de META-INF/build-info.properties, elle-même générée depuis
        // la propriété <revision> du pom racine : une seule source de vérité.
        BuildProperties build = buildProperties.getIfAvailable();
        this.version = build != null ? build.getVersion() : "inconnue";
    }

    @GetMapping("/statut")
    public Map<String, String> statut() {
        return Map.of(
                "application", "Le Sourire - Serveur",
                "version", version,
                "heureServeur", LocalDateTime.now().toString());
    }
}
