package com.lesourire.serveur.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.SauvegardeDTO;
import com.lesourire.serveur.repository.ParametreRepository;

/**
 * Sauvegardes MariaDB via mysqldump dans le dossier configuré
 * (paramètre sauvegarde.dossier).
 */
@Service
public class SauvegardeService {

    private static final DateTimeFormatter FORMAT_FICHIER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern JDBC = Pattern.compile("jdbc:mariadb://([^:/]+)(?::(\\d+))?/([^?]+)");
    private final ParametreRepository parametreRepository;
    private final AuditService auditService;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public SauvegardeService(ParametreRepository parametreRepository,
            AuditService auditService,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.parametreRepository = parametreRepository;
        this.auditService = auditService;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public List<SauvegardeDTO> lister() {
        Path dossier = dossierSauvegarde();
        if (!Files.isDirectory(dossier)) {
            return List.of();
        }
        try (Stream<Path> fichiers = Files.list(dossier)) {
            return fichiers
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".sql") || n.endsWith(".sql.gz");
                    })
                    .map(this::versDTO)
                    .sorted(Comparator.comparing(SauvegardeDTO::dateModification).reversed())
                    .toList();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de lister les sauvegardes : " + e.getMessage());
        }
    }

    public SauvegardeDTO lancer(String auteur) {
        Path dossier = dossierSauvegarde();
        try {
            Files.createDirectories(dossier);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de créer le dossier de sauvegarde : " + e.getMessage());
        }
        Matcher m = JDBC.matcher(jdbcUrl);
        if (!m.find()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "URL JDBC non reconnue.");
        }
        String hote = m.group(1);
        String port = m.group(2) != null ? m.group(2) : "3306";
        String base = m.group(3);
        String nomFichier = "lesourire-" + LocalDateTime.now().format(FORMAT_FICHIER) + ".sql";
        Path cible = dossier.resolve(nomFichier);
        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-h", hote,
                "-P", port,
                "-u", username,
                "--single-transaction",
                "--routines",
                "--triggers",
                base);
        pb.environment().put("MYSQL_PWD", password);
        pb.redirectOutput(cible.toFile());
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            int code = process.waitFor();
            if (code != 0) {
                String erreur = Files.readString(cible);
                Files.deleteIfExists(cible);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "mysqldump a échoué (code " + code + ") : "
                                + erreur.substring(0, Math.min(200, erreur.length())));
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                Files.deleteIfExists(cible);
            } catch (IOException ignored) {
                // ignore
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la sauvegarde : " + e.getMessage()
                            + " (mysqldump est-il installé ?)");
        }
        auditService.enregistrer(auteur, "CREATION", "sauvegarde", null, nomFichier);
        return versDTO(cible);
    }

    private Path dossierSauvegarde() {
        String chemin = parametreRepository.findById("sauvegarde.dossier")
                .map(p -> p.getValeur())
                .filter(v -> v != null && !v.isBlank())
                .orElse("sauvegardes");
        Path p = Paths.get(chemin);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
        }
        return p;
    }

    private SauvegardeDTO versDTO(Path fichier) {
        try {
            long taille = Files.size(fichier);
            LocalDateTime date = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Files.getLastModifiedTime(fichier).toMillis()),
                    ZoneId.systemDefault());
            return new SauvegardeDTO(fichier.getFileName().toString(), taille, date);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lecture fichier impossible : " + e.getMessage());
        }
    }
}
