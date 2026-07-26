package com.lesourire.client.service;

import java.time.LocalDateTime;
import java.util.List;

import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.dto.RdvDTO;
import com.lesourire.commun.dto.UtilisateurDTO;

public interface ServiceRdv {

    List<RdvDTO> lister(LocalDateTime debut, LocalDateTime fin, Long praticienId) throws Exception;

    long compter(LocalDateTime debut, LocalDateTime fin) throws Exception;

    RdvDTO creer(RdvDTO dto) throws Exception;

    RdvDTO modifier(Long id, RdvDTO dto) throws Exception;

    RdvDTO changerStatut(Long id, StatutRdv statut) throws Exception;

    List<UtilisateurDTO> praticiens() throws Exception;
}
