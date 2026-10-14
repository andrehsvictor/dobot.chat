package chat.dobot.exemplos.telegram;

import chat.dobot.bot.annotations.Entidade;
import chat.dobot.bot.annotations.Id;

/**
 * Entidade que representa um item de pedido armazenado no banco de dados H2.
 *
 * <p>Mapeada pelo motor de persistência do DoBot.chat através das anotações
 * {@link Entidade} e {@link Id}.
 *
 * @param id          identificador único gerado automaticamente pelo H2
 * @param item        nome do produto (ex: "Espresso", "Croissant")
 * @param preco       valor monetário do produto
 * @param observacao  detalhes ou observações do pedido
 */
@Entidade
public record ItemPedido(
        @Id int id,
        String item,
        double preco,
        String observacao
) {
}
