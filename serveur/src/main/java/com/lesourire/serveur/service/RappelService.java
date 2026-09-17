package com.lesourire.serveur.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.serveur.entite.Rappel;
import com.lesourire.serveur.notification.NotificationService;
import com.lesourire.serveur.notification.ResultatEnvoi;
import com.lesourire.serveur.repository.RappelRepository;

/**
 * Cycle de vie des notifications patients : consultation, envoi et annulation.
 *
 * <p>Deux façons d'envoyer :</p>
 * <ul>
 *   <li>{@link #envoyer(Long, String)} — envoi automatique (SMS via l'API
 *       Orange), appelé par le planificateur ou par le bouton « Envoyer » ;</li>
 *   <li>{@link #marquerEnvoye(Long, String)} — envoi assisté déjà effectué par
 *       le secrétariat (WhatsApp, e-mail, appel téléphonique).</li>
 * </ul>
 */
@Service
@Transactional
public class RappelService {

    /** Délai avant une nouvelle tentative après un échec d'envoi. */
    private static final int MINUTES_AVANT_NOUVELLE_TENTATIVE = 15;

    private final RappelRepository rappelRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public RappelService(RappelRepository rappelRepository,
            NotificationService notificationService, AuditService auditService) {
        this.rappelRepository = rappelRepository;
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
}
