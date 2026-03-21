package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaRowRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaWebRequest;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ConformacionCuadrillaRequestValidator {
    private static final Set<String> ESTADOS_VALIDOS =
            new HashSet<>(Arrays.asList("ACTIVO", "AUSENTE"));
    private static final Set<String> ACTIVIDADES_VALIDAS =
            new HashSet<>(Arrays.asList("TITULAR", "BACKUP"));

    void validarBackoffice(ConformacionCuadrillaRowRequest fila) {
        if (fila == null) {
            throw validationError("Fila requerida.");
        }

        fila.setEstado(normalizarEstado(fila.getEstado()));
        fila.setActividad(toUpperTrimOrNull(fila.getActividad()));

        List<String> faltantes = new ArrayList<>();
        agregarSiFalta(faltantes, fila.getIdTecnico() == null, "idTecnico");
        agregarSiFalta(faltantes, fila.getEstado() == null, "estado");
        agregarSiFalta(faltantes, fila.getActividad() == null, "actividad");
        agregarSiFalta(faltantes, fila.getIdUsuarioSupervisor() == null, "idUsuarioSupervisor");
        agregarSiFalta(faltantes, isBlank(fila.getSucursal()), "sucursal");
        agregarSiFalta(faltantes, fila.getIdUsuarioRegistra() == null, "idUsuarioRegistra");

        validarTecnicosDistintos(fila.getIdTecnico(), fila.getIdTecnicoAuxiliar());
        validarFaltantes(faltantes);
        validarEstado(fila.getEstado());
    }

    void validarWeb(ConformacionCuadrillaWebRequest fila) {
        if (fila == null) {
            throw validationError("Request requerido.");
        }

        normalizarWeb(fila);

        List<String> faltantes = new ArrayList<>();
        agregarSiFalta(faltantes, fila.getEstado() == null, "estado");
        agregarSiFalta(faltantes, fila.getActividad() == null, "actividad");
        agregarSiFalta(faltantes, fila.getIdTecnico() == null, "idTecnico");
        agregarSiFalta(faltantes, fila.getIdUsuarioSupervisor() == null, "idUsuarioSupervisor");
        agregarSiFalta(faltantes, fila.getSucursal() == null, "sucursal");
        agregarSiFalta(faltantes, fila.getIdUsuarioRegistra() == null, "idUsuarioRegistra");

        validarFaltantes(faltantes);
        validarEstado(fila.getEstado());
        validarTecnicosDistintos(fila.getIdTecnico(), fila.getIdTecnicoAuxiliar());
    }

    private void normalizarWeb(ConformacionCuadrillaWebRequest fila) {
        fila.setEstado(normalizarEstado(fila.getEstado()));
        fila.setActividad(toUpperTrimOrNull(fila.getActividad()));
        fila.setCuentaSf(trimToNull(fila.getCuentaSf()));
        fila.setSalesforce(trimToNull(fila.getSalesforce()));
        fila.setHabilidad(trimToNull(fila.getHabilidad()));
        fila.setVehiculo(trimToNull(fila.getVehiculo()));
        fila.setGrupo(trimToNull(fila.getGrupo()));
        fila.setAlmacen(trimToNull(fila.getAlmacen()));
        fila.setGrupoDigitacion(trimToNull(fila.getGrupoDigitacion()));
        fila.setDigitador(trimToNull(fila.getDigitador()));
        fila.setTecnico(trimToNull(fila.getTecnico()));
        fila.setAuxiliar(trimToNull(fila.getAuxiliar()));
        fila.setSupervisorACargo(trimToNull(fila.getSupervisorACargo()));
        fila.setSucursal(trimToNull(fila.getSucursal()));
        fila.setObservacion(trimToNull(fila.getObservacion()));
    }

    private void validarFaltantes(List<String> faltantes) {
        if (!faltantes.isEmpty()) {
            throw validationError("Faltan campos requeridos: " + String.join(", ", faltantes));
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw validationError("estado invalido. Valores permitidos: ACTIVO, AUSENTE.");
        }
    }

    private void validarActividad(String actividad) {
        if (!ACTIVIDADES_VALIDAS.contains(actividad)) {
            throw validationError("actividad invalida. Valores permitidos: TITULAR, BACKUP.");
        }
    }

    private void validarTecnicosDistintos(Integer idTecnico, Integer idTecnicoAuxiliar) {
        if (idTecnicoAuxiliar != null && idTecnicoAuxiliar.equals(idTecnico)) {
            throw validationError("idTecnicoAuxiliar no puede ser igual a idTecnico.");
        }
    }

    private void agregarSiFalta(List<String> faltantes, boolean condicion, String campo) {
        if (condicion) {
            faltantes.add(campo);
        }
    }

    private ApiException validationError(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message
        );
    }

    private String normalizarEstado(String value) {
        String normalized = toUpperTrimOrNull(value);
        if ("INACTIVO".equals(normalized)) {
            return "AUSENTE";
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String toUpperTrimOrNull(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
