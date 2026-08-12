package com.lesourire.serveur.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.Facturation.Payeur;
import com.lesourire.commun.dto.EncaissementDTO;
import com.lesourire.commun.dto.ImpayeDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.serveur.entite.Paiement;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.repository.FactureRepository;
import com.lesourire.serveur.repository.PaiementRepository;

@Service
@Transactional(readOnly = true)
public class ComptabiliteService {

    private final PaiementRepository paiementRepository;
    private final FactureRepository factureRepository;

    public ComptabiliteService(PaiementRepository paiementRepository,
            FactureRepository factureRepository) {
        this.paiementRepository = paiementRepository;
        this.factureRepository = factureRepository;
    }

    public List<EncaissementDTO> encaissementsDuJour(LocalDate date) {
        LocalDate jour = date == null ? LocalDate.now() : date;
        return journal(jour, jour);
    }

    public List<EncaissementDTO> journal(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les dates de début et de fin sont obligatoires.");
        }
        if (fin.isBefore(debut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être postérieure ou égale à la date de début.");
        }
        LocalDateTime debutDt = debut.atStartOfDay();
        LocalDateTime finDt = fin.plusDays(1).atStartOfDay();
        return paiementRepository.findEntre(debutDt, finDt).stream()
                .map(this::versEncaissement)
                .toList();
    }

    public List<ImpayeDTO> impayes() {
        List<ImpayeDTO> resultat = new ArrayList<>();
        for (Object[] ligne : factureRepository.listerRelances()) {
            ImpayeDTO dto = new ImpayeDTO();
            dto.factureId = ((Number) ligne[0]).longValue();
            dto.factureNumero = (String) ligne[1];
            dto.dateFacture = toLocalDate(ligne[2]);
            dto.dateEcheance = toLocalDate(ligne[3]);
            String nom = (String) ligne[4];
            String prenom = (String) ligne[5];
            dto.patientNom = (nom + " " + (prenom == null ? "" : prenom)).trim();
            dto.payeur = Payeur.valueOf((String) ligne[6]);
            dto.payeurNom = (String) ligne[7];
            dto.solde = (BigDecimal) ligne[8];
            resultat.add(dto);
        }
        return resultat;
    }

    private EncaissementDTO versEncaissement(Paiement p) {
        PaiementDTO base = p.versDTO();
        EncaissementDTO dto = new EncaissementDTO();
        dto.id = base.id;
        dto.factureId = base.factureId;
        dto.factureNumero = p.getFacture().getNumero();
        Patient patient = p.getFacture().getPatient();
        dto.patientNom = (patient.getNom() + " "
                + (patient.getPrenom() == null ? "" : patient.getPrenom())).trim();
        dto.datePaiement = base.datePaiement;
        dto.montant = base.montant;
        dto.mode = base.mode;
        dto.payeur = base.payeur;
        dto.reference = base.reference;
        dto.recuParNom = base.recuParNom;
        dto.notes = base.notes;
        return dto;
    }

    private static LocalDate toLocalDate(Object valeur) {
        if (valeur == null) {
            return null;
        }
        if (valeur instanceof LocalDate ld) {
            return ld;
        }
        if (valeur instanceof Date d) {
            return d.toLocalDate();
        }
        if (valeur instanceof java.util.Date d) {
            return new Date(d.getTime()).toLocalDate();
        }
        throw new IllegalArgumentException("Type de date inattendu : " + valeur.getClass());
    }
}
