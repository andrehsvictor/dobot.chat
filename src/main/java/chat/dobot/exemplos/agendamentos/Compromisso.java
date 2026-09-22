package chat.dobot.exemplos.agendamentos;

import chat.dobot.bot.annotations.Entidade;
import chat.dobot.bot.annotations.Id;

@Entidade
public record Compromisso(@Id int id, String descricao, String dataHora) {
}
