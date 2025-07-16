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

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy

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

# 애플리케이션 포트 노출
EXPOSE 8080

# JVM 최적화 옵션 설정
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV DB_URL=jdbc:mysql://localhost:3306/mait
ENV DB_USERNAME=sa
ENV DB_PASSWORD=

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"] 
