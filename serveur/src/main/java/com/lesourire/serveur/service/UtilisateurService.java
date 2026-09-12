package com.lesourire.serveur.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.Role;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;
import com.lesourire.serveur.entite.Utilisateur;
import com.lesourire.serveur.repository.UtilisateurRepository;

@Service
@Transactional
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder, AuditService auditService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDTO> rechercher(String recherche, boolean inclureInactifs) {
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.FRENCH);
        // Nom et prénom sont chiffrés en base : filtrage et tri en mémoire.
        return utilisateurRepository.findAll().stream()
                .filter(u -> inclureInactifs || u.isActif())
                .filter(u -> correspond(u, q))
                .sorted(Comparator.comparing(Utilisateur::getNom,
                                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Utilisateur::getPrenom,
                                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Utilisateur::getNomUtilisateur,
                                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)))
                .map(Utilisateur::versDTO)
                .toList();
    }

    private static boolean correspond(Utilisateur u, String q) {
        return q.isEmpty()
                || contient(u.getNomUtilisateur(), q)
                || contient(u.getNom(), q)
                || contient(u.getPrenom(), q);
    }

    private static boolean contient(String valeur, String q) {
        return valeur != null && valeur.toLowerCase(Locale.FRENCH).contains(q);
    }

    @Transactional(readOnly = true)
    public UtilisateurDTO obtenir(Long id) {
        return trouver(id).versDTO();
    }

    public UtilisateurDTO creer(UtilisateurEcritureDTO dto, String auteur) {
        validerSaisie(dto, true);
        if (utilisateurRepository.findByNomUtilisateur(dto.nomUtilisateur.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce nom d'utilisateur est déjà pris.");
        }
        Utilisateur u = new Utilisateur();
        appliquer(u, dto, true);
        u = utilisateurRepository.save(u);
        auditService.enregistrer(auteur, "CREATION", "utilisateur", u.getId(),
                "Création de " + u.getNomUtilisateur() + " (" + u.getRole() + ")");
        return u.versDTO();
    }

    public UtilisateurDTO modifier(Long id, UtilisateurEcritureDTO dto, String auteur) {
        validerSaisie(dto, false);
        Utilisateur u = trouver(id);

        String login = dto.nomUtilisateur.trim();
        if (utilisateurRepository.existsByNomUtilisateurAndIdNot(login, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce nom d'utilisateur est déjà pris.");
        }

        // Empêche de se désactiver soi-même ou de retirer le dernier administrateur actif
        if (u.isActif() && !dto.actif) {
            if (u.getNomUtilisateur().equals(auteur)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Vous ne pouvez pas désactiver votre propre compte.");
            }
            if (u.getRole() == Role.ADMINISTRATEUR
                    && utilisateurRepository.countByRoleAndActifTrue(Role.ADMINISTRATEUR) <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Impossible de désactiver le dernier administrateur actif.");
            }
        }
        if (u.getRole() == Role.ADMINISTRATEUR && dto.role != Role.ADMINISTRATEUR
                && u.isActif()
                && utilisateurRepository.countByRoleAndActifTrue(Role.ADMINISTRATEUR) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Impossible de retirer le rôle du dernier administrateur actif.");
        }

        appliquer(u, dto, false);
        u = utilisateurRepository.save(u);
        auditService.enregistrer(auteur, "MODIFICATION", "utilisateur", u.getId(),
                "Modification de " + u.getNomUtilisateur());
        return u.versDTO();
    }

    private Utilisateur trouver(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable."));
    }

    private void validerSaisie(UtilisateurEcritureDTO dto, boolean creation) {
        if (dto.nomUtilisateur == null || dto.nomUtilisateur.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom d'utilisateur est obligatoire.");
        }
        if (dto.nom == null || dto.nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (dto.role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle est obligatoire.");
        }
        if (creation && (dto.motDePasse == null || dto.motDePasse.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe est obligatoire à la création.");
        }
        if (dto.motDePasse != null && !dto.motDePasse.isBlank() && dto.motDePasse.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins 4 caractères.");
        }
    }

    private void appliquer(Utilisateur u, UtilisateurEcritureDTO dto, boolean creation) {
        u.setNomUtilisateur(dto.nomUtilisateur.trim());
        u.setNom(dto.nom.trim());
        u.setPrenom(videSiBlank(dto.prenom));
        u.setRole(dto.role);
        u.setEmail(videSiBlank(dto.email));
        u.setTelephone(videSiBlank(dto.telephone));
        u.setActif(dto.actif);
        if (creation || (dto.motDePasse != null && !dto.motDePasse.isBlank())) {
            u.setMotDePasse(passwordEncoder.encode(dto.motDePasse));
        }
    }

    private static String videSiBlank(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
