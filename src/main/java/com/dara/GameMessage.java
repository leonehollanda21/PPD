package com.dara;

/**
 * Classe que representa as mensagens trocadas via socket
 */
public class GameMessage {
    public enum Type {
        PLACE_PIECE,
        MOVE_PIECE, 
        CAPTURE_PIECE,
        CHAT_MESSAGE,
        FORFEIT,
        GAME_STATE_UPDATE,
        PLAYER_JOIN,
        PLAYER_DISCONNECT,
        ERROR
    }
    
    private Type type;
    private String content;
    private int[] data; // Para coordenadas e outros dados numéricos
    
    public GameMessage(Type type, String content) {
        this.type = type;
        this.content = content;
    }
    
    public GameMessage(Type type, String content, int[] data) {
        this.type = type;
        this.content = content;
        this.data = data;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public int[] getData() {
        return data;
    }
    
    public void setData(int[] data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        if (content != null && !content.isEmpty()) {
            sb.append(":").append(content);
        }
        if (data != null) {
            sb.append(":DATA:");
            for (int i = 0; i < data.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(data[i]);
            }
        }
        return sb.toString();
    }
    
    public static GameMessage fromString(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length < 1) return null;
        
        Type type = Type.valueOf(parts[0]);
        String content = parts.length > 1 ? parts[1] : "";
        
        int[] data = null;
        if (parts.length > 2 && parts[2].startsWith("DATA:")) {
            String dataStr = parts[2].substring(5);
            if (!dataStr.isEmpty()) {
                String[] dataNumbers = dataStr.split(",");
                data = new int[dataNumbers.length];
                for (int i = 0; i < dataNumbers.length; i++) {
                    data[i] = Integer.parseInt(dataNumbers[i]);
                }
            }
        }
        
        return new GameMessage(type, content, data);
    }
}
