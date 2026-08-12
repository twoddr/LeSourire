package com.lesourire.client.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.lesourire.commun.Facturation.ModePaiement;
import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.dto.EncaissementDTO;
import com.lesourire.commun.dto.ImpayeDTO;

public class ServiceComptabiliteDemo implements ServiceComptabilite {

    private final List<EncaissementDTO> encaissements = new ArrayList<>();
    private final List<ImpayeDTO> impayes = new ArrayList<>();

    public ServiceComptabiliteDemo() {
        EncaissementDTO e1 = new EncaissementDTO();
        e1.id = 1L;
        e1.factureId = 10L;
        e1.factureNumero = "2026-00012";
        e1.patientNom = "Mbarga Paul";
        e1.datePaiement = LocalDate.now().atTime(9, 15);
        e1.montant = new BigDecimal("25000");
        e1.mode = ModePaiement.ESPECES;
        e1.payeur = Payeur.PATIENT;
        e1.recuParNom = "Secrétariat";
        encaissements.add(e1);

        EncaissementDTO e2 = new EncaissementDTO();
        e2.id = 2L;
        e2.factureId = 11L;
        e2.factureNumero = "2026-00008";
        e2.patientNom = "Ngo Marie";
        e2.datePaiement = LocalDate.now().atTime(11, 40);
        e2.montant = new BigDecimal("75000");
        e2.mode = ModePaiement.MOBILE_MONEY;
        e2.payeur = Payeur.ASSUREUR;
        e2.reference = "OM-45821";
        e2.recuParNom = "Secrétariat";
        encaissements.add(e2);

        ImpayeDTO i1 = new ImpayeDTO();
        i1.factureId = 12L;
        i1.factureNumero = "2026-00005";
        i1.dateFacture = LocalDate.now().minusDays(20);
        i1.dateEcheance = LocalDate.now().minusDays(5);
        i1.patientNom = "Essomba Jean";
        i1.payeur = Payeur.PATIENT;
        i1.solde = new BigDecimal("40000");
        impayes.add(i1);

        ImpayeDTO i2 = new ImpayeDTO();
        i2.factureId = 13L;
        i2.factureNumero = "2026-00003";
        i2.dateFacture = LocalDate.now().minusDays(35);
        i2.dateEcheance = LocalDate.now().minusDays(10);
        i2.patientNom = "Fouda Claire";
        i2.payeur = Payeur.SOCIETE;
        i2.payeurNom = "CSA Soft";
        i2.solde = new BigDecimal("120000");
        impayes.add(i2);
    }

    @Override
    public List<EncaissementDTO> encaissements(LocalDate date) throws Exception {
        LocalDate jour = date == null ? LocalDate.now() : date;
        return journal(jour, jour);
    }

    @Override
    public List<EncaissementDTO> journal(LocalDate debut, LocalDate fin) throws Exception {
        return encaissements.stream()
                .filter(e -> {
                    LocalDate d = e.datePaiement.toLocalDate();
                    return !d.isBefore(debut) && !d.isAfter(fin);
                })
                .toList();
    }

    @Override
    public List<ImpayeDTO> impayes() throws Exception {
        return List.copyOf(impayes);
    }
}
