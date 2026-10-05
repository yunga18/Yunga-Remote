# Yunga Remote

Aplicación Android pensada para el **Redmi Note 12** y un TV Stick genérico compatible con el perfil IR **X96 Max+**.

## Qué hace

- Mando por **infrarrojo** sin instalar nada en el TV Stick.
- Perfil NEC a 38 kHz para la familia X96 Max+, con navegación, OK, Inicio, Atrás, Menú, volumen, silencio, reproducción/pausa, modo mouse y encendido.
- Modo **teclado Bluetooth HID** para escribir desde el celular en el TV Stick.
- No usa Internet, no tiene anuncios y no necesita servidor.

## Uso del mando IR

Abre la app y usa los botones. El IR funciona de manera independiente del Bluetooth.

## Uso del teclado Bluetooth

1. Activa Bluetooth en el Redmi y en el TV Stick.
2. En Yunga Remote toca **Activar teclado Bluetooth**.
3. Toca **Hacer visible el celular (5 min)**.
4. En el TV Stick entra en `Ajustes → Bluetooth / Accesorios → Añadir dispositivo` y selecciona el Redmi.
5. Regresa a Yunga Remote, actualiza la lista de emparejados y toca **Conectar como teclado**.
6. Escribe en el cuadro y pulsa **Enviar texto**.

> El perfil HID depende del soporte de Bluetooth del firmware del celular y del TV Stick. Si el HID no está disponible, el mando infrarrojo sigue funcionando normalmente.

## Compilar

### Android Studio

Abre la carpeta como proyecto Android y ejecuta `app` en el Redmi Note 12.

### GitHub Actions

El proyecto incluye `.github/workflows/build-apk.yml`. Cada cambio en `main` compila un APK de prueba y lo deja como artefacto `YungaRemote-debug-apk`.

## Compatibilidad de texto

El envío HID incluido cubre letras ASCII, números y signos comunes. Tildes y `ñ` pueden depender de la distribución de teclado configurada en el TV Stick.
