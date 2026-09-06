package chat.dobot.bot;

import chat.dobot.bot.domain.DoBotTema;

public class DoBotConfig {

    private String mensagemInicial;
    private DoBotTema tema;

    public DoBotConfig(String mensagemInicial, DoBotTema tema) {
        this.mensagemInicial = mensagemInicial;
        this.tema = tema;
    }

    public DoBotConfig() {
        this("", new DoBotTema());
    }

    public DoBotConfig(DoBotConfig config) {
        this(config.mensagemInicial, new DoBotTema(
                config.tema.getCorFundoPagina(),
                config.tema.getCorFundoChat(),
                config.tema.getCorTextoChat(),
                config.tema.getCorFundoMensagemUsuario(),
                config.tema.getCorFundoMensagemBot()));
    }

    public String getMensagemInicial() {
        return mensagemInicial;
    }

    public void setMensagemInicial(String mensagemInicial) {
        this.mensagemInicial = mensagemInicial;
    }

    public DoBotTema getTema() {
        return tema;
    }
}
