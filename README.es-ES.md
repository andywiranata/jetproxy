

# JetProxy
![Overview!](https://jetproxy.andywiranata.me/assets/images/jetproxy-intro-3a53dc6772cf521d3d37312d672cf6f7.png "JetProxy Overview")

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/andywiranata/jetproxy)

**JetProxy** es un **proxy inverso** rápido, ligero y flexible construido con **Jetty**. Diseñado para desarrolladores y equipos que necesitan control granular sobre el comportamiento del proxy, JetProxy es una alternativa amigable para desarrolladores en comparación con herramientas como **Traefik** y **Nginx**.

[Documentación](https://jetproxy.andywiranata.me/docs/intro)

### Características

- 🌐 Proxy inverso HTTP con enrutamiento dinámico
- 🔐 Soporte de autenticación basado en middleware (BasicAuth, JWT, ForwardAuth)
- 🧠 Motor de middleware flexible para el manejo y transformación de solicitudes:
  - Reglas de solicitud, manipulación de encabezados, CORS y limitación de velocidad
  - Caché incorporado, circuit breakers y soporte de espejado (mirroring)
  - Conversión experimental de REST a gRPC y manejo de idempotencia
- ⚡ Núcleo de servidor Jetty de alto rendimiento
- 📊 Estadísticas por ruta (consultas, códigos de estado, latencia, desglose por host)
- 💡 Configuración declarativa basada en YAML para rutas y políticas
- ☁️ Despliegue fácil como un JAR independiente: sin dependencias externas
- 🛠️ Desarrollado con Java 21 moderno y Gradle

### Simplicidad y Eficiencia
JetProxy elimina la necesidad de actualizaciones manuales constantes al ofrecer una configuración YAML sencilla y ajustes en tiempo real. Su diseño intuitivo reduce la complejidad operativa, permitiendo a los equipos centrarse en el desarrollo de servicios en lugar de mantener configuraciones de proxy complejas.

## Ejecución para Desarrollo

Para el desarrollo activo, se proporciona un script unificado `run.sh` para administrar tanto los servicios de backend como los de frontend.

### Modo de Desarrollo

Inicia el frontend con el servidor de desarrollo de recarga en vivo de Vite y arranca el backend. Utilícelo para la mayoría de las tareas de desarrollo.

En este modo, el frontend es accesible en `http://localhost:5173` (el puerto predeterminado de Vite) y se conecta al backend que se ejecuta en el puerto definido en su configuración (por ejemplo, `http://localhost:8080`).

```bash
# Run dev mode with the default config (demo/config.yaml)
./run.sh --dev


### Modo de Producción

Compila el frontend en un paquete estático optimizado y lo sirve directamente desde el servidor backend. Esto es útil para probar la aplicación final como una sola unidad.

```bash
# Run production mode with the default config (demo/config.yaml)
./run.sh

# Run production mode with a custom config file
./run.sh --config=path/to/your.yaml
```

### Ejecución de Servicios Individuales

También puede ejecutar los servicios del dashboard o del proxy de forma independiente.

```bash
# Run only the dashboard development server
./run.sh --dashboard-only

# Run only the proxy server (in production mode)
./run.sh --proxy-only

# Run only the proxy server (in development mode)
./run.sh --proxy-only --dev
```

# Cómo Ejecutarlo
## Paso 1: Descargar el Último JAR
Descargue el último [jar](https://github.com/andywiranata/jetproxy/releases) 
## Paso 2: Crear el Archivo de Configuración
Cree el archivo **config.yaml**
```
appName: ${JET_APP_NAME:API-PROXY}
port: ${JET_PORT:8080}
defaultTimeout: ${JET_DEFAULT_TIMEOUT:10000}
dashboard: ${JET_DASHBOARD:true}
rootPath: ${JET_DASHBOARD:/}
accessLog: ${JET_DEBUG_MODE:true}

corsFilter:
  accessControlAllowMethods:
    - "*"
  accessControlAllowHeaders:
    - "*"
  accessControlAllowOriginList:
    - "*"
  maxAge: 3600

storage:
  inMemory:
    enabled: true
    maxMemory: 50 # MB
    size: 10000

proxies:
  - path: /upload
    service: fileUploadService
    middleware:
      rule: "HeaderPrefix('Content-Type', 'multipart/form-data')"
  - path: /v2/grpc
    service: userGrpcApi
    ttl: 10000
  - path: /user
    service: userApi
    matches:
      - rule: "Header('Content-Type', 'application/json')"
        service: userApi
      - rule: "Header('x-header-hello', 'x-header-hello')"
        service: userApi
    middleware:
      basicAuth: 'basicAuth:administrator'
      mirroring:
        enabled: true
        mirrorService: userV2Api
        mirrorPercentage: 100
      forwardAuth:
        enabled: false
        path: /verify
        service: authApi
        requestHeaders: "Forward(X-Custom-*);Forward(Authorization);"
        responseHeaders: "Remove(X-Powered-By)"
    ttl: 1000
services:
  - name: userApi
    url: http://localhost:30001
    methods: ['GET', 'POST']
  - name: userV2Api
    url: http://localhost:30001/v2
    methods: [ 'GET', 'POST']
  - name: authApi
    url: http://localhost:30001
    methods: ['POST']
  - name: fileUploadService
    url: http://localhost:30001
    methods: ['POST']

grpcServices:
  - name: userGrpcApi
    host: localhost
    port: 50051

users:
  - username: admin
    password: admin
    role: administrator

```
## Paso 3: Ejecutar
```
export APP_CONFIG_PATH=/path/to/config.yaml
java -jar jetproxy-latest.jar
```
