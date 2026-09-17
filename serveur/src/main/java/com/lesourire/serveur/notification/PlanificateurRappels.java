package com.lesourire.serveur.notification;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.serveur.service.RappelService;

/**
 * Envoi automatique des notifications arrivées à échéance.
 *
 * <p>Le planificateur tourne en boucle courte mais chaque envoi est isolé dans
 * sa propre transaction : une panne réseau ne bloque donc pas les rappels
 * suivants. WhatsApp et l'e-mail ne sont pas concernés — ils restent dans la
 * file d'attente traitée par le secrétariat (canaux assistés).</p>
 *
 * <p>L'API Orange limite le débit à 5 SMS par seconde : on respecte une pause
 * entre deux messages.</p>
 */
@Component
public class PlanificateurRappels {

    private static final Logger log = LoggerFactory.getLogger(PlanificateurRappels.class);

    /** Auteur porté au journal d'audit pour les envois automatiques. */
    private static final String AUTEUR = "planificateur";

    private final RappelService rappelService;
    private final NotificationService notificationService;
    private final long pauseMs;

    public PlanificateurRappels(RappelService rappelService,
            NotificationService notificationService,
            @Value("${lesourire.notification.pause-ms:250}") long pauseMs) {
        this.rappelService = rappelService;
        this.notificationService = notificationService;
        this.pauseMs = Math.max(0, pauseMs);
    }

    @Scheduled(fixedDelayString = "${lesourire.notification.intervalle-ms:60000}",
            initialDelayString = "${lesourire.notification.delai-initial-ms:30000}")
    public void envoyerRappelsDus() {
        if (!notificationService.active()) {
            return;
        }
        List<Long> ids;
        try {
            ids = rappelService.idsDus(Rappels.Canal.SMS);
        } catch (RuntimeException e) {
            log.warn("Lecture des rappels à envoyer impossible : {}", e.getMessage());
            return;
        }
        if (ids.isEmpty()) {
            return;
        }
        log.info("{} notification(s) à envoyer.", ids.size());
        int envoyes = 0;
        int echecs = 0;
        for (Long id : ids) {
            try {
                RappelDTO rappel = rappelService.envoyer(id, AUTEUR);
                if (rappel.statut == Rappels.Statut.ENVOYE) {
                    envoyes++;
                } else {
                    echecs++;
                    log.warn("Rappel {} non envoyé : {}", id, rappel.messageErreur);
                }
            } catch (RuntimeException e) {
                echecs++;
                log.warn("Rappel {} : envoi impossible ({}).", id, e.getMessage());
            }
            pause();
        }
        log.info("Notifications traitées : {} envoyée(s), {} en échec.", envoyes, echecs);
    }

    private void pause() {
        if (pauseMs == 0) {
            return;
        }
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
