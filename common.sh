# shared helpers for build-vps.sh / build-termux.sh
make_keystore() {
  if [ ! -f keystore.properties ]; then
    PASS=$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 16)
    keytool -genkeypair -v -keystore release.keystore -alias battle -keyalg RSA -keysize 2048 -validity 36500 \
      -storepass "$PASS" -keypass "$PASS" -dname "CN=Battle App, O=Battle, C=IN" >/dev/null 2>&1
    printf 'storeFile=release.keystore\nstorePassword=%s\nkeyAlias=battle\nkeyPassword=%s\n' "$PASS" "$PASS" > keystore.properties
    echo ">>> New signing key created (release.keystore + keystore.properties). BACK THESE 2 FILES UP:"
    echo ">>> every future update of the APK must be signed with the same key, or phones will refuse to update."
  fi
}
collect_apk() {
  mkdir -p dist
  APP_NAME=$(grep '^APP_NAME=' config.properties | cut -d= -f2- | tr ' ' '_')
  VER=$(grep '^VERSION_NAME=' config.properties | cut -d= -f2-)
  cp app/build/outputs/apk/release/app-release.apk "dist/${APP_NAME}-${VER}.apk"
  echo; echo "APK READY:  $(pwd)/dist/${APP_NAME}-${VER}.apk"
  echo "Upload it to cPanel and paste its link in Admin > Settings (APK link)."
}
