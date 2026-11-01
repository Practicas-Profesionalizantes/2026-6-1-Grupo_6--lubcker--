#!/bin/sh
# Copia las imagenes del repositorio al volumen de uploads (sin pisar las que ya existan) y arranca el backend.
cp -n /app/uploads-semilla/* /app/uploads/ 2>/dev/null || true
exec java -jar /app/app.jar
