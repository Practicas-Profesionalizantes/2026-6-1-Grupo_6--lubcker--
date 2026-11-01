# Lubcker - Backend

API REST en Spring Boot 4 (Java 21) con MySQL.

## Cómo correrlo en local

1. Levantar MySQL con la base `Lubcker` (script en `../ProyectoLubcker-sql-3/lubcker_corregido.sql`).
2. Copiar `local.properties.example` como `local.properties` en esta carpeta y completarlo (este archivo no se sube a GitHub):

   ```properties
   spring.datasource.password=LA_CLAVE_DE_MYSQL
   app.jwt.secret=UNA_CLAVE_LARGA_DE_AL_MENOS_32_CARACTERES
   ```

3. Arrancar desde esta carpeta:

   ```bash
   ./mvnw spring-boot:run
   ```

Si no se define `app.jwt.secret`, se usa una clave aleatoria y las sesiones se pierden cada vez que se reinicia el backend.

## Variables de entorno (producción)

| Variable | Para qué | Valor por defecto |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Conexión a MySQL | `localhost`, `3306`, `Lubcker` |
| `DB_USER`, `DB_PASSWORD` | Credenciales de MySQL | `root`, (vacío) |
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres) | aleatoria en cada arranque |
| `JWT_HORAS` | Horas de validez de una sesión | `8` |
| `APP_CORS_ORIGINS` | Dominios del frontend permitidos (separados por coma) | `http://localhost:3000` |
| `DDL_AUTO` | Qué hace Hibernate con las tablas (`update`, `validate`, `none`) | `update` (en producción usar `validate`) |

## Documentación de la API (Swagger)

Con el backend corriendo:

- **Swagger UI** (para probar las rutas): http://localhost:8080/swagger-ui.html
- **OpenAPI en JSON**: http://localhost:8080/v3/api-docs
- **Archivo estático**: [`docs/openapi.json`](docs/openapi.json) (se puede importar en Postman, Insomnia o https://editor.swagger.io).

Para probar rutas protegidas: hacer `POST /api/auth/login`, copiar el `token` de la respuesta y pegarlo en el botón **Authorize** (sin escribir "Bearer").
Cada operación indica si es *Público*, de *Sesión* o *Solo Admin* (se calcula con las mismas reglas de `SecurityConfig`).

Se genera sola a partir de los controllers con springdoc; los textos están en `@Tag` y `@Operation`. Si cambian las rutas, regenerar `docs/openapi.json` bajando `/v3/api-docs` con el backend corriendo.
En producción apagarla con la variable de entorno `SWAGGER_HABILITADO=false`.

## Seguridad

- Contraseñas con BCrypt. Las que estaban en texto plano se convierten solas la primera vez que el usuario inicia sesión.
- Login con token JWT (`POST /api/auth/login`, `POST /api/auth/registro`). El frontend lo manda en `Authorization: Bearer <token>`.
- Reglas de acceso en `config/SecurityConfig.java`:
  - **Público**: catálogo (GET de productos, categorías, fichas, reseñas y métodos de pago), imágenes subidas, login y registro.
  - **Con sesión**: compra, subir imágenes, reseñas y los datos propios (perfil, direcciones, pedidos). Cada usuario solo accede a lo suyo.
  - **Solo admin**: todo lo demás. Un endpoint nuevo queda restringido a admin por defecto.
- El rol `ADMIN` se asigna directamente en la base:

  ```sql
  UPDATE usuario SET rol = 'ADMIN' WHERE email = 'correo@ejemplo.com';
  ```

## Productos que se venden por medida (kg, litro, metro)

Un producto tiene una **unidad de medida** (`unidad`, `kg`, `litro` o `metro`), un **precio por una unidad** de esa medida y una lista de **medidas a la venta**. Ejemplo: precio $10.000 por kg y medidas 3, 5 y 10 → el cliente elige 5 kg y el precio final es $50.000 (precio × medida).

- El admin lo carga en "Gestionar productos" (campos *Unidad de medida*, *Medidas a la venta* y *Precio por kg*). Sin medidas, el producto se vende de a 1 unidad como siempre.
- El **stock se cuenta en la unidad de medida** (100 = 100 kg): al comprar 2 bolsas de 5 kg se descuentan 10, y si el pedido se cancela vuelven al stock.
- El carrito tiene una línea por producto y medida. El backend valida que la medida sea una de las que vende el producto y calcula el precio: el navegador solo manda producto, medida y cantidad.
- Las medidas son números enteros entre 1 y 100000 (hasta 12 por producto).
- Cada pedido guarda la medida elegida (`detalle_orden.tamano`) y el precio del paquete, así que los pedidos viejos no cambian si después se modifica el precio.

## Materia prima y costos (solo admin)

Menú lateral de administración: **Insumos** y **Materia prima de productos**.

- **Insumos:** la materia prima que compra el negocio (nombre, unidad: kg, g, litro, ml, unidad o metro, proveedor y **costo por unidad, obligatorio**). Un insumo que está en una receta no se puede borrar ni cambiarle la unidad.
- **Materia prima de productos:** se elige un producto publicado y se cargan los insumos que lleva y la cantidad **por una unidad del producto** (1 kg, 1 litro, 1 unidad; admite decimales, ej: 0,25 kg). La pantalla calcula en vivo el **costo de fabricación** (suma de cantidad × costo de cada insumo), lo compara con el **precio de venta publicado** y muestra la **ganancia y el margen**. Además, el admin ingresa cuánto vendió (ej: 3 kg) y se calcula el ingreso, el costo y la **ganancia total** de lo vendido (ese número se guarda solo en el navegador).
- El costo es siempre el de hoy: si cambia el costo de un insumo, cambia el costo de fabricación de los productos que lo usan; si no, es fijo.
- En **Gestionar productos**, cada producto con materia prima muestra su costo de fabricación, precio de venta y ganancia.
- Todo vive bajo `/api/admin/**` (solo ADMIN). `/api/productos` es público y nunca devuelve costos.

## Pagos con Mercado Pago (Checkout Pro)

El único medio de pago es Mercado Pago. Al confirmar el pedido, el backend crea el pedido, descuenta el stock y devuelve un link de pago; el cliente paga en Mercado Pago y vuelve a `/pago/resultado`.

**Configuración:** copiar `secrets.properties.example` como `secrets.properties` y completar `app.mercadopago.access-token` (o usar la variable `MERCADOPAGO_ACCESS_TOKEN`). Para pruebas, usar el token de "Credenciales de prueba" de https://www.mercadopago.com.ar/developers/panel. Sin token la tienda funciona pero no se puede finalizar una compra.

| Variable | Para qué | Valor por defecto |
|---|---|---|
| `MERCADOPAGO_ACCESS_TOKEN` | Access token de la aplicación | (vacío: compras desactivadas) |
| `APP_FRONTEND_URL` | A dónde vuelve el cliente después de pagar | `http://localhost:3000` |
| `MERCADOPAGO_WEBHOOK_URL` | URL pública del backend para los avisos de pago, ej: `https://mi-backend.com/api/mercadopago/webhook` | (vacía) |
| `MERCADOPAGO_WEBHOOK_SECRET` | Secreto de firma de los avisos (opcional) | (vacío) |
| `MERCADOPAGO_HORAS_PARA_PAGAR` | Horas para pagar antes de cancelar el pedido | `24` |

**Cómo se confirma un pago:** el pedido solo pasa a "Confirmado" después de que el backend consulta el pago a Mercado Pago con el access token y verifica que el monto, la moneda y el pedido coincidan. Ni el navegador ni un aviso falso pueden aprobar un pago. Hay dos caminos: el aviso (webhook) de Mercado Pago, que necesita una URL pública, y la verificación cuando el cliente vuelve a la tienda, que funciona también en localhost.

**Pedidos sin pagar:** pasadas las horas configuradas, el pedido se cancela y el stock vuelve. Mientras tanto el cliente puede pagarlo desde "Mis compras" (botón "Pagar ahora").

**Producción:** usar las credenciales de producción de la cuenta de Mercado Pago del negocio y configurar `APP_FRONTEND_URL` (https) y `MERCADOPAGO_WEBHOOK_URL`.

## Asistente de chat (Lubcki)

Botón flotante en la tienda que responde preguntas frecuentes, asesora sobre el catálogo y cuenta el estado de los pedidos del usuario con sesión. Usa la API de Gemini de Google (modelo `gemini-3.5-flash` por defecto).

**Para activarlo hace falta una clave de Gemini** (https://aistudio.google.com/apikey):

1. Copiar `secrets.properties.example` como `secrets.properties` en esta carpeta y completar `app.chat.api-key`. Este archivo **no se sube a GitHub**: no pongas la clave en `local.properties` ni en `application.properties`, porque esos sí se suben y cualquiera con acceso al repositorio podría gastar tu saldo.
2. O definir la variable de entorno `GEMINI_API_KEY` (recomendado en producción).

Sin clave, el resto de la tienda funciona igual y el chat avisa que no está disponible.

| Variable | Para qué | Valor por defecto |
|---|---|---|
| `GEMINI_API_KEY` | Clave de la API | (vacío: chat desactivado) |
| `CHAT_MODELO` | Modelo a usar | `gemini-3.5-flash` |
| `CHAT_LIMITE_HORA` | Mensajes por persona por hora | `30` |
| `CHAT_LIMITE_DIA` | Mensajes por día entre todos (tope de gasto) | `2000` |

**Seguridad de los datos**
- La herramienta de pedidos no recibe ningún parámetro del modelo: el cliente sale siempre del token de sesión validado en el servidor, así que el asistente solo puede ver los pedidos del usuario que está chateando.
- Al asistente nunca se le envían contraseñas, correos ni direcciones de pedidos.
- Las conversaciones no se guardan en la base de datos de la tienda. Ojo: en el plan gratuito de Gemini, Google puede usar lo que se envía para mejorar sus productos; con una clave de un proyecto con facturación activa eso no ocurre.

**Editar lo que sabe el asistente:** el archivo `src/main/resources/chat/conocimiento.txt` tiene las respuestas sobre cómo usar la tienda. Se edita como texto y se reinicia el backend.
