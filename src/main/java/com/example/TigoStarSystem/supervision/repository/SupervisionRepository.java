package com.example.TigoStarSystem.supervision.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class SupervisionRepository {
    private static final String SP_LISTAR =
            "EXEC dbo.spx_ListarSupervisionManual ?, ?, ?, ?";
    private static final String SP_OBTENER_DETALLE =
            "EXEC dbo.spx_ObtenerSupervisionManualPorId ?, ?";
    private static final String SP_REGISTRAR =
            "EXEC dbo.spx_RegistrarSupervisionManual ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

    private final JdbcTemplate tigohogarJdbcTemplate;

    public SupervisionRepository(@Qualifier("tigohogarJdbcTemplate") JdbcTemplate tigohogarJdbcTemplate) {
        this.tigohogarJdbcTemplate = tigohogarJdbcTemplate;
    }

    public List<Map<String, Object>> listar(String idSupervisor, java.time.LocalDate fechaDesde, java.time.LocalDate fechaHasta, Integer limite) {
        return tigohogarJdbcTemplate.queryForList(
                SP_LISTAR,
                idSupervisor,
                fechaDesde == null ? null : Date.valueOf(fechaDesde),
                fechaHasta == null ? null : Date.valueOf(fechaHasta),
                resolveLimit(limite)
        );
    }

    public Map<String, Object> obtenerDetalle(String idSupervision, String idSupervisor) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(SP_OBTENER_DETALLE, idSupervision, idSupervisor);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public String registrar(
            Integer idSupervisor,
            String idTecnicoPrincipal,
            String idTecnicoAuxiliar,
            String idTipoSupervision,
            String idTipoTrabajo,
            String idTipoPenalizacion,
            String supervisionPor,
            String tecnologia,
            String codigo,
            String ordenTrabajo,
            String tipoRevision,
            String fotoBoletaSupervision,
            String fotoCanalesPilos,
            String fotoNivelesDocsis,
            String fotoMedicionRuido,
            String fotoBarridoCanales,
            String fotoObservacion1,
            String fotoObservacion2,
            String fotoObservacion3,
            String fotoObservacion4,
            String observacion,
            String descripcionAdicionalObservacion,
            String ubicacion) {
        List<Map<String, Object>> rows = tigohogarJdbcTemplate.queryForList(
                SP_REGISTRAR,
                idSupervisor,
                trimToNull(idTecnicoPrincipal),
                trimToNull(idTecnicoAuxiliar),
                trimToNull(idTipoSupervision),
                trimToNull(idTipoTrabajo),
                trimToNull(idTipoPenalizacion),
                trimToNull(supervisionPor),
                trimToNull(tecnologia),
                trimToNull(codigo),
                trimToNull(ordenTrabajo),
                trimToNull(tipoRevision),
                trimToNull(fotoBoletaSupervision),
                trimToNull(fotoCanalesPilos),
                trimToNull(fotoNivelesDocsis),
                trimToNull(fotoMedicionRuido),
                trimToNull(fotoBarridoCanales),
                trimToNull(fotoObservacion1),
                trimToNull(fotoObservacion2),
                trimToNull(fotoObservacion3),
                trimToNull(fotoObservacion4),
                trimToNull(observacion),
                trimToNull(descripcionAdicionalObservacion),
                trimToNull(ubicacion)
        );

        if (rows != null && !rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            Object id = findValue(row, "idSupervision", "id_supervision", "Id_Supervision");
            if (id == null && !row.isEmpty()) {
                id = row.values().iterator().next();
            }
            if (id != null) {
                String text = String.valueOf(id).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }

        throw new IllegalStateException("No se pudo obtener Id_Supervision generado.");
    }

    public List<Map<String, Object>> listarTiposSupervision() {
        return tigohogarJdbcTemplate.queryForList(
                "SELECT Id_TipoSupervision AS idTipoSupervision, TipoSupervision AS tipoSupervision " +
                        "FROM dbo.tbl_TipoSupervision " +
                        "ORDER BY TipoSupervision"
        );
    }

    public List<Map<String, Object>> listarTiposTrabajo() {
        return tigohogarJdbcTemplate.queryForList(
                "SELECT Id_TipoTrabajo AS idTipoTrabajo, TipoTrabajo AS tipoTrabajo " +
                        "FROM dbo.tbl_TipoTrabajo " +
                        "ORDER BY TipoTrabajo"
        );
    }

    public List<Map<String, Object>> listarTiposPenalizacion() {
        return tigohogarJdbcTemplate.queryForList(
                "SELECT Id_TipoPenalizacion AS idTipoPenalizacion, TipoPenalizacion AS tipoPenalizacion " +
                        "FROM dbo.tbl_TipoPenalizacion " +
                        "ORDER BY TipoPenalizacion"
        );
    }

    private int resolveLimit(Integer limite) {
        if (limite == null || limite <= 0) {
            return 200;
        }
        return Math.min(limite, 1000);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Object findValue(Map<String, Object> row, String... keys) {
        if (row == null || row.isEmpty() || keys == null || keys.length == 0) {
            return null;
        }
        Map<String, Object> normalized = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            normalized.put(normalize(entry.getKey()), entry.getValue());
        }
        for (String key : keys) {
            String normalizedKey = normalize(key);
            if (normalized.containsKey(normalizedKey)) {
                return normalized.get(normalizedKey);
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }
}
