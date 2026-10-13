package chat.dobot.bot;

import chat.dobot.bot.service.DoBotService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Contexto {

    private final String mensagemUsuario;
    private String estado;
    private final List<String> respostas;
    private final Map<String, DoBotService<Record>> servicos;
    private final Map<String, Object> dados;
    private Object teclado;

    public Contexto(String mensagemUsuario, String estado, Map<String, DoBotService<Record>> servicos) {
        this(mensagemUsuario, estado, servicos, new HashMap<>());
    }

    public Contexto(String mensagemUsuario, String estado, Map<String, DoBotService<Record>> servicos, Map<String, Object> dados) {
        this.mensagemUsuario = mensagemUsuario;
        this.estado = estado != null ? estado.toLowerCase() : "";
        this.servicos = servicos != null ? servicos : new HashMap<>();
        this.dados = dados != null ? dados : new HashMap<>();
        this.respostas = new ArrayList<>(20);
    }

    public String getMensagemUsuario() {
        return mensagemUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public void mudarEstado(String estado) {
        this.estado = estado;
    }

    public void responder(String resposta) {
        if (resposta != null) {
            this.respostas.add(resposta);
        }
    }

    public List<String> getRespostas() {
        return respostas;
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

    // ── Dados / Atributos da Sessão (Per-user state) ─────────────────────────

    public void set(String chave, Object valor) {
        if (chave != null) {
            this.dados.put(chave, valor);
        }
    }

    public void setDado(String chave, Object valor) {
        set(chave, valor);
    }

    public Object get(String chave) {
        return chave != null ? this.dados.get(chave) : null;
    }

    public Object getDado(String chave) {
        return get(chave);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String chave, Class<T> tipo) {
        Object val = get(chave);
        if (val == null) {
            return null;
        }
        if (tipo.isInstance(val)) {
            return (T) val;
        }
        if (tipo == Integer.class && val instanceof Number num) {
            return (T) Integer.valueOf(num.intValue());
        }
        if (tipo == Long.class && val instanceof Number num) {
            return (T) Long.valueOf(num.longValue());
        }
        if (tipo == Double.class && val instanceof Number num) {
            return (T) Double.valueOf(num.doubleValue());
        }
        if (tipo == String.class) {
            return (T) val.toString();
        }
        throw new ClassCastException("Atributo '" + chave + "' é do tipo " + val.getClass().getName() + ", esperado " + tipo.getName());
    }

    public <T> T getDado(String chave, Class<T> tipo) {
        return get(chave, tipo);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String chave, T valorPadrao) {
        Object val = get(chave);
        if (val == null) {
            return valorPadrao;
        }
        return (T) val;
    }

    public <T> T getDado(String chave, T valorPadrao) {
        return get(chave, valorPadrao);
    }

    public boolean has(String chave) {
        return chave != null && this.dados.containsKey(chave);
    }

    public boolean temDado(String chave) {
        return has(chave);
    }

    public Object remove(String chave) {
        return chave != null ? this.dados.remove(chave) : null;
    }

    public Object removerDado(String chave) {
        return remove(chave);
    }

    public void limparDados() {
        this.dados.clear();
    }

    public Map<String, Object> getDados() {
        return this.dados;
    }

    public Long getChatId() {
        return getDado("_chatId", Long.class);
    }

    public void setChatId(Long chatId) {
        setDado("_chatId", chatId);
    }

    // ── Suporte a Teclado / Botões (Telegram ReplyKeyboardMarkup) ────────────

    /**
     * Adiciona uma resposta com um teclado customizado (objeto de teclado do Telegram).
     *
     * @param resposta mensagem de texto a enviar
     * @param teclado  teclado a ser exibido junto à resposta
     */
    public void responder(String resposta, Object teclado) {
        responder(resposta);
        this.teclado = teclado;
    }

    /**
     * Responde com botões de teclado personalizados (ReplyKeyboardMarkup).
     * Os botões são dispostos automaticamente em linhas (até 3 botões por linha).
     *
     * <p>Requer que a biblioteca {@code telegrambots} esteja no classpath.
     *
     * @param resposta mensagem de texto a enviar
     * @param botoes   rótulos dos botões
     */
    public void responderComBotoes(String resposta, String... botoes) {
        responder(resposta);
        if (botoes == null || botoes.length == 0) {
            return;
        }
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarTecladoBotoes(botoes);
    }

    /**
     * Responde com botões de teclado a partir de uma lista de rótulos.
     *
     * @param resposta mensagem de texto a enviar
     * @param botoes   lista com os rótulos dos botões
     */
    public void responderComBotoes(String resposta, List<String> botoes) {
        responderComBotoes(resposta, botoes != null ? botoes.toArray(new String[0]) : new String[0]);
    }

    /**
     * Responde com botões dispostos exatamente de acordo com a grade (lista de linhas) informada.
     *
     * @param resposta    mensagem de texto a enviar
     * @param gradeBotoes linhas de botões
     */
    public void responderComGradeBotoes(String resposta, List<List<String>> gradeBotoes) {
        responder(resposta);
        if (gradeBotoes == null || gradeBotoes.isEmpty()) {
            return;
        }
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarTecladoGrade(gradeBotoes);
    }

    /**
     * Define o teclado a ser enviado junto à próxima resposta.
     *
     * @param teclado objeto de teclado (ex: ReplyKeyboardMarkup, InlineKeyboardMarkup)
     */
    public void setTeclado(Object teclado) {
        this.teclado = teclado;
    }

    /**
     * Retorna o teclado configurado para a próxima resposta.
     *
     * @return o teclado, ou {@code null} se nenhum foi configurado
     */
    public Object getTeclado() {
        return teclado;
    }

    /**
     * Envia uma resposta e remove qualquer teclado customizado anteriormente ativo.
     *
     * @param resposta mensagem de texto a enviar
     */
    public void removerTeclado(String resposta) {
        responder(resposta);
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarRemocaoTeclado();
    }

    /**
     * Configura a remoção do teclado customizado ativo.
     */
    public void removerTeclado() {
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarRemocaoTeclado();
    }

    // ── Suporte a Teclados Inline (InlineKeyboardMarkup) ─────────────────────

    /**
     * Envia uma resposta associada a um teclado inline customizado.
     *
     * @param resposta      mensagem de texto a enviar
     * @param tecladoInline teclado inline do Telegram
     */
    public void responderComTecladoInline(String resposta, Object tecladoInline) {
        responder(resposta);
        this.teclado = tecladoInline;
    }

    /**
     * Responde com botões inline simples onde cada botão possui o mesmo texto e callbackData.
     *
     * @param resposta mensagem de texto a enviar
     * @param botoes   rótulos dos botões
     */
    public void responderComBotoesInline(String resposta, String... botoes) {
        responder(resposta);
        if (botoes == null || botoes.length == 0) {
            return;
        }
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarTecladoInline(botoes);
    }

    /**
     * Responde com botões inline a partir de uma lista de rótulos.
     *
     * @param resposta mensagem de texto a enviar
     * @param botoes   lista com os rótulos dos botões
     */
    public void responderComBotoesInline(String resposta, List<String> botoes) {
        responderComBotoesInline(resposta, botoes != null ? botoes.toArray(new String[0]) : new String[0]);
    }

    /**
     * Responde com botões inline mapeando cada rótulo de botão para um callbackData específico.
     *
     * @param resposta             mensagem de texto a enviar
     * @param textoParaCallbackData mapa de texto visível → valor de callback enviado ao bot
     */
    public void responderComBotoesInline(String resposta, Map<String, String> textoParaCallbackData) {
        responder(resposta);
        if (textoParaCallbackData == null || textoParaCallbackData.isEmpty()) {
            return;
        }
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarTecladoInline(textoParaCallbackData);
    }

    /**
     * Responde com botões inline dispostos em uma grade com suporte completo a links e callbacks.
     *
     * @param resposta    mensagem de texto a enviar
     * @param gradeBotoes lista de linhas de {@link BotaoInline}
     */
    public void responderComGradeBotoesInline(String resposta, List<List<BotaoInline>> gradeBotoes) {
        responder(resposta);
        if (gradeBotoes == null || gradeBotoes.isEmpty()) {
            return;
        }
        this.teclado = chat.dobot.bot.telegram.TelegramKeyboardHelper.criarTecladoInlineGrade(gradeBotoes);
    }

    // ── CallbackQuery Metadata & Notificações ────────────────────────────────

    private String callbackQueryId;
    private String callbackData;
    private String alertaCallbackTexto;
    private boolean alertaCallbackShowAlert;

    /**
     * Indica se a interação atual foi acionada por um clique em botão inline.
     */
    public boolean isCallbackQuery() {
        return callbackQueryId != null;
    }

    public String getCallbackQueryId() {
        return callbackQueryId;
    }

    public void setCallbackQueryId(String callbackQueryId) {
        this.callbackQueryId = callbackQueryId;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    /**
     * Configura um toast ou alerta de confirmação no Telegram para o clique no botão inline.
     *
     * @param texto      mensagem a ser exibida
     * @param showAlert  se {@code true}, exibe como popup modal; se {@code false}, como notificação discreta no topo
     */
    public void setAlertaCallback(String texto, boolean showAlert) {
        this.alertaCallbackTexto = texto;
        this.alertaCallbackShowAlert = showAlert;
    }

    public void setAlertaCallback(String texto) {
        setAlertaCallback(texto, false);
    }

    public String getAlertaCallbackTexto() {
        return alertaCallbackTexto;
    }

    public boolean isAlertaCallbackShowAlert() {
        return alertaCallbackShowAlert;
    }
}
