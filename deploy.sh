./gradlew build -x test
docker build -t 743018522/onepiece-backend:latest .
docker image push 743018522/onepiece-backend:latest
kubectl rollout restart deployment onepiece-backend