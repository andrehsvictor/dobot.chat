package chat.dobot.exemplos.produtos;


import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotChatApp;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.service.DoBotService;

import java.util.List;
import java.util.Locale;

@DoBotChat(id = "cadProdutos", nome = "Cadastro de Produtos", descricao = "Bot que Cadastra e lista produtos")
public class CadastroProdutosBot {

    public static void main(String[] args) {
        DoBotChatApp meubot = DoBotChatApp.novoBot();
        meubot.ativarBot("cadProdutos");
        meubot.start(8083,8084);

    }

    @Config
    public void config(DoBotConfig config){
        config.setMensagemInicial("👋 Olá! Eu sou o cadastro de produtos.\n\n" +
            "Menu:\n" +
            "1 - Cadastrar produto\n" +
            "2 - Listar produtos\n" +
            "3 - Buscar produto\n" +
            "4 - Excluir produto\n" +
            "5 - Sair");
    }

    @EstadoChat(inicial = true)
    public void inicio(Contexto chat) {
        menu(chat);
    }

    @EstadoChat(estado = "menu")
    public void menu(Contexto chat) {
        switch (chat.getMensagemUsuario()) {
            case "1" -> {
                chat.responder("Digite o nome do produto:");
                chat.mudarEstado("cadastrarNome");
            }
            case "2" -> {
                listarProdutos(chat);
                mostrarMenu(chat);
            }
            case "3" -> {
                chat.responder("Digite o ID do produto:");
                chat.mudarEstado("buscarProduto");
            }
            case "4" -> {
                chat.responder("Digite o ID do produto que deseja excluir:");
                chat.mudarEstado("excluirProduto");
            }
            case "5" -> {
                chat.responder("Até logo!");
                chat.mudarEstado("encerrado");
            }
            default -> {
                chat.responder("Opção inválida.");
                mostrarMenu(chat);
            }
        }
    }

    @EstadoChat(estado = "cadastrarNome")
    public void cadastrarNome(Contexto chat) {
        String nome = chat.getMensagemUsuario().trim();
        if (nome.isBlank()) {
            chat.responder("O nome não pode ficar vazio. Digite o nome do produto:");
            return;
        }
        chat.armazenar("produtoNome", nome);
        chat.responder("Digite o preço do produto, por exemplo: 19.90");
        chat.mudarEstado("cadastrarPreco");
    }

    @EstadoChat(estado = "cadastrarPreco")
    public void cadastrarPreco(Contexto chat) {
        try {
            double preco = Double.parseDouble(chat.getMensagemUsuario().trim().replace(',', '.'));
            if (!Double.isFinite(preco) || preco < 0) {
                throw new NumberFormatException();
            }

            String nome = (String) chat.obter("produtoNome");
            DoBotService<Produto> servico = chat.getServico(Produto.class);
            servico.salvar(new Produto(0, nome, preco));
            chat.remover("produtoNome");
            chat.responder("Produto cadastrado com sucesso!");
            mostrarMenu(chat);
            chat.mudarEstado("menu");
        } catch (NumberFormatException e) {
            chat.responder("Preço inválido. Digite um valor como 19.90:");
        } catch (Exception e) {
            chat.responder("Não foi possível cadastrar o produto: " + e.getMessage());
            chat.mudarEstado("menu");
            mostrarMenu(chat);
        }
    }

    @EstadoChat(estado = "buscarProduto")
    public void buscarProduto(Contexto chat) {
        try {
            int id = Integer.parseInt(chat.getMensagemUsuario().trim());
            Produto produto = chat.getServico(Produto.class).buscarPorId(id);
            chat.responder(produto == null ? "Produto não encontrado." : formatar(produto));
        } catch (NumberFormatException e) {
            chat.responder("ID inválido. Digite um número inteiro:");
            return;
        } catch (Exception e) {
            chat.responder("Não foi possível buscar o produto: " + e.getMessage());
        }
        mostrarMenu(chat);
        chat.mudarEstado("menu");
    }

    @EstadoChat(estado = "excluirProduto")
    public void excluirProduto(Contexto chat) {
        try {
            int id = Integer.parseInt(chat.getMensagemUsuario().trim());
            DoBotService<Produto> servico = chat.getServico(Produto.class);
            Produto produto = servico.buscarPorId(id);
            if (produto == null) {
                chat.responder("Produto não encontrado.");
            } else {
                servico.deletarPorId(id);
                chat.responder("Produto excluído: " + produto.nome());
            }
        } catch (NumberFormatException e) {
            chat.responder("ID inválido. Digite um número inteiro:");
            return;
        } catch (Exception e) {
            chat.responder("Não foi possível excluir o produto: " + e.getMessage());
        }
        mostrarMenu(chat);
        chat.mudarEstado("menu");
    }

    @EstadoChat(estado = "encerrado")
    public void encerrado(Contexto chat) {
        chat.responder("Esta conversa foi encerrada. Envie qualquer mensagem para abrir o menu novamente.");
        chat.mudarEstado("main");
    }

    private void listarProdutos(Contexto chat) {
        try {
            List<Produto> produtos = chat.getServico(Produto.class).buscarTodos();
            if (produtos.isEmpty()) {
                chat.responder("Nenhum produto cadastrado.");
                return;
            }
            StringBuilder resposta = new StringBuilder("Produtos cadastrados:\n");
            produtos.forEach(produto -> resposta.append(formatar(produto)).append('\n'));
            chat.responder(resposta.toString().trim());
        } catch (Exception e) {
            chat.responder("Não foi possível listar os produtos: " + e.getMessage());
        }
    }

    private String formatar(Produto produto) {
        return String.format(Locale.US, "%d - %s - R$ %.2f", produto.id(), produto.nome(), produto.preco());
    }

    private void mostrarMenu(Contexto chat) {
        chat.responder("Menu:\n1 - Cadastrar produto\n2 - Listar produtos\n3 - Buscar produto\n4 - Excluir produto\n5 - Sair");
    }
}
