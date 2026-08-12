package com.lesourire.client.service;

import java.time.LocalDate;
import java.util.List;

import com.lesourire.commun.dto.EncaissementDTO;
import com.lesourire.commun.dto.ImpayeDTO;

public interface ServiceComptabilite {

    List<EncaissementDTO> encaissements(LocalDate date) throws Exception;

    List<EncaissementDTO> journal(LocalDate debut, LocalDate fin) throws Exception;

    List<ImpayeDTO> impayes() throws Exception;
}
