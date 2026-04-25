package com.example.TigoStarSystem.tecnicoiniciojornada.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class TecnicoInicioJornadaRepository {

    public boolean existeRegistroHoy(JdbcTemplate template, Integer idTecnico) {
        Integer count = template.queryForObject(
                "SELECT COUNT(1) FROM dbo.tbl_InicioJornadaAlturas WHERE id_tecnico = ? AND CAST(fecha_registro AS DATE) = CAST(GETDATE() AS DATE) AND ISNULL(e_eliminado,0)=0",
                Integer.class,
                idTecnico
        );
        return count != null && count > 0;
    }

    public int marcarNoCierreAtrasado(JdbcTemplate template, Integer idTecnico) {
        return template.update(
                "UPDATE dbo.tbl_InicioJornadaAlturas " +
                        "SET no_marco_cierre = 1 " +
                        "WHERE id_tecnico = ? " +
                        "  AND CAST(fecha_registro AS DATE) < CAST(GETDATE() AS DATE) " +
                        "  AND fecha_cierre IS NULL " +
                        "  AND ISNULL(e_eliminado,0)=0 " +
                        "  AND ISNULL(no_marco_cierre,0)=0",
                idTecnico
        );
    }

    public Map<String, Object> estadoCierreHoy(JdbcTemplate template, Integer idTecnico) {
        List<Map<String, Object>> rows = template.queryForList(
                "SELECT TOP 1 id_inicio, fecha_registro, fecha_cierre, no_marco_cierre " +
                        "FROM dbo.tbl_InicioJornadaAlturas " +
                        "WHERE id_tecnico = ? " +
                        "  AND CAST(fecha_registro AS DATE) = CAST(GETDATE() AS DATE) " +
                        "  AND ISNULL(e_eliminado,0)=0 " +
                        "ORDER BY id_inicio DESC",
                idTecnico
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int countNoMarco(JdbcTemplate template, Integer idTecnico) {
        Integer count = template.queryForObject(
                "SELECT COUNT(1) FROM dbo.tbl_InicioJornadaAlturas WHERE id_tecnico = ? AND ISNULL(no_marco_cierre,0)=1 AND ISNULL(e_eliminado,0)=0",
                Integer.class,
                idTecnico
        );
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> listarEncargados(JdbcTemplate tecnicosTemplate) {
        List<Map<String, Object>> rows = queryForListConSpAlternativos(
                tecnicosTemplate,
                "EXEC dbo.spx_ObtenerSupervisoresConformacionCuadrillaWeb",
                "EXEC spx_ObtenerSupervisoresConformacionCuadrillaWeb",
                "EXEC dbo.spx_ObtenerSupervisores",
                "EXEC spx_ObtenerSupervisores"
        );
        return normalizarEncargados(rows);
    }

    private List<Map<String, Object>> queryForListConSpAlternativos(
            JdbcTemplate template,
            String... sqlAlternativos
    ) {
        if (template == null || sqlAlternativos == null || sqlAlternativos.length == 0) {
            return new ArrayList<>();
        }
        for (String sql : sqlAlternativos) {
            try {
                List<Map<String, Object>> rows = template.queryForList(sql);
                if (rows != null && !rows.isEmpty()) {
                    return rows;
                }
            } catch (DataAccessException ex) {
                // intenta siguiente SP
            }
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> normalizarEncargados(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        for (Map<String, Object> row : rows) {
            Object id = findValue(row,
                    "idEncargado", "id_encargado", "id_usuario_supervisor", "idusuariosupervisor",
                    "id_supervisor", "idsupervisor", "id_usuario", "idusuario");
            Object nombre = findValue(row,
                    "encargado", "supervisor", "supervisor_a_cargo", "supervisorACargo", "nombre");
            if (id == null || nombre == null) {
                continue;
            }
            String idText = String.valueOf(id).trim();
            String nombreText = String.valueOf(nombre).trim();
            if (idText.isEmpty() || nombreText.isEmpty()) {
                continue;
            }
            Map<String, Object> normalizada = new LinkedHashMap<>();
            normalizada.put("idEncargado", idText);
            normalizada.put("encargado", nombreText);
            out.add(normalizada);
        }
        return out;
    }

    private Object findValue(Map<String, Object> row, String... keys) {
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

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    public List<Map<String, Object>> registrar(
            JdbcTemplate template,
            Integer idTecnico,
            Integer idAuxiliar,
            Integer idEncargado,
            String capacitado,
            String charla,
            String botiquin,
            String extintor,
            LocalDate fechaVencimiento,
            String equipoEpp,
            String estadoEpp,
            String apr,
            String escalera,
            String anclaje,
            String imagen
    ) {
        template.update(
                "INSERT INTO dbo.tbl_InicioJornadaAlturas (" +
                        "id_tecnico, id_auxiliar, id_encargado, fecha_registro, pendiente, " +
                        "capacitado, charla, botiquin, extintor, fecha_vencimiento, " +
                        "equipo_epp, estado_epp, apr, escalera, anclaje, imagen, e_eliminado" +
                        ") VALUES (?, ?, ?, GETDATE(), 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                idTecnico,
                idAuxiliar,
                idEncargado,
                capacitado,
                charla,
                botiquin,
                extintor,
                fechaVencimiento == null ? null : Date.valueOf(fechaVencimiento),
                equipoEpp,
                estadoEpp,
                apr,
                escalera,
                anclaje,
                imagen
        );

        return template.queryForList(
                "SELECT TOP 1 * FROM dbo.tbl_InicioJornadaAlturas WHERE id_tecnico = ? ORDER BY id_inicio DESC",
                idTecnico
        );
    }

    public List<Map<String, Object>> cerrarJornada(
            JdbcTemplate template,
            Integer idTecnico,
            String codigoCliente,
            Boolean danoMaterial,
            String observacionMaterial,
            Boolean danoPersona,
            String observacionPersona,
            Boolean novedadesTrabajo,
            String observacionNovedades,
            String ubicacionGeoRef
    ) {
        int affected = template.update(
                "UPDATE dbo.tbl_InicioJornadaAlturas " +
                        "SET fecha_cierre = GETDATE(), " +
                        "    codigo_cliente = ?, " +
                        "    dano_material = ?, " +
                        "    observacion_material = ?, " +
                        "    dano_persona = ?, " +
                        "    observacion_persona = ?, " +
                        "    novedades_trabajo = ?, " +
                        "    observacion_novedades = ?, " +
                        "    ubicacion_georef = ?, " +
                        "    no_marco_cierre = 0 " +
                        "WHERE id_inicio = ( " +
                        "    SELECT TOP 1 id_inicio " +
                        "    FROM dbo.tbl_InicioJornadaAlturas " +
                        "    WHERE id_tecnico = ? " +
                        "      AND CAST(fecha_registro AS DATE) = CAST(GETDATE() AS DATE) " +
                        "      AND fecha_cierre IS NULL " +
                        "      AND ISNULL(e_eliminado,0)=0 " +
                        "    ORDER BY id_inicio DESC " +
                        ")",
                codigoCliente,
                danoMaterial,
                observacionMaterial,
                danoPersona,
                observacionPersona,
                novedadesTrabajo,
                observacionNovedades,
                ubicacionGeoRef,
                idTecnico
        );

        if (affected <= 0) {
            return List.of();
        }

        return template.queryForList(
                "SELECT TOP 1 * FROM dbo.tbl_InicioJornadaAlturas " +
                        "WHERE id_tecnico = ? AND CAST(fecha_registro AS DATE) = CAST(GETDATE() AS DATE) " +
                        "ORDER BY id_inicio DESC",
                idTecnico
        );
    }
}
