./gradlew build -x test
docker buildx build  --platform linux/amd64,linux/arm64 -t 743018522/onepiece-backend:latest --push .
kubectl delete pods --selector=app=onepiece-backend