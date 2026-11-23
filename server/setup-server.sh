#!/usr/bin/env bash
#
# Convierte un VPS Ubuntu/Debian en un servidor WireGuard para la app WorldVPN.
#
# Uso (como root, en el VPS de cada ubicación):
#   sudo ./setup-server.sh <ID> <CLAVE_PUBLICA_DEL_MOVIL> [Nombre] [País] [Bandera] [Región]
#
# Ejemplo:
#   sudo ./setup-server.sh de-fra "AbCd...=" "Fráncfort" "Alemania" "🇩🇪" "Europa"
#
# - <ID> debe coincidir con el "id" de la ubicación en app/src/main/assets/servers.json
#   (de-fra, jp-tyo, us-nyc...). Los demás argumentos solo hacen falta si es una ubicación nueva.
# - <CLAVE_PUBLICA_DEL_MOVIL> la copias en la app: Ajustes -> "Copiar clave".
#
# Al terminar imprime un JSON: cópialo y pégalo en la app (Ajustes -> Importar).
# Este script admite UN dispositivo por servidor (10.66.66.2). Si lo vuelves a ejecutar con otra
# clave, sustituye a la anterior.

set -euo pipefail

if [[ $EUID -ne 0 ]]; then
  echo "Ejecuta este script como root (sudo)." >&2
  exit 1
fi

if [[ $# -lt 2 ]]; then
  sed -n '3,17p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

ID="$1"
CLIENT_PUB="$2"
NAME="${3:-}"
COUNTRY="${4:-}"
FLAG="${5:-}"
REGION="${6:-}"

PORT="${WG_PORT:-51820}"
SERVER_ADDR="10.66.66.1/24"
CLIENT_ADDR="10.66.66.2/32"

if ! [[ "$CLIENT_PUB" =~ ^[A-Za-z0-9+/]{43}=$ ]]; then
  echo "La clave pública del móvil no parece válida (debe tener 44 caracteres y acabar en '=')." >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
echo "==> Instalando WireGuard..."
apt-get update -y >/dev/null
apt-get install -y wireguard iptables curl >/dev/null

umask 077
mkdir -p /etc/wireguard

if [[ ! -s /etc/wireguard/server_private.key ]]; then
  wg genkey | tee /etc/wireguard/server_private.key | wg pubkey > /etc/wireguard/server_public.key
fi
SERVER_PRIV="$(cat /etc/wireguard/server_private.key)"
SERVER_PUB="$(cat /etc/wireguard/server_public.key)"

IFACE="$(ip -4 route show default | awk '{print $5; exit}')"
if [[ -z "$IFACE" ]]; then
  echo "No he podido detectar la interfaz de red de salida." >&2
  exit 1
fi

PUBLIC_IP="$(curl -4 -fsS --max-time 10 https://api.ipify.org || curl -4 -fsS --max-time 10 https://ifconfig.me || true)"
if [[ -z "$PUBLIC_IP" ]]; then
  echo "No he podido averiguar la IP pública. Edita el JSON final a mano." >&2
  PUBLIC_IP="REEMPLAZA_CON_LA_IP_DEL_SERVIDOR"
fi

echo "==> Escribiendo /etc/wireguard/wg0.conf (salida por $IFACE)..."
cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
Address = ${SERVER_ADDR}
ListenPort = ${PORT}
PrivateKey = ${SERVER_PRIV}
PostUp = iptables -I INPUT -p udp --dport ${PORT} -j ACCEPT; iptables -I FORWARD -i wg0 -j ACCEPT; iptables -I FORWARD -o wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o ${IFACE} -j MASQUERADE
PostDown = iptables -D INPUT -p udp --dport ${PORT} -j ACCEPT; iptables -D FORWARD -i wg0 -j ACCEPT; iptables -D FORWARD -o wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o ${IFACE} -j MASQUERADE

[Peer]
PublicKey = ${CLIENT_PUB}
AllowedIPs = ${CLIENT_ADDR}
EOF
chmod 600 /etc/wireguard/wg0.conf

echo "==> Activando reenvío de paquetes..."
echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-wireguard.conf
sysctl --system >/dev/null

if command -v ufw >/dev/null 2>&1 && ufw status | grep -q "Status: active"; then
  echo "==> Abriendo ${PORT}/udp en ufw..."
  ufw allow "${PORT}/udp" >/dev/null
fi

echo "==> Arrancando WireGuard..."
systemctl enable wg-quick@wg0 >/dev/null 2>&1
systemctl restart wg-quick@wg0

# Construye el JSON (solo incluye los campos opcionales que se hayan pasado)
JSON="{ \"id\": \"${ID}\""
[[ -n "$NAME"    ]] && JSON+=", \"name\": \"${NAME}\""
[[ -n "$COUNTRY" ]] && JSON+=", \"country\": \"${COUNTRY}\""
[[ -n "$FLAG"    ]] && JSON+=", \"flag\": \"${FLAG}\""
[[ -n "$REGION"  ]] && JSON+=", \"region\": \"${REGION}\""
JSON+=", \"endpoint\": \"${PUBLIC_IP}\", \"port\": ${PORT}, \"publicKey\": \"${SERVER_PUB}\" }"

echo
echo "=============================================================="
echo " Servidor listo. Recuerda abrir el puerto UDP ${PORT} en el firewall"
echo " de tu proveedor (panel web del VPS)."
echo
echo " Copia esta línea y pégala en la app (Ajustes -> Importar):"
echo
echo "${JSON}"
echo "=============================================================="
