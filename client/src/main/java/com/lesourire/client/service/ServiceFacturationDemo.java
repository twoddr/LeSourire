package com.lesourire.client.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.client.coeur.ApiClient.ApiException;
import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.Role;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.FactureLigneDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;

/**
 * Implémentation en mémoire pour le mode démonstration.
 * Reproduit les règles principales du serveur (statuts, soldes par payeur)
 * sans persistance.
 */
public class ServiceFacturationDemo implements ServiceFacturation {

    private static final BigDecimal CENT = new BigDecimal("100");

    private final AtomicLong sequence = new AtomicLong(500);
    private final List<FactureDTO> factures = new ArrayList<>();
    private final List<PrestationDTO> prestations = new ArrayList<>();

    public ServiceFacturationDemo() {
        prestations.add(prestation(1L, "CONS", "Consultation", null, null, new BigDecimal("15000")));
        prestations.add(prestation(2L, "DET", "Détartrage complet", "Z", new BigDecimal("12"), null));
        prestations.add(prestation(3L, "EXT-S", "Extraction simple", "D", new BigDecimal("10"), null));
        prestations.add(prestation(4L, "OBT-C", "Obturation composite", "D", new BigDecimal("15"), null));

        FactureDTO exemple = new FactureDTO();
        exemple.id = sequence.incrementAndGet();
        exemple.numero = "FA-" + LocalDate.now().getYear() + "-0001";
        exemple.patientId = 101L;
        exemple.patientNom = "NGONO Marie";
        exemple.patientNumeroDossier = "P-000101";
        exemple.dateFacture = LocalDate.now().minusDays(6);
        FactureLigneDTO ligne = new FactureLigneDTO();
        ligne.prestationId = 2L;
        ligne.prestationCode = "DET";
        ligne.designation = "Détartrage complet";
        ligne.quantite = 1;
        ligne.prixUnitaire = new BigDecimal("14400");
        ligne.montant = new BigDecimal("14400");
        exemple.lignes.add(ligne);
        exemple.montantBrut = new BigDecimal("14400");
        exemple.montantNet = new BigDecimal("14400");
        exemple.assureurNom = "Assurance Alpha";
        exemple.pourcentageAssureur = new BigDecimal("70");
        exemple.quoteAssureur = new BigDecimal("10080");
        exemple.quotePatient = new BigDecimal("4320");
        exemple.soldeAssureur = exemple.quoteAssureur;
        exemple.soldePatient = exemple.quotePatient;
        exemple.statut = StatutFacture.EMISE;
        factures.add(exemple);
    }

    private PrestationDTO prestation(Long id, String code, String libelle, String lettre,
            BigDecimal coefficient, BigDecimal forfait) {
        PrestationDTO p = new PrestationDTO();
        p.id = id;
        p.code = code;
        p.libelle = libelle;
        p.lettreCle = lettre;
        p.coefficient = coefficient;
        p.tarifForfait = forfait;
        return p;
    }

    @Override
    public List<FactureDTO> rechercher(String recherche, StatutFacture statut) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return factures.stream()
                .filter(f -> statut == null || f.statut == statut)
                .filter(f -> q.isEmpty()
                        || f.numero.toLowerCase(Locale.FRENCH).contains(q)
                        || (f.patientNom != null
                                && f.patientNom.toLowerCase(Locale.FRENCH).contains(q)))
                .sorted((a, b) -> b.dateFacture.compareTo(a.dateFacture))
                .toList();
    }

    @Override
    public FactureDTO obtenir(Long id) throws ApiException {
        return factures.stream().filter(f -> f.id.equals(id)).findFirst()
                .orElseThrow(() -> new ApiException("Facture introuvable."));
    }

    @Override
    public FactureDTO creer(FactureDTO facture) {
        facture.id = sequence.incrementAndGet();
        facture.numero = "FA-" + LocalDate.now().getYear() + "-"
                + String.format("%04d", factures.size() + 1);
        if (facture.dateFacture == null) {
            facture.dateFacture = LocalDate.now();
        }
        facture.statut = StatutFacture.BROUILLON;
        recalculer(facture);
        factures.add(facture);
        return facture;
    }

    @Override
    public FactureDTO modifier(Long id, FactureDTO facture) throws ApiException {
        FactureDTO existante = obtenir(id);
        if (existante.statut != StatutFacture.BROUILLON) {
            throw new ApiException("Seul un brouillon peut être modifié.");
        }
        existante.lignes = facture.lignes;
        existante.remise = facture.remise;
        existante.notes = facture.notes;
        existante.dateFacture = facture.dateFacture;
        existante.dateEcheance = facture.dateEcheance;
        recalculer(existante);
        return existante;
    }

    private void recalculer(FactureDTO f) {
        BigDecimal brut = f.lignes.stream()
                .map(l -> l.montant == null ? BigDecimal.ZERO : l.montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        f.montantBrut = brut;
        if (f.remise == null) {
            f.remise = BigDecimal.ZERO;
        }
        f.montantNet = brut.subtract(f.remise);
        f.quoteAssureur = f.montantNet.multiply(f.pourcentageAssureur)
                .divide(CENT, 2, RoundingMode.HALF_UP);
        f.quoteSociete = f.montantNet.multiply(f.pourcentageSociete)
                .divide(CENT, 2, RoundingMode.HALF_UP);
        f.quotePatient = f.montantNet.subtract(f.quoteAssureur).subtract(f.quoteSociete);
        rafraichirSoldes(f);
    }

    private void rafraichirSoldes(FactureDTO f) {
        f.payePatient = totalPaye(f, Payeur.PATIENT);
        f.payeAssureur = totalPaye(f, Payeur.ASSUREUR);
        f.payeSociete = totalPaye(f, Payeur.SOCIETE);
        f.soldePatient = f.quotePatient.subtract(f.payePatient);
        f.soldeAssureur = f.quoteAssureur.subtract(f.payeAssureur);
        f.soldeSociete = f.quoteSociete.subtract(f.payeSociete);
        if (f.statut == StatutFacture.EMISE || f.statut == StatutFacture.PARTIELLEMENT_PAYEE
                || f.statut == StatutFacture.PAYEE) {
            BigDecimal paye = f.totalPaye();
            f.statut = paye.compareTo(BigDecimal.ZERO) <= 0 ? StatutFacture.EMISE
                    : paye.compareTo(f.montantNet) < 0 ? StatutFacture.PARTIELLEMENT_PAYEE
                            : StatutFacture.PAYEE;
        }
    }

    private BigDecimal totalPaye(FactureDTO f, Payeur payeur) {
        return f.paiements.stream()
                .filter(p -> p.payeur == payeur)
                .map(p -> p.montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public FactureDTO emettre(Long id) throws ApiException {
        FactureDTO f = obtenir(id);
        if (f.statut != StatutFacture.BROUILLON) {
            throw new ApiException("Seul un brouillon peut être émis.");
        }
        f.statut = StatutFacture.EMISE;
        return f;
    }

    @Override
    public FactureDTO annuler(Long id) throws ApiException {
        FactureDTO f = obtenir(id);
        if (!f.paiements.isEmpty()) {
            throw new ApiException("Des paiements sont déjà enregistrés sur cette facture.");
        }
        f.statut = StatutFacture.ANNULEE;
        return f;
    }

    @Override
    public FactureDTO encaisser(Long id, PaiementDTO paiement) throws ApiException {
        FactureDTO f = obtenir(id);
        if (f.statut != StatutFacture.EMISE && f.statut != StatutFacture.PARTIELLEMENT_PAYEE) {
            throw new ApiException("Cette facture ne peut pas être encaissée (statut "
                    + f.statut + ").");
        }
        paiement.id = sequence.incrementAndGet();
        paiement.factureId = id;
        if (paiement.datePaiement == null) {
            paiement.datePaiement = LocalDateTime.now();
        }
        paiement.recuParNom = "Démo";
        f.paiements.add(paiement);
        rafraichirSoldes(f);
        return f;
    }

    @Override
    public FactureDTO supprimerPaiement(Long id, Long paiementId) throws ApiException {
        FactureDTO f = obtenir(id);
        f.paiements.removeIf(p -> p.id.equals(paiementId));
        rafraichirSoldes(f);
        return f;
    }

    @Override
    public List<PrestationDTO> prestations(String recherche) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return prestations.stream()
                .filter(p -> q.isEmpty()
                        || p.libelle.toLowerCase(Locale.FRENCH).contains(q)
                        || p.code.toLowerCase(Locale.FRENCH).contains(q))
                .toList();
    }

    @Override
    public List<ValeurLettreCleDTO> valeursLettres() {
        return List.of(
                new ValeurLettreCleDTO(1L, "D", new BigDecimal("1200"), LocalDate.of(2026, 1, 1), null),
                new ValeurLettreCleDTO(2L, "Z", new BigDecimal("1200"), LocalDate.of(2026, 1, 1), null));
    }

    @Override
    public List<UtilisateurDTO> praticiens() {
        return List.of(new UtilisateurDTO(1L, "nadine", "TOWE", "Nadine",
                Role.DENTISTE, null, null, true));
    }
}
