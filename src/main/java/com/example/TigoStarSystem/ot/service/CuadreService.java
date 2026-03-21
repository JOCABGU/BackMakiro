package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.ot.repository.CuadreRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class CuadreService {
    private final CuadreRepository cuadreRepository;

    public CuadreService(CuadreRepository cuadreRepository) {
        this.cuadreRepository = cuadreRepository;
    }

    public List<Map<String, Object>> validarCuadreRuta(Integer idRuta, LocalDate fecha) {
        return cuadreRepository.validarCuadreRuta(idRuta, fecha);
    }
}
