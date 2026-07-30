package com.lesourire.serveur.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.FactureLigneDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.serveur.entite.Acte;
import com.lesourire.serveur.entite.Facture;
import com.lesourire.serveur.entite.FactureLigne;
import com.lesourire.serveur.entite.Paiement;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.PatientCouverture;
import com.lesourire.serveur.entite.Prestation;
import com.lesourire.serveur.entite.Utilisateur;
import com.lesourire.serveur.repository.ActeRepository;
import com.lesourire.serveur.repository.FactureRepository;
import com.lesourire.serveur.repository.PaiementRepository;
import com.lesourire.serveur.repository.PatientCouvertureRepository;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.PrestationRepository;
import com.lesourire.serveur.repository.UtilisateurRepository;
import com.lesourire.serveur.repository.ValeurLettreCleRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Facturation : création et cycle de vie des factures, encaissements.
 *
 * Principes :
 * - une ligne adossée à une prestation du tarifaire crée l'acte clinique
 *   correspondant, avec le tarif du jour figé (coefficient × valeur lettre) ;
 * - les pourcentages des tiers payants sont figés d'après les couvertures
 *   actives du patient à la date de la facture ;
 * - une facture n'est modifiable qu'au statut BROUILLON ; ensuite elle
 *   s'émet, s'encaisse ou s'annule (si aucun paiement) ;
 * - les montants payés et le statut PARTIELLEMENT_PAYEE/PAYEE sont recalculés
 *   par les triggers de la base à chaque paiement.
 */
@Service
@Transactional
public class FactureService {

    private static final BigDecimal CENT = new BigDecimal("100");

    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final ActeRepository acteRepository;
    private final PatientRepository patientRepository;
    private final PatientCouvertureRepository couvertureRepository;
    private final PrestationRepository prestationRepository;
    private final ValeurLettreCleRepository valeurLettreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    public FactureService(FactureRepository factureRepository,
            PaiementRepository paiementRepository,
            ActeRepository acteRepository,
            PatientRepository patientRepository,
            PatientCouvertureRepository couvertureRepository,
            PrestationRepository prestationRepository,
            ValeurLettreCleRepository valeurLettreRepository,
            UtilisateurRepository utilisateurRepository,
            AuditService auditService) {
        this.factureRepository = factureRepository;
        this.paiementRepository = paiementRepository;
        this.acteRepository = acteRepository;
        this.patientRepository = patientRepository;
        this.couvertureRepository = couvertureRepository;
        this.prestationRepository = prestationRepository;
        this.valeurLettreRepository = valeurLettreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------- lecture

    @Transactional(readOnly = true)
    public List<FactureDTO> rechercher(String recherche, StatutFacture statut) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return factureRepository.rechercher(q, statut).stream()
                .map(Facture::versDTOResume)
                .toList();
    }

    @Transactional(readOnly = true)
    public FactureDTO obtenir(Long id) {
        return versDTOComplet(chercher(id));
    }

    // ---------------------------------------------------- création / brouillon

    public FactureDTO creer(FactureDTO dto, String auteur) {
        Patient patient = patientRepository.findById(exiger(dto.patientId, "Le patient"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Patient introuvable."));
        Utilisateur praticien = utilisateurRepository
                .findById(exiger(dto.praticienId, "Le praticien"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Praticien introuvable."));

        Facture facture = new Facture();
        facture.setPatient(patient);
        facture.setDateFacture(dto.dateFacture != null ? dto.dateFacture : LocalDate.now());
        facture.setDateEcheance(dto.dateEcheance);
        facture.setNotes(nettoyer(dto.notes));
        facture.setStatut(StatutFacture.BROUILLON);
        facture.setCreePar(auditService.utilisateurCourant(auteur));
        facture.setNumero(prochainNumero(facture.getDateFacture()));

        remplacerLignes(facture, dto, praticien, auteur);
        figerRepartition(facture, dto.remise);

        facture = factureRepository.saveAndFlush(facture);
        // Relit les colonnes calculées par la base (soldes générés)
        entityManager.refresh(facture);
        auditService.enregistrer(auteur, "CREATION", "facture", facture.getId(),
                "Création de la facture " + facture.getNumero() + " ("
                        + facture.getMontantNet() + " XAF, " + patient.nomComplet() + ")");
        return versDTOComplet(facture);
    }

    public FactureDTO modifier(Long id, FactureDTO dto, String auteur) {
        Facture facture = chercher(id);
        exigerStatut(facture, "modifiée", StatutFacture.BROUILLON);
        Utilisateur praticien = utilisateurRepository
                .findById(exiger(dto.praticienId, "Le praticien"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Praticien introuvable."));

        facture.setDateFacture(dto.dateFacture != null ? dto.dateFacture : facture.getDateFacture());
        facture.setDateEcheance(dto.dateEcheance);
        facture.setNotes(nettoyer(dto.notes));

        // Les actes créés pour l'ancien brouillon sont remplacés avec lui
        List<Acte> anciensActes = facture.getLignes().stream()
                .map(FactureLigne::getActe)
                .filter(a -> a != null)
                .toList();
        facture.getLignes().clear();
        remplacerLignes(facture, dto, praticien, auteur);
        figerRepartition(facture, dto.remise);

        factureRepository.saveAndFlush(facture);
        acteRepository.deleteAll(anciensActes);
        entityManager.refresh(facture);

        auditService.enregistrer(auteur, "MODIFICATION", "facture", facture.getId(),
                "Modification du brouillon " + facture.getNumero());
        return versDTOComplet(facture);
    }

    // ------------------------------------------------------------ cycle de vie

    public FactureDTO emettre(Long id, String auteur) {
        Facture facture = chercher(id);
        exigerStatut(facture, "émise", StatutFacture.BROUILLON);
        if (facture.getLignes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'émettre une facture sans aucune ligne.");
        }
        facture.setStatut(StatutFacture.EMISE);
        auditService.enregistrer(auteur, "MODIFICATION", "facture", facture.getId(),
                "Émission de la facture " + facture.getNumero());
        return versDTOComplet(facture);
    }

    public FactureDTO annuler(Long id, String auteur) {
        Facture facture = chercher(id);
        if (facture.getStatut() == StatutFacture.ANNULEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cette facture est déjà annulée.");
        }
        if (paiementRepository.countByFactureId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible d'annuler : des paiements sont déjà enregistrés sur cette facture.");
        }
        facture.setStatut(StatutFacture.ANNULEE);
        auditService.enregistrer(auteur, "MODIFICATION", "facture", facture.getId(),
                "Annulation de la facture " + facture.getNumero());
        return versDTOComplet(facture);
    }

    // ------------------------------------------------------------- paiements

    public FactureDTO encaisser(Long id, PaiementDTO dto, String auteur) {
        Facture facture = chercher(id);
        exigerStatut(facture, "encaissée",
                StatutFacture.EMISE, StatutFacture.PARTIELLEMENT_PAYEE);
        if (dto.montant == null || dto.montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant du paiement doit être strictement positif.");
        }
        if (dto.mode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mode de paiement est obligatoire.");
        }
        Payeur payeur = dto.payeur == null ? Payeur.PATIENT : dto.payeur;
        BigDecimal solde = switch (payeur) {
            case PATIENT -> facture.getSoldePatient();
            case ASSUREUR -> facture.getSoldeAssureur();
            case SOCIETE -> facture.getSoldeSociete();
        };
        if (dto.montant.compareTo(solde) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le paiement (" + dto.montant + ") dépasse le solde restant dû par "
                            + payeur.name().toLowerCase(Locale.FRENCH) + " (" + solde + ").");
        }

        Paiement paiement = new Paiement();
        paiement.setFacture(facture);
        paiement.setDatePaiement(dto.datePaiement != null ? dto.datePaiement : LocalDateTime.now());
        paiement.setMontant(dto.montant);
        paiement.setMode(dto.mode);
        paiement.setPayeur(payeur);
        paiement.setReference(nettoyer(dto.reference));
        paiement.setNotes(nettoyer(dto.notes));
        paiement.setRecuPar(auditService.utilisateurCourant(auteur));
        paiement = paiementRepository.saveAndFlush(paiement);

        // Les triggers ont mis à jour paye_* et le statut : on recharge la facture
        entityManager.refresh(facture);

        auditService.enregistrer(auteur, "CREATION", "paiement", paiement.getId(),
                "Encaissement de " + dto.montant + " XAF (" + payeur + ") sur "
                        + facture.getNumero());
        return versDTOComplet(facture);
    }

    public FactureDTO supprimerPaiement(Long id, Long paiementId, String auteur) {
        Facture facture = chercher(id);
        Paiement paiement = paiementRepository.findById(paiementId)
                .filter(p -> p.getFacture().getId().equals(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paiement introuvable sur cette facture."));
        paiementRepository.delete(paiement);
        paiementRepository.flush();
        entityManager.refresh(facture);

        auditService.enregistrer(auteur, "SUPPRESSION", "paiement", paiementId,
                "Annulation d'un paiement de " + paiement.getMontant() + " XAF ("
                        + paiement.getPayeur() + ") sur " + facture.getNumero());
        return versDTOComplet(facture);
    }

    // -------------------------------------------------------------- internes

    /** Construit les lignes (et les actes associés) à partir du DTO. */
    private void remplacerLignes(Facture facture, FactureDTO dto, Utilisateur praticien,
            String auteur) {
        if (dto.lignes == null || dto.lignes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La facture doit contenir au moins une ligne.");
        }
        Utilisateur createur = auditService.utilisateurCourant(auteur);
        for (FactureLigneDTO ligneDTO : dto.lignes) {
            if (ligneDTO.quantite <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La quantité d'une ligne doit être au moins 1.");
            }
            FactureLigne ligne = new FactureLigne();
            ligne.setFacture(facture);
            ligne.setQuantite(ligneDTO.quantite);

            if (ligneDTO.prestationId != null) {
                Prestation prestation = prestationRepository.findById(ligneDTO.prestationId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Prestation introuvable : " + ligneDTO.prestationId));
                BigDecimal prixUnitaire = prixUnitaire(prestation, facture.getDateFacture());
                BigDecimal montant = prixUnitaire
                        .multiply(BigDecimal.valueOf(ligneDTO.quantite))
                        .setScale(2, RoundingMode.HALF_UP);

                Acte acte = new Acte();
                acte.setPatient(facture.getPatient());
                acte.setPraticien(praticien);
                acte.setPrestation(prestation);
                acte.setDateActe(facture.getDateFacture().atStartOfDay());
                acte.setDents(nettoyer(ligneDTO.dents));
                acte.setQuantite(ligneDTO.quantite);
                if (prestation.getLettreCle() != null) {
                    acte.setCoefficientApplique(prestation.getCoefficient());
                    acte.setValeurLettreAppliquee(
                            valeurLettre(prestation.getLettreCle(), facture.getDateFacture()));
                }
                acte.setMontant(montant);
                acte.setCreePar(createur);
                acte = acteRepository.save(acte);

                ligne.setActe(acte);
                ligne.setDesignation(prestation.getLibelle()
                        + (ligneDTO.dents == null || ligneDTO.dents.isBlank()
                                ? "" : " — dent(s) " + ligneDTO.dents.trim()));
                ligne.setPrixUnitaire(prixUnitaire);
                ligne.setMontant(montant);
            } else {
                // Ligne libre : désignation et prix saisis à la main
                if (ligneDTO.designation == null || ligneDTO.designation.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La désignation d'une ligne libre est obligatoire.");
                }
                if (ligneDTO.prixUnitaire == null
                        || ligneDTO.prixUnitaire.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Le prix unitaire d'une ligne libre est obligatoire.");
                }
                ligne.setDesignation(ligneDTO.designation.trim());
                ligne.setPrixUnitaire(ligneDTO.prixUnitaire);
                ligne.setMontant(ligneDTO.prixUnitaire
                        .multiply(BigDecimal.valueOf(ligneDTO.quantite))
                        .setScale(2, RoundingMode.HALF_UP));
            }
            facture.getLignes().add(ligne);
        }
    }

    /** Prix unitaire d'une prestation au tarif du jour de la facture. */
    private BigDecimal prixUnitaire(Prestation prestation, LocalDate jour) {
        if (prestation.getLettreCle() != null) {
            return prestation.getCoefficient()
                    .multiply(valeurLettre(prestation.getLettreCle(), jour))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return prestation.getTarifForfait();
    }

    private BigDecimal valeurLettre(String lettre, LocalDate jour) {
        return valeurLettreRepository.valeurAuJour(lettre, jour)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Aucune valeur de la lettre-clé " + lettre + " n'est définie au " + jour
                                + ". Vérifiez le tarifaire."))
                .getValeur();
    }

    /**
     * Calcule brut/net et fige la répartition entre payeurs d'après les
     * couvertures actives du patient à la date de la facture.
     */
    private void figerRepartition(Facture facture, BigDecimal remise) {
        BigDecimal brut = facture.getLignes().stream()
                .map(FactureLigne::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remiseSure = remise == null ? BigDecimal.ZERO : remise;
        if (remiseSure.compareTo(BigDecimal.ZERO) < 0 || remiseSure.compareTo(brut) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La remise doit être comprise entre 0 et le montant brut (" + brut + ").");
        }
        BigDecimal net = brut.subtract(remiseSure);

        facture.setMontantBrut(brut);
        facture.setRemise(remiseSure);
        facture.setMontantNet(net);

        facture.setAssureur(null);
        facture.setSociete(null);
        BigDecimal pctAssureur = BigDecimal.ZERO;
        BigDecimal pctSociete = BigDecimal.ZERO;

        List<PatientCouverture> actives = couvertureRepository.actives(
                List.of(facture.getPatient().getId()), facture.getDateFacture());
        for (PatientCouverture couverture : actives) {
            CouvertureDTO c = couverture.versDTO();
            BigDecimal pct = c.pourcentageEffectif == null ? BigDecimal.ZERO : c.pourcentageEffectif;
            if (couverture.getAssureur() != null) {
                facture.setAssureur(couverture.getAssureur());
                pctAssureur = pct;
            } else if (couverture.getSociete() != null) {
                facture.setSociete(couverture.getSociete());
                pctSociete = pct;
            }
        }
        facture.setPourcentageAssureur(pctAssureur);
        facture.setPourcentageSociete(pctSociete);

        if (pctAssureur.add(pctSociete).compareTo(CENT) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les couvertures actives dépassent 100 % du montant (assureur "
                            + pctAssureur + " % + société " + pctSociete + " %).");
        }

        BigDecimal quoteAssureur = net.multiply(pctAssureur)
                .divide(CENT, 2, RoundingMode.HALF_UP);
        BigDecimal quoteSociete = net.multiply(pctSociete)
                .divide(CENT, 2, RoundingMode.HALF_UP);
        facture.setQuoteAssureur(quoteAssureur);
        facture.setQuoteSociete(quoteSociete);
        // Le patient absorbe les arrondis : la somme des quotes vaut le net
        facture.setQuotePatient(net.subtract(quoteAssureur).subtract(quoteSociete));
    }

    private String prochainNumero(LocalDate dateFacture) {
        String prefixe = "FA-" + dateFacture.getYear() + "-";
        int suivant = factureRepository.findTopByNumeroStartingWithOrderByNumeroDesc(prefixe)
                .map(f -> Integer.parseInt(f.getNumero().substring(prefixe.length())) + 1)
                .orElse(1);
        return prefixe + String.format("%04d", suivant);
    }

    private FactureDTO versDTOComplet(Facture facture) {
        FactureDTO dto = facture.versDTO();
        dto.paiements = new ArrayList<>(paiementRepository
                .findByFactureIdOrderByDatePaiementAscIdAsc(facture.getId()).stream()
                .map(Paiement::versDTO)
                .toList());
        return dto;
    }

    private Facture chercher(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Facture introuvable : " + id));
    }

    private void exigerStatut(Facture facture, String action, StatutFacture... attendus) {
        for (StatutFacture attendu : attendus) {
            if (facture.getStatut() == attendu) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Une facture au statut " + facture.getStatut() + " ne peut pas être " + action + ".");
    }

    private Long exiger(Long valeur, String champ) {
        if (valeur == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    champ + " est obligatoire.");
        }
        return valeur;
    }

    private String nettoyer(String valeur) {
        String v = valeur == null ? "" : valeur.trim();
        return v.isEmpty() ? null : v;
    }
}
