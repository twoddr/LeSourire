package com.lesourire.serveur.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.Role;
import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.TypeRdv;
import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.Rappel;
import com.lesourire.serveur.entite.Rdv;
import com.lesourire.serveur.entite.Utilisateur;
import com.lesourire.serveur.repository.ParametreRepository;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.RappelRepository;
import com.lesourire.serveur.repository.RdvRepository;
import com.lesourire.serveur.repository.UtilisateurRepository;

@Service
@Transactional
public class RdvService {

    private static final List<StatutRdv> STATUTS_IGNORER_CHEVAUCHEMENT =
            List.of(StatutRdv.ANNULE, StatutRdv.ABSENT);
    private static final DateTimeFormatter FORMAT_HEURE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    private final RdvRepository rdvRepository;
    private final RappelRepository rappelRepository;
    private final PatientRepository patientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreRepository parametreRepository;
    private final AuditService auditService;

    public RdvService(RdvRepository rdvRepository, RappelRepository rappelRepository,
            PatientRepository patientRepository, UtilisateurRepository utilisateurRepository,
            ParametreRepository parametreRepository, AuditService auditService) {
        this.rdvRepository = rdvRepository;
        this.rappelRepository = rappelRepository;
        this.patientRepository = patientRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.parametreRepository = parametreRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RdvDTO> lister(LocalDateTime debut, LocalDateTime fin, Long praticienId) {
        if (debut == null || fin == null || !fin.isAfter(debut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La plage de dates est invalide.");
        }
        return rdvRepository.trouverEntre(debut, fin, praticienId).stream()
                .map(Rdv::versDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long compterJournee(LocalDateTime debut, LocalDateTime fin) {
        return rdvRepository.countByDebutGreaterThanEqualAndDebutLessThanAndStatutNotIn(
                debut, fin, STATUTS_IGNORER_CHEVAUCHEMENT);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDTO> praticiens() {
        return utilisateurRepository
                .findByRoleAndActifTrueOrderByNomAscPrenomAsc(Role.DENTISTE)
                .stream()
                .map(Utilisateur::versDTO)
                .toList();
    }

    public RdvDTO creer(RdvDTO dto, String auteur) {
        validerCreneau(dto);
        Patient patient = trouverPatient(dto.patientId);
        Utilisateur praticien = trouverPraticien(dto.praticienId);
        verifierChevauchement(praticien.getId(), dto.debut, dto.fin, null);

        Rdv rdv = new Rdv();
        appliquer(rdv, dto, patient, praticien);
        rdv.setStatut(dto.statut == null ? StatutRdv.PLANIFIE : dto.statut);
        rdv.setCreePar(auditService.utilisateurCourant(auteur));
        rdv = rdvRepository.save(rdv);

        programmerRappel(rdv);

        auditService.enregistrer(auteur, "CREATION", "rdv", rdv.getId(),
                "RDV " + patient.nomComplet() + " le " + rdv.getDebut().format(FORMAT_HEURE));
        return rdv.versDTO();
    }

    public RdvDTO modifier(Long id, RdvDTO dto, String auteur) {
        validerCreneau(dto);
        Rdv rdv = trouver(id);
        LocalDateTime ancienDebut = rdv.getDebut();

        Patient patient = trouverPatient(dto.patientId);
        Utilisateur praticien = trouverPraticien(dto.praticienId);
        verifierChevauchement(praticien.getId(), dto.debut, dto.fin, id);

        appliquer(rdv, dto, patient, praticien);
        if (dto.statut != null) {
            rdv.setStatut(dto.statut);
        }
        rdv = rdvRepository.save(rdv);

        if (!ancienDebut.equals(rdv.getDebut())
                || EnumSet.of(StatutRdv.ANNULE, StatutRdv.ABSENT).contains(rdv.getStatut())) {
            annulerRappelsEnAttente(rdv.getId());
            if (!EnumSet.of(StatutRdv.ANNULE, StatutRdv.ABSENT).contains(rdv.getStatut())) {
                programmerRappel(rdv);
            }
        }

        auditService.enregistrer(auteur, "MODIFICATION", "rdv", rdv.getId(),
                "Modification RDV " + patient.nomComplet());
        return rdv.versDTO();
    }

    public RdvDTO changerStatut(Long id, StatutRdv statut, String auteur) {
        if (statut == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut obligatoire.");
        }
        Rdv rdv = trouver(id);
        rdv.setStatut(statut);
        rdv = rdvRepository.save(rdv);

        if (statut == StatutRdv.ANNULE || statut == StatutRdv.ABSENT) {
            annulerRappelsEnAttente(rdv.getId());
        }

        auditService.enregistrer(auteur, "MODIFICATION", "rdv", rdv.getId(),
                "Statut → " + statut);
        return rdv.versDTO();
    }

    private void annulerRappelsEnAttente(Long rdvId) {
        rappelRepository.annulerEnAttentePourRdv(rdvId, Rappels.Type.RAPPEL_RDV,
                Rappels.Statut.ANNULE, Rappels.Statut.EN_ATTENTE);
    }

    private void programmerRappel(Rdv rdv) {
        int jours = joursAvantRappel();
        LocalDateTime datePrevue = rdv.getDebut().minusDays(jours);
        if (!datePrevue.isAfter(LocalDateTime.now())) {
            return;
        }

        Patient patient = rdv.getPatient();
        CanalDest destinataire = choisirDestinataire(patient);
        if (destinataire == null) {
            return;
        }

        Rappel rappel = new Rappel();
        rappel.setPatient(patient);
        rappel.setRdv(rdv);
        rappel.setType(Rappels.Type.RAPPEL_RDV);
        rappel.setCanal(destinataire.canal());
        rappel.setDatePrevue(datePrevue);
        rappel.setStatut(Rappels.Statut.EN_ATTENTE);
        rappel.setDestinataire(destinataire.adresse());
        rappel.setContenu("Rappel de rendez-vous le " + rdv.getDebut().format(FORMAT_HEURE)
                + " — Cabinet Dentaire Le Sourire");
        rappelRepository.save(rappel);
    }

    private int joursAvantRappel() {
        return parametreRepository.findById("rappel.jours_avant_rdv")
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValeur().trim());
                    } catch (Exception e) {
                        return 2;
                    }
                })
                .orElse(2);
    }

    private static CanalDest choisirDestinataire(Patient patient) {
        if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
            return new CanalDest(Rappels.Canal.EMAIL, patient.getEmail().trim());
        }
        if (patient.getTelephoneWhatsapp() != null && !patient.getTelephoneWhatsapp().isBlank()) {
            return new CanalDest(Rappels.Canal.WHATSAPP, patient.getTelephoneWhatsapp().trim());
        }
        if (patient.getTelephone() != null && !patient.getTelephone().isBlank()) {
            return new CanalDest(Rappels.Canal.SMS, patient.getTelephone().trim());
        }
        return null;
    }

    private void validerCreneau(RdvDTO dto) {
        if (dto.patientId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le patient est obligatoire.");
        }
        if (dto.praticienId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le praticien est obligatoire.");
        }
        if (dto.debut == null || dto.fin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'heure de début et de fin sont obligatoires.");
        }
        if (!dto.fin.isAfter(dto.debut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fin doit être après le début.");
        }
        if (dto.type == null) {
            dto.type = TypeRdv.CONSULTATION;
        }
    }

    private void appliquer(Rdv rdv, RdvDTO dto, Patient patient, Utilisateur praticien) {
        rdv.setPatient(patient);
        rdv.setPraticien(praticien);
        rdv.setDebut(dto.debut);
        rdv.setFin(dto.fin);
        rdv.setType(dto.type);
        rdv.setMotif(videSiBlank(dto.motif));
        rdv.setNotes(videSiBlank(dto.notes));
        rdv.setActeOrigineId(dto.acteOrigineId);
    }

    private void verifierChevauchement(Long praticienId, LocalDateTime debut, LocalDateTime fin,
            Long exclureId) {
        long n = rdvRepository.compterChevauchements(praticienId, debut, fin, exclureId,
                STATUTS_IGNORER_CHEVAUCHEMENT);
        if (n > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce créneau chevauche un autre rendez-vous du praticien.");
        }
    }

    private Rdv trouver(Long id) {
        return rdvRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Rendez-vous introuvable."));
    }

    private Patient trouverPatient(Long id) {
        return patientRepository.findById(id)
                .filter(Patient::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Patient introuvable ou inactif."));
    }

    private Utilisateur trouverPraticien(Long id) {
        Utilisateur u = utilisateurRepository.findById(id)
                .filter(Utilisateur::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Praticien introuvable ou inactif."));
        if (u.getRole() != Role.DENTISTE && u.getRole() != Role.ADMINISTRATEUR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le praticien doit être un dentiste.");
        }
        return u;
    }

    private static String videSiBlank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private record CanalDest(Rappels.Canal canal, String adresse) {
    }
}
