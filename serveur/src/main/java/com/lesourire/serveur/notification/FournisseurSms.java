package com.lesourire.serveur.notification;

/**
 * Envoi d'un SMS court.
 *
 * <p>Deux implémentations : {@link FournisseurOrangeSms} (API Orange Cameroun)
 * et {@link FournisseurJournal} (mode test : le SMS est seulement journalisé).</p>
 */
public interface FournisseurSms {

    /** Nom technique, valeur du paramètre {@code notification.fournisseur}. */
    String nom();

    /**
     * @param expediteur adresse d'expédition telle que contractualisée
     *                   ({@code tel:+237…} ou nom d'expéditeur whitelisté)
     * @param numeroE164 destinataire au format international ({@code +237…})
     * @param contenu    texte du SMS
     */
    ResultatEnvoi envoyer(String expediteur, String numeroE164, String contenu);
}
