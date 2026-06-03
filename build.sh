trap 'rm -rf build' EXIT INT TERM

javac -d build $(find src -name "*.java") || exit 1
java -cp build Main
