-- O papel ADMIN foi removido do enum UserRole: não protegia mais nenhum endpoint
-- exclusivo (era idêntico a MEDIC, do qual apenas herdava permissões).
--
-- Como role é persistido como texto (@Enumerated(STRING)), qualquer linha antiga
-- com role='ADMIN' quebraria o carregamento da entidade. Esta migração converte
-- esses registros em MEDIC (sem efeito prático, pois ADMIN só herdava MEDIC).
-- Idempotente: em bancos sem ADMIN, não altera nada.

UPDATE tb_users SET role = 'MEDIC' WHERE role = 'ADMIN';
