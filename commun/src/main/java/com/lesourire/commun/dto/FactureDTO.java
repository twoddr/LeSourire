package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.lesourire.commun.Facturation.StatutFacture;

/**
 * Facture d'un patient. Les montants (brut, net, quotes-parts, payés, soldes)
 * sont calculés et maintenus par le serveur et la base : le client ne fait
 * que les afficher. Les pourcentages des tiers payants sont figés au moment
 * de la facture, indépendamment des changements de couverture ultérieurs.
 */
public class FactureDTO {

    public Long id;
    public String numero;
    public Long patientId;
    public String patientNom;           // renseigné par le serveur (affichage)
    public String patientNumeroDossier; // renseigné par le serveur (affichage)
    public LocalDate dateFacture;
    public LocalDate dateEcheance;

    /** Praticien à qui imputer les actes (utilisé à la création uniquement). */
    public Long praticienId;

    public List<FactureLigneDTO> lignes = new ArrayList<>();

    public BigDecimal montantBrut = BigDecimal.ZERO;
    public BigDecimal remise = BigDecimal.ZERO;
    public BigDecimal montantNet = BigDecimal.ZERO;

    // Répartition figée au moment de la facture
    public Long assureurId;
    public String assureurNom;
    public BigDecimal pourcentageAssureur = BigDecimal.ZERO;
    public BigDecimal quoteAssureur = BigDecimal.ZERO;
    public Long societeId;
    public String societeNom;
    public BigDecimal pourcentageSociete = BigDecimal.ZERO;
    public BigDecimal quoteSociete = BigDecimal.ZERO;
    public BigDecimal quotePatient = BigDecimal.ZERO;

    // Suivi des paiements (maintenu par la base)
    public BigDecimal payePatient = BigDecimal.ZERO;
    public BigDecimal payeAssureur = BigDecimal.ZERO;
    public BigDecimal payeSociete = BigDecimal.ZERO;
    public BigDecimal soldePatient = BigDecimal.ZERO;
    public BigDecimal soldeAssureur = BigDecimal.ZERO;
    public BigDecimal soldeSociete = BigDecimal.ZERO;

    public StatutFacture statut = StatutFacture.BROUILLON;
    public String notes;

    public List<PaiementDTO> paiements = new ArrayList<>();

    public BigDecimal totalPaye() {
        return payePatient.add(payeAssureur).add(payeSociete);
    }

    public BigDecimal soldeTotal() {
        return soldePatient.add(soldeAssureur).add(soldeSociete);
    }
}
