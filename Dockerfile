# ──────────────────────────────────────────────────────────
# Multi-stage Build
# Stage 1: Gradle 빌드
# Stage 2: 실행 이미지 (JRE만 포함, 용량 최소화)
# ──────────────────────────────────────────────────────────

# Stage 1 — Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 의존성 캐시 레이어 (소스 변경 시에도 재다운로드 방지)
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 비-루트 유저로 실행 (보안)
RUN addgroup -S stockfolio && adduser -S stockfolio -G stockfolio
USER stockfolio

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 타임존 설정
ENV TZ=Asia/Seoul

# JVM 최적화 옵션
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
