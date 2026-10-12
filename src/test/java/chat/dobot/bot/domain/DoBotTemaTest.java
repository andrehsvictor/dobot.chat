package chat.dobot.bot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DoBotTema")
class DoBotTemaTest {

    // ── Valores padrão ───────────────────────────────────────────────────────

    @Test
    @DisplayName("construtor padrão deve definir cores padrão corretamente")
    void construtorPadraoDefineCorePadrao() {
        DoBotTema tema = new DoBotTema();

        assertEquals("#ffffff", tema.getCorFundoPagina());
        assertEquals("#D5D5D5", tema.getCorFundoChat());
        assertEquals("#000000", tema.getCorTextoChat());
        assertEquals("#FEB600", tema.getCorFundoMensagemUsuario());
        assertEquals("#EE8134", tema.getCorFundoMensagemBot());
    }

    // ── Construtor parametrizado ─────────────────────────────────────────────

    @Test
    @DisplayName("construtor parametrizado deve armazenar todos os valores corretamente")
    void construtorParametrizadoArmazenaValores() {
        DoBotTema tema = new DoBotTema("#111111", "#222222", "#333333", "#444444", "#555555");

        assertEquals("#111111", tema.getCorFundoPagina());
        assertEquals("#222222", tema.getCorFundoChat());
        assertEquals("#333333", tema.getCorTextoChat());
        assertEquals("#444444", tema.getCorFundoMensagemUsuario());
        assertEquals("#555555", tema.getCorFundoMensagemBot());
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setCorFundoPagina deve alterar a cor do fundo da página")
    void setCorFundoPaginaAlteraValor() {
        DoBotTema tema = new DoBotTema();
        tema.setCorFundoPagina("#aabbcc");
        assertEquals("#aabbcc", tema.getCorFundoPagina());
    }

    @Test
    @DisplayName("setCorFundoChat deve alterar a cor do fundo do chat")
    void setCorFundoChatAlteraValor() {
        DoBotTema tema = new DoBotTema();
        tema.setCorFundoChat("#112233");
        assertEquals("#112233", tema.getCorFundoChat());
    }

    @Test
    @DisplayName("setCorTextoChat deve alterar a cor do texto do chat")
    void setCorTextoChatAlteraValor() {
        DoBotTema tema = new DoBotTema();
        tema.setCorTextoChat("#ffffff");
        assertEquals("#ffffff", tema.getCorTextoChat());
    }

    @Test
    @DisplayName("setCorFundoMensagemUsuario deve alterar a cor do fundo da mensagem do usuário")
    void setCorFundoMensagemUsuarioAlteraValor() {
        DoBotTema tema = new DoBotTema();
        tema.setCorFundoMensagemUsuario("#abc123");
        assertEquals("#abc123", tema.getCorFundoMensagemUsuario());
    }

    @Test
    @DisplayName("setCorFundoMensagemBot deve alterar a cor do fundo da mensagem do bot")
    void setCorFundoMensagemBotAlteraValor() {
        DoBotTema tema = new DoBotTema();
        tema.setCorFundoMensagemBot("#ff0000");
        assertEquals("#ff0000", tema.getCorFundoMensagemBot());
    }
}
