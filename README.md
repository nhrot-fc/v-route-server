# PLG System - Gas Liquefied Petroleum Logistics System

## Overview

**PLG** is a company dedicated to the commercialization and distribution of Liquefied Petroleum Gas (LPG) in XYZ city. Their clients include industrial plants, commercial enterprises, condominiums, etc.

This system provides a comprehensive solution for managing fleet operations, orders, depots, maintenance, incidents, and real-time monitoring for the gas distribution company.

## 🚀 Features

- **Fleet Management**: Track vehicles, their positions, capacity, and availability
- **Order Management**: Handle customer orders with depot assignment and delivery tracking
- **Depot Management**: Manage storage facilities with capacity and stock monitoring
- **Maintenance Scheduling**: Track vehicle maintenance (preventive and corrective)
- **Incident Reporting**: Log and monitor operational incidents
- **Real-time Dashboard**: Monitor fleet status, orders, and system health
- **RESTful API**: Complete CRUD operations for all entities
- **Database Integration**: Full JPA/Hibernate integration with PostgreSQL

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.0
- **Database**: PostgreSQL (Production), H2 (Testing)
- **ORM**: JPA/Hibernate
- **Web Framework**: Spring Web MVC
- **Testing**: JUnit 5, Spring Boot Test
- **Build Tool**: Gradle
- **Java Version**: 21

### Key Components

#### Model Layer
- **Vehicle**: Fleet vehicles with position tracking and capacity management
- **Order**: Customer orders with delivery details and status tracking
- **Depot**: Storage facilities with stock management
- **Maintenance**: Vehicle maintenance scheduling and tracking
- **Incident**: Operational incident logging
- **Blockage**: Road/route blockage management
- **Shift**: Work shift management

#### Repository Layer
- JPA repositories with custom queries for each entity
- Optimized queries for fleet management and operational reporting

#### Controller Layer
- RESTful APIs for all entities
- Dashboard controller for monitoring and overview
- Comprehensive error handling and validation

#### Service Layer
- Data initialization service for fleet setup
- Business logic for operational management

## 📊 Database Schema

### Core Entities

**Vehicles Table**
```sql
CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(255) NOT NULL,
    capacity DOUBLE PRECISION NOT NULL,
    current_load DOUBLE PRECISION NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    available BOOLEAN NOT NULL
);
```

**Orders Table**
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    client_name VARCHAR(255) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    order_time TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL,
    depot_id BIGINT,
    vehicle_id BIGINT
);
```

**Depots Table**
```sql
CREATE TABLE depots (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    capacity DOUBLE PRECISION NOT NULL,
    current_stock DOUBLE PRECISION NOT NULL
);
```

## 🛠️ Setup Instructions

### Prerequisites
- Java 21 or higher
- PostgreSQL 12+ (for production)
- Gradle 7.0+ (or use included wrapper)

### Database Configuration

#### Production (PostgreSQL)
1. Create a PostgreSQL database:
   ```sql
   CREATE DATABASE plg_db;
   ```

2. Set environment variables:
   ```bash
   export SPRING_DATASOURCE_USERNAME=your_username
   export SPRING_DATASOURCE_PASSWORD=your_password
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/plg_db
   ```

3. The application will automatically create tables using Hibernate DDL.

#### Testing (H2 In-Memory)
Tests use an in-memory H2 database configured automatically.

### Building and Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd PLGSystem
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run tests**
   ```bash
   ./gradlew test
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Vehicles API
- `GET /api/vehicles` - Get all vehicles
- `POST /api/vehicles` - Create a new vehicle
- `GET /api/vehicles/{id}` - Get vehicle by ID
- `PUT /api/vehicles/{id}` - Update vehicle
- `DELETE /api/vehicles/{id}` - Delete vehicle
- `GET /api/vehicles/available` - Get available vehicles
- `GET /api/vehicles/capacity/{minCapacity}` - Get vehicles by minimum capacity

### Orders API
- `GET /api/orders` - Get all orders
- `POST /api/orders` - Create a new order
- `GET /api/orders/{id}` - Get order by ID
- `PUT /api/orders/{id}` - Update order
- `DELETE /api/orders/{id}` - Delete order
- `GET /api/orders/status/{status}` - Get orders by status
- `GET /api/orders/depot/{depotId}` - Get orders by depot

### Depots API
- `GET /api/depots` - Get all depots
- `POST /api/depots` - Create a new depot
- `GET /api/depots/{id}` - Get depot by ID
- `PUT /api/depots/{id}` - Update depot
- `DELETE /api/depots/{id}` - Delete depot

### Maintenance API
- `GET /api/maintenance` - Get all maintenance records
- `POST /api/maintenance` - Create maintenance record
- `GET /api/maintenance/{id}` - Get maintenance by ID
- `PUT /api/maintenance/{id}` - Update maintenance
- `DELETE /api/maintenance/{id}` - Delete maintenance
- `GET /api/maintenance/vehicle/{vehicleId}` - Get maintenance by vehicle
- `GET /api/maintenance/pending` - Get pending maintenance

### Dashboard API
- `GET /api/dashboard/overview` - System overview statistics
- `GET /api/dashboard/fleet` - Fleet status summary
- `GET /api/dashboard/orders` - Orders summary

## 🧪 Testing

The project includes comprehensive test coverage:

### Repository Integration Tests
- JPA entity persistence and retrieval
- Custom query validation
- Database constraint testing

### Controller Integration Tests
- REST API endpoint testing
- JSON serialization/deserialization
- Complete request/response cycle validation

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests="RepositoryIntegrationTest"

# Run with coverage
./gradlew test jacocoTestReport
```

## 🔄 Data Initialization

The system includes automatic data initialization for development:

- Fleet vehicles are created based on predefined specifications
- Multiple depots are established with initial stock levels
- Sample data follows realistic operational parameters

To disable data initialization (e.g., for production), set the profile to exclude initialization:
```properties
spring.profiles.active=production
```

## 📈 Monitoring and Operations

### Health Checks
The application includes Spring Boot Actuator for monitoring:
- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application information

### Logging
Structured logging is configured for operational visibility:
- Database operations (SQL queries in development)
- API request/response logging
- Error tracking and debugging information

## 🔧 Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Database Configuration
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/plg_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Server Configuration
server.port=8080
```

### Environment-Specific Configuration
- `application.properties` - Default configuration
- `application-test.properties` - Test environment (H2 database)
- `application-prod.properties` - Production environment (can be created)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Original Problem Description (Spanish)

El componente planificador deberá ser parametrizable para estos escenarios y presentar gráficamente información relevante del desempeño.

## 2. Entorno Operativo

### 2.1. Mapa de la Ciudad (XYZ)
* **Estructura:** Retícula rectangular.
* **Dimensiones:** 70 Km (eje X) x 50 Km (eje Y).
* **Calles:** Todas de doble sentido. No existen calles diagonales ni curvas.
* **Nodos:** Las esquinas se conceptualizan como nodos. Las ubicaciones de los clientes se refieren a coordenadas (x,y) de un nodo.
* **Distancia entre Nodos:** 1 Km.
* **Origen de Coordenadas:** (0,0), ubicado en la parte inferior izquierda de la retícula.

### 2.2. Puntos de Suministro (Almacenes/Depósitos)
* **Planta Principal (Almacén Central):**
    * Ubicación: (X=12, Y=8).
    * Abastecimiento: Se considera que está abastecida todo el tiempo.
* **Tanques Cisternas Intermedios (2 unidades):**
    * **Tanque Intermedio Norte:**
        * Ubicación: (X=42, Y=42).
        * Capacidad "efectiva": 160 m³.
        * Abastecimiento: Se abastece una vez al día a las 00:00 horas hasta su capacidad efectiva.
    * **Tanque Intermedio Este:**
        * Ubicación: (X=63, Y=3).
        * Capacidad "efectiva": 160 m³.
        * Abastecimiento: Se abastece una vez al día a las 00:00 horas hasta su capacidad efectiva.

### 2.3. Horario de Trabajo y Supuestos Operativos
* **Operación:** 24 horas al día, 7 días a la semana (24x7) para pedidos y entregas.
* **Tráfico y Semáforos:** Se considera que la velocidad promedio es constante e inalterable. Los semáforos siempre están en verde y no hay tráfico.
* **Velocidad Promedio de Camiones:** 50 Km/h.

## 3. Flota de Camiones Cisterna

### 3.1. Tipos y Capacidades
La flota total tiene una capacidad de 200 m³.

| Tipo | Peso Bruto (Tara) | Carga GLP (m³) | Peso Carga GLP (Ton) | Peso Combinado (Ton) | Unidades | Capacidad Total Tipo (m³) |
|------|-------------------|----------------|----------------------|----------------------|----------|---------------------------|
| TA   | 2.5 Ton           | 25 m³          | 12.5 Ton             | 15.0 Ton             | 02       | 50 m³                     |
| TB   | 2.0 Ton           | 15 m³          | 7.5 Ton              | 9.5 Ton              | 04       | 60 m³                     |
| TC   | 1.5 Ton           | 10 m³          | 5.0 Ton              | 6.5 Ton              | 04       | 40 m³                     |
| TD   | 1.0 Ton           | 5 m³           | 2.5 Ton              | 3.5 Ton              | 10       | 50 m³                     |

* **Códigos de Camiones:** `TTNN` (TT = Tipo, NN = Correlativo. Ej: `TA01`, `TD10`).
* **Capacidad Tanque de Combustible (Petróleo):** Todos los camiones tienen tanques de 25 Galones.

### 3.2. Consumo de Combustible (Petróleo)
* **Fórmula:** `Consumo [Galones] = (Distancia [Km] * Peso Combinado [Ton]) / 180`
    * Peso Combinado = Peso Bruto (Tara) + Peso Carga GLP.
    * El peso del petróleo (combustible del camión) se considera despreciable.
* **Ejemplo de Autonomía Máxima (TA con carga llena):**
    * Peso Combinado TA (lleno) = 15 Ton.
    * Distancia Máx. = (25 Galones * 180) / 15 Ton = 300 Km.

### 3.3. Mantenimiento
* **Mantenimiento Preventivo:**
    * Duración: 24 horas (indisponibilidad de 00:00 a 23:59 del día programado).
    * Frecuencia: Bimensual (se repite cada 2 meses).
    * Archivo: `mantpreventivo` (ver sección Formatos de Archivos).
    * Regla: Si un camión está en ruta al inicio de su mantenimiento, debe regresar a la planta sin completar la ruta.
    * Carga de datos: Directamente en la Base de Datos antes de la primera presentación.
* **Mantenimiento Correctivo (Averías):** Ver sección "Averías de Unidades".

### 3.4. Tiempos Operativos Adicionales
* **Tiempo de Carga/Descarga en Planta:** Despreciable (T=0).
* **Tiempo de Descarga en Entrega a Clientes:** 15 minutos.
    * Este tiempo *no* se considera dentro del plazo de entrega pactado (el plazo es hasta la llegada al cliente).
    * Sí afecta la hora de salida del camión de las instalaciones del cliente.
* **Tiempo de "Mantenimiento de Rutina para Salir" en Planta:** 15 minutos (tiempo que debe pasar un camión en planta antes de volver a salir).
* **Tiempo de Trasvase en Ruta (por avería):** 15 minutos.

## 4. Gestión de Pedidos

### 4.1. Políticas de Entrega
* **Plazo Mínimo de Entrega:** 4 horas desde la realización del pedido.
* **Política de Cumplimiento:** Cero incumplimientos si el cliente realiza el pedido con un mínimo de 4 horas de antelación.
* **Supuesto del Curso:** No habrá demoras en los plazos de entrega si se cumple la condición anterior.

### 4.2. Archivo de Pedidos
* Nombre: `ventas2025mm` (mm = mes, ej: 01 para enero).
* Contenido: Pedidos históricos y proyectados.
* Formato Registro: `##d##h##m:posX,posY,c-idCliente,m3,hLímite`
    * `##d##h##m`: Día, hora, minuto de llegada del pedido (ej: `11d13h31m`).
    * `posX,posY`: Ubicación de entrega (nodo).
    * `c-idCliente`: Identificador del cliente.
    * `m3`: Cantidad de GLP en metros cúbicos (entero).
    * `hLímite`: Número de horas límite para la entrega desde la llegada del pedido.

## 5. Interrupciones y Eventos

### 5.1. Bloqueos de Calles
* **Definición:** Tramos de pista identificados por un par de puntos extremos (nodos) donde no es posible el acceso vehicular. Un nodo bloqueado no se puede atravesar ni girar sobre él (implica media vuelta).
* **Tipo Considerado:** Solo bloqueos planificados con antelación (no fortuitos como accidentes).
* **Naturaleza del Bloqueo (para este caso):** Polígonos abiertos (siempre es posible llegar a todos los puntos de la poligonal, aunque por otra ruta).
* **Archivo de Bloqueos:** `aaaamm.bloqueadas` (ver sección Formatos de Archivos). Años 2025, 2026. Se entregará al menos un año de datos.
* **Impacto de Averías en Vías:** Si una unidad se avería, la vía sigue habilitada para otros vehículos.

### 5.2. Averías de Unidades (Incidentes)
* **Definición:** Interrupción al servicio comunicada por el conductor.
* **Archivo de Averías:** `averias.txt` (ver sección Formatos de Archivos). Se repite todos los días para el ejercicio académico.
* **Momento de Ocurrencia:** Si una unidad está en operación en el turno indicado en el archivo, la avería ocurre de forma aleatoria entre el 5% y 35% del recorrido total de su ruta planificada (ida y vuelta). La posición debe ajustarse a un nodo. Este rango busca asegurar que la avería ocurra con carga.
* **Frecuencia:** Una unidad solo se avería una vez por turno.
* **Tipos de Incidentes:**
    * **Tipo 1 (Ej: llanta baja reparable in situ):**
        * Inmovilización en el lugar: 2 horas.
        * Post-inmovilización: Unidad retoma ruta designada.
    * **Tipo 2 (Ej: motor obstruido):**
        * Inmovilización en el lugar: 2 horas.
        * Inoperatividad en taller: 1 turno completo.
            * Si ocurre en T1 (día A) -> Disponible en T3 (día A).
            * Si ocurre en T2 (día A) -> Disponible en T1 (día A+1).
            * Si ocurre en T3 (día A) -> Disponible en T2 (día A+1).
        * Post-inmovilización (en sitio): Unidad llevada al almacén/taller.
    * **Tipo 3 (Ej: choque):**
        * Inmovilización en el lugar: 4 horas.
        * Inoperatividad en taller: 1 día completo.
            * Si ocurre en T1, T2 o T3 (día A) -> Disponible en T1 (día A+3).
        * Post-inmovilización (en sitio): Unidad llevada al almacén/taller.
* **Reglas Comunes a Todos los Tipos de Incidentes:**
    * Trasvase de producto: Mientras la unidad esté inmovilizada, otra unidad puede recoger parte o toda la carga (tiempo de trasvase: 15 min).
* **Manejo en Escenarios:**
    * Operaciones día a día / Simulación 7 días: Se pueden registrar por pantalla o archivo.
    * Simulación colapso: No aplican averías.
* **Turnos de Trabajo (Referencial para disponibilidad por averías y archivo `averias.txt`):**
    * **T1:** 00:00 - 07:59
    * **T2:** 08:00 - 15:59
    * **T3:** 16:00 - 23:59

## 6. Requisitos del Sistema a Desarrollar

### 6.1. Componentes Principales
1.  **Registro de Pedidos.**
2.  **Componente Planificador:** Planifica y replanifica rutas.
3.  **Componente Visualizador:** Monitoreo gráfico en mapa en tiempo real.

### 6.2. Escenarios de Evaluación y Simulación
* **Operaciones Día a Día:**
    * Ingreso de pedidos por teclado (simultáneo por miembros del equipo) o archivo de texto (formato data histórica).
    * Pedidos de archivo se consideran futuros respecto al instante de ejecución.
* **Simulaciones:**
    * Ingreso de fecha y hora de inicio de simulación.
    * **Simulación de 7 días:** Cubre 168 horas siguientes, consumiendo data proyectada.
* **Consulta de Estatus de Camiones:** Debe ser posible para cada escenario (mantenimiento, averiado, en desplazamiento, etc.).

### 6.3. Replanificación
* **Definición:** Cambio de ruta de camiones para atender nuevos pedidos más críticos.
* El sistema debe ser capaz de ajustar rutas en función de nuevos pedidos con plazos de entrega más ajustados que los originalmente asignados.

### 6.4. Parametrización del Algoritmo
* El equipo de desarrollo definirá parámetros como:
    * `Sa` (salto del algoritmo) o equivalente.
    * `Sc` (salto del consumo) o equivalente.
    * `Ta` (tiempo de ejecución del algoritmo) o equivalente.
* El tiempo de simulación de 7 días debe estar dentro de lo establecido (20-50 minutos).
* El plazo mínimo de entrega de 4 horas debe ser una variable configurable.

## 7. Formatos de Archivos de Entrada

### 7.1. Archivo de Bloqueos (`aaaamm.bloqueadas`)
* **Nombre:** `aaaamm.bloqueadas` (aaaa = 2025 o 2026, mm = 01-12).
* **Registro:** `##d##h##m-##d##h##m:x1,y1,x2,y2,...,xn,yn`
    * **Ejemplo:** `01d06h00m-01d15h00m:31,21,34,21`
    * **1er `##d##h##m`:** Inicio del bloqueo (Día, Hora, Minuto).
    * **2do `##d##h##m`:** Fin del bloqueo.
    * **`xi,yi`:** Coordenadas de nodos que definen tramos bloqueados. P1(x1,y1) es inicio del primer tramo, P2(x2,y2) es fin del primer tramo e inicio del segundo, etc.

### 7.2. Archivo de Mantenimiento (`mantpreventivo`)
* **Nombre:** `mantpreventivo`
* **Registro:** `aaaammdd:TTNN`
    * `aaaammdd`: Año, mes, día del mantenimiento.
    * `TT`: Tipo de camión (TA, TB, etc.).
    * `NN`: Número correlativo del camión.
    * **Ejemplo:** `20250401:TA01`

### 7.3. Archivo de Pedidos (`ventas2025mm`)
* **Nombre:** `ventas2025mm` (mm = 01-12).
* **Registro:** `##d##h##m:posX,posY,c-idCliente,m3,hLímite`
    * `##d##h##m`: Momento de llegada del pedido.
    * `posX,posY`: Ubicación del cliente.
    * `c-idCliente`: ID del cliente.
    * `m3`: Metros cúbicos de GLP.
    * `hLímite`: Horas límite para entrega.
    * **Ejemplo:** `11d13h31m:45,43,c-167,9m3,36h`

### 7.4. Archivo de Averías (`averias.txt`)
* **Nombre:** `averias.txt`
* **Registro:** `tt_######_ti`
    * `tt`: Turno de ocurrencia (T1, T2, T3).
    * `######`: Código de la unidad vehicular (TTNN).
    * `ti`: Tipo de incidente (TI1, TI2, TI3).
    * **Ejemplo:** `T1_TA01_TI2`

## 8. Consideraciones Específicas para Pruebas

### 8.1. Juego de Datos para Forzar Salida Completa de Flota
* **Alternativa 1 (Explícita):** 20 pedidos individuales que cubren la capacidad total de la flota, todos al punto más lejano (69,49) con plazo holgado (12h), registrados al mismo tiempo (ej: `01d11h25m`).
    * Ej: `01d11h25m:69,49,c-999,25m3,12h` (para un camión TA)
    * ... (repetir para cubrir 2xTA, 4xTB, 4xTC, 10xTD)
* **Alternativa 2 (Implícita):** Un único pedido grande que requiera la capacidad total de la flota (200m³).
    * Ej: `01d11h25m:69,49,c-999,200m3,12h`

### 8.2. Juego de Datos para Forzar Replanificación
Estos pedidos se registran *después* del juego de datos anterior (ej. 1 hora después, `01d12h25m`), con plazos de entrega más cortos (4h) y ubicaciones diferentes, forzando al sistema a reasignar camiones.
* `01d12h25m:2,49,c-999,25m3,4h`
* `01d12h25m:2,48,c-997,15m3,4h`
* `01d12h25m:2,47,c-996,15m3,4h`
* `01d12h25m:1,49,c-993,10m3,4h`
* `01d12h25m:1,48,c-991,10m3,4h`
* `01d12h25m:1,47,c-989,05m3,4h`
* `01d12h25m:3,49,c-988,05m3,4h`

**Importante:** La fecha y hora de estos juegos de datos deben ajustarse para pruebas en el escenario "día a día" o pueden usarse en la simulación semanal.