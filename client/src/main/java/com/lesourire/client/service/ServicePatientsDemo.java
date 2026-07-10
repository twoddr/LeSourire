package com.lesourire.client.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.client.coeur.ApiClient.ApiException;
import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.SocieteDTO;

/**
 * Implémentation en mémoire pour le mode démonstration : permet de présenter
 * le module Patients (recherche, création, couvertures) sans serveur.
 * Rien n'est persisté.
 */
public class ServicePatientsDemo implements ServicePatients {

    private final AtomicLong sequence = new AtomicLong(100);
    private final List<PatientDTO> patients = new ArrayList<>();
    private final List<AssureurDTO> assureurs = new ArrayList<>();
    private final List<SocieteDTO> societes = new ArrayList<>();

    public ServicePatientsDemo() {
        assureurs.add(new AssureurDTO(1L, "Assurance Alpha", null, null, new BigDecimal("70"), true));
        assureurs.add(new AssureurDTO(2L, "Mutuelle Bêta", null, null, new BigDecimal("80"), true));
        societes.add(new SocieteDTO(1L, "Société Gamma", null, null, new BigDecimal("50"), true));

        PatientDTO p1 = exemple("NGONO", "Marie", "1985-04-12", "F", "699 11 22 33");
        ajouterCouvertureInterne(p1, CouvertureDTO.TYPE_ASSUREUR, 1L, "Assurance Alpha",
                new BigDecimal("70"), "AA-1024");
        PatientDTO p2 = exemple("MBALLA", "Jean", "1972-11-03", "M", "677 44 55 66");
        PatientDTO p3 = exemple("FOTSO", "Claire", "1998-07-25", "F", "655 77 88 99");
        ajouterCouvertureInterne(p3, CouvertureDTO.TYPE_SOCIETE, 1L, "Société Gamma",
                new BigDecimal("50"), null);
        patients.add(p1);
        patients.add(p2);
        patients.add(p3);
    }

    private PatientDTO exemple(String nom, String prenom, String naissance, String sexe,
            String telephone) {
        PatientDTO p = new PatientDTO();
        p.id = sequence.incrementAndGet();
        p.numeroDossier = String.format("P-%06d", p.id);
        p.nom = nom;
        p.prenom = prenom;
        p.dateNaissance = LocalDate.parse(naissance);
        p.sexe = sexe;
        p.telephone = telephone;
        p.ville = "Douala";
        return p;
    }

    private void ajouterCouvertureInterne(PatientDTO patient, String type, Long payeurId,
            String payeurNom, BigDecimal pourcentageEffectif, String numeroAssure) {
        CouvertureDTO c = new CouvertureDTO();
        c.id = sequence.incrementAndGet();
        c.patientId = patient.id;
        c.type = type;
        c.payeurId = payeurId;
        c.payeurNom = payeurNom;
        c.pourcentageEffectif = pourcentageEffectif;
        c.numeroAssure = numeroAssure;
        c.dateDebut = LocalDate.now().minusYears(1);
        patient.couvertures = new ArrayList<>(patient.couvertures);
        patient.couvertures.add(c);
        rafraichirNomsActifs(patient);
    }

    private void rafraichirNomsActifs(PatientDTO patient) {
        patient.assureurActifNom = null;
        patient.societeActiveNom = null;
        for (CouvertureDTO c : patient.couvertures) {
            if (!c.estEnCours()) {
                continue;
            }
            if (CouvertureDTO.TYPE_ASSUREUR.equals(c.type)) {
                patient.assureurActifNom = c.payeurNom;
            } else {
                patient.societeActiveNom = c.payeurNom;
            }
        }
    }

    @Override
    public List<PatientDTO> rechercher(String recherche) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return patients.stream()
                .filter(p -> p.actif)
                .filter(p -> q.isEmpty()
                        || contient(p.nom, q) || contient(p.prenom, q)
                        || contient(p.numeroDossier, q) || contient(p.telephone, q))
                .sorted(Comparator.comparing((PatientDTO p) -> p.nom == null ? "" : p.nom)
                        .thenComparing(p -> p.prenom == null ? "" : p.prenom))
                .toList();
    }

    private boolean contient(String valeur, String q) {
        return valeur != null && valeur.toLowerCase(Locale.FRENCH).contains(q);
    }

    @Override
    public PatientDTO obtenir(Long id) throws ApiException {
        return patients.stream().filter(p -> p.id.equals(id)).findFirst()
                .orElseThrow(() -> new ApiException("Patient introuvable."));
    }

    @Override
    public PatientDTO creer(PatientDTO patient) throws ApiException {
        patient.id = sequence.incrementAndGet();
        patient.numeroDossier = String.format("P-%06d", patient.id);
        List<CouvertureDTO> aAjouter = patient.couvertures;
        patient.couvertures = new ArrayList<>();
        patients.add(patient);
        for (CouvertureDTO c : aAjouter) {
            ajouterCouverture(patient.id, c);
        }
        return patient;
    }

    @Override
    public PatientDTO modifier(PatientDTO patient) throws ApiException {
        PatientDTO existant = obtenir(patient.id);
        patient.couvertures = existant.couvertures;
        patients.removeIf(p -> p.id.equals(patient.id));
        patients.add(patient);
        rafraichirNomsActifs(patient);
        return patient;
    }

    @Override
    public CouvertureDTO ajouterCouverture(Long patientId, CouvertureDTO couverture)
            throws ApiException {
        PatientDTO patient = obtenir(patientId);
        if (couverture.dateDebut == null) {
            couverture.dateDebut = LocalDate.now();
        }
        // Simule le contrôle de chevauchement du serveur
        boolean conflit = patient.couvertures.stream()
                .filter(c -> c.type.equals(couverture.type))
                .anyMatch(c -> (c.dateFin == null || !c.dateFin.isBefore(couverture.dateDebut))
                        && (couverture.dateFin == null || !couverture.dateFin.isBefore(c.dateDebut)));
        if (conflit) {
            throw new ApiException("Ce patient a déjà une couverture de ce type sur cette période. "
                    + "Clôturez d'abord la couverture en cours.");
        }
        couverture.id = sequence.incrementAndGet();
        couverture.patientId = patientId;
        if (couverture.payeurNom == null) {
            couverture.payeurNom = nomPayeur(couverture);
        }
        if (couverture.pourcentageEffectif == null) {
            couverture.pourcentageEffectif = couverture.pourcentage != null
                    ? couverture.pourcentage
                    : pourcentageDefautPayeur(couverture);
        }
        patient.couvertures = new ArrayList<>(patient.couvertures);
        patient.couvertures.add(0, couverture);
        rafraichirNomsActifs(patient);
        return couverture;
    }

    private String nomPayeur(CouvertureDTO c) {
        if (CouvertureDTO.TYPE_ASSUREUR.equals(c.type)) {
            return assureurs.stream().filter(a -> a.id().equals(c.payeurId))
                    .map(AssureurDTO::nom).findFirst().orElse("?");
        }
        return societes.stream().filter(s -> s.id().equals(c.payeurId))
                .map(SocieteDTO::nom).findFirst().orElse("?");
    }

    private BigDecimal pourcentageDefautPayeur(CouvertureDTO c) {
        if (CouvertureDTO.TYPE_ASSUREUR.equals(c.type)) {
            return assureurs.stream().filter(a -> a.id().equals(c.payeurId))
                    .map(AssureurDTO::pourcentageDefaut).findFirst().orElse(BigDecimal.ZERO);
        }
        return societes.stream().filter(s -> s.id().equals(c.payeurId))
                .map(SocieteDTO::pourcentageDefaut).findFirst().orElse(BigDecimal.ZERO);
    }

    @Override
    public CouvertureDTO cloturerCouverture(Long patientId, Long couvertureId,
            LocalDate dateFin, String motifFin) throws ApiException {
        PatientDTO patient = obtenir(patientId);
        CouvertureDTO couverture = patient.couvertures.stream()
                .filter(c -> c.id.equals(couvertureId)).findFirst()
                .orElseThrow(() -> new ApiException("Couverture introuvable."));
        couverture.dateFin = dateFin != null ? dateFin : LocalDate.now();
        couverture.motifFin = motifFin;
        rafraichirNomsActifs(patient);
        return couverture;
    }

    @Override
    public List<AssureurDTO> listerAssureurs() {
        return List.copyOf(assureurs);
    }

    @Override
    public AssureurDTO creerAssureur(String nom, BigDecimal pourcentageDefaut) {
        AssureurDTO a = new AssureurDTO(sequence.incrementAndGet(), nom, null, null,
                pourcentageDefaut, true);
        assureurs.add(a);
        return a;
    }

    @Override
    public List<SocieteDTO> listerSocietes() {
        return List.copyOf(societes);
    }

    @Override
    public SocieteDTO creerSociete(String nom, BigDecimal pourcentageDefaut) {
        SocieteDTO s = new SocieteDTO(sequence.incrementAndGet(), nom, null, null,
                pourcentageDefaut, true);
        societes.add(s);
        return s;
    }
}
