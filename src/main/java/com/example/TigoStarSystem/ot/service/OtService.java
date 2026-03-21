package com.example.TigoStarSystem.ot.service;

import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.ot.dto.OtCrearRequest;
import com.example.TigoStarSystem.ot.dto.OtCrearResponse;
import com.example.TigoStarSystem.ot.dto.OtModificarDatosRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaRequest;
import com.example.TigoStarSystem.ot.dto.OtModificarFechaResponse;
import com.example.TigoStarSystem.ot.dto.OtRealizadaRequest;
import com.example.TigoStarSystem.ot.repository.OtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OtService {
    private static final Logger logger = LoggerFactory.getLogger(OtService.class);
    private final OtRepository otRepository;

    public OtService(OtRepository otRepository) {
        this.otRepository = otRepository;
    }

    public List<Map<String, Object>> listarPorFecha(LocalDate fecha, Integer idSucursal) {
        logger.info("Listar OT por fecha={}", fecha);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenesPorFecha(fecha, idSucursal);
        logger.debug("Listar OT por fecha: filas={}", rows == null ? 0 : rows.size());
        return rows;
    }

    public List<Map<String, Object>> listarPorRango(LocalDate inicio, LocalDate fin, Integer idSucursal) {
        logger.info("Listar OT por rango inicio={}, fin={}", inicio, fin);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenesPorRango(inicio, fin, idSucursal);
        logger.debug("Listar OT por rango: filas={}", rows == null ? 0 : rows.size());
        return rows;
    }

    public Map<String, Object> obtenerPorId(Long idVenta, Integer idSucursal) {
        logger.info("Obtener OT por idVenta={}", idVenta);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenTrabajoPorIdVenta(idVenta, idSucursal);
        if (rows.isEmpty()) {
            throw notFound("Orden de trabajo no encontrada para id: " + idVenta);
        }
        return rows.get(0);
    }

    public Map<String, Object> obtenerPorNumero(String numeroOrden, Integer idSucursal) {
        logger.info("Obtener OT por numero={}", numeroOrden);
        List<Map<String, Object>> rows = otRepository.obtenerOrdenTrabajoPorNumero(numeroOrden, idSucursal);
        if (rows.isEmpty()) {
            throw notFound("Orden de trabajo no encontrada para numero: " + numeroOrden);
        }
        return rows.get(0);
    }

    public List<Map<String, Object>> obtenerDetalleInstalado(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleInstalado(idVenta, idSucursal);
    }

    public List<Map<String, Object>> obtenerDetalleRetirado(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleRetirado(idVenta, idSucursal);
    }

    public List<Map<String, Object>> obtenerDetalleExcedente(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleExcedente(idVenta, idSucursal);
    }

    public List<Map<String, Object>> obtenerDetalleCargoUsuario(Long idVenta, Integer idSucursal) {
        return otRepository.obtenerDetalleCargoUsuario(idVenta, idSucursal);
    }

    public OtCrearResponse crearOt(OtCrearRequest request, Integer idSucursal) {
        Map<String, Object> result = otRepository.registrarOt(
                request.getIdUsuario(),
                request.getIdRuta(),
                request.getIdTipoServicio(),
                request.getCodigoCliente(),
                request.getIdEstado(),
                request.getObservacion(),
                request.getTieneObservacion(),
                request.getIdSucursal(),
                request.getNombreCliente(),
                idSucursal
        );
        Integer idVenta = toInteger(findValue(result, "idventa", "id_venta"));
        Integer ordenTrabajo = toInteger(findValue(result, "ordentrabajo", "orden_trabajo"));
        return new OtCrearResponse(idVenta, ordenTrabajo);
    }

    public int registrarOtRealizada(OtRealizadaRequest request, Integer idSucursal) {
        return otRepository.modificarOtRealizada(
                request.getObservacion(),
                request.getIdEstado(),
                request.getNumeroOrden(),
                idSucursal
        );
    }

    public int modificarDatosOt(Long idVentaPath, OtModificarDatosRequest request, Integer idSucursal) {
        String numeroOrden = request.getNumeroOrden();
        if (numeroOrden == null || numeroOrden.trim().isEmpty()) {
            // TODO: Confirmar si el {id} del endpoint corresponde al NroOrden o al Id_Venta.
            numeroOrden = String.valueOf(idVentaPath);
        }
        return otRepository.modificarOtRealizada(
                request.getObservacion(),
                request.getIdEstado(),
                numeroOrden,
                idSucursal
        );
    }

    public OtModificarFechaResponse modificarFecha(Long idVenta, OtModificarFechaRequest request, Integer idSucursal) {
        List<Map<String, Object>> validacionModificacion =
                otRepository.sePuedeModificarOrdenTrabajo(
                        request.getFechaVieja(),
                        request.getFechaNueva(),
                        request.getIdRuta(),
                        idSucursal
                );
        List<Map<String, Object>> validacionCuadre =
                otRepository.validarCuadreRuta(request.getIdRuta(), request.getFechaNueva(), idSucursal);

        boolean puedeModificar = resultadoValido(validacionModificacion);
        boolean cuadreValido = resultadoValido(validacionCuadre);

        if (!puedeModificar || !cuadreValido) {
            Map<String, Object> details = new HashMap<>();
            details.put("validacionModificacion", validacionModificacion);
            details.put("validacionCuadre", validacionCuadre);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VALIDACION_CUADRE_FALLIDA",
                    "La OT no puede modificar su fecha segun las validaciones de ruta/cuadre.",
                    details
            );
        }

        int updated = otRepository.modificarOrdenTrabajoFecha(
                request.getIdUsuario(),
                request.getFechaNueva(),
                idVenta,
                idSucursal
        );
        return new OtModificarFechaResponse(updated, validacionCuadre, validacionModificacion);
    }

    public int anularSoloCu(Long idVenta, Integer idUsuario, Integer idSucursal) {
        if (idUsuario == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "idUsuario es requerido para anular solo CU."
            );
        }
        return otRepository.eliminarCodigoUsuarioVenta(idVenta, idUsuario, idSucursal);
    }

    public void anularConCu(Long idVenta, Integer idUsuario) {
        Map<String, Object> details = new HashMap<>();
        details.put("idVenta", idVenta);
        details.put("idUsuario", idUsuario);
        details.put("procedimientosRequeridos",
                java.util.Collections.singletonList("TODO: SP de anulacion OT + CU (rollback estados)."));
        throw new ApiException(
                HttpStatus.NOT_IMPLEMENTED,
                "MISSING_STORED_PROCEDURE",
                "No existe un SP definido para anular OT + CU y revertir estados.",
                details
        );
    }

    public List<Map<String, Object>> filtrarListado(
            List<Map<String, Object>> rows,
            Integer idUsuario,
            String rol,
            Boolean pendiente) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        boolean filtrarPendientes = pendiente == null || pendiente;
        boolean filtrarUsuario = esTecnico(rol) && idUsuario != null;

        if (!filtrarPendientes && !filtrarUsuario) {
            return rows;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (filtrarPendientes && !esPendiente(row)) {
                continue;
            }
            if (filtrarUsuario && !perteneceUsuario(row, idUsuario)) {
                continue;
            }
            result.add(row);
        }

        logger.debug(
                "Filtrado OT: total={}, result={}, pendiente={}, filtrarUsuario={}, idUsuario={}, rol={}",
                rows.size(),
                result.size(),
                filtrarPendientes,
                filtrarUsuario,
                idUsuario,
                rol
        );
        return result;
    }

    private boolean esTecnico(String rol) {
        if (rol == null) {
            return false;
        }
        String normalized = normalizeText(rol);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.contains("tecnico") || normalized.equals("tec") || normalized.contains("tech");
    }

    private boolean esPendiente(Map<String, Object> row) {
        Object flag = findValue(row,
                "otrealizada", "ot_realizada", "realizada", "realizado",
                "e_realizada", "e_realizado", "otrealizado", "ot_realizado");
        Boolean doneFlag = toBoolean(flag);
        if (doneFlag != null) {
            return !doneFlag;
        }

        Object estadoText = findValue(row,
                "estado", "estado_ot", "estadoot", "estado_orden", "estadoorden",
                "estadotrabajo", "estado_trabajo", "estado_venta", "estadoventa");
        if (estadoText instanceof String) {
            String normalized = normalizeText((String) estadoText);
            String compact = normalized.replace(" ", "");
            if (contieneEstadoFinal(normalized) || contieneEstadoFinal(compact)) {
                return false;
            }
            if (contieneEstadoPendiente(normalized) || contieneEstadoPendiente(compact)) {
                return true;
            }
            return true;
        }

        Object idEstado = findValue(row,
                "id_estado", "idestado", "id_estado_ot", "idestadot",
                "id_estadoorden", "idestadotrabajo");
        if (idEstado != null) {
            logger.debug("Estado numerico sin mapeo, se mantiene como pendiente. keys={}", row.keySet());
            return true;
        }

        return true;
    }

    private boolean perteneceUsuario(Map<String, Object> row, Integer idUsuario) {
        Object value = findValue(row,
                "idusuario", "id_usuario", "iduser", "usuarioid",
                "id_tecnico", "idtecnico", "tecnicoid",
                "id_vendedor", "idvendedor", "id_usuario_asignado",
                "idasignado", "id_asignado", "idpersonal", "id_personal",
                "idempleado", "id_empleado");
        if (value == null) {
            logger.debug("No se encontro campo de usuario en OT. keys={}", row.keySet());
            return false;
        }
        Integer rowId = toInteger(value);
        if (rowId == null) {
            logger.debug("No se pudo convertir el idUsuario de OT. value={}", value);
            return false;
        }
        return rowId.equals(idUsuario);
    }

    private boolean contieneEstadoFinal(String normalized) {
        return contiene(normalized,
                "realizada", "realizado",
                "cerrada", "cerrado",
                "finalizada", "finalizado",
                "anulada", "anulado",
                "cancelada", "cancelado",
                "completada", "completado",
                "entregada", "entregado");
    }

    private boolean contieneEstadoPendiente(String normalized) {
        return contiene(normalized,
                "pendiente",
                "abierto",
                "asignado",
                "enproceso",
                "en proceso",
                "programada",
                "programado");
    }

    private boolean contiene(String value, String... tokens) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String normalized = normalizeText(value.toString());
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.equals("si") || normalized.equals("true") || normalized.equals("1") || normalized.equals("s")) {
            return true;
        }
        if (normalized.equals("no") || normalized.equals("false") || normalized.equals("0") || normalized.equals("n")) {
            return false;
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Object findValue(Map<String, Object> row, String... candidates) {
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = normalizeKey(entry.getKey());
            normalized.put(key, entry.getValue());
        }
        for (String candidate : candidates) {
            String key = normalizeKey(candidate);
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace("_", "")
                .replace("-", "")
                .replace(" ", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private boolean resultadoValido(List<Map<String, Object>> rows) {
        // TODO: Ajustar esta logica cuando se conozcan columnas exactas de los SPs de validacion.
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        Map<String, Object> first = rows.get(0);
        for (Object value : first.values()) {
            if (value instanceof Boolean) {
                if (((Boolean) value)) {
                    return true;
                }
            }
            if (value instanceof Number) {
                if (((Number) value).intValue() == 1) {
                    return true;
                }
            }
            if (value instanceof String) {
                String normalized = ((String) value).trim().toUpperCase();
                if (normalized.equals("OK") || normalized.equals("SI") || normalized.equals("TRUE") || normalized.equals("1")) {
                    return true;
                }
            }
        }
        return false;
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
