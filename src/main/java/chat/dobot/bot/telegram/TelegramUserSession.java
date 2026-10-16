package chat.dobot.bot.telegram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa o estado da sessão de um usuário específico do Telegram.
 *
 * <p>Cada usuário (identificado pelo {@code chatId}) possui sua própria sessão,
 * contendo o estado atual da máquina de estados do bot e um mapa de dados/atributos
 * isolado para armazenar dados da conversa (ex: pontuação, número secreto, preferências).
 */
class TelegramUserSession {

    private String estado;
    private final Map<String, Object> dados = new ConcurrentHashMap<>();
    private boolean nova = true;

    TelegramUserSession(String estadoInicial) {
        this.estado = estadoInicial;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Map<String, Object> getDados() {
        return dados;
    }

    public boolean isNova() {
        return nova;
    }

    public void setNova(boolean nova) {
        this.nova = nova;
    }

    /**
     * Reinicializa a sessão para o estado inicial, limpando todos os dados da sessão.
     * Usado, por exemplo, no comando {@code /start}.
     *
     * @param estadoInicial nome do estado inicial (ex: {@code "main"})
     */
    public void reset(String estadoInicial) {
        this.estado = estadoInicial;
        this.dados.clear();
        this.nova = false;
    }
}
