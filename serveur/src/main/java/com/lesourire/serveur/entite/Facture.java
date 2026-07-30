package com.lesourire.serveur.entite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.FactureLigneDTO;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Facture d'un patient.
 * Les colonnes paye_* sont maintenues par les triggers de la base à chaque
 * paiement, et les solde_* sont des colonnes générées : elles sont mappées en
 * lecture seule. Après un encaissement, recharger l'entité pour les voir.
 */
@Entity
@Table(name = "facture")
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_patient")
    private Patient patient;

    @Column(name = "date_facture", nullable = false)
    private LocalDate dateFacture;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "montant_brut", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantBrut = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal remise = BigDecimal.ZERO;

    @Column(name = "montant_net", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantNet = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_assureur")
    private Assureur assureur;

    @Column(name = "pourcentage_assureur", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentageAssureur = BigDecimal.ZERO;

    @Column(name = "quote_assureur", nullable = false, precision = 12, scale = 2)
    private BigDecimal quoteAssureur = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_societe")
    private Societe societe;

    @Column(name = "pourcentage_societe", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentageSociete = BigDecimal.ZERO;

    @Column(name = "quote_societe", nullable = false, precision = 12, scale = 2)
    private BigDecimal quoteSociete = BigDecimal.ZERO;

    @Column(name = "quote_patient", nullable = false, precision = 12, scale = 2)
    private BigDecimal quotePatient = BigDecimal.ZERO;

    // Maintenus par les triggers de la base (lecture seule côté application)
    @Column(name = "paye_patient", insertable = false, updatable = false)
    private BigDecimal payePatient;

    @Column(name = "paye_assureur", insertable = false, updatable = false)
    private BigDecimal payeAssureur;

    @Column(name = "paye_societe", insertable = false, updatable = false)
    private BigDecimal payeSociete;

    // Colonnes générées par la base (quote - payé)
    @Column(name = "solde_patient", insertable = false, updatable = false)
    private BigDecimal soldePatient;

    @Column(name = "solde_assureur", insertable = false, updatable = false)
    private BigDecimal soldeAssureur;

    @Column(name = "solde_societe", insertable = false, updatable = false)
    private BigDecimal soldeSociete;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutFacture statut = StatutFacture.BROUILLON;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par")
    private Utilisateur creePar;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<FactureLigne> lignes = new ArrayList<>();

    /** DTO allégé pour les listes (sans lignes ni paiements). */
    public FactureDTO versDTOResume() {
        FactureDTO dto = new FactureDTO();
        dto.id = id;
        dto.numero = numero;
        dto.patientId = patient.getId();
        dto.patientNom = patient.nomComplet();
        dto.patientNumeroDossier = patient.getNumeroDossier();
        dto.dateFacture = dateFacture;
        dto.dateEcheance = dateEcheance;
        dto.montantBrut = montantBrut;
        dto.remise = remise;
        dto.montantNet = montantNet;
        if (assureur != null) {
            dto.assureurId = assureur.getId();
            dto.assureurNom = assureur.getNom();
        }
        dto.pourcentageAssureur = pourcentageAssureur;
        dto.quoteAssureur = quoteAssureur;
        if (societe != null) {
            dto.societeId = societe.getId();
            dto.societeNom = societe.getNom();
        }
        dto.pourcentageSociete = pourcentageSociete;
        dto.quoteSociete = quoteSociete;
        dto.quotePatient = quotePatient;
        dto.payePatient = zeroSiNull(payePatient);
        dto.payeAssureur = zeroSiNull(payeAssureur);
        dto.payeSociete = zeroSiNull(payeSociete);
        dto.soldePatient = zeroSiNull(soldePatient);
        dto.soldeAssureur = zeroSiNull(soldeAssureur);
        dto.soldeSociete = zeroSiNull(soldeSociete);
        dto.statut = statut;
        dto.notes = notes;
        return dto;
    }

    /** DTO complet, lignes incluses (les paiements sont ajoutés par le service). */
    public FactureDTO versDTO() {
        FactureDTO dto = versDTOResume();
        dto.lignes = lignes.stream().map(FactureLigne::versDTO)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return dto;
    }

    private static BigDecimal zeroSiNull(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDate getDateFacture() {
        return dateFacture;
    }

    public void setDateFacture(LocalDate dateFacture) {
        this.dateFacture = dateFacture;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontantNet() {
        return montantNet;
    }

    public void setMontantBrut(BigDecimal montantBrut) {
        this.montantBrut = montantBrut;
    }

    public void setRemise(BigDecimal remise) {
        this.remise = remise;
    }

    public void setMontantNet(BigDecimal montantNet) {
        this.montantNet = montantNet;
    }

    public void setAssureur(Assureur assureur) {
        this.assureur = assureur;
    }

    public void setPourcentageAssureur(BigDecimal pourcentageAssureur) {
        this.pourcentageAssureur = pourcentageAssureur;
    }

    public BigDecimal getQuoteAssureur() {
        return quoteAssureur;
    }

    public void setQuoteAssureur(BigDecimal quoteAssureur) {
        this.quoteAssureur = quoteAssureur;
    }

    public void setSociete(Societe societe) {
        this.societe = societe;
    }

    public void setPourcentageSociete(BigDecimal pourcentageSociete) {
        this.pourcentageSociete = pourcentageSociete;
    }

    public BigDecimal getQuoteSociete() {
        return quoteSociete;
    }

    public void setQuoteSociete(BigDecimal quoteSociete) {
        this.quoteSociete = quoteSociete;
    }

    public BigDecimal getQuotePatient() {
        return quotePatient;
    }

    public void setQuotePatient(BigDecimal quotePatient) {
        this.quotePatient = quotePatient;
    }

    public BigDecimal getSoldePatient() {
        return zeroSiNull(soldePatient);
    }

    public BigDecimal getSoldeAssureur() {
        return zeroSiNull(soldeAssureur);
    }

    public BigDecimal getSoldeSociete() {
        return zeroSiNull(soldeSociete);
    }

    public StatutFacture getStatut() {
        return statut;
    }

    public void setStatut(StatutFacture statut) {
        this.statut = statut;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreePar(Utilisateur creePar) {
        this.creePar = creePar;
    }

    public List<FactureLigne> getLignes() {
        return lignes;
    }
}
