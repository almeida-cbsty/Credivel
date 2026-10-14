# Manual do sistema Credível

## 1. Público e alcance

Este manual destina-se a leitores, editores, programadores e operadores. Foi elaborado a partir do código local em 14/10/2026. Comandos são instruções de execução, não confirmação de que a instalação, o banco ou os serviços estejam ativos. Consulte a [descrição técnica](DESCRICAO_TECNICA.md), a [API](API.md) e a [implantação](../DEPLOYMENT.md).

## 2. Utilização pelo leitor

1. Abra a página inicial para consultar notícias publicadas.
2. Use as setas ou indicadores do carrossel para mudar o destaque; a mudança automática ocorre a cada cinco segundos.
3. Escreva no campo de pesquisa para filtrar título, resumo e categoria dos artigos já carregados. Use o botão de limpar para voltar à listagem.
4. Clique no título, imagem do cartão ou ligação de leitura para abrir o artigo completo.
5. Consulte a classificação, fonte, autor e data antes de interpretar o conteúdo. A classificação é um registo editorial.
6. Use Carregar mais notícias quando disponível. O acesso demonstrativo será pedido se não existir sessão local.

A pesquisa não cobre automaticamente toda a base: carregar mais artigos antes de pesquisar amplia o conjunto local. Não há filtros de categoria ou classificação no servidor.

## 3. Publicações

A secção Publicações carrega até 50 registos de uma API externa, exclui vagas e concursos e mostra inicialmente seis itens. Carregar mais revela outros itens já recebidos; não faz paginação remota adicional.

Os filtros disponíveis são Todas, Vídeos e Imagens. O filtro de imagens tem divergência entre `IMAGEM` e `IMAGE` no código e depende do formato externo. Textos longos podem ser expandidos com Ver mais. Vídeos utilizam controlos do navegador.

Gostos são temporários, comentários mostram apenas quantidade e partilha copia o endereço da página atual. Não existe formulário de publicação implementado, apesar do texto da barra lateral sugerir entrada na conta.

## 4. Entrada e saída

Conta demonstrativa: email `user@credivel.com`, senha `user123`. A sessão fica no navegador, em `localStorage`, sem expiração. Isso não é uma conta validada no servidor.

Não existe botão de saída na interface atual. Para terminar a sessão local, pode remover a chave `credivel.session` nas ferramentas de armazenamento do navegador ou executar na consola:

```javascript
localStorage.removeItem('credivel.session');
location.reload();
```

Cada navegador ou origem tem armazenamento próprio; entrar em localhost não cria sessão em 127.0.0.1 ou em outro domínio.

## 5. Trabalho editorial

Não há painel administrativo. A edição é feita por cliente HTTP conforme [API.md](API.md). O fluxo possível é criar com `DRAFT`, guardar o ID da resposta, revisar o conteúdo e atualizar com `PUBLISHED`.

Para atualizar, reenvie todos os campos obrigatórios. Preserve `slug` e `publishedAt` se desejar conservar URL e data. Para retirar um artigo do público, atualize-o para `DRAFT`; para apagar definitivamente, use DELETE. Rascunhos não podem ser consultados pelos GET públicos: mantenha o conteúdo no processo editorial externo.

As classificações aceites são Credível e Falso; não existem estados de revisão, aprovações, histórico, anexos de evidência ou rastreio de quem alterou. A API de escrita atualmente não é autenticada.

## 6. Preparação para desenvolvimento

Requisitos: Java 21, Maven ou wrapper disponível, Node compatível com `^20.19.0 || >=22.12.0`, npm e um banco MySQL acessível ou diretório H2 gravável.

Verificação de ferramentas:

```bash
java -version
node --version
npm --version
```

O wrapper está presente localmente, mas `mvnw` e `.mvn` constam do `.gitignore`; uma cópia nova do repositório pode não os conter. Nesse caso utilize Maven instalado, substituindo `./mvnw` por `mvn`.

### 6.1 API com MySQL

Prepare o banco conforme `DEPLOYMENT.md`. No terminal do backend configure:

```bash
cd /opt/credivel/backend
export CREDIVEL_DB_URL='jdbc:mysql://127.0.0.1:3306/credivel_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export CREDIVEL_DB_USERNAME='credivel'
export CREDIVEL_DB_PASSWORD='SUBSTITUIR_PELA_SENHA_LOCAL'
export CREDIVEL_CORS_ORIGINS='http://localhost:5173,http://127.0.0.1:5173'
./mvnw spring-boot:run
```

Execute sem perfil `demo` para usar esses valores. Evite guardar credenciais reais no Git ou em ficheiros partilhados.

### 6.2 API demonstrativa

O caminho H2 padrão é absoluto e precisa de permissão de escrita. Para utilizar um ficheiro temporário no desenvolvimento:

```bash
cd /opt/credivel/backend
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=demo \
  '-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:file:/tmp/credivel-dev;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH --credivel.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173'
```

Esse comando sobrepõe caminho e CORS do perfil. Não utilize o mesmo ficheiro H2 em duas instâncias simultâneas. O primeiro arranque de uma base vazia cria amostras fictícias.

### 6.3 Frontend

```bash
cd /opt/credivel/frontend
npm ci
npm run dev
```

O código usa `/api/news` relativo à origem. Não há proxy Vite configurado no projeto. Abrir apenas o servidor Vite não encaminha automaticamente os pedidos para 8083; CORS, por si só, não faz esse encaminhamento.

Para o modo de desenvolvimento com API, configure um proxy `/api` no Vite ou um proxy reverso que una frontend e backend. Exemplo de configuração sugerida, ausente do código atual:

```javascript
// frontend/vite.config.js
import { defineConfig } from 'vite';
export default defineConfig({
  server: {
    proxy: { '/api': 'http://127.0.0.1:8083' }
  }
});
```

Alternativamente compile o frontend e use o Nginx de exemplo, que já implementa o encaminhamento. `npm run preview` também não tem proxy API definido no projeto.

## 7. Compilação e validação

Frontend:

```bash
cd /opt/credivel/frontend
npm ci
npm run build
```

Backend:

```bash
cd /opt/credivel/backend
./mvnw test
./mvnw package
```

Artefactos esperados: `frontend/dist` e `backend/target/credivel-api-1.0.0.jar`. Não foram encontrados testes próprios em `src/test`; existe a dependência Spring Boot Test. Não há script frontend de testes ou lint. Um build bem-sucedido não comprova funcionalidades, segurança ou integração.

Roteiro funcional recomendado num ambiente isolado:

| Verificação | Resultado esperado |
|---|---|
| GET de página válida | 200 com `content` e `last` |
| Página negativa ou tamanho 51 | 400 |
| Criar rascunho | 201 com ID |
| Consultar rascunho | 404 |
| Publicar com PUT completo | 200 e inclusão no feed |
| Consultar artigo | Contador incrementado |
| Atualizar conservando data e slug | URL e data mantidas |
| Apagar registo de teste | 204 e posterior 404 |
| Abrir diretamente rota de detalhe | SPA servida pelo Nginx |
| Pesquisa acentuada | Correspondência local normalizada |
| Sessão demonstrativa | Botão de carregar mais deixa de redirecionar |
| Feed externo | Carregamento, media e comportamento dos filtros |

O quadro é uma proposta de verificação, não um relatório de execução.

## 8. Configuração

| Chave | Função |
|---|---|
| `SERVER_PORT` | Porta API, padrão 8083 |
| `CREDIVEL_DB_URL` | URL JDBC padrão MySQL |
| `CREDIVEL_DB_USERNAME` | Utilizador MySQL |
| `CREDIVEL_DB_PASSWORD` | Senha MySQL |
| `CREDIVEL_CORS_ORIGINS` | Lista de origens separadas por vírgula, sem espaços |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring; `demo` seleciona H2 |

Não há variáveis frontend para API externa, host de media ou identidade. Mudanças nesses valores exigem editar o código e reconstruir o frontend. Alterar ficheiros em `public` também exige nova compilação para atualizar `dist`.

## 9. Operação

Depois de instalado conforme o guia de implantação:

```bash
systemctl status credivel --no-pager
journalctl -u credivel -n 100 --no-pager
nginx -t
curl --fail-with-body 'http://127.0.0.1:8083/api/news?page=0&size=1'
curl --fail-with-body 'http://127.0.0.1:8084/api/news?page=0&size=1'
```

O GET de listagem não incrementa visualizações. Não existe `/actuator/health` configurado. Logs da aplicação vão normalmente para o journal quando executada por systemd; logs Nginx dependem da configuração global do servidor.

Atualização de versão: prepare backup, compile os artefactos, coloque-os nos caminhos esperados, verifique permissões, reinicie a API e valide a página e a API. O backend pode alterar esquema no arranque por `ddl-auto=update`. Não há mecanismo de atualização sem interrupção ou CI/CD no projeto.

## 10. Backup e recuperação

Não existe script automático de backup no projeto. O plano deve incluir banco ativo, imagens próprias de `public`, configuração externa protegida e artefactos necessários para reverter uma versão.

Para MySQL, use uma conta de backup e um ficheiro de credenciais com acesso restrito. Exemplo de exportação, após preparar esse ficheiro:

```bash
mysqldump --defaults-extra-file=/CAMINHO/SEGURO/mysql-backup.cnf \
  --single-transaction --default-character-set=utf8mb4 \
  credivel_db > /CAMINHO/DE/BACKUP/credivel_db.sql
```

Os caminhos são placeholders. Valide espaço, código de saída e restauro numa base de teste. O dump cobre o banco local, não publicações ou ficheiros pertencentes ao serviço externo.

Para H2 em ficheiro, prefira cópia consistente com a aplicação parada, incluindo os ficheiros do banco em `data`. Uma cópia simples de ficheiro enquanto o banco está em uso não garante consistência. Teste o restauro com uma instância isolada e a mesma versão de dependências.

Rollback de JAR não reverte alterações de esquema ou dados. Defina recuperação do banco e retenção de backups antes de alterações de produção.

## 11. Diagnóstico

| Sintoma | Causa possível e verificação |
|---|---|
| API não inicia | Java incompatível, banco inacessível, senha ou env ausente; consultar journal |
| Serviço não usa MySQL | `SPRING_PROFILES_ACTIVE=demo` na unidade fornecida |
| Erro de ficheiro H2 | Diretório sem escrita, ficheiro bloqueado ou outra instância |
| Nginx retorna 502 | API parada ou porta diferente de 8083 |
| Frontend Vite não carrega notícias | Ausência de proxy `/api` |
| Publicações falham | API externa, CORS, rede ou conteúdo HTTP numa página HTTPS |
| Notícias não aparecem | Status `DRAFT`, base diferente ou lista vazia |
| Slug retorna 404 | Artigo não publicado ou slug alterado |
| Criação retorna 500 | Colisão de slug, falha de banco ou exceção não tratada especificamente |
| Pesquisa omite notícia | Artigo ainda não carregado ou campo fora do filtro |
| Filtro de imagens vazio | Divergência `IMAGEM`/`IMAGE` |
| Data mudou depois de editar | PUT de publicado sem `publishedAt` |
| `updatedAt` muda ao ler | Incremento de views aciona callback de update |
| Gostos desaparecem | Estado apenas local do cartão |
| Logótipo alternativo aparece | `/logo.png` ausente ou imagem falhou |

## 12. Manutenção e entrega

Conserve o lockfile, documente as mudanças de contrato e atualize estes guias quando alterar rotas, propriedades ou modelo. Não exponha o CRUD a acesso público sem implementar autorização backend. Antes de migrar para HTTPS, ajuste a integração externa para uma origem compatível.

Não foram executadas implantações, alterações de banco, chamadas de escrita ou testes funcionais como parte da redação destes documentos.
