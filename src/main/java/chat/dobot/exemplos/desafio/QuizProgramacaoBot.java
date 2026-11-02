package chat.dobot.exemplos.desafio;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.telegram.DoBotTelegramApp;

@DoBotChat(
        id = "quiz-programacao",
        nome = "Quiz de Programação",
        descricao = "Quiz curto sobre conceitos básicos de programação"
)
public class QuizProgramacaoBot {

    private static final String COMECAR = "Começar quiz";
    private static final String PONTUACAO = "pontuacao";
    private static final String PERGUNTA = "pergunta";

    public static void main(String[] args) {
        DoBotTelegramApp.novoBot()
                .token("TELEGRAM_TOKEN")
                .username("USERNAME_BOT")
                .botClass(QuizProgramacaoBot.class)
                .start();
    }

    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial(
                "Olá! Vamos testar seus conhecimentos básicos de programação.");
    }

    @EstadoChat(inicial = true)
    public void menu(Contexto ctx) {
        ctx.responderComBotoes(
                "Quiz de Programação\nVocê responderá 3 perguntas.",
                COMECAR);
        ctx.mudarEstado("processarMenu");
    }

    @EstadoChat(estado = "processarMenu")
    public void processarMenu(Contexto ctx) {
        if (COMECAR.equals(ctx.getMensagemUsuario().trim())) {
            ctx.setDado(PONTUACAO, 0);
            ctx.setDado(PERGUNTA, 1);
            enviarPergunta(ctx);
        } else {
            ctx.responder("Escolha o botão \"Começar quiz\" para iniciar.");
            menu(ctx);
        }
    }

    @EstadoChat(estado = "responderPergunta")
    public void responderPergunta(Contexto ctx) {
        String resposta = ctx.getMensagemUsuario().trim();
        int numeroPergunta = ctx.getDado(PERGUNTA, Integer.class);

        if (!resposta.equals("1") && !resposta.equals("2") && !resposta.equals("3")) {
            ctx.responder("Digite apenas 1, 2 ou 3.");
            return;
        }

        int respostaCorreta = numeroPergunta == 1 ? 2 : numeroPergunta == 2 ? 1 : 3;
        if (Integer.parseInt(resposta) == respostaCorreta) {
            int pontuacao = ctx.getDado(PONTUACAO, Integer.class);
            ctx.setDado(PONTUACAO, pontuacao + 1);
            ctx.responder("Resposta correta!");
        } else {
            ctx.responder("Resposta incorreta. :(");
        }

        if (numeroPergunta == 3) {
            mostrarResultado(ctx);
        } else {
            ctx.setDado(PERGUNTA, numeroPergunta + 1);
            enviarPergunta(ctx);
        }
    }

    private void enviarPergunta(Contexto ctx) {
        int numeroPergunta = ctx.getDado(PERGUNTA, Integer.class);

        if (numeroPergunta == 1) {
            ctx.responderComBotoes(
                    "1) Um tipo de dado\n2) Uma estrutura de repetição\n3) Um banco de dados",
                    "1", "2", "3");
        } else if (numeroPergunta == 2) {
            ctx.responderComBotoes(
                    "Qual palavra-chave cria uma classe em Java?\n" +
                            "1) class\n2) function\n3) struct",
                    "1", "2", "3");
        } else {
            ctx.responderComBotoes(
                    "Qual estrutura guarda vários valores em sequência?\n" +
                            "1) boolean\n2) if\n3) List",
                    "1", "2", "3");
        }

        ctx.mudarEstado("responderPergunta");
    }

    private void mostrarResultado(Contexto ctx) {
        int pontuacao = ctx.getDado(PONTUACAO, Integer.class);
        ctx.responder("Fim do quiz! Voce acertou " + pontuacao + " de 3.");
        menu(ctx);
    }
}
