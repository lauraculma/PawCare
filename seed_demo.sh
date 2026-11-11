#!/usr/bin/env bash
set -e

BASE_URL=${1:-http://localhost/api}

echo "Seed demo PawCare"

# Register demo owner if not exists
curl -s -X POST -H 'Content-Type: application/json' -d '{
  "fullName":"Ana García",
  "email":"ana@example.com",
  "password":"AnaPass123",
  "role":"OWNER",
  "phone":"3001234567"
}' "$BASE_URL/auth/register" || true

# Register demo vet
curl -s -X POST -H 'Content-Type: application/json' -d '{
  "fullName":"Dr. Laura Méndez",
  "email":"laura.vet@example.com",
  "password":"LauraVet123",
  "role":"VETERINARIAN",
  "phone":"3007654321"
}' "$BASE_URL/auth/register" || true

echo "Seed completado. Revisa usuarios en la DB."
