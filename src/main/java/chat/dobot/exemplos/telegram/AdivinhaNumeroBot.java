package chat.dobot.exemplos.telegram;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.telegram.DoBotTelegramApp;

/**
 * Exemplo de chatbot DoBot integrado ao Telegram.
 *
 * <p>
 * Este bot demonstra o uso do adaptador {@link DoBotTelegramApp} com os mesmos
 * padrões
 * de anotação do DoBot ({@code @DoBotChat}, {@code @Config},
 * {@code @EstadoChat}).
 *
 * <p>
 * O bot implementa um menu de adivinhação simples com múltiplos estados:
 * <ul>
 * <li><b>main</b> — apresenta o menu inicial</li>
 * <li><b>adivinhar</b> — recebe o palpite do usuário e verifica</li>
 * <li><b>resultado</b> — exibe o resultado e oferece nova tentativa</li>
 * </ul>
 *
 * <h2>Como usar</h2>
 * <ol>
 * <li>Crie um bot no Telegram via
 * <a href="https://t.me/botfather">BotFather</a>.</li>
 * <li>Substitua {@code SEU_TOKEN_AQUI} pelo token gerado.</li>
 * <li>Substitua {@code seu_bot_username} pelo username do seu bot (sem
 * "@").</li>
 * <li>Execute o método {@code main} desta classe.</li>
 * </ol>
 */
@DoBotChat(id = "adivinhaNumero", nome = "🎲 Adivinha o Número", descricao = "Bot que desafia o usuário a adivinhar um número entre 1 e 10")
public class AdivinhaNumeroBot {

    public static void main(String[] args) {
        DoBotTelegramApp.novoBot()
                .token("SEU_TOKEN_AQUI") // <-- substitua pelo token do BotFather
                .username("seu_bot_username") // <-- substitua pelo username do bot (sem @)
                .botClass(AdivinhaNumeroBot.class) // garante que este bot seja carregado, não outro @DoBotChat
                .ativarExemplos()
                .start();
    }

    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial(
                "🎲 Bem-vindo ao jogo de Adivinha o Número!\n" +
                        "Estou pensando em um número entre 1 e 10.\n" +
                        "Você consegue adivinhar?");
    }

    /**
     * Estado inicial: sorteia o número secreto para este usuário e pede um palpite.
     * O número é armazenado no contexto da sessão do usuário, garantindo que
     * cada jogador tenha seu próprio jogo isolado.
     */
    @EstadoChat(inicial = true)
    public void main(Contexto ctx) {
        int numeroSecreto = gerarNumero();
        ctx.setDado("numeroSecreto", numeroSecreto);
        ctx.responder("🤔 Estou pensando em um número entre 1 e 10...");
        ctx.responder("Qual é o seu palpite?");
        ctx.mudarEstado("adivinhar");
    }

    /**
     * Estado de recepção do palpite: valida o palpite contra o número secreto do
     * usuário.
     */
    @EstadoChat(estado = "adivinhar")
    public void adivinhar(Contexto ctx) {
        String mensagem = ctx.getMensagemUsuario().trim();

        int palpite;
        try {
            palpite = Integer.parseInt(mensagem);
        } catch (NumberFormatException e) {
            ctx.responder("❌ Por favor, envie um número inteiro entre 1 e 10.");
            return; // mantém o estado "adivinhar"
        }

        if (palpite < 1 || palpite > 10) {
            ctx.responder("⚠️ O número deve ser entre 1 e 10. Tente novamente!");
            return;
        }

        Integer numeroSecreto = ctx.getDado("numeroSecreto", Integer.class);
        if (numeroSecreto == null) {
            numeroSecreto = gerarNumero();
            ctx.setDado("numeroSecreto", numeroSecreto);
        }

        if (palpite == numeroSecreto) {
            ctx.responder("🎉 Parabéns! Você acertou! O número era " + numeroSecreto + "!");
        } else if (palpite < numeroSecreto) {
            ctx.responder("📈 Quase! O número secreto é MAIOR que " + palpite + ".");
        } else {
            ctx.responder("📉 Quase! O número secreto é MENOR que " + palpite + ".");
        }

        // Exibe botões interativos no Telegram
        ctx.responderComBotoes("Quer jogar novamente?", "Sim", "Não");
        ctx.mudarEstado("resultado");
    }

    /**
     * Estado de resultado: pergunta se o usuário quer jogar outra rodada.
     */
    @EstadoChat(estado = "resultado")
    public void resultado(Contexto ctx) {
        String resposta = ctx.getMensagemUsuario().trim().toLowerCase();

        if (resposta.equals("sim") || resposta.equals("s") || resposta.equals("yes")) {
            int novoNumero = gerarNumero();
            ctx.setDado("numeroSecreto", novoNumero);
            ctx.removerTeclado("🎲 Nova rodada! Estou pensando em um novo número entre 1 e 10...");
            ctx.responder("Qual é o seu palpite?");
            ctx.mudarEstado("adivinhar");
        } else {
            ctx.removerDado("numeroSecreto");
            ctx.removerTeclado(
                    "👋 Obrigado por jogar! Até a próxima!\nEnvie /start para começar um novo jogo a qualquer momento.");
            ctx.mudarEstado("main");
        }
    }

    /** Gera um número aleatório entre 1 e 10 (inclusive). */
    private static int gerarNumero() {
        return (int) (Math.random() * 10) + 1;
    }
}
