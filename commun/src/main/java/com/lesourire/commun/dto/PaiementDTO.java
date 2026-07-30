package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.Facturation.Payeur;

/** Paiement encaissé sur une facture, imputé à un payeur précis. */
public class PaiementDTO {

    public Long id;
    public Long factureId;
    public LocalDateTime datePaiement;
    public BigDecimal montant;
    public ModePaiement mode;
    public Payeur payeur = Payeur.PATIENT;
    public String reference;            // n° de chèque, référence de virement...
    public String recuParNom;           // renseigné par le serveur (affichage)
    public String notes;
}
