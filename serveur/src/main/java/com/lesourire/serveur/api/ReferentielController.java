package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;
import com.lesourire.serveur.service.ReferentielService;

/**
 * Référentiels des tiers payants (assureurs et sociétés conventionnées).
 * Liste active pour la fiche patient ; gestion complète (modification,
 * désactivation) depuis le module Administration.
 */
@RestController
@RequestMapping("/api")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/assureurs")
    public List<AssureurDTO> assureurs(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return referentielService.listerAssureurs(inclureInactifs);
    }

    @PostMapping("/assureurs")
    public AssureurDTO creerAssureur(@RequestBody AssureurDTO dto, Principal principal) {
        return referentielService.creerAssureur(dto, principal.getName());
    }

    @PutMapping("/assureurs/{id}")
    public AssureurDTO modifierAssureur(@PathVariable Long id, @RequestBody AssureurDTO dto,
            Principal principal) {
        return referentielService.modifierAssureur(id, dto, principal.getName());
    }

    @GetMapping("/societes")
    public List<SocieteDTO> societes(
            @RequestParam(name = "inclureInactifs", defaultValue = "false") boolean inclureInactifs) {
        return referentielService.listerSocietes(inclureInactifs);
    }

    @PostMapping("/societes")
    public SocieteDTO creerSociete(@RequestBody SocieteDTO dto, Principal principal) {
        return referentielService.creerSociete(dto, principal.getName());
    }

    @PutMapping("/societes/{id}")
    public SocieteDTO modifierSociete(@PathVariable Long id, @RequestBody SocieteDTO dto,
            Principal principal) {
        return referentielService.modifierSociete(id, dto, principal.getName());
    }
}
