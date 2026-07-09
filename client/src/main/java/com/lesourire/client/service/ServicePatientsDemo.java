package com.lesourire.client.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.dto.AssureurDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.commun.dto.SocieteDTO;

/**
 * Implémentation en mémoire pour le mode démonstration : permet de présenter
 * le module Patients (recherche, création, modification) sans serveur.
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

        patients.add(exemple("NGONO", "Marie", "1985-04-12", "F", "699 11 22 33", 1L, "Assurance Alpha"));
        patients.add(exemple("MBALLA", "Jean", "1972-11-03", "M", "677 44 55 66", null, null));
        patients.add(exemple("FOTSO", "Claire", "1998-07-25", "F", "655 77 88 99", 2L, "Mutuelle Bêta"));
    }

    private PatientDTO exemple(String nom, String prenom, String naissance, String sexe,
            String telephone, Long assureurId, String assureurNom) {
        PatientDTO p = new PatientDTO();
        p.id = sequence.incrementAndGet();
        p.numeroDossier = String.format("P-%06d", p.id);
        p.nom = nom;
        p.prenom = prenom;
        p.dateNaissance = LocalDate.parse(naissance);
        p.sexe = sexe;
        p.telephone = telephone;
        p.ville = "Douala";
        p.assureurId = assureurId;
        p.assureurNom = assureurNom;
        return p;
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
    public PatientDTO creer(PatientDTO patient) {
        patient.id = sequence.incrementAndGet();
        patient.numeroDossier = String.format("P-%06d", patient.id);
        renseignerNoms(patient);
        patients.add(patient);
        return patient;
    }

    @Override
    public PatientDTO modifier(PatientDTO patient) {
        patients.removeIf(p -> p.id.equals(patient.id));
        renseignerNoms(patient);
        patients.add(patient);
        return patient;
    }

    private void renseignerNoms(PatientDTO patient) {
        patient.assureurNom = assureurs.stream()
                .filter(a -> a.id().equals(patient.assureurId))
                .map(AssureurDTO::nom).findFirst().orElse(null);
        patient.societeNom = societes.stream()
                .filter(s -> s.id().equals(patient.societeId))
                .map(SocieteDTO::nom).findFirst().orElse(null);
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
