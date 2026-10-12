package chat.dobot.bot;

import chat.dobot.bot.service.DoBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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

    /** Record auxiliar para os testes de getServico. */
    record ServicoFake(int id) {}
}
