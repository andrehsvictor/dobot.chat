package chat.dobot.exemplos.produtos;

import chat.dobot.bot.annotations.Entidade;
import chat.dobot.bot.annotations.Id;

@Entidade
public record Produto(@Id int id, String nome, double preco) {
}