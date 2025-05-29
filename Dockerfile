# 먼저 필요한 패키지를 설치하고 두 스테이지를 합치기
FROM amazoncorretto:17 AS builder

# gradle 설정 및 소스 코드 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 실행 권한 부여 및 gradle 빌드 (테스트는 생략)
RUN chmod +x ./gradlew && ./gradlew bootJar -x test

# 두 번째 빌드 스테이지: 실행용 이미지
FROM amazoncorretto:17

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드 스테이지에서 생성된 JAR 파일을 복사
COPY --from=builder build/libs/*.jar app.jar

# RUNTIME 환경변수 설정
# 기본 프로파일은 prod로 설정 (빌드 시 --build-arg PROFILE=값 으로 변경 가능)
ARG PROFILE=prod

# 컨테이너 시작 시 Spring Boot 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]