package chat.dobot.bot.telegram;

import chat.dobot.bot.domain.DoBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelegramBotAdapter")
class TelegramBotAdapterTest {

    private DoBot doBot;
    private TestableTelegramBotAdapter adapter;

    static class TestableTelegramBotAdapter extends TelegramBotAdapter {
        final List<SendMessage> mensagensEnviadas = new ArrayList<>();
        final List<AnswerCallbackQuery> callbacksRespondidos = new ArrayList<>();

        TestableTelegramBotAdapter(DoBot doBot) {
            super("fake-token", "test_bot", doBot, new HashMap<>());
        }

        @Override
        void enviarMensagem(long chatId, String texto, ReplyKeyboard replyMarkup) {
            if (texto == null || texto.isBlank()) return;
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(texto);
            if (replyMarkup != null) {
                msg.setReplyMarkup(replyMarkup);
            }
            mensagensEnviadas.add(msg);
        }

        @Override
        void responderCallbackQuery(String callbackQueryId, String texto, boolean showAlert) {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            answer.setText(texto);
            answer.setShowAlert(showAlert);
            callbacksRespondidos.add(answer);
        }
    }

    private Update criarUpdate(long chatId, String texto) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        message.setText(texto);
        update.setMessage(message);
        return update;
    }

    private Update criarUpdateCallback(long chatId, String data, String callbackId) {
        Update update = new Update();
        org.telegram.telegrambots.meta.api.objects.CallbackQuery cb = new org.telegram.telegrambots.meta.api.objects.CallbackQuery();
        cb.setId(callbackId);
        cb.setData(data);

        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        cb.setMessage(message);

        org.telegram.telegrambots.meta.api.objects.User user = new org.telegram.telegrambots.meta.api.objects.User();
        user.setId(chatId);
        cb.setFrom(user);

        update.setCallbackQuery(cb);
        return update;
    }

    @BeforeEach
    void setUp() {
        doBot = new DoBot("testBot", "Bot Teste", "Descrição");
        doBot.getConfig().setMensagemInicial("Bem-vindo ao bot!");
        adapter = new TestableTelegramBotAdapter(doBot);
    }

    @Test
    @DisplayName("deve isolar estado e dados entre usuários diferentes")
    void isolaSessaoEntreUsuarios() {
        doBot.setEstados(Map.of(
                "main", ctx -> {
                    ctx.setDado("numero", ctx.getMensagemUsuario().equals("usuario1") ? 10 : 20);
                    ctx.responder("Guardado");
                    ctx.mudarEstado("verificar");
                },
                "verificar", ctx -> {
                    int num = ctx.getDado("numero", Integer.class);
                    ctx.responder("Seu numero é " + num);
                }
        ));

        // Usuario 1 inicia
        adapter.onUpdateReceived(criarUpdate(100L, "usuario1"));
        // Usuario 2 inicia
        adapter.onUpdateReceived(criarUpdate(200L, "usuario2"));

        TelegramUserSession sessao1 = adapter.getSessoes().get(100L);
        TelegramUserSession sessao2 = adapter.getSessoes().get(200L);

        assertEquals("verificar", sessao1.getEstado());
        assertEquals("verificar", sessao2.getEstado());
        assertEquals(10, sessao1.getDados().get("numero"));
        assertEquals(20, sessao2.getDados().get("numero"));

        // Usuario 1 consulta
        adapter.onUpdateReceived(criarUpdate(100L, "consulta"));
        SendMessage ultimaMsg1 = adapter.mensagensEnviadas.get(adapter.mensagensEnviadas.size() - 1);
        assertEquals("Seu numero é 10", ultimaMsg1.getText());

        // Usuario 2 consulta
        adapter.onUpdateReceived(criarUpdate(200L, "consulta"));
        SendMessage ultimaMsg2 = adapter.mensagensEnviadas.get(adapter.mensagensEnviadas.size() - 1);
        assertEquals("Seu numero é 20", ultimaMsg2.getText());
    }

    @Test
    @DisplayName("comando /start deve reiniciar a sessão do usuário e executar estado main")
    void comandoStartReiniciaSessao() {
        doBot.setEstados(Map.of(
                "main", ctx -> {
                    ctx.setDado("partida", 1);
                    ctx.responder("Iniciando nova partida!");
                    ctx.mudarEstado("jogando");
                },
                "jogando", ctx -> {
                    ctx.responder("Jogando...");
                }
        ));

        // Primeira mensagem
        adapter.onUpdateReceived(criarUpdate(100L, "iniciar"));
        assertEquals("jogando", adapter.getSessoes().get(100L).getEstado());
        assertEquals(1, adapter.getSessoes().get(100L).getDados().get("partida"));

        // Usuário envia /start enquanto estava em "jogando"
        adapter.onUpdateReceived(criarUpdate(100L, "/start"));

        assertEquals("jogando", adapter.getSessoes().get(100L).getEstado());
        boolean enviouMsgInicial = adapter.mensagensEnviadas.stream()
                .anyMatch(m -> m.getText().equals("Bem-vindo ao bot!"));
        assertTrue(enviouMsgInicial);
    }

    @Test
    @DisplayName("deve anexar ReplyKeyboardMarkup na última resposta do bot")
    void anexaTecladoNaUltimaResposta() {
        doBot.setEstados(Map.of(
                "main", ctx -> {
                    ctx.responder("Primeira mensagem");
                    ctx.responderComBotoes("Escolha uma opção:", "Opção A", "Opção B");
                }
        ));

        adapter.onUpdateReceived(criarUpdate(100L, "/start"));

        List<SendMessage> msgsChat = adapter.mensagensEnviadas.stream()
                .filter(m -> m.getChatId().equals("100"))
                .toList();

        SendMessage ultima = msgsChat.get(msgsChat.size() - 1);
        assertEquals("Escolha uma opção:", ultima.getText());
        assertNotNull(ultima.getReplyMarkup());
        assertInstanceOf(ReplyKeyboardMarkup.class, ultima.getReplyMarkup());

        SendMessage anterior = msgsChat.get(msgsChat.size() - 2);
        assertNull(anterior.getReplyMarkup());
    }

    @Test
    @DisplayName("deve anexar InlineKeyboardMarkup na última resposta do bot")
    void anexaTecladoInlineNaUltimaResposta() {
        doBot.setEstados(Map.of(
                "main", ctx -> {
                    ctx.responderComBotoesInline("Opções inline:", "A", "B");
                }
        ));

        adapter.onUpdateReceived(criarUpdate(100L, "oi"));

        SendMessage ultima = adapter.mensagensEnviadas.get(adapter.mensagensEnviadas.size() - 1);
        assertEquals("Opções inline:", ultima.getText());
        assertNotNull(ultima.getReplyMarkup());
        assertInstanceOf(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class, ultima.getReplyMarkup());
    }

    @Test
    @DisplayName("deve processar CallbackQuery, avançar estado e responder o callback")
    void processaCallbackQuery() {
        doBot.setEstados(Map.of(
                "main", ctx -> {
                    ctx.responderComBotoesInline("Escolha uma cor:", "Vermelho", "Azul");
                    ctx.mudarEstado("escolha");
                },
                "escolha", ctx -> {
                    assertTrue(ctx.isCallbackQuery());
                    assertEquals("cb_999", ctx.getCallbackQueryId());
                    assertEquals("Azul", ctx.getCallbackData());
                    ctx.setAlertaCallback("Cor azul escolhida!", true);
                    ctx.responder("Você escolheu: " + ctx.getMensagemUsuario());
                    ctx.mudarEstado("finalizado");
                },
                "finalizado", ctx -> {}
        ));

        // Inicia o bot
        adapter.onUpdateReceived(criarUpdate(100L, "start"));
        assertEquals("escolha", adapter.getSessoes().get(100L).getEstado());

        // Usuário clica no botão inline "Azul"
        adapter.onUpdateReceived(criarUpdateCallback(100L, "Azul", "cb_999"));

        // Estado deve ter avançado
        assertEquals("finalizado", adapter.getSessoes().get(100L).getEstado());

        // Mensagem de resposta deve ter sido enviada
        SendMessage resposta = adapter.mensagensEnviadas.get(adapter.mensagensEnviadas.size() - 1);
        assertEquals("Você escolheu: Azul", resposta.getText());

        // Callback query deve ter sido respondida com alerta
        assertEquals(1, adapter.callbacksRespondidos.size());
        AnswerCallbackQuery cbAnswer = adapter.callbacksRespondidos.get(0);
        assertEquals("cb_999", cbAnswer.getCallbackQueryId());
        assertEquals("Cor azul escolhida!", cbAnswer.getText());
        assertTrue(cbAnswer.getShowAlert());
    }

    @Test
    @DisplayName("deve recuperar graciosamente de EstadoInvalidoException")
    void recuperaDeEstadoInvalido() {
        doBot.setEstados(Map.of(
                "main", ctx -> {
                    ctx.mudarEstado("estado_que_nao_existe");
                }
        ));

        adapter.onUpdateReceived(criarUpdate(100L, "/start"));

        assertEquals(DoBot.ESTADO_INICIAL, adapter.getSessoes().get(100L).getEstado());
        boolean enviouAvisoErro = adapter.mensagensEnviadas.stream()
                .anyMatch(m -> m.getText().contains("Ocorreu um erro no processamento"));
        assertTrue(enviouAvisoErro);
    }
}
