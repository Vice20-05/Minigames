#!/bin/sh
# Starts Mini Games on Linux. Needs Java 21 or newer (https://adoptium.net).
cd "$(dirname "$0")" || exit 1

if ! command -v java > /dev/null 2>&1; then
    echo "Java was not found. Install Java 21 or newer from https://adoptium.net and try again."
    exit 1
fi

# "java -version" prints for example: openjdk version "21.0.5" -> 21 (old Java 8: "1.8.0" -> 8)
version=$(java -version 2>&1 | sed -n 's/.*version "\(1\.\)\{0,1\}\([0-9]*\).*/\2/p' | head -n 1)
if [ "${version:-0}" -lt 21 ]; then
    echo "Mini Games needs Java 21 or newer, this computer has Java ${version:-?}."
    echo "Install it from https://adoptium.net and try again."
    exit 1
fi

exec java -jar Minigames.jar
