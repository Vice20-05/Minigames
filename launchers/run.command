#!/bin/sh
# Starts Mini Games on macOS (double-click it, it opens in Terminal).
# Needs Java 21 or newer (https://adoptium.net).
cd "$(dirname "$0")" || exit 1

# macOS has a "java" command even without Java, it then prints "No Java runtime present",
# so we check the version instead of only checking that the command exists.
# "java -version" prints for example: openjdk version "21.0.5" -> 21 (old Java 8: "1.8.0" -> 8)
version=$(java -version 2>&1 | sed -n 's/.*version "\(1\.\)\{0,1\}\([0-9]*\).*/\2/p' | head -n 1)
if [ "${version:-0}" -lt 21 ]; then
    echo "Mini Games needs Java 21 or newer (found: ${version:-none})."
    echo "Install it from https://adoptium.net and try again."
    exit 1
fi

exec java -Xdock:name="Mini Games" -jar Minigames.jar
