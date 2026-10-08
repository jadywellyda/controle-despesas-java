#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
TASK_ACTION="${1:-executar}"
case "$TASK_ACTION" in executar|testar|compilar) ;; *) echo 'Use executar, testar ou compilar.' >&2; exit 1;; esac
mkdir -p build/main
find src/main/java -name '*.java' > build/main-sources.txt
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d build/main @build/main-sources.txt
if [ "$TASK_ACTION" = 'testar' ]; then
    mkdir -p build/test
    find src/test/java -name '*.java' > build/test-sources.txt
    javac --release 21 -encoding UTF-8 -Xlint:all -Werror -cp build/main -d build/test @build/test-sources.txt
    java -Dfile.encoding=UTF-8 -cp 'build/main:build/test' br.dev.jady.despesas.ControleDespesasTest
elif [ "$TASK_ACTION" = 'executar' ]; then
    if [ "$#" -ge 2 ]; then java -Dfile.encoding=UTF-8 -cp build/main br.dev.jady.despesas.Main "$2"
    else java -Dfile.encoding=UTF-8 -cp build/main br.dev.jady.despesas.Main; fi
fi
