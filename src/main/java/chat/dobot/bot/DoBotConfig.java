package chat.dobot.bot;

import chat.dobot.bot.domain.DoBotTema;

public class DoBotConfig {

    private String mensagemInicial;
    private DoBotTema tema;
    private boolean armazenarMensagensEmMemoria = true;

    public DoBotConfig(String mensagemInicial, DoBotTema tema) {
        this.mensagemInicial = mensagemInicial;
        this.tema = tema;
        this.armazenarMensagensEmMemoria = true;
    }

    public DoBotConfig() {
        this("", new DoBotTema());
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

    public boolean isArmazenarMensagensEmMemoria() {
        return armazenarMensagensEmMemoria;
    }

    public void setArmazenarMensagensEmMemoria(boolean armazenarMensagensEmMemoria) {
        this.armazenarMensagensEmMemoria = armazenarMensagensEmMemoria;
    }
}

