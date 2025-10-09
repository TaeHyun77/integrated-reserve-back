# Java 17 환경 사용
FROM amazoncorretto:17-alpine

# 컨테이너 내 작업 디렉토리
WORKDIR /app

# 빌드 결과물 복사
COPY build/libs/kotlin-0.0.1-SNAPSHOT.jar intergrated-reserve.jar

# 애플리케이션 실행
CMD ["java", "-jar", "intergrated-reserve.jar"]