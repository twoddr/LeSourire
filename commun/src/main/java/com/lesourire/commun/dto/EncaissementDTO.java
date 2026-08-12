package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.Facturation.Payeur;

/** Paiement enrichi pour les écrans de comptabilité (journal / caisse du jour). */
public class EncaissementDTO {

    public Long id;
    public Long factureId;
    public String factureNumero;
    public String patientNom;
    public LocalDateTime datePaiement;
    public BigDecimal montant;
    public ModePaiement mode;
    public Payeur payeur;
    public String reference;
    public String recuParNom;
    public String notes;
}
