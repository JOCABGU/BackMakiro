package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaRowRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaWebRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaWebResponse;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaRepository;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaWebRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ConformacionCuadrillaWebService {
    private final ConformacionCuadrillaWebRepository repository;
    private final ConformacionCuadrillaRepository backofficeRepository;
    private final ConformacionCuadrillaMailService mailService;
    private final ConformacionCuadrillaRequestValidator validator;
    private final AuthService authService;

    public ConformacionCuadrillaWebService(
            ConformacionCuadrillaWebRepository repository,
            ConformacionCuadrillaRepository backofficeRepository,
            ConformacionCuadrillaMailService mailService,
            AuthService authService) {
        this.repository = repository;
        this.backofficeRepository = backofficeRepository;
        this.mailService = mailService;
        this.authService = authService;
        this.validator = new ConformacionCuadrillaRequestValidator();
    }

    public List<ConformacionCuadrillaWebResponse> listar(
            LocalDate fecha,
            String sucursal,
            Integer limite,
            String token) {
        if (limite != null && limite < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "limite no puede ser negativo."
            );
        }
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        return repository.listar(fecha, sucursalResuelta, limite);
    }

    public ConformacionCuadrillaWebResponse obtenerPorId(Long id, String sucursal, String token) {
        validarId(id);
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        ConformacionCuadrillaWebResponse row = repository.obtenerPorId(id, sucursalResuelta);
        if (row == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "Registro de conformacion cuadrilla web no encontrado."
            );
        }
        return row;
    }

    public ConformacionCuadrillaWebResponse crear(ConformacionCuadrillaWebRequest request, String token) {
        completarContextoSesion(request, token);
        validator.validarWeb(request);
        Long id = repository.crear(request);
        if (id == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INSERT_FAILED",
                    "No se pudo registrar la conformacion cuadrilla web."
            );
        }

        enviarCorreo(request);
        ConformacionCuadrillaWebResponse persisted = repository.obtenerPorId(id, request.getSucursal());
        return persisted == null ? repository.obtenerPorIdPersistido(id) : persisted;
    }

    public ConformacionCuadrillaWebResponse actualizar(Long id, ConformacionCuadrillaWebRequest request, String token) {
        validarId(id);
        completarContextoSesion(request, token);
        validator.validarWeb(request);
        int affected = repository.actualizar(id, request);
        if (affected == 0) {
            int affectedBackoffice = backofficeRepository.actualizarFila(id, mapToBackOfficeRow(request));
            if (affectedBackoffice > 0) {
                enviarCorreo(request);
                ConformacionCuadrillaWebResponse byRoute = repository.obtenerPorId(id, request.getSucursal());
                if (byRoute != null) {
                    return byRoute;
                }
                return construirRespuestaDesdeRequest(id, request);
            }

            Long nuevoId = repository.crear(request);
            if (nuevoId == null) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "UPSERT_FAILED",
                        "No se pudo actualizar ni registrar la conformacion cuadrilla web."
                );
            }
            enviarCorreo(request);
            ConformacionCuadrillaWebResponse byRoute = repository.obtenerPorId(id, request.getSucursal());
            if (byRoute != null) {
                return byRoute;
            }
            ConformacionCuadrillaWebResponse bySavedId = repository.obtenerPorIdPersistido(nuevoId);
            if (bySavedId != null) {
                return bySavedId;
            }
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UPSERT_READ_FAILED",
                    "Conformacion cuadrilla web guardada, pero no se pudo recuperar el registro."
            );
        }
        enviarCorreo(request);
        ConformacionCuadrillaWebResponse persisted = repository.obtenerPorId(id, request.getSucursal());
        return persisted == null ? repository.obtenerPorIdPersistido(id) : persisted;
    }

    private ConformacionCuadrillaWebResponse construirRespuestaDesdeRequest(Long id, ConformacionCuadrillaWebRequest request) {
        ConformacionCuadrillaWebResponse out = new ConformacionCuadrillaWebResponse();
        out.setId(id);
        out.setFecha(request.getFecha());
        out.setEstado(request.getEstado());
        out.setActividad(request.getActividad());
        out.setIdTecnico(request.getIdTecnico());
        out.setCuentaSf(request.getCuentaSf());
        out.setSalesforce(request.getSalesforce());
        out.setHabilidad(request.getHabilidad());
        out.setVehiculo(request.getVehiculo());
        out.setGrupo(request.getGrupo());
        out.setAlmacen(request.getAlmacen());
        out.setGrupoDigitacion(request.getGrupoDigitacion());
        out.setIdUsuarioDigitador(request.getIdUsuarioDigitador());
        out.setDigitador(request.getDigitador());
        out.setTecnico(request.getTecnico());
        out.setIdTecnicoAuxiliar(request.getIdTecnicoAuxiliar());
        out.setAuxiliar(request.getAuxiliar());
        out.setIdUsuarioSupervisor(request.getIdUsuarioSupervisor());
        out.setSupervisorACargo(request.getSupervisorACargo());
        out.setSucursal(request.getSucursal());
        out.setObservacion(request.getObservacion());
        out.setIdUsuarioRegistra(request.getIdUsuarioRegistra());
        out.setEEliminado(false);
        return out;
    }

    public int eliminar(Long id) {
        validarId(id);
        int affected = repository.eliminar(id);
        if (affected == 0) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "Registro de conformacion cuadrilla web no encontrado."
            );
        }
        return affected;
    }

    public List<Map<String, Object>> listarTecnicos(String q, Integer limit, String sucursal, String token) {
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        return TecnicoSearchUtil.filterAndLimit(repository.listarTecnicos(sucursalResuelta), q, limit);
    }

    public List<Map<String, Object>> obtenerTecnicoDetalle(Integer idTecnico, String sucursal, String token) {
        if (idTecnico == null || idTecnico <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idTecnico es requerido."
            );
        }
        String sucursalResuelta = resolveSucursalNombre(sucursal, token);
        return repository.obtenerTecnicoDetalle(idTecnico, sucursalResuelta);
    }

    public List<Map<String, Object>> listarAuxiliares(String sucursal, String token) {
        return repository.listarAuxiliares(resolveSucursalNombre(sucursal, token));
    }

    public List<Map<String, Object>> listarDigitadores(String sucursal, String token) {
        return repository.listarDigitadores(resolveSucursalNombre(sucursal, token));
    }

    public List<Map<String, Object>> listarSupervisores(String sucursal, String token) {
        return repository.listarSupervisores(resolveSucursalNombre(sucursal, token));
    }

    public List<Map<String, Object>> listarActividades(String sucursal, String token) {
        return repository.listarActividades(resolveSucursalNombre(sucursal, token));
    }

    public List<Map<String, Object>> listarVehiculos(String filtro, String sucursal, String token) {
        return repository.listarVehiculos(resolveSucursalNombre(sucursal, token), filtro);
    }

    public List<Map<String, Object>> listarSucursales(String sucursal, String token) {
        return repository.listarSucursales(resolveSucursalNombre(sucursal, token));
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "id es requerido."
            );
        }
    }

    private void enviarCorreo(ConformacionCuadrillaWebRequest request) {
        List<ConformacionCuadrillaRowRequest> confirmadas = new ArrayList<>();
        confirmadas.add(mapToBackOfficeRow(request));
        mailService.enviarDetalleCuadrillasNoConfirmadas(
                backofficeRepository.listarGruposFiltroEdicion(request.getSucursal()),
                confirmadas
        );
    }

    private ConformacionCuadrillaRowRequest mapToBackOfficeRow(ConformacionCuadrillaWebRequest in) {
        ConformacionCuadrillaRowRequest out = new ConformacionCuadrillaRowRequest();
        out.setFecha(in.getFecha());
        out.setEstado(in.getEstado());
        out.setActividad(in.getActividad());
        out.setIdTecnico(in.getIdTecnico());
        out.setCuentaSf(in.getCuentaSf());
        out.setSalesforce(in.getSalesforce());
        out.setHabilidad(in.getHabilidad());
        out.setVehiculo(in.getVehiculo());
        out.setGrupo(in.getGrupo());
        out.setAlmacen(in.getAlmacen());
        out.setGrupoDigitacion(in.getGrupoDigitacion());
        out.setIdUsuarioDigitador(in.getIdUsuarioDigitador());
        out.setDigitador(in.getDigitador());
        out.setTecnico(in.getTecnico());
        out.setIdTecnicoAuxiliar(in.getIdTecnicoAuxiliar());
        out.setAuxiliar(in.getAuxiliar());
        out.setIdUsuarioSupervisor(in.getIdUsuarioSupervisor());
        out.setSupervisorACargo(in.getSupervisorACargo());
        out.setSucursal(in.getSucursal());
        out.setObservacion(in.getObservacion());
        out.setIdUsuarioRegistra(in.getIdUsuarioRegistra());
        return out;
    }

    private void completarContextoSesion(ConformacionCuadrillaWebRequest request, String token) {
        if (request == null) {
            return;
        }
        if (isBlank(request.getSucursal())) {
            request.setSucursal(resolveSucursalNombre(null, token));
        }
    }

    private String resolveSucursalNombre(String sucursal, String token) {
        if (!isBlank(sucursal)) {
            return sucursal.trim();
        }
        if (isBlank(token)) {
            return null;
        }

        AuthMeResponse me = authService.me(token);
        Integer idSucursal = me.getUsuario() == null ? null : me.getUsuario().getIdSucursal();
        if (idSucursal == null) {
            return null;
        }

        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return item.getSucursal();
            }
        }
        return String.valueOf(idSucursal);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
