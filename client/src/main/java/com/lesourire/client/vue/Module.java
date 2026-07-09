package com.lesourire.client.vue;

import java.util.Set;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.lesourire.commun.Role;

import static com.lesourire.commun.Role.ADMINISTRATEUR;
import static com.lesourire.commun.Role.ASSISTANT;
import static com.lesourire.commun.Role.COMPTABLE;
import static com.lesourire.commun.Role.DENTISTE;
import static com.lesourire.commun.Role.SECRETAIRE;

/**
 * Les modules de l'application : libellé, icône et rôles autorisés.
 * La barre de navigation se construit à partir de cette énumération.
 */
public enum Module {

    TABLEAU_BORD("Tableau de bord", Material2AL.DASHBOARD,
            "Vue globale de la journée : rendez-vous, encaissements, alertes.",
            Set.of(DENTISTE, ASSISTANT, SECRETAIRE, COMPTABLE, ADMINISTRATEUR)),

    PATIENTS("Patients", Material2AL.GROUP,
            "Dossiers patients : identité, contacts, antécédents, prise en charge assureur et société.",
            Set.of(DENTISTE, ASSISTANT, SECRETAIRE, ADMINISTRATEUR)),

    AGENDA("Agenda", Material2AL.EVENT,
            "Rendez-vous et salle d'attente, rappels automatiques J-2 et revisites post-intervention.",
            Set.of(DENTISTE, ASSISTANT, SECRETAIRE, ADMINISTRATEUR)),

    FACTURATION("Facturation", Material2MZ.RECEIPT,
            "Actes cotés en D et Z, factures, réductions, quotes-parts patient / assureur / société, paiements.",
            Set.of(DENTISTE, SECRETAIRE, COMPTABLE, ADMINISTRATEUR)),

    STOCK("Stock", Material2MZ.SHOPPING_CART,
            "Articles et fournisseurs, entrées et sorties, alertes de seuil et de péremption.",
            Set.of(DENTISTE, ASSISTANT, ADMINISTRATEUR)),

    COMPTABILITE("Comptabilité", Material2AL.ACCOUNT_BALANCE,
            "Encaissements du jour, impayés, journaux et états destinés au comptable.",
            Set.of(COMPTABLE, ADMINISTRATEUR)),

    ADMINISTRATION("Administration", Material2MZ.SETTINGS,
            "Utilisateurs et rôles, tarifaire (valeurs de D et Z, prestations), paramètres du cabinet, sauvegardes.",
            Set.of(ADMINISTRATEUR));

    private final String libelle;
    private final Ikon icone;
    private final String description;
    private final Set<Role> rolesAutorises;

    Module(String libelle, Ikon icone, String description, Set<Role> rolesAutorises) {
        this.libelle = libelle;
        this.icone = icone;
        this.description = description;
        this.rolesAutorises = rolesAutorises;
    }

    public String getLibelle() {
        return libelle;
    }

    public Ikon getIcone() {
        return icone;
    }

    public String getDescription() {
        return description;
    }

    public boolean estAccessiblePar(Role role) {
        return rolesAutorises.contains(role);
    }
}
