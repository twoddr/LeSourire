package com.lesourire.serveur.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Acte;

public interface ActeRepository extends JpaRepository<Acte, Long> {
}
