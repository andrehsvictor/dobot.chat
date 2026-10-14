package chat.dobot.exemplos.telegram;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.service.DoBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CafeGourmetBot")
class CafeGourmetBotTest {

    private CafeGourmetBot bot;
    private DoBotService<ItemPedido> servicoMock;
    private Map<String, DoBotService<Record>> servicos;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        bot = new CafeGourmetBot();
        servicoMock = mock(DoBotService.class);
        servicos = new HashMap<>();
        servicos.put("ItemPedido", (DoBotService) servicoMock);
    }

    @Test
    @DisplayName("deve configurar a mensagem inicial corretamente")
    void deveConfigurarMensagemInicial() {
        DoBotConfig config = new DoBotConfig();
        bot.config(config);
        assertNotNull(config.getMensagemInicial());
        assertTrue(config.getMensagemInicial().contains("Café Gourmet"));
    }

    @Test
    @DisplayName("deve enviar o cardápio com botões inline no estado inicial")
    void deveEnviarCardapioComBotoesInline() {
        Contexto ctx = new Contexto("olá", "menu", servicos, new HashMap<>());

        bot.menu(ctx);

        assertEquals("trataracao", ctx.getEstado());
        assertFalse(ctx.getRespostas().isEmpty());
        assertNotNull(ctx.getTeclado());
    }

    @Test
    @DisplayName("deve adicionar item ao banco H2 e emitir toast de confirmação")
    void deveAdicionarItemAoBancoH2() throws Exception {
        Contexto ctx = new Contexto("", "tratarAcao", servicos, new HashMap<>());
        ctx.setCallbackQueryId("query123");
        ctx.setCallbackData("add_espresso");

        bot.tratarAcao(ctx);

        verify(servicoMock, times(1)).salvar(any(ItemPedido.class));
        assertNotNull(ctx.getAlertaCallbackTexto());
        assertTrue(ctx.getAlertaCallbackTexto().contains("Espresso"));
        assertFalse(ctx.isAlertaCallbackShowAlert()); // Toast discreto
        assertFalse(ctx.getRespostas().isEmpty());
    }

    @Test
    @DisplayName("deve listar pedidos do banco H2 com botões de remoção individual")
    void deveListarPedidosDoBancoH2() throws Exception {
        List<ItemPedido> pedidos = List.of(
                new ItemPedido(1, "☕ Espresso", 6.00, "Café"),
                new ItemPedido(2, "🥐 Croissant", 12.00, "Lanche")
        );
        when(servicoMock.buscarTodos()).thenReturn(pedidos);

        Contexto ctx = new Contexto("", "tratarAcao", servicos, new HashMap<>());
        ctx.setCallbackData("ver_pedidos");

        bot.tratarAcao(ctx);

        verify(servicoMock, times(1)).buscarTodos();
        String resposta = ctx.getRespostas().get(0);
        assertTrue(resposta.contains("Espresso"));
        assertTrue(resposta.contains("Croissant"));
        assertTrue(resposta.contains("18.00") || resposta.contains("18,00")); // Total
        assertNotNull(ctx.getTeclado());
    }

    @Test
    @DisplayName("deve remover item por ID do banco H2")
    void deveRemoverItemPorId() throws Exception {
        Contexto ctx = new Contexto("", "tratarAcao", servicos, new HashMap<>());
        ctx.setCallbackData("del_1");

        bot.tratarAcao(ctx);

        verify(servicoMock, times(1)).deletarPorId(1);
        assertNotNull(ctx.getAlertaCallbackTexto());
        assertTrue(ctx.getAlertaCallbackTexto().contains("#1"));
    }

    @Test
    @DisplayName("deve limpar todos os pedidos do banco H2 e disparar modal")
    void deveLimparTodosPedidos() throws Exception {
        List<ItemPedido> pedidos = List.of(
                new ItemPedido(1, "☕ Espresso", 6.00, "Café"),
                new ItemPedido(2, "🥐 Croissant", 12.00, "Lanche")
        );
        when(servicoMock.buscarTodos()).thenReturn(pedidos);

        Contexto ctx = new Contexto("", "tratarAcao", servicos, new HashMap<>());
        ctx.setCallbackData("limpar");

        bot.tratarAcao(ctx);

        verify(servicoMock, times(1)).deletarPorId(1);
        verify(servicoMock, times(1)).deletarPorId(2);
        assertTrue(ctx.isAlertaCallbackShowAlert()); // Modal popup
        assertTrue(ctx.getAlertaCallbackTexto().contains("removidos"));
    }

    @Test
    @DisplayName("deve finalizar pedido calculando total e esvaziando o banco H2")
    void deveFinalizarPedidoComSucesso() throws Exception {
        List<ItemPedido> pedidos = List.of(
                new ItemPedido(1, "☕ Espresso", 6.00, "Café"),
                new ItemPedido(2, "🍰 Cheesecake", 15.00, "Doce")
        );
        when(servicoMock.buscarTodos()).thenReturn(pedidos);

        Contexto ctx = new Contexto("", "tratarAcao", servicos, new HashMap<>());
        ctx.setCallbackData("finalizar");

        bot.tratarAcao(ctx);

        verify(servicoMock, times(1)).deletarPorId(1);
        verify(servicoMock, times(1)).deletarPorId(2);
        assertTrue(ctx.isAlertaCallbackShowAlert()); // Modal popup
        assertTrue(ctx.getAlertaCallbackTexto().contains("21.00") || ctx.getAlertaCallbackTexto().contains("21,00"));
    }
}
