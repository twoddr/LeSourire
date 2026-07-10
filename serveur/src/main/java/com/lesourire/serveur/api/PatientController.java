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

import com.lesourire.commun.dto.ClotureCouvertureDTO;
import com.lesourire.commun.dto.CouvertureDTO;
import com.lesourire.commun.dto.PatientDTO;
import com.lesourire.serveur.service.PatientService;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientDTO> rechercher(
            @RequestParam(name = "recherche", required = false) String recherche) {
        return patientService.rechercher(recherche);
    }

    @GetMapping("/{id}")
    public PatientDTO obtenir(@PathVariable Long id) {
        return patientService.obtenir(id);
    }

    @PostMapping
    public PatientDTO creer(@RequestBody PatientDTO dto, Principal principal) {
        return patientService.creer(dto, principal.getName());
    }

    @PutMapping("/{id}")
    public PatientDTO modifier(@PathVariable Long id, @RequestBody PatientDTO dto,
            Principal principal) {
        return patientService.modifier(id, dto, principal.getName());
    }

    // ------------------------------------------------------------ couvertures

    @GetMapping("/{id}/couvertures")
    public List<CouvertureDTO> couvertures(@PathVariable Long id) {
        return patientService.listerCouvertures(id);
    }

    @PostMapping("/{id}/couvertures")
    public CouvertureDTO ajouterCouverture(@PathVariable Long id,
            @RequestBody CouvertureDTO dto, Principal principal) {
        return patientService.ajouterCouverture(id, dto, principal.getName());
    }

    @PutMapping("/{id}/couvertures/{couvertureId}/cloturer")
    public CouvertureDTO cloturerCouverture(@PathVariable Long id,
            @PathVariable Long couvertureId,
            @RequestBody ClotureCouvertureDTO cloture, Principal principal) {
        return patientService.cloturerCouverture(id, couvertureId, cloture, principal.getName());
    }
}
