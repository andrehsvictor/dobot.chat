package chat.dobot.bot;

import chat.dobot.bot.service.DoBotService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Contexto {

    private final String mensagemUsuario;
    private String estado;
    private final List<String> respostas;
    private final Map<String, DoBotService<Record>> servicos;
    private final Map<String, Object> dados;

    public Contexto(String mensagemUsuario, String estado, Map<String, DoBotService<Record>> servicos) {
        this(mensagemUsuario, estado, servicos, new java.util.HashMap<>());
    }

    public Contexto(String mensagemUsuario, String estado, Map<String, DoBotService<Record>> servicos,
                    Map<String, Object> dados) {
        this.mensagemUsuario = mensagemUsuario;
        this.estado = estado.toLowerCase();
        this.servicos = servicos;
        this.dados = dados;
        respostas = new ArrayList<>(20);
    }

    public String getMensagemUsuario() {
        return mensagemUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public void mudarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("O estado não pode ser vazio");
        }
        this.estado = estado.toLowerCase();
    }

    public void responder(String resposta) {
        this.respostas.add(resposta);
    }

    public List<String> getRespostas() {
        return respostas;
    }

    public void armazenar(String chave, Object valor) {
        dados.put(chave, valor);
    }

    public Object obter(String chave) {
        return dados.get(chave);
    }

    public void remover(String chave) {
        dados.remove(chave);
    }

    public <T extends Record> DoBotService<T> getServico(Class<T> recordClass) {
        DoBotService<?> servico = servicos.get(recordClass.getSimpleName());

        if (servico == null) {
            throw new DoBotException("Serviço não encontrado para a classe " + recordClass.getSimpleName() + "!");
        }

        @SuppressWarnings("unchecked")
        DoBotService<T> servicoTipado = (DoBotService<T>) servico;

        return servicoTipado;
    }
}
