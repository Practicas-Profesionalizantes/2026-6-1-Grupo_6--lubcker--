# Backend de Lubcker (Spring Boot). Se construye en dos etapas: primero se compila, despues
# se arma una imagen liviana solo con el .jar.
#
# Construir:  docker build -t lubcker-backend .      (desde la carpeta ecommerce)
# La configuracion (base de datos, claves, etc.) entra por variables de entorno: ver ../docs/DEPLOY.md

# ---------- 1) Compilar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn
# El script puede traer saltos de linea de Windows (CRLF); en Linux eso lo rompe
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Las dependencias se bajan en una capa aparte: si solo cambia el codigo, no se vuelven a bajar
RUN ./mvnw -q dependency:go-offline

COPY src src
# Los tests usan la base de datos real: no se corren al construir la imagen
RUN ./mvnw -q package -DskipTests

# ---------- 2) Imagen final ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

# Imagenes de productos que ya vienen en el repositorio. Se copian al volumen "uploads" al arrancar
# (sin pisar las que ya existan) porque un volumen vacio taparia lo que traiga la imagen.
COPY uploads /app/uploads-semilla
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh && mkdir -p /app/uploads

# Tope de memoria de Java (se puede cambiar con la variable JAVA_TOOL_OPTIONS)
ENV JAVA_TOOL_OPTIONS="-Xmx512m"

EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
