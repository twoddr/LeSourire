package com.lesourire.client.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import com.lesourire.commun.Role;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;

/** Stockage mémoire pour le mode démonstration (rien n'est persisté). */
public class ServiceUtilisateursDemo implements ServiceUtilisateurs {

    private static final AtomicLong SEQ = new AtomicLong(1);
    private final List<UtilisateurDTO> utilisateurs = new ArrayList<>();

    public ServiceUtilisateursDemo() {
        utilisateurs.add(new UtilisateurDTO(SEQ.getAndIncrement(), "admin", "Administrateur",
                null, Role.ADMINISTRATEUR, null, null, true));
        utilisateurs.add(new UtilisateurDTO(SEQ.getAndIncrement(), "ntowe", "Towe", "Nadine",
                Role.DENTISTE, null, null, true));
        utilisateurs.add(new UtilisateurDTO(SEQ.getAndIncrement(), "accueil", "Mbarga", "Claire",
                Role.SECRETAIRE, null, null, true));
    }

    @Override
    public List<UtilisateurDTO> rechercher(String recherche, boolean inclureInactifs) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        return utilisateurs.stream()
                .filter(u -> inclureInactifs || u.actif())
                .filter(u -> q.isEmpty()
                        || u.nomUtilisateur().toLowerCase(Locale.FRENCH).contains(q)
                        || u.nom().toLowerCase(Locale.FRENCH).contains(q)
                        || (u.prenom() != null
                                && u.prenom().toLowerCase(Locale.FRENCH).contains(q)))
                .toList();
    }

    @Override
    public UtilisateurDTO creer(UtilisateurEcritureDTO saisie) {
        UtilisateurDTO u = new UtilisateurDTO(SEQ.getAndIncrement(), saisie.nomUtilisateur.trim(),
                saisie.nom.trim(), blank(saisie.prenom), saisie.role, blank(saisie.email),
                blank(saisie.telephone), saisie.actif);
        utilisateurs.add(u);
        return u;
    }

    @Override
    public UtilisateurDTO modifier(Long id, UtilisateurEcritureDTO saisie) {
        for (int i = 0; i < utilisateurs.size(); i++) {
            if (utilisateurs.get(i).id().equals(id)) {
                UtilisateurDTO u = new UtilisateurDTO(id, saisie.nomUtilisateur.trim(),
                        saisie.nom.trim(), blank(saisie.prenom), saisie.role, blank(saisie.email),
                        blank(saisie.telephone), saisie.actif);
                utilisateurs.set(i, u);
                return u;
            }
        }
        throw new IllegalArgumentException("Utilisateur introuvable.");
    }

    private static String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
