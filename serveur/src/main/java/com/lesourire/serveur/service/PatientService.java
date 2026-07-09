package com.lesourire.serveur.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.serveur.entite.Assureur;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.Societe;
import com.lesourire.serveur.repository.AssureurRepository;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.SocieteRepository;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final AssureurRepository assureurRepository;
    private final SocieteRepository societeRepository;
    private final AuditService auditService;

    public PatientService(PatientRepository patientRepository,
            AssureurRepository assureurRepository,
            SocieteRepository societeRepository,
            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.assureurRepository = assureurRepository;
        this.societeRepository = societeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PatientDTO> rechercher(String recherche) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return patientRepository.rechercher(q).stream()
                .map(Patient::versDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientDTO obtenir(Long id) {
        return chercher(id).versDTO();
    }

    public PatientDTO creer(PatientDTO dto, String nomUtilisateur) {
        valider(dto);
        Patient patient = new Patient();
        patient.appliquer(dto, resoudreAssureur(dto), resoudreSociete(dto));
        patient.setCreePar(auditService.utilisateurCourant(nomUtilisateur));
        // Numéro provisoire unique le temps d'obtenir l'id auto-généré
        patient.setNumeroDossier("EN-COURS");
        patient = patientRepository.saveAndFlush(patient);
        patient.setNumeroDossier(String.format("P-%06d", patient.getId()));

        auditService.enregistrer(nomUtilisateur, "CREATION", "patient", patient.getId(),
                "Création du dossier " + patient.getNumeroDossier() + " (" + patient.getNom() + ")");
        return patient.versDTO();
    }

    public PatientDTO modifier(Long id, PatientDTO dto, String nomUtilisateur) {
        valider(dto);
        Patient patient = chercher(id);
        patient.appliquer(dto, resoudreAssureur(dto), resoudreSociete(dto));

        auditService.enregistrer(nomUtilisateur, "MODIFICATION", "patient", patient.getId(),
                "Modification du dossier " + patient.getNumeroDossier());
        return patient.versDTO();
    }

    private Patient chercher(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient introuvable : " + id));
    }

    private void valider(PatientDTO dto) {
        if (dto.nom == null || dto.nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom du patient est obligatoire.");
        }
    }

    private Assureur resoudreAssureur(PatientDTO dto) {
        if (dto.assureurId == null) {
            return null;
        }
        return assureurRepository.findById(dto.assureurId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Assureur inconnu : " + dto.assureurId));
    }

    private Societe resoudreSociete(PatientDTO dto) {
        if (dto.societeId == null) {
            return null;
        }
        return societeRepository.findById(dto.societeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Société inconnue : " + dto.societeId));
    }
}
