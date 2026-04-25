package com.example.TigoStarSystem.centralgrupos.service;

import com.example.TigoStarSystem.auth.dto.AuthLoginResponse;
import com.example.TigoStarSystem.auth.dto.AuthMeResponse;
import com.example.TigoStarSystem.auth.dto.SucursalResponse;
import com.example.TigoStarSystem.auth.service.AuthService;
import com.example.TigoStarSystem.centralgrupos.repository.CentralGruposRepository;
import com.example.TigoStarSystem.common.ApiException;
import com.example.TigoStarSystem.config.DbConnectionManager;
import com.example.TigoStarSystem.supervisor.SucursalCanonicalizer;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CentralGruposService {
    private final CentralGruposRepository repository;
    private final DbConnectionManager dbConnectionManager;
    private final AuthService authService;

    public CentralGruposService(
            CentralGruposRepository repository,
            DbConnectionManager dbConnectionManager,
            AuthService authService) {
        this.repository = repository;
        this.dbConnectionManager = dbConnectionManager;
        this.authService = authService;
    }

    public List<Map<String, Object>> listarGrupos(String token, String sucursal) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        return repository.listarGrupos(template, usuario.getIdUsuario());
    }

    public List<Map<String, Object>> listarSupervisoresFiltro(String token, String sucursal) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        return repository.listarSupervisoresFiltro(template);
    }

    public List<Map<String, Object>> listarTecnicosFiltro(String token, String sucursal) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        return repository.listarTecnicosFiltro(template);
    }

    public Map<String, Object> crearGrupo(String token, String sucursal, String nombre) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.crearGrupo(template, usuario.getIdUsuario(), nombre);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo crear el grupo.");
        }
        return rows.get(0);
    }

    public Map<String, Object> asignarSupervisor(
            String token,
            String sucursal,
            Integer idGrupo,
            Integer idUsuarioSupervisor) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.asignarSupervisor(template, usuario.getIdUsuario(), idGrupo, idUsuarioSupervisor);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo asignar supervisor al grupo.");
        }
        return rows.get(0);
    }

    public Map<String, Object> asignarTecnico(
            String token,
            String sucursal,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.asignarTecnico(template, usuario.getIdUsuario(), idGrupo, idUsuarioTecnico);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo asignar tecnico al grupo.");
        }
        return rows.get(0);
    }

    public Map<String, Object> quitarTecnico(
            String token,
            String sucursal,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.quitarTecnico(template, usuario.getIdUsuario(), idGrupo, idUsuarioTecnico);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo quitar tecnico del grupo.");
        }
        return rows.get(0);
    }

    public Map<String, Object> eliminarGrupo(
            String token,
            String sucursal,
            Integer idGrupo) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.eliminarGrupo(template, usuario.getIdUsuario(), idGrupo);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo eliminar el grupo.");
        }
        return rows.get(0);
    }

    public Map<String, Object> marcarSupervisorAusente(
            String token,
            String sucursal,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.marcarSupervisorAusente(template, usuario.getIdUsuario(), idGrupo, idUsuarioTecnico);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo marcar supervisor ausente.");
        }
        return rows.get(0);
    }

    public Map<String, Object> restaurarSupervisor(
            String token,
            String sucursal,
            Integer idGrupo) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.restaurarSupervisor(template, usuario.getIdUsuario(), idGrupo);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo restaurar supervisor.");
        }
        return rows.get(0);
    }

    public Map<String, Object> cambiarColaboradorBackup(
            String token,
            String sucursal,
            Integer idGrupo,
            Integer idUsuarioTecnico) {
        AuthLoginResponse usuario = requireCentral(token);
        JdbcTemplate template = resolveTemplate(sucursal, usuario);
        List<Map<String, Object>> rows = repository.cambiarColaboradorBackup(template, usuario.getIdUsuario(), idGrupo, idUsuarioTecnico);
        if (rows == null || rows.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DATA", "No se pudo cambiar colaborador temporal.");
        }
        return rows.get(0);
    }

    private AuthLoginResponse requireCentral(String token) {
        AuthMeResponse me = authService.me(token);
        AuthLoginResponse usuario = me.getUsuario();
        String rol = normalize(usuario == null ? null : usuario.getRol());
        if (!"central".equals(rol)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN_CENTRAL_ONLY",
                    "Esta funcionalidad es solo para rol Central."
            );
        }
        return usuario;
    }

    private JdbcTemplate resolveTemplate(String sucursalParam, AuthLoginResponse usuario) {
        String sucursal = SucursalCanonicalizer.canonicalize(sucursalParam);
        if (isBlank(sucursal)) {
            sucursal = resolveSucursalDesdeUsuario(usuario);
        }
        if ("sucre".equals(normalize(sucursal))) {
            return dbConnectionManager.connDb("sucre");
        }
        return dbConnectionManager.connDb("operativa");
    }

    private String resolveSucursalDesdeUsuario(AuthLoginResponse usuario) {
        if (usuario == null || usuario.getIdSucursal() == null) {
            return null;
        }
        Integer idSucursal = usuario.getIdSucursal();
        List<SucursalResponse> sucursales = authService.listarSucursales();
        for (SucursalResponse item : sucursales) {
            if (item != null && idSucursal.equals(item.getIdSucursal())) {
                return SucursalCanonicalizer.canonicalize(item.getSucursal());
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
