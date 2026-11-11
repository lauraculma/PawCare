# Collar inteligente y simulador de ventas PawCare

## Funcionalidad incorporada

- Precio de demostración fijo: **$200.000 COP por collar**.
- Visor 3D del archivo `frontend/public/models/collar-pawcare.glb`.
- Controles para rotar y acercar/alejar el modelo.
- Formulario de comprador y selección de cantidad (1 a 10).
- Cálculo de subtotal y total.
- Confirmación de venta simulada con referencia y comprobante demostrativo.
- Historial local de ventas de demostración, almacenado en `localStorage` del navegador.

## Consideraciones

- No hay pasarela de pago ni cobro real.
- No se crea una orden en el backend ni se envía información a una transportadora.
- El historial es local al navegador y al dispositivo donde se realiza la demostración.
- El visor 3D carga la librería `@google/model-viewer` desde jsDelivr; por tanto, necesita conexión a Internet para cargar esa librería. El modelo `.glb` se sirve desde el propio frontend.
- Si la librería externa no está disponible, la página ofrece un enlace para abrir el archivo GLB directamente.

## Prueba local con Docker

Desde la raíz del proyecto:

```bash
docker compose up --build
```

Abrir `http://localhost:3000` (o el puerto que esté configurado en `APP_PORT`) y entrar en **Collar inteligente**.
