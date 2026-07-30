package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.Facturation.StatutFacture;
import com.lesourire.commun.dto.FactureDTO;
import com.lesourire.commun.dto.PaiementDTO;
import com.lesourire.serveur.service.FactureService;

@RestController
@RequestMapping("/api/factures")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @GetMapping
    public List<FactureDTO> rechercher(
            @RequestParam(name = "recherche", required = false) String recherche,
            @RequestParam(name = "statut", required = false) StatutFacture statut) {
        return factureService.rechercher(recherche, statut);
    }

    @GetMapping("/{id}")
    public FactureDTO obtenir(@PathVariable Long id) {
        return factureService.obtenir(id);
    }

    @PostMapping
    public FactureDTO creer(@RequestBody FactureDTO dto, Principal principal) {
        return factureService.creer(dto, principal.getName());
    }

    @PutMapping("/{id}")
    public FactureDTO modifier(@PathVariable Long id, @RequestBody FactureDTO dto,
            Principal principal) {
        return factureService.modifier(id, dto, principal.getName());
    }

    @PostMapping("/{id}/emettre")
    public FactureDTO emettre(@PathVariable Long id, Principal principal) {
        return factureService.emettre(id, principal.getName());
    }

    @PostMapping("/{id}/annuler")
    public FactureDTO annuler(@PathVariable Long id, Principal principal) {
        return factureService.annuler(id, principal.getName());
    }

    @PostMapping("/{id}/paiements")
    public FactureDTO encaisser(@PathVariable Long id, @RequestBody PaiementDTO dto,
            Principal principal) {
        return factureService.encaisser(id, dto, principal.getName());
    }

    @DeleteMapping("/{id}/paiements/{paiementId}")
    public FactureDTO supprimerPaiement(@PathVariable Long id, @PathVariable Long paiementId,
            Principal principal) {
        return factureService.supprimerPaiement(id, paiementId, principal.getName());
    }
}
