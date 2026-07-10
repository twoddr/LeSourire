package com.lesourire.serveur.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.ClotureCouvertureDTO;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.PatientCouverture;
import com.lesourire.serveur.repository.AssureurRepository;
import com.lesourire.serveur.repository.PatientCouvertureRepository;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.SocieteRepository;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientCouvertureRepository couvertureRepository;
    private final AssureurRepository assureurRepository;
    private final SocieteRepository societeRepository;
    private final AuditService auditService;

    public PatientService(PatientRepository patientRepository,
            PatientCouvertureRepository couvertureRepository,
            AssureurRepository assureurRepository,
            SocieteRepository societeRepository,
            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.couvertureRepository = couvertureRepository;
        this.assureurRepository = assureurRepository;
        this.societeRepository = societeRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------- patients

    @Transactional(readOnly = true)
    public List<PatientDTO> rechercher(String recherche) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        List<Patient> patients = patientRepository.rechercher(q);
        List<PatientDTO> dtos = patients.stream().map(Patient::versDTO).toList();
        renseignerCouverturesActives(dtos);
        return dtos;
    }

    /** Renseigne les noms des tiers payants actifs pour l'affichage en liste. */
    private void renseignerCouverturesActives(List<PatientDTO> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream().map(d -> d.id).toList();
        Map<Long, List<PatientCouverture>> parPatient = couvertureRepository
                .actives(ids, LocalDate.now()).stream()
                .collect(Collectors.groupingBy(c -> c.getPatient().getId()));

        for (PatientDTO dto : dtos) {
            for (PatientCouverture c : parPatient.getOrDefault(dto.id, List.of())) {
                if (c.getAssureur() != null) {
                    dto.assureurActifNom = c.getAssureur().getNom();
                }
                if (c.getSociete() != null) {
                    dto.societeActiveNom = c.getSociete().getNom();
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public PatientDTO obtenir(Long id) {
        PatientDTO dto = chercher(id).versDTO();
        dto.couvertures = couvertureRepository.historique(id).stream()
                .map(PatientCouverture::versDTO)
                .toList();
        renseignerCouverturesActives(List.of(dto));
        return dto;
    }

    public PatientDTO creer(PatientDTO dto, String nomUtilisateur) {
        valider(dto);
        Patient patient = new Patient();
        patient.appliquer(dto);
        patient.setCreePar(auditService.utilisateurCourant(nomUtilisateur));
        // Numéro provisoire unique le temps d'obtenir l'id auto-généré
        patient.setNumeroDossier("EN-COURS");
        patient = patientRepository.saveAndFlush(patient);
        patient.setNumeroDossier(String.format("P-%06d", patient.getId()));

        // Couvertures fournies à la création (ex. depuis la fiche du client)
        for (CouvertureDTO couverture : dto.couvertures) {
            ajouterCouverture(patient.getId(), couverture, nomUtilisateur);
        }

        auditService.enregistrer(nomUtilisateur, "CREATION", "patient", patient.getId(),
                "Création du dossier " + patient.getNumeroDossier() + " (" + patient.getNom() + ")");
        return obtenirApresEcriture(patient.getId());
    }

    public PatientDTO modifier(Long id, PatientDTO dto, String nomUtilisateur) {
        valider(dto);
        Patient patient = chercher(id);
        patient.appliquer(dto);

        auditService.enregistrer(nomUtilisateur, "MODIFICATION", "patient", patient.getId(),
                "Modification du dossier " + patient.getNumeroDossier());
        return obtenirApresEcriture(id);
    }

    // ---------------------------------------------------------- couvertures

    @Transactional(readOnly = true)
    public List<CouvertureDTO> listerCouvertures(Long patientId) {
        chercher(patientId);
        return couvertureRepository.historique(patientId).stream()
                .map(PatientCouverture::versDTO)
                .toList();
    }

    public CouvertureDTO ajouterCouverture(Long patientId, CouvertureDTO dto, String nomUtilisateur) {
        Patient patient = chercher(patientId);

        PatientCouverture couverture = new PatientCouverture();
        couverture.setPatient(patient);
        couverture.setNumeroAssure(dto.numeroAssure);
        couverture.setPourcentage(dto.pourcentage);
        couverture.setDateDebut(dto.dateDebut != null ? dto.dateDebut : LocalDate.now());
        couverture.setDateFin(dto.dateFin);
        couverture.setCreePar(auditService.utilisateurCourant(nomUtilisateur));

        if (CouvertureDTO.TYPE_ASSUREUR.equals(dto.type)) {
            couverture.setType(CouvertureDTO.TYPE_ASSUREUR);
            couverture.setAssureur(assureurRepository.findById(exigerPayeur(dto))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Assureur inconnu : " + dto.payeurId)));
        } else if (CouvertureDTO.TYPE_SOCIETE.equals(dto.type)) {
            couverture.setType(CouvertureDTO.TYPE_SOCIETE);
            couverture.setSociete(societeRepository.findById(exigerPayeur(dto))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Société inconnue : " + dto.payeurId)));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de couverture invalide : " + dto.type);
        }

        try {
            couverture = couvertureRepository.saveAndFlush(couverture);
        } catch (DataAccessException e) {
            throw traduireErreurBD(e);
        }

        auditService.enregistrer(nomUtilisateur, "CREATION", "patient_couverture",
                couverture.getId(), "Couverture " + couverture.getType() + " pour le dossier "
                        + patient.getNumeroDossier());
        return couverture.versDTO();
    }

    public CouvertureDTO cloturerCouverture(Long patientId, Long couvertureId,
            ClotureCouvertureDTO cloture, String nomUtilisateur) {
        Patient patient = chercher(patientId);
        PatientCouverture couverture = couvertureRepository.findById(couvertureId)
                .filter(c -> c.getPatient().getId().equals(patientId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Couverture introuvable pour ce patient."));

        if (couverture.getDateFin() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cette couverture est déjà clôturée.");
        }
        LocalDate dateFin = cloture.dateFin() != null ? cloture.dateFin() : LocalDate.now();
        if (dateFin.isBefore(couverture.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin ne peut pas précéder la date de début ("
                            + couverture.getDateDebut() + ").");
        }
        couverture.setDateFin(dateFin);
        couverture.setMotifFin(cloture.motifFin());

        auditService.enregistrer(nomUtilisateur, "MODIFICATION", "patient_couverture",
                couverture.getId(), "Clôture de couverture au " + dateFin
                        + " pour le dossier " + patient.getNumeroDossier());
        return couverture.versDTO();
    }

    // ------------------------------------------------------------- internes

    /** Relit le dossier complet (couvertures comprises) après une écriture. */
    private PatientDTO obtenirApresEcriture(Long id) {
        PatientDTO dto = chercher(id).versDTO();
        dto.couvertures = couvertureRepository.historique(id).stream()
                .map(PatientCouverture::versDTO)
                .toList();
        renseignerCouverturesActives(List.of(dto));
        return dto;
    }

    private Long exigerPayeur(CouvertureDTO dto) {
        if (dto.payeurId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le tiers payant de la couverture est obligatoire.");
        }
        return dto.payeurId;
    }

    /** Convertit les erreurs des triggers (SIGNAL 45000) en messages lisibles. */
    private ResponseStatusException traduireErreurBD(DataAccessException e) {
        String message = e.getMostSpecificCause().getMessage();
        if (message != null && message.contains("Chevauchement")) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce patient a déjà une couverture de ce type sur cette période. "
                            + "Clôturez d'abord la couverture en cours.");
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Enregistrement refusé par la base de données : " + message);
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
}
