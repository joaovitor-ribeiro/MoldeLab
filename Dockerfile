# Imagem ??nica para o Cloud Run: o backend Spring Boot serve a API (/api) e o frontend (build do Vite).
# Build:  docker build -t moldelab .
# Rodar:  docker run -p 8080:8080 -e DATABASE_URL=... -e DATABASE_USERNAME=... -e DATABASE_PASSWORD=... moldelab

# 1) frontend
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) backend (com o frontend embutido em /static)
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/dist src/main/resources/static
RUN ./mvnw -q -B package -DskipTests

# 3) runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 moldelab && mkdir -p /app/data && chown moldelab /app/data
COPY --from=backend /app/target/moldelab-backend-*.jar app.jar
USER moldelab
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
