# Sistema de Clinicas Psicologia del Peru

Sistema de gestion clinica para la sucursal Cono Sur. Cubre flujos de anfitriona, recepcion, caja y psicologia con autenticacion JWT, backend Spring Boot y base de datos PostgreSQL en Neon.

## Estructura

```text
.
├── backend/      # API REST Spring Boot 3 / Java 17
├── frontend/     # Angular
├── database/     # Script PostgreSQL para Neon
└── Figma/        # Diseno original
```

## Stack Actual

- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL driver.
- Frontend: Angular, Bootstrap, HttpClient con interceptor JWT.
- Base de datos: PostgreSQL compatible con Neon.
- API principal: Spring Boot bajo `/api`.
- PostgREST: si se usa, debe apuntar al mismo esquema PostgreSQL definido en `database/init.sql`.

## Base de Datos PostgreSQL / Neon

El esquema oficial esta en:

```bash
database/init.sql
```

El script usa nombres en minuscula, que coinciden con las entidades JPA:

```text
rol
usuario
especialidad
psicologo
horariopsicologo
paciente
ticket
cita
deuda
comprobantepago
procesoterapeutico
sesion
```

Para cargarlo en Neon con `psql`:

```bash
psql "postgresql://USUARIO:PASSWORD@HOST/DB?sslmode=require" -f database/init.sql
```

Si ya tienes variables en `.env`, deben existir estas claves:

```env
DB_HOST=
DB_PORT=5432
DB_NAME=
DB_USER=
DB_PASSWORD=
JWT_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
```

## Desarrollo Local

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

En Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm start
```

Angular usa `/api` y el proxy de desarrollo envia esas llamadas a `http://localhost:8080`.

## Usuarios Demo

Password demo: `123`

| Usuario     | Rol        | Ruta        |
|-------------|------------|-------------|
| admin       | ADMIN      | /admin      |
| recepcion   | RECEPCION  | /recepcion  |
| caja        | CAJA       | /caja       |
| psicologo   | PSICOLOGO  | /psicologia |
| anfitriona  | ANFITRIONA | /anfitriona |

## Endpoints Principales

Auth:

```text
POST /api/auth/login
GET  /api/auth/me
PUT  /api/auth/me
POST /api/auth/recuperar
POST /api/auth/recuperar/verificar
```

Anfitriona:

```text
POST /api/anfitriona/tickets/emitir
GET  /api/anfitriona/tickets/hoy
```

Recepcion:

```text
GET   /api/recepcion/tickets
GET   /api/recepcion/tickets/actual
PATCH /api/recepcion/tickets/{id}/llamar
PATCH /api/recepcion/tickets/{id}/finalizar
GET   /api/recepcion/pacientes/buscar?q=
GET   /api/recepcion/pacientes/dni/{dni}
POST  /api/recepcion/pacientes
GET   /api/recepcion/especialidades
GET   /api/recepcion/psicologos
GET   /api/recepcion/psicologos/{id}/horario-disponible
POST  /api/recepcion/citas
```

Caja:

```text
GET  /api/caja/deudas/buscar
POST /api/caja/pagar/{deudaId}
GET  /api/caja/comprobantes/{id}
```

Psicologia:

```text
GET   /api/psicologia/agenda
PATCH /api/psicologia/citas/{id}/estado
GET   /api/psicologia/pacientes/{id}/proceso
POST  /api/psicologia/pacientes/{id}/proceso
PATCH /api/psicologia/procesos/{id}/fase
POST  /api/psicologia/sesiones
GET   /api/psicologia/sesiones/proceso/{procesoId}
```

## Verificacion

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run build
```

Nota: el build frontend puede requerir ajustar budgets de Angular si los estilos/fuentes superan el limite configurado.
