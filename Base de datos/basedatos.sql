drop database if exists E;
CREATE DATABASE E;
use E;

CREATE TABLE cliente (
    id_cliente INT PRIMARY KEY,
    nombre VARCHAR(100),
    apellido VARCHAR(100)
);

CREATE TABLE usuario (
    id_usuario INT PRIMARY KEY,
    id_cliente INT,
    nombre_usuario VARCHAR(100),
    email VARCHAR(100),
    contrasena VARCHAR(100),
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

CREATE TABLE direccion (
    id_direccion INT PRIMARY KEY,
    id_cliente INT,
    ciudad VARCHAR(100),
    calle VARCHAR(100),
    provincia VARCHAR(100),
    FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

CREATE TABLE categoria (
id_categoria INT PRIMARY KEY,
nombre VARCHAR(100)
);

CREATE TABLE producto (
id_producto INT PRIMARY KEY,
nombre VARCHAR(100),
email VARCHAR(100),
precio INT,
id_categoria INT,
FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
);

CREATE TABLE ficha_tecnica (
id_ficha INT PRIMARY KEY,
id_producto INT,
descripcion VARCHAR(255),
FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
CREATE TABLE proveedor (
id_proveedor INT PRIMARY KEY,
nombre VARCHAR(100)
);

CREATE TABLE producto_proveedor (
id_producto INT,
id_proveedor INT,
PRIMARY KEY (id_producto, id_proveedor),
FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor)
);
CREATE TABLE orden (
id_orden INT PRIMARY KEY,
id_cliente INT,
fecha VARCHAR(50),
FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
);

CREATE TABLE detalle_orden (
id_detalle INT PRIMARY KEY,
id_orden INT,
id_producto INT,
cantidad INT,
FOREIGN KEY (id_orden) REFERENCES orden(id_orden),
FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
CREATE TABLE empresa_logistica (
id_empresa INT PRIMARY KEY,
nombre VARCHAR(100)
);

CREATE TABLE envio (
id_envio INT PRIMARY KEY,
id_orden INT,
id_direccion INT,
estado VARCHAR(50),
direccion VARCHAR(100),
id_empresa INT,
FOREIGN KEY (id_orden) REFERENCES orden(id_orden),
FOREIGN KEY (id_empresa) REFERENCES empresa_logistica(id_empresa),
Foreign key (id_direccion) REFERENCES direccion(id_direccion)
);
CREATE TABLE metodo_pago (
id_metodo INT PRIMARY KEY,
nombre VARCHAR(50)
);

CREATE TABLE pago (
id_pago INT PRIMARY KEY,
id_orden INT,
monto INT,
id_metodo_pago INT,
FOREIGN KEY (id_orden) REFERENCES orden(id_orden),
FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago(id_metodo)
);

Create table resenas (
id_resenas int primary key,
id_cliente int,
id_producto int,
valoracion int,
descripcion VARCHAR(255),
foreign key (id_cliente) REFERENCES cliente(id_cliente),
foreign key (id_producto) references producto(id_producto)
);