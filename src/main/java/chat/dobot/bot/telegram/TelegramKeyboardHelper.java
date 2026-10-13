package chat.dobot.bot.telegram;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import chat.dobot.bot.BotaoInline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Classe utilitária que constrói objetos de teclado da API do Telegram
 * a partir de dados genéricos (Strings, listas, mapas).
 *
 * <p>
 * Esta classe centraliza toda a dependência de
 * {@code org.telegram.telegrambots.meta.api.objects.replykeyboard} para que
 * a classe {@link chat.dobot.bot.Contexto} permaneça desacoplada do Telegram.
 */
public final class TelegramKeyboardHelper {

    private TelegramKeyboardHelper() {
    }

    /**
     * Cria um {@link ReplyKeyboardMarkup} a partir de rótulos de botões.
     * Os botões são dispostos automaticamente em linhas (até 3 botões por linha,
     * ou todos na mesma linha se houver 4 ou menos).
     *
     * @param botoes rótulos dos botões
     * @return o teclado criado
     */
    public static ReplyKeyboardMarkup criarTecladoBotoes(String... botoes) {
        if (botoes == null || botoes.length == 0) {
            return null;
        }
        List<KeyboardRow> rows = new ArrayList<>();
        int maxPorLinha = botoes.length <= 4 ? botoes.length : 3;
        KeyboardRow linhaAtual = new KeyboardRow();
        for (String botao : botoes) {
            if (linhaAtual.size() >= maxPorLinha) {
                rows.add(linhaAtual);
                linhaAtual = new KeyboardRow();
            }
            linhaAtual.add(botao);
        }
        if (!linhaAtual.isEmpty()) {
            rows.add(linhaAtual);
        }
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(rows);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        return markup;
    }

    /**
     * Cria um {@link ReplyKeyboardMarkup} a partir de uma grade de rótulos de botões.
     *
     * @param gradeBotoes linhas de botões
     * @return o teclado criado, ou {@code null} se a grade for vazia
     */
    public static ReplyKeyboardMarkup criarTecladoGrade(List<List<String>> gradeBotoes) {
        if (gradeBotoes == null || gradeBotoes.isEmpty()) {
            return null;
        }
        List<KeyboardRow> rows = new ArrayList<>();
        for (List<String> linha : gradeBotoes) {
            if (linha == null || linha.isEmpty()) continue;
            KeyboardRow keyboardRow = new KeyboardRow();
            for (String textoBotao : linha) {
                keyboardRow.add(textoBotao);
            }
            rows.add(keyboardRow);
        }
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(rows);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        return markup;
    }

    /**
     * Cria um {@link ReplyKeyboardRemove} para remover o teclado customizado.
     *
     * @return instância de remoção de teclado
     */
    public static ReplyKeyboardRemove criarRemocaoTeclado() {
        return new ReplyKeyboardRemove(true);
    }

    /**
     * Cria um {@link InlineKeyboardMarkup} a partir de rótulos de botões simples
     * (texto = callbackData).
     *
     * @param botoes rótulos dos botões
     * @return o teclado inline criado, ou {@code null} se não houver botões
     */
    public static InlineKeyboardMarkup criarTecladoInline(String... botoes) {
        if (botoes == null || botoes.length == 0) {
            return null;
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        int maxPorLinha = botoes.length <= 4 ? botoes.length : 3;
        List<InlineKeyboardButton> linhaAtual = new ArrayList<>();

        for (String botao : botoes) {
            if (linhaAtual.size() >= maxPorLinha) {
                rows.add(linhaAtual);
                linhaAtual = new ArrayList<>();
            }
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(botao);
            btn.setCallbackData(botao);
            linhaAtual.add(btn);
        }
        if (!linhaAtual.isEmpty()) {
            rows.add(linhaAtual);
        }
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * Cria um {@link InlineKeyboardMarkup} a partir de um mapa de texto → callbackData.
     *
     * @param textoParaCallbackData mapa de texto visível → valor de callback
     * @return o teclado inline criado, ou {@code null} se o mapa for vazio
     */
    public static InlineKeyboardMarkup criarTecladoInline(Map<String, String> textoParaCallbackData) {
        if (textoParaCallbackData == null || textoParaCallbackData.isEmpty()) {
            return null;
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        int maxPorLinha = textoParaCallbackData.size() <= 4 ? textoParaCallbackData.size() : 2;
        List<InlineKeyboardButton> linhaAtual = new ArrayList<>();

        for (Map.Entry<String, String> entry : textoParaCallbackData.entrySet()) {
            if (linhaAtual.size() >= maxPorLinha) {
                rows.add(linhaAtual);
                linhaAtual = new ArrayList<>();
            }
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(entry.getKey());
            btn.setCallbackData(entry.getValue());
            linhaAtual.add(btn);
        }
        if (!linhaAtual.isEmpty()) {
            rows.add(linhaAtual);
        }
        return new InlineKeyboardMarkup(rows);
    }

    /**
     * Cria um {@link InlineKeyboardMarkup} a partir de uma grade de {@link BotaoInline}.
     *
     * @param gradeBotoes linhas de botões inline
     * @return o teclado inline criado, ou {@code null} se a grade for vazia
     */
    public static InlineKeyboardMarkup criarTecladoInlineGrade(List<List<BotaoInline>> gradeBotoes) {
        if (gradeBotoes == null || gradeBotoes.isEmpty()) {
            return null;
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (List<BotaoInline> linha : gradeBotoes) {
            if (linha == null || linha.isEmpty()) continue;
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (BotaoInline b : linha) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(b.texto());
                if (b.callbackData() != null) btn.setCallbackData(b.callbackData());
                if (b.url() != null) btn.setUrl(b.url());
                row.add(btn);
            }
            rows.add(row);
        }
        return new InlineKeyboardMarkup(rows);
    }
}
