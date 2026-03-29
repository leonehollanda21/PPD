package com.dara;

import java.util.List;

/**
 * Classe principal que controla a lógica do jogo Dara
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
     * Realiza uma jogada (colocação ou movimento)
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
     * Realiza movimento de uma peça
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
     * Coloca uma peça no tabuleiro (fase de colocação)
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
     * Verifica se o jogador atual formou 3 em linha
     */
    private void checkForThreeInARow() {
        List<Position> threeInARow = board.checkThreeInARow(currentPlayer);
        if (!threeInARow.isEmpty()) {
            waitingForCapture = true;
        }
    }
    
    /**
     * Captura uma peça do oponente
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
     * Verifica se o jogo terminou
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
     * Muda para o próximo jogador
     */
    private void switchPlayer() {
        currentPlayer = (currentPlayer == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
    }
    
    /**
     * Permite que um jogador desista
     */
    public void forfeit(PieceType player) {
        winner = (player == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
        gameEnded = true;
        phase = GamePhase.GAME_OVER;
    }
    
    // Getters
    public GameBoard getBoard() {
        return board;
    }
    
    public GamePhase getPhase() {
        return phase;
    }
    
    public PieceType getCurrentPlayer() {
        return currentPlayer;
    }
    
    public int getPlayer1PiecesPlaced() {
        return player1PiecesPlaced;
    }
    
    public int getPlayer2PiecesPlaced() {
        return player2PiecesPlaced;
    }
    
    public int getPiecesRemaining(PieceType player) {
        if (player == PieceType.PLAYER1) {
            return TOTAL_PIECES - player1PiecesPlaced;
        } else if (player == PieceType.PLAYER2) {
            return TOTAL_PIECES - player2PiecesPlaced;
        }
        return 0;
    }
    
    public boolean isGameEnded() {
        return gameEnded;
    }
    
    public PieceType getWinner() {
        return winner;
    }
    
    public boolean isWaitingForCapture() {
        return waitingForCapture;
    }
    
    public List<Position> getOpponentPieces(PieceType currentPlayer) {
        PieceType opponent = (currentPlayer == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
        return board.getOpponentPieces(opponent);
    }
    
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
