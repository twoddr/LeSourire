package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.commun.dto.UtilisateurEcritureDTO;
import com.lesourire.serveur.service.UtilisateurService;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public List<UtilisateurDTO> rechercher(
            @RequestParam(name = "recherche", required = false) String recherche,
            @RequestParam(name = "inclureInactifs", defaultValue = "true") boolean inclureInactifs) {
        return utilisateurService.rechercher(recherche, inclureInactifs);
    }

    @GetMapping("/{id}")
    public UtilisateurDTO obtenir(@PathVariable Long id) {
        return utilisateurService.obtenir(id);
    }

    @PostMapping
    public UtilisateurDTO creer(@RequestBody UtilisateurEcritureDTO dto, Principal principal) {
        return utilisateurService.creer(dto, principal.getName());
    }

    @PutMapping("/{id}")
    public UtilisateurDTO modifier(@PathVariable Long id, @RequestBody UtilisateurEcritureDTO dto,
            Principal principal) {
        return utilisateurService.modifier(id, dto, principal.getName());
    }
}
