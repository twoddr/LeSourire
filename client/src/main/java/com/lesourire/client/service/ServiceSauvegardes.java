package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.SauvegardeDTO;

public interface ServiceSauvegardes {

    List<SauvegardeDTO> lister() throws Exception;

    SauvegardeDTO lancer() throws Exception;
}
