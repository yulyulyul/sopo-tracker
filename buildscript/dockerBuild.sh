#!/bin/bash
cd ..
#./gradlew clean
./gradlew bootJar
docker rmi www.sooopo.com/sopo_prelive/tracker:0.0.2
docker build -f ./src/main/resources/Dockerfile . -t www.sooopo.com/sopo_prelive/tracker:0.0.2
docker push www.sooopo.com/sopo_prelive/tracker:0.0.2
exit 0
