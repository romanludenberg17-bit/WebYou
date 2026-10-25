#!/usr/bin/env bash
set -euo pipefail

mkdir -p signing
keytool -genkeypair \
  -alias webyou \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -keystore signing/webyou-release.jks \
  -storetype PKCS12 \
  -storepass "${KEYSTORE_PASSWORD:-change-me}" \
  -keypass "${KEY_PASSWORD:-change-me}" \
  -dname "CN=WebYou, OU=WebYou, O=WebYou, L=Internet, ST=Internet, C=XX"

echo "created signing/webyou-release.jks"
echo "do not commit this file to git"
