package chat.dobot.bot;

import chat.dobot.bot.domain.DoBot;
import chat.dobot.bot.domain.EstadoInvalidoException;
import chat.dobot.bot.domain.Mensagem;
import chat.dobot.bot.service.DoBotService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Núcleo de execução do DoBot, independente do transporte da mensagem. */
public class DoBotRuntime {

    private final Map<String, DoBot> definicoes;
    private final Map<String, DoBotService<Record>> servicos;
    private final Map<String, DoBot> conversas = new ConcurrentHashMap<>();

    public DoBotRuntime(Map<String, DoBot> definicoes, Map<String, DoBotService<Record>> servicos) {
        this.definicoes = Map.copyOf(definicoes);
        this.servicos = Map.copyOf(servicos);
    }

    public DoBot getBot(String botId) {
        DoBot bot = definicoes.get(botId);
        if (bot == null) {
            throw new DoBotException("Bot não encontrado: " + botId);
        }
        return bot;
    }

    public Map<String, DoBot> getBots() {
        return definicoes;
    }

    public DoBot getConversa(String botId, String conversationId) {
        getBot(botId);
        return conversas.computeIfAbsent(chave(botId, conversationId), chave -> definicoes.get(botId).novaConversa());
    }

    /**
     * Processa uma mensagem recebida por qualquer canal, como web, Telegram ou CLI.
     * Retorna somente as respostas novas do bot para que o adaptador as entregue.
     */
    public List<String> processarMensagem(String botId, String conversationId, String mensagem)
            throws EstadoInvalidoException {
        DoBot conversa = getConversa(botId, conversationId);
        synchronized (conversa) {
            int mensagensAntes = conversa.getMensagens().size();
            conversa.receberMensagem(new Contexto(mensagem, conversa.getEstadoAtual(), servicos));

            List<String> respostas = new ArrayList<>();
            conversa.getMensagens().subList(mensagensAntes, conversa.getMensagens().size()).stream()
                .filter(mensagemNova -> mensagemNova.autor() == Autor.BOT)
                .map(Mensagem::conteudo)
                .forEach(respostas::add);
            return respostas;
        }
    }

    public List<Mensagem> getMensagens(String botId, String conversationId) {
        return List.copyOf(getConversa(botId, conversationId).getMensagens());
    }

    public void limparConversa(String botId, String conversationId) {
        conversas.remove(chave(botId, conversationId));
    }

    private String chave(String botId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("O identificador da conversa não pode ser vazio");
        }
        return botId + "\u0000" + conversationId;
    }
}