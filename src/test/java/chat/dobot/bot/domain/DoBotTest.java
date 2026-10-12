package chat.dobot.bot.domain;

import chat.dobot.bot.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DoBot")
class DoBotTest {

    private DoBot bot;
    private Map<String, BotStateMethod> estadosBasicos;

    @BeforeEach
    void setUp() {
        bot = new DoBot("bot-test", "Bot de Teste", "Descrição do bot de teste");
        estadosBasicos = new HashMap<>();
        estadosBasicos.put("main", ctx -> ctx.responder("Olá do estado principal!"));
    }

    // ── Construtor ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve armazenar id, nome e descrição corretamente")
    void construtorArmazenaDadosCorretamente() {
        assertEquals("bot-test", bot.getId());
        assertEquals("Bot de Teste", bot.getNome());
        assertEquals("Descrição do bot de teste", bot.getDescricao());
    }

    @Test
    @DisplayName("deve inicializar com lista de mensagens vazia")
    void construtorInicializaMensagensVazia() {
        assertTrue(bot.getMensagens().isEmpty());
    }

    // ── setEstados ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("setEstados deve definir os estados e o estado inicial como 'main'")
    void setEstadosDefineEstadoInicialMain() {
        bot.setEstados(estadosBasicos);
        assertEquals("main", bot.getEstadoAtual());
    }

    @Test
    @DisplayName("setEstados sem estado 'main' deve lançar DoBotException")
    void setEstadosSemMainLancaExcecao() {
        Map<String, BotStateMethod> semMain = new HashMap<>();
        semMain.put("outro", ctx -> {});

        assertThrows(DoBotException.class, () -> bot.setEstados(semMain));
    }

    @Test
    @DisplayName("getEstados deve retornar os nomes dos estados registrados")
    void getEstadosRetornaChavesRegistradas() {
        estadosBasicos.put("cadastro", ctx -> {});
        bot.setEstados(estadosBasicos);

        assertTrue(bot.getEstados().contains("main"));
        assertTrue(bot.getEstados().contains("cadastro"));
    }

    // ── setEstadoAtual ───────────────────────────────────────────────────────

    @Test
    @DisplayName("setEstadoAtual com estado existente deve alterar estado atual")
    void setEstadoAtualComEstadoExistente() {
        estadosBasicos.put("cadastro", ctx -> {});
        bot.setEstados(estadosBasicos);

        bot.setEstadoAtual("cadastro");
        assertEquals("cadastro", bot.getEstadoAtual());
    }

    @Test
    @DisplayName("setEstadoAtual deve normalizar para letras minúsculas (requer chave já em minúsculo no mapa)")
    void setEstadoAtualNormalizaParaMinusculas() {
        // DoBot valida a existência da chave antes de normalizar, portanto
        // o mapa de estados deve conter a chave em minúsculo.
        estadosBasicos.put("cadastro", ctx -> {});
        bot.setEstados(estadosBasicos);

        // A chave "cadastro" existe; "CADASTRO" não existe → deve lançar exceção
        assertThrows(EstadoInvalidoException.class, () -> bot.setEstadoAtual("CADASTRO"));
    }

    @Test
    @DisplayName("setEstadoAtual com estado inexistente deve lançar EstadoInvalidoException")
    void setEstadoAtualComEstadoInexistenteLancaExcecao() {
        bot.setEstados(estadosBasicos);
        assertThrows(EstadoInvalidoException.class, () -> bot.setEstadoAtual("nao-existe"));
    }

    @Test
    @DisplayName("setEstadoAtual com null deve lançar IllegalArgumentException")
    void setEstadoAtualComNullLancaExcecao() {
        bot.setEstados(estadosBasicos);
        assertThrows(IllegalArgumentException.class, () -> bot.setEstadoAtual(null));
    }

    @Test
    @DisplayName("getEstadoAtual antes de setEstados deve lançar IllegalStateException")
    void getEstadoAtualSemEstadosLancaExcecao() {
        assertThrows(IllegalStateException.class, () -> bot.getEstadoAtual());
    }

    // ── receberMensagem ──────────────────────────────────────────────────────

    @Test
    @DisplayName("receberMensagem deve executar o handler do estado e registrar mensagens")
    void receberMensagemExecutaHandlerERegistraMensagens() {
        bot.setEstados(estadosBasicos);

        Contexto ctx = new Contexto("oi", "main", new HashMap<>());
        bot.receberMensagem(ctx);

        // Uma mensagem do usuário + uma resposta do bot
        assertEquals(2, bot.getMensagens().size());
        assertEquals(Autor.USUARIO, bot.getMensagens().get(0).autor());
        assertEquals("oi", bot.getMensagens().get(0).conteudo());
        assertEquals(Autor.BOT, bot.getMensagens().get(1).autor());
        assertEquals("Olá do estado principal!", bot.getMensagens().get(1).conteudo());
    }

    @Test
    @DisplayName("receberMensagem com estado inválido deve lançar DoBotException")
    void receberMensagemComEstadoInvalidoLancaExcecao() {
        bot.setEstados(estadosBasicos);

        Contexto ctx = new Contexto("oi", "estado-que-nao-existe", new HashMap<>());
        assertThrows(DoBotException.class, () -> bot.receberMensagem(ctx));
    }

    // ── setMensagemInicial ───────────────────────────────────────────────────

    @Test
    @DisplayName("setMensagemInicial deve adicionar mensagem do BOT à lista")
    void setMensagemInicialAdicionaMensagemBot() {
        bot.setMensagemInicial("Bem-vindo!");

        assertEquals(1, bot.getMensagens().size());
        assertEquals(Autor.BOT, bot.getMensagens().get(0).autor());
        assertEquals("Bem-vindo!", bot.getMensagens().get(0).conteudo());
    }

    // ── addMensagem ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addMensagem deve adicionar mensagem com o autor e conteúdo corretos")
    void addMensagemAdicionaCorretamente() {
        bot.addMensagem(Autor.USUARIO, "Olá!");

        assertEquals(1, bot.getMensagens().size());
        assertEquals(Autor.USUARIO, bot.getMensagens().get(0).autor());
        assertEquals("Olá!", bot.getMensagens().get(0).conteudo());
    }

    // ── getDoBotTema ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDoBotTema deve retornar um tema não-nulo com valores padrão")
    void getDoBotTemaRetornaTemaNaoNulo() {
        assertNotNull(bot.getDoBotTema());
        assertNotNull(bot.getDoBotTema().getCorFundoPagina());
    }
}
