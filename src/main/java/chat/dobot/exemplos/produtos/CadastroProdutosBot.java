package chat.dobot.exemplos.produtos;


import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotChatApp;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;

@DoBotChat(id = "cadProdutos", nome = "Cadastro de Produtos", descricao = "Bot que Cadastra e lista produtos")
public class CadastroProdutosBot {

    public static void main(String[] args) {
        DoBotChatApp meubot = DoBotChatApp.novoBot();
        meubot.start(8083,8084);

    }

    @Config
    public void config(DoBotConfig config){
        config.setMensagemInicial("👋 Olá! Eu sou o chatbot De Cadastro de produtos!");
    }

    @EstadoChat(inicial = true)
    public void aloMundo(Contexto chat) {
        String msg = chat.getMensagemUsuario();
        chat.responder("Alô "+msg);
        chat.responder("Vou mostrar o menu na próxima mensagem.");
        chat.mudarEstado("menu");
    }

    @EstadoChat(estado = "menu")
    public void menu(Contexto chat) {
        chat.responder("Menu: \n1 - Alô Mundo\n2 - Sair");
        chat.mudarEstado("opcao");
    }

    @EstadoChat(estado = "opcao")
    public void opcao(Contexto chat) {
        switch (chat.getMensagemUsuario()) {
            case "1" -> {
                chat.responder("Alô Mundo!");
                chat.mudarEstado("main");
            }
            case "2" -> {
                chat.responder("Até logo!");
                chat.mudarEstado("main");
            }
            default -> chat.responder("Opção inválida. Escolha 1 ou 2.");
        }
    }

}
