package com.dara;

import java.util.List;

/**
 * Núcleo de regras do Dara implementado como máquina de estados.
 * Essa lógica é determinística para que a mesma sequência de mensagens de rede
 * produza o mesmo estado no servidor e no cliente.
 */
public class DaraGame {
    private static final int TOTAL_PIECES = 12;
    
    private GameBoard board;
    private GamePhase phase;
    private PieceType currentPlayer;
    private int player1PiecesPlaced;
    private int player2PiecesPlaced;
    private int player1Captures;
    private int player2Captures;
    private boolean gameEnded;
    private PieceType winner;
    private boolean waitingForCapture;
    
    /**
     * Inicializa um novo estado de partida com tabuleiro vazio e fase de colocação.
     */
    public DaraGame() {
        board = new GameBoard();
        phase = GamePhase.PLACEMENT;
        currentPlayer = PieceType.PLAYER1;
        player1PiecesPlaced = 0;
        player2PiecesPlaced = 0;
        player1Captures = 0;
        player2Captures = 0;
        gameEnded = false;
        winner = PieceType.EMPTY;
        waitingForCapture = false;
    }
    
    /**
     * Porta de entrada de jogada simples usada na fase de colocação.
     * Na fase de movimentação, o fluxo completo exige origem e destino e é tratado em movePiece.
     */
    public boolean makeMove(int row, int col) {
        if (gameEnded || waitingForCapture) return false;
        
        if (phase == GamePhase.PLACEMENT) {
            return placePiece(row, col);
        } else if (phase == GamePhase.MOVEMENT) {
            // Para movimentação, precisa selecionar origem primeiro
            return false;
        }
        return false;
    }
    
    /**
     * Executa movimentação na fase de movimento, validando dono da peça e adjacência no tabuleiro.
     * Após uma jogada válida, verifica formação de linha de 3 e alternância de turno.
     */
    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        if (gameEnded || phase != GamePhase.MOVEMENT || waitingForCapture) {
            return false;
        }
        
        if (board.getPiece(fromRow, fromCol) != currentPlayer) {
            return false;
        }
        
        if (board.movePiece(fromRow, fromCol, toRow, toCol)) {
            checkForThreeInARow();
            if (!waitingForCapture) {
                switchPlayer();
            }
            return true;
        }
        return false;
    }
    
    /**
     * Regra da fase de colocação: adiciona peça, bloqueia formação de 3 em linha nessa fase
     * e muda para MOVEMENT quando ambos os jogadores colocam todas as peças.
     */
    private boolean placePiece(int row, int col) {
        if (!board.placePiece(row, col, currentPlayer)) {
            return false;
        }
        
        if (currentPlayer == PieceType.PLAYER1) {
            player1PiecesPlaced++;
        } else {
            player2PiecesPlaced++;
        }
        
        // Verifica se formou 3 em linha na fase de colocação (não permitido)
        List<Position> threeInARow = board.checkThreeInARow(currentPlayer);
        if (!threeInARow.isEmpty()) {
            // Desfaz a jogada
            board.removePiece(row, col);
            if (currentPlayer == PieceType.PLAYER1) {
                player1PiecesPlaced--;
            } else {
                player2PiecesPlaced--;
            }
            return false;
        }
        
        // Verifica se todas as peças foram colocadas
        if (player1PiecesPlaced == TOTAL_PIECES && player2PiecesPlaced == TOTAL_PIECES) {
            phase = GamePhase.MOVEMENT;
        }
        
        switchPlayer();
        return true;
    }
    
    /**
     * Verifica formação de 3 em linha após uma jogada de movimento.
     * Se ocorrer, o turno não troca e o jogador entra no estado obrigatório de captura.
     */
    private void checkForThreeInARow() {
        List<Position> threeInARow = board.checkThreeInARow(currentPlayer);
        if (!threeInARow.isEmpty()) {
            waitingForCapture = true;
        }
    }
    
    /**
     * Remove uma peça adversária quando o jogo está aguardando captura,
     * contabiliza captura e avalia condição de fim de jogo.
     */
    public boolean capturePiece(int row, int col) {
        if (!waitingForCapture) return false;
        
        PieceType opponent = (currentPlayer == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
        
        if (board.getPiece(row, col) != opponent) {
            return false;
        }
        
        board.removePiece(row, col);
        
        if (currentPlayer == PieceType.PLAYER1) {
            player1Captures++;
        } else {
            player2Captures++;
        }
        
        waitingForCapture = false;
        
        // Verifica se o jogo terminou
        checkGameEnd();
        
        if (!gameEnded) {
            switchPlayer();
        }
        
        return true;
    }
    
    /**
     * Regra de término: jogador com 2 ou menos peças no tabuleiro perde a partida.
     */
    private void checkGameEnd() {
        int player1Pieces = board.countPieces(PieceType.PLAYER1);
        int player2Pieces = board.countPieces(PieceType.PLAYER2);
        
        if (player1Pieces <= 2) {
            winner = PieceType.PLAYER2;
            gameEnded = true;
            phase = GamePhase.GAME_OVER;
        } else if (player2Pieces <= 2) {
            winner = PieceType.PLAYER1;
            gameEnded = true;
            phase = GamePhase.GAME_OVER;
        }
    }
    
    /**
     * Alterna o token do jogador atual, representando troca de turno.
     */
    private void switchPlayer() {
        currentPlayer = (currentPlayer == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
    }
    
    /**
     * Encerra imediatamente a partida por desistência e define vencedor como o oponente.
     */
    public void forfeit(PieceType player) {
        winner = (player == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
        gameEnded = true;
        phase = GamePhase.GAME_OVER;
    }
    
    /**
     * Expõe o tabuleiro atual para renderização e validações na interface.
     */
    public GameBoard getBoard() {
        return board;
    }
    
    /**
     * Informa a fase global da máquina de estados da partida.
     */
    public GamePhase getPhase() {
        return phase;
    }
    
    /**
     * Informa de quem é o turno na lógica central do jogo.
     */
    public PieceType getCurrentPlayer() {
        return currentPlayer;
    }
    
    /**
     * Retorna quantas peças o jogador 1 já colocou durante a fase inicial.
     */
    public int getPlayer1PiecesPlaced() {
        return player1PiecesPlaced;
    }
    
    /**
     * Retorna quantas peças o jogador 2 já colocou durante a fase inicial.
     */
    public int getPlayer2PiecesPlaced() {
        return player2PiecesPlaced;
    }
    
    /**
     * Calcula quantas peças ainda faltam para o jogador posicionar na fase de colocação.
     */
    public int getPiecesRemaining(PieceType player) {
        if (player == PieceType.PLAYER1) {
            return TOTAL_PIECES - player1PiecesPlaced;
        } else if (player == PieceType.PLAYER2) {
            return TOTAL_PIECES - player2PiecesPlaced;
        }
        return 0;
    }
    
    /**
     * Informa se a partida já terminou.
     */
    public boolean isGameEnded() {
        return gameEnded;
    }
    
    /**
     * Retorna o vencedor definido ao final da partida.
     */
    public PieceType getWinner() {
        return winner;
    }
    
    /**
     * Indica se o jogador da vez precisa realizar captura antes de passar o turno.
     */
    public boolean isWaitingForCapture() {
        return waitingForCapture;
    }
    
    /**
     * Lista peças do oponente elegíveis para captura com base no jogador informado.
     */
    public List<Position> getOpponentPieces(PieceType currentPlayer) {
        PieceType opponent = (currentPlayer == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
        return board.getOpponentPieces(opponent);
    }
    
    /**
     * Gera uma visão textual do estado da partida para depuração e logs.
     */
    public String getGameStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Fase: ").append(phase.name()).append("\n");
        status.append("Jogador atual: ").append(currentPlayer == PieceType.PLAYER1 ? "Jogador 1" : "Jogador 2").append("\n");
        
        if (phase == GamePhase.PLACEMENT) {
            status.append("Peças restantes - J1: ").append(getPiecesRemaining(PieceType.PLAYER1));
            status.append(", J2: ").append(getPiecesRemaining(PieceType.PLAYER2)).append("\n");
        } else {
            status.append("Peças no tabuleiro - J1: ").append(board.countPieces(PieceType.PLAYER1));
            status.append(", J2: ").append(board.countPieces(PieceType.PLAYER2)).append("\n");
        }
        
        if (waitingForCapture) {
            status.append("Aguardando captura!\n");
        }
        
        if (gameEnded) {
            status.append("Jogo finalizado! Vencedor: ");
            status.append(winner == PieceType.PLAYER1 ? "Jogador 1" : "Jogador 2");
        }
        
        return status.toString();
    }
}
