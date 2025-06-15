-- Script de inicialização do banco de dados

-- Criar extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Inserir dados iniciais para testes
-- Tabela de parceiros (será criada automaticamente pelo Hibernate)
-- Mas podemos inserir dados iniciais aqui após a criação

-- Função para inserir dados de teste após a criação das tabelas
DO $$
BEGIN
    -- Aguardar a criação das tabelas pelo Hibernate
    -- Este script rodará antes, mas podemos criar dados de exemplo

    -- Criar um usuário para conectar se necessário
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_user') THEN
        CREATE ROLE app_user LOGIN PASSWORD 'app_pass';
        GRANT CONNECT ON DATABASE pedidos_b2b TO app_user;
        GRANT USAGE ON SCHEMA public TO app_user;
        GRANT CREATE ON SCHEMA public TO app_user;
    END IF;

END $$;

-- Configurações de performance para PostgreSQL
ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';
ALTER SYSTEM SET track_activity_query_size = 2048;
ALTER SYSTEM SET pg_stat_statements.track = 'all';
ALTER SYSTEM SET log_statement = 'all';
ALTER SYSTEM SET log_min_duration_statement = 1000;

-- Índices que podem ser úteis (serão criados após as tabelas existirem)
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pedido_parceiro_id ON pedidos(parceiro_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pedido_status ON pedidos(status);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pedido_data_criacao ON pedidos(data_criacao);

-- Configurar timezone
SET timezone = 'America/Sao_Paulo';