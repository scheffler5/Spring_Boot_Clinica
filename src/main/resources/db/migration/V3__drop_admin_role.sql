-- O papel ADMIN foi removido do enum UserRole: não protegia mais nenhum endpoint
-- exclusivo (era idêntico a MEDIC, do qual apenas herdava permissões).
--
-- Como role é persistido como texto (@Enumerated(STRING)), qualquer linha antiga
-- com role='ADMIN' quebraria o carregamento da entidade. Esta migração converte
-- esses registros em MEDIC (sem efeito prático, pois ADMIN só herdava MEDIC).
-- Idempotente: em bancos sem ADMIN, não altera nada. Em bancos novos (o Flyway
-- roda antes do Hibernate criar o schema), tb_users ainda não existe, então o
-- UPDATE é envolvido por uma guarda que o torna um no-op nesse cenário.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'tb_users'
    ) THEN
        UPDATE tb_users SET role = 'MEDIC' WHERE role = 'ADMIN';
    END IF;
END $$;
