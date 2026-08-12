package com.lesourire.serveur.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lesourire.commun.dto.EncaissementDTO;
import com.lesourire.commun.dto.ImpayeDTO;
import com.lesourire.serveur.service.ComptabiliteService;

@RestController
@RequestMapping("/api/comptabilite")
public class ComptabiliteController {

    private final ComptabiliteService comptabiliteService;

    public ComptabiliteController(ComptabiliteService comptabiliteService) {
        this.comptabiliteService = comptabiliteService;
    }

    @GetMapping("/encaissements")
    public List<EncaissementDTO> encaissements(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return comptabiliteService.encaissementsDuJour(date);
    }

    @GetMapping("/journal")
    public List<EncaissementDTO> journal(
            @RequestParam(name = "debut")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(name = "fin")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return comptabiliteService.journal(debut, fin);
    }

    @GetMapping("/impayes")
    public List<ImpayeDTO> impayes() {
        return comptabiliteService.impayes();
    }
}
