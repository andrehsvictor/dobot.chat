package chat.dobot.bot;

import chat.dobot.bot.domain.DoBotTema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DoBotConfig")
class DoBotConfigTest {

    // ── Construtor padrão ────────────────────────────────────────────────────

    @Test
    @DisplayName("construtor padrão deve definir mensagemInicial como string vazia")
    void construtorPadraoMensagemInicialVazia() {
        DoBotConfig config = new DoBotConfig();
        assertEquals("", config.getMensagemInicial());
    }

    @Test
    @DisplayName("construtor padrão deve criar um tema não-nulo")
    void construtorPadraoTemaNaoNulo() {
        DoBotConfig config = new DoBotConfig();
        assertNotNull(config.getTema());
    }

    // ── Construtor parametrizado ─────────────────────────────────────────────

    @Test
    @DisplayName("construtor parametrizado deve armazenar mensagem e tema")
    void construtorParametrizadoArmazenaValores() {
        DoBotTema tema = new DoBotTema("#000000", "#111111", "#222222", "#333333", "#444444");
        DoBotConfig config = new DoBotConfig("Olá, como posso ajudar?", tema);

        assertEquals("Olá, como posso ajudar?", config.getMensagemInicial());
        assertSame(tema, config.getTema());
    }

    // ── setMensagemInicial ───────────────────────────────────────────────────

    @Test
    @DisplayName("setMensagemInicial deve atualizar o valor")
    void setMensagemInicialAtualizaValor() {
        DoBotConfig config = new DoBotConfig();
        config.setMensagemInicial("Nova mensagem!");
        assertEquals("Nova mensagem!", config.getMensagemInicial());
    }
}
