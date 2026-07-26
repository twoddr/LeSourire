package com.lesourire.serveur.api;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.StatutRdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;
import com.lesourire.serveur.service.RdvService;

@RestController
@RequestMapping("/api")
public class RdvController {

    private final RdvService rdvService;

    public RdvController(RdvService rdvService) {
        this.rdvService = rdvService;
    }

    @GetMapping("/rdv")
    public List<RdvDTO> lister(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(required = false) Long praticienId) {
        return rdvService.lister(debut, fin, praticienId);
    }

    @GetMapping("/rdv/count")
    public long compter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return rdvService.compterJournee(debut, fin);
    }

    @PostMapping("/rdv")
    public RdvDTO creer(@RequestBody RdvDTO dto, Principal principal) {
        return rdvService.creer(dto, principal.getName());
    }

    @PutMapping("/rdv/{id}")
    public RdvDTO modifier(@PathVariable Long id, @RequestBody RdvDTO dto, Principal principal) {
        return rdvService.modifier(id, dto, principal.getName());
    }

    @PutMapping("/rdv/{id}/statut")
    public RdvDTO changerStatut(@PathVariable Long id, @RequestBody StatutRdvDTO corps,
            Principal principal) {
        return rdvService.changerStatut(id, corps.statut(), principal.getName());
    }

    @GetMapping("/praticiens")
    public List<UtilisateurDTO> praticiens() {
        return rdvService.praticiens();
    }
}
