package chat.dobot.bot;

/**
 * Representa um botão para teclados inline ({@link org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup}).
 *
 * <p>Um botão inline pode disparar uma ação no bot (via {@code callbackData})
 * ou abrir um link externo no navegador (via {@code url}).
 *
 * @param texto        rótulo visível no botão
 * @param callbackData payload enviado ao bot quando o botão é clicado (opcional)
 * @param url          URL externa aberta ao clicar no botão (opcional)
 */
public record BotaoInline(String texto, String callbackData, String url) {

    /**
     * Cria um botão inline com ação de callback.
     *
     * @param texto        texto exibido no botão
     * @param callbackData dado recebido pelo bot ao ser clicado
     * @return nova instância de {@link BotaoInline}
     */
    public static BotaoInline callback(String texto, String callbackData) {
        return new BotaoInline(texto, callbackData, null);
    }

    /**
     * Cria um botão inline com ação de callback onde o dado recebido é igual ao texto exibido.
     *
     * @param texto texto exibido no botão e usado como callbackData
     * @return nova instância de {@link BotaoInline}
     */
    public static BotaoInline callback(String texto) {
        return new BotaoInline(texto, texto, null);
    }

    /**
     * Cria um botão inline que abre um link externo.
     *
     * @param texto texto exibido no botão
     * @param url   endereço web a ser aberto
     * @return nova instância de {@link BotaoInline}
     */
    public static BotaoInline link(String texto, String url) {
        return new BotaoInline(texto, null, url);
    }
}
