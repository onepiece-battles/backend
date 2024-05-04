./gradlew build -x test
docker buildx build  --platform linux/amd64,linux/arm64 -t 743018522/onepiece-backend:latest --push .
docker image push 743018522/onepiece-backend:latest
docker rmi 743018522/onepiece-backend:latest
kubectl delete pods --selector=app=onepiece-backend