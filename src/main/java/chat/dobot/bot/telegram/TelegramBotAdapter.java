package chat.dobot.bot.telegram;

import chat.dobot.bot.Autor;
import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotException;
import chat.dobot.bot.domain.DoBot;
import chat.dobot.bot.domain.EstadoInvalidoException;
import chat.dobot.bot.service.DoBotService;
import chat.dobot.bot.utils.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador interno que conecta a API de Long Polling do Telegram ao motor de
 * estados do DoBot.
 *
 * <p>Cada usuário (chatId) possui um estado de sessão independente.
 * Quando uma mensagem chega, o adaptador:
 * <ol>
 *   <li>Recupera (ou inicializa) o estado atual da sessão para aquele chatId;</li>
 *   <li>Constrói um {@link Contexto} com a mensagem recebida;</li>
 *   <li>Delega ao {@link DoBot#receberMensagem(Contexto)};</li>
 *   <li>Envia todas as respostas produzidas pelo bot de volta ao Telegram.</li>
 * </ol>
 */
class TelegramBotAdapter extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotAdapter.class);

    /** Estado atual da sessão por chatId */
    private final Map<Long, String> sessoes = new ConcurrentHashMap<>();

    private final String botToken;
    private final String botUsername;
    private final DoBot doBot;
    private final Map<String, DoBotService<Record>> servicos;

    TelegramBotAdapter(String botToken,
                       String botUsername,
                       DoBot doBot,
                       Map<String, DoBotService<Record>> servicos) {
        super();
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.doBot = doBot;
        this.servicos = servicos;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Ponto de entrada para cada atualização recebida do Telegram.
     * Apenas mensagens de texto são processadas.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String msgUsuario = update.getMessage().getText();

        logger.debug("Mensagem recebida do chatId {}: {}", chatId, msgUsuario);

        // Sessão nova: envia a mensagem inicial do bot e registra o estado inicial
        boolean sessaoNova = !sessoes.containsKey(chatId);
        String estadoAtual = sessoes.getOrDefault(chatId, DoBot.ESTADO_INICIAL);

        if (sessaoNova) {
            sessoes.put(chatId, DoBot.ESTADO_INICIAL);
            String mensagemInicial = doBot.getConfig().getMensagemInicial();
            if (mensagemInicial != null && !mensagemInicial.isBlank()) {
                enviarMensagem(chatId, mensagemInicial);
            }
        }

        // Processa a mensagem do usuário através do motor de estados do DoBot
        Contexto contexto = new Contexto(msgUsuario, estadoAtual, servicos);
        try {
            doBot.receberMensagem(contexto);
            // Atualiza o estado da sessão com o estado resultante
            sessoes.put(chatId, contexto.getEstado());
        } catch (EstadoInvalidoException e) {
            logger.warn("Estado inválido para chatId {}: {}", chatId, e.getMessage());
            ConsoleUtil.printWarning("Estado inválido: " + e.getMessage());
            enviarMensagem(chatId, "⚠️ Ocorreu um erro no processamento. Reiniciando a conversa...");
            sessoes.put(chatId, DoBot.ESTADO_INICIAL);
            enviarMensagem(chatId, doBot.getConfig().getMensagemInicial());
            return;
        } catch (DoBotException e) {
            logger.error("Erro DoBot para chatId {}: {}", chatId, e.getMessage());
            ConsoleUtil.printErro("Erro DoBot: " + e.getMessage(), e);
            enviarMensagem(chatId, "🚨 Ocorreu um erro inesperado. Por favor, tente novamente.");
            return;
        } catch (Exception e) {
            logger.error("Erro inesperado para chatId {}: {}", chatId, e.getMessage(), e);
            ConsoleUtil.printErro("Erro inesperado: " + e.getMessage(), e);
            enviarMensagem(chatId, "🚨🚨 Ocorreu um erro inesperado. Por favor, tente novamente.");
            return;
        }

        // Envia todas as respostas produzidas pelo bot
        for (String resposta : contexto.getRespostas()) {
            enviarMensagem(chatId, resposta);
        }
    }

    /**
     * Envia uma mensagem de texto para o chatId especificado.
     *
     * @param chatId identificador da conversa Telegram
     * @param texto  conteúdo da mensagem a enviar
     */
    private void enviarMensagem(long chatId, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        SendMessage mensagem = new SendMessage();
        mensagem.setChatId(String.valueOf(chatId));
        mensagem.setText(texto);
        try {
            execute(mensagem);
        } catch (TelegramApiException e) {
            logger.error("Falha ao enviar mensagem para chatId {}: {}", chatId, e.getMessage(), e);
            ConsoleUtil.printErro("Falha ao enviar mensagem para chatId " + chatId, e);
        }
    }
}
