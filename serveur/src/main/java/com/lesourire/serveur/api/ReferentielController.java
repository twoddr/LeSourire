package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;
import com.lesourire.serveur.entite.Assureur;
import com.lesourire.serveur.entite.Societe;
import com.lesourire.serveur.repository.AssureurRepository;
import com.lesourire.serveur.repository.SocieteRepository;
import com.lesourire.serveur.service.AuditService;

/**
 * Référentiels des tiers payants (assureurs et sociétés conventionnées).
 * La gestion complète (modification, désactivation) viendra avec le module
 * Administration ; la création rapide est disponible dès la fiche patient.
 */
@RestController
@RequestMapping("/api")
public class ReferentielController {

    private final AssureurRepository assureurRepository;
    private final SocieteRepository societeRepository;
    private final AuditService auditService;

    public ReferentielController(AssureurRepository assureurRepository,
            SocieteRepository societeRepository, AuditService auditService) {
        this.assureurRepository = assureurRepository;
        this.societeRepository = societeRepository;
        this.auditService = auditService;
    }

    @GetMapping("/assureurs")
    public List<AssureurDTO> assureurs() {
        return assureurRepository.findByActifTrueOrderByNom().stream()
                .map(Assureur::versDTO)
                .toList();
    }

    @PostMapping("/assureurs")
    public AssureurDTO creerAssureur(@RequestBody AssureurDTO dto, Principal principal) {
        if (dto.nom() == null || dto.nom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom de l'assureur est obligatoire.");
        }
        Assureur assureur = new Assureur();
        assureur.setNom(dto.nom().trim());
        assureur.setTelephone(dto.telephone());
        assureur.setEmail(dto.email());
        if (dto.pourcentageDefaut() != null) {
            assureur.setPourcentageDefaut(dto.pourcentageDefaut());
        }
        assureur = assureurRepository.save(assureur);
        auditService.enregistrer(principal.getName(), "CREATION", "assureur",
                assureur.getId(), "Création de l'assureur " + assureur.getNom());
        return assureur.versDTO();
    }

    @GetMapping("/societes")
    public List<SocieteDTO> societes() {
        return societeRepository.findByActifTrueOrderByNom().stream()
                .map(Societe::versDTO)
                .toList();
    }

    @PostMapping("/societes")
    public SocieteDTO creerSociete(@RequestBody SocieteDTO dto, Principal principal) {
        if (dto.nom() == null || dto.nom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom de la société est obligatoire.");
        }
        Societe societe = new Societe();
        societe.setNom(dto.nom().trim());
        societe.setTelephone(dto.telephone());
        societe.setEmail(dto.email());
        if (dto.pourcentageDefaut() != null) {
            societe.setPourcentageDefaut(dto.pourcentageDefaut());
        }
        societe = societeRepository.save(societe);
        auditService.enregistrer(principal.getName(), "CREATION", "societe",
                societe.getId(), "Création de la société " + societe.getNom());
        return societe.versDTO();
    }
}
