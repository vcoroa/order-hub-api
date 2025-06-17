# Order Hub API

Sistema de gestão de pedidos B2B desenvolvido com Spring Boot.

## 📋 Pré-requisitos

Antes de executar o projeto, certifique-se de ter instalado:

- **Docker** (versão 20.0 ou superior)
- **Docker Compose** (versão 2.0 ou superior)
- **Git** (para clonar o repositório)

### Verificando as instalações

```bash
# Verificar Docker
docker --version

# Verificar Docker Compose
docker-compose --version

# Verificar Git
git --version
```

## 🚀 Instruções de Execução

### Passo 1: Clone o repositório

```bash
git clone https://github.com/seu-usuario/order-hub-api.git
cd order-hub-api
```

### Passo 2: Execute a aplicação

```bash
# Comando único para subir toda a aplicação
docker-compose up --build
```

**O que este comando faz:**
- Baixa as imagens Docker necessárias (PostgreSQL, etc.)
- Constrói a imagem da aplicação Spring Boot
- Inicia o banco de dados PostgreSQL
- Aguarda o banco ficar pronto (health check)
- Inicia a aplicação Spring Boot
- Configura a rede entre os containers

### Passo 3: Aguarde a inicialização

A aplicação estará pronta quando você ver no terminal:

```
pedidos-app | Started OrderHubApiApplication in X.XXX seconds
```

### Passo 4: Acesse a aplicação

- **API Direta:** http://localhost:8080/api
- **Health Check:** http://localhost:8080/api/actuator/health
- **Prometheus:** http://localhost:9090
- **pgAdmin:** http://localhost:5050

## 🔍 Verificando se está funcionando

### Teste 1: Health Check
```bash
curl http://localhost:8080/api/actuator/health
```

**Resposta esperada:**
```json
{
  "status": "UP",
  "components": {
    "db":
      {"status": "UP"},
    "diskSpace":
      {"status": "UP"},
    "ping":
      {"status": "UP"},
     "ssl":
      {"status": "UP"}
  }
}
```

### Teste 2: Listar Pedidos
```bash
curl http://localhost:8080/api/pedidos
```

## 🛠️ Comandos Úteis

### Executar em background
```bash
# Subir em background (modo detached)
docker-compose up -d --build

# Ver logs em tempo real
docker-compose logs -f app
```

### Parar a aplicação
```bash
# Parar todos os containers
docker-compose down

# Parar e remover volumes (limpar dados)
docker-compose down -v
```

### Rebuild da aplicação
```bash
# Rebuild apenas a aplicação (após mudanças no código)
docker-compose up --build app

# Rebuild completo
docker-compose down
docker-compose up --build
```

### Ver logs
```bash
# Logs da aplicação
docker-compose logs -f app

# Logs do banco
docker-compose logs -f postgres

# Logs de todos os serviços
docker-compose logs -f
```

### Acessar containers
```bash
# Acessar container da aplicação
docker exec -it pedidos-app bash

# Acessar container do banco
docker exec -it pedidos-postgres psql -U pedidos_user -d pedidos_b2b
```

## 🧪 Testando a API

### Endpoints principais

#### 1. Listar Parceiros
```bash
curl -X GET http://localhost:8080/api/parceiros
```

#### 2. Criar Parceiro
```bash
curl -X POST http://localhost:8080/api/parceiros \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Empresa Teste LTDA",
    "cnpj": "12345678000123",
    "limiteCredito": 50000.00
  }'
```

#### 3. Listar Pedidos
```bash
curl -X GET http://localhost:8080/api/pedidos
```

#### 4. Criar Pedido
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "parceiroPublicId": "PARC_XXXXXXXX",
    "itens": [
      {
        "produto": "Notebook Dell",
        "quantidade": 2,
        "precoUnitario": 2500.00,
        "unidadeMedida": "UN"
      },
      {
        "produto": "Mouse Wireless",
        "quantidade": 5,
        "precoUnitario": 85.00,
        "unidadeMedida": "UN"
      }
    ]
  }'
```

#### 5. Aprovar Pedido
```bash
curl -X PUT http://localhost:8080/api/pedidos/PED_XXXXXXXX/aprovar
```

## 🔧 Configurações de Ambiente

### Portas utilizadas
- **8080:** Spring Boot API (Direto)
- **5432:** PostgreSQL
- **5050:** pgAdmin
- **9090:** Prometheus

### Variáveis de ambiente (opcional)

Você pode sobrescrever configurações editando o `docker-compose.yml`:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pedidos_b2b
  SPRING_DATASOURCE_USERNAME: pedidos_user
  SPRING_DATASOURCE_PASSWORD: pedidos_pass
  SERVER_PORT: 8080
  SPRING_JPA_HIBERNATE_DDL_AUTO: update
```

## ❗ Solução de Problemas

### Erro "Port already in use"
```bash
# Verificar o que está usando a porta 8080
netstat -tulpn | grep 8080

# Parar processo na porta 8080 (se necessário)
sudo kill -9 $(sudo lsof -t -i:8080)
```

### Erro "Cannot connect to database"
```bash
# Verificar se o PostgreSQL está rodando
docker-compose ps

# Verificar logs do banco
docker-compose logs postgres

# Restart apenas o banco
docker-compose restart postgres
```

### Aplicação não inicia
```bash
# Ver logs detalhados
docker-compose logs app

# Verificar se o container foi criado
docker ps -a

# Rebuild completo
docker-compose down
docker system prune -a
docker-compose up --build
```

### Limpar ambiente completamente
```bash
# Parar tudo e limpar
docker-compose down -v
docker system prune -a --volumes

# Subir novamente
docker-compose up --build
```

## 🌐 Acessos Administrativos

### pgAdmin (Administração do Banco)
- **URL:** http://localhost:5050
- **Email:** admin@admin.com
- **Senha:** admin

**Para conectar ao banco pelo pgAdmin:**
1. Acesse http://localhost:5050 e faça login
2. Clique em "Add New Server"
3. **General → Name:** OrderHub DB
4. **Connection:**
    - **Host:** postgres
    - **Port:** 5432
    - **Database:** pedidos_b2b
    - **Username:** pedidos_user
    - **Password:** pedidos_pass

### Conectar diretamente ao PostgreSQL
```bash
# Via Docker
docker exec -it pedidos-postgres psql -U pedidos_user -d pedidos_b2b

# Via cliente externo
psql -h localhost -p 5432 -U pedidos_user -d pedidos_b2b
```

### Prometheus (Métricas)
- **URL:** http://localhost:9090
- **Targets:** Vai mostrar o Spring Boot como target
- **Métricas da API:** Disponíveis automaticamente

## 📊 Monitoramento

### Endpoints de monitoramento
- **Health:** http://localhost:8080/api/actuator/health
- **Info:** http://localhost:8080/api/actuator/info
- **Metrics:** http://localhost:8080/api/actuator/metrics
- **Prometheus Metrics:** http://localhost:8080/api/actuator/prometheus
- **Dashboard Prometheus:** http://localhost:9090

### Verificar status dos containers
```bash
# Ver containers rodando
docker-compose ps

# Ver recursos utilizados
docker stats
```

## 🎯 Validação Final

Para validar que tudo está funcionando:

1. ✅ **API responde:** `curl http://localhost:8080/api/actuator/health`
2. ✅ **Banco conectado:** Health check retorna status UP
3. ✅ **Endpoints funcionando:** `curl http://localhost:8080/api/pedidos`
4. ✅ **pgAdmin acessível:** http://localhost:5050
5. ✅ **Prometheus coletando:** http://localhost:9090/targets

**Pronto! Seu ambiente completo Order Hub API está executando com sucesso! 🚀**

### Arquitetura Final
```
Request → Spring Boot (8080) → PostgreSQL (5432)
                           ↓
                     Prometheus (9090) ← Actuator Metrics
                           ↓
                     pgAdmin (5050) → PostgreSQL
```