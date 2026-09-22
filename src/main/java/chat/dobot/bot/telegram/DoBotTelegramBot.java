package chat.dobot.bot.telegram;

import chat.dobot.bot.DoBotException;
import chat.dobot.bot.DoBotRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

/** Adaptador Telegram baseado em long polling para um bot DoBot. */
public class DoBotTelegramBot extends TelegramLongPollingBot {

    private static final int LIMITE_MENSAGEM_TELEGRAM = 4096;
    private static final Logger logger = LoggerFactory.getLogger(DoBotTelegramBot.class);

    private final DoBotRuntime runtime;
    private final String botId;
    private final String username;

    public DoBotTelegramBot(DoBotRuntime runtime, String botId, String username, String token) {
        super(validar(token));
        this.runtime = Objects.requireNonNull(runtime, "runtime não pode ser nulo");
        this.botId = Objects.requireNonNull(botId, "botId não pode ser nulo");
        this.username = Objects.requireNonNull(username, "username não pode ser nulo");
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String conversationId = String.valueOf(update.getMessage().getChatId());
        String texto = update.getMessage().getText();
        try {
            runtime.processarMensagem(botId, conversationId, texto)
                    .forEach(resposta -> enviarRespostas(update.getMessage().getChatId(), resposta));
        } catch (Exception exception) {
            logger.error("Erro ao processar mensagem Telegram para o bot {}", botId, exception);
            enviarRespostas(update.getMessage().getChatId(),
                    "Ocorreu um erro ao processar sua mensagem. Tente novamente.");
        }
    }

    private void enviarRespostas(Long chatId, String resposta) {
        if (resposta == null || resposta.isBlank()) {
            return;
        }
        for (int inicio = 0; inicio < resposta.length(); inicio += LIMITE_MENSAGEM_TELEGRAM) {
            int fim = Math.min(inicio + LIMITE_MENSAGEM_TELEGRAM, resposta.length());
            SendMessage mensagem = new SendMessage();
            mensagem.setChatId(chatId.toString());
            mensagem.setText(resposta.substring(inicio, fim));
            try {
                execute(mensagem);
            } catch (TelegramApiException exception) {
                throw new DoBotException("Falha ao enviar resposta pelo Telegram", exception);
            }
        }
    }

    private static String validar(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("O token do Telegram não pode ser vazio");
        }
        return token;
    }
}