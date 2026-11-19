# 멀티스테이지 빌드를 위한 Dockerfile

# 1단계: 빌드 스테이지
FROM eclipse-temurin:21-jdk-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼 파일과 설정 파일 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY config/ config/

# 실행 권한 부여
RUN chmod +x gradlew

# 의존성 다운로드를 위한 캐시 레이어
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src/ src/

# 애플리케이션 빌드
RUN ./gradlew clean bootJar --no-daemon

# 공식 Jammy 이미지에 CVE-2026-114938 패치가 반영될 때까지
# 개발·운영 배포 대상인 amd64에는 Adoptium의 패치 JRE를 설치한다.
FROM eclipse-temurin:21.0.13_9-jre-jammy AS runtime-amd64
ENV JAVA_VERSION=jdk-21.0.13.1+2
RUN curl --fail --location --silent --show-error --retry 3 \
        'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13.1%2B2/OpenJDK21U-jre_x64_linux_hotspot_21.0.13.1_2.tar.gz' \
        --output /tmp/temurin-jre.tar.gz \
    && echo '18f44fab9065217f3ba61d0eef8a7b5f3c3713aa7293001b0d4ac10ede3850f0  /tmp/temurin-jre.tar.gz' | sha256sum --check - \
    && rm -rf "$JAVA_HOME" && mkdir -p "$JAVA_HOME" \
    && tar -xzf /tmp/temurin-jre.tar.gz -C "$JAVA_HOME" --strip-components=1 --no-same-owner \
    && rm /tmp/temurin-jre.tar.gz \
    && java -version

FROM eclipse-temurin:21-jre-jammy AS runtime-arm64
FROM eclipse-temurin:21-jre-jammy AS runtime-ppc64le
FROM eclipse-temurin:21-jre-jammy AS runtime-s390x

FROM runtime-${TARGETARCH} AS app

# 애플리케이션 실행을 위한 사용자 생성
RUN groupadd -r mait && useradd -r -g mait mait

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 파일 소유권 변경
RUN chown -R mait:mait /app

# 사용자 변경
USER mait

EXPOSE 8080

# JVM 최적화 옵션 설정
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

ENV SPRING_PROFILES_ACTIVE=
ENV JPA_DDL_AUTO=none
# DB 관련 환경변수는 docker-compose.yml에서 주입 필요
ENV DB_URL=
ENV DB_USERNAME=
ENV DB_PASSWORD=

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"] 
