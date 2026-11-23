# WorldVPN

App de VPN para Android (Kotlin + Jetpack Compose) con ubicaciones en todo el mundo. Usa el túnel
**WireGuard** oficial (`com.wireguard.android:tunnel`, modo userspace, sin root).

## Compilar y ejecutar

1. Android Studio → **Open** → selecciona esta carpeta.
2. Espera al *Gradle Sync* (descargará Gradle 8.7, el SDK 34 si falta y las dependencias).
   Si Android Studio ofrece actualizar el plugin de Gradle (AGP), puedes aceptar o ignorarlo.
3. Conecta el móvil con depuración USB y pulsa **Run ▶**.
4. Al pulsar *Conectar* por primera vez, Android pedirá permiso para crear una VPN.

También hay un workflow de GitHub Actions (`.github/workflows/build.yml`) que compila el APK en cada
push; se descarga desde la pestaña *Actions* → último run → *Artifacts*.

## Importante: las ubicaciones necesitan servidores

Una app de VPN no crea ubicaciones por sí sola: cada ubicación es **un servidor tuyo** en ese país.
La app incluye 18 ubicaciones (Europa, América, Asia, Oriente Medio, Oceanía y África), pero aparecen
como *"Sin servidor"* hasta que despliegues uno. Así se activa cada una (≈5 minutos):

1. Alquila un VPS barato Ubuntu/Debian en esa ciudad (Hetzner, Vultr, DigitalOcean, Oracle Cloud
   tiene capa gratuita, etc.). Con 1 vCPU y 512 MB sobra.
2. En el panel del proveedor abre el puerto **UDP 51820**.
3. En la app: **Ajustes → Copiar clave** (es la clave pública de tu móvil).
4. Entra al VPS por SSH, copia `server/setup-server.sh` y ejecuta:
   ```bash
   sudo ./setup-server.sh de-fra "CLAVE_PUBLICA_DE_TU_MOVIL"
   ```
   (`de-fra` es el `id` de la ubicación, ver `app/src/main/assets/servers.json`).
5. El script imprime una línea JSON. Cópiala y pégala en la app: **Ajustes → Importar**.
6. Elige la ubicación y conecta.

No hace falta recompilar para añadir servidores. Si prefieres dejarlos fijos en la app, rellena
`endpoint` y `publicKey` en `app/src/main/assets/servers.json`.

### Oracle Cloud (capa gratuita)

- Crea la instancia con imagen **Ubuntu 22.04/24.04** (no Oracle Linux: el script usa `apt`).
- Además del firewall del script, abre **UDP 51820** en la *Security List* de la subred (VCN →
  subred → Default Security List → Add Ingress Rules → UDP, destino 51820, origen 0.0.0.0/0).
- La instancia gratuita *Micro* limita Internet a ~50 Mbps y Oracle puede recuperar instancias
  inactivas; ver https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm

Para una ubicación que no esté en la lista, pasa nombre, país, bandera y región al script:

```bash
sudo ./setup-server.sh ar-bue "CLAVE" "Buenos Aires" "Argentina" "🇦🇷" "América"
```

Cada servidor admite **un dispositivo** (10.66.66.2). Para varios móviles habría que dar a cada uno
una IP distinta y añadir más bloques `[Peer]`.

## Estructura

```
app/src/main/
├── assets/servers.json            Lista de ubicaciones (id, nombre, país, bandera, endpoint, clave)
├── java/com/bratneytg/worldvpn/
│   ├── MainActivity.kt            Navegación y permiso de VPN
│   ├── VpnController.kt           Túnel WireGuard (GoBackend): conectar / desconectar / estado
│   ├── VpnViewModel.kt
│   ├── Server.kt, ServerRepository.kt   Ubicaciones, importación y claves del dispositivo
│   └── ui/                        Pantallas: Home, Ubicaciones, Ajustes
server/setup-server.sh             Convierte un VPS en servidor WireGuard
```

## Privacidad y seguridad

- Todo el tráfico (IPv4 e IPv6) va por el túnel; DNS por defecto 1.1.1.1 / 1.0.0.1 (configurable por
  servidor con el campo `dns`).
- La clave privada del móvil se genera en el dispositivo, se guarda en el almacenamiento privado de la
  app (sin copia de seguridad) y nunca se envía a ninguna parte.
- La app no tiene analíticas ni publicidad. Tu proveedor del VPS sí ve el tráfico de salida, así que
  usa uno de confianza.
- Pendiente (ideas): kill switch propio (hoy puedes usar *VPN siempre activa* + *Bloquear conexiones sin
  VPN* de Android), selección de apps, ping a cada servidor, varios dispositivos.
