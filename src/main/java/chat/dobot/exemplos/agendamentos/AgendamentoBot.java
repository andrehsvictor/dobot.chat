package chat.dobot.exemplos.agendamentos;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotChatApp;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.service.DoBotService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@DoBotChat(id = "agendaBot", nome = "📅 Agendamento de Compromissos", descricao = "Bot para cadastrar, alterar e cancelar compromissos")
public class AgendamentoBot {

    private static final DateTimeFormatter FORMATADOR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static void main(String[] args) {
        DoBotChatApp app = DoBotChatApp.novoBot();
        app.ativarBot("agendaBot");
        app.start(8083, 8084);
    }

    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial("👋 Olá! Eu sou o bot de agendamentos.\n\n" +
                "Menu:\n" +
                "1 - Agendar compromisso\n" +
                "2 - Listar compromissos\n" +
                "3 - Alterar compromisso\n" +
                "4 - Cancelar compromisso\n" +
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
                chat.responder("Descreva o compromisso:");
                chat.mudarEstado("cadastrarDescricao");
            }
            case "2" -> {
                listarCompromissos(chat);
                mostrarMenu(chat);
            }
            case "3" -> {
                chat.responder("Digite o ID do compromisso que deseja alterar:");
                chat.mudarEstado("solicitarIdAlteracao");
            }
            case "4" -> {
                chat.responder("Digite o ID do compromisso que deseja cancelar:");
                chat.mudarEstado("cancelarCompromisso");
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

    @EstadoChat(estado = "cadastrarDescricao")
    public void cadastrarDescricao(Contexto chat) {
        String descricao = chat.getMensagemUsuario().trim();
        if (descricao.isBlank()) {
            chat.responder("A descrição não pode ser vazia. Descreva o compromisso:");
            return;
        }
        chat.armazenar("descricaoCompromisso", descricao);
        pedirConjuntoDatas(chat);
        chat.mudarEstado("cadastrarDatas");
    }

    @EstadoChat(estado = "cadastrarDatas")
    public void cadastrarDatas(Contexto chat) {
        List<String> opcoes = extrairDatas(chat.getMensagemUsuario());
        if (opcoes.isEmpty()) {
            chat.responder("Nenhuma data válida foi encontrada.");
            pedirConjuntoDatas(chat);
            return;
        }
        chat.armazenar("datasDisponiveis", opcoes);
        chat.responder(formatarOpcoesDatas(opcoes));
        chat.mudarEstado("escolherDataCadastro");
    }

    @EstadoChat(estado = "escolherDataCadastro")
    public void escolherDataCadastro(Contexto chat) {
        List<String> opcoes = obterDatasDisponiveis(chat);
        Integer indice = obterIndiceEscolhido(chat.getMensagemUsuario(), opcoes.size());
        if (indice == null) {
            chat.responder("Escolha inválida. Digite apenas o número de uma opção da lista.");
            return;
        }

        String descricao = (String) chat.obter("descricaoCompromisso");
        String dataEscolhida = opcoes.get(indice);
        try {
            chat.getServico(Compromisso.class).salvar(new Compromisso(0, descricao, dataEscolhida));
            chat.responder("Compromisso agendado para " + dataEscolhida + ".");
        } catch (Exception e) {
            chat.responder("Não foi possível agendar: " + e.getMessage());
        } finally {
            limparDadosTemporarios(chat);
        }
        mostrarMenu(chat);
        chat.mudarEstado("menu");
    }

    @EstadoChat(estado = "solicitarIdAlteracao")
    public void solicitarIdAlteracao(Contexto chat) {
        Integer id = parseId(chat.getMensagemUsuario());
        if (id == null) {
            chat.responder("ID inválido. Digite um número inteiro:");
            return;
        }

        try {
            Compromisso compromisso = chat.getServico(Compromisso.class).buscarPorId(id);
            if (compromisso == null) {
                chat.responder("Compromisso não encontrado.");
                mostrarMenu(chat);
                chat.mudarEstado("menu");
                return;
            }
            chat.armazenar("idCompromissoAlteracao", id);
            chat.responder("Descrição atual: " + compromisso.descricao());
            chat.responder("Digite a nova descrição do compromisso:");
            chat.mudarEstado("alterarDescricao");
        } catch (Exception e) {
            chat.responder("Não foi possível buscar o compromisso: " + e.getMessage());
            mostrarMenu(chat);
            chat.mudarEstado("menu");
        }
    }

    @EstadoChat(estado = "alterarDescricao")
    public void alterarDescricao(Contexto chat) {
        String descricao = chat.getMensagemUsuario().trim();
        if (descricao.isBlank()) {
            chat.responder("A descrição não pode ser vazia. Digite a nova descrição:");
            return;
        }
        chat.armazenar("descricaoCompromisso", descricao);
        pedirConjuntoDatas(chat);
        chat.mudarEstado("alterarDatas");
    }

    @EstadoChat(estado = "alterarDatas")
    public void alterarDatas(Contexto chat) {
        List<String> opcoes = extrairDatas(chat.getMensagemUsuario());
        if (opcoes.isEmpty()) {
            chat.responder("Nenhuma data válida foi encontrada.");
            pedirConjuntoDatas(chat);
            return;
        }
        chat.armazenar("datasDisponiveis", opcoes);
        chat.responder(formatarOpcoesDatas(opcoes));
        chat.mudarEstado("escolherDataAlteracao");
    }

    @EstadoChat(estado = "escolherDataAlteracao")
    public void escolherDataAlteracao(Contexto chat) {
        List<String> opcoes = obterDatasDisponiveis(chat);
        Integer indice = obterIndiceEscolhido(chat.getMensagemUsuario(), opcoes.size());
        if (indice == null) {
            chat.responder("Escolha inválida. Digite apenas o número de uma opção da lista.");
            return;
        }

        Integer id = (Integer) chat.obter("idCompromissoAlteracao");
        String descricao = (String) chat.obter("descricaoCompromisso");
        String dataEscolhida = opcoes.get(indice);

        try {
            DoBotService<Compromisso> servico = chat.getServico(Compromisso.class);
            Compromisso existente = servico.buscarPorId(id);
            if (existente == null) {
                chat.responder("Compromisso não encontrado para alteração.");
            } else {
                servico.deletarPorId(id);
                servico.salvar(new Compromisso(id, descricao, dataEscolhida));
                chat.responder("Compromisso alterado para " + dataEscolhida + ".");
            }
        } catch (Exception e) {
            chat.responder("Não foi possível alterar o compromisso: " + e.getMessage());
        } finally {
            limparDadosTemporarios(chat);
            chat.remover("idCompromissoAlteracao");
        }
        mostrarMenu(chat);
        chat.mudarEstado("menu");
    }

    @EstadoChat(estado = "cancelarCompromisso")
    public void cancelarCompromisso(Contexto chat) {
        Integer id = parseId(chat.getMensagemUsuario());
        if (id == null) {
            chat.responder("ID inválido. Digite um número inteiro:");
            return;
        }

        try {
            DoBotService<Compromisso> servico = chat.getServico(Compromisso.class);
            Compromisso compromisso = servico.buscarPorId(id);
            if (compromisso == null) {
                chat.responder("Compromisso não encontrado.");
            } else {
                servico.deletarPorId(id);
                chat.responder("Compromisso cancelado: " + compromisso.descricao());
            }
        } catch (Exception e) {
            chat.responder("Não foi possível cancelar o compromisso: " + e.getMessage());
        }
        mostrarMenu(chat);
        chat.mudarEstado("menu");
    }

    @EstadoChat(estado = "encerrado")
    public void encerrado(Contexto chat) {
        chat.responder("Conversa encerrada. Envie qualquer mensagem para voltar ao menu.");
        chat.mudarEstado("menu");
    }

    private void listarCompromissos(Contexto chat) {
        try {
            List<Compromisso> compromissos = chat.getServico(Compromisso.class).buscarTodos();
            if (compromissos.isEmpty()) {
                chat.responder("Nenhum compromisso agendado.");
                return;
            }

            StringBuilder resposta = new StringBuilder("Compromissos agendados:\n");
            for (Compromisso compromisso : compromissos) {
                resposta.append(compromisso.id())
                        .append(" - ")
                        .append(compromisso.descricao())
                        .append(" - ")
                        .append(compromisso.dataHora())
                        .append('\n');
            }
            chat.responder(resposta.toString().trim());
        } catch (Exception e) {
            chat.responder("Não foi possível listar os compromissos: " + e.getMessage());
        }
    }

    private void mostrarMenu(Contexto chat) {
        chat.responder("Menu:\n1 - Agendar compromisso\n2 - Listar compromissos\n3 - Alterar compromisso\n4 - Cancelar compromisso\n5 - Sair");
    }

    private void pedirConjuntoDatas(Contexto chat) {
        chat.responder("Informe um conjunto de datas e horários separados por vírgula no formato dd/MM/yyyy HH:mm.");
        chat.responder("Exemplo: 25/09/2026 09:00, 25/09/2026 14:30, 26/09/2026 10:00");
    }

    private List<String> extrairDatas(String mensagem) {
        String[] partes = mensagem.split(",");
        List<String> datas = new ArrayList<>();
        for (String parte : partes) {
            String dataTexto = parte.trim();
            if (dataTexto.isBlank()) {
                continue;
            }
            if (isDataValida(dataTexto)) {
                datas.add(dataTexto);
            }
        }
        return datas;
    }

    private boolean isDataValida(String texto) {
        try {
            LocalDateTime.parse(texto, FORMATADOR);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String formatarOpcoesDatas(List<String> opcoes) {
        StringBuilder resposta = new StringBuilder("Escolha uma data para o compromisso:\n");
        for (int i = 0; i < opcoes.size(); i++) {
            resposta.append(i + 1).append(" - ").append(opcoes.get(i)).append('\n');
        }
        return resposta.toString().trim();
    }

    private Integer obterIndiceEscolhido(String mensagem, int totalOpcoes) {
        try {
            int escolha = Integer.parseInt(mensagem.trim());
            if (escolha < 1 || escolha > totalOpcoes) {
                return null;
            }
            return escolha - 1;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> obterDatasDisponiveis(Contexto chat) {
        @SuppressWarnings("unchecked")
        List<String> opcoes = (List<String>) chat.obter("datasDisponiveis");
        return opcoes == null ? List.of() : opcoes;
    }

    private Integer parseId(String mensagem) {
        try {
            return Integer.parseInt(mensagem.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void limparDadosTemporarios(Contexto chat) {
        chat.remover("descricaoCompromisso");
        chat.remover("datasDisponiveis");
    }
}
