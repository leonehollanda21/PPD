# Dara - Documentacao de Funcionamento e Roteiro de Testes

Este guia cobre dois objetivos:
1. Explicar como o jogo funciona nesta implementacao Java.
2. Dar um passo a passo de testes para validar todas as funcionalidades pedidas no `1939397-Projeto_1.pdf`.

## 1) Escopo do que o PDF pede

Requisitos principais do enunciado:
- Jogo Dara com comunicacao por sockets (tabuleiros em maquinas diferentes).
- Regras base do Dara:
  - Tabuleiro 5x6.
  - 12 pecas por jogador.
  - Fase de colocacao sem permitir 3 em linha.
  - Fase de movimentacao para casa adjacente (horizontal/vertical).
  - Captura ao formar 3 em linha.
  - Vencedor quando o oponente ficar com 2 pecas.
- Funcionalidades basicas:
  - Controle de turno (com definicao de quem inicia).
  - Movimentacao das pecas.
  - Desistencia.
  - Chat durante a partida.
  - Indicacao de vencedor.

## 2) Tecnologias e arquitetura

- Linguagem: Java.
- UI: Swing (`DaraGameGUI`).
- Logica de jogo: `DaraGame` + `GameBoard`.
- Rede: TCP socket com troca de mensagens texto (`DaraClient`, `DaraServer`, `GameMessage`).
- Porta padrao: `12345`.

Fluxo de alto nivel:
- Instancia A inicia como Servidor (Jogador 1).
- Instancia B inicia como Cliente (Jogador 2) e conecta no IP do servidor.
- Acoes (jogada, captura, chat, desistencia) sao enviadas como `GameMessage`.
- A UI aplica atualizacao de estado no metodo `onGameMove(...)`.

## 3) Como compilar e executar

### 3.1 Pre-requisitos

- Java 8+ instalado.
- Porta `12345` liberada no firewall (se for testar entre maquinas).

### 3.2 Compilar

No Windows, na pasta raiz do projeto:

```bat
compile.bat
```

Comando manual equivalente:

```bat
javac -d bin -cp src\main\java src\main\java\com\dara\*.java
```

### 3.3 Executar

Opcao A (classes compiladas):

```bat
run.bat
```

Opcao B (comando manual):

```bat
java -cp bin com.dara.DaraGameGUI
```

Opcao C (JAR, se disponivel e valido no seu ambiente):

```bat
run-jar.bat
```

## 4) Como o jogo funciona

### 4.1 Fase de colocacao (`PLACEMENT`)

- Cada jogador coloca 1 peca por turno.
- Jogador 1 inicia a partida.
- A fase termina quando ambos colocarem 12 pecas.
- Regra aplicada: se uma colocacao formar 3 em linha, a jogada e rejeitada.

### 4.2 Fase de movimentacao (`MOVEMENT`)

- Jogador seleciona uma peca sua e move para casa vazia adjacente (cima/baixo/esquerda/direita).
- Nao pode mover na diagonal.
- Nao pode mover para casa ocupada.

### 4.3 Captura

- Se um movimento formar 3 em linha (horizontal/vertical), o jogador entra em estado de captura obrigatoria.
- Nesse estado, deve clicar em uma peca adversaria para remover do tabuleiro.
- Depois da captura, o turno passa ao outro jogador (se o jogo nao tiver terminado).

### 4.4 Fim de jogo (`GAME_OVER`)

- O jogo termina quando um jogador fica com 2 pecas no tabuleiro.
- Tambem pode terminar por desistencia.
- A UI exibe o vencedor no status.

### 4.5 Chat e desistencia

- Chat: enviar mensagem pelo campo de texto e Enter.
- Desistencia: botao "Desistir" encerra a partida e declara vencedor o oponente.

## 5) Matriz de rastreabilidade (PDF -> teste)

| ID | Requisito do PDF | Cenario de teste | Evidencia esperada |
|---|---|---|---|
| R1 | Controle de turno com quem inicia | Iniciar servidor e cliente; tentar jogar fora do turno | Apenas jogador da vez consegue jogar; jogador 1 inicia |
| R2 | Movimentacao das pecas | Chegar na fase `MOVEMENT` e mover para casa adjacente | Movimento valido aceito; invalido (diagonal/ocupada) rejeitado |
| R3 | Desistencia | Durante a partida, clicar em "Desistir" | Partida encerrada e vencedor definido como oponente |
| R4 | Chat durante partida | Enviar mensagens dos dois lados | Mensagens aparecem na area de chat do outro lado |
| R5 | Indicacao de vencedor | Capturar ate deixar oponente com 2 pecas | Status mostra "Jogo finalizado" e vencedor |
| R6 | Sem 3 em linha na colocacao | Tentar criar 3 em linha na fase `PLACEMENT` | Jogada rejeitada (peca nao permanece) |
| R7 | Captura apos 3 em linha na movimentacao | Formar 3 em linha em `MOVEMENT` | Sistema exige captura antes de proximo turno |

## 6) Passo a passo completo de teste (manual)

Use 2 instancias do jogo (A e B). Pode ser no mesmo PC.

### 6.1 Preparacao

1. Abra dois terminais na pasta do projeto.
2. No terminal 1, execute o jogo e escolha `Servidor`.
3. No terminal 2, execute o jogo e escolha `Cliente`.
4. Informe `localhost` para teste local.
5. Verifique no chat/status se a conexao foi estabelecida.

### 6.2 Teste de turno e jogador inicial (R1)

1. Com os dois conectados, confirme que o Jogador 1 inicia.
2. Tente clicar no tabuleiro do lado que nao esta na vez.
3. Confirme que a acao fora do turno nao altera o tabuleiro.

### 6.3 Teste da fase de colocacao (R6)

1. Faça jogadas alternadas para preencher pecas.
2. Em algum momento, tente uma colocacao que faria 3 em linha.
3. Confirme que a jogada e recusada.
4. Continue ate ambos completarem 12 pecas.
5. Confirme mudanca de fase para `Movimentacao`.

### 6.4 Teste de movimentacao valida/invalida (R2)

1. Selecione uma peca propria.
2. Tente mover para:
   - Casa adjacente vazia (deve aceitar).
   - Casa diagonal (deve rejeitar).
   - Casa ocupada (deve rejeitar).
3. Verifique alternancia de turno apos movimento valido.

### 6.5 Teste de captura apos 3 em linha (R7)

1. Monte uma situacao para formar 3 em linha durante `MOVEMENT`.
2. Execute o movimento que cria a linha.
3. Confirme mensagem de captura obrigatoria.
4. Clique em uma peca do oponente para capturar.
5. Confirme que o turno so troca apos a captura.

### 6.6 Teste de chat (R4)

1. Envie mensagem de A para B.
2. Envie resposta de B para A.
3. Confirme exibicao das mensagens em ambos os chats.

### 6.7 Teste de desistencia (R3)

1. Inicie uma nova partida.
2. Em um dos lados, clique em `Desistir`.
3. Confirme encerramento imediato da partida e vencedor no status.

### 6.8 Teste de fim por numero de pecas (R5)

1. Jogue ate capturar pecas suficientes para deixar o oponente com 2.
2. Confirme estado `GAME_OVER`.
3. Confirme exibicao do vencedor.

## 7) Checklist de validacao final

Marque cada item durante a demonstracao:

- [ ] Servidor sobe e aceita conexao de cliente.
- [ ] Cliente conecta informando IP/host.
- [ ] Jogador 1 inicia e turno alterna corretamente.
- [ ] Colocacao bloqueia 3 em linha na fase inicial.
- [ ] Transicao para fase de movimentacao apos 12+12 pecas.
- [ ] Movimento apenas para casa adjacente vazia.
- [ ] Captura obrigatoria apos formar 3 em linha na movimentacao.
- [ ] Chat funciona durante toda a partida.
- [ ] Desistencia encerra a partida e define vencedor.
- [ ] Vitoria por reduzir oponente a 2 pecas.

## 8) Evidencias recomendadas para entrega

Para cada requisito do PDF, salvar:
- 1 screenshot da tela (status/tabuleiro/chat).
- 1 breve descricao do que foi testado e resultado.
- Opcional: pequeno video mostrando conexao, turno, captura, chat e encerramento.

Isso facilita a apresentacao presencial e a avaliacao por funcionalidade.

