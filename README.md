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

🚧 **Pre-producto.** El repositorio contiene hoy la plantilla base de Kotlin
Multiplatform. La hoja de ruta técnica:

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
| UI | Compose Multiplatform |
| Dominio y datos | Kotlin Multiplatform (Kotlin puro) |
| Red | Ktor Client *(planificado)* |
| Persistencia local | SQLDelight *(planificado)* |
| Inyección de dependencias | Koin *(planificado)* |
| Notificaciones | Firebase Cloud Messaging *(planificado)* |
| Backend | Ktor Server + PostgreSQL, con OCR e IA (GLM Flash / DeepSeek Flash) *(planificado)* |

## Estructura

```
androidApp/   Punto de entrada Android
iosApp/       Punto de entrada iOS (Xcode)
shared/       Código compartido entre plataformas
  src/commonMain/   Lógica y UI comunes
  src/androidMain/  Código específico de Android
  src/iosMain/      Código específico de iOS
```

## Cómo ejecutar

**Android**

```bash
./gradlew :androidApp:assembleDebug
```

**iOS** — abrir el directorio [`iosApp`](./iosApp) en Xcode y ejecutar desde ahí.

También puedes usar las configuraciones de ejecución de Android Studio / IntelliJ.

## Tests

```bash
./gradlew :shared:testAndroidHostTest     # tests en host Android
./gradlew :shared:iosSimulatorArm64Test   # tests en simulador iOS
```

## Equipo

- Sandro Fabrizio Enrique Avila Agurto
- James Frank Mendoza Rios
- Meyly Cielo Mendoza Cobeñas
