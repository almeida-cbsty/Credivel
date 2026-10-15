# Credível

Aplicação web de notícias com classificação editorial, frontend React, API Spring Boot e integração com publicações externas.

## Clonar e executar

Requisitos: Java 21, Node.js 22.12+ e npm. A primeira compilação requer internet
para obter dependências. Não precisa de instalar Maven, MySQL ou Nginx.

```bash
git clone https://github.com/almeida-cbsty/Credivel.git
cd Credivel
./run.sh
```

Abra http://localhost:8083. O comando compila o frontend, inclui-o no JAR e
inicia a aplicação com H2 persistente em `data/`. Para mudar a porta:
`SERVER_PORT=8090 ./run.sh`. Depois de compilar, pode iniciar sem recompilar:

```bash
SPRING_PROFILES_ACTIVE=local java -jar backend/target/credivel-api-1.0.0.jar
```

### Notícias incluídas

`backend/src/main/resources/seed/news.json` contém as 55 notícias publicadas
exportadas em 15/10/2026, com títulos, resumos, textos completos, classificações,
fontes, autores, datas e referências às imagens. As 55 imagens estão em
`frontend/public/imported/` e são incluídas no JAR.
A importação acontece automaticamente apenas quando a tabela de notícias está
vazia. Bases existentes não são substituídas e reiniciar não duplica notícias.
Os identificadores são gerados novamente; os slugs são preservados.
Alterações posteriores na base do servidor não atualizam o JSON automaticamente.

A secção Publicações continua a consultar a API externa da MozDigital e depende
desse serviço. As notícias incluídas funcionam sem essa API.
Login demonstrativo: `user@credivel.com` / `user123`.
Para usar MySQL, consulte o guia de implantação e escolha
`SPRING_PROFILES_ACTIVE=prod`, configurando as variáveis `CREDIVEL_DB_*`.

## Documentação

- [Descrição técnica completa](docs/DESCRICAO_TECNICA.md): arquitetura, funcionalidades, componentes, dados e limitações.
- [Manual do sistema](docs/MANUAL_DO_SISTEMA.md): utilização, desenvolvimento, configuração e manutenção.
- [Referência da API](docs/API.md): endpoints, validações e exemplos.
- [Implantação](DEPLOYMENT.md): configuração do servidor com MySQL ou H2, systemd e Nginx.

Documentação elaborada em 14/10/2026 a partir do código local. O login atual é demonstrativo e o backend não protege os endpoints de escrita com autenticação.
