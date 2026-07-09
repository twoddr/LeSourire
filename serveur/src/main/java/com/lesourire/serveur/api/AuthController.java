package com.lesourire.serveur.api;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.serveur.repository.UtilisateurRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurRepository utilisateurRepository;

    public AuthController(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Renvoie le profil de l'utilisateur authentifié.
     * Sert de "connexion" au client : si les identifiants Basic sont bons,
     * il reçoit son profil (dont son rôle), sinon 401.
     */
    @GetMapping("/moi")
    public UtilisateurDTO moi(Principal principal) {
        return utilisateurRepository
                .findByNomUtilisateurAndActifTrue(principal.getName())
                .map(u -> u.versDTO())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
