./gradlew build -x test
docker buildx build  --platform linux/amd64,linux/arm64 -t registry.cn-shanghai.aliyuncs.com/onepiece-battles/onepiece-backend:latest --push .