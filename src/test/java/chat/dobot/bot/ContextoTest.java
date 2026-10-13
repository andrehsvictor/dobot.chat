package chat.dobot.bot;

import chat.dobot.bot.service.DoBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("Contexto")
class ContextoTest {

    private Map<String, DoBotService<Record>> servicosVazios;

    @BeforeEach
    void setUp() {
        servicosVazios = new HashMap<>();
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve normalizar o estado para letras minúsculas no construtor")
    void construtorNormalizaEstadoParaMinusculas() {
        Contexto ctx = new Contexto("oi", "MAIN", servicosVazios);
        assertEquals("main", ctx.getEstado());
    }

    @Test
    @DisplayName("deve armazenar a mensagem do usuário")
    void construtorArmazenaMensagemUsuario() {
        Contexto ctx = new Contexto("olá mundo", "main", servicosVazios);
        assertEquals("olá mundo", ctx.getMensagemUsuario());
    }

    @Test
    @DisplayName("deve inicializar a lista de respostas vazia")
    void construtorInicializaListaRespostasVazia() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        assertTrue(ctx.getRespostas().isEmpty());
    }

    // ── mudarEstado ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("mudarEstado deve alterar o estado atual")
    void mudarEstadoAlteraEstado() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.mudarEstado("cadastro");
        assertEquals("cadastro", ctx.getEstado());
    }

    // ── responder ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("responder deve adicionar a resposta à lista")
    void responderAdicionaResposta() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.responder("Olá!");
        assertEquals(1, ctx.getRespostas().size());
        assertEquals("Olá!", ctx.getRespostas().get(0));
    }

    @Test
    @DisplayName("múltiplas chamadas a responder devem acumular respostas na ordem")
    void multiplosResponderAcumulamRespostas() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.responder("Primeira");
        ctx.responder("Segunda");
        ctx.responder("Terceira");

        assertEquals(3, ctx.getRespostas().size());
        assertEquals("Primeira", ctx.getRespostas().get(0));
        assertEquals("Segunda",  ctx.getRespostas().get(1));
        assertEquals("Terceira", ctx.getRespostas().get(2));
    }

    // ── getServico ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getServico deve lançar DoBotException quando o serviço não existe")
    void getServicoLancaExcecaoQuandoServicoNaoEncontrado() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);

        DoBotException ex = assertThrows(DoBotException.class,
                () -> ctx.getServico(ServicoFake.class));

        assertTrue(ex.getMessage().contains("ServicoFake"));
    }

    @Test
    @DisplayName("getServico deve retornar o serviço quando ele está registrado")
    @SuppressWarnings("unchecked")
    void getServicoRetornaServicoRegistrado() {
        DoBotService<Record> servicoMock = mock(DoBotService.class);

        Map<String, DoBotService<Record>> servicos = new HashMap<>();
        servicos.put("ServicoFake", servicoMock);

        Contexto ctx = new Contexto("oi", "main", servicos);
        DoBotService<ServicoFake> result = ctx.getServico(ServicoFake.class);

        assertNotNull(result);
    }

    // ── Dados / Atributos da Sessão ──────────────────────────────────────────

    @Test
    @DisplayName("deve armazenar e recuperar dados da sessão com set/get e setDado/getDado")
    void armazenaERecuperaDadosSessao() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.set("pontos", 100);
        ctx.setDado("nome", "Alice");

        assertEquals(100, ctx.get("pontos"));
        assertEquals("Alice", ctx.getDado("nome"));
    }

    @Test
    @DisplayName("deve recuperar dados com conversão de tipo tipada")
    void recuperaDadosComTipoTipado() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.setDado("numeroSecreto", 7);
        ctx.setDado("ativo", true);
        ctx.setDado("score", 42L);

        Integer num = ctx.getDado("numeroSecreto", Integer.class);
        Boolean ativo = ctx.getDado("ativo", Boolean.class);
        Long score = ctx.getDado("score", Long.class);

        assertEquals(7, num);
        assertTrue(ativo);
        assertEquals(42L, score);
    }

    @Test
    @DisplayName("deve retornar valor padrão quando chave não existe")
    void retornaValorPadraoQuandoNaoExiste() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);

        assertEquals(10, ctx.getDado("inexistente", 10));
        assertEquals("default", ctx.get("outroInexistente", "default"));
    }

    @Test
    @DisplayName("deve verificar existência de chave com has e temDado")
    void verificaExistenciaChave() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.set("chave", "valor");

        assertTrue(ctx.has("chave"));
        assertTrue(ctx.temDado("chave"));
        assertFalse(ctx.has("nao_existe"));
    }

    @Test
    @DisplayName("deve remover dado com remove e removerDado")
    void removeDadoSessao() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.set("temp", "valor");
        assertEquals("valor", ctx.removerDado("temp"));
        assertFalse(ctx.has("temp"));
    }

    @Test
    @DisplayName("limparDados deve esvaziar todos os dados")
    void limparDadosEsvaziaMapa() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.set("a", 1);
        ctx.set("b", 2);
        ctx.limparDados();

        assertTrue(ctx.getDados().isEmpty());
    }

    @Test
    @DisplayName("getChatId e setChatId devem persistir chatId no contexto")
    void getESetChatId() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.setChatId(123456789L);

        assertEquals(123456789L, ctx.getChatId());
    }

    // ── Teclados (ReplyKeyboardMarkup) ───────────────────────────────────────

    @Test
    @DisplayName("responderComBotoes deve configurar ReplyKeyboardMarkup com botões")
    void responderComBotoesConfiguraReplyKeyboardMarkup() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.responderComBotoes("Quer jogar novamente?", "Sim", "Não");

        assertEquals(1, ctx.getRespostas().size());
        assertEquals("Quer jogar novamente?", ctx.getRespostas().get(0));
        assertNotNull(ctx.getTeclado());
        assertInstanceOf(org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup.class, ctx.getTeclado());

        org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup markup =
                (org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup) ctx.getTeclado();
        assertTrue(markup.getResizeKeyboard());
        assertEquals(1, markup.getKeyboard().size());
        assertEquals(2, markup.getKeyboard().get(0).size());
    }

    @Test
    @DisplayName("removerTeclado deve configurar ReplyKeyboardRemove")
    void removerTecladoConfiguraReplyKeyboardRemove() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.removerTeclado("Tchau!");

        assertEquals(1, ctx.getRespostas().size());
        assertEquals("Tchau!", ctx.getRespostas().get(0));
        assertNotNull(ctx.getTeclado());
        assertInstanceOf(org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove.class, ctx.getTeclado());
    }

    // ── Teclados Inline (InlineKeyboardMarkup) ───────────────────────────────

    @Test
    @DisplayName("responderComBotoesInline deve configurar InlineKeyboardMarkup com botões")
    void responderComBotoesInlineConfiguraMarkup() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        ctx.responderComBotoesInline("Escolha uma opção:", "Opção 1", "Opção 2", "Opção 3");

        assertEquals(1, ctx.getRespostas().size());
        assertEquals("Escolha uma opção:", ctx.getRespostas().get(0));
        assertNotNull(ctx.getTeclado());
        assertInstanceOf(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class, ctx.getTeclado());

        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup =
                (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) ctx.getTeclado();
        assertEquals(1, markup.getKeyboard().size());
        assertEquals(3, markup.getKeyboard().get(0).size());
        assertEquals("Opção 1", markup.getKeyboard().get(0).get(0).getText());
        assertEquals("Opção 1", markup.getKeyboard().get(0).get(0).getCallbackData());
    }

    @Test
    @DisplayName("responderComBotoesInline com mapa deve configurar texto e callbackData customizados")
    void responderComBotoesInlineComMapa() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        Map<String, String> opcoes = Map.of("✅ Sim", "sim", "❌ Não", "nao");
        ctx.responderComBotoesInline("Confirma?", opcoes);

        assertNotNull(ctx.getTeclado());
        assertInstanceOf(org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup.class, ctx.getTeclado());
    }

    @Test
    @DisplayName("responderComGradeBotoesInline deve configurar botões de callback e links externos")
    void responderComGradeBotoesInline() {
        Contexto ctx = new Contexto("oi", "main", servicosVazios);
        List<List<BotaoInline>> grade = List.of(
                List.of(BotaoInline.callback("Ação 1", "act_1"), BotaoInline.link("Site", "https://dobot.chat"))
        );
        ctx.responderComGradeBotoesInline("Menu:", grade);

        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup =
                (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) ctx.getTeclado();
        assertEquals(1, markup.getKeyboard().size());
        assertEquals(2, markup.getKeyboard().get(0).size());
        assertEquals("act_1", markup.getKeyboard().get(0).get(0).getCallbackData());
        assertEquals("https://dobot.chat", markup.getKeyboard().get(0).get(1).getUrl());
    }

    @Test
    @DisplayName("metadados de CallbackQuery devem ser persistidos no Contexto")
    void metadadosCallbackQuery() {
        Contexto ctx = new Contexto("btn_click", "main", servicosVazios);
        assertFalse(ctx.isCallbackQuery());

        ctx.setCallbackQueryId("cb_123");
        ctx.setCallbackData("btn_click");
        ctx.setAlertaCallback("Ação processada!", true);

        assertTrue(ctx.isCallbackQuery());
        assertEquals("cb_123", ctx.getCallbackQueryId());
        assertEquals("btn_click", ctx.getCallbackData());
        assertEquals("Ação processada!", ctx.getAlertaCallbackTexto());
        assertTrue(ctx.isAlertaCallbackShowAlert());
    }

    /** Record auxiliar para os testes de getServico. */
    record ServicoFake(int id) {}
}
