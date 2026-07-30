package com.lesourire.serveur.api;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.ParametreDTO;
import com.lesourire.serveur.service.ParametreService;

@RestController
@RequestMapping("/api/parametres")
public class ParametreController {
    private final ParametreService parametreService;

    public ParametreController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @GetMapping
    public List<ParametreDTO> lister() {
        return parametreService.lister();
    }

    @PutMapping("/{cle}")
    public ParametreDTO modifier(@PathVariable String cle, @RequestBody ParametreDTO dto,
            Principal principal) {
        return parametreService.modifier(cle, dto, principal.getName());
    }
}
