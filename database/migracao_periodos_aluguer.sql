USE automoz_db;

-- Execute uma única vez numa base criada com a versão anterior do Automoz.
ALTER TABLE contratos
  ADD COLUMN periodo ENUM('UM_DIA','DOIS_DIAS','SEMANAL','MENSAL') NULL AFTER numero_meses,
  ADD COLUMN data_inicio DATETIME NULL AFTER periodo;

-- Mantém a duração e o valor dos contratos antigos através do campo numero_meses.
-- Define data_inicio para tornar esses registos legíveis pela versão nova.
UPDATE contratos
SET data_inicio = data_assinatura
WHERE tipo = 'ALUGUER' AND data_inicio IS NULL;
