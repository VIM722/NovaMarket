# NOVA Market

Aplicación local del laboratorio de Arquitectura de Software de la semana 11. Registra pedidos, calcula descuentos y muestra las reacciones de Cliente, Almacén y Administración al confirmar una venta.

## Ejecutar en local

Requisitos: JDK 17 o superior, Maven 3.6.3 o superior y un navegador. La primera compilación requiere acceso a Maven Central si faltan dependencias en la caché.

Desde esta carpeta, ejecuta:

```powershell
mvn spring-boot:run
```

Abre [NOVA Market](http://127.0.0.1:8080/). Si el puerto está ocupado, puedes ejecutar `mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'` y abrir el puerto 8081.

También puedes generar y ejecutar el paquete:

```powershell
mvn package
java -jar target/nova-market-1.0.0.jar
```

## Demostración

1. En **Nuevo pedido**, elige el caso rápido **Nuevo**, **Frecuente** o **No identificado**. Cada botón rellena un ejemplo de S/ 200.00.
2. Pulsa **Registrar pedido**. Repite para las tres categorías. El historial mostrará descuentos de S/ 10.00, S/ 20.00 y S/ 0.00, con totales de S/ 190.00, S/ 180.00 y S/ 200.00.
3. Pulsa **Ver** en una fila y luego **Confirmar pedido**. Comprueba que cambia el estado y que el detalle contiene el UUID del evento y las tres reacciones. Cada confirmación también queda registrada en la consola.
4. Para repetir una confirmación vía HTTP, envía de nuevo la misma solicitud. La respuesta conserva el UUID y las reacciones previas.

| Método | Ruta | Operación |
| --- | --- | --- |
| GET | `/api/politicas` | Consultar categorías y reglas disponibles |
| GET | `/api/pedidos` | Consultar el historial y la evidencia de cada pedido |
| POST | `/api/pedidos` | Registrar con `customer`, `category` y `amount` (HTTP 201) |
| POST | `/api/pedidos/{id}/confirmar` | Confirmar o devolver la confirmación previa (HTTP 200) |

```powershell
$base = 'http://127.0.0.1:8080/api'
$payload = @{ customer = 'Ana Torres'; category = 'NUEVO'; amount = 200 } | ConvertTo-Json
$order = Invoke-RestMethod "$base/pedidos" -Method Post -ContentType 'application/json' -Body $payload
$confirmed = Invoke-RestMethod "$base/pedidos/$($order.id)/confirmar" -Method Post
$confirmed | ConvertTo-Json -Depth 5
```

## Pruebas y extensiones

Ejecuta `mvn test`. `OrderServiceTest` cubre cálculos, redondeo, validaciones, extensión de Strategy, concurrencia y fallos de observadores. `OrderHttpTest` levanta Spring Boot en un puerto temporal, consulta la página y la API e incorpora un cuarto observador de prueba llamado Auditoría. Maven deja el detalle en `target/surefire-reports/`.

- **Strategy:** incorpora una clase `@Component` que implemente `DiscountPolicy` y declare un código único. `DiscountCatalog` recibe todos los beans de ese contrato y la web lista las categorías obtenidas de la API.
- **Observer:** incorpora una clase `@Component` que implemente `OrderObserver`. `ConfirmationPublisher` recibe todos los beans del contrato y entrega a cada uno el mismo `OrderConfirmed`.

Los pedidos y las reacciones se conservan en memoria hasta que se detiene el proceso. Los avisos se simulan mediante respuestas visibles en la aplicación y registros de consola. El servidor escucha en la interfaz local y la demo prescinde de cuentas, cobros y servicios externos.
