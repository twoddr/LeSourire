package com.lesourire.client.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.lesourire.commun.dto.SauvegardeDTO;

public class ServiceSauvegardesDemo implements ServiceSauvegardes {

    private final List<SauvegardeDTO> sauvegardes = new ArrayList<>();

    public ServiceSauvegardesDemo() {
        sauvegardes.add(new SauvegardeDTO("lesourire-20260729-220000.sql",
                1_250_000L, LocalDateTime.now().minusDays(1)));
    }

    @Override
    public List<SauvegardeDTO> lister() {
        return sauvegardes.stream()
                .sorted(Comparator.comparing(SauvegardeDTO::dateModification).reversed())
                .toList();
    }

    @Override
    public SauvegardeDTO lancer() {
        SauvegardeDTO s = new SauvegardeDTO(
                "lesourire-" + LocalDateTime.now().toString().replace(':', '-') + ".sql",
                1_300_000L, LocalDateTime.now());
        sauvegardes.add(s);
        return s;
    }
}
