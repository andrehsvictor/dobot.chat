package chat.dobot.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DoBotException")
class DoBotExceptionTest {

    @Test
    @DisplayName("deve criar exceção com mensagem")
    void criaExcecaoComMensagem() {
        DoBotException ex = new DoBotException("Erro de teste");
        assertEquals("Erro de teste", ex.getMessage());
    }

    @Test
    @DisplayName("deve criar exceção com mensagem e causa")
    void criaExcecaoComMensagemECausa() {
        RuntimeException causa = new RuntimeException("causa original");
        DoBotException ex = new DoBotException("Erro com causa", causa);

        assertEquals("Erro com causa", ex.getMessage());
        assertSame(causa, ex.getCause());
    }

    @Test
    @DisplayName("deve ser uma RuntimeException")
    void deveSerRuntimeException() {
        DoBotException ex = new DoBotException("teste");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
