# Referência da API

## 1. Convenções

Base: `/api/news`. Comunicação JSON. Datas são instantes ISO 8601; exemplos usam UTC. IDs são inteiros longos. Slugs enviados no caminho devem ser codificados como segmento de URL. Não há versionamento, autenticação, PATCH, upload, pesquisa remota ou endpoint de utilizadores.

Os exemplos usam `http://127.0.0.1:8083`; através do Nginx local pode ser usado `http://127.0.0.1:8084`. Pedidos de escrita alteram dados: execute os exemplos num ambiente de desenvolvimento e substitua IDs pelos efetivamente devolvidos.

## 2. Endpoints

| Método | Caminho | Resultado |
|---|---|---|
| GET | `/api/news?page=0&size=12` | 200, página de notícias publicadas |
| GET | `/api/news/slug/{slug}` | 200, publicado por slug; incrementa visualizações |
| GET | `/api/news/{id}` | 200, publicado por ID; incrementa visualizações |
| POST | `/api/news` | 201, notícia criada |
| PUT | `/api/news/{id}` | 200, notícia atualizada |
| DELETE | `/api/news/{id}` | 204, sem corpo |

Não há cabeçalho `Location` explicitamente definido na criação. Consultar rascunho retorna 404. Atualização e eliminação aceitam também IDs de rascunhos.

## 3. Paginação

`page` deve ser maior ou igual a zero; `size` deve estar entre 1 e 50. Valores padrão: zero e 12. A ordenação é `publishedAt DESC`, fixa no serviço; não existe parâmetro de ordenação ou filtro.

```bash
curl --fail-with-body 'http://127.0.0.1:8083/api/news?page=0&size=9'
```

A resposta é um `Page<NewsResponse>` serializado pelo Spring. O frontend utiliza `content` e `last`. Outros campos habituais incluem `totalElements`, `totalPages`, `number`, `size`, `first`, `numberOfElements` e `empty`; a estrutura de metadados não é um envelope próprio versionado.

Exemplo abreviado, não representação de todos os metadados:

```json
{
  "content": [],
  "last": true,
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 9
}
```

## 4. Corpo de criação e atualização

POST e PUT usam o mesmo `NewsRequest`. PUT exige reenviar os campos obrigatórios; não é uma atualização parcial.

| Campo | Tipo JSON | Validação |
|---|---|---|
| `title` | string | Obrigatório, não branco, máximo 255 |
| `slug` | string/null | Opcional, máximo 280; derivado do título se vazio |
| `summary` | string | Obrigatório, não branco, máximo 800 |
| `content` | string | Obrigatório, não branco; sem máximo explícito |
| `imageUrl` | string | Obrigatório, não branco, máximo 1.000 |
| `classification` | string | Obrigatório: `CREDIVEL` ou `FALSO` |
| `source` | string | Obrigatório, não branco, máximo 180 |
| `author` | string/null | Opcional, máximo 160 |
| `category` | string | Obrigatório, não branco, máximo 100 |
| `status` | string | Obrigatório: `DRAFT` ou `PUBLISHED` |
| `publishedAt` | string/null | Instante ISO 8601 opcional |

Strings editoriais recebem `trim()`; autor vazio vira null. `imageUrl` não recebe validação de URL, domínio ou extensão. Categoria e fonte são texto livre. A validação de tamanho acontece antes da normalização do serviço.

`id`, `views` e `updatedAt` não são campos de entrada do DTO. Para `PUBLISHED` sem data, o instante atual é atribuído em criação e atualização. Para conservar data e slug num PUT, reenvie os valores existentes.

Criação de rascunho:

```bash
curl --fail-with-body -X POST 'http://127.0.0.1:8083/api/news' \
  -H 'Content-Type: application/json' \
  --data-binary '{
    "title":"Exemplo de artigo editorial",
    "slug":"exemplo-de-artigo-editorial",
    "summary":"Resumo demonstrativo do artigo.",
    "content":"Primeiro parágrafo.\nSegundo parágrafo.",
    "imageUrl":"/news-verify.svg",
    "classification":"CREDIVEL",
    "source":"Equipa editorial",
    "author":"Editor de demonstração",
    "category":"Literacia digital",
    "status":"DRAFT",
    "publishedAt":null
  }'
```

Publicação de um ID de exemplo:

```bash
curl --fail-with-body -X PUT 'http://127.0.0.1:8083/api/news/123' \
  -H 'Content-Type: application/json' \
  --data-binary '{
    "title":"Exemplo de artigo editorial",
    "slug":"exemplo-de-artigo-editorial",
    "summary":"Resumo demonstrativo do artigo.",
    "content":"Primeiro parágrafo.\nSegundo parágrafo.",
    "imageUrl":"/news-verify.svg",
    "classification":"CREDIVEL",
    "source":"Equipa editorial",
    "author":"Editor de demonstração",
    "category":"Literacia digital",
    "status":"PUBLISHED",
    "publishedAt":"2026-10-14T12:00:00Z"
  }'
```

Leitura e eliminação:

```bash
curl --fail-with-body 'http://127.0.0.1:8083/api/news/slug/exemplo-de-artigo-editorial'
curl --fail-with-body 'http://127.0.0.1:8083/api/news/123'
curl --fail-with-body -X DELETE 'http://127.0.0.1:8083/api/news/123'
```

A eliminação é definitiva e o modelo não possui restauro lógico.

## 5. Resposta de notícia

Exemplo ilustrativo:

```json
{
  "id": 123,
  "title": "Exemplo de artigo editorial",
  "slug": "exemplo-de-artigo-editorial",
  "summary": "Resumo demonstrativo do artigo.",
  "content": "Primeiro parágrafo.\nSegundo parágrafo.",
  "imageUrl": "/news-verify.svg",
  "classification": "CREDIVEL",
  "source": "Equipa editorial",
  "author": "Editor de demonstração",
  "category": "Literacia digital",
  "status": "PUBLISHED",
  "views": 1,
  "publishedAt": "2026-10-14T12:00:00Z",
  "updatedAt": "2026-10-14T12:00:00Z"
}
```

A resposta de consulta individual já inclui o contador incrementado. `updatedAt` é alterado na sincronização da entidade; o valor devolvido pode preceder o callback dessa atualização. Não o utilize como garantia de timestamp final do incremento.

## 6. Erros

| Situação tratada | HTTP | Corpo |
|---|---|---|
| Notícia inexistente ou não publicada no GET | 404 | `{"message":"Notícia não encontrada."}` |
| Bean Validation ou IllegalArgumentException | 400 | `{"message":"Dados inválidos."}` |
| Outras exceções capturadas | 500 | `{"message":"Não foi possível concluir o pedido."}` |

Não há detalhes por campo, código de erro estruturado ou tratamento específico de slug duplicado. JSON malformado, enum inválido e erros de conversão não têm handlers dedicados e podem cair no handler genérico 500. Não se deve assumir 400 para toda entrada inválida. Comportamentos HTTP fora destes handlers dependem do framework e não foram testados aqui.

## 7. CORS e acesso

`WebConfig` habilita as origens configuradas para `/api/**`, métodos GET, POST, PUT e DELETE, com cache de preflight de 3.600 segundos. Não configura credenciais explicitamente. As origens são separadas por vírgula, sem remoção de espaços pelo código.

Nenhum endpoint exige autenticação. O login demonstrativo do frontend não muda este contrato. A integração externa de publicações tem contrato próprio, apenas parcialmente adaptado no componente React; não pertence a esta API.
