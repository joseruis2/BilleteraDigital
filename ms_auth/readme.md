# ms-auth

Microservicio de autenticación de **MiBilletera**, encargado de gestionar el inicio de sesión de los usuarios y la emisión y validación de tokens JWT utilizados por los demás microservicios del sistema.

## Rol en la arquitectura

`ms-auth` no expone un endpoint propio de registro. Los usuarios se crean de forma **reactiva mediante eventos de Kafka**, cuando un nuevo cliente se registra en `ms-cliente`.

### Flujo de creación de usuarios

```text
ms-cliente (POST /api/clientes/registro)
        │
        ▼
   Kafka: tópico "cliente-registrado"
        │
        ▼
ms-auth (ClienteRegistradoListener)
        │
        ▼
Crea usuario_auth con contraseña protegida mediante BCrypt
```

Una vez creado el usuario, `ms-auth` es responsable de:

1. **Autenticación:** valida las credenciales mediante `POST /api/auth/login` y devuelve un JWT firmado.
2. **Publicación de claves públicas:** expone `GET /.well-known/jwks.json` para que los demás microservicios (`ms-cliente`, `ms-cuentas` y `ms-pagos`) puedan verificar los JWT de forma independiente, sin necesidad de consultar a `ms-auth` en cada petición.

### Flujo de autenticación y validación JWT

```text
Cliente
   │
   │ POST /api/auth/login
   ▼
ms-auth
   │
   │ JWT firmado con RS256
   ▼
Cliente
   │
   │ Envía JWT en las peticiones
   ▼
ms-cliente / ms-cuentas / ms-pagos
   │
   │ Obtienen la clave pública desde JWKS
   ▼
Validan la firma del JWT localmente
```

## ¿Por qué JWT firmado con RSA y no HMAC?

Con una clave simétrica (HMAC), cualquier servicio que necesite validar un token también requiere conocer la misma clave secreta utilizada para firmarlo. Esto implica distribuir un secreto sensible entre varios microservicios.

Con RSA (criptografía asimétrica):

* **Clave privada:** permanece exclusivamente en `ms-auth` y se utiliza para firmar los JWT.
* **Clave pública:** se expone mediante `/.well-known/jwks.json` y permite a los demás microservicios verificar la autenticidad de los tokens.

De esta manera, los servicios pueden comprobar que un JWT fue firmado legítimamente sin tener la capacidad de generar nuevos tokens válidos.

## Endpoints

| Método | Ruta                     | Autenticación requerida | Descripción                                                     |
| ------ | ------------------------ | ----------------------- | --------------------------------------------------------------- |
| `POST` | `/api/auth/login`        | No                      | Valida las credenciales y devuelve un JWT.                      |
| `GET`  | `/.well-known/jwks.json` | No                      | Expone la clave pública en formato JWK para validar los tokens. |

### Ejemplo: Inicio de sesión

**Request**

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "joseruiz",
  "password": "password123"
}
```

**Response (200 OK)**

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "expiresIn": 3600,
  "username": "joseruiz",
  "rol": "CLIENTE"
}
```

### Errores posibles

| Código HTTP        | Descripción                                                    |
| ------------------ | -------------------------------------------------------------- |
| `401 Unauthorized` | Usuario inexistente, contraseña incorrecta o usuario inactivo. |

Por seguridad, se utiliza un mensaje de error genérico para estos casos, evitando revelar información sobre la existencia o el estado de una cuenta.

## Estructura del JWT

Cada token emitido contiene los siguientes claims:

```json
{
  "sub": "joseruiz",
  "clienteId": 1,
  "rol": "CLIENTE",
  "iat": 1790722993,
  "exp": 1790726593
}
```

### Descripción de los claims

| Claim       | Descripción                                    |
| ----------- | ---------------------------------------------- |
| `sub`       | Identificador del usuario autenticado.         |
| `clienteId` | Identificador del cliente asociado al usuario. |
| `rol`       | Rol asignado al usuario.                       |
| `iat`       | Fecha y hora de emisión del token.             |
| `exp`       | Fecha y hora de expiración del token.          |

El claim `clienteId` permite que los demás microservicios identifiquen al propietario de los recursos.

Por ejemplo, `ms-cuentas` utiliza este identificador en `GET /api/cuentas/mias` para recuperar únicamente las cuentas pertenecientes al cliente autenticado.

### Configuración del token

| Propiedad                | Valor                  |
| ------------------------ | ---------------------- |
| Algoritmo de firma       | RS256                  |
| Tipo de token            | JWT                    |
| Tiempo de expiración     | 3600 segundos (1 hora) |
| Formato de clave pública | JWK / JWKS             |

## Variables de entorno

El servicio requiere las siguientes variables de entorno para su ejecución:

| Variable           | Descripción                                                                                                                  |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| `DB_PASSWORD`      | Contraseña de acceso a la base de datos MySQL `ms_auth_db`.                                                                  |
| `INTERNAL_API_KEY` | Clave compartida con los demás microservicios. Debe coincidir con la configurada en `ms-cliente`, `ms-cuentas` y `ms-pagos`. |

No se utilizan valores por defecto inseguros para estas variables.

## Ejecución local

### Requisitos previos

Antes de iniciar `ms-auth`, asegúrate de tener disponibles los siguientes servicios:

* MySQL en `localhost:3306`.
* Apache Kafka en `localhost:9092`.
* Eureka Server en `localhost:8761`.
* Java 17 o la versión compatible configurada en el proyecto.
* Maven.

### Configuración de variables de entorno

En Linux o Git Bash:

```bash
export DB_PASSWORD=tu_password
export INTERNAL_API_KEY=tu_clave_compartida
```

En Windows PowerShell:

```powershell
$env:DB_PASSWORD="tu_password"
$env:INTERNAL_API_KEY="tu_clave_compartida"
```

### Ejecutar el microservicio

```bash
mvn spring-boot:run
```

### Configuración del servicio

| Propiedad                 | Valor        |
| ------------------------- | ------------ |
| Puerto                    | `8082`       |
| Nombre en Eureka          | `ms-auth`    |
| Base de datos             | `ms_auth_db` |
| Protocolo de comunicación | HTTP         |
| Mensajería                | Apache Kafka |

## Limitación conocida: persistencia de las claves RSA

Actualmente, `JwtKeyProvider` genera un nuevo par de claves RSA en memoria cada vez que se inicia el servicio.

Esto implica las siguientes limitaciones:

* Los JWT emitidos antes de un reinicio dejan de ser válidos, ya que la clave pública publicada cambia.
* Si se ejecutan varias instancias de `ms-auth` simultáneamente, cada una tendrá su propio par de claves, por lo que no podrán validar correctamente los tokens emitidos por las demás.

### Mejora pendiente para producción

Implementar una gestión persistente y centralizada de las claves criptográficas mediante alguna de las siguientes alternativas:

* Keystore PKCS12 o JKS.
* AWS Key Management Service (KMS).
* HashiCorp Vault.

Esto permitirá mantener la continuidad de la validación de tokens entre reinicios y facilitará el escalamiento horizontal del servicio.

## Stack tecnológico

| Tecnología        | Uso                                     |
| ----------------- | --------------------------------------- |
| Java              | Lenguaje de programación                |
| Spring Boot 3.5   | Framework principal                     |
| Spring WebFlux    | Desarrollo de API reactivas             |
| Spring Data R2DBC | Acceso reactivo a la base de datos      |
| MySQL             | Persistencia de datos                   |
| Flyway            | Gestión de migraciones de base de datos |
| JJWT              | Generación y gestión de tokens JWT      |
| RSA / RS256       | Firma digital de tokens                 |
| BCrypt            | Hash seguro de contraseñas              |
| Spring Kafka      | Consumo del evento `cliente-registrado` |
| Eureka Client     | Registro y descubrimiento de servicios  |

---

**Proyecto:** MiBilletera
**Microservicio:** `ms-auth`
**Responsabilidad principal:** Autenticación, emisión de JWT y publicación de claves públicas para la validación distribuida.
