# Automoz

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
2. Registar clientes com intenção de compra ou aluguer.
3. Listar veículos e clientes.
4. Criar reservas com validade de 48 horas.
5. Confirmar uma reserva e formalizar o contrato apropriado.
6. Cancelar uma reserva ativa.
7. Listar reservas e contratos, incluindo o valor calculado.
8. Simular dois clientes a tentar reservar simultaneamente o mesmo veículo.
9. Atualizar reservas expiradas quando o menu volta a ser apresentado.

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

Se já tiver importado a versão antiga de `automoz.sql`, faça primeiro uma cópia de segurança e execute uma única vez `database/migracao_periodos_aluguer.sql` no phpMyAdmin. Em bases novas, basta importar o `automoz.sql` atualizado. Os contratos antigos mantêm os meses e valores através do campo legado.

Ao iniciar, o sistema carrega da base os veículos, clientes, reservas e contratos existentes. Novos registos e alterações de estado são gravados no MySQL. Se a conexão falhar, a aplicação apresenta uma mensagem com os passos de configuração; não inicia em modo temporário em memória.

## Utilização

O menu principal apresenta estas opções:

| Opção | Ação |
|---|---|
| 1 | Registar veículo |
| 2 | Registar cliente |
| 3 | Listar veículos |
| 4 | Listar clientes |
| 5 | Criar reserva |
| 6 | Confirmar reserva e formalizar contrato |
| 7 | Cancelar reserva |
| 8 | Listar reservas |
| 9 | Listar contratos |
| 10 | Simular duas reservas concorrentes para o mesmo veículo |
| 0 | Sair |

### Registar veículo

Introduza marca, modelo, tipo e preço. Para venda, o preço é tratado como preço de venda; para aluguer, como mensalidade. O sistema atribui IDs inteiros sequenciais a partir de 1 e inicia o veículo no estado `DISPONIVEL`.

### Registar cliente

Introduza nome, e-mail, contacto e intenção (`ALUGUER` ou `VENDA`). O contacto deve ter exatamente nove algarismos. Clientes de aluguer também têm de indicar carta de condução. O programa atribui IDs separados por intenção: `CALG300001`, `CALG300002`, ... para aluguer e `CVD780001`, `CVD780002`, ... para venda.

### Criar reserva

Escolha um cliente e um veículo compatíveis. O sistema atribui um ID sequencial (`R1`, `R2`, ...) e define a expiração para 48 horas após a criação. O veículo passa a `RESERVADO`.

### Formalizar contrato

Escolha uma reserva ativa. Para aluguer, escolha 1 dia, 2 dias, semanal ou mensal. Depois informe a data de início no formato `dd/MM/aaaa`; prima Enter ou escreva `HOJE` para usar a data atual. Para venda, estes dados não são pedidos. O contrato recebe um ID (`C1`, `C2`, ...), confirma a reserva e atualiza o estado do veículo.

O preço registado para um veículo de aluguer continua a ser a mensalidade. Para as opções curtas, o sistema calcula o valor proporcional usando 30 dias por mês: 1 dia = mensalidade/30; 2 dias = 2×mensalidade/30; semanal = 7×mensalidade/30; mensal = mensalidade. A listagem de contratos mostra o período e a data de início.

## Regras de negócio

### Pessoas e clientes

- ID e nome são obrigatórios.
- E-mail tem de corresponder ao padrão simples `nome@dominio.extensao`.
- Contacto deve conter exatamente nove dígitos numéricos.
- Intenção do cliente é obrigatória e tem de coincidir com o tipo do veículo reservado.
- Carta de condução é obrigatória para clientes cuja intenção seja aluguer.

### Veículos

- ID tem de ser positivo; marca e modelo não podem estar vazios.
- Preço/mensalidade tem de ser finito e maior que zero.
- O tipo é `VENDA` ou `ALUGUER`.
- Só veículos `DISPONIVEL` podem ser reservados.
- Um veículo reservado não pode receber outra reserva.
- Alterar tipo só é permitido quando o veículo está disponível. A aplicação de consola não expõe atualmente a edição de veículos.

### Reservas

- Uma reserva precisa de cliente e veículo válidos, e as intenções/tipos devem coincidir.
- Uma reserva criada fica `ACTIVA` durante 48 horas.
- Apenas reservas ativas podem ser confirmadas ou canceladas.
- A confirmação depois da expiração é recusada.
- Cancelamento ou expiração liberta o veículo.

### Contratos

- Uma reserva ativa é necessária para formalizar um contrato.
- Veículo de venda cria `ContractoVenda`; veículo de aluguer cria `ContractoAluguer`.
- Duração de aluguer: um dia, dois dias, uma semana ou um mês. A data final é calculada a partir da data de início escolhida; a assinatura conserva a data/hora em que o contrato foi formalizado.
- Depois da formalização, a reserva passa a `CONFIRMADA`; o veículo passa a `VENDIDO` ou `ALUGADO`.
- A classe base `Contracto` possui campos para assinatura digital e imagem de assinatura, mas os serviços atuais criam os contratos com esses campos a `null`; não há fluxo de assinatura implementado.

## Estados e fluxos

### Estados do veículo

| Estado | Significado |
|---|---|
| `DISPONIVEL` | Pode ser reservado. |
| `RESERVADO` | Está associado a uma reserva ativa. |
| `ALUGADO` | O contrato de aluguer foi formalizado. |
| `VENDIDO` | O contrato de venda foi formalizado. |

### Estados da reserva

| Estado | Significado |
|---|---|
| `ACTIVA` | Reserva dentro do prazo, elegível para confirmação ou cancelamento. |
| `EXPIRADA` | O prazo de 48 horas terminou; o veículo é libertado. |
| `CONFIRMADA` | Reserva usada para formalizar um contrato. |
| `CANCELADA` | Reserva cancelada pelo utilizador; o veículo é libertado. |

Fluxo normal: `DISPONIVEL` → `RESERVADO` → `ALUGADO` ou `VENDIDO`. Fluxos alternativos: `RESERVADO` → `DISPONIVEL` quando a reserva é cancelada ou expira. A atualização automática de expiração ocorre ao início de cada iteração do menu; não existe uma thread temporizada em segundo plano.

## Concorrência

A opção 10 em `MenuService.simularReservasConcorrentes()` inicia duas threads de pedido. Um `CountDownLatch` faz ambas aguardarem até estarem prontas e, em seguida, liberta-as para tentarem criar uma reserva para o mesmo veículo. A aplicação espera pelo fim das duas com `join()` e apresenta o resultado de cada pedido.

O método `AutomozService.criarReserva()` é `synchronized`. Na mesma instância do serviço, apenas uma thread entra nesse método de cada vez. A primeira reserva o veículo; a segunda encontra o veículo já reservado e é recusada. Isto protege também a geração sequencial do ID e a alteração dos mapas durante esse fluxo.

`AutomozService.formalizarContrato()` também cria uma thread para formalizar o contrato e espera imediatamente por ela com `join()`. Portanto, esse método executa o trabalho noutra thread, mas o chamador continua bloqueado até terminar; não permite que a interface prossiga em paralelo. A secção que altera contratos é sincronizada por `formalizarContratoComSeguranca()`.

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

- Não há autenticação, permissões ou interface gráfica/web.
- O registo de contratos armazena referências aos objetos em memória; não existe emissão/armazenamento de documentos.
- Os campos de assinatura digital não são produzidos nem verificados.
- `Gestor` e `FuncionarioAtendimento` são modelos incompletos e não participam no fluxo implementado.
- A atualização de reservas expiradas ocorre quando o menu é atualizado, e não exatamente no instante da expiração.
- A sincronização Java cobre pedidos concorrentes dentro da mesma execução; não foi desenhada como mecanismo de bloqueio distribuído entre várias instâncias da aplicação.
