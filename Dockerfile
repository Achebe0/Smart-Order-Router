# --- Stage 1: Build the frontend ---
FROM node:18 as frontend-builder
WORKDIR /app/FluxFront

COPY FluxFront/package.json FluxFront/package-lock.json ./
RUN npm install

COPY FluxFront/ ./
RUN npm run build


# --- Stage 2: Build the backend ---
FROM eclipse-temurin:17-jdk-jammy as backend-builder
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src

# Copy built frontend into Spring static folder
COPY --from=frontend-builder /app/FluxFront/dist ./src/main/resources/static

RUN ./mvnw clean package -DskipTests


# --- Stage 3: Run the app ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=backend-builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]