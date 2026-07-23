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

import com.lesourire.commun.dto.CategoriePrestationDTO;
import com.lesourire.commun.dto.NouvelleValeurLettreDTO;
import com.lesourire.commun.dto.PrestationDTO;
import com.lesourire.commun.dto.ValeurLettreCleDTO;
import com.lesourire.serveur.service.PrestationService;

@RestController
@RequestMapping("/api")
public class PrestationController {

    private final PrestationService prestationService;

    public PrestationController(PrestationService prestationService) {
        this.prestationService = prestationService;
    }

    @GetMapping("/categories-prestation")
    public List<CategoriePrestationDTO> categories() {
        return prestationService.categories();
    }

    @GetMapping("/prestations")
    public List<PrestationDTO> rechercher(
            @RequestParam(name = "recherche", required = false) String recherche,
            @RequestParam(name = "inclureInactifs", defaultValue = "true") boolean inclureInactifs) {
        return prestationService.rechercher(recherche, inclureInactifs);
    }

    @PostMapping("/prestations")
    public PrestationDTO creer(@RequestBody PrestationDTO dto, Principal principal) {
        return prestationService.creer(dto, principal.getName());
    }

    @PutMapping("/prestations/{id}")
    public PrestationDTO modifier(@PathVariable Long id, @RequestBody PrestationDTO dto,
            Principal principal) {
        return prestationService.modifier(id, dto, principal.getName());
    }

    @GetMapping("/lettres-cles/valeurs")
    public List<ValeurLettreCleDTO> valeursEnVigueur() {
        return prestationService.valeursEnVigueur();
    }

    @GetMapping("/lettres-cles/{lettre}/valeurs")
    public List<ValeurLettreCleDTO> historique(@PathVariable String lettre) {
        return prestationService.historique(lettre);
    }

    @PostMapping("/lettres-cles/{lettre}/valeurs")
    public ValeurLettreCleDTO changerValeur(@PathVariable String lettre,
            @RequestBody NouvelleValeurLettreDTO dto, Principal principal) {
        return prestationService.changerValeur(lettre, dto, principal.getName());
    }
}
