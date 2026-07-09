package com.lesourire.serveur;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée du serveur Le Sourire.
 * Héberge l'API REST, applique les migrations de base de données au démarrage
 * et exécutera les tâches planifiées (rappels J-2, revisites, sauvegardes).
 */
@SpringBootApplication
@EnableScheduling
public class LeSourireServeurApplication {

    public static void main(String[] args) {
        // Toutes les dates sont interprétées dans le fuseau du cabinet
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Douala"));
        SpringApplication.run(LeSourireServeurApplication.class, args);
    }
}
