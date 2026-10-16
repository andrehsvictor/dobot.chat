package chat.dobot.bot.telegram;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.DoBotException;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.domain.DoBot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DoBotTelegramApp")
class DoBotTelegramAppTest {

    // Bot de teste com dependência no construtor (sem construtor padrão)
    @DoBotChat(id = "botComDep", nome = "Bot Com Dependência")
    static class BotComDependencia {
        private final String prefixo;

        public BotComDependencia(String prefixo) {
            this.prefixo = prefixo;
        }

        @Config
        public void config(DoBotConfig config) {
            config.setMensagemInicial("Iniciando...");
        }

        @EstadoChat(inicial = true)
        public void main(Contexto ctx) {
            ctx.responder(prefixo + ": " + ctx.getMensagemUsuario());
        }
    }

    // Bot comum com construtor padrão
    @DoBotChat(id = "botSimples", nome = "Bot Simples")
    static class BotSimples {
        @Config
        public void config(DoBotConfig config) {
            config.setMensagemInicial("Oi!");
        }

        @EstadoChat(inicial = true)
        public void main(Contexto ctx) {
            ctx.responder("Resposta simples");
        }
    }

    // Classe sem anotação @DoBotChat
    static class ClasseSemAnotacao {
        @EstadoChat(inicial = true)
        public void main(Contexto ctx) {
        }
    }

    @Test
    @DisplayName("deve carregar bot a partir de instância direta com construtor customizado")
    void carregaBotDeInstanciaCustomizada() {
        BotComDependencia instancia = new BotComDependencia("Echo");

        DoBotTelegramApp app = DoBotTelegramApp.novoBot().bot(instancia);
        DoBot doBot = app.carregarInstanciaChatbot();

        assertNotNull(doBot);
        assertEquals("botComDep", doBot.getId());
        assertEquals("Bot Com Dependência", doBot.getNome());
        assertTrue(doBot.getEstados().contains("main"));
    }

    @Test
    @DisplayName("deve carregar bot a partir de classe via botClass")
    void carregaBotDeClasse() {
        DoBotTelegramApp app = DoBotTelegramApp.novoBot().botClass(BotSimples.class);
        DoBot doBot = app.carregarInstanciaChatbot();

        assertNotNull(doBot);
        assertEquals("botSimples", doBot.getId());
        assertEquals("Bot Simples", doBot.getNome());
        assertTrue(doBot.getEstados().contains("main"));
    }

    @Test
    @DisplayName("deve lançar DoBotException ao tentar configurar objeto sem anotação @DoBotChat")
    void lancaExcecaoSeNaoTiverAnotacao() {
        DoBotTelegramApp app = DoBotTelegramApp.novoBot().bot(new ClasseSemAnotacao());

        DoBotException ex = assertThrows(DoBotException.class, app::carregarInstanciaChatbot);
        assertTrue(ex.getMessage().contains("@DoBotChat"));
    }

    @Test
    @DisplayName("deve lançar DoBotException se token não for configurado ao iniciar")
    void lancaExcecaoSemToken() {
        DoBotTelegramApp app = DoBotTelegramApp.novoBot()
                .username("meubot")
                .bot(new BotSimples());

        DoBotException ex = assertThrows(DoBotException.class, app::start);
        assertTrue(ex.getMessage().contains("Token"));
    }

    @Test
    @DisplayName("deve lançar DoBotException se username não for configurado ao iniciar")
    void lancaExcecaoSemUsername() {
        DoBotTelegramApp app = DoBotTelegramApp.novoBot()
                .token("123:abc")
                .bot(new BotSimples());

        DoBotException ex = assertThrows(DoBotException.class, app::start);
        assertTrue(ex.getMessage().contains("Username"));
    }
}
