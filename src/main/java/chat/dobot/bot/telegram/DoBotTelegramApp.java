package chat.dobot.bot.telegram;

import chat.dobot.bot.BotStateMethod;
import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.DoBotException;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.domain.DoBot;
import chat.dobot.bot.service.DoBotService;
import chat.dobot.bot.utils.ConsoleUtil;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Ponto de entrada para criar um chatbot DoBot integrado ao Telegram via Long Polling.
 *
 * <p>Esta classe é o espelho de {@link chat.dobot.bot.DoBotChatApp} para o canal Telegram.
 * Ela reutiliza o mesmo motor de estados do DoBot (anotações {@code @DoBotChat},
 * {@code @EstadoChat} e {@code @Config}), mas em vez de iniciar um servidor Javalin/HTTP,
 * registra o bot na API do Telegram usando Long Polling.
 *
 * <h2>Uso básico</h2>
 * <pre>{@code
 * @DoBotChat(id = "meubot", nome = "Meu Bot Telegram")
 * public class MeuBotTelegram {
 *
 *     public static void main(String[] args) {
 *         DoBotTelegramApp.novoBot()
 *             .token("SEU_TOKEN_AQUI")
 *             .username("seu_bot_username")
 *             .ativarExemplos()   // opcional: carrega bots de exemplo do pacote chat.dobot
 *             .start();
 *     }
 *
 *     @Config
 *     public void config(DoBotConfig config) {
 *         config.setMensagemInicial("Olá! Como posso ajudar?");
 *     }
 *
 *     @EstadoChat(inicial = true)
 *     public void main(Contexto ctx) {
 *         ctx.responder("Você disse: " + ctx.getMensagemUsuario());
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Atenção:</strong> ao contrário do {@link chat.dobot.bot.DoBotChatApp}, que mantém
 * um único estado compartilhado por bot (sessão HTTP), esta implementação gerencia o estado
 * de forma <em>independente por usuário</em> (chatId do Telegram). Isso significa que cada
 * usuário do Telegram tem sua própria sessão de conversa isolada.
 */
public class DoBotTelegramApp {

    private static final String PACOTE_EXEMPLOS = "chat.dobot";
    private static final Logger logger = LoggerFactory.getLogger(DoBotTelegramApp.class);

    private String token;
    private String username;
    private boolean carregarExemplos = false;
    private Class<?> botClass = null;

    private DoBotTelegramApp() {
    }

    /**
     * Cria uma nova instância do {@code DoBotTelegramApp}.
     *
     * @return nova instância, pronta para configuração
     */
    public static DoBotTelegramApp novoBot() {
        return new DoBotTelegramApp();
    }

    /**
     * Define o token de autenticação do bot Telegram (gerado pelo BotFather).
     *
     * @param token token do bot
     * @return esta instância (fluent API)
     */
    public DoBotTelegramApp token(String token) {
        this.token = token;
        return this;
    }

    /**
     * Define o username do bot Telegram (sem o "@", ex: {@code "meu_bot"}).
     *
     * @param username username do bot
     * @return esta instância (fluent API)
     */
    public DoBotTelegramApp username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Ativa o carregamento dos bots de exemplo do pacote {@code chat.dobot}.
     * Por padrão, bots de exemplo são ignorados.
     *
     * @return esta instância (fluent API)
     */
    public DoBotTelegramApp ativarExemplos() {
        this.carregarExemplos = true;
        return this;
    }

    /**
     * Especifica explicitamente qual classe anotada com {@code @DoBotChat} deve ser
     * instanciada como bot do Telegram.
     *
     * <p>Isso é necessário quando há mais de uma classe {@code @DoBotChat} no classpath
     * (por exemplo, quando {@link #ativarExemplos()} está ativo), pois a ordem de
     * descoberta pelo classgraph é não-determinística.
     *
     * <pre>{@code
     * DoBotTelegramApp.novoBot()
     *     .token("...")
     *     .username("...")
     *     .botClass(AdivinhaNumeroBot.class)
     *     .start();
     * }</pre>
     *
     * @param botClass classe anotada com {@code @DoBotChat} a ser usada
     * @return esta instância (fluent API)
     */
    public DoBotTelegramApp botClass(Class<?> botClass) {
        this.botClass = botClass;
        return this;
    }

    /**
     * Inicializa o bot Telegram com Long Polling.
     *
     * <p>Este método bloqueia enquanto o bot estiver em execução.
     *
     * @throws DoBotException se o token ou username não estiver configurado, se nenhuma classe
     *                        {@code @DoBotChat} for encontrada, ou se ocorrer erro ao iniciar o bot
     */
    public void start() {
        if (token == null || token.isBlank()) {
            throw new DoBotException("Token do bot Telegram não configurado! Use .token(\"SEU_TOKEN\").");
        }
        if (username == null || username.isBlank()) {
            throw new DoBotException("Username do bot Telegram não configurado! Use .username(\"seu_username\").");
        }

        try {
            ConsoleUtil.printYellow(getdoBotAsciiArt());
            ConsoleUtil.printYellow("DoBotChat Telegram Adapter");

            // Localiza e instancia o primeiro bot anotado com @DoBotChat
            DoBot doBot = carregarInstanciaChatbot();

            // Sem persistência configurada para o adaptador Telegram por padrão
            Map<String, DoBotService<Record>> servicos = new HashMap<>();

            ConsoleUtil.printConsole("🤖 Iniciando bot Telegram @" + username + "...");

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramBotAdapter(token, username, doBot, servicos));

            ConsoleUtil.printConsole("✅ Bot Telegram @" + username + " iniciado com sucesso! Aguardando mensagens...");
        } catch (TelegramApiException e) {
            ConsoleUtil.printErro("Falha ao registrar o bot Telegram!", e);
            throw new DoBotException("Falha ao registrar o bot Telegram: " + e.getMessage(), e);
        } catch (DoBotException e) {
            ConsoleUtil.printErro("Falha ao iniciar o DoBotTelegramApp!", e);
            throw e;
        } catch (Exception e) {
            ConsoleUtil.printErro("Erro inesperado ao iniciar o DoBotTelegramApp!", e);
            throw new DoBotException("Erro inesperado: " + e.getMessage(), e);
        }
    }

    /**
     * Escaneia o classpath procurando por uma classe anotada com {@code @DoBotChat},
     * instancia-a e constrói o {@link DoBot} correspondente com todos os seus estados.
     *
     * <p>Se {@link #botClass} foi definido via {@link #botClass(Class)}, apenas aquela classe
     * é utilizada. Caso contrário, a primeira elegível encontrada no classpath é usada.
     *
     * @return o bot carregado e pronto para uso
     * @throws DoBotException se nenhuma classe adequada for encontrada ou houver erro na instância
     */
    private DoBot carregarInstanciaChatbot() {
        ConsoleUtil.printConsole("Carregando chatbot para o Telegram...");

        // Se a classe do bot foi especificada explicitamente, usa-a diretamente
        if (botClass != null) {
            return instanciarBot(botClass);
        }

        try (ScanResult scanResult = new ClassGraph().enableAnnotationInfo().scan()) {
            for (Class<?> classe : scanResult.getClassesWithAnnotation(DoBotChat.class).loadClasses()) {
                if (classe.getName().startsWith(PACOTE_EXEMPLOS) && !carregarExemplos) {
                    continue;
                } else {
                    ConsoleUtil.printConsole("Candidato encontrado: " + classe.getName());
                }

                try {
                    return instanciarBot(classe);
                } catch (DoBotException e) {
                    ConsoleUtil.printWarning(e.getMessage());
                    ConsoleUtil.printConsole("💬❌ Chatbot não inicializado, tentando próximo...");
                }
            }
        } catch (Exception e) {
            throw new DoBotException("Erro ao escanear o classpath:\n" + e.getMessage(), e);
        }

        throw new DoBotException("Nenhuma classe anotada com @DoBotChat foi encontrada para o adaptador Telegram.");
    }

    /**
     * Instancia um {@link DoBot} a partir de uma classe anotada com {@code @DoBotChat}.
     *
     * @param classe a classe a ser instanciada
     * @return o bot configurado e pronto para uso
     * @throws DoBotException se a classe não possuir a anotação ou houver erro na instanciação
     */
    private DoBot instanciarBot(Class<?> classe) {
        DoBotChat annotation = classe.getAnnotation(DoBotChat.class);
        if (annotation == null) {
            throw new DoBotException("A classe " + classe.getName() + " não possui a anotação @DoBotChat!");
        }
        try {
            Object instancia = classe.getDeclaredConstructor().newInstance();
            DoBot doBot = new DoBot(annotation.id(), annotation.nome(), annotation.descricao());
            ConsoleUtil.printConsole("\n💬⚙️ Carregando chatbot '" + annotation.nome() + "'(" + annotation.id() + ")...");
            varrerMetodos(doBot, instancia);
            ConsoleUtil.printConsole("💬⚙️ Estados: " + doBot.getEstados().toString());
            ConsoleUtil.printConsole("💬✅ Chatbot '" + annotation.nome() + "'(" + annotation.id() + ") carregado com sucesso!");
            return doBot;
        } catch (DoBotException e) {
            throw e;
        } catch (Exception e) {
            throw new DoBotException("Erro ao instanciar a classe " + classe.getName() + ":\n" + e.getMessage(), e);
        }
    }

    /**
     * Varre os métodos da classe anotada com {@code @DoBotChat}, mapeando estados e configuração.
     *
     * @param bot         instância do {@link DoBot} a ser configurada
     * @param chatbotImpl objeto do usuário dev que contém as anotações
     */
    private void varrerMetodos(DoBot bot, Object chatbotImpl) {
        Map<String, BotStateMethod> estadosMap = new HashMap<>();

        for (Method metodo : chatbotImpl.getClass().getDeclaredMethods()) {

            if (metodo.isAnnotationPresent(Config.class)) {
                conferirAssinaturaMetodoConfig(metodo);
                try {
                    metodo.invoke(chatbotImpl, bot.getConfig());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new DoBotException("Problema ao executar o método de configuração do bot. Método: " + metodo.getName(), e);
                }
                bot.setMensagemInicial(bot.getConfig().getMensagemInicial());
            }

            if (metodo.isAnnotationPresent(EstadoChat.class)) {
                final String nomeEstado = getNomeEstado(metodo);
                if (estadosMap.containsKey(nomeEstado)) {
                    throw new DoBotException("Estado duplicado: " + nomeEstado);
                }
                conferirAssinaturaMetodoEstado(metodo);
                estadosMap.put(nomeEstado, contexto -> {
                    try {
                        metodo.invoke(chatbotImpl, contexto);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new DoBotException("Problema ao executar o estado '" + nomeEstado + "', método: " + metodo.getName(), e);
                    }
                });
            }
        }

        if (estadosMap.isEmpty()) {
            throw new DoBotException("Nenhum estado mapeado para " + chatbotImpl.getClass().getSimpleName() + "!");
        }
        if (!estadosMap.containsKey("main")) {
            throw new DoBotException("Nenhum estado inicial definido!");
        }

        bot.setEstados(estadosMap);
    }

    private void conferirAssinaturaMetodoConfig(Method metodo) {
        String configName = DoBotConfig.class.getSimpleName();
        if (metodo.getParameterCount() != 1) {
            throw new DoBotException("Método " + metodo.getName() + " deve ter um parâmetro do tipo " + configName);
        }
        if (!metodo.getParameterTypes()[0].getSimpleName().equals(configName)) {
            throw new DoBotException("Método " + metodo.getName() + " deve ter um parâmetro do tipo " + configName);
        }
    }

    private String getNomeEstado(Method metodo) {
        EstadoChat estadoChat = metodo.getAnnotation(EstadoChat.class);
        if (estadoChat == null) {
            throw new IllegalArgumentException("BUG! Método " + metodo.getName() + " deveria possuir anotação @EstadoChat!");
        }
        if (estadoChat.inicial()) {
            return DoBot.ESTADO_INICIAL;
        }
        return estadoChat.estado().isBlank() ? metodo.getName().toLowerCase() : estadoChat.estado().toLowerCase();
    }

    private void conferirAssinaturaMetodoEstado(Method metodo) {
        String contextoName = Contexto.class.getSimpleName();
        if (metodo.getParameterCount() != 1) {
            throw new DoBotException("Método " + metodo.getName() + " deve ter um parâmetro do tipo " + contextoName);
        }
        if (!metodo.getParameterTypes()[0].getSimpleName().equals(contextoName)) {
            throw new DoBotException("Método " + metodo.getName() + " deve ter um parâmetro do tipo " + contextoName);
        }
    }

    private String getdoBotAsciiArt() {
        return """
                  ____        ____        _         _           _   
                 |  _ \\  ___ | __ )  ___ | |_   ___| |__   __ _| |_ 
                 | | | |/ _ \\|  _ \\ / _ \\| __| / __| '_ \\ / _` | __|
                 | |_| | (_) | |_) | (_) | |_ | (__| | | | (_| | |_ 
                 |____/ \\___/|____/ \\___/ \\__(_)___|_| |_|\\__,_|\\__|
                """;
    }
}
