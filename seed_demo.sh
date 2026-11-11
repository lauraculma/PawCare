#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost/api}"
OWNER_EMAIL="${DEMO_OWNER_EMAIL:-test@example.com}"
OWNER_PASSWORD="${DEMO_OWNER_PASSWORD:-ChangeMe123!}"
VET_EMAIL="${DEMO_VET_EMAIL:-vet@test.com}"
VET_PASSWORD="${DEMO_VET_PASSWORD:-ChangeMe123!}"

register_user() {
  local full_name="$1" email="$2" password="$3" role="$4"
  local payload
  payload=$(printf '{"fullName":"%s","email":"%s","password":"%s","role":"%s"}' \
    "$full_name" "$email" "$password" "$role")

  # A 409 means the account already exists; other failures stop the script.
  local status
  status=$(curl -sS -o /tmp/pawcare-seed-response.json -w '%{http_code}' \
    -X POST -H 'Content-Type: application/json' -d "$payload" \
    "$BASE_URL/auth/register")
  if [[ "$status" != "201" && "$status" != "409" ]]; then
    cat /tmp/pawcare-seed-response.json
    echo "No fue posible registrar $email (HTTP $status)." >&2
    exit 1
  fi
  echo "Usuario disponible: $email"
}

register_user "Tutor Demo" "$OWNER_EMAIL" "$OWNER_PASSWORD" "OWNER"
register_user "Veterinario Demo" "$VET_EMAIL" "$VET_PASSWORD" "VETERINARIAN"

echo "Usuarios de demostracion registrados."
echo "Propietario: $OWNER_EMAIL (puede iniciar sesion)."
echo "Veterinario: $VET_EMAIL (requiere aprobacion de un administrador antes de iniciar sesion)."
echo "Para cargar mascotas y datos de ejemplo, ejecuta seed_demo.sql con psql."
