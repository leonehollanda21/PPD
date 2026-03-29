package com.dara;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Classe servidor para conexões via socket
 */
public class DaraServer extends Thread {
    private static final int PORT = 12345;
    
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private DaraGameGUI gui;
    private boolean running;
    private BlockingQueue<GameMessage> messageQueue;
    
    public DaraServer(DaraGameGUI gui) {
        this.gui = gui;
        this.running = false;
        this.messageQueue = new LinkedBlockingQueue<>();
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            running = true;
            
            gui.addChatMessage("Servidor iniciado na porta " + PORT);
            gui.addChatMessage("Aguardando conexão de cliente...");
            
            // Aguardar conexão
            clientSocket = serverSocket.accept();
            
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
            gui.addChatMessage("Cliente conectado: " + clientSocket.getInetAddress());
            
            // Thread para processar mensagens da fila
            Thread messageProcessor = new Thread(this::processMessages);
            messageProcessor.start();
            
            // Loop principal de recebimento
            String inputLine;
            while (running && (inputLine = in.readLine()) != null) {
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
            if (running) { // Só mostra erro se não foi parada intencionalmente
                gui.addChatMessage("Erro no servidor: " + e.getMessage());
                System.err.println("Erro no servidor: " + e.getMessage());
            }
        } finally {
            stopServer();
        }
    }
    
    private void processMessages() {
        while (running) {
            try {
                GameMessage message = messageQueue.take();
                gui.onGameMove(message);
                
                // Cliente envia jogada para o servidor: o servidor aplica localmente
                // e devolve a mesma jogada para sincronizar o estado no cliente.
                if (shouldForwardToClient(message)) {
                    sendMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private boolean shouldForwardToClient(GameMessage message) {
        return message != null && switch (message.getType()) {
            case PLACE_PIECE, MOVE_PIECE, CAPTURE_PIECE -> true;
            default -> false;
        };
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
        if (running && out != null) {
            out.println(message.toString());
        }
    }
    
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar servidor: " + e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
