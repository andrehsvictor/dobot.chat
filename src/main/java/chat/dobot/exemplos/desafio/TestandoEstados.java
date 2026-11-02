package chat.dobot.exemplos.desafio;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.telegram.DoBotTelegramApp;

import java.util.ArrayList;
import java.util.List;

@DoBotChat(nome = "Bot de Teste", descricao = "Bot para testar estados e fluxos de conversa", id = "test-bot")
public class TestandoEstados {

    private static final String ADICIONAR = "Adicionar Tarefa";
    private static final String LISTAR = "Listar Tarefas";
    private static final String LIMPAR = "Limpar";
    private static final String TAREFAS = "tarefas";


    public static void main(String[] args) {
        DoBotTelegramApp.novoBot()
                .token("TOKEN_TELEGRAM")
                .username("USERNAME_BOT")
                .botClass(TestandoEstados.class)
                .start();
    }

    // Mensagem de boas vindas do bot
    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial(
                "Olá! Este bot ensina como uma conversa muda de estado.\n" +
                        "Escolha uma opção para começar.");
    }


    // Estado inicial - o menu
    @EstadoChat(inicial = true)
    public void menu(Contexto ctx) {
        ctx.responderComBotoes(
                "Menu de tarefas\nEscolha uma opção:",
                ADICIONAR,
                LISTAR,
                LIMPAR);
        ctx.mudarEstado("processarOpcao");
    }

    // Estado que processa a opção escolhida pelo usuário
    @EstadoChat(estado = "processarOpcao")
    public void processarOpcao(Contexto ctx) {
        String opcao = ctx.getMensagemUsuario() == null
                ? ""
                : ctx.getMensagemUsuario().trim();

        if (ADICIONAR.equals(opcao)) {
            ctx.responder("Digite o nome da tarefa:");
            ctx.mudarEstado("receberTarefa");
        } else if (LISTAR.equals(opcao)) {
            ctx.mudarEstado("listarTarefas");
        } else if (LIMPAR.equals(opcao)) {
            ctx.mudarEstado("limparTarefas");
        } else {
            ctx.responder("Não entendi essa opção. Escolha um dos botões do menu.");
            ctx.mudarEstado("menu");
        }
    }

    // Estado que recebe a tarefa digitada pelo usuário
    @EstadoChat(estado = "receberTarefa")
    public void receberTarefa(Contexto ctx) {
        String tarefa = ctx.getMensagemUsuario() == null
                ? ""
                : ctx.getMensagemUsuario().trim(); // Pega a mensagem do usuário e remove espaços em branco no início e no fim

        if (tarefa.isEmpty()) {
            ctx.responder("A tarefa não pode ficar vazia. Digite um nome:");
            return;
        }

        List<String> tarefas = obterTarefas(ctx);
        tarefas.add(tarefa);
        ctx.responder("Tarefa adicionada: " + tarefa);
        menu(ctx);
    }

    // Estado que lista as tarefas cadastradas
    @EstadoChat(estado = "listarTarefas")
    public void listarTarefas(Contexto ctx) {
        List<String> tarefas = obterTarefas(ctx);
        if (tarefas.isEmpty()) {
            ctx.responder("Você ainda não tem tarefas cadastradas.");
        } else {
            StringBuilder mensagem = new StringBuilder("Suas tarefas:\n");
            for (int i = 0; i < tarefas.size(); i++) {
                mensagem.append(i + 1).append(". ").append(tarefas.get(i)).append('\n');
            }
            ctx.responder(mensagem.toString().trim());
        }
        ctx.mudarEstado("menu");
    }

    @EstadoChat(estado = "limparTarefas")
    public void limparTarefas(Contexto ctx) {
        ctx.removerDado(TAREFAS);
        ctx.responder("Todas as tarefas foram limpas.");
        ctx.mudarEstado("menu");
    }

    @SuppressWarnings("unchecked")
    private List<String> obterTarefas(Contexto ctx) {
        List<String> tarefas = ctx.getDado(TAREFAS, List.class);
        if (tarefas == null) {
            tarefas = new ArrayList<>();
            ctx.setDado(TAREFAS, tarefas);
        }
        return tarefas;
    }

}
