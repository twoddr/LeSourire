package com.lesourire.client.service;

import java.util.List;

import com.lesourire.commun.dto.ParametreDTO;

public interface ServiceParametres {
    List<ParametreDTO> lister() throws Exception;

    ParametreDTO modifier(String cle, String valeur) throws Exception;
}
