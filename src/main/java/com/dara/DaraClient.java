package com.dara;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Classe cliente para conexão via socket
 */
public class DaraClient extends Thread {
    private String serverHost;
    private int serverPort;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DaraGameGUI gui;
    private boolean connected;
    private BlockingQueue<GameMessage> messageQueue;
    
    public DaraClient(String host, int port, DaraGameGUI gui) {
        this.serverHost = host;
        this.serverPort = port;
        this.gui = gui;
        this.connected = false;
        this.messageQueue = new LinkedBlockingQueue<>();
    }
    
    @Override
    public void run() {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            gui.addChatMessage("Conexao estabelecida com " + serverHost + ":" + serverPort);
            
            // Enviar mensagem de entrada
            sendMessage(new GameMessage(GameMessage.Type.PLAYER_JOIN, "Cliente conectado"));
            
            // Thread para processar mensagens da fila
            Thread messageProcessor = new Thread(this::processMessages);
            messageProcessor.start();
            
            // Loop principal de recebimento
            String inputLine;
            while (connected && (inputLine = in.readLine()) != null) {
                try {
                    GameMessage message = GameMessage.fromString(inputLine);
                    if (message != null) {
                        messageQueue.offer(message);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar mensagem: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            gui.addChatMessage("Falha ao conectar em " + serverHost + ":" + serverPort + " - " + e.getMessage());
            System.err.println("Falha ao conectar em " + serverHost + ":" + serverPort + " - " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private void processMessages() {
        while (connected) {
            try {
                GameMessage message = messageQueue.take();
                gui.onGameMove(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void sendPlacement(int row, int col) {
        GameMessage message = new GameMessage(
            GameMessage.Type.PLACE_PIECE, 
            "Colocação", 
            new int[]{row, col}
        );
        sendMessage(message);
    }
    
    public void sendMovement(int fromRow, int fromCol, int toRow, int toCol) {
        GameMessage message = new GameMessage(
            GameMessage.Type.MOVE_PIECE, 
            "Movimento", 
            new int[]{fromRow, fromCol, toRow, toCol}
        );
        sendMessage(message);
    }
    
    public void sendCapture(int row, int col) {
        GameMessage message = new GameMessage(
            GameMessage.Type.CAPTURE_PIECE, 
            "Captura", 
            new int[]{row, col}
        );
        sendMessage(message);
    }
    
    public void sendChatMessage(String text) {
        GameMessage message = new GameMessage(GameMessage.Type.CHAT_MESSAGE, text);
        sendMessage(message);
        gui.addChatMessage("Você: " + text);
    }
    
    public void sendForfeit() {
        GameMessage message = new GameMessage(GameMessage.Type.FORFEIT, "Desistência");
        sendMessage(message);
    }
    
    private void sendMessage(GameMessage message) {
        if (connected && out != null) {
            out.println(message.toString());
        }
    }
    
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
}
