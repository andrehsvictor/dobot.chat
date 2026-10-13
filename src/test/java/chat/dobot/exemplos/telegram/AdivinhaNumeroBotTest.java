package chat.dobot.exemplos.telegram;

import chat.dobot.bot.Contexto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdivinhaNumeroBot")
class AdivinhaNumeroBotTest {

    private AdivinhaNumeroBot bot;

    @BeforeEach
    void setUp() {
        bot = new AdivinhaNumeroBot();
    }

    @Test
    @DisplayName("main deve inicializar o número secreto no contexto e mudar para 'adivinhar'")
    void mainInicializaNumeroEMudaEstado() {
        Contexto ctx = new Contexto("iniciar", "main", new HashMap<>());
        bot.main(ctx);

        assertEquals("adivinhar", ctx.getEstado());
        assertNotNull(ctx.getDado("numeroSecreto"));
        Integer numero = ctx.getDado("numeroSecreto", Integer.class);
        assertTrue(numero >= 1 && numero <= 10);
        assertFalse(ctx.getRespostas().isEmpty());
    }

    @Test
    @DisplayName("adivinhar com palpite correto deve parabenizar e mudar para 'resultado'")
    void adivinharAcerto() {
        Contexto ctx = new Contexto("5", "adivinhar", new HashMap<>());
        ctx.setDado("numeroSecreto", 5);

        bot.adivinhar(ctx);

        assertEquals("resultado", ctx.getEstado());
        assertTrue(ctx.getRespostas().stream().anyMatch(r -> r.contains("Parabéns")));
        assertNotNull(ctx.getTeclado());
    }

    @Test
    @DisplayName("adivinhar com palpite menor deve avisar e manter 'resultado'")
    void adivinharPalpiteMenor() {
        Contexto ctx = new Contexto("3", "adivinhar", new HashMap<>());
        ctx.setDado("numeroSecreto", 7);

        bot.adivinhar(ctx);

        assertEquals("resultado", ctx.getEstado());
        assertTrue(ctx.getRespostas().stream().anyMatch(r -> r.contains("MAIOR")));
    }

    @Test
    @DisplayName("adivinhar com palpite maior deve avisar e manter 'resultado'")
    void adivinharPalpiteMaior() {
        Contexto ctx = new Contexto("8", "adivinhar", new HashMap<>());
        ctx.setDado("numeroSecreto", 4);

        bot.adivinhar(ctx);

        assertEquals("resultado", ctx.getEstado());
        assertTrue(ctx.getRespostas().stream().anyMatch(r -> r.contains("MENOR")));
    }

    @Test
    @DisplayName("adivinhar com valor inválido deve solicitar número válido e permanecer em 'adivinhar'")
    void adivinharEntradaInvalida() {
        Contexto ctx = new Contexto("abc", "adivinhar", new HashMap<>());
        ctx.setDado("numeroSecreto", 5);

        bot.adivinhar(ctx);

        assertEquals("adivinhar", ctx.getEstado());
        assertTrue(ctx.getRespostas().stream().anyMatch(r -> r.contains("número inteiro")));
    }

    @Test
    @DisplayName("resultado com 'sim' deve iniciar nova rodada no estado 'adivinhar'")
    void resultadoJogarNovamente() {
        Contexto ctx = new Contexto("sim", "resultado", new HashMap<>());
        ctx.setDado("numeroSecreto", 5);

        bot.resultado(ctx);

        assertEquals("adivinhar", ctx.getEstado());
        assertNotNull(ctx.getDado("numeroSecreto"));
    }

    @Test
    @DisplayName("resultado com 'não' deve finalizar e mudar para 'main'")
    void resultadoNaoJogarNovamente() {
        Contexto ctx = new Contexto("não", "resultado", new HashMap<>());
        ctx.setDado("numeroSecreto", 5);

        bot.resultado(ctx);

        assertEquals("main", ctx.getEstado());
        assertNull(ctx.getDado("numeroSecreto"));
    }

    @Test
    @DisplayName("múltiplos usuários não devem compartilhar estado no mesmo bot")
    void multiplosUsuariosNaoCompartilhamEstado() {
        Contexto ctxUser1 = new Contexto("start", "main", new HashMap<>());
        Contexto ctxUser2 = new Contexto("start", "main", new HashMap<>());

        bot.main(ctxUser1);
        ctxUser1.setDado("numeroSecreto", 3);

        bot.main(ctxUser2);
        ctxUser2.setDado("numeroSecreto", 8);

        assertEquals(3, ctxUser1.getDado("numeroSecreto"));
        assertEquals(8, ctxUser2.getDado("numeroSecreto"));

        // User 1 chuta 3 -> acerta!
        Contexto chuteUser1 = new Contexto("3", "adivinhar", new HashMap<>(), ctxUser1.getDados());
        bot.adivinhar(chuteUser1);
        assertTrue(chuteUser1.getRespostas().stream().anyMatch(r -> r.contains("Parabéns")));

        // User 2 chuta 3 -> erra!
        Contexto chuteUser2 = new Contexto("3", "adivinhar", new HashMap<>(), ctxUser2.getDados());
        bot.adivinhar(chuteUser2);
        assertTrue(chuteUser2.getRespostas().stream().anyMatch(r -> r.contains("MAIOR")));
    }
}
