package com.dara;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe que representa o tabuleiro do jogo Dara
 */
public class GameBoard {
    private static final int ROWS = 5;
    private static final int COLS = 6;
    private PieceType[][] board;
    
    /**
     * Cria a matriz do tabuleiro e inicializa todas as casas como vazias.
     */
    public GameBoard() {
        board = new PieceType[ROWS][COLS];
        initializeBoard();
    }
    
    /**
     * Preenche a estrutura de dados do tabuleiro com EMPTY.
     */
    private void initializeBoard() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = PieceType.EMPTY;
            }
        }
    }
    
    /**
     * Valida limites de linha/coluna para evitar acesso inválido na matriz.
     */
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }
    
    /**
     * Verifica se uma posição existente está livre para receber peça.
     */
    public boolean isEmpty(int row, int col) {
        return isValidPosition(row, col) && board[row][col] == PieceType.EMPTY;
    }
    
    /**
     * Consulta segura do conteúdo de uma casa; fora do tabuleiro retorna EMPTY.
     */
    public PieceType getPiece(int row, int col) {
        if (!isValidPosition(row, col)) {
            return PieceType.EMPTY;
        }
        return board[row][col];
    }
    
    /**
     * Tenta colocar uma peça em casa vazia durante a fase de colocação.
     */
    public boolean placePiece(int row, int col, PieceType piece) {
        if (isEmpty(row, col) && piece != PieceType.EMPTY) {
            board[row][col] = piece;
            return true;
        }
        return false;
    }
    
    /**
     * Move peça ortogonalmente para casa adjacente vazia, conforme regra do Dara.
     * Como é determinístico, o mesmo comando de rede gera o mesmo estado nos dois nós.
     */
    public boolean movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        if (!isValidPosition(fromRow, fromCol) || !isValidPosition(toRow, toCol)) {
            return false;
        }
        
        if (board[fromRow][fromCol] == PieceType.EMPTY || !isEmpty(toRow, toCol)) {
            return false;
        }
        
        // Verifica se é movimento adjacente (horizontal ou vertical)
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        
        if (!((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1))) {
            return false;
        }
        
        PieceType piece = board[fromRow][fromCol];
        board[fromRow][fromCol] = PieceType.EMPTY;
        board[toRow][toCol] = piece;
        return true;
    }
    
    /**
     * Remove peça de uma posição válida (usado em capturas e rollback de jogada inválida).
     */
    public void removePiece(int row, int col) {
        if (isValidPosition(row, col)) {
            board[row][col] = PieceType.EMPTY;
        }
    }
    
    /**
     * Verifica se há uma linha de 3 peças do mesmo tipo
     * @param piece Tipo de peça a verificar
     * @return Lista de posições que formam a linha de 3
     */
    public List<Position> checkThreeInARow(PieceType piece) {
        List<Position> lines = new ArrayList<>();
        
        // Verificar linhas horizontais
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= COLS - 3; col++) {
                if (board[row][col] == piece && 
                    board[row][col + 1] == piece && 
                    board[row][col + 2] == piece) {
                    List<Position> line = new ArrayList<>();
                    line.add(new Position(row, col));
                    line.add(new Position(row, col + 1));
                    line.add(new Position(row, col + 2));
                    lines.addAll(line);
                }
            }
        }
        
        // Verificar linhas verticais
        for (int row = 0; row <= ROWS - 3; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == piece && 
                    board[row + 1][col] == piece && 
                    board[row + 2][col] == piece) {
                    List<Position> line = new ArrayList<>();
                    line.add(new Position(row, col));
                    line.add(new Position(row + 1, col));
                    line.add(new Position(row + 2, col));
                    lines.addAll(line);
                }
            }
        }
        
        return lines;
    }
    
    /**
     * Conta o número de peças de um jogador no tabuleiro
     */
    public int countPieces(PieceType piece) {
        int count = 0;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] == piece) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Retorna uma lista de posições de peças do oponente que podem ser capturadas
     */
    public List<Position> getOpponentPieces(PieceType opponentPiece) {
        List<Position> pieces = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] == opponentPiece) {
                    pieces.add(new Position(i, j));
                }
            }
        }
        return pieces;
    }
    
    /**
     * Constrói representação textual para logs e depuração de estado.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int j = 0; j < COLS; j++) {
            sb.append(j).append(" ");
        }
        sb.append("\n");
        
        for (int i = 0; i < ROWS; i++) {
            sb.append(i).append(" ");
            for (int j = 0; j < COLS; j++) {
                char symbol = switch (board[i][j]) {
                    case PLAYER1 -> '●';
                    case PLAYER2 -> '○';
                    default -> '·';
                };
                sb.append(symbol).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
