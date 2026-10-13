package chat.dobot.bot.telegram;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotException;
import chat.dobot.bot.domain.DoBot;
import chat.dobot.bot.domain.EstadoInvalidoException;
import chat.dobot.bot.service.DoBotService;
import chat.dobot.bot.utils.ConsoleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador interno que conecta a API de Long Polling do Telegram ao motor de
 * estados do DoBot.
 *
 * <p>
 * Cada usuário (chatId) possui uma sessão independente
 * ({@link TelegramUserSession})
 * que gerencia o estado da conversa e os dados da sessão de maneira isolada.
 *
 * <p>
 * Principais funcionalidades:
 * <ul>
 * <li><b>Isolamento de Sessão:</b> Dados e estados de cada usuário são
 * completamente independentes;</li>
 * <li><b>Comando /start:</b> Reinicia a sessão do usuário automaticamente,
 * disparando a mensagem inicial e o estado principal;</li>
 * <li><b>Teclados customizados:</b> Suporte a ReplyKeyboardMarkup e InlineKeyboardMarkup;</li>
 * <li><b>Botões Inline & Callbacks:</b> Suporte a cliques em botões inline (CallbackQuery) integrados à máquina de estados;</li>
 * <li><b>Thread safety:</b> Processamento sincronizado por usuário para evitar
 * race conditions em mensagens rápidas simultâneas.</li>
 * </ul>
 */
class TelegramBotAdapter extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotAdapter.class);

    /** Sessões ativas por chatId */
    private final Map<Long, TelegramUserSession> sessoes = new ConcurrentHashMap<>();

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
     * Retorna o mapa de sessões ativas (visibilidade de pacote para testes).
     */
    Map<Long, TelegramUserSession> getSessoes() {
        return sessoes;
    }

    /**
     * Ponto de entrada para cada atualização recebida do Telegram.
     * Trata mensagens de texto e cliques em botões inline (CallbackQuery).
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            processarCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String msgUsuario = update.getMessage().getText();

        logger.debug("Mensagem recebida do chatId {}: {}", chatId, msgUsuario);

        TelegramUserSession sessao = sessoes.computeIfAbsent(chatId,
                id -> new TelegramUserSession(DoBot.ESTADO_INICIAL));

        // Sincroniza apenas no nível da sessão do usuário para garantir ordem por
        // usuário sem travar outros
        synchronized (sessao) {
            processarMensagem(chatId, msgUsuario, sessao);
        }
    }

    /**
     * Processa um clique em botão inline (CallbackQuery).
     */
    private void processarCallbackQuery(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage() != null
                ? callbackQuery.getMessage().getChatId()
                : callbackQuery.getFrom().getId();
        String callbackData = callbackQuery.getData();
        String callbackId = callbackQuery.getId();

        logger.debug("CallbackQuery recebida do chatId {}: data='{}', id='{}'", chatId, callbackData, callbackId);

        TelegramUserSession sessao = sessoes.computeIfAbsent(chatId,
                id -> new TelegramUserSession(DoBot.ESTADO_INICIAL));

        synchronized (sessao) {
            String estadoAtual = sessao.getEstado();
            Contexto contexto = new Contexto(callbackData, estadoAtual, servicos, sessao.getDados());
            contexto.setChatId(chatId);
            contexto.setCallbackQueryId(callbackId);
            contexto.setCallbackData(callbackData);

            executarDoBot(chatId, sessao, contexto);

            // Confirma o clique no botão inline para cessar o spinner de carregamento no Telegram
            responderCallbackQuery(callbackId, contexto.getAlertaCallbackTexto(), contexto.isAlertaCallbackShowAlert());
        }
    }

    /**
     * Processa a mensagem do usuário no contexto de sua sessão.
     */
    private void processarMensagem(long chatId, String msgUsuario, TelegramUserSession sessao) {
        String msgTrim = msgUsuario != null ? msgUsuario.trim() : "";
        boolean isComandoStart = msgTrim.equalsIgnoreCase("/start") || msgTrim.startsWith("/start ");

        // Comando /start: reinicia a sessão do usuário automaticamente
        if (isComandoStart) {
            logger.info("Comando /start recebido para chatId {}. Reiniciando sessão.", chatId);
            sessao.reset(DoBot.ESTADO_INICIAL);

            String mensagemInicial = doBot.getConfig().getMensagemInicial();
            if (mensagemInicial != null && !mensagemInicial.isBlank()) {
                enviarMensagem(chatId, mensagemInicial, null);
            }

            Contexto contexto = new Contexto(msgUsuario, DoBot.ESTADO_INICIAL, servicos, sessao.getDados());
            contexto.setChatId(chatId);

            executarDoBot(chatId, sessao, contexto);
            return;
        }

        // Sessão nova (primeira mensagem não sendo /start): envia mensagem inicial
        if (sessao.isNova()) {
            sessao.setNova(false);
            String mensagemInicial = doBot.getConfig().getMensagemInicial();
            if (mensagemInicial != null && !mensagemInicial.isBlank()) {
                enviarMensagem(chatId, mensagemInicial, null);
            }
        }

        String estadoAtual = sessao.getEstado();
        Contexto contexto = new Contexto(msgUsuario, estadoAtual, servicos, sessao.getDados());
        contexto.setChatId(chatId);

        executarDoBot(chatId, sessao, contexto);
    }

    /**
     * Delega a execução da mensagem para a máquina de estados do DoBot e envia as
     * respostas.
     */
    private void executarDoBot(long chatId, TelegramUserSession sessao, Contexto contexto) {
        try {
            doBot.receberMensagem(contexto);
            // Atualiza o estado da sessão com o estado resultante do contexto
            sessao.setEstado(contexto.getEstado());
        } catch (EstadoInvalidoException e) {
            logger.warn("Estado inválido para chatId {}: {}", chatId, e.getMessage());
            ConsoleUtil.printWarning("Estado inválido: " + e.getMessage());
            enviarMensagem(chatId, "⚠️ Ocorreu um erro no processamento. Reiniciando a conversa...",
                    new ReplyKeyboardRemove(true));
            sessao.reset(DoBot.ESTADO_INICIAL);
            String mensagemInicial = doBot.getConfig().getMensagemInicial();
            if (mensagemInicial != null && !mensagemInicial.isBlank()) {
                enviarMensagem(chatId, mensagemInicial, null);
            }
            return;
        } catch (DoBotException e) {
            logger.error("Erro DoBot para chatId {}: {}", chatId, e.getMessage());
            ConsoleUtil.printErro("Erro DoBot: " + e.getMessage(), e);
            enviarMensagem(chatId, "🚨 Ocorreu um erro inesperado. Por favor, tente novamente.", null);
            return;
        } catch (Exception e) {
            logger.error("Erro inesperado para chatId {}: {}", chatId, e.getMessage(), e);
            ConsoleUtil.printErro("Erro inesperado: " + e.getMessage(), e);
            enviarMensagem(chatId, "🚨🚨 Ocorreu um erro inesperado. Por favor, tente novamente.", null);
            return;
        }

        enviarRespostas(chatId, contexto);
    }

    /**
     * Envia todas as respostas produzidas pelo bot, anexando qualquer teclado
     * especificado
     * à última resposta do lote.
     */
    private void enviarRespostas(long chatId, Contexto contexto) {
        List<String> respostas = contexto.getRespostas();
        ReplyKeyboard teclado = contexto.getTeclado() instanceof ReplyKeyboard rk ? rk : null;

        for (int i = 0; i < respostas.size(); i++) {
            String resposta = respostas.get(i);
            boolean ultima = (i == respostas.size() - 1);
            ReplyKeyboard markup = ultima ? teclado : null;
            enviarMensagem(chatId, resposta, markup);
        }
    }

    /**
     * Envia uma mensagem de texto simples para o chatId especificado.
     *
     * @param chatId identificador da conversa Telegram
     * @param texto  conteúdo da mensagem a enviar
     */
    void enviarMensagem(long chatId, String texto) {
        enviarMensagem(chatId, texto, null);
    }

    /**
     * Envia uma mensagem de texto com teclado (ou remoção de teclado) para o chatId
     * especificado.
     *
     * @param chatId      identificador da conversa Telegram
     * @param texto       conteúdo da mensagem a enviar
     * @param replyMarkup teclado customizado ou remoção de teclado (pode ser null)
     */
    void enviarMensagem(long chatId, String texto, ReplyKeyboard replyMarkup) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        SendMessage mensagem = new SendMessage();
        mensagem.setChatId(String.valueOf(chatId));
        mensagem.setText(texto);
        if (replyMarkup != null) {
            mensagem.setReplyMarkup(replyMarkup);
        }
        try {
            execute(mensagem);
        } catch (TelegramApiException e) {
            logger.error("Falha ao enviar mensagem para chatId {}: {}", chatId, e.getMessage(), e);
            ConsoleUtil.printErro("Falha ao enviar mensagem para chatId " + chatId, e);
        }
    }

    /**
     * Responde a uma CallbackQuery para confirmar o clique no botão inline.
     *
     * @param callbackQueryId identificador da query
     * @param texto           mensagem de alerta/toast opcional (pode ser null)
     * @param showAlert       se true, exibe alerta modal; se false, toast discreto
     */
    void responderCallbackQuery(String callbackQueryId, String texto, boolean showAlert) {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        if (texto != null && !texto.isBlank()) {
            answer.setText(texto);
            answer.setShowAlert(showAlert);
        }
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Falha ao responder callback query {}: {}", callbackQueryId, e.getMessage(), e);
        }
    }
}
