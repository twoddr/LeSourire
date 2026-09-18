package com.lesourire.serveur.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.commun.dto.RappelEcritureDTO;
import com.lesourire.commun.dto.RappelStatsDTO;
import com.lesourire.serveur.entite.Patient;
import com.lesourire.serveur.entite.Rappel;
import com.lesourire.serveur.entite.Rdv;
import com.lesourire.serveur.notification.NotificationService;
import com.lesourire.serveur.notification.ResultatEnvoi;
import com.lesourire.serveur.repository.PatientRepository;
import com.lesourire.serveur.repository.RappelRepository;
import com.lesourire.serveur.repository.RdvRepository;

/**
 * Cycle de vie des notifications patients : création, consultation, envoi et
 * annulation.
 *
 * <p>Trois façons d'envoyer :</p>
 * <ul>
 *   <li>{@link #envoyer(Long, String)} — envoi automatique (SMS via l'API
 *       Orange), appelé par le planificateur ou par le bouton « Envoyer » ;</li>
 *   <li>{@link #marquerEnvoye(Long, String)} — envoi assisté déjà effectué par
 *       le secrétariat (WhatsApp, e-mail, appel téléphonique) ;</li>
 *   <li>{@link #creer(RappelEcritureDTO, String)} — notification manuelle, au
 *       clic droit sur un rendez-vous de l'agenda.</li>
 * </ul>
 */
@Service
@Transactional
public class RappelService {

    /** Délai avant une nouvelle tentative après un échec d'envoi. */
    private static final int MINUTES_AVANT_NOUVELLE_TENTATIVE = 15;

    /** Bornes du journal : on ne remonte jamais toute la base. */
    private static final int JOURNAL_MIN = 1;
    private static final int JOURNAL_MAX = 1000;

    private final RappelRepository rappelRepository;
    private final PatientRepository patientRepository;
    private final RdvRepository rdvRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public RappelService(RappelRepository rappelRepository, PatientRepository patientRepository,
            RdvRepository rdvRepository, NotificationService notificationService,
            AuditService auditService) {
        this.rappelRepository = rappelRepository;
        this.patientRepository = patientRepository;
        this.rdvRepository = rdvRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RappelDTO> lister(Rappels.Statut statut) {
        List<Rappel> rappels = statut == null
                ? rappelRepository.findAll()
                : rappelRepository.findByStatutOrderByDatePrevueAsc(statut);
        return rappels.stream().map(this::versDTO).toList();
    }

    /** File d'attente affichée au secrétariat. */
    @Transactional(readOnly = true)
    public List<RappelDTO> aEnvoyer() {
        return lister(Rappels.Statut.EN_ATTENTE);
    }

    /** Rappels automatiques arrivés à échéance (identifiants seulement). */
    @Transactional(readOnly = true)
    public List<Long> idsDus(Rappels.Canal canal) {
        return rappelRepository.idsDus(Rappels.Statut.EN_ATTENTE, canal, LocalDateTime.now());
    }

    // -------------------------------------------------------------- création

    /**
     * Crée une notification manuelle rattachée à un rendez-vous.
     *
     * <p>Le message standard du cabinet est appliqué si l'appelant n'en fournit
     * pas. Aucune bascule silencieuse de canal : si la coordonnée demandée
     * manque, la création est refusée avec un message explicite.</p>
     */
    public RappelDTO creer(RappelEcritureDTO saisie, String auteur) {
        if (saisie == null || saisie.patientId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le patient est obligatoire.");
        }
        Patient patient = patientRepository.findById(saisie.patientId)
                .filter(Patient::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Patient introuvable ou inactif."));
        if (!patient.isConsentementRappel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce patient a refusé d'être notifié : mettez sa fiche à jour d'abord.");
        }
        Rdv rdv = saisie.rdvId == null ? null : rdvRepository.findById(saisie.rdvId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Rendez-vous introuvable."));

        NotificationService.DestinataireRappel destinataire =
                notificationService.choisirDestinataire(patient, saisie.canal);
        if (destinataire == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    messageSansCoordonnee(saisie.canal));
        }

        Rappels.Type type = saisie.type == null ? Rappels.Type.RAPPEL_RDV : saisie.type;
        String contenu = videSiBlank(saisie.contenu);
        if (contenu == null) {
            contenu = messageStandard(type, patient, rdv);
        }

        Rappel rappel = new Rappel();
        rappel.setPatient(patient);
        rappel.setRdv(rdv);
        rappel.setType(type);
        rappel.setCanal(destinataire.canal());
        rappel.setDatePrevue(saisie.datePrevue == null ? LocalDateTime.now() : saisie.datePrevue);
        rappel.setStatut(Rappels.Statut.EN_ATTENTE);
        rappel.setDestinataire(destinataire.adresse());
        rappel.setContenu(contenu);
        rappel = rappelRepository.save(rappel);

        auditService.enregistrer(auteur, "CREATION", "rappel", rappel.getId(),
                type + " " + rappel.getCanal() + " → " + patient.nomComplet());
        return versDTO(rappel);
    }

    private String messageStandard(Rappels.Type type, Patient patient, Rdv rdv) {
        return switch (type) {
            case CONFIRMATION_RDV -> {
                if (rdv == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Choisissez un rendez-vous pour la confirmation.");
                }
                yield notificationService.messageConfirmation(patient.getPrenom(), rdv.getDebut(),
                        rdv.getPraticien() == null
                                ? null : rdv.getPraticien().versDTO().nomComplet());
            }
            case RAPPEL_RDV -> {
                if (rdv == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Choisissez un rendez-vous pour le rappel.");
                }
                yield notificationService.messageRappel(patient.getPrenom(), rdv.getDebut());
            }
            case REVISITE -> notificationService.messageRevisite(patient.getPrenom());
        };
    }

    private static String messageSansCoordonnee(Rappels.Canal canal) {
        if (canal == null) {
            return "Ce patient n'a aucune coordonnée joignable (téléphone, WhatsApp ou e-mail).";
        }
        return "Ce patient n'a pas de coordonnée pour le canal " + canal.getLibelle() + ".";
    }

    // --------------------------------------------------------------- journal

    /** Derniers envois réussis, les plus récents d'abord. */
    @Transactional(readOnly = true)
    public List<RappelDTO> journal(int limite) {
        int taille = Math.min(Math.max(limite, JOURNAL_MIN), JOURNAL_MAX);
        return rappelRepository
                .findByStatutOrderByDateEnvoiDesc(Rappels.Statut.ENVOYE,
                        PageRequest.of(0, taille))
                .stream().map(this::versDTO).toList();
    }

    /** Compteurs affichés dans le bandeau et le dialogue d'historique. */
    @Transactional(readOnly = true)
    public RappelStatsDTO statistiques() {
        RappelStatsDTO stats = new RappelStatsDTO();
        stats.enAttente = rappelRepository.countByStatut(Rappels.Statut.EN_ATTENTE);
        stats.envoyees = rappelRepository.countByStatut(Rappels.Statut.ENVOYE);
        stats.echecs = rappelRepository.countByStatut(Rappels.Statut.ECHEC);
        stats.annulees = rappelRepository.countByStatut(Rappels.Statut.ANNULE);
        stats.envoyeesAujourdhui = rappelRepository
                .countByStatutAndDateEnvoiGreaterThanEqual(Rappels.Statut.ENVOYE,
                        LocalDate.now().atStartOfDay());
        List<Rappel> derniers = rappelRepository.findByStatutOrderByDateEnvoiDesc(
                Rappels.Statut.ENVOYE, PageRequest.of(0, 1));
        stats.derniereEnvoyeeLe = derniers.isEmpty() ? null : derniers.get(0).getDateEnvoi();
        return stats;
    }

    /** Envoi automatique. Réservé aux canaux que le serveur sait traiter seul. */
    public RappelDTO envoyer(Long id, String auteur) {
        Rappel rappel = trouver(id);
        exigerEnAttente(rappel);
        if (!notificationService.canalAutomatique(rappel.getCanal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce rappel passe par " + libelle(rappel.getCanal())
                            + " : ouvrez le lien d'envoi puis marquez-le comme envoyé.");
        }
        ResultatEnvoi resultat = notificationService.envoyerSms(
                rappel.getDestinataire(), rappel.getContenu());
        return finaliser(rappel, resultat, auteur, true);
    }

    /** Envoi assisté déjà réalisé (WhatsApp, e-mail…) : il ne reste qu'à le noter. */
    public RappelDTO marquerEnvoye(Long id, String auteur) {
        Rappel rappel = trouver(id);
        exigerEnAttente(rappel);
        return finaliser(rappel, ResultatEnvoi.ok(), auteur, false);
    }

    /** Un rappel devenu sans objet (patient prévenu autrement, RDV déplacé…). */
    public RappelDTO annuler(Long id, String auteur) {
        Rappel rappel = trouver(id);
        exigerEnAttente(rappel);
        rappel.setStatut(Rappels.Statut.ANNULE);
        rappel = rappelRepository.save(rappel);
        auditService.enregistrer(auteur, "MODIFICATION", "rappel", rappel.getId(),
                "Rappel annulé (" + rappel.getType() + ")");
        return versDTO(rappel);
    }

    private RappelDTO finaliser(Rappel rappel, ResultatEnvoi resultat, String auteur,
            boolean tentativeAutomatique) {
        rappel.setDateEnvoi(LocalDateTime.now());
        if (tentativeAutomatique) {
            rappel.setTentatives(rappel.getTentatives() + 1);
        }
        if (resultat.succes()) {
            rappel.setStatut(Rappels.Statut.ENVOYE);
            rappel.setMessageErreur(null);
        } else if (rappel.getTentatives() < notificationService.maxTentatives()) {
            // Nouvel essai plus tard : le rappel reste dans la file d'attente.
            rappel.setStatut(Rappels.Statut.EN_ATTENTE);
            rappel.setDatePrevue(LocalDateTime.now()
                    .plusMinutes(MINUTES_AVANT_NOUVELLE_TENTATIVE));
            rappel.setMessageErreur(resultat.messageErreur());
        } else {
            rappel.setStatut(Rappels.Statut.ECHEC);
            rappel.setMessageErreur(resultat.messageErreur());
        }
        rappel = rappelRepository.save(rappel);
        auditService.enregistrer(auteur, "ENVOI", "rappel", rappel.getId(),
                rappel.getType() + " " + rappel.getCanal() + " → " + rappel.getStatut());
        return versDTO(rappel);
    }

    private RappelDTO versDTO(Rappel rappel) {
        RappelDTO dto = rappel.versDTO();
        dto.lienEnvoi = notificationService.lienManuel(rappel.getCanal(),
                rappel.getDestinataire(), rappel.getContenu());
        return dto;
    }

    private Rappel trouver(Long id) {
        return rappelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Rappel introuvable."));
    }

    private static void exigerEnAttente(Rappel rappel) {
        if (rappel.getStatut() != Rappels.Statut.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce rappel a déjà été traité (" + rappel.getStatut() + ").");
        }
    }

    private static String libelle(Rappels.Canal canal) {
        return switch (canal) {
            case WHATSAPP -> "WhatsApp";
            case EMAIL -> "l'e-mail";
            default -> "le SMS";
        };
    }

    private static String videSiBlank(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
