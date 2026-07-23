package com.lesourire.serveur.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.NouvelleValeurLettreDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;
import com.lesourire.serveur.entite.CategoriePrestation;
import com.lesourire.serveur.entite.Prestation;
import com.lesourire.serveur.entite.ValeurLettreCle;
import com.lesourire.serveur.repository.CategoriePrestationRepository;
import com.lesourire.serveur.repository.PrestationRepository;
import com.lesourire.serveur.repository.ValeurLettreCleRepository;

@Service
@Transactional
public class PrestationService {

    private static final Set<String> LETTRES = Set.of("D", "Z");

    private final PrestationRepository prestationRepository;
    private final CategoriePrestationRepository categorieRepository;
    private final ValeurLettreCleRepository valeurRepository;
    private final AuditService auditService;

    public PrestationService(PrestationRepository prestationRepository,
            CategoriePrestationRepository categorieRepository,
            ValeurLettreCleRepository valeurRepository,
            AuditService auditService) {
        this.prestationRepository = prestationRepository;
        this.categorieRepository = categorieRepository;
        this.valeurRepository = valeurRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CategoriePrestationDTO> categories() {
        return categorieRepository.findAllByOrderByOrdreAffichageAscLibelleAsc().stream()
                .map(CategoriePrestation::versDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestationDTO> rechercher(String recherche, boolean inclureInactifs) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return prestationRepository.rechercher(q, inclureInactifs).stream()
                .map(Prestation::versDTO)
                .toList();
    }

    public PrestationDTO creer(PrestationDTO dto, String auteur) {
        valider(dto);
        if (prestationRepository.findByCode(dto.code.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce code de prestation est déjà utilisé.");
        }
        Prestation p = new Prestation();
        appliquer(p, dto);
        p = prestationRepository.save(p);
        auditService.enregistrer(auteur, "CREATION", "prestation", p.getId(),
                "Création de " + p.getCode());
        return p.versDTO();
    }

    public PrestationDTO modifier(Long id, PrestationDTO dto, String auteur) {
        valider(dto);
        Prestation p = prestationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Prestation introuvable."));
        if (prestationRepository.existsByCodeAndIdNot(dto.code.trim(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce code de prestation est déjà utilisé.");
        }
        appliquer(p, dto);
        p = prestationRepository.save(p);
        auditService.enregistrer(auteur, "MODIFICATION", "prestation", p.getId(),
                "Modification de " + p.getCode());
        return p.versDTO();
    }

    @Transactional(readOnly = true)
    public List<ValeurLettreCleDTO> valeursEnVigueur() {
        return valeurRepository.findByDateFinIsNullOrderByLettreCleAsc().stream()
                .map(ValeurLettreCle::versDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ValeurLettreCleDTO> historique(String lettre) {
        verifierLettre(lettre);
        return valeurRepository.findByLettreCleOrderByDateDebutDesc(lettre).stream()
                .map(ValeurLettreCle::versDTO)
                .toList();
    }

    /**
     * Clôt la période courante (date_fin = veille de dateDebut) et ouvre une
     * nouvelle valeur. Conserve l'historique pour les actes déjà facturés.
     */
    public ValeurLettreCleDTO changerValeur(String lettre, NouvelleValeurLettreDTO dto,
            String auteur) {
        verifierLettre(lettre);
        if (dto.valeur() == null || dto.valeur().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La valeur doit être strictement positive.");
        }
        LocalDate debut = dto.dateDebut() == null ? LocalDate.now() : dto.dateDebut();

        ValeurLettreCle courante = valeurRepository.findByLettreCleAndDateFinIsNull(lettre)
                .orElse(null);
        if (courante != null) {
            if (!debut.isAfter(courante.getDateDebut())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La nouvelle date de début doit être postérieure à "
                                + courante.getDateDebut() + ".");
            }
            courante.setDateFin(debut.minusDays(1));
            valeurRepository.save(courante);
        }

        ValeurLettreCle nouvelle = new ValeurLettreCle();
        nouvelle.setLettreCle(lettre);
        nouvelle.setValeur(dto.valeur());
        nouvelle.setDateDebut(debut);
        nouvelle.setDateFin(null);
        nouvelle = valeurRepository.save(nouvelle);

        auditService.enregistrer(auteur, "MODIFICATION", "valeur_lettre_cle", nouvelle.getId(),
                "Nouvelle valeur " + lettre + " = " + dto.valeur() + " au " + debut);
        return nouvelle.versDTO();
    }

    private void valider(PrestationDTO dto) {
        if (dto.code == null || dto.code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code est obligatoire.");
        }
        if (dto.libelle == null || dto.libelle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le libellé est obligatoire.");
        }
        if (dto.categorieId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La catégorie est obligatoire.");
        }
        boolean lettre = dto.lettreCle != null && !dto.lettreCle.isBlank();
        boolean forfait = dto.tarifForfait != null;
        if (lettre == forfait) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indiquez soit une lettre-clé × coefficient, soit un forfait.");
        }
        if (lettre) {
            verifierLettre(dto.lettreCle.trim().toUpperCase(Locale.ROOT));
            if (dto.coefficient == null || dto.coefficient.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le coefficient doit être strictement positif.");
            }
        }
        if (forfait && dto.tarifForfait.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le forfait doit être strictement positif.");
        }
    }

    private void appliquer(Prestation p, PrestationDTO dto) {
        CategoriePrestation cat = categorieRepository.findById(dto.categorieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Catégorie introuvable."));
        p.setCode(dto.code.trim().toUpperCase(Locale.ROOT));
        p.setLibelle(dto.libelle.trim());
        p.setCategorie(cat);
        p.setNotes(dto.notes == null || dto.notes.isBlank() ? null : dto.notes.trim());
        p.setActif(dto.actif);
        if (dto.lettreCle != null && !dto.lettreCle.isBlank()) {
            p.setLettreCle(dto.lettreCle.trim().toUpperCase(Locale.ROOT));
            p.setCoefficient(dto.coefficient);
            p.setTarifForfait(null);
        } else {
            p.setLettreCle(null);
            p.setCoefficient(null);
            p.setTarifForfait(dto.tarifForfait);
        }
    }

    private static void verifierLettre(String lettre) {
        if (lettre == null || !LETTRES.contains(lettre.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lettre-clé inconnue (D ou Z).");
        }
    }
}
