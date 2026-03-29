# Jogo Dara - Projeto de Sockets

## Documentacao completa (funcionamento + testes)

Consulte o guia detalhado em `docs/GUIA_FUNCIONAMENTO_E_TESTES.md` para:
- explicacao do funcionamento do jogo e da arquitetura;
- matriz de rastreabilidade requisito -> teste -> evidencia;
- passo a passo de validacao de todas as funcionalidades do `1939397-Projeto_1.pdf`.

## Descrição

Implementação do jogo Dara utilizando Sockets para comunicação entre os tabuleiros que podem estar em máquinas diferentes.

O Dara é um jogo de estratégia de tabuleiro de origem africana (Nigéria/Níger), jogado por duas pessoas com o objetivo de alinhar três peças (na horizontal ou vertical) para capturar as peças do oponente.

## Funcionalidades Implementadas

✅ **Controle de turno** - Com definição de quem inicia a partida  
✅ **Movimentação das peças** - Nos tabuleiros  
✅ **Desistência** - Permite que um jogador desista  
✅ **Chat** - Para comunicação durante toda a partida  
✅ **Indicação de vencedor** - Quando o jogo termina  
✅ **Interface Gráfica** - Interface amigável usando Java Swing  
✅ **Comunicação via Sockets** - Entre diferentes máquinas  

## Regras do Jogo

### Tabuleiro
- Composto por 5 linhas e 6 colunas
- Cada jogador começa com 12 peças

### Fases do Jogo

#### 1. Fase de Colocação
- Os jogadores posicionam, alternadamente, uma peça por vez nas intersecções
- Todas as peças devem estar no tabuleiro antes da próxima fase
- **NÃO é permitido formar uma linha de 3 peças nesta fase**

#### 2. Fase de Movimentação
- Após posicionar todas as peças, os jogadores movem uma de suas peças para uma casa adjacente vazia (horizontal ou vertical)
- Ao alinhar 3 peças da mesma cor (horizontal ou vertical), o jogador remove uma peça do oponente do tabuleiro

### Objetivo e Vitória
- O jogo termina quando um jogador tem apenas 2 peças restantes
- Vence quem conseguir reduzir o oponente a 2 peças

## Como Compilar

1. Execute o script de compilação:
```bash
./compile.bat
```

## Como Executar

### Opção 1: Executar JAR (Recomendado)
```bash
java -jar DaraGame.jar
```
ou
```bash
./run-jar.bat
```

### Opção 2: Executar diretamente
```bash
./run.bat
```

### Opção 3: Comando Java manual
```bash
java -cp bin com.dara.DaraGameGUI
```

## Como Jogar

### 1. Iniciar o Jogo
- Execute o programa
- Escolha entre **Servidor** ou **Cliente**

### 2. Configurar Conexão

#### Como Servidor:
- Selecione "Servidor"
- O jogo aguardará conexões na porta 12345
- Você será o Jogador 1 (peças azuis ●)

#### Como Cliente:
- Selecione "Cliente" 
- Digite o IP do servidor (use "localhost" para teste local)
- Você será o Jogador 2 (peças vermelhas ○)

### 3. Jogando

#### Fase de Colocação:
- Clique em uma posição vazia para colocar sua peça
- Não é permitido formar 3 em linha nesta fase
- Continue até colocar todas as 12 peças

#### Fase de Movimentação:
- Clique em uma de suas peças para selecioná-la
- Clique em uma posição adjacente vazia para mover
- Se formar 3 em linha, clique em uma peça do oponente para capturá-la

#### Chat:
- Use o campo de texto na lateral para conversar com o oponente
- Pressione Enter para enviar mensagens

#### Desistência:
- Clique no botão "Desistir" para encerrar o jogo

## Estrutura do Projeto

```
src/main/java/com/dara/
├── DaraGame.java          # Lógica principal do jogo
├── DaraGameGUI.java       # Interface gráfica
├── DaraClient.java        # Cliente Socket
├── DaraServer.java        # Servidor Socket
├── GameBoard.java         # Tabuleiro do jogo
├── GameMessage.java       # Protocolo de mensagens
├── GamePhase.java         # Fases do jogo
├── PieceType.java         # Tipos de peças
└── Position.java          # Posições no tabuleiro
```

## Requisitos

- Java 8 ou superior
- Sistema operacional: Windows (scripts .bat) / Linux/Mac (adaptar scripts)
- Porta 12345 disponível para conexões de rede

## Protocolo de Comunicação

O jogo utiliza um protocolo baseado em texto via TCP Socket:

- `PLACE_PIECE:conteudo:DATA:row,col` - Colocação de peça
- `MOVE_PIECE:conteudo:DATA:fromRow,fromCol,toRow,toCol` - Movimento de peça  
- `CAPTURE_PIECE:conteudo:DATA:row,col` - Captura de peça
- `CHAT_MESSAGE:mensagem` - Mensagem de chat
- `FORFEIT:` - Desistência
- `PLAYER_JOIN:` - Entrada de jogador

## Observações Técnicas

- O jogo utiliza threads separadas para processamento de mensagens
- A interface gráfica é thread-safe usando SwingUtilities.invokeLater()
- Tratamento de erros de conexão e desconexões inesperadas
- Fila de mensagens para processamento assíncrono

## Autor

Implementado para a disciplina de Programação Paralela e Distribuída - IFCE  
Prof. Cidcley T. de Souza

## Data de Entrega

25/03/2026
