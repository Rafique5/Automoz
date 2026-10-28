CREATE DATABASE IF NOT EXISTS automoz_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE automoz_db;

CREATE TABLE IF NOT EXISTS veiculos (
  id INT PRIMARY KEY,
  marca VARCHAR(100) NOT NULL,
  modelo VARCHAR(100) NOT NULL,
  preco DECIMAL(12,2) NOT NULL,
  tipo ENUM('VENDA','ALUGUER') NOT NULL,
  estado ENUM('DISPONIVEL','RESERVADO','ALUGADO','VENDIDO') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS clientes (
  id VARCHAR(32) PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  email VARCHAR(190) NOT NULL,
  contacto CHAR(9) NOT NULL,
  carta_conducao VARCHAR(100) NOT NULL DEFAULT '',
  intencao ENUM('VENDA','ALUGUER') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservas (
  id VARCHAR(32) PRIMARY KEY,
  cliente_id VARCHAR(32) NOT NULL,
  veiculo_id INT NOT NULL,
  data_reserva DATETIME NOT NULL,
  data_expiracao DATETIME NOT NULL,
  estado ENUM('ACTIVA','EXPIRADA','CONFIRMADA','CANCELADA') NOT NULL,
  CONSTRAINT fk_reserva_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT fk_reserva_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id),
  INDEX idx_reserva_estado (estado)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS contratos (
  id VARCHAR(32) PRIMARY KEY,
  reserva_id VARCHAR(32) NOT NULL UNIQUE,
  cliente_id VARCHAR(32) NOT NULL,
  veiculo_id INT NOT NULL,
  tipo ENUM('VENDA','ALUGUER') NOT NULL,
  data_assinatura DATETIME NOT NULL,
  numero_meses INT NULL COMMENT 'Campo mantido para contratos antigos',
  periodo ENUM('UM_DIA','DOIS_DIAS','SEMANAL','MENSAL') NULL,
  data_inicio DATETIME NULL,
  mensalidade DECIMAL(12,2) NULL,
  data_fim DATETIME NULL,
  assinatura_digital_base64 LONGTEXT NULL,
  imagem_assinatura LONGBLOB NULL,
  CONSTRAINT fk_contrato_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id),
  CONSTRAINT fk_contrato_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT fk_contrato_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id)
) ENGINE=InnoDB;
