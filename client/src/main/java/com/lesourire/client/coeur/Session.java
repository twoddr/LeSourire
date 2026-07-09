package com.lesourire.client.coeur;

import com.lesourire.commun.dto.UtilisateurDTO;

/** Session de travail courante : utilisateur connecté et accès au serveur. */
public final class Session {

    private static UtilisateurDTO utilisateur;
    private static ApiClient apiClient;
    private static boolean modeDemonstration;

    private Session() {
    }

    public static void ouvrir(UtilisateurDTO u, ApiClient client, boolean demo) {
        utilisateur = u;
        apiClient = client;
        modeDemonstration = demo;
    }

    public static void fermer() {
        utilisateur = null;
        apiClient = null;
        modeDemonstration = false;
    }

    public static UtilisateurDTO utilisateur() {
        return utilisateur;
    }

    public static ApiClient api() {
        return apiClient;
    }

    public static boolean estModeDemonstration() {
        return modeDemonstration;
    }
}
