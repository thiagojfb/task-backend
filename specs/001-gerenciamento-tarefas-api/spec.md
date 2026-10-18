# Feature Specification: API de Gerenciamento de Tarefas

**Feature Branch**: `001-gerenciamento-tarefas-api`

**Created**: 2026-09-19

**Status**: Draft

**Input**: User description: "A aplicação é uma lista de tarefas por prioridade, descrição, data de abertura, data de conclusão, valor em reais caso precise gastar dinheiro, possibilidade de cadastrar nova tarefa, arrastar para o topo, final meio ou topo da lista, edição da tarefa, histórico de edição, exclusão. O usuário sempre logará com seu login e senha no keycloak. [...] Primeiro vou fazer somente o backend, só depois vou iniciar o front."

## Contexto e Escopo

Esta feature entrega o **backend** de uma lista de tarefas pessoais com controle financeiro.
A interface visual (dashboard com barra lateral, cabeçalho, lista em formato de cartões,
drawer de edição, popover de exclusão) descrita pelo usuário **NÃO faz parte desta
entrega** e será construída em fase posterior. Ela é usada aqui apenas como referência
para garantir que o backend exponha todos os dados e operações que a futura interface
precisará consumir.

**Dentro do escopo**: cadastro, consulta, edição, conclusão, reordenação, histórico de
alterações e exclusão de tarefas; filtros por situação; busca textual; indicador
financeiro mensal; autenticação obrigatória via provedor de identidade externo.

**Fora do escopo**: qualquer tela ou componente visual; cadastro/gestão de usuários
(delegado ao provedor de identidade); notificações; anexos; compartilhamento de tarefas
entre usuários; categorias/etiquetas além da prioridade.

## Clarifications

### Session 2026-09-19

- Q: As tarefas são privadas por usuário ou uma lista única compartilhada entre todos os
  usuários autenticados? → A: Privadas por usuário — cada um vê e altera somente as
  tarefas que criou; não há papel administrativo nem compartilhamento.
- Q: "Data de conclusão" é o prazo previsto ou a data em que a tarefa foi concluída?
  → A: Dois campos distintos — **data prevista de conclusão** (prazo, informada e
  editável pelo usuário) e **data/hora real de conclusão** (gravada automaticamente ao
  marcar como concluída, apagada ao reabrir). "Atrasada" = aberta com prazo anterior à
  data atual.
- Q: A lista de tarefas deve ser retornada inteira ou paginada? → A: Lista completa,
  sem paginação (uso pessoal, centenas de tarefas por usuário no máximo); necessária
  para a reordenação por arrastar-e-soltar.
- Q: Uma tarefa concluída pode ser editada e reordenada? → A: Sim, livremente; não há
  bloqueio por situação. Toda alteração continua gerando registro de histórico.
- Q: A busca por descrição deve ignorar acentos além de maiúsculas/minúsculas? → A: Sim;
  a comparação ignora maiúsculas/minúsculas e acentuação em ambos os lados (termo
  buscado e descrição armazenada).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastrar e visualizar tarefas (Priority: P1)

Um usuário autenticado cadastra uma nova tarefa informando descrição, prioridade
(Alta, Média ou Baixa), data prevista de conclusão e, opcionalmente, o valor em reais
que precisará gastar. O sistema registra automaticamente a data de abertura e coloca a
tarefa no final da lista. O usuário então consulta sua lista completa, ordenada pela
posição manual, vendo todos os dados de cada tarefa.

**Why this priority**: sem cadastrar e listar tarefas não existe produto. É o MVP mínimo
que já entrega valor: uma lista pessoal de pendências com controle de gastos.

**Independent Test**: pode ser testado integralmente cadastrando três tarefas com dados
distintos e consultando a lista; entrega valor ao permitir que o usuário registre e
enxergue suas pendências.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado sem tarefas, **When** cadastra uma tarefa com
   descrição "Pagar conta de luz", prioridade Alta, data prevista 2026-09-30 e valor
   R$ 250,00, **Then** a tarefa é criada com data de abertura igual à data/hora atual,
   situação "Aberta", posição 1 e todos os dados informados são retornados.
2. **Given** um usuário com duas tarefas, **When** cadastra uma terceira, **Then** ela
   ocupa a última posição (3) e as posições das anteriores permanecem inalteradas.
3. **Given** um usuário autenticado, **When** cadastra uma tarefa sem informar valor,
   **Then** a tarefa é criada com valor R$ 0,00.
4. **Given** um usuário autenticado, **When** tenta cadastrar uma tarefa sem descrição
   ou com prioridade inexistente, **Then** o cadastro é rejeitado com mensagem em
   português indicando cada campo inválido.
5. **Given** um usuário não autenticado, **When** tenta acessar qualquer operação de
   tarefas, **Then** o acesso é negado e nenhum dado é retornado.
6. **Given** dois usuários distintos com tarefas próprias, **When** um deles consulta
   sua lista, **Then** somente as tarefas de sua autoria são exibidas.

---

### User Story 2 - Editar e concluir tarefas (Priority: P1)

O usuário altera qualquer dado de uma tarefa existente (descrição, prioridade, data
prevista, valor) e pode marcá-la como concluída ou reabri-la. Ao concluir, o sistema
registra o momento da conclusão.

**Why this priority**: tarefas mudam com o tempo; sem edição e conclusão a lista fica
obsoleta rapidamente e o indicador financeiro perde sentido.

**Independent Test**: cadastrar uma tarefa, alterar seu valor e prioridade, marcá-la
como concluída e verificar que a consulta reflete as mudanças e a situação "Concluída".

**Acceptance Scenarios**:

1. **Given** uma tarefa aberta com valor R$ 50,00, **When** o usuário altera o valor
   para R$ 70,00, **Then** a consulta seguinte retorna R$ 70,00 e os demais campos
   permanecem inalterados.
2. **Given** uma tarefa aberta, **When** o usuário a marca como concluída, **Then** a
   situação passa a "Concluída" e a data/hora real de conclusão é registrada.
3. **Given** uma tarefa concluída, **When** o usuário a reabre, **Then** a situação
   volta a "Aberta" e a data/hora real de conclusão é apagada.
4. **Given** uma tarefa de outro usuário, **When** o usuário tenta editá-la, **Then** a
   operação é negada como se a tarefa não existisse.
5. **Given** um identificador inexistente, **When** o usuário tenta editar, **Then**
   recebe erro indicando que a tarefa não foi encontrada.

---

### User Story 3 - Reordenar tarefas na lista (Priority: P2)

O usuário reposiciona uma tarefa para qualquer lugar da lista: topo, final ou uma
posição intermediária específica (equivalente ao arrastar-e-soltar da futura interface).
As demais tarefas são deslocadas para manter a sequência contínua.

**Why this priority**: a ordenação manual é o diferencial de usabilidade pedido pelo
usuário; sem ela a lista é apenas cronológica.

**Independent Test**: cadastrar cinco tarefas, mover a quinta para o topo, a primeira
para o final e a terceira para a posição 2; verificar a sequência resultante.

**Acceptance Scenarios**:

1. **Given** tarefas nas posições 1..5, **When** o usuário move a tarefa da posição 5
   para o topo, **Then** ela passa a ocupar a posição 1 e as antigas 1..4 passam a 2..5.
2. **Given** tarefas nas posições 1..5, **When** o usuário move a tarefa da posição 1
   para o final, **Then** ela ocupa a posição 5 e as antigas 2..5 passam a 1..4.
3. **Given** tarefas nas posições 1..5, **When** o usuário move a tarefa da posição 4
   para a posição 2, **Then** a ordem final é [1, 4, 2, 3, 5] (identificando pelas
   posições originais).
4. **Given** tarefas nas posições 1..5, **When** o usuário solicita mover para a
   posição 9 ou 0, **Then** a operação é rejeitada com mensagem indicando o intervalo
   válido.
5. **Given** qualquer reordenação, **When** concluída, **Then** as posições de todas as
   tarefas do usuário formam uma sequência contínua sem lacunas nem repetições.

---

### User Story 4 - Consultar histórico de alterações (Priority: P2)

Toda alteração feita em uma tarefa gera automaticamente um registro de histórico com
quem alterou, quando, qual campo mudou e os valores anterior e novo. O usuário consulta
essa linha do tempo para entender a evolução da tarefa.

**Why this priority**: rastreabilidade é requisito explícito do usuário ("Histórico")
e sustenta o controle financeiro (saber que o valor subiu de R$ 50 para R$ 70).

**Independent Test**: cadastrar uma tarefa, alterar valor e prioridade em duas
operações distintas, concluir a tarefa e consultar o histórico verificando três ou mais
registros ordenados do mais recente ao mais antigo.

**Acceptance Scenarios**:

1. **Given** uma tarefa com valor R$ 50,00, **When** o usuário altera para R$ 70,00,
   **Then** o histórico ganha um registro contendo o nome do usuário, a data/hora,
   o campo "valor", o valor anterior "R$ 50,00" e o novo "R$ 70,00".
2. **Given** uma edição que altera descrição e prioridade ao mesmo tempo, **When**
   confirmada, **Then** o histórico registra um item por campo alterado.
3. **Given** uma edição que envia os mesmos valores já existentes, **When** confirmada,
   **Then** nenhum registro de histórico é criado.
4. **Given** uma tarefa marcada como concluída, **When** o histórico é consultado,
   **Then** existe um registro de mudança de situação "Aberta" → "Concluída".
5. **Given** uma tarefa recém-criada, **When** o histórico é consultado, **Then**
   existe um único registro de criação.
6. **Given** uma reordenação de posição, **When** o histórico é consultado, **Then**
   nenhum registro é criado (mudança de posição não é considerada edição de conteúdo).

---

### User Story 5 - Excluir tarefa (Priority: P3)

O usuário exclui definitivamente uma tarefa que não faz mais sentido. A lista é
reorganizada para não deixar lacuna na sequência de posições.

**Why this priority**: necessário para higiene da lista, mas o usuário pode conviver
temporariamente marcando como concluída.

**Independent Test**: cadastrar três tarefas, excluir a do meio e verificar que a lista
tem duas tarefas nas posições 1 e 2.

**Acceptance Scenarios**:

1. **Given** tarefas nas posições 1..3, **When** o usuário exclui a da posição 2,
   **Then** ela deixa de existir e a antiga posição 3 passa a 2.
2. **Given** uma tarefa excluída, **When** o usuário consulta seu histórico ou tenta
   editá-la, **Then** recebe erro de "não encontrada".
3. **Given** uma tarefa de outro usuário, **When** o usuário tenta excluí-la, **Then**
   a operação é negada como se a tarefa não existisse.

---

### User Story 6 - Filtrar, buscar e acompanhar gastos (Priority: P3)

O usuário filtra a lista por situação ("Todas", "Abertas", "Concluídas"), busca tarefas
por trecho da descrição e consulta o indicador "Total a gastar neste mês", que soma o
valor das tarefas abertas com data prevista de conclusão dentro do mês corrente.

**Why this priority**: aumenta a utilidade da lista quando ela cresce, mas não é
necessário para o valor central do MVP.

**Independent Test**: cadastrar tarefas abertas e concluídas, com e sem valor, dentro
e fora do mês corrente; verificar cada filtro, uma busca parcial e o total mensal.

**Acceptance Scenarios**:

1. **Given** 3 tarefas abertas e 2 concluídas, **When** o usuário filtra por "Abertas",
   **Then** recebe exatamente as 3 abertas, mantendo a ordem por posição.
2. **Given** tarefas com descrições "Pagar luz" e "Comprar lâmpada", **When** o usuário
   busca "LU", **Then** recebe apenas "Pagar luz"; **When** busca "lampada", **Then**
   recebe apenas "Comprar lâmpada" (a busca ignora maiúsculas/minúsculas e acentos).
3. **Given** tarefas abertas com valores R$ 1.000,00 e R$ 250,00 vencendo neste mês,
   uma concluída de R$ 500,00 neste mês e uma aberta de R$ 300,00 no mês seguinte,
   **When** o usuário consulta o indicador, **Then** o total é R$ 1.250,00.
4. **Given** nenhuma tarefa aberta no mês, **When** o usuário consulta o indicador,
   **Then** o total é R$ 0,00.
5. **Given** filtro e busca combinados, **When** aplicados juntos, **Then** o resultado
   respeita ambos os critérios.

---

### Edge Cases

- Tarefa com data prevista de conclusão anterior à data atual: aceita (o usuário pode
  registrar algo já atrasado), e a consulta expõe a informação necessária para que a
  interface a identifique como "atrasada" (aberta e com data prevista no passado).
- Valor negativo ou com mais de duas casas decimais: rejeitado com mensagem clara.
- Descrição com mais de 500 caracteres ou só espaços: rejeitada.
- Duas edições concorrentes na mesma tarefa: a última gravada prevalece; ambas geram
  registros de histórico.
- Reordenação de uma tarefa para a posição que ela já ocupa: operação sem efeito, sem
  erro.
- Lista vazia: consultas retornam lista vazia e indicador financeiro R$ 0,00.
- Token de autenticação expirado ou inválido: acesso negado com mensagem padronizada,
  sem vazar detalhes internos.
- Usuário autenticado sem nome de exibição no provedor de identidade: o histórico
  registra o identificador único do usuário no lugar do nome.

## Requirements *(mandatory)*

### Functional Requirements

**Autenticação e autorização**

- **FR-001**: O sistema DEVE exigir que toda operação sobre tarefas seja realizada por
  um usuário autenticado por login e senha junto ao provedor de identidade externo.
- **FR-002**: O sistema DEVE associar cada tarefa ao usuário que a criou e restringir
  consulta, edição, reordenação, histórico e exclusão às tarefas do próprio usuário.
- **FR-003**: O sistema DEVE responder a tentativas de acesso a tarefas de terceiros
  como se a tarefa não existisse, sem revelar sua existência.

**Cadastro e consulta**

- **FR-004**: O sistema DEVE permitir cadastrar tarefa com: descrição (obrigatória,
  1 a 500 caracteres), prioridade (obrigatória: Alta, Média ou Baixa), data prevista de
  conclusão (opcional) e valor em reais (opcional, padrão R$ 0,00, não negativo, até
  duas casas decimais).
- **FR-005**: O sistema DEVE registrar automaticamente a data/hora de abertura no momento
  do cadastro, sem permitir que o usuário a informe ou altere.
- **FR-006**: O sistema DEVE atribuir à nova tarefa a última posição da lista do usuário.
- **FR-007**: O sistema DEVE toda nova tarefa iniciar na situação "Aberta".
- **FR-008**: O sistema DEVE retornar a lista **completa** (sem paginação) de tarefas do
  usuário ordenada pela posição manual crescente, incluindo para cada tarefa:
  identificador, descrição, prioridade, situação, data de abertura, data prevista de
  conclusão, data real de conclusão, valor, posição e indicador de atraso (verdadeiro
  quando aberta com data prevista anterior à data atual).
- **FR-009**: O sistema DEVE permitir consultar uma tarefa individual pelo identificador.

**Edição e conclusão**

- **FR-010**: O sistema DEVE permitir alterar descrição, prioridade, data prevista de
  conclusão e valor de uma tarefa existente, independentemente de sua situação (aberta
  ou concluída), aplicando as mesmas validações do cadastro.
- **FR-011**: O sistema DEVE permitir marcar uma tarefa como "Concluída", registrando a
  data/hora real de conclusão e movendo-a automaticamente para o final da lista (sem
  registro de histórico para a mudança de posição), e permitir reabri-la, apagando essa
  data e mantendo a posição atual. *(Emenda 2026-09-20, a pedido do usuário após uso.)*
- **FR-012**: O sistema DEVE rejeitar edições com dados inválidos retornando, em
  português, a lista de campos e motivos.

**Reordenação**

- **FR-013**: O sistema DEVE permitir mover uma tarefa (aberta ou concluída) para o topo,
  para o final ou para uma posição específica da lista do usuário.
- **FR-014**: O sistema DEVE, após qualquer reordenação ou exclusão, manter as posições
  das tarefas do usuário como uma sequência contínua iniciada em 1, sem lacunas nem
  duplicidades.
- **FR-015**: O sistema DEVE rejeitar posições fora do intervalo 1..N (N = total de
  tarefas do usuário), informando o intervalo válido.

**Histórico**

- **FR-016**: O sistema DEVE gerar automaticamente um registro de histórico na criação
  da tarefa e a cada campo de conteúdo alterado (descrição, prioridade, data prevista,
  valor, situação), contendo: identificador e nome do usuário autor, data/hora, nome do
  campo, valor anterior e valor novo.
- **FR-017**: O sistema NÃO DEVE gerar registro de histórico quando a edição não altera
  nenhum campo nem quando apenas a posição muda.
- **FR-018**: O sistema DEVE permitir consultar o histórico de uma tarefa ordenado do
  registro mais recente ao mais antigo.
- **FR-019**: O sistema DEVE apresentar valores monetários no histórico no formato
  brasileiro (ex.: "R$ 50,00") e datas no formato dd/MM/yyyy, para consumo direto pela
  interface.

**Exclusão**

- **FR-020**: O sistema DEVE permitir excluir definitivamente uma tarefa do próprio
  usuário, removendo também seu histórico.

**Filtros, busca e indicador financeiro**

- **FR-021**: O sistema DEVE permitir filtrar a lista por situação: todas, abertas ou
  concluídas.
- **FR-022**: O sistema DEVE permitir buscar tarefas por trecho da descrição, sem
  distinção de maiúsculas/minúsculas nem de acentuação (em ambos os lados da
  comparação), combinável com o filtro de situação.
- **FR-023**: O sistema DEVE fornecer o indicador "total a gastar no mês": soma dos
  valores das tarefas abertas do usuário cuja data prevista de conclusão está dentro do
  mês corrente; tarefas sem data prevista não entram no cálculo.

**Comportamento geral**

- **FR-024**: O sistema DEVE responder a erros (validação, não encontrado, não
  autorizado) com estrutura padronizada e mensagens em português.
- **FR-025**: O sistema DEVE publicar documentação interativa de todas as operações,
  em português, permitindo que a futura interface seja construída sem consulta ao
  código-fonte.

### Key Entities *(include if feature involves data)*

- **Tarefa**: item da lista de pendências de um usuário. Atributos: identificador,
  descrição, descrição normalizada para busca (sem acentos, minúsculas — derivada
  automaticamente), prioridade (Alta/Média/Baixa), situação (Aberta/Concluída),
  data/hora de abertura (automática), data prevista de conclusão (opcional), data/hora
  real de conclusão (preenchida ao concluir), valor em reais (padrão zero), posição na
  lista, identificador do usuário proprietário.
- **Registro de Histórico**: evento imutável de alteração de uma Tarefa. Atributos:
  identificador, tarefa relacionada, identificador e nome do usuário autor, data/hora,
  tipo (criação, alteração, conclusão, reabertura), campo alterado, valor anterior,
  valor novo. Relacionamento: muitos registros para uma Tarefa; removidos junto com ela.
- **Usuário**: identidade gerida externamente pelo provedor de identidade; a aplicação
  conhece apenas seu identificador único e nome de exibição obtidos no momento da
  autenticação. Não é persistido como entidade própria.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um usuário consegue cadastrar uma tarefa completa (descrição, prioridade,
  data e valor) em uma única operação e vê-la na lista imediatamente após.
- **SC-002**: 100% das operações sobre tarefas realizadas sem autenticação válida são
  negadas; 0 tarefas de um usuário são visíveis a outro usuário.
- **SC-003**: Após qualquer sequência de cadastros, reordenações e exclusões, as
  posições da lista permanecem contínuas de 1 a N em 100% dos casos testados.
- **SC-004**: Toda alteração de conteúdo gera registro de histórico rastreável ao autor,
  data e valores anterior/novo em 100% dos casos; edições sem mudança geram 0 registros.
- **SC-005**: O indicador financeiro mensal reflete exatamente a soma das tarefas abertas
  do mês (tolerância zero em centavos) em todos os cenários de teste.
- **SC-006**: Consultas de lista com até 500 tarefas retornam em menos de 1 segundo na
  percepção do usuário.
- **SC-007**: Toda mensagem de erro ou validação apresentada ao usuário está em português
  e identifica o campo problemático.
- **SC-008**: A documentação interativa publicada cobre 100% das operações disponíveis e
  permite executar cada uma delas com autenticação.

## Assumptions

- **Tarefas são privadas por usuário** (confirmado em clarificação): cada usuário vê e
  manipula apenas as tarefas que criou. Compartilhamento entre usuários está fora do
  escopo; o autor registrado no histórico será sempre o próprio dono da tarefa.
- **"Data de conclusão" informada pelo usuário é a data prevista (prazo)** (confirmado
  em clarificação), usada para identificar atraso. A conclusão efetiva é uma ação
  explícita do usuário que registra uma data/hora real separada.
- A prioridade é uma classificação visual (tag colorida) e **não** altera a ordem da
  lista; a ordem é exclusivamente manual.
- O indicador "total a gastar neste mês" considera apenas tarefas **abertas** com data
  prevista dentro do mês corrente, no fuso horário de Brasília.
- Exclusão é definitiva (sem lixeira/restauração); a confirmação é responsabilidade da
  interface.
- Todos os usuários autenticados possuem o mesmo perfil de acesso ("usuário"); não há
  papel administrativo nesta fase.
- Uma tarefa concluída pode ser reaberta, editada e reordenada livremente (confirmado em
  clarificação); a situação não bloqueia nenhuma operação.
- Volume esperado: uso pessoal, até algumas centenas de tarefas por usuário; por isso a
  lista é entregue completa, sem paginação (confirmado em clarificação).
- O provedor de identidade externo já fornece identificador único e nome de exibição do
  usuário; o cadastro de novos usuários ocorre diretamente nele, fora desta aplicação.
