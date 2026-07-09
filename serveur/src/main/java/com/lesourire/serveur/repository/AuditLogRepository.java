package com.lesourire.serveur.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
