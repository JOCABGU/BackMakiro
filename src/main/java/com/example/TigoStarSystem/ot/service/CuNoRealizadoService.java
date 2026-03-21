package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.dto.CuNoRealizadoCreateRequest;
import com.example.TigoStarSystem.ot.repository.CuNoRealizadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CuNoRealizadoService {
    private final CuNoRealizadoRepository repository;

    public CuNoRealizadoService(CuNoRealizadoRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listar() {
        return repository.listar();
    }

    public Map<String, Object> obtenerPorId(Long id) {
        List<Map<String, Object>> rows = repository.obtenerPorId(id);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Cargo usuario no realizado no encontrado.");
        }
        return rows.get(0);
    }

    public void registrar(CuNoRealizadoCreateRequest request) {
        try {
            repository.registrarPlaceholder(request.getDatos());
        } catch (UnsupportedOperationException ex) {
            Map<String, Object> details = new HashMap<>();
            details.put("mensaje", ex.getMessage());
            details.put("datosRecibidos", request.getDatos());
            throw new ApiException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "MISSING_STORED_PROCEDURE",
                    "Falta el SP para registrar Cargo Usuario No Realizado.",
                    details
            );
        }
    }
}
