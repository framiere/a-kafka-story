find . -name "docker-compose.yml" -exec docker-compose -f {} pull \;
find . -name "docker-compose.yml" -exec docker-compose -f {} build \;
mvn install
