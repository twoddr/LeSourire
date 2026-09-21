package com.lesourire.serveur.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.SauvegardeDTO;
import com.lesourire.serveur.crypto.CheminsInstallation;
import com.lesourire.serveur.repository.ParametreRepository;

import jakarta.annotation.PostConstruct;

/**
 * Sauvegardes MariaDB via mysqldump dans le dossier configuré
 * (paramètre sauvegarde.dossier).
 */
@Service
public class SauvegardeService {

    private static final Logger log = LoggerFactory.getLogger(SauvegardeService.class);

    private final ParametreRepository parametreRepository;
    private final AuditService auditService;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String mysqldump;

    public SauvegardeService(ParametreRepository parametreRepository,
            AuditService auditService,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${lesourire.sauvegarde.mysqldump:}") String mysqldump) {
        this.parametreRepository = parametreRepository;
        this.auditService = auditService;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.mysqldump = mysqldump;
    }

    /**
     * Rend visible dès le démarrage le dossier <strong>réel</strong> des
     * sauvegardes : c'est la question la plus fréquente (« où sont mes
     * sauvegardes ? ») et il dépend d'un paramètre enregistré en base.
     */
    @PostConstruct
    void journaliserDossier() {
        try {
            log.info("Sauvegardes : dossier {} (paramètre sauvegarde.dossier)", dossierSauvegarde());
        } catch (RuntimeException e) {
            log.warn("Sauvegardes : dossier indéterminé au démarrage ({})", e.getMessage());
        }
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
                    "Impossible de créer le dossier de sauvegarde " + dossier + " : " + e.getMessage());
        }
        SauvegardeMysqldump.Coordonnees coordonnees;
        Path programme;
        try {
            coordonnees = SauvegardeMysqldump.analyserUrl(jdbcUrl);
            programme = SauvegardeMysqldump.trouverMysqldump(mysqldump);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
        String nomFichier = SauvegardeMysqldump.nomFichier(LocalDateTime.now());
        Path cible = dossier.resolve(nomFichier);
        log.info("Sauvegarde de la base {} vers {} (mysqldump : {})",
                coordonnees.base(), cible, programme);
        try {
            SauvegardeMysqldump.executer(programme, coordonnees, username, password, cible);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Échec de la sauvegarde dans " + dossier + " : " + e.getMessage(), e);
        }
        auditService.enregistrer(auteur, "CREATION", "sauvegarde", null, nomFichier);
        copierCleDeChiffrement(cible);
        return versDTO(cible);
    }

    /**
     * Dossier des sauvegardes : chemin absolu du paramètre, sinon relatif à la
     * <strong>racine de l'installation</strong> ({@code -Dlesourire.home}, sinon
     * dossier du JAR) et non plus au répertoire de travail : le dossier des
     * sauvegardes ne doit pas changer selon l'endroit d'où le serveur est lancé.
     */
    private Path dossierSauvegarde() {
        String chemin = parametreRepository.findById("sauvegarde.dossier")
                .map(p -> p.getValeur())
                .filter(v -> v != null && !v.isBlank())
                .orElse("sauvegardes");
        Path p = Paths.get(chemin);
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return racineInstallation().resolve(p).normalize();
    }

    private Path racineInstallation() {
        Path racine = CheminsInstallation.racineExplicite();
        if (racine != null) {
            return racine;
        }
        Path dossier = CheminsInstallation.dossierJar();
        return dossier != null ? dossier : CheminsInstallation.repertoireCourant();
    }

    /**
     * Copie la clé de chiffrement à côté du dump : une sauvegarde de la base
     * seule ne permet pas de relire les données chiffrées.
     */
    private void copierCleDeChiffrement(Path dump) {
        Path cle = CheminsInstallation.emplacementGeneration();
        if (!Files.isRegularFile(cle)) {
            log.warn("Sauvegarde {} : clé de chiffrement introuvable en {} — copiez-la "
                    + "manuellement avec le dump, sinon les données chiffrées seront illisibles.",
                    dump.getFileName(), cle);
            return;
        }
        try {
            Path copie = SauvegardeMysqldump.copierCle(cle, dump);
            log.info("Sauvegarde {} : clé de chiffrement copiée en {} (à conserver ensemble).",
                    dump.getFileName(), copie == null ? "(échec)" : copie.getFileName());
        } catch (IOException e) {
            log.warn("Sauvegarde {} : copie de la clé impossible ({}) — copiez {} manuellement.",
                    dump.getFileName(), e.getMessage(), cle);
        }
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
