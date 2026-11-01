DROP DATABASE IF EXISTS Lubcker;
CREATE DATABASE Lubcker;
USE Lubcker;

-- 1. Clientes y sus datos asociados
CREATE TABLE cliente (
    id_cliente INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL
);

CREATE TABLE direccion (
    id_direccion INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    provincia VARCHAR(100),
    ciudad VARCHAR(100),
    calle VARCHAR(100),
    altura INT,
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

CREATE TABLE usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT UNIQUE NOT NULL,
    nombre_usuario VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    contrasena VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
    foto_perfil VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

-- 2. Productos y catalogación
CREATE TABLE categoria (
    id_categoria INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE producto (
    id_producto INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    precio INT NOT NULL,
    id_categoria INT,
    imagen VARCHAR(255),
    stock INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    -- Venta por medida: precio = por UNA unidad_medida (kg, litro, metro, unidad); stock en esa unidad.
    -- presentaciones = medidas en que se vende, separadas por coma (ej: '3,5,10')
    unidad_medida VARCHAR(20) NOT NULL DEFAULT 'unidad',
    presentaciones VARCHAR(255) NOT NULL DEFAULT '1',
    FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
);

CREATE TABLE ficha_tecnica (
    id_ficha INT PRIMARY KEY AUTO_INCREMENT,
    id_producto INT NOT NULL UNIQUE,
    modelo VARCHAR(50),
    presentacion VARCHAR(100),
    descripcion VARCHAR(255),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- 3. Ordenes, Ventas y Pagos
CREATE TABLE orden (
    id_orden INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    fecha DATE NOT NULL,
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

CREATE TABLE detalle_orden (
    id_detalle INT PRIMARY KEY AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    -- Medida de cada paquete (ej: 5 = 5 kg). precio_unitario es el precio de UN paquete.
    tamano INT NOT NULL DEFAULT 1,
    precio_unitario INT NOT NULL,
    subtotal INT NOT NULL,
    FOREIGN KEY (id_orden) REFERENCES orden(id_orden),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

CREATE TABLE metodo_pago (
    id_metodo INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL
);

CREATE TABLE pago (
    id_pago INT PRIMARY KEY AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_metodo_pago INT,
    estado BOOLEAN,
    monto INT NOT NULL,
    mp_payment_id VARCHAR(255),
    FOREIGN KEY (id_orden) REFERENCES orden(id_orden),
    FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id_metodo)
);

-- 4. Logística
CREATE TABLE empresa_logistica (
    id_empresa INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    sucursal VARCHAR(100) NOT NULL
);

CREATE TABLE envio (
    id_envio INT PRIMARY KEY AUTO_INCREMENT,
    id_orden INT NOT NULL,
    id_direccion INT NOT NULL,
    id_empresa INT NOT NULL,
    estado VARCHAR(50) DEFAULT 'Pendiente',
    FOREIGN KEY (id_orden) REFERENCES orden(id_orden),
    FOREIGN KEY (id_direccion) REFERENCES direccion(id_direccion),
    FOREIGN KEY (id_empresa) REFERENCES empresa_logistica(id_empresa)
);

-- 5. Reseñas
CREATE TABLE resenas (
    id_resenas INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    id_producto INT NOT NULL,
    valoracion INT CHECK (valoracion BETWEEN 1 AND 5),
    descripcion VARCHAR(255),
    UNIQUE (id_cliente, id_producto),
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- Datos del negocio de cada cliente (se piden al registrarse)
CREATE TABLE negocio (
    id_negocio INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    rubro VARCHAR(100) NOT NULL,
    provincia VARCHAR(100) NOT NULL,
    ciudad VARCHAR(100) NOT NULL,
    calle VARCHAR(100) NOT NULL,
    altura INT NOT NULL,
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

-- Imagenes del carrusel de la pagina principal (las carga el admin)
CREATE TABLE banner (
    id_banner INT PRIMARY KEY AUTO_INCREMENT,
    imagen VARCHAR(255) NOT NULL,
    titulo VARCHAR(100),
    enlace VARCHAR(255),
    orden INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Materia prima (solo la ve el admin). Insumo = lo que se compra; producto_insumo = la "receta":
-- cuanto de cada insumo lleva UNA unidad del producto (1 kg, 1 litro, 1 unidad).
-- El costo de fabricacion se calcula en el momento con el costo actual de cada insumo.
CREATE TABLE insumo (
    id_insumo INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    unidad_medida VARCHAR(20) NOT NULL,
    costo_unitario DECIMAL(14,2) NOT NULL,
    proveedor VARCHAR(100)
);

CREATE TABLE producto_insumo (
    id_producto_insumo INT PRIMARY KEY AUTO_INCREMENT,
    id_producto INT NOT NULL,
    id_insumo INT NOT NULL,
    cantidad DECIMAL(14,4) NOT NULL,
    UNIQUE (id_producto, id_insumo),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
    FOREIGN KEY (id_insumo) REFERENCES insumo(id_insumo)
);

-- Productos marcados como favoritos por cada cliente
CREATE TABLE favorito (
    id_favorito INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    id_producto INT NOT NULL,
    UNIQUE (id_cliente, id_producto),
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
    FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
