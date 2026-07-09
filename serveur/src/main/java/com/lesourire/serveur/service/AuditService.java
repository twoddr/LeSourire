package com.lesourire.serveur.service;

import org.springframework.stereotype.Service;

import com.lesourire.serveur.entite.AuditLog;
import com.lesourire.serveur.entite.Utilisateur;
import com.lesourire.serveur.repository.AuditLogRepository;
import com.lesourire.serveur.repository.UtilisateurRepository;

/** Trace les actions sensibles dans le journal d'audit. */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AuditService(AuditLogRepository auditLogRepository,
            UtilisateurRepository utilisateurRepository) {
        this.auditLogRepository = auditLogRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public void enregistrer(String nomUtilisateur, String action, String entite,
            Long entiteId, String details) {
        Utilisateur utilisateur = utilisateurRepository
                .findByNomUtilisateurAndActifTrue(nomUtilisateur)
                .orElse(null);
        auditLogRepository.save(AuditLog.de(utilisateur, action, entite, entiteId, details));
    }

    public Utilisateur utilisateurCourant(String nomUtilisateur) {
        return utilisateurRepository
                .findByNomUtilisateurAndActifTrue(nomUtilisateur)
                .orElse(null);
    }
}
