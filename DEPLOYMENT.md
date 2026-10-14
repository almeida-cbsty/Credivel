# Implantação do Credível

## 1. Escopo e requisitos

Este guia descreve uma instalação Linux com systemd e Nginx nos caminhos usados pelo projeto. Os comandos são instruções e não foram executados durante a documentação. Operações em `/etc`, criação de contas, alteração de serviços e instalação de pacotes exigem privilégios administrativos.

Requisitos: Java 21, Node compatível com `^20.19.0 || >=22.12.0`, npm, Maven ou wrapper, Nginx e MySQL para o modo principal. Para o perfil `demo`, utiliza-se H2 em ficheiro e não é necessário servidor MySQL.

Portas do modelo: backend 8083, frontend Nginx 8084, MySQL local 3306. O serviço externo de publicações usa HTTP na porta 8081 de `72.62.234.76`.

O backend atual não autentica operações CRUD. Para exposição pública, implemente autorização no servidor ou restrinja administrativamente o acesso à API de escrita. O modelo de implantação não fornece autenticação nem TLS.

## 2. Compilar

```bash
cd /opt/credivel/frontend
npm ci
npm run build
```

```bash
cd /opt/credivel/backend
./mvnw package
```

Se o wrapper estiver ausente, utilize `mvn package`: os ficheiros do wrapper são ignorados pelo Git. Saídas esperadas: `frontend/dist` e `backend/target/credivel-api-1.0.0.jar`.

Para utilizar logótipo próprio, coloque `logo.png` em `frontend/public` antes da compilação. Sem ele, a interface apresenta a marca alternativa CSS.

## 3. Conta e permissões

Crie uma conta de serviço dedicada, se ainda não existir:

```bash
sudo useradd --system --user-group --home-dir /opt/credivel --no-create-home --shell /usr/sbin/nologin credivel
sudo install -d -m 750 /etc/credivel
```

A conta `credivel` precisa de leitura do JAR e acesso ao diretório backend. O utilizador do Nginx precisa de leitura de `frontend/dist` e de travessia dos diretórios superiores. Para H2, apenas o diretório de dados deve ser gravável pelo serviço; não é necessário tornar todo o projeto gravável.

## 4. Selecionar banco

### 4.1 MySQL

Execute como administrador do MySQL, substituindo a senha antes de utilizar:

```sql
CREATE DATABASE credivel_db CHARACTER SET utf8mb4;
CREATE USER 'credivel'@'127.0.0.1' IDENTIFIED BY 'SUBSTITUIR_POR_SENHA_FORTE';
GRANT ALL PRIVILEGES ON credivel_db.* TO 'credivel'@'127.0.0.1';
```

O grant é limitado ao banco dedicado e permite alterações de esquema usadas por `ddl-auto=update`. Ajuste privilégios se adotar migrações executadas por outra conta.

Crie `/etc/credivel/credivel.env` com valores reais:

```ini
CREDIVEL_DB_URL=jdbc:mysql://127.0.0.1:3306/credivel_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
CREDIVEL_DB_USERNAME=credivel
CREDIVEL_DB_PASSWORD=SUBSTITUIR_POR_SENHA_FORTE
CREDIVEL_CORS_ORIGINS=http://localhost:8084,http://127.0.0.1:8084
SERVER_PORT=8083
```

Proteja-o:

```bash
sudo chown root:root /etc/credivel/credivel.env
sudo chmod 600 /etc/credivel/credivel.env
```

Esse ficheiro é lido pelo systemd; não é automaticamente carregado por um `java -jar` manual. Sintaxe é de EnvironmentFile, sem `export`. Configure a URL conforme a segurança da sua ligação; os parâmetros acima reproduzem o padrão local.

**Remova `Environment=SPRING_PROFILES_ACTIVE=demo` da cópia instalada da unidade para usar MySQL.** O modelo em `deploy/credivel.service` ativa H2 explicitamente. Definir apenas `CREDIVEL_DB_*` não elimina essa sobreposição de perfil.

### 4.2 H2 demonstrativo

Mantenha `Environment=SPRING_PROFILES_ACTIVE=demo` na unidade. Prepare o diretório:

```bash
sudo install -d -o credivel -g credivel -m 750 /opt/credivel/data
```

O ficheiro `credivel.env` continua obrigatório para a unidade fornecida; pode conter `SERVER_PORT=8083`. O perfil define utilizador `sa`, senha vazia, ficheiro `/opt/credivel/data/credivel` e CORS para localhost/127.0.0.1 na porta 8084.

Para outra origem no perfil demo, sobreponha diretamente a propriedade Spring, por exemplo com `CREDIVEL_CORS_ALLOWED_ORIGINS=https://SEU_DOMINIO`, ou com argumento `--credivel.cors.allowed-origins=...` no arranque. `CREDIVEL_CORS_ORIGINS` alimenta o placeholder da configuração padrão, mas o perfil demo define a propriedade com valor literal.

O perfil demo é persistente em ficheiro, não em memória. Não execute duas instâncias contra o mesmo ficheiro. A migração H2 para MySQL não é automática: envolve exportação, transformação e validação dos dados.

## 5. Instalar serviço

```bash
sudo cp /opt/credivel/deploy/credivel.service /etc/systemd/system/credivel.service
```

Edite a cópia instalada para selecionar o perfil conforme a secção anterior. Confirme os caminhos do Java e do JAR. A unidade usa `User=credivel`, `Group=credivel`, `WorkingDirectory=/opt/credivel/backend`, reinício em falha, `NoNewPrivileges=true` e `PrivateTmp=true`.

Depois da configuração:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now credivel
sudo systemctl status credivel --no-pager
sudo journalctl -u credivel -n 100 --no-pager
```

Um banco vazio recebe três artigos fictícios no primeiro arranque, inclusive em MySQL. Verifique o conteúdo antes de disponibilizar a aplicação.

## 6. Configurar Nginx

Em distribuições com `sites-available` e `sites-enabled`:

```bash
sudo cp /opt/credivel/deploy/nginx-credivel.conf /etc/nginx/sites-available/credivel
sudo ln -s /etc/nginx/sites-available/credivel /etc/nginx/sites-enabled/credivel
sudo nginx -t
sudo systemctl reload nginx
```

Crie a ligação apenas se não existir. Outras distribuições podem exigir configuração em `conf.d`.

O modelo usa `root /opt/credivel/frontend/dist`, `listen 8084`, `server_name _`, limite de corpo `6m`, proxy `/api/` para `127.0.0.1:8083` e fallback `try_files` para `index.html`.

Ao mudar `SERVER_PORT`, altere também o upstream. Configure domínio, certificado TLS, firewall e encaminhamento de rede segundo o ambiente. O código não define `server.address`; não assuma que a API fica restrita a loopback. Se a entrada externa deve passar apenas pelo Nginx, configure essa restrição explicitamente.

Antes de ativar HTTPS, adapte as URLs HTTP da integração de publicações para uma origem HTTPS ou outro encaminhamento compatível. A aplicação atual pode sofrer bloqueio de conteúdo misto.

## 7. Verificar implantação

```bash
curl --fail-with-body 'http://127.0.0.1:8083/api/news?page=0&size=1'
curl --fail-with-body 'http://127.0.0.1:8084/api/news?page=0&size=1'
curl --fail-with-body 'http://127.0.0.1:8084/'
```

Confirme no navegador: página inicial, detalhe aberto diretamente por URL, imagens, pesquisa, login demonstrativo, carregamento adicional e publicações externas. Consulte consola e painel de rede para CORS, media e erros de integração.

Não há healthcheck Actuator configurado. O GET da lista verifica leitura da API e do banco sem incrementar visualizações.

## 8. Atualização e recuperação

Antes de atualizar, guarde backup consistente do banco, assets próprios e configuração externa. Compile a nova versão, substitua os artefactos, reinicie `credivel`, valide API e interface e consulte logs. O sistema não fornece publicação sem interrupção.

Uma versão anterior do JAR pode não ser compatível com um esquema alterado por `ddl-auto=update`; rollback deve considerar banco e código. Consulte [o manual](docs/MANUAL_DO_SISTEMA.md) para backup, diagnóstico e verificação funcional.

Não há automação de certificados, backup, migrações, monitorização ou pipeline CI/CD incluída no projeto.
