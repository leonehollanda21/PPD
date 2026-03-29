package com.dara;

/**
 * Classe que representa uma posição no tabuleiro do jogo Dara
 */
public class Position {
    private int row;
    private int col;
    
    /**
     * Cria uma coordenada imutável por convenção lógica (linha, coluna) no tabuleiro.
     */
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }
    
    /**
     * Retorna o índice de linha da posição.
     */
    public int getRow() {
        return row;
    }
    
    /**
     * Retorna o índice de coluna da posição.
     */
    public int getCol() {
        return col;
    }
    
    /**
     * Define igualdade estrutural por coordenadas para uso em coleções e comparações de jogadas.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return row == position.row && col == position.col;
    }
    
    /**
     * Gera hash consistente com equals para estruturas como HashSet e HashMap.
     */
    @Override
    public int hashCode() {
        return row * 31 + col;
    }
    
    /**
     * Retorna representação legível de coordenada para logs e depuração.
     */
    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}
