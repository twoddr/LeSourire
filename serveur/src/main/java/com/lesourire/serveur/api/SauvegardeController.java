package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.SauvegardeDTO;
import com.lesourire.serveur.service.SauvegardeService;

@RestController
@RequestMapping("/api/sauvegardes")
public class SauvegardeController {

    private final SauvegardeService sauvegardeService;

    public SauvegardeController(SauvegardeService sauvegardeService) {
        this.sauvegardeService = sauvegardeService;
    }

    @GetMapping
    public List<SauvegardeDTO> lister() {
        return sauvegardeService.lister();
    }

    @PostMapping
    public SauvegardeDTO lancer(Principal principal) {
        return sauvegardeService.lancer(principal.getName());
    }
}
