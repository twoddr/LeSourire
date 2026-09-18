package com.lesourire.serveur.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import com.lesourire.serveur.notification.NotificationService;
import com.lesourire.serveur.repository.ParametreRepository;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.RappelRepository;
import com.lesourire.serveur.repository.RdvRepository;
import com.lesourire.serveur.repository.UtilisateurRepository;

@Service
@Transactional
public class RdvService {

    private static final List<StatutRdv> STATUTS_IGNORER_CHEVAUCHEMENT = List.of(StatutRdv.ANNULE, StatutRdv.ABSENT);
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm",
            Locale.FRENCH);

    private final RdvRepository rdvRepository;
    private final RappelRepository rappelRepository;
    private final PatientRepository patientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreRepository parametreRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public RdvService(RdvRepository rdvRepository, RappelRepository rappelRepository,
            PatientRepository patientRepository, UtilisateurRepository utilisateurRepository,
            ParametreRepository parametreRepository, AuditService auditService,
            NotificationService notificationService) {
        this.rdvRepository = rdvRepository;
        this.rappelRepository = rappelRepository;
        this.patientRepository = patientRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.parametreRepository = parametreRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
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
        // Nom/prénom chiffrés en base : le tri se fait en mémoire.
        return utilisateurRepository
                .findByRoleAndActifTrue(Role.DENTISTE)
                .stream()
                .sorted(Comparator.comparing(Utilisateur::getNom,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Utilisateur::getPrenom,
                                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
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

        programmerConfirmation(rdv);
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
        // flush avant l'UPDATE des rappels (sinon clearAutomatically annule la modif)
        rdv = rdvRepository.saveAndFlush(rdv);

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
        rdv = rdvRepository.saveAndFlush(rdv);

        if (statut == StatutRdv.ANNULE || statut == StatutRdv.ABSENT) {
            annulerRappelsEnAttente(rdv.getId());
        }

        auditService.enregistrer(auteur, "MODIFICATION", "rdv", rdv.getId(),
                "Statut → " + statut);
        return rdv.versDTO();
    }

    private void annulerRappelsEnAttente(Long rdvId) {
        // Un rendez-vous déplacé ou annulé ne doit plus rien annoncer : la
        // confirmation comme le rappel de la veille deviennent caducs.
        rappelRepository.annulerToutEnAttentePourRdv(rdvId,
                Rappels.Statut.ANNULE, Rappels.Statut.EN_ATTENTE);
    }

    /** Rappel de la veille (J-N, cf. paramètre {@code rappel.jours_avant_rdv}, défaut 1). */
    private void programmerRappel(Rdv rdv) {
        int jours = joursAvantRappel();
        LocalDateTime datePrevue = rdv.getDebut().minusDays(jours);
        if (!datePrevue.isAfter(LocalDateTime.now())) {
            return;
        }
        Patient patient = rdv.getPatient();
        NotificationService.DestinataireRappel destinataire = notificationService.choisirDestinataire(patient);
        if (destinataire == null) {
            return;
        }
        enregistrer(rdv, Rappels.Type.RAPPEL_RDV, destinataire, datePrevue,
                notificationService.messageRappel(patient.getPrenom(), rdv.getDebut()));
    }

    /**
     * Confirmation envoyée sans délai dès la prise de rendez-vous : le patient
     * sait tout de suite que le créneau est réservé. Le message part avec la
     * prochaine passe du planificateur (moins d'une minute).
     */
    private void programmerConfirmation(Rdv rdv) {
        Patient patient = rdv.getPatient();
        NotificationService.DestinataireRappel destinataire = notificationService.choisirDestinataire(patient);
        if (destinataire == null) {
            return;
        }
        enregistrer(rdv, Rappels.Type.CONFIRMATION_RDV, destinataire, LocalDateTime.now(),
                notificationService.messageConfirmation(patient.getPrenom(), rdv.getDebut(),
                        rdv.getPraticien().versDTO().nomComplet()));
    }

    private void enregistrer(Rdv rdv, Rappels.Type type,
            NotificationService.DestinataireRappel destinataire,
            LocalDateTime datePrevue, String contenu) {
        Rappel rappel = new Rappel();
        rappel.setPatient(rdv.getPatient());
        rappel.setRdv(rdv);
        rappel.setType(type);
        rappel.setCanal(destinataire.canal());
        rappel.setDatePrevue(datePrevue);
        rappel.setStatut(Rappels.Statut.EN_ATTENTE);
        rappel.setDestinataire(destinataire.adresse());
        rappel.setContenu(contenu);
        rappelRepository.save(rappel);
    }

    private int joursAvantRappel() {
        return parametreRepository.findById("rappel.jours_avant_rdv")
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValeur().trim());
                    } catch (Exception e) {
                        return 1;
                    }
                })
                .orElse(1);
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
}
