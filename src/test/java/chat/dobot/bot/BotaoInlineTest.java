package chat.dobot.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BotaoInline")
class BotaoInlineTest {

    @Test
    @DisplayName("callback(texto, callbackData) deve criar botão com payload")
    void callbackComData() {
        BotaoInline botao = BotaoInline.callback("Opção 1", "opt_1");

        assertEquals("Opção 1", botao.texto());
        assertEquals("opt_1", botao.callbackData());
        assertNull(botao.url());
    }

    @Test
    @DisplayName("callback(texto) deve criar botão com callbackData igual ao texto")
    void callbackApenasTexto() {
        BotaoInline botao = BotaoInline.callback("Confirmar");

        assertEquals("Confirmar", botao.texto());
        assertEquals("Confirmar", botao.callbackData());
        assertNull(botao.url());
    }

    @Test
    @DisplayName("link(texto, url) deve criar botão com URL externa")
    void linkExterno() {
        BotaoInline botao = BotaoInline.link("Site", "https://dobot.chat");

        assertEquals("Site", botao.texto());
        assertNull(botao.callbackData());
        assertEquals("https://dobot.chat", botao.url());
    }
}
