package com.dara;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Camada de apresentação Swing do jogo.
 * Integra ações do usuário com o modelo local e com a camada de rede,
 * respeitando as regras de thread do Swing para evitar condições de corrida na UI.
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
    
    /**
     * Constrói a janela principal, inicializa estado local e inicia o fluxo de conexão distribuída.
     */
    public DaraGameGUI() {
        initializeGame();
        initializeGUI();
        showConnectionDialog();
    }
    
    /**
     * Inicializa o estado de domínio do jogo no processo local.
     */
    private void initializeGame() {
        game = new DaraGame();
        selectedPosition = null;
    }
    
    /**
     * Monta os componentes Swing da tela e registra listeners de interação.
     */
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
    
    /**
     * Cria grade visual do tabuleiro e associa cada botão à coordenada correspondente.
     */
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
    
    /**
     * Trata clique no tabuleiro conforme fase do jogo e modo de execução (local, servidor ou cliente).
     * Em rede, a GUI envia comando e a aplicação da jogada ocorre pela mensagem recebida para manter ordem causal.
     */
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
    
    /**
     * Limpa destaque visual de peça selecionada na fase de movimentação.
     */
    private void clearSelection() {
        if (selectedPosition != null) {
            int row = selectedPosition.getRow();
            int col = selectedPosition.getCol();
            boardButtons[row][col].setBorder(BorderFactory.createRaisedBevelBorder());
            selectedPosition = null;
        }
    }
    
    /**
     * Verifica se o turno lógico corresponde ao jogador desta instância.
     */
    private boolean isMyTurn() {
        return game.getCurrentPlayer() == playerType;
    }
    
    /**
     * Re-renderiza o tabuleiro com base no estado atual do modelo de jogo.
     */
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
    
    /**
     * Atualiza rótulos de fase/turno e mensagens de fim de jogo para feedback imediato ao usuário.
     */
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
    
    /**
     * Envia chat para o peer conectado e limpa caixa de entrada.
     */
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
    
    /**
     * Processa desistência local e propaga evento ao outro nó quando há conexão ativa.
     */
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
    
    /**
     * Adiciona texto no histórico do chat e mantém rolagem no fim.
     */
    public void addChatMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * Abre diálogo inicial para escolher papel distribuído da instância (servidor ou cliente).
     */
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
    
    /**
     * Inicializa endpoint servidor desta GUI e define o jogador local como PLAYER1.
     */
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
    
    /**
     * Inicializa endpoint cliente, conecta ao servidor informado e define jogador local como PLAYER2.
     */
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
    
    /**
     * Callback de mensagens de rede. Usa invokeLater para garantir que toda mutação de UI
     * e aplicação de eventos aconteçam na Event Dispatch Thread (sincronização correta do Swing).
     */
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
    
    /**
     * Expõe o modelo de jogo para inspeção/testes externos da GUI.
     */
    public DaraGame getGame() {
        return game;
    }
    
    /**
     * Ponto de entrada da aplicação desktop.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DaraGameGUI().setVisible(true);
        });
    }
}
