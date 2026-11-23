# WorldVPN

App de VPN personal para Android (Kotlin + Jetpack Compose). Cifra todo el tráfico del móvil con
**WireGuard** (`com.wireguard.android:tunnel`, modo userspace, sin root) hasta **tu propio servidor**.

La configuración por defecto es una única ubicación: **EE. UU. Este (Virginia)**, pensada para un
servidor gratuito de Oracle Cloud. Se pueden añadir más ubicaciones (ver abajo).

## 1. Compilar y ejecutar la app

1. Android Studio → **Open** → selecciona esta carpeta.
2. Espera al *Gradle Sync* (descargará Gradle 8.7, el SDK 34 si falta y las dependencias).
   Si Android Studio ofrece actualizar el plugin de Gradle (AGP), puedes aceptar o ignorarlo.
3. Conecta el móvil con depuración USB y pulsa **Run ▶**.

También hay un workflow de GitHub Actions (`.github/workflows/build.yml`) que compila el APK en cada
push; se descarga desde la pestaña *Actions* → último run → *Artifacts*.

## 2. Montar el servidor

La app necesita un servidor tuyo. Guía paso a paso para Oracle Cloud (gratis), pensada para Windows:
**[docs/GUIA-ORACLE.md](docs/GUIA-ORACLE.md)**.

Resumen: crear una instancia Ubuntu → abrir UDP 51820 → por SSH:

```bash
curl -fsSLO https://raw.githubusercontent.com/bratneytg/VPN/main/server/setup-server.sh
sudo bash setup-server.sh
```

→ pegar la línea JSON que imprime en la app (**Ajustes → Importar**) → **Conectar**.

El script sirve en cualquier VPS Ubuntu/Debian, no solo en Oracle.

## Añadir más ubicaciones

Repite el proceso en otro VPS indicando el `id` y los datos de la ubicación:

```bash
sudo bash setup-server.sh de-fra auto "Fráncfort" "Alemania" "🇩🇪" "Europa"
```

(`auto` = el servidor genera las claves del móvil.) Pega el JSON en **Ajustes → Importar**. Cada
servidor admite un dispositivo.

## Estructura

```
app/src/main/
├── assets/servers.json            Ubicaciones por defecto (id, nombre, país, endpoint, clave)
├── java/com/bratneytg/worldvpn/
│   ├── MainActivity.kt            Navegación y permiso de VPN
│   ├── VpnController.kt           Túnel WireGuard (GoBackend): conectar / desconectar / estado
│   ├── VpnViewModel.kt
│   ├── Server.kt, ServerRepository.kt   Ubicaciones, importación y claves
│   └── ui/                        Pantallas: Inicio, Ubicaciones, Ajustes
server/setup-server.sh             Convierte un VPS en servidor WireGuard
docs/GUIA-ORACLE.md                Guía paso a paso
```

## Privacidad y seguridad

- Todo el tráfico (IPv4 e IPv6) va por el túnel; DNS por defecto 1.1.1.1 / 1.0.0.1 (configurable por
  servidor con el campo `dns`).
- La clave privada se guarda en el almacenamiento privado de la app (sin copia de seguridad).
- Sin analíticas, sin publicidad, sin servidores de terceros.
- Kill switch: en Ajustes hay un acceso directo a los ajustes de VPN de Android para activar
  *VPN siempre activa* y *Bloquear conexiones sin VPN*.
- Un VPN no te hace anónimo: el proveedor del servidor ve el tráfico saliente y las webs pueden
  seguirte por cuentas, cookies o huella del navegador.
