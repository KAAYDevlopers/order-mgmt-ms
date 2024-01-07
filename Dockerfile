FROM amazoncorretto:17
COPY target/*.jar order-mgmt-ms-0.0.1.jar
EXPOSE 8089
ENTRYPOINT ["java","-Dspring.profiles.active=dev", "-jar", "order-mgmt-ms-0.0.1.jar"]
ENV SPRING_CONFIG_LOCATION=file:/app/config/
