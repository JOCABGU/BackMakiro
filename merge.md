# Merge Notes - Modulo NPS

## Backend (TigoStarSystem)

### Nuevos archivos
- `src/main/java/com/example/TigoStarSystem/nps/controller/NpsController.java`
  - Endpoint `GET /nps/dashboard`
  - Recibe `fechaInicio`, `fechaFin`, `idSucursal`, `idSupervisor`, `idTecnico`

- `src/main/java/com/example/TigoStarSystem/nps/service/NpsService.java`
  - Logica por rol:
    - `tecnico`: solo su data (idTecnico = usuario logueado)
    - `supervisor`: data de su equipo (idSupervisor = usuario logueado)
    - `central/sistemas/admin`: puede elegir `idSucursal`, `idSupervisor`, `idTecnico`
  - Resuelve DB de sucursal desde sesion (`AuthService.listarSucursales`) y crea `JdbcTemplate` dinamico
  - Todo consulta via SP `SP_NPS_*`

- `src/main/java/com/example/TigoStarSystem/nps/repository/NpsRepository.java`
  - Llamadas exclusivas a SP:
    - `SP_NPS_LISTAR_SUPERVISORES_SUCURSAL`
    - `SP_NPS_LISTAR_TECNICOS_POR_SUPERVISOR`
    - `SP_NPS_LISTAR_TECNICOS_SUPERVISOR_CENTRAL`
    - `SP_NPS_DASHBOARD_CONSULTA`

- `SQL_SP_NPS_BASE.sql`
  - Script base para crear/alterar los SP requeridos.

### Archivos modificados
- Ningun archivo existente fue alterado en backend fuera del nuevo modulo `nps`.

## Frontend (TigoStarPage)

### Nuevos archivos
- `src/api/npsApi.ts`
  - Cliente API para `GET /nps/dashboard`

- `src/pages/NpsDashboardPage.tsx`
  - Pantalla inicial NPS con filtros:
    - rango de fecha
    - sucursal
    - encargado/supervisor
    - tecnico
  - Consume `filtros.supervisores`, `filtros.tecnicos` y `rows`

### Archivos modificados
- `src/routes/AppRoutes.tsx`
  - Nueva ruta: `/nps`

- `src/config/navigation.ts`
  - Nuevo item de menu: `NPS`
  - Roles habilitados: `tecnico`, `supervisor`, `central`, `sistemas`, `admin`

## Integracion esperada
1. Crear SP en BDControlOrdenes y en DB(s) de sucursal usando `SQL_SP_NPS_BASE.sql` (ajustar nombres fisicos si difieren).
2. Levantar backend.
3. Levantar frontend y entrar a `/nps`.
4. Validar con usuario tecnico, supervisor y central.
