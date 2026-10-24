#!/usr/bin/env bash
# Builds the game for players: one zip per computer type, with Java included,
# so players only unzip it and double-click. Nothing to install.
#
#   ./build.sh            (Linux / Mac terminal, or Git Bash on Windows)
#
# Result (in dist/, which is in .gitignore):
#   Minigames.jar                the game itself (needs Java 21+ installed to run directly)
#   Minigames-windows-x64.zip    -> Minigames.bat
#   Minigames-linux-x64.zip      -> Minigames.sh
#   Minigames-mac-arm64.zip      -> Minigames.app   (Apple Silicon: M1, M2, ...)
#   Minigames-mac-x64.zip        -> Minigames.app   (older Intel Macs)
#
# ONLINE SCORES: if config.properties exists, a copy goes INTO the jar, so the game
# connects by itself. Anyone can unzip a jar and read it, so it must be the LIMITED
# game user from docs/supabase_game_user.sql, never the postgres admin password.
#
# Needs: a JDK 21+ (javac, jar), curl, tar, unzip, zip.
# The Java runtimes come from Eclipse Temurin (adoptium.net), downloaded once into build/jre.

set -euo pipefail
cd "$(dirname "$0")"

# The Java version put in the zips (pinned, so every build is the same)
JRE_VERSION="21.0.12.1+1"
JRE_FILE_VERSION="${JRE_VERSION/+/_}"   # 21.0.12.1_1
JRE_BASE_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${JRE_VERSION/+/%2B}"

# platform name : Temurin os : Temurin cpu : archive type
PLATFORMS=(
    "windows-x64:windows:x64:zip"
    "linux-x64:linux:x64:tar.gz"
    "mac-arm64:mac:aarch64:tar.gz"
    "mac-x64:mac:x64:tar.gz"
)

BUILD=build
DIST=dist
JAR_NAME=Minigames.jar

for tool in javac jar curl tar unzip zip; do
    command -v "$tool" > /dev/null || { echo "Missing '$tool', please install it."; exit 1; }
done

echo "== 1/3 Compiling"
rm -rf "$BUILD/classes" "$BUILD/jar" "$DIST"
mkdir -p "$BUILD/classes" "$BUILD/jar" "$DIST"
# --release 21 = runs on Java 21 and newer
javac --release 21 -encoding UTF-8 -d "$BUILD/classes" -cp "lib/*" \
    src/common/*.java src/f1/*.java src/battletanks/*.java

echo "== 2/3 Making $JAR_NAME"
# One jar with everything inside: our classes, the pictures and music, and both drivers
cp -r "$BUILD/classes/." "$BUILD/jar/"
cp -r resources "$BUILD/jar/"
DRIVERS=""
for driver in lib/*.jar; do
    (cd "$BUILD/jar" && unzip -qo "../../$driver" -x META-INF/MANIFEST.MF)
    # Both drivers have META-INF/services/java.sql.Driver (how JDBC finds them). Unzipping
    # the second one overwrites the first, so we glue both lists together instead.
    DRIVERS+="$(unzip -p "$driver" META-INF/services/java.sql.Driver 2>/dev/null || true)"$'\n'
done
printf '%s' "$DRIVERS" | grep -v '^\s*$' > "$BUILD/jar/META-INF/services/java.sql.Driver"
rm -f "$BUILD/jar"/META-INF/*.SF "$BUILD/jar"/META-INF/*.RSA "$BUILD/jar"/META-INF/*.DSA # signatures of the drivers' own jars

if [[ -f config.properties ]]; then
    cp config.properties "$BUILD/jar/"
    if grep -q '^db.user=postgres\.' config.properties; then
        echo "   WARNING: config.properties uses the postgres ADMIN user. Anyone with the jar can read it."
        echo "            Use the limited game user (docs/supabase_game_user.sql) for builds you share."
    fi
    echo "   config.properties added: the game connects to Supabase by itself"
else
    echo "   No config.properties: this build only saves scores offline"
fi

cat > "$BUILD/manifest.txt" <<'EOF'
Main-Class: common.Main
Enable-Native-Access: ALL-UNNAMED
Multi-Release: true
EOF
jar --create --file "$DIST/$JAR_NAME" --manifest "$BUILD/manifest.txt" -C "$BUILD/jar" .

echo "== 3/3 Making the zips (Java $JRE_VERSION included)"
mkdir -p "$BUILD/jre"
for platform in "${PLATFORMS[@]}"; do
    IFS=: read -r name os cpu archive <<< "$platform"
    file="OpenJDK21U-jre_${cpu}_${os}_hotspot_${JRE_FILE_VERSION}.${archive}"

    # Download the Java runtime once, and check it wasn't damaged on the way (sha256)
    if [[ ! -f "$BUILD/jre/$file" ]]; then
        echo "   downloading Java for $name"
        curl -fsSL -o "$BUILD/jre/$file.part" "$JRE_BASE_URL/$file"
        expected=$(curl -fsSL "$JRE_BASE_URL/$file.sha256.txt" | cut -d' ' -f1)
        actual=$( (sha256sum "$BUILD/jre/$file.part" 2>/dev/null || shasum -a 256 "$BUILD/jre/$file.part") | cut -d' ' -f1)
        [[ "$expected" == "$actual" ]] || { echo "Checksum wrong for $file"; rm "$BUILD/jre/$file.part"; exit 1; }
        mv "$BUILD/jre/$file.part" "$BUILD/jre/$file"
    fi

    out="$BUILD/zip/$name/Minigames"
    rm -rf "$BUILD/zip/$name"
    mkdir -p "$BUILD/zip/$name/unpacked" "$out"
    if [[ $archive == zip ]]; then
        unzip -q "$BUILD/jre/$file" -d "$BUILD/zip/$name/unpacked"
    else
        tar -xzf "$BUILD/jre/$file" -C "$BUILD/zip/$name/unpacked"
    fi
    runtime=$(echo "$BUILD/zip/$name/unpacked"/*)  # the one folder inside, like jdk-21.0.12.1+1-jre

    case $os in
        windows)
            mv "$runtime" "$out/runtime"
            cp "$DIST/$JAR_NAME" "$out/"
            # javaw = Java without a black console window. "start" lets the .bat window close.
            printf '%s\r\n' '@echo off' 'cd /d "%~dp0"' \
                'start "" "runtime\bin\javaw.exe" --enable-native-access=ALL-UNNAMED -jar Minigames.jar' \
                > "$out/Minigames.bat"
            ;;
        linux)
            mv "$runtime" "$out/runtime"
            cp "$DIST/$JAR_NAME" "$out/"
            cat > "$out/Minigames.sh" <<'EOF'
#!/bin/sh
# Starts the game with the Java that comes with it
cd "$(dirname "$0")"
exec ./runtime/bin/java --enable-native-access=ALL-UNNAMED -jar Minigames.jar "$@"
EOF
            chmod +x "$out/Minigames.sh"
            ;;
        mac)
            # A Mac app is a folder: Minigames.app/Contents/{Info.plist, MacOS, Resources}
            app="$out/Minigames.app/Contents"
            mkdir -p "$app/MacOS" "$app/Resources"
            mv "$runtime" "$app/runtime"
            cp "$DIST/$JAR_NAME" "$app/Resources/"
            cat > "$app/MacOS/Minigames" <<'EOF'
#!/bin/sh
# Starts the game with the Java that comes with it
CONTENTS="$(cd "$(dirname "$0")/.." && pwd)"
exec "$CONTENTS/runtime/Contents/Home/bin/java" -Xdock:name="Mini Games" \
    --enable-native-access=ALL-UNNAMED -jar "$CONTENTS/Resources/Minigames.jar"
EOF
            chmod +x "$app/MacOS/Minigames"
            cat > "$app/Info.plist" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key><string>Mini Games</string>
    <key>CFBundleDisplayName</key><string>Mini Games</string>
    <key>CFBundleIdentifier</key><string>com.vice2005.minigames</string>
    <key>CFBundleVersion</key><string>1.0</string>
    <key>CFBundleShortVersionString</key><string>1.0</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>CFBundleExecutable</key><string>Minigames</string>
    <key>NSHighResolutionCapable</key><true/>
</dict>
</plist>
EOF
            ;;
    esac
    cp HOW_TO_PLAY.txt "$out/"
    rm -rf "$BUILD/zip/$name/unpacked"

    # zip -y keeps links, and zip keeps the "can run" flag of the scripts for Linux/Mac
    (cd "$BUILD/zip/$name" && zip -qry "../../../$DIST/Minigames-$name.zip" Minigames)
    echo "   $DIST/Minigames-$name.zip"
done

echo "Done. Upload the zips (e.g. as a GitHub Release). Don't commit them, dist/ is in .gitignore."
