-- Corrige o drift de schema acumulado por spring.jpa.hibernate.ddl-auto=update,
-- que cria colunas/constraints mas nunca as remove. Resolve as issues #3 e #6.
--
-- Idempotente: em bancos novos (tabela/coluna ainda inexistente) cada comando é
-- um no-op graças a IF EXISTS, então a migração roda com segurança em qualquer
-- ambiente (dev antigo, dev novo ou CI).

-- Issue #3: paciente se cadastra apenas com login+senha; a entidade Usuario
-- mapeia "email" como nullable, mas schemas antigos mantêm NOT NULL.
ALTER TABLE IF EXISTS tb_users
    ALTER COLUMN email DROP NOT NULL;

-- Issue #6: a coluna "duracao_consulta_minutos" deixou de ser mapeada por
-- DisponibilidadeMedico no Fase D (a duração vem de Usuario). A coluna órfã
-- NOT NULL persiste em bancos antigos e quebra o INSERT de disponibilidade.
ALTER TABLE IF EXISTS tb_disponibilidade_medico
    DROP COLUMN IF EXISTS duracao_consulta_minutos;
