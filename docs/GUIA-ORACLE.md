# Guía: tu servidor VPN gratis en Oracle Cloud (Virginia, EE. UU.)

Tiempo: unos 20-30 minutos. Oracle no tiene región en Nueva York; la más cercana es
**US East (Ashburn, Virginia)**, que es la que usa la app (`us-ash`).

> **Importante antes de empezar:** la región "home" que eliges al registrarte **no se puede cambiar
> después** y es la única donde tendrás recursos gratis. Elige **US East (Ashburn)**.

## 1. Crear la cuenta

1. Entra en https://www.oracle.com/cloud/free/ → **Start for free**.
2. Rellena tus datos. Pide una **tarjeta de crédito/débito** (no acepta prepago, virtual ni de un
   solo uso) para verificar la identidad.
3. En **Home Region** elige **US East (Ashburn)**. Si no aparece, elige la más cercana y
   avísame para ajustar la guía.
4. Espera el correo de "cuenta lista" (puede tardar unos minutos).

## 2. Crear el servidor (instancia)

Menú ☰ → **Compute → Instances → Create instance**.

- **Image:** *Change image* → **Canonical Ubuntu 24.04** (o 22.04). No Oracle Linux.
- **Shape:** *Change shape*.
  - Primera opción: **Ampere → VM.Standard.A1.Flex** con 1 OCPU y 6 GB (más rápido, sin el límite
    de 50 Mbps). Si da "Out of host capacity", reintenta más tarde.
  - Alternativa: **Specialty and previous generation → VM.Standard.E2.1.Micro** (AMD, hasta 50 Mbps).
- **Networking:** deja *Create new virtual cloud network* y *Create new public subnet*, y
  **Assign a public IPv4 address** activado.
- **Add SSH keys:** *Generate a key pair for me* → pulsa **Save private key** (guarda el fichero
  `.key` en un sitio seguro; no se puede volver a descargar).
- **Create**. Cuando esté en estado *Running*, anota su **Public IP address**.

## 3. Abrir el puerto de WireGuard (UDP 51820)

En la instancia: enlace **Subnet** → **Default Security List for …** → **Add Ingress Rules**:

- Source CIDR: `0.0.0.0/0`
- IP Protocol: `UDP`
- Destination Port Range: `51820`

→ **Add Ingress Rules**.

## 4. Conectarte por SSH desde Windows

Abre **PowerShell**. Si Windows se queja de permisos de la clave (`UNPROTECTED PRIVATE KEY FILE`):

```powershell
icacls "C:\ruta\a\ssh-key.key" /inheritance:r
icacls "C:\ruta\a\ssh-key.key" /grant:r "$($env:USERNAME):R"
```

Conéctate (usuario `ubuntu`):

```powershell
ssh -i "C:\ruta\a\ssh-key.key" ubuntu@TU_IP_PUBLICA
```

## 5. Instalar el VPN en el servidor (un solo comando)

Ya dentro del servidor:

```bash
curl -fsSLO https://raw.githubusercontent.com/bratneytg/VPN/main/server/setup-server.sh
sudo bash setup-server.sh
```

Al terminar imprime **una línea JSON**. Cópiala entera.

> Esa línea incluye la clave privada de tu móvil: trátala como una contraseña. Pásala a tu móvil
> por un canal tuyo (por ejemplo, "Mensajes guardados" de Telegram o un correo a ti mismo) y bórrala
> después.

## 6. Importar en la app y conectar

1. App **WorldVPN** → ⚙ **Ajustes** → **Importar** → pega la línea → **Importar**.
2. Vuelve a la pantalla principal → **Conectar** → acepta el permiso de VPN de Android.
3. Comprueba en https://ifconfig.me desde el navegador del móvil: debe mostrar la IP de Oracle (EE. UU.).

## 7. Recomendado: evitar fugas

En la app: ⚙ Ajustes → **Abrir ajustes de VPN de Android** → rueda dentada junto a WorldVPN →
activa **VPN siempre activa** y **Bloquear conexiones sin VPN**. Así, si el túnel cae, el móvil no
sale a internet sin cifrar.

## Problemas típicos

| Síntoma | Causa probable |
|---|---|
| Conecta pero no hay internet | Falta la regla UDP 51820 (paso 3), o no se ejecutó el script completo |
| "Out of host capacity" al crear la instancia | Oracle sin hueco en ese momento: reintenta o usa la AMD Micro |
| Funciona unos días y luego se para | Oracle recupera instancias gratis "inactivas" (CPU y red <20 % durante 7 días). Pasar la cuenta a *Pay As You Go* lo evita; solo pagas lo que supere los límites gratuitos |
| Muy lento | La AMD Micro limita a ~50 Mbps; usa la Ampere A1 |

## Qué protege y qué no

El VPN cifra tu tráfico entre el móvil y el servidor: tu proveedor de internet o el Wi-Fi público
no ven qué webs visitas, y las webs ven la IP de Oracle en vez de la tuya. **No te hace anónimo**:
Oracle ve el tráfico que sale de su servidor y tu cuenta está ligada a tu identidad y tu tarjeta, y
las webs pueden seguirte igualmente por cuentas iniciadas, cookies o la huella del navegador.
