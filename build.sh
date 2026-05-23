javac -d build $(find src -name "*.java")
java -cp build Main
rm -rf build