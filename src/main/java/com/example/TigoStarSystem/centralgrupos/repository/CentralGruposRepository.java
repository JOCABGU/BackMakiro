package com.example.TigoStarSystem.centralgrupos.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CentralGruposRepository {

    public List<Map<String, Object>> listarGrupos(JdbcTemplate template, Integer idUsuarioEjecutor) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_ListarCentral ?",
                idUsuarioEjecutor
        );
    }

    public List<Map<String, Object>> listarSupervisoresFiltro(JdbcTemplate template) {
        return template.queryForList("EXEC dbo.spx_Grupo_FiltroSupervisoresCentral");
    }

    public List<Map<String, Object>> listarTecnicosFiltro(JdbcTemplate template) {
        return template.queryForList("EXEC dbo.spx_Grupo_FiltroTecnicosCentral");
    }

    public List<Map<String, Object>> crearGrupo(JdbcTemplate template, Integer idUsuarioEjecutor, String nombre) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_CrearCentral ?, ?",
                idUsuarioEjecutor,
                nombre
        );
    }

    public List<Map<String, Object>> asignarSupervisor(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioSupervisor) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_AsignarSupervisorCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioSupervisor
        );
    }

    public List<Map<String, Object>> asignarTecnico(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_AsignarTecnicoCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> quitarTecnico(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_QuitarTecnicoCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> eliminarGrupo(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_EliminarCentral ?, ?",
                idUsuarioEjecutor,
                idGrupo
        );
    }

    public List<Map<String, Object>> marcarSupervisorAusente(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_MarcarSupervisorAusenteCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }

    public List<Map<String, Object>> restaurarSupervisor(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_RestaurarSupervisorCentral ?, ?",
                idUsuarioEjecutor,
                idGrupo
        );
    }

    public List<Map<String, Object>> cambiarColaboradorBackup(
            JdbcTemplate template,
            Integer idUsuarioEjecutor,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        return template.queryForList(
                "EXEC dbo.spx_Grupo_CambiarColaboradorBackupCentral ?, ?, ?",
                idUsuarioEjecutor,
                idGrupo,
                idUsuarioTecnico
        );
    }
}
