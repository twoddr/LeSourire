package com.lesourire.serveur.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lesourire.serveur.entite.Parametre;

public interface ParametreRepository extends JpaRepository<Parametre, String> {
}
