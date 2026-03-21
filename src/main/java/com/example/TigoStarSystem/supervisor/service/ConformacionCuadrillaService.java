package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaCreateRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaRowRequest;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class ConformacionCuadrillaService {
    private final ConformacionCuadrillaRepository repository;
    private final ConformacionCuadrillaMailService mailService;
    private final ConformacionCuadrillaRequestValidator validator;
    private final ConformacionCuadrillaRowMapper rowMapper;

    public ConformacionCuadrillaService(
            ConformacionCuadrillaRepository repository,
            ConformacionCuadrillaMailService mailService) {
        this.repository = repository;
        this.mailService = mailService;
        this.validator = new ConformacionCuadrillaRequestValidator();
        this.rowMapper = new ConformacionCuadrillaRowMapper();
    }

    public List<Map<String, Object>> listar(LocalDate fecha, String sucursal, Integer limite, Integer idTecnico) {
        return repository.listar(fecha, sucursal, limite, idTecnico);
    }

    public Map<String, Object> obtenerDetalle(Long id, String sucursal) {
        validarId(id);
        Map<String, Object> row = repository.obtenerPorId(id, sucursal);
        if (row == null || row.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "Registro de conformacion cuadrilla no encontrado."
            );
        }
        return rowMapper.mapConfirmada(row, sucursal, null);
    }

    public List<Map<String, Object>> listarTecnicos(String q, Integer limit, String sucursal) {
        return TecnicoSearchUtil.filterAndLimit(repository.listarTecnicos(sucursal), q, limit);
    }

    public List<Map<String, Object>> listarTecnicosFiltroEdicion(String q, Integer limit, String sucursal) {
        return TecnicoSearchUtil.filterAndLimit(repository.listarTecnicosFiltroEdicion(sucursal), q, limit);
    }

    public List<Map<String, Object>> listarAuxiliares(String q, Integer limit) {
        return TecnicoSearchUtil.filterAndLimit(repository.listarAuxiliares(), q, limit);
    }

    public List<Map<String, Object>> listarActividades() {
        List<Map<String, Object>> actividades = new ArrayList<>();
        actividades.add(crearActividad("TITULAR"));
        actividades.add(crearActividad("BACKUP"));
        return actividades;
    }

    public List<Map<String, Object>> obtenerTecnicoDetalle(Integer idTecnico) {
        if (idTecnico == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idTecnico es requerido."
            );
        }
        return repository.obtenerTecnicoDetalle(idTecnico);
    }

    public List<Map<String, Object>> listarDigitadores() {
        return repository.listarDigitadores();
    }

    public List<Map<String, Object>> listarDigitadoresFiltroEdicion() {
        return rowMapper.deduplicarPorPrimerCampoNoNulo(
                repository.listarDigitadoresFiltroEdicion(),
                "idUsuarioDigitador",
                "id_usuario_digitador",
                "idusuariodigitador",
                "digitador"
        );
    }

    public List<Map<String, Object>> listarSupervisores() {
        return repository.listarSupervisores();
    }

    public List<Map<String, Object>> listarVehiculos(String filtro) {
        return rowMapper.deduplicarPorPrimerCampoNoNulo(
                repository.listarVehiculos(filtro),
                "vehiculo",
                "placa",
                "placavehiculo",
                "placaVehiculo"
        );
    }

    public List<Map<String, Object>> listarVehiculosFiltroEdicion(Integer idTecnico) {
        return rowMapper.deduplicarPorPrimerCampoNoNulo(
                repository.listarVehiculosFiltroEdicion(idTecnico),
                "vehiculo",
                "placa",
                "placavehiculo",
                "placaVehiculo"
        );
    }

    public List<Map<String, Object>> listarGruposFiltroEdicion() {
        return listarGruposFiltroEdicion(null, null, null);
    }

    public List<Map<String, Object>> listarGruposFiltroEdicion(String sucursal, String q, Integer limit) {
        List<Map<String, Object>> grupos = rowMapper.deduplicarPorPrimerCampoNoNulo(
                repository.listarGruposFiltroEdicion(sucursal),
                "grupo",
                "nombre",
                "ruta",
                "descripcion"
        );
        return rowMapper.filtrarPorTextoYLimite(
                grupos,
                q,
                limit,
                "grupo",
                "cuadrilla",
                "ruta",
                "nombre",
                "nombrevendedor",
                "tecnico"
        );
    }

    public List<Map<String, Object>> listarCuadrillasPendientes(
            LocalDate fecha,
            String sucursal,
            String q,
            Integer limit) {
        LocalDate fechaConsulta = resolverFecha(fecha);
        List<Map<String, Object>> catalogo = repository.listarGruposFiltroEdicion(sucursal);
        List<Map<String, Object>> confirmadas = repository.listarConEliminadosCentral(fechaConsulta, sucursal, null, null);
        Map<Integer, Map<String, Object>> tecnicosById = rowMapper.indexTecnicosById(repository.listarTecnicos(sucursal));

        Set<String> clavesConfirmadas = obtenerClavesConfirmadas(confirmadas);
        List<Map<String, Object>> pendientes = new ArrayList<>();

        for (Map<String, Object> row : catalogo) {
            String key = rowMapper.claveCuadrillaDesdeCatalogo(row);
            if (key == null || clavesConfirmadas.contains(key)) {
                continue;
            }
            pendientes.add(rowMapper.mapPendiente(row, sucursal, fechaConsulta, tecnicosById));
        }

        return rowMapper.filtrarPorTextoYLimite(
                pendientes,
                q,
                limit,
                "grupo",
                "cuadrilla",
                "ruta",
                "nombre",
                "nombrevendedor",
                "tecnico",
                "vehiculo"
        );
    }

    public List<Map<String, Object>> listarCuadrillasConfirmadas(
            LocalDate fecha,
            String sucursal,
            String q,
            Integer limit) {
        return listarCuadrillasPorEstado(fecha, sucursal, q, limit, false);
    }

    public List<Map<String, Object>> listarCuadrillasEliminadas(
            LocalDate fecha,
            String sucursal,
            String q,
            Integer limit) {
        return listarCuadrillasPorEstado(fecha, sucursal, q, limit, true);
    }

    public List<Map<String, Object>> obtenerSucursalActual() {
        return repository.obtenerSucursalActual();
    }

    public int guardar(ConformacionCuadrillaCreateRequest request) {
        validarRequestCreacion(request);

        int total = 0;
        for (ConformacionCuadrillaRowRequest fila : request.getFilas()) {
            validator.validarBackoffice(fila);
            total += repository.guardarFilaConfirmada(fila);
        }

        enviarCorreoCuadrillasNoConfirmadas(request.getFilas());
        return total;
    }

    public int actualizar(Long id, ConformacionCuadrillaRowRequest request) {
        validarId(id);
        validator.validarBackoffice(request);
        int affected = repository.actualizarFila(id, request);
        if (affected > 0) {
            List<ConformacionCuadrillaRowRequest> filas = new ArrayList<>();
            filas.add(request);
            enviarCorreoCuadrillasNoConfirmadas(filas);
        }
        return affected;
    }

    private List<Map<String, Object>> listarCuadrillasPorEstado(
            LocalDate fecha,
            String sucursal,
            String q,
            Integer limit,
            boolean eliminadas) {
        LocalDate fechaConsulta = resolverFecha(fecha);
        List<Map<String, Object>> rows = repository.listarConEliminadosCentral(fechaConsulta, sucursal, null, null);
        List<Map<String, Object>> out = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            boolean eliminado = rowMapper.isEliminado(row);
            if (eliminadas != eliminado) {
                continue;
            }
            out.add(rowMapper.mapConfirmada(row, sucursal, fechaConsulta));
        }

        return rowMapper.filtrarPorTextoYLimite(
                out,
                q,
                limit,
                "grupo",
                "cuadrilla",
                "ruta",
                "tecnico",
                "auxiliar",
                "digitador",
                "supervisoracargo",
                "observacion",
                "vehiculo"
        );
    }

    private Set<String> obtenerClavesConfirmadas(List<Map<String, Object>> confirmadas) {
        Set<String> claves = new HashSet<>();
        if (confirmadas == null || confirmadas.isEmpty()) {
            return claves;
        }

        for (Map<String, Object> row : confirmadas) {
            if (rowMapper.isEliminado(row)) {
                continue;
            }
            String key = rowMapper.claveCuadrillaDesdeConfirmada(row);
            if (key != null) {
                claves.add(key);
            }
        }
        return claves;
    }

    private void enviarCorreoCuadrillasNoConfirmadas(List<ConformacionCuadrillaRowRequest> filas) {
        String sucursal = filas.isEmpty() ? null : filas.get(0).getSucursal();
        mailService.enviarDetalleCuadrillasNoConfirmadas(
                repository.listarGruposFiltroEdicion(sucursal),
                filas
        );
    }

    private void validarRequestCreacion(ConformacionCuadrillaCreateRequest request) {
        if (request == null || request.getFilas() == null || request.getFilas().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Debe enviar filas para registrar."
            );
        }
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

    private LocalDate resolverFecha(LocalDate fecha) {
        return fecha == null ? LocalDate.now() : fecha;
    }

    private Map<String, Object> crearActividad(String actividad) {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("actividad", actividad);
        return item;
    }
}
