+# Automoz

Sistema de consola em Java para gerir veículos para venda ou aluguer, clientes, reservas e contratos. A aplicação usa MySQL/MariaDB do XAMPP para persistir os dados entre execuções.

## Índice

- [Funcionalidades](#funcionalidades)
- [Tecnologias e execução](#tecnologias-e-execução)
- [Configuração da base de dados](#configuração-da-base-de-dados)
- [Utilização](#utilização)
- [Regras de negócio](#regras-de-negócio)
- [Estados e fluxos](#estados-e-fluxos)
- [Concorrência](#concorrência)
- [Organização do código](#organização-do-código)
- [Validações e erros](#validações-e-erros)
- [Limitações atuais](#limitações-atuais)

## Funcionalidades

1. Registar veículos para venda ou aluguer.
2. Registar clientes sem os vincular previamente a uma intenção de venda ou aluguer.
3. Listar veículos e clientes.
4. Criar reservas com validade de 48 horas.
5. Confirmar uma reserva e formalizar o contrato apropriado.
6. Cancelar uma reserva ativa.
7. Listar reservas e contratos, incluindo o valor calculado.
8. Simular três clientes a tentar reservar simultaneamente um veículo para venda ou aluguer.
9. Atualizar reservas expiradas quando o menu volta a ser apresentado.
10. Separar o acesso de cliente do painel autenticado do funcionário.

## Tecnologias e execução

- Java, com API padrão (`java.time`, coleções, `Thread` e utilitários de concorrência).
- MySQL/MariaDB via JDBC (`mysql-connector-j`).
- Aplicação de consola; o ponto de entrada é `app.Main`.
- Maven para resolver o driver JDBC (`pom.xml`); o projeto inclui também `automoz.iml` para IntelliJ IDEA.

Depois de configurar a base de dados abaixo, execute `mvn compile exec:java` ou abra o projeto Maven numa IDE e execute `app.Main`.

## Configuração da base de dados

1. Inicie os módulos **Apache** (opcional para phpMyAdmin) e **MySQL** no XAMPP.
2. Abra `http://localhost/phpmyadmin`, escolha **Importar**, selecione `database/automoz.sql` e confirme. O script cria a base `automoz_db` e as tabelas `veiculos`, `clientes`, `reservas` e `contratos`.
3. A configuração padrão do programa é `jdbc:mysql://localhost:3306/automoz_db`, utilizador `root` e senha vazia, como numa instalação local padrão do XAMPP. Se as credenciais forem diferentes, defina as variáveis de ambiente antes de iniciar:

   - `AUTOMOZ_DB_URL` (por exemplo, `jdbc:mysql://localhost:3306/automoz_db?useSSL=false&serverTimezone=UTC`)
   - `AUTOMOZ_DB_USER`
   - `AUTOMOZ_DB_PASSWORD`

4. No terminal, na pasta do projeto, execute `mvn compile exec:java`. Maven descarrega o Connector/J declarado em `pom.xml` na primeira execução, pelo que é necessária ligação à internet nesse momento. Numa IDE, importe `pom.xml` como projeto Maven para incluir o driver.

Se já tiver uma base criada pela versão anterior, faça primeiro uma cópia de segurança. Execute uma única vez `database/migracao_periodos_aluguer.sql` se a tabela `contratos` ainda não tiver as colunas `periodo` e `data_inicio`; depois execute `database/migracao_correcoes.sql` para os novos intervalos de reserva, emails únicos, sequências e funcionários. Antes de adicionar a chave única, confirme que não há emails duplicados: `SELECT LOWER(email), COUNT(*) FROM clientes GROUP BY LOWER(email) HAVING COUNT(*) > 1;`. Em bases novas, basta importar o `automoz.sql` atualizado. Os contratos antigos mantêm os meses e valores através do campo legado.

Na primeira execução sem funcionários, é criado o utilizador inicial `admin` com palavra-passe `admin123`. Pode definir `AUTOMOZ_ADMIN_USER` e `AUTOMOZ_ADMIN_PASSWORD` antes de criar a conta inicial; depois de iniciar sessão, use a opção 12 para alterar a palavra-passe. As palavras-passe são guardadas com PBKDF2 e salt. Para clientes já existentes, a inscrição não pede intenção; a carta de condução é opcional no registo e necessária para reservar um veículo de aluguer.

Ao iniciar, o sistema carrega da base os veículos, clientes, reservas e contratos existentes. Novos registos e alterações de estado são gravados no MySQL. Se a conexão falhar, a aplicação apresenta uma mensagem com os passos de configuração; não inicia em modo temporário em memória.

### Corrigir `No suitable driver found`

Este erro significa que o MySQL Connector/J não foi incluído no classpath de execução. No IntelliJ IDEA, abra `pom.xml` e escolha **Add as Maven Project** (ou abra a pasta do projeto e confirme que o `pom.xml` aparece na janela Maven). Na janela Maven, carregue em **Reload All Maven Projects** para descarregar e associar `mysql-connector-j`; depois execute `app.Main` novamente a partir do projeto Maven. A dependência já está declarada em `pom.xml`. Executar apenas o módulo Java antigo `automoz.iml`, sem recarregar o Maven, não carrega essa dependência.

Se definir a URL no PowerShell, coloque-a entre aspas simples para que os caracteres `&` da query string não sejam tratados como operadores:

```powershell
$env:AUTOMOZ_DB_URL = 'jdbc:mysql://localhost:3306/automoz_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:AUTOMOZ_DB_USER = 'root'
$env:AUTOMOZ_DB_PASSWORD = ''
```

As variáveis definidas no terminal só são herdadas por programas iniciados a partir desse terminal. Se executar pela IDE, configure-as na Run Configuration ou mantenha os valores padrão do XAMPP.

## Utilização

O menu principal apresenta estas opções:

| Opção | Ação |
|---|---|
| 1 | Cliente: iniciar sessão ou criar conta |
| 2 | Funcionário: autenticar e abrir o painel de gestão |
| 0 | Sair |
O primeiro ecrã oferece **Cliente** e **Funcionário**. Em Cliente, escolha **Iniciar sessão** ou **Criar conta**. O início de sessão valida o email e o número de telefone registados; se os dados não corresponderem, pode criar uma conta. Após autenticação ou registo, a sessão mantém o cliente identificado e a reserva não pede o seu ID. A opção Funcionário pede utilizador e palavra-passe antes de abrir o painel de gestão.

### Registar veículo

Introduza marca, modelo, tipo e preço. Para venda, o preço é tratado como preço de venda; para aluguer, como mensalidade. O sistema atribui IDs inteiros sequenciais a partir de 1 e inicia o veículo no estado `DISPONIVEL`.

### Registar cliente

Introduza nome, e-mail, contacto e, opcionalmente, carta de condução. O e-mail é validado durante a inscrição e não pode estar repetido; o contacto deve ter exatamente nove algarismos. Clientes recebem IDs `CL000001`, `CL000002`, ... . A carta é obrigatória quando o cliente reserva um veículo de aluguer.

### Criar reserva

Na sessão de cliente, escolha Criar reserva e o sistema associa-a automaticamente à conta autenticada; não é necessário introduzir o ID. No painel do funcionário, o ID do cliente continua a ser solicitado e verificado antes dos detalhes da reserva. Para aluguer, escolha o início e o período. Intervalos sobrepostos são recusados; reservas futuras sem conflito são permitidas. Reservas pendentes expiram em 48 horas.

### Formalizar contrato

Escolha uma reserva ativa. Para aluguer, escolha 1 dia, 2 dias, semanal ou mensal. Depois informe a data de início no formato `dd/MM/aaaa`; prima Enter ou escreva `HOJE` para usar a data atual. Para venda, estes dados não são pedidos. O contrato recebe um ID (`C1`, `C2`, ...), confirma a reserva e atualiza o estado do veículo.

O preço registado para um veículo de aluguer continua a ser a mensalidade. Para as opções curtas, o sistema calcula o valor proporcional usando 30 dias por mês: 1 dia = mensalidade/30; 2 dias = 2×mensalidade/30; semanal = 7×mensalidade/30; mensal = mensalidade. A listagem de contratos mostra o período e a data de início.

## Regras de negócio

### Pessoas e clientes

- ID e nome são obrigatórios.
- E-mail tem de corresponder ao padrão simples `nome@dominio.extensao`.
- Contacto deve conter exatamente nove dígitos numéricos.
- A inscrição não pede intenção de compra/aluguer.
- Carta de condução é obrigatória ao reservar um veículo de aluguer.

### Veículos

- ID tem de ser positivo; marca e modelo não podem estar vazios.
- Preço/mensalidade tem de ser finito e maior que zero.
- O tipo é `VENDA` ou `ALUGUER`.
- Veículos vendidos não podem voltar a ser reservados.
- Para venda, só pode existir uma reserva ativa por veículo.
- Para aluguer, reservas podem ser feitas para datas futuras desde que os intervalos não se sobreponham.
- Alterar tipo só é permitido quando o veículo está disponível. A aplicação de consola não expõe atualmente a edição de veículos.

### Reservas

- Uma reserva precisa de cliente e veículo válidos; aluguer também precisa de carta, data de início e período.
- Uma reserva criada fica `ACTIVA` durante 48 horas.
- Apenas reservas ativas podem ser confirmadas ou canceladas.
- A confirmação depois da expiração é recusada.
- Cancelamento ou expiração remove a reserva da disponibilidade; o estado do veículo é recalculado considerando outros contratos e intervalos.

### Contratos

- Uma reserva ativa é necessária para formalizar um contrato.
- Veículo de venda cria `ContractoVenda`; veículo de aluguer cria `ContractoAluguer`.
- Duração de aluguer: um dia, dois dias, uma semana ou um mês. A data final é calculada a partir da data de início escolhida; a assinatura conserva a data/hora em que o contrato foi formalizado.
- Depois da formalização, a reserva passa a `CONFIRMADA`; uma venda passa a `VENDIDO`. Um aluguer futuro fica `RESERVADO` e passa a `ALUGADO` quando começa; ao terminar ou ser devolvido, o sistema liberta-o se não houver outra reserva futura.
- A classe base `Contracto` possui campos para assinatura digital e imagem de assinatura, mas os serviços atuais criam os contratos com esses campos a `null`; não há fluxo de assinatura implementado.

## Estados e fluxos

### Estados do veículo

| Estado | Significado |
|---|---|
| `DISPONIVEL` | Pode ser reservado. |
| `RESERVADO` | Tem uma reserva ativa ou confirmada para uma data futura. |
| `ALUGADO` | O período de um contrato de aluguer está em curso. |
| `VENDIDO` | O contrato de venda foi formalizado. |

### Estados da reserva

| Estado | Significado |
|---|---|
| `ACTIVA` | Reserva dentro do prazo, elegível para confirmação ou cancelamento. |
| `EXPIRADA` | O prazo de 48 horas terminou; o intervalo deixa de bloquear novas reservas. |
| `CONFIRMADA` | Reserva usada para formalizar um contrato. |
| `CANCELADA` | Reserva cancelada; o intervalo deixa de bloquear novas reservas. |

Fluxo normal: `DISPONIVEL` → `RESERVADO` → `ALUGADO` ou `VENDIDO`. Um aluguer futuro pode ser reservado depois do fim do intervalo atual; estados e expirações também são verificados em segundo plano a cada 30 segundos.

## Concorrência

A opção 10 em `MenuService.simularReservasConcorrentes()` inicia três threads de pedido. Um `CountDownLatch` faz as três aguardarem até estarem prontas e depois liberta-as para tentarem criar uma reserva para o mesmo veículo e intervalo. O teste permite escolher veículos de venda ou aluguer.

O método `AutomozService.criarReserva()` é `synchronized` e a operação SQL usa transação, bloqueio `SELECT ... FOR UPDATE` do veículo e verificação de interseção de datas. Isto protege inclusive quando duas instâncias do serviço disputam o mesmo veículo. A tabela `automoz_sequencias` coordena a geração de IDs.

`AutomozService.formalizarContrato()` também cria uma thread para formalizar o contrato e espera imediatamente por ela com `join()`. Esse fluxo continua síncrono para devolver o resultado ao menu; a comunicação assíncrona é aplicada ao envio e processamento de eventos.

## Orientado a eventos

`AutomozService` publica eventos depois de a operação correspondente ser persistida. `BarramentoEventos` entrega-os numa fila processada por um worker dedicado, sem bloquear o fluxo do pedido. O ouvinte `OuvinteNotificacoesGestor` converte os eventos em alertas, que o painel do funcionário consome na próxima iteração. São publicados eventos de reserva criada, prazo a terminar (até 24 horas), reserva expirada ou cancelada, pedido aprovado, contrato criado e devolução registada. Um agendador verifica prazos e expirações a cada 30 segundos.

A aplicação é um programa de consola local com um único processo. Por isso, o barramento assíncrono interno resolve o desacoplamento entre regras de negócio e notificações sem exigir um broker externo. JMS/ActiveMQ faria sentido se o sistema fosse separado em processos/serviços ou precisasse de filas duráveis e entrega entre máquinas; os alertas atuais são apenas em memória e não são recuperados após encerrar a aplicação.

## Organização do código

```text
src/
├── app/
│   ├── Main.java              ponto de entrada
│   └── AutomozMenu.java       menu, ciclo da aplicação e tratamento de erros
├── model/                     entidades, tipos e estados
└── service/                   validações e regras de negócio
```

### Pacote `app`

- `Main`: inicia a aplicação.
- `AutomozMenu`: lê opções, chama os serviços de menu, atualiza reservas expiradas e captura erros de operação.

### Pacote `model`

- `Pessoa`: classe base com ID, nome, e-mail e contacto.
- `Cliente`: pessoa com carta de condução e intenção de venda/aluguer.
- `Gestor` e `FuncionarioAtendimento`: especializações de `Pessoa` presentes no modelo; as suas operações são atualmente métodos vazios e não estão ligadas ao menu.
- `Veiculo`: identificação, marca, modelo, preço, tipo e estado.
- `Reserva`: cliente, veículo, datas e estado.
- `Contracto`: base abstrata com cliente, veículo, data e campos reservados para assinatura.
- `ContractoVenda` e `ContractoAluguer`: especializações de contrato; o contrato de aluguer inclui período, mensalidade, data de início e data final.
- `TipoVeiculo`, `EstadoVeiculo`, `EstadoReserva`: enumeradores dos tipos e estados válidos.

### Pacote `service`

- `AutomozService`: fachada e armazenamento em memória; coordena IDs e operações principais.
- `MenuService`: interação de consola para cada funcionalidade.
- `PessoaService` e `ClienteService`: validação de dados pessoais e requisitos do cliente.
- `VeiculoService`: criação, validação e transições de estado do veículo.
- `ReservaService`: criação, expiração, confirmação e cancelamento de reservas.
- `ContractoService`: escolhe o tipo de contrato e coordena confirmação da reserva e mudança do veículo.
- `ContractoVendaService` e `ContractoAluguerService`: validação e cálculo dos respetivos contratos.

## Validações e erros

Entradas em falta ou mal formatadas geram `IllegalArgumentException`; operações incompatíveis com o estado atual geram `IllegalStateException`. `AutomozMenu` captura ambas e apresenta `Operacao recusada: ...`, mantendo o ciclo do menu ativo. Entradas numéricas inválidas são convertidas em mensagens de validação. A opção de contacto aceita `CANCELAR` para interromper o registo do cliente.

## Limitações atuais

- Não há gestão/criação de novas contas de funcionários pela interface; a conta inicial é criada por configuração e os funcionários autenticados podem alterar a própria palavra-passe.
- O registo de contratos armazena referências aos objetos em memória; não existe emissão/armazenamento de documentos.
- Os campos de assinatura digital não são produzidos nem verificados.
- `Gestor` e `FuncionarioAtendimento` são modelos incompletos e não participam no fluxo implementado.
- A atualização de reservas expiradas ocorre quando o menu é atualizado, e não exatamente no instante da expiração.

## Testes

Com Connector/J disponível no repositório Maven e MySQL do XAMPP iniciado com o esquema/migração atualizados, execute `./tests/run-tests.ps1` no PowerShell. O conjunto de domínio valida emails, dados do cliente, carta, períodos e mensalidades. O teste de integração cria dados temporários para autenticação, duplicados, três reservas concorrentes para aluguer/venda, intervalos futuros e devolução; remove os dados temporários ao terminar.
