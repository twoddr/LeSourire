package com.lesourire.commun.dto;

import java.time.LocalDateTime;

import com.lesourire.commun.StatutRdv;
import com.lesourire.commun.TypeRdv;

/** Rendez-vous échangé entre serveur et client. */
public class RdvDTO {

    public Long id;
    public Long patientId;
    public String patientNom;
    public String patientTelephone;
    public Long praticienId;
    public String praticienNom;
    public LocalDateTime debut;
    public LocalDateTime fin;
    public TypeRdv type = TypeRdv.CONSULTATION;
    public StatutRdv statut = StatutRdv.PLANIFIE;
    public String motif;
    public String notes;
    public Long acteOrigineId;
}
