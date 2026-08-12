package com.lesourire.commun.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.lesourire.commun.Facturation.Payeur;

/** Ligne de relance : solde restant dû par un payeur sur une facture. */
public class ImpayeDTO {

    public Long factureId;
    public String factureNumero;
    public LocalDate dateFacture;
    public LocalDate dateEcheance;
    public String patientNom;
    public Payeur payeur;
    public String payeurNom; // assureur / société ; null pour le patient
    public BigDecimal solde;
}
