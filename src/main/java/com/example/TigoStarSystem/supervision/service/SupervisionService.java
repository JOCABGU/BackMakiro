package com.example.TigoStarSystem.supervision.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervision.dto.SupervisionCrearRequest;
import com.example.TigoStarSystem.supervision.repository.SupervisionRepository;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaWebRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupervisionService {
    private final SupervisionRepository repository;
    private final ConformacionCuadrillaWebRepository cuadrillaWebRepository;
    private final AuthService authService;

    public SupervisionService(
            SupervisionRepository repository,
            ConformacionCuadrillaWebRepository cuadrillaWebRepository,
            AuthService authService) {
        this.repository = repository;
        this.cuadrillaWebRepository = cuadrillaWebRepository;
        this.authService = authService;
    }

    public List<Map<String, Object>> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Integer limite,
            String token) {
        validarRangoFechas(fechaDesde, fechaHasta);
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        return repository.listar(String.valueOf(idSupervisor), fechaDesde, fechaHasta, limite);
    }

    public Map<String, Object> obtenerDetalle(String idSupervision, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        Map<String, Object> detalle = repository.obtenerDetalle(idSupervision, String.valueOf(idSupervisor));
        if (detalle == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "No se encontro la nota de supervision indicada."
            );
        }
        return detalle;
    }

    public Map<String, Object> registrar(SupervisionCrearRequest request, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request de supervision es requerido."
            );
        }

        String idGenerado = repository.registrar(
                idSupervisor,
                request.getIdTecnicoPrincipal(),
                request.getIdTecnicoAuxiliar(),
                request.getIdTipoSupervision(),
                request.getIdTipoTrabajo(),
                request.getIdTipoPenalizacion(),
                request.getSupervisionPor(),
                request.getTecnologia(),
                request.getCodigo(),
                request.getOrdenTrabajo(),
                request.getTipoRevision(),
                request.getFotoBoletaSupervision(),
                request.getFotoCanalesPilos(),
                request.getFotoNivelesDocsis(),
                request.getFotoMedicionRuido(),
                request.getFotoBarridoCanales(),
                request.getFotoObservacion1(),
                request.getFotoObservacion2(),
                request.getFotoObservacion3(),
                request.getFotoObservacion4(),
                request.getObservacion(),
                request.getDescripcionAdicionalObservacion(),
                request.getUbicacion()
        );

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("idSupervision", idGenerado);
        out.put("idUsuarioSesion", idSupervisor);
        return out;
    }

    public List<Map<String, Object>> listarTecnicos(String q, Integer limit, String sucursal, String token) {
        AuthMeResponse me = authService.me(token);
        Integer idSupervisor = resolveIdUsuario(me);
        String sucursalResuelta = resolveSucursalNombre(sucursal, me);
        List<Map<String, Object>> rows = cuadrillaWebRepository.listarTecnicos(sucursalResuelta, idSupervisor);
        return filterAndLimitTecnicos(rows, q, limit);
    }

    public List<Map<String, Object>> listarTiposSupervision(String token) {
        authService.me(token);
        try {
            return repository.listarTiposSupervision();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposTrabajo(String token) {
        authService.me(token);
        try {
            return repository.listarTiposTrabajo();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    public List<Map<String, Object>> listarTiposPenalizacion(String token) {
        authService.me(token);
        try {
            return repository.listarTiposPenalizacion();
        } catch (DataAccessException ex) {
            return java.util.Collections.emptyList();
        }
    }

    private Integer resolveIdUsuario(AuthMeResponse me) {
        Integer idUsuario = me != null && me.getUsuario() != null ? me.getUsuario().getIdUsuario() : null;
        if (idUsuario == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_INVALID",
                    "No se pudo identificar el usuario de la sesion."
            );
        }
        return idUsuario;
    }

    private String resolveSucursalNombre(String sucursal, AuthMeResponse me) {
        if (!isBlank(sucursal)) {
            return SucursalCanonicalizer.canonicalize(sucursal);
        }

        Integer idSucursal = me.getUsuario() == null ? null : me.getUsuario().getIdSucursal();
        if (idSucursal == null) {
            return null;
        }

        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return null;
    }

    private void validarRangoFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "fechaDesde no puede ser mayor a fechaHasta."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<Map<String, Object>> filterAndLimitTecnicos(List<Map<String, Object>> source, String query, Integer limit) {
        if (source == null || source.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        String term = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        int resolvedLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 300);
        java.util.List<Map<String, Object>> filtered = new java.util.ArrayList<>();

        for (Map<String, Object> row : source) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            if (term.isEmpty()) {
                filtered.add(row);
            } else {
                String text = row.toString().toLowerCase(java.util.Locale.ROOT);
                if (text.contains(term)) {
                    filtered.add(row);
                }
            }
            if (filtered.size() >= resolvedLimit) {
                break;
            }
        }
        return filtered;
    }
}
