# ============================================================
# Recall - 운영 컨테이너 이미지 (멀티스테이지)
# Oracle Cloud Ampere(ARM64) / x86 모두 지원 (temurin 멀티아치)
# 빌드:  docker build -t recall .
# 실행:  docker run -d --name recall -p 8080:8080 \
#          -e SPRING_PROFILES_ACTIVE=prod \
#          -e GITHUB_CLIENT_ID=... -e GITHUB_CLIENT_SECRET=... \
#          -e APP_LOGIN_USERNAME=... -e APP_LOGIN_PASSWORD=... \
#          -v $PWD/data:/app/data \
#          recall
# ============================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/project-recall-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
# 데이터(./data)는 볼륨으로 마운트해 영속 보관
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
