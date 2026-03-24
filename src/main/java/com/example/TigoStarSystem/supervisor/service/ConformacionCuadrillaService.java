package com.example.TigoStarSystem.supervisor.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaCreateRequest;
import com.example.TigoStarSystem.supervisor.dto.ConformacionCuadrillaRowRequest;
import com.example.TigoStarSystem.supervisor.repository.ConformacionCuadrillaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
        List<Map<String, Object>> confirmadas = repository.listarConEliminados(fechaConsulta, sucursal, null, null);
        Map<Integer, Map<String, Object>> historicoByTecnico = indexUltimaConfirmacionPorTecnico(fechaConsulta, sucursal);
        Map<Integer, Map<String, Object>> tecnicosById = rowMapper.indexTecnicosById(repository.listarTecnicos(sucursal));

        Set<String> clavesConfirmadas = obtenerClavesConfirmadas(confirmadas);
        List<Map<String, Object>> pendientes = new ArrayList<>();

        for (Map<String, Object> row : catalogo) {
            String key = rowMapper.claveCuadrillaDesdeCatalogo(row);
            if (key == null || clavesConfirmadas.contains(key)) {
                continue;
            }
            Map<String, Object> pendiente = rowMapper.mapPendiente(row, sucursal, fechaConsulta, tecnicosById);
            Integer idTecnico = valueAsInteger(getCaseInsensitive(
                    pendiente,
                    "idTecnico",
                    "id_tecnico",
                    "idtecnico",
                    "id_vendedor",
                    "idvendedor"
            ));
            if (idTecnico != null) {
                Map<String, Object> historico = historicoByTecnico.get(idTecnico);
                if (historico != null) {
                    aplicarSugerenciasDesdeHistorico(pendiente, historico);
                }
            }
            pendientes.add(pendiente);
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
        List<Map<String, Object>> rows = repository.listarConEliminados(fechaConsulta, sucursal, null, null);
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

    private Map<Integer, Map<String, Object>> indexUltimaConfirmacionPorTecnico(LocalDate fechaConsulta, String sucursal) {
        Map<Integer, Map<String, Object>> exactAyer = new HashMap<>();
        Map<Integer, Map<String, Object>> previas = new HashMap<>();
        if (fechaConsulta == null) {
            return previas;
        }

        LocalDate fechaAyer = fechaConsulta.minusDays(1);
        List<Map<String, Object>> rows = repository.listarConEliminados(null, sucursal, null, null);
        if (rows == null || rows.isEmpty()) {
            return previas;
        }

        for (Map<String, Object> row : rows) {
            if (rowMapper.isEliminado(row)) {
                continue;
            }
            LocalDate fechaRow = valueAsLocalDate(getCaseInsensitive(row, "fecha"));
            if (fechaRow == null || !fechaRow.isBefore(fechaConsulta)) {
                continue;
            }

            Integer idTecnico = valueAsInteger(getCaseInsensitive(
                    row,
                    "id_tecnico",
                    "idtecnico",
                    "idTecnico",
                    "id_vendedor",
                    "idvendedor"
            ));
            if (idTecnico == null) {
                continue;
            }

            if (!previas.containsKey(idTecnico)) {
                previas.put(idTecnico, row);
            }
            if (fechaRow.equals(fechaAyer) && !exactAyer.containsKey(idTecnico)) {
                exactAyer.put(idTecnico, row);
            }
        }

        previas.putAll(exactAyer);
        return previas;
    }

    private void aplicarSugerenciasDesdeHistorico(Map<String, Object> pendiente, Map<String, Object> historico) {
        if (pendiente == null || pendiente.isEmpty() || historico == null || historico.isEmpty()) {
            return;
        }

        setIfBlankWithAliases(
                pendiente,
                getCaseInsensitive(historico, "id_tecnicoAuxiliar", "idtecnicoauxiliar", "id_tecnico_auxiliar"),
                "idTecnicoAuxiliar",
                "id_tecnicoAuxiliar",
                "id_tecnico_auxiliar",
                "idtecnicoauxiliar"
        );
        setIfBlankWithAliases(
                pendiente,
                getCaseInsensitive(historico, "auxiliar", "tecnicoauxiliar", "nombreauxiliar"),
                "auxiliar"
        );
        setIfBlankWithAliases(
                pendiente,
                getCaseInsensitive(historico, "idUsuarioDigitador", "id_usuario_digitador", "idusuariodigitador"),
                "idUsuarioDigitador",
                "id_usuario_digitador",
                "idusuariodigitador"
        );
        setIfBlankWithAliases(
                pendiente,
                getCaseInsensitive(historico, "digitador", "nombredigitador", "usuarioDigitador"),
                "digitador"
        );
        setIfBlankWithAliases(
                pendiente,
                getCaseInsensitive(historico, "idUsuarioSupervisor", "id_usuario_supervisor", "idusuariosupervisor", "idsupervisor"),
                "idUsuarioSupervisor",
                "id_usuario_supervisor",
                "idusuariosupervisor"
        );
        setIfBlankWithAliases(
                pendiente,
                getCaseInsensitive(historico, "supervisorACargo", "supervisor_a_cargo", "supervisor", "nombresupervisor"),
                "supervisorACargo",
                "supervisor_a_cargo",
                "supervisor"
        );

        Object vehiculoHistorico = getCaseInsensitive(historico, "vehiculo", "Vehiculo", "placa", "placavehiculo", "placaVehiculo");
        if (isBlankValue(getCaseInsensitive(pendiente, "vehiculo", "Vehiculo")) && !isBlankValue(vehiculoHistorico)) {
            pendiente.put("vehiculo", vehiculoHistorico);
            pendiente.put("Vehiculo", vehiculoHistorico);
        }
    }

    private void setIfBlankWithAliases(Map<String, Object> target, Object value, String... aliases) {
        if (target == null || aliases == null || aliases.length == 0 || isBlankValue(value)) {
            return;
        }
        Object current = getCaseInsensitive(target, aliases);
        if (!isBlankValue(current)) {
            return;
        }
        for (String alias : aliases) {
            target.put(alias, value);
        }
    }

    private Object getCaseInsensitive(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String current = normalizeKey(entry.getKey());
            for (String key : keys) {
                if (current.equals(normalizeKey(key))) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "").trim().toLowerCase();
    }

    private Integer valueAsInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate valueAsLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).toLocalDate();
        }
        try {
            return LocalDate.parse(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        return false;
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
