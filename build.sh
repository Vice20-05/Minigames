#!/usr/bin/env bash
# Builds the game for players: one zip to upload as a GitHub Release.
#
#   ./build.sh            (Linux / Mac terminal, or Git Bash on Windows)
#
# Result (in dist/, which is in .gitignore, never commit it):
#   Minigames.jar   the game: classes, pictures, music and both database drivers in ONE file
#   Minigames.zip   the folder "Minigames" with:
#                     Minigames.jar
#                     run.bat       (Windows)   \
#                     run.sh        (Linux)      > from launchers/, each one starts the jar
#                     run.command   (macOS)     /
#                     HOW_TO_PLAY.txt
#   Players need Java 21 or newer. They can also just double-click Minigames.jar.
#
# ONLINE SCORES: if config.properties exists, a copy goes INTO the jar, so the game
# connects by itself. Anyone can unzip a jar and read it, so it must be the LIMITED
# game user from docs/supabase_game_user.sql, never the postgres admin password.
# config.properties itself stays out of git (.gitignore).
#
# Needs: a JDK 21+ (javac, jar), unzip, zip.

set -euo pipefail
cd "$(dirname "$0")"

BUILD=build
DIST=dist
JAR_NAME=Minigames.jar

for tool in javac jar unzip zip; do
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

echo "== 3/3 Making Minigames.zip"
out="$BUILD/zip/Minigames"
rm -rf "$BUILD/zip"
mkdir -p "$out"
cp "$DIST/$JAR_NAME" HOW_TO_PLAY.txt launchers/run.bat launchers/run.sh launchers/run.command "$out/"
chmod +x "$out/run.sh" "$out/run.command"
# zip keeps the "can run" flag of run.sh and run.command for Linux and macOS
(cd "$BUILD/zip" && zip -qr "../../$DIST/Minigames.zip" Minigames)

echo "Done: $DIST/Minigames.zip (upload it as a GitHub Release, don't commit it)"
