# 첫 번째 빌드 스테이지: 빌드용
FROM amazoncorretto:17 AS builder

WORKDIR /build

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Gradle 래퍼와 설정 파일들만 먼저 복사 (의존성 캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여 및 의존성 다운로드 (소스코드 변경과 무관하게 캐시됨)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 (이 시점에서만 소스 변경이 빌드에 영향)
COPY src src

# gradle 빌드 (테스트는 생략)
RUN ./gradlew bootJar -x test --no-daemon

# 두 번째 빌드 스테이지: 실행용 이미지
FROM amazoncorretto:17

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드 스테이지에서 생성된 JAR 파일을 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 컨테이너 시작 시 Spring Boot 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]