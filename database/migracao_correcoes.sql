USE automoz_db;

-- Execute uma vez na base existente, depois de fazer uma cópia de segurança.
-- Preserva a coluna intencao antiga como nullable; a aplicação já não a utiliza.
ALTER TABLE clientes MODIFY intencao ENUM('VENDA','ALUGUER') NULL DEFAULT NULL;
ALTER TABLE clientes ADD UNIQUE KEY uq_clientes_email (email);

ALTER TABLE reservas
  ADD COLUMN data_inicio_utilizacao DATETIME NULL AFTER data_expiracao,
  ADD COLUMN data_fim_utilizacao DATETIME NULL AFTER data_inicio_utilizacao,
  ADD COLUMN periodo ENUM('UM_DIA','DOIS_DIAS','SEMANAL','MENSAL') NULL AFTER data_fim_utilizacao;

-- Conserva os intervalos dos contratos de aluguer já formalizados.
UPDATE reservas r
JOIN contratos c ON c.reserva_id = r.id AND c.tipo = 'ALUGUER'
SET r.data_inicio_utilizacao = COALESCE(c.data_inicio, c.data_assinatura),
    r.data_fim_utilizacao = c.data_fim,
    r.periodo = COALESCE(c.periodo, 'MENSAL');

-- Reservas antigas ainda sem contrato recebem um intervalo mensal a partir da criação.
UPDATE reservas r
JOIN veiculos v ON v.id = r.veiculo_id AND v.tipo = 'ALUGUER'
SET r.data_inicio_utilizacao = r.data_reserva,
    r.data_fim_utilizacao = DATE_ADD(r.data_reserva, INTERVAL 1 MONTH),
    r.periodo = 'MENSAL'
WHERE r.data_inicio_utilizacao IS NULL;

CREATE TABLE IF NOT EXISTS funcionarios (
  utilizador VARCHAR(80) PRIMARY KEY,
  sal_base64 VARCHAR(64) NOT NULL,
  hash_senha_base64 VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS automoz_sequencias (
  entidade VARCHAR(32) PRIMARY KEY,
  proximo BIGINT NOT NULL
) ENGINE=InnoDB;
