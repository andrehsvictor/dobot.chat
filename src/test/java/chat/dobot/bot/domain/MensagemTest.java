package chat.dobot.bot.domain;

import chat.dobot.bot.Autor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mensagem")
class MensagemTest {

    @Test
    @DisplayName("deve armazenar autor e conteúdo corretamente")
    void armazenaAutorEConteudo() {
        Mensagem mensagem = new Mensagem(Autor.BOT, "Olá!");

        assertEquals(Autor.BOT, mensagem.autor());
        assertEquals("Olá!", mensagem.conteudo());
    }

    @Test
    @DisplayName("deve suportar mensagens do USUARIO")
    void suportaMensagemDoUsuario() {
        Mensagem mensagem = new Mensagem(Autor.USUARIO, "Preciso de ajuda");

        assertEquals(Autor.USUARIO, mensagem.autor());
        assertEquals("Preciso de ajuda", mensagem.conteudo());
    }

    @Test
    @DisplayName("dois records com mesmo autor e conteúdo devem ser iguais (equals de record)")
    void recordEqualsComMesmosValores() {
        Mensagem m1 = new Mensagem(Autor.BOT, "Texto");
        Mensagem m2 = new Mensagem(Autor.BOT, "Texto");

        assertEquals(m1, m2);
    }

    @Test
    @DisplayName("dois records com conteúdos diferentes não devem ser iguais")
    void recordEqualsComConteudosDiferentesNaoSaoIguais() {
        Mensagem m1 = new Mensagem(Autor.BOT, "Texto A");
        Mensagem m2 = new Mensagem(Autor.BOT, "Texto B");

        assertNotEquals(m1, m2);
    }
}
