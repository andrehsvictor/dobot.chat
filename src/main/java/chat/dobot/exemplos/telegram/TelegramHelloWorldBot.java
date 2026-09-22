package chat.dobot.exemplos.telegram;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotChatApp;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@DoBotChat(
        id = "telegramHello",
        nome = "Telegram Hello World",
        descricao = "Bot de exemplo do DoBot.chat para Telegram"
)
public class TelegramHelloWorldBot {

    public static void main(String[] args) throws TelegramApiException {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        String username = System.getenv("TELEGRAM_BOT_USERNAME");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Defina a variavel TELEGRAM_BOT_TOKEN antes de iniciar o bot");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Defina a variavel TELEGRAM_BOT_USERNAME antes de iniciar o bot");
        }

        DoBotChatApp app = DoBotChatApp.novoBot();
        app.ativarBot("telegramHello");
        app.startTelegram("telegramHello", username, token);
    }

    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial("Olá! Eu sou um bot Telegram criado com o DoBot.chat. Envie uma mensagem.");
    }

    @EstadoChat(inicial = true)
    public void responder(Contexto contexto) {
        contexto.responder("Você disse: " + contexto.getMensagemUsuario());
    }
}
