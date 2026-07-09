package com.lesourire.serveur.entite;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_utilisateur")
    private Utilisateur utilisateur;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entite;

    @Column(name = "entite_id")
    private Long entiteId;

    @Column(columnDefinition = "TEXT")
    private String details;

    public static AuditLog de(Utilisateur utilisateur, String action, String entite,
            Long entiteId, String details) {
        AuditLog log = new AuditLog();
        log.utilisateur = utilisateur;
        log.action = action;
        log.entite = entite;
        log.entiteId = entiteId;
        log.details = details;
        return log;
    }
}
