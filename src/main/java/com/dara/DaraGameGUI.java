package com.dara;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Interface gráfica principal do jogo Dara
 */
public class DaraGameGUI extends JFrame {
    private static final int BOARD_SIZE = 500;
    private static final Color PLAYER1_COLOR = Color.BLUE;
    private static final Color PLAYER2_COLOR = Color.RED;
    private static final Color EMPTY_COLOR = Color.LIGHT_GRAY;
    private static final Color BOARD_COLOR = Color.WHITE;
    
    private DaraGame game;
    private JPanel boardPanel;
    private JButton[][] boardButtons;
    private JLabel statusLabel;
    private JLabel phaseLabel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton forfeitButton;
    private Position selectedPosition;
    
    // Network components
    private DaraClient client;
    private DaraServer server;
    private boolean isServer;
    private PieceType playerType;
    
    public DaraGameGUI() {
        initializeGame();
        initializeGUI();
        showConnectionDialog();
    }
    
    private void initializeGame() {
        game = new DaraGame();
        selectedPosition = null;
    }
    
    private void initializeGUI() {
        setTitle("Jogo Dara");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Panel principal do tabuleiro
        boardPanel = new JPanel(new GridLayout(5, 6));
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        boardPanel.setBackground(BOARD_COLOR);
        
        boardButtons = new JButton[5][6];
        initializeBoardButtons();
        
        add(boardPanel, BorderLayout.CENTER);
        
        // Panel de informações
        JPanel infoPanel = new JPanel(new BorderLayout());
        
        // Status do jogo
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        phaseLabel = new JLabel("Fase: Colocação");
        statusLabel = new JLabel("Jogador 1 - Aguardando conexão...");
        statusPanel.add(phaseLabel);
        statusPanel.add(statusLabel);
        
        infoPanel.add(statusPanel, BorderLayout.NORTH);
        
        // Chat
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        
        chatArea = new JTextArea(10, 30);
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendChatMessage());
        
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);
        
        infoPanel.add(chatPanel, BorderLayout.CENTER);
        
        // Botão de desistência
        forfeitButton = new JButton("Desistir");
        forfeitButton.addActionListener(e -> forfeitGame());
        forfeitButton.setEnabled(false);
        
        infoPanel.add(forfeitButton, BorderLayout.SOUTH);
        
        add(infoPanel, BorderLayout.EAST);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void initializeBoardButtons() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(80, 80));
                button.setBackground(EMPTY_COLOR);
                button.setBorder(BorderFactory.createRaisedBevelBorder());
                
                final int row = i, col = j;
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handleBoardClick(row, col);
                    }
                });
                
                boardButtons[i][j] = button;
                boardPanel.add(button);
            }
        }
    }
    
    private void handleBoardClick(int row, int col) {
        if (game.isGameEnded() || !isMyTurn()) {
            return;
        }
        
        if (game.isWaitingForCapture()) {
            // Modo captura
            if (client != null) {
                client.sendCapture(row, col);
            } else if (server != null && server.isRunning()) {
                if (game.capturePiece(row, col)) {
                    server.sendCapture(row, col);
                    updateBoard();
                    updateStatus();
                }
            } else {
                game.capturePiece(row, col);
                updateBoard();
                updateStatus();
            }
            return;
        }
        
        if (game.getPhase() == GamePhase.PLACEMENT) {
            // Fase de colocação
            if (client != null) {
                client.sendPlacement(row, col);
            } else if (server != null && server.isRunning()) {
                if (game.makeMove(row, col)) {
                    server.sendPlacement(row, col);
                    updateBoard();
                    updateStatus();
                }
            } else {
                if (game.makeMove(row, col)) {
                    updateBoard();
                    updateStatus();
                }
            }
        } else if (game.getPhase() == GamePhase.MOVEMENT) {
            // Fase de movimentação
            if (selectedPosition == null) {
                // Selecionar peça para mover
                if (game.getBoard().getPiece(row, col) == game.getCurrentPlayer()) {
                    selectedPosition = new Position(row, col);
                    boardButtons[row][col].setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                }
            } else {
                // Mover peça selecionada
                if (client != null) {
                    client.sendMovement(selectedPosition.getRow(), selectedPosition.getCol(), row, col);
                } else if (server != null && server.isRunning()) {
                    if (game.movePiece(selectedPosition.getRow(), selectedPosition.getCol(), row, col)) {
                        server.sendMovement(selectedPosition.getRow(), selectedPosition.getCol(), row, col);
                        updateBoard();
                        updateStatus();
                    }
                } else {
                    if (game.movePiece(selectedPosition.getRow(), selectedPosition.getCol(), row, col)) {
                        updateBoard();
                        updateStatus();
                    }
                }
                clearSelection();
            }
        }
    }
    
    private void clearSelection() {
        if (selectedPosition != null) {
            int row = selectedPosition.getRow();
            int col = selectedPosition.getCol();
            boardButtons[row][col].setBorder(BorderFactory.createRaisedBevelBorder());
            selectedPosition = null;
        }
    }
    
    private boolean isMyTurn() {
        return game.getCurrentPlayer() == playerType;
    }
    
    private void updateBoard() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                PieceType piece = game.getBoard().getPiece(i, j);
                JButton button = boardButtons[i][j];
                
                switch (piece) {
                    case PLAYER1:
                        button.setBackground(PLAYER1_COLOR);
                        button.setText("●");
                        break;
                    case PLAYER2:
                        button.setBackground(PLAYER2_COLOR);
                        button.setText("○");
                        break;
                    default:
                        button.setBackground(EMPTY_COLOR);
                        button.setText("");
                        break;
                }
            }
        }
    }
    
    private void updateStatus() {
        phaseLabel.setText("Fase: " + (game.getPhase() == GamePhase.PLACEMENT ? "Colocação" : "Movimentação"));
        
        String currentPlayerName = game.getCurrentPlayer() == PieceType.PLAYER1 ? "Jogador 1" : "Jogador 2";
        String myPlayerName = playerType == PieceType.PLAYER1 ? "Jogador 1" : "Jogador 2";
        
        if (game.isWaitingForCapture()) {
            statusLabel.setText(currentPlayerName + " deve capturar uma peça!");
        } else if (game.isGameEnded()) {
            String winner = game.getWinner() == PieceType.PLAYER1 ? "Jogador 1" : "Jogador 2";
            statusLabel.setText("Jogo finalizado! Vencedor: " + winner);
            forfeitButton.setEnabled(false);
        } else {
            statusLabel.setText("Vez de: " + currentPlayerName + (isMyTurn() ? " (Sua vez)" : ""));
        }
    }
    
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        if (client != null) {
            client.sendChatMessage(message);
            chatInput.setText("");
        } else if (server != null && server.isRunning()) {
            server.sendChatMessage(message);
            chatInput.setText("");
        }
    }
    
    private void forfeitGame() {
        if (client != null) {
            client.sendForfeit();
            game.forfeit(playerType);
            updateStatus();
        } else if (server != null && server.isRunning()) {
            server.sendForfeit();
            game.forfeit(playerType);
            updateStatus();
        } else {
            game.forfeit(playerType);
            updateStatus();
        }
    }
    
    public void addChatMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private void showConnectionDialog() {
        String[] options = {"Servidor", "Cliente"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Escolha o modo de conexão:",
            "Conectar",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice == 0) {
            // Modo servidor
            startServer();
        } else if (choice == 1) {
            // Modo cliente
            String host = JOptionPane.showInputDialog(this, "Digite o IP do servidor:", "localhost");
            if (host != null) {
                connectToServer(host);
            }
        }
    }
    
    private void startServer() {
        isServer = true;
        playerType = PieceType.PLAYER1;
        
        try {
            server = new DaraServer(this);
            server.start();
            addChatMessage("Servidor iniciado. Aguardando conexões...");
            forfeitButton.setEnabled(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao iniciar servidor: " + e.getMessage());
        }
    }
    
    private void connectToServer(String host) {
        isServer = false;
        playerType = PieceType.PLAYER2;
        
        try {
            client = new DaraClient(host, 12345, this);
            client.start();
            addChatMessage("Tentando conectar ao servidor " + host + ":12345...");
            forfeitButton.setEnabled(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar: " + e.getMessage());
        }
    }
    
    public void onGameMove(GameMessage message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case PLACE_PIECE:
                    if (message.getData() != null && message.getData().length >= 2) {
                        int row = message.getData()[0];
                        int col = message.getData()[1];
                        game.makeMove(row, col);
                    }
                    break;
                case MOVE_PIECE:
                    if (message.getData() != null && message.getData().length >= 4) {
                        int fromRow = message.getData()[0];
                        int fromCol = message.getData()[1];
                        int toRow = message.getData()[2];
                        int toCol = message.getData()[3];
                        game.movePiece(fromRow, fromCol, toRow, toCol);
                    }
                    break;
                case CAPTURE_PIECE:
                    if (message.getData() != null && message.getData().length >= 2) {
                        int row = message.getData()[0];
                        int col = message.getData()[1];
                        game.capturePiece(row, col);
                    }
                    break;
                case CHAT_MESSAGE:
                    addChatMessage("Oponente: " + message.getContent());
                    break;
                case FORFEIT:
                    PieceType opponent = (playerType == PieceType.PLAYER1) ? PieceType.PLAYER2 : PieceType.PLAYER1;
                    game.forfeit(opponent);
                    addChatMessage("Oponente desistiu!");
                    break;
            }
            
            clearSelection();
            updateBoard();
            updateStatus();
        });
    }
    
    public DaraGame getGame() {
        return game;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DaraGameGUI().setVisible(true);
        });
    }
}
