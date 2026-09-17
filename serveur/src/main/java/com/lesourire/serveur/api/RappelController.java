package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.Rappels;
import com.lesourire.commun.dto.RappelDTO;
import com.lesourire.serveur.service.RappelService;

/**
 * Notifications patients : file d'attente, envoi automatique (SMS) et
 * acquittement des envois assistés (WhatsApp, e-mail).
 */
@RestController
@RequestMapping("/api/rappels")
public class RappelController {

    private final RappelService rappelService;

    public RappelController(RappelService rappelService) {
        this.rappelService = rappelService;
    }

    /** Rappels, tous états confondus ou filtrés par statut. */
    @GetMapping
    public List<RappelDTO> lister(@RequestParam(required = false) Rappels.Statut statut) {
        return rappelService.lister(statut);
    }

    /** File d'attente affichée au secrétariat. */
    @GetMapping("/a-envoyer")
    public List<RappelDTO> aEnvoyer() {
        return rappelService.aEnvoyer();
    }

    /** Envoi immédiat (SMS uniquement) — bouton « Envoyer ». */
    @PostMapping("/{id}/envoyer")
    public RappelDTO envoyer(@PathVariable Long id, Principal principal) {
        return rappelService.envoyer(id, principal.getName());
    }

    /** L'envoi assisté (WhatsApp / e-mail) a été fait : on le note. */
    @PostMapping("/{id}/marquer-envoye")
    public RappelDTO marquerEnvoye(@PathVariable Long id, Principal principal) {
        return rappelService.marquerEnvoye(id, principal.getName());
    }

    @PostMapping("/{id}/annuler")
    public RappelDTO annuler(@PathVariable Long id, Principal principal) {
        return rappelService.annuler(id, principal.getName());
    }
}
