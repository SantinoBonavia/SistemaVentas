# Sistema de Ventas — Ferretería

Punto de venta de escritorio para una ferretería argentina, hecho en Java (Swing + FlatLaf).
Trabaja en pesos argentinos, calcula el IVA y descuenta el stock en cada venta.

## Qué hace

- **Vender (F1)**: el catálogo se ve como tarjetas con foto, precio final y stock. Tocás un producto
  o escribís/escaneás su código y presionás Enter. El ticket muestra subtotal, descuento, IVA y total.
  Medios de pago: efectivo (calcula el vuelto), débito, crédito, transferencia y QR. Cobrar con **F12**.
- **Comprobantes**: consumidor final → "B" con *IVA contenido* (Régimen de Transparencia Fiscal al
  Consumidor, Ley 27.743); cliente responsable inscripto o monotributista → "A" con neto gravado e IVA
  discriminados (valida el CUIT). Se pueden imprimir.
- **Productos (F2)**: alta, edición y baja; código/SKU, categoría, alícuota de IVA, stock mínimo y foto
  (una foto propia del equipo o una ilustración incorporada). El formulario calcula en vivo
  *precio sin IVA + IVA = precio final*. Ingreso de mercadería y ajustes de stock. Alertas de stock bajo.
- **Ventas (F3)**: historial por período (hoy, 7 días, mes, todo), total vendido, ticket promedio,
  IVA débito fiscal, ventas por medio de pago, productos más vendidos, reimpresión y anulación (devuelve
  la mercadería al stock).
- **Configuración (F4)**: datos del comercio que encabezan el ticket (CUIT, condición frente al IVA,
  punto de venta) y referencia de las alícuotas vigentes.

### IVA

Los precios se cargan y se muestran **finales, con IVA incluido**, como en el mostrador. Cada producto
tiene su alícuota: 21 % general (la de casi todo el rubro), 10,5 % reducida, 27 % incrementada o
0 % exento — vigentes según la Ley de IVA, art. 28, verificadas al 24/10/2026. Si cambian, se
actualizan desde *Productos → Editar*; las ventas ya emitidas conservan el IVA con el que se hicieron.

> Los tickets son **comprobantes internos**: no son facturas electrónicas de ARCA (no piden CAE).

## Cómo ejecutarlo

Requiere Java 21 y Maven.

```bash
mvn compile exec:java     # abre la aplicación
mvn test                  # corre las pruebas (sin pantalla)
```

La primera vez carga un catálogo de ejemplo de ferretería. Los datos (productos, ventas, fotos y
configuración) se guardan solos en `~/.sistemaventas/`. Para usar otra carpeta:
`mvn exec:java -Dsistemaventas.datos=/ruta/a/carpeta` o la variable `SISTEMAVENTAS_DATOS`.

## Estructura

| Paquete | Contenido |
|---|---|
| `inventory` | Productos: alta, edición, baja, búsqueda y movimientos de stock |
| `sales` | Carrito, cobro, anulación, reportes y formato del ticket |
| `tax` | Alícuotas de IVA, cálculo de IVA, CUIT y letra del comprobante |
| `store` | Datos del comercio |
| `persistence` | Guardado en disco |
| `ui` | Pantallas Swing, tema e ilustraciones de herramientas |

El esquema MySQL (`sql/`) y los diagramas UML (`docs/uml/`) documentan el modelo de datos original;
la aplicación de escritorio hoy guarda en un archivo local y no necesita MySQL.

## Tecnologías

- Java 21, Swing, [FlatLaf](https://www.formdev.com/flatlaf/)
- JUnit 5
- MySQL / JDBC (esquema y conexión opcional)
- PlantUML
