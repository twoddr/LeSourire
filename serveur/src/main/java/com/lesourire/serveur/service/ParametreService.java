package com.lesourire.serveur.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lesourire.commun.dto.ParametreDTO;
import com.lesourire.serveur.entite.Parametre;
import com.lesourire.serveur.repository.ParametreRepository;

@Service
@Transactional
public class ParametreService {

    private final ParametreRepository parametreRepository;
    private final AuditService auditService;

    public ParametreService(ParametreRepository parametreRepository, AuditService auditService) {
        this.parametreRepository = parametreRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ParametreDTO> lister() {
        return parametreRepository.findAll().stream()
                .sorted((a, b) -> a.getCle().compareToIgnoreCase(b.getCle()))
                .map(Parametre::versDTO)
                .toList();
    }

    public ParametreDTO modifier(String cle, ParametreDTO dto, String auteur) {
        Parametre p = parametreRepository.findById(cle)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paramètre introuvable : " + cle));
        // La clé ne change pas ; on n'autorise que la valeur
        p.setValeur(dto.valeur() == null ? "" : dto.valeur());
        p = parametreRepository.save(p);
        auditService.enregistrer(auteur, "MODIFICATION", "parametre", null,
                "Paramètre " + cle + " → " + p.getValeur());
        return p.versDTO();
    }
}
