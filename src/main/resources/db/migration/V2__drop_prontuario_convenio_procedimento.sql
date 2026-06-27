-- Remove o bloco prontuário/convênio/procedimento, que nunca teve UI nem fluxo
-- (endpoints órfãos; tb_prontuarios sempre vazia). As entidades, controllers,
-- services e DTOs correspondentes foram removidos do código.
--
-- Idempotente (IF EXISTS) e seguro: as tabelas não tinham dados.
-- tb_prontuarios é dropada primeiro por referenciar as outras duas via FK.

DROP TABLE IF EXISTS tb_prontuarios CASCADE;
DROP TABLE IF EXISTS tb_convenios CASCADE;
DROP TABLE IF EXISTS tb_procedimentos CASCADE;
