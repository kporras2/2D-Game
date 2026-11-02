#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mvn -q package
java -Djava.awt.headless=true -cp target/classes:target/test-classes main.PrototypeChecks
java -Djava.awt.headless=true -cp target/classes:target/test-classes main.SpriteSheetChecks
