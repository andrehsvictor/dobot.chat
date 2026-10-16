package chat.dobot.exemplos.telegram;

import java.util.ArrayList;
import java.util.List;

import chat.dobot.bot.BotaoInline;
import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.service.DoBotService;
import chat.dobot.bot.telegram.DoBotTelegramApp;

/**
 * Exemplo de Chatbot Telegram combinando <b>Botões Inline Interativos</b>
 * e persistência dos pedidos.
 *
 * <p>
 * Recursos demonstrados:
 * <ul>
 * <li>Botões inline organizados em grade
 * ({@link Contexto#responderComGradeBotoesInline})</li>
 * <li>Ações via callback com feedback instantâneo (toasts e popups modais via
 * {@link Contexto#setAlertaCallback})</li>
 * <li>Operações CRUD completas (inserir, listar e deletar) via
 * {@link DoBotService}</li>
 * </ul>
 *
 * <h2>Como executar</h2>
 * 
 * <pre>{@code
 * public static void main(String[] args) {
 *     DoBotTelegramApp.novoBot()
 *             .token("SEU_TOKEN_DO_BOTFATHER")
 *             .username("seu_bot_username")
 *             .botClass(CafeGourmetBot.class)
 *             .start();
 * }
 * }</pre>
 */
@DoBotChat(id = "cafeGourmet", nome = "☕ Café Gourmet DoBot", descricao = "Bot de cafeteria com botões inline e persistência de pedidos")
public class CafeGourmetBot {

    public static void main(String[] args) {
        DoBotTelegramApp.novoBot()
                .token(System.getenv().getOrDefault("TELEGRAM_BOT_TOKEN",
                        "TELEGRAM_BOT_TOKEN"))
                .username(System.getenv().getOrDefault("TELEGRAM_BOT_USERNAME", "TELEGRAM_BOT_USERNAME"))
                .botClass(CafeGourmetBot.class)
                .portaH2(8082)
                .ativarExemplos()
                .start();
    }

    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial(
                "☕ *Bem-vindo ao Café Gourmet DoBot!*\n\n" +
                        "Toque nos botões inline abaixo para montar seu pedido.\n" +
                        "Cada escolha é salva para acompanhar o seu pedido!");
    }

    /**
     * Estado inicial: apresenta o cardápio interativo com botões inline.
     */
    @EstadoChat(inicial = true)
    public void menu(Contexto ctx) {
        ctx.responderComGradeBotoesInline(
                "📋 *Cardápio Digital*\nEscolha uma das delícias abaixo:",
                criarGradeMenuPrincipal());
        ctx.mudarEstado("tratarAcao");
    }

    /**
     * Estado que processa os cliques dos botões inline e mensagens do usuário.
     */
    @EstadoChat(estado = "tratarAcao")
    public void tratarAcao(Contexto ctx) {
        String acao = ctx.getCallbackData();

        // Se o usuário digitou texto em vez de clicar no botão
        if (acao == null || acao.isBlank()) {
            acao = ctx.getMensagemUsuario();
        }

        DoBotService<ItemPedido> servico = obterServico(ctx);

        if (acao == null || acao.isBlank()) {
            menu(ctx);
            return;
        }

        // 1. Adicionar itens ao pedido
        if (acao.startsWith("add_")) {
            processarAdicaoItem(ctx, servico, acao);
            return;
        }

        // 2. Visualizar pedidos salvos
        if ("ver_pedidos".equalsIgnoreCase(acao)) {
            exibirPedidosSalvos(ctx, servico);
            return;
        }

        // 3. Remover um item específico por ID
        if (acao.startsWith("del_")) {
            processarRemocaoItem(ctx, servico, acao);
            return;
        }

        // 4. Limpar todos os pedidos
        if ("limpar".equalsIgnoreCase(acao)) {
            limparTodosPedidos(ctx, servico);
            return;
        }

        // 5. Finalizar pedido
        if ("finalizar".equalsIgnoreCase(acao)) {
            finalizarPedido(ctx, servico);
            return;
        }

        // 6. Voltar ao menu
        if ("voltar_menu".equalsIgnoreCase(acao)) {
            menu(ctx);
            return;
        }

        // Qualquer outro comando ou texto não reconhecido: reapresenta o menu
        ctx.responder("Opção não reconhecida. Use os botões abaixo:");
        menu(ctx);
    }

    // ── Métodos de Negócio ───────────────────────────────────────────────────

    private void processarAdicaoItem(Contexto ctx, DoBotService<ItemPedido> servico, String acao) {
        ItemPedido itemParaSalvar = switch (acao) {
            case "add_espresso" -> new ItemPedido(0, "☕ Espresso Especial", 6.00, "Café Gourmet");
            case "add_croissant" -> new ItemPedido(0, "🥐 Croissant Amanteigado", 12.00, "Forno da Casa");
            case "add_cheesecake" -> new ItemPedido(0, "🍰 Cheesecake de Frutas", 15.00, "Sobremesa");
            case "add_suco" -> new ItemPedido(0, "🧃 Suco de Laranja Natural", 8.00, "Bebida Gelada");
            default -> null;
        };

        if (itemParaSalvar != null) {
            // Salva o registro
            if (servico != null) {
                try {
                    servico.salvar(itemParaSalvar);
                } catch (Exception e) {
                    ctx.responder("Erro ao salvar o item: " + e.getMessage());
                    return;
                }
            }

            // Notificação rápida (toast) no Telegram
            ctx.setAlertaCallback("✅ " + itemParaSalvar.item() + " adicionado ao pedido!", false);

            ctx.responderComGradeBotoesInline(
                    "Item registrado no pedido:\n" +
                            "• *" + itemParaSalvar.item() + "* — R$ " + String.format("%.2f", itemParaSalvar.preco())
                            + "\n\n" +
                            "Deseja adicionar mais algum item?",
                    criarGradeMenuPrincipal());
        }
    }

    private void exibirPedidosSalvos(Contexto ctx, DoBotService<ItemPedido> servico) {
        List<ItemPedido> pedidos = buscarPedidos(servico);

        if (pedidos.isEmpty()) {
            if (ctx.getAlertaCallbackTexto() == null) {
                ctx.setAlertaCallback("📭 Nenhum pedido registrado!", false);
            }
            ctx.responderComGradeBotoesInline(
                    "Seu carrinho está vazio.\nEscolha algum item do cardápio:",
                    criarGradeMenuPrincipal());
            return;
        }

        double total = 0.0;
        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Seus Pedidos:*\n\n");

        List<List<BotaoInline>> gradeRemocao = new ArrayList<>();

        for (ItemPedido p : pedidos) {
            total += p.preco();
            sb.append("• `#").append(p.id()).append("` — ").append(p.item())
                    .append(" (R$ ").append(String.format("%.2f", p.preco())).append(")\n");

            // Botão individual para deletar este item específico
            gradeRemocao.add(List.of(
                    BotaoInline.callback("❌ Remover #" + p.id() + " (" + p.item() + ")", "del_" + p.id())));
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("💰 *Total Geral:* R$ ").append(String.format("%.2f", total));
        sb.append(" (").append(pedidos.size()).append(" itens)");

        // Botões de ação para o carrinho
        gradeRemocao.add(List.of(
                BotaoInline.callback("💳 Finalizar Conta", "finalizar"),
                BotaoInline.callback("➕ Mais Itens", "voltar_menu")));
        gradeRemocao.add(List.of(
                BotaoInline.callback("🗑️ Limpar Todos", "limpar")));

        ctx.responderComGradeBotoesInline(sb.toString(), gradeRemocao);
    }

    private void processarRemocaoItem(Contexto ctx, DoBotService<ItemPedido> servico, String acao) {
        try {
            int id = Integer.parseInt(acao.replace("del_", ""));
            deletarPedido(servico, id);
            ctx.setAlertaCallback("🗑️ Item #" + id + " removido do pedido!", false);
            exibirPedidosSalvos(ctx, servico);
        } catch (Exception e) {
            ctx.responder("Erro ao remover item: " + e.getMessage());
            menu(ctx);
        }
    }

    private void limparTodosPedidos(Contexto ctx, DoBotService<ItemPedido> servico) {
        List<ItemPedido> pedidos = buscarPedidos(servico);
        for (ItemPedido p : pedidos) {
            deletarPedido(servico, p.id());
        }
        // Alerta popup modal no Telegram
        ctx.setAlertaCallback("🧹 Todos os itens foram removidos!", true);
        ctx.responderComGradeBotoesInline(
                "O pedido foi limpo com sucesso.\nQue tal iniciar um novo pedido?",
                criarGradeMenuPrincipal());
    }

    private void finalizarPedido(Contexto ctx, DoBotService<ItemPedido> servico) {
        List<ItemPedido> pedidos = buscarPedidos(servico);

        if (pedidos.isEmpty()) {
            ctx.setAlertaCallback("⚠️ Seu carrinho está vazio!", true);
            return;
        }

        double total = 0.0;
        for (ItemPedido p : pedidos) {
            total += p.preco();
            deletarPedido(servico, p.id());
        }

        // Popup modal de comemoração
        ctx.setAlertaCallback(
                "🎉 Pedido de R$ " + String.format("%.2f", total) + " confirmado com sucesso!",
                true);

        ctx.responderComGradeBotoesInline(
                "🧾 *Pedido Concluído!*\n\n" +
                        "Valor total: *R$ " + String.format("%.2f", total) + "*\n" +
                        "Seus itens já foram enviados para a cozinha!\n\n" +
                        "Deseja fazer outro pedido?",
                criarGradeMenuPrincipal());
    }

    private DoBotService<ItemPedido> obterServico(Contexto ctx) {
        try {
            return ctx.getServico(ItemPedido.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<ItemPedido> buscarPedidos(DoBotService<ItemPedido> servico) {
        if (servico == null) {
            return List.of();
        }
        try {
            return servico.buscarTodos();
        } catch (Exception e) {
            return List.of();
        }
    }

    private void deletarPedido(DoBotService<ItemPedido> servico, int id) {
        if (servico != null) {
            try {
                servico.deletarPorId(id);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Constrói a grade de botões inline do menu principal.
     */
    private List<List<BotaoInline>> criarGradeMenuPrincipal() {
        return List.of(
                // Linha 1: Bebidas quentes e salgados
                List.of(
                        BotaoInline.callback("☕ Espresso (R$ 6,00)", "add_espresso"),
                        BotaoInline.callback("🥐 Croissant (R$ 12,00)", "add_croissant")),
                // Linha 2: Doces e bebidas frias
                List.of(
                        BotaoInline.callback("🍰 Cheesecake (R$ 15,00)", "add_cheesecake"),
                        BotaoInline.callback("🧃 Suco Laranja (R$ 8,00)", "add_suco")),
                // Linha 3: Gerenciamento do pedido
                List.of(
                        BotaoInline.callback("📋 Ver Pedidos", "ver_pedidos"),
                        BotaoInline.callback("💳 Pagar Conta", "finalizar")),
                // Linha 4: Limpeza do pedido
                List.of(
                        BotaoInline.callback("🗑️ Limpar Pedidos", "limpar")));
    }
}
