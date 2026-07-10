package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Couverture d'un patient par un tiers payant (assureur ou société),
 * valable sur une période. L'historique complet est conservé :
 * on ne modifie jamais une couverture, on la clôture et on en crée une autre.
 */
public class CouvertureDTO {

    public static final String TYPE_ASSUREUR = "ASSUREUR";
    public static final String TYPE_SOCIETE = "SOCIETE";

    public Long id;
    public Long patientId;
    public String type;                 // ASSUREUR ou SOCIETE
    public Long payeurId;               // id de l'assureur ou de la société
    public String payeurNom;            // renseigné par le serveur (affichage)
    public String numeroAssure;         // pertinent pour un assureur
    public BigDecimal pourcentage;      // NULL = pourcentage par défaut du payeur
    public BigDecimal pourcentageEffectif;  // renseigné par le serveur (affichage)
    public LocalDate dateDebut;
    public LocalDate dateFin;           // NULL = en cours
    public String motifFin;

    public boolean estEnCours() {
        LocalDate aujourdHui = LocalDate.now();
        return (dateDebut == null || !dateDebut.isAfter(aujourdHui))
                && (dateFin == null || !dateFin.isBefore(aujourdHui));
    }
}
