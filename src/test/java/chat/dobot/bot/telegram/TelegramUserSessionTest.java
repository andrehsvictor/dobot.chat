package chat.dobot.bot.telegram;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelegramUserSession")
class TelegramUserSessionTest {

    @Test
    @DisplayName("deve inicializar com o estado inicial e nova=true")
    void inicializaComEstadoInicialENova() {
        TelegramUserSession sessao = new TelegramUserSession("main");

        assertEquals("main", sessao.getEstado());
        assertTrue(sessao.isNova());
        assertTrue(sessao.getDados().isEmpty());
    }

    @Test
    @DisplayName("deve permitir alterar o estado da sessão")
    void alteraEstado() {
        TelegramUserSession sessao = new TelegramUserSession("main");
        sessao.setEstado("jogando");

        assertEquals("jogando", sessao.getEstado());
    }

    @Test
    @DisplayName("deve permitir armazenar e recuperar dados da sessão")
    void armazenaERecuperaDados() {
        TelegramUserSession sessao = new TelegramUserSession("main");
        sessao.getDados().put("numeroSecreto", 42);

        assertEquals(42, sessao.getDados().get("numeroSecreto"));
    }

    @Test
    @DisplayName("reset deve redefinir o estado, limpar dados e marcar nova=false")
    void resetLimpaDadosERedefineEstado() {
        TelegramUserSession sessao = new TelegramUserSession("main");
        sessao.setEstado("resultado");
        sessao.getDados().put("numeroSecreto", 7);
        sessao.setNova(true);

        sessao.reset("main");

        assertEquals("main", sessao.getEstado());
        assertTrue(sessao.getDados().isEmpty());
        assertFalse(sessao.isNova());
    }
}
