# Centavot

> *Tu dinero, bajo control. Siempre.*

Centavot es una aplicación móvil (Android / iOS) con inteligencia artificial para
independientes, comerciantes y emprendedores peruanos bajo los regímenes
tributarios **RUS / RER**. Ayuda a ordenar el dinero del negocio y a llegar a
la declaración ante SUNAT sin sorpresas — **sin pedir nunca credenciales bancarias**.

Proyecto del curso *Proyecto Startup* — Universidad ESAN.

## Qué hace

- **Registro de gastos sin fricción** — por foto de boleta (OCR), texto libre o voz.
- **Separación automática** entre gasto personal y gasto de negocio, con corrección manual.
- **Alertas de tope de régimen** — aviso al cruzar el 80 %, 90 % y 100 % del tope RUS/RER.
- **Reporte tributario pre-armado** para SUNAT, exportable (PDF/Excel) para compartir con el contador.
- **Offline-first** — funciona con mala señal y sincroniza al recuperar conexión.

## Para quién

| Perfil | Dolor principal |
|---|---|
| **Carlos** — freelancer con RUC en RUS/RER | Miedo a pasarse del tope de su régimen y recibir una multa |
| **Mari** — comerciante de mercado que cobra por Yape/Plin | Mezcla el dinero de la casa con el del negocio |
| **Rosa** — emprendedora de provincia, cliente de caja municipal | Ninguna app está pensada para ella; prefiere hablar a escribir |

## Estado del proyecto

🚧 **MVP local (Android).** La app funciona sin backend: todo se guarda en el
teléfono con Room. Incluye:

- Elegir régimen (RUS Cat. 1, RUS Cat. 2 o RER) con topes referenciales.
- Inicio con el % del tope usado, alertas 80/90/100 %, totales del mes y últimos gastos.
- Registrar, editar y eliminar gastos (monto, negocio/personal, descripción, fecha).
- Movimientos agrupados por día, con filtro por categoría.
- Reporte mensual de gastos de negocio.

Aún no incluye: foto de boleta (OCR), voz, exportar el reporte ni backend.

Hoja de ruta técnica:

| Fase | Entregable |
|---|---|
| 0 | Concierge MVP — validación manual con usuarios reales (en curso) |
| 1 | Núcleo KMP (`domain` + `data`), autenticación por teléfono + OTP, backend mínimo |
| 2 | Registro por foto (OCR) y texto — Android primero |
| 3 | Separación automática + reporte SUNAT |
| 4 | Alertas de tope + notificaciones push |
| 5 | Registro por voz, paridad iOS y piloto con una caja municipal |

## Stack

| Capa | Tecnología |
|---|---|
| UI | Compose Multiplatform + Material 3 |
| Navegación | Navigation Compose (rutas tipadas) |
| Dominio y datos | Kotlin Multiplatform (Kotlin puro) |
| Persistencia local | Room (KMP) + SQLite |
| Inyección de dependencias | Koin |
| Fechas | kotlinx-datetime |
| Red | Ktor Client *(planificado)* |
| Notificaciones | Firebase Cloud Messaging *(planificado)* |
| Backend | Ktor Server + PostgreSQL, con OCR e IA (GLM Flash / DeepSeek Flash) *(planificado)* |

## Estructura

En esta etapa el desarrollo se enfoca **solo en Android**. El código vive en
`shared/` para poder reutilizarlo en iOS más adelante.

```
androidApp/   Punto de entrada Android
iosApp/       Punto de entrada iOS (Xcode) — pausado en esta etapa
shared/       Código compartido entre plataformas
  src/commonMain/   Lógica y UI comunes (Clean Architecture, ver abajo)
  src/androidMain/  Código específico de Android
  src/iosMain/      Código específico de iOS
```

### Clean Architecture

`shared/src/commonMain/kotlin/com/app/centavot/`:

```
core/           Utilidades transversales
  error/          Tipos de error de la app (pendiente)
  util/           Helpers genéricos (Reloj)
domain/         Reglas de negocio — Kotlin puro, sin frameworks
  model/          Modelos: Gasto, Monto, RegimenTributario, ProximidadTope, ReporteSunat
  repository/     Interfaces de repositorio (se implementan en data)
  usecase/        Un caso de uso por operación (GuardarGasto, ObservarProximidadTope...)
data/           Implementación de acceso a datos
  local/          Base de datos Room: entidades, DAOs, topes referenciales
  remote/         Cliente de API (Ktor), DTOs (pendiente, sin backend aún)
  mapper/         Conversión DTO/Entity ↔ modelo de dominio
  repository/     Implementaciones de las interfaces de domain
presentation/   UI con Compose
  screens/        Pantallas y sus ViewModels: regimen, inicio, gasto,
                  movimientos, reporte
  components/     Componentes reutilizables
  navigation/     Rutas y grafo de navegación
  theme/          Colores, tipografía, tema
di/             Módulos de inyección de dependencias (Koin)
```

**Reglas de dependencia**

```
presentation → domain, core
data         → domain, core
domain       → core
core         → (nada)
```

- `domain` nunca importa nada de `data`, `presentation` ni de Android.
- La UI nunca recibe DTOs ni entidades de base de datos, solo modelos de `domain`.
- La lógica de negocio va en casos de uso, no en los ViewModels.
- Los topes de RUS/RER vendrán del backend. Mientras tanto están en un solo
  lugar (`data/local/TopesReferenciales.kt`), nunca en la lógica ni en la UI.

La base de datos de Android se crea en `shared/src/androidMain` (`di/ModuloAndroid.kt`).

## Cómo ejecutar

**Android**

```bash
./gradlew :androidApp:assembleDebug
```

**iOS** — abrir el directorio [`iosApp`](./iosApp) en Xcode y ejecutar desde ahí.

También puedes usar las configuraciones de ejecución de Android Studio / IntelliJ.

## Tests

```bash
./gradlew :shared:testAndroidHostTest     # tests en host Android (dominio y formato)
./gradlew :shared:iosSimulatorArm64Test   # tests en simulador iOS
```

## Equipo

- Sandro Fabrizio Enrique Avila Agurto
- James Frank Mendoza Rios
- Meyly Cielo Mendoza Cobeñas
