package com.dara;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Componente cliente da arquitetura distribuída do jogo.
 * Responsável por abrir socket TCP para o servidor, serializar comandos locais
 * e desserializar eventos remotos mantendo o estado sincronizado entre processos.
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
    
    /**
     * Inicializa os dados de conexão e a fila concorrente usada entre thread de rede e thread de processamento.
     */
    public DaraClient(String host, int port, DaraGameGUI gui) {
        this.serverHost = host;
        this.serverPort = port;
        this.gui = gui;
        this.connected = false;
        this.messageQueue = new LinkedBlockingQueue<>();
    }
    
    /**
     * Thread principal do cliente: conecta no servidor, lê o socket continuamente e publica mensagens na fila.
     * Esse desenho evita processar regras de jogo diretamente na mesma thread de I/O.
     */
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
    
    /**
     * Consome mensagens da fila bloqueante e entrega para a GUI aplicar a lógica local de sincronização.
     */
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
    
    /**
     * Encapsula uma jogada de colocação no protocolo de mensagens distribuídas.
     */
    public void sendPlacement(int row, int col) {
        GameMessage message = new GameMessage(
            GameMessage.Type.PLACE_PIECE, 
            "Colocação", 
            new int[]{row, col}
        );
        sendMessage(message);
    }
    
    /**
     * Encapsula uma jogada de movimentação no protocolo e envia ao servidor autoritativo.
     */
    public void sendMovement(int fromRow, int fromCol, int toRow, int toCol) {
        GameMessage message = new GameMessage(
            GameMessage.Type.MOVE_PIECE, 
            "Movimento", 
            new int[]{fromRow, fromCol, toRow, toCol}
        );
        sendMessage(message);
    }
    
    /**
     * Encapsula uma captura para que ambos os nós apliquem a mesma transição de estado.
     */
    public void sendCapture(int row, int col) {
        GameMessage message = new GameMessage(
            GameMessage.Type.CAPTURE_PIECE, 
            "Captura", 
            new int[]{row, col}
        );
        sendMessage(message);
    }
    
    /**
     * Envia texto de chat para o outro processo e também registra localmente a mensagem enviada.
     */
    public void sendChatMessage(String text) {
        GameMessage message = new GameMessage(GameMessage.Type.CHAT_MESSAGE, text);
        sendMessage(message);
        gui.addChatMessage("Você: " + text);
    }
    
    /**
     * Notifica desistência ao par remoto para finalizar a sessão de jogo de forma consistente.
     */
    public void sendForfeit() {
        GameMessage message = new GameMessage(GameMessage.Type.FORFEIT, "Desistência");
        sendMessage(message);
    }
    
    /**
     * Ponto único de escrita no socket; protege contra envio quando a conexão não está ativa.
     */
    private void sendMessage(GameMessage message) {
        if (connected && out != null) {
            out.println(message.toString());
        }
    }
    
    /**
     * Encerra recursos de rede e marca o cliente como desconectado para parar loops concorrentes.
     */
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
    
    /**
     * Informa para a camada de UI se o canal distribuído está pronto para troca de jogadas.
     */
    public boolean isConnected() {
        return connected;
    }
}
