package com.lesourire.serveur.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.SocieteDTO;
import com.lesourire.serveur.entite.Assureur;
import com.lesourire.serveur.entite.Societe;
import com.lesourire.serveur.repository.AssureurRepository;
import com.lesourire.serveur.repository.SocieteRepository;

@Service
@Transactional
public class ReferentielService {

    private final AssureurRepository assureurRepository;
    private final SocieteRepository societeRepository;
    private final AuditService auditService;

    public ReferentielService(AssureurRepository assureurRepository,
            SocieteRepository societeRepository, AuditService auditService) {
        this.assureurRepository = assureurRepository;
        this.societeRepository = societeRepository;
        this.auditService = auditService;
    }

    // -------------------------------------------------------------- Assureurs

    @Transactional(readOnly = true)
    public List<AssureurDTO> listerAssureurs(boolean inclureInactifs) {
        List<Assureur> liste = inclureInactifs
                ? assureurRepository.findAllByOrderByNomAsc()
                : assureurRepository.findByActifTrueOrderByNom();
        return liste.stream().map(Assureur::versDTO).toList();
    }

    public AssureurDTO creerAssureur(AssureurDTO dto, String auteur) {
        validerNom(dto.nom(), "assureur");
        if (assureurRepository.existsByNomIgnoreCase(dto.nom().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un assureur porte déjà ce nom.");
        }
        Assureur a = new Assureur();
        appliquerAssureur(a, dto);
        a = assureurRepository.save(a);
        auditService.enregistrer(auteur, "CREATION", "assureur", a.getId(),
                "Création de l'assureur " + a.getNom());
        return a.versDTO();
    }

    public AssureurDTO modifierAssureur(Long id, AssureurDTO dto, String auteur) {
        validerNom(dto.nom(), "assureur");
        Assureur a = assureurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Assureur introuvable."));
        if (assureurRepository.existsByNomIgnoreCaseAndIdNot(dto.nom().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un assureur porte déjà ce nom.");
        }
        appliquerAssureur(a, dto);
        a = assureurRepository.save(a);
        auditService.enregistrer(auteur, "MODIFICATION", "assureur", a.getId(),
                "Modification de l'assureur " + a.getNom());
        return a.versDTO();
    }

    // -------------------------------------------------------------- Sociétés

    @Transactional(readOnly = true)
    public List<SocieteDTO> listerSocietes(boolean inclureInactifs) {
        List<Societe> liste = inclureInactifs
                ? societeRepository.findAllByOrderByNomAsc()
                : societeRepository.findByActifTrueOrderByNom();
        return liste.stream().map(Societe::versDTO).toList();
    }

    public SocieteDTO creerSociete(SocieteDTO dto, String auteur) {
        validerNom(dto.nom(), "société");
        if (societeRepository.existsByNomIgnoreCase(dto.nom().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Une société porte déjà ce nom.");
        }
        Societe s = new Societe();
        appliquerSociete(s, dto);
        s = societeRepository.save(s);
        auditService.enregistrer(auteur, "CREATION", "societe", s.getId(),
                "Création de la société " + s.getNom());
        return s.versDTO();
    }

    public SocieteDTO modifierSociete(Long id, SocieteDTO dto, String auteur) {
        validerNom(dto.nom(), "société");
        Societe s = societeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Société introuvable."));
        if (societeRepository.existsByNomIgnoreCaseAndIdNot(dto.nom().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Une société porte déjà ce nom.");
        }
        appliquerSociete(s, dto);
        s = societeRepository.save(s);
        auditService.enregistrer(auteur, "MODIFICATION", "societe", s.getId(),
                "Modification de la société " + s.getNom());
        return s.versDTO();
    }

    private void appliquerAssureur(Assureur a, AssureurDTO dto) {
        a.setNom(dto.nom().trim());
        a.setTelephone(videSiBlank(dto.telephone()));
        a.setEmail(videSiBlank(dto.email()));
        a.setPourcentageDefaut(pourcentage(dto.pourcentageDefaut()));
        a.setActif(dto.actif());
    }

    private void appliquerSociete(Societe s, SocieteDTO dto) {
        s.setNom(dto.nom().trim());
        s.setTelephone(videSiBlank(dto.telephone()));
        s.setEmail(videSiBlank(dto.email()));
        s.setPourcentageDefaut(pourcentage(dto.pourcentageDefaut()));
        s.setActif(dto.actif());
    }

    private static void validerNom(String nom, String type) {
        if (nom == null || nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom de " + ("assureur".equals(type) ? "l'assureur" : "la société")
                            + " est obligatoire.");
        }
    }

    private static BigDecimal pourcentage(BigDecimal valeur) {
        if (valeur == null) {
            return BigDecimal.ZERO;
        }
        if (valeur.compareTo(BigDecimal.ZERO) < 0 || valeur.compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le pourcentage doit être compris entre 0 et 100.");
        }
        return valeur;
    }

    private static String videSiBlank(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
