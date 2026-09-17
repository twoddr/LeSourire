package com.lesourire.serveur.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fournisseur « journal » : n'envoie rien, écrit le SMS dans le journal du
 * serveur et déclare l'envoi réussi.
 *
 * <p>Il sert à valider de bout en bout la chaîne de notification (création du
 * rappel, planificateur, écran « Rappels à envoyer ») avant l'ouverture du
 * compte opérateur, et de dépannage si l'accès Internet du cabinet tombe.</p>
 */
@Component
public class FournisseurJournal implements FournisseurSms {

    private static final Logger log = LoggerFactory.getLogger(FournisseurJournal.class);

    @Override
    public String nom() {
        return "journal";
    }

    @Override
    public ResultatEnvoi envoyer(String expediteur, String numeroE164, String contenu) {
        log.info("[SMS simulé] de « {} » vers {} : {}", expediteur, numeroE164, contenu);
        return ResultatEnvoi.ok();
    }
}
