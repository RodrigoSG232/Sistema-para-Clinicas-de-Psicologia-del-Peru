# 🧠 Sistema de Clínicas Psicología del Perú

Sistema de gestión clínica para la sucursal Cono Sur. Cubre los flujos de **Recepción**, **Caja** y **Psicología** con autenticación JWT y base de datos SQL Server.

---

## 🗂 Estructura del proyecto

```
clinica-psicologia/
├── database/
│   └── init.sql              ← Script de BD SQL Server (tablas + datos demo)
├── backend/                  ← API REST Spring Boot 3 / Java 17
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/clinica/psicologia/
│       ├── controller/       ← AuthController, RecepcionController, CajaController, PsicologiaController
│       ├── entity/           ← Entidades JPA
│       ├── repository/       ← Spring Data JPA
│       ├── security/         ← JwtUtil, JwtFilter
│       └── config/           ← SecurityConfig (CORS, roles)
├── frontend/                 ← Angular 18 (código original del equipo)
│   ├── Dockerfile            ← Build Angular + Nginx
│   ├── nginx.conf            ← SPA routing + proxy /api/ → backend
│   └── src/app/
│       ├── services/         ← AuthService, RecepcionService, CajaService, PsicologiaService
│       └── interceptors/     ← authInterceptor (adjunta JWT automáticamente)
└── docker-compose.yml        ← Orquesta los 3 servicios
```

---

## 🚀 Levantar el proyecto con Docker

### Pre-requisitos

- **Docker** ≥ 24 y **Docker Compose** ≥ 2.20 instalados
- Sin necesidad de instalar Java, Maven, Node, SQL Server

### 1. Copiar el frontend original al directorio frontend/

```bash
# (ya está incluido en el zip del proyecto)
# Sólo asegúrate de que la carpeta frontend/ tenga el código Angular
```

### 2. Levantar todos los servicios

```bash
cd clinica-psicologia
docker compose up --build
```

La primera vez tarda ~5-8 min (descarga imágenes, compila Maven y Angular).

### 3. Acceder

| Servicio   | URL                       |
| ---------- | ------------------------- |
| Frontend   | http://localhost:4200     |
| Backend    | http://localhost:8080/api |
| SQL Server | localhost:1433            |

### 4. Apagar

```bash
docker compose down          # mantiene los datos
docker compose down -v       # borra también el volumen de BD
```

---

## 👤 Usuarios demo (contraseña: `123`)

| Usuario     | Rol       | Módulo       |
| ----------- | --------- | ------------ |
| `recepcion` | RECEPCION | /recepcion   |
| `caja`      | CAJA      | /caja        |
| `psicologo` | PSICOLOGO | /psicologia  |
| `admin`     | ADMIN     | acceso total |

---

## 🗄 Base de datos SQL Server

### Tablas principales

```
Rol                   → roles del sistema
Usuario               → cuentas de acceso
Especialidad          → especialidades psicológicas (tarifa incluida)
Psicologo             → datos del profesional + FK a Usuario y Especialidad
HorarioPsicologo      → disponibilidad Lun-Sab 08:00-19:00
Paciente              → historia clínica (numero_historia: HC-XXXX)
Ticket                → cola de atención (A-001, A-002 …)
Cita                  → agendamiento + estado (PENDIENTE_PAGO→PAGADA→ATENDIDA)
Deuda                 → generada automáticamente al crear cita
ComprobantePago       → boleta/factura emitida en caja (B-00001 …)
ProcesoTerapeutico    → proceso global del paciente (4 fases)
Sesion                → registro clínico de cada consulta
```

### Conectar con SQL Server Management Studio (SSMS)

```
Servidor:  localhost,1433
Auth:      SQL Server Authentication
Usuario:   sa
Password:  Admin123!
BD:        ClinicaPsicologia
```

---

## 🔌 API REST — Endpoints principales

### Auth

```
POST   /api/auth/login       body: { username, password }  → { token, rol, ruta, … }
GET    /api/auth/me          header: Bearer <token>
```

### Recepción

```
GET    /api/recepcion/tickets                          → lista de tickets
POST   /api/recepcion/tickets/emitir                  → genera nuevo ticket (A-XXX)
PATCH  /api/recepcion/tickets/{id}/estado              body: { estado }
GET    /api/recepcion/pacientes/buscar?q=<texto>       → buscar por DNI/nombre
GET    /api/recepcion/pacientes/dni/{dni}              → buscar paciente por DNI
POST   /api/recepcion/pacientes                        → aperturar historia clínica
GET    /api/recepcion/especialidades                   → lista de especialidades
GET    /api/recepcion/psicologos?especialidadId=N      → psicólogos por especialidad
GET    /api/recepcion/psicologos/{id}/horario-disponible?fecha=YYYY-MM-DD
POST   /api/recepcion/citas                            → registrar cita + genera deuda
```

### Caja

```
GET    /api/caja/deudas/buscar?paciente=<texto>&concepto=<texto>
POST   /api/caja/pagar/{deudaId}    body: { medioPago: EFECTIVO|TARJETA|YAPE_PLIN }
GET    /api/caja/comprobantes/{id}
```

### Psicología

```
GET    /api/psicologia/agenda?fecha=YYYY-MM-DD         → agenda del psicólogo logueado
PATCH  /api/psicologia/citas/{id}/estado               body: { estado }
GET    /api/psicologia/pacientes/{id}/proceso
POST   /api/psicologia/pacientes/{id}/proceso          → iniciar proceso terapéutico
PATCH  /api/psicologia/procesos/{id}/fase              body: { faseActual: 1-4 }
POST   /api/psicologia/sesiones                        → guardar evolución + indicaciones
GET    /api/psicologia/sesiones/proceso/{procesoId}
```

---

## 🔧 Desarrollo sin Docker

### Backend (requiere JDK 17 y SQL Server local)

```bash
cd backend
# Ajusta application.properties o usa variables de entorno:
export DB_HOST=localhost
export DB_PASSWORD=Admin123!
mvn spring-boot:run
```

### Frontend (requiere Node 20)

```bash
cd frontend
npm install
ng serve --open
# Apunta a http://localhost:8080/api por defecto (environment.ts)
```

---

## 🧩 Fases del proceso terapéutico

| Fase | Nombre                   | Descripción                                      |
| ---- | ------------------------ | ------------------------------------------------ |
| 1    | Evaluación               | Anamnesis, tests, diagnóstico diferencial        |
| 2    | Explicación de hipótesis | Devolver resultados y formular objetivos         |
| 3    | Tratamiento              | Técnicas terapéuticas según enfoque              |
| 4    | Seguimiento              | Mantenimiento de logros y prevención de recaídas |

> Basado en: https://www.unir.net/salud/revista/proceso-terapeutico/

---

## 📝 Notas técnicas

- **JWT** expira en 24 horas. El interceptor Angular lo adjunta automáticamente.
- **CORS** configurado para `localhost:4200` (dev) y el contenedor `frontend` (prod Docker).
- El **número de historia clínica** se genera automáticamente: `HC-XXXX`.
- Los **comprobantes** siguen la serie `B-XXXXX`.
- La **deuda** se crea automáticamente al registrar una cita; el cajero solo la busca y cobra.
