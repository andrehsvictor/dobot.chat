# Tutorial prático: chatbot interativo no Telegram com Java 17

Este material acompanha uma sessão prática de **1 hora** sobre a evolução do
framework DoBot.chat para integração com o Telegram, disponível na branch
`telegram-integration`. Ao final, você terá criado um bot simples, entendido
os principais recursos da API e estará preparado para implementar um
gerenciador de tarefas.

> **Objetivo da sessão:** praticar fundamentos de orientação a objetos (POO),
> métodos, anotações, listas, condicionais e fluxo de estados conversacionais.

## 1. DIRETRIZES ÉTICAS E DE USO DE INTELIGÊNCIA ARTIFICIAL

Ferramentas de IA generativa, como **ChatGPT, Claude e Copilot**, podem ser
usadas durante a atividade **somente para tirar dúvidas conceituais** sobre
Java, sintaxe, tipos, métodos, listas, condicionais, anotações ou lógica de
programação.

É **proibido** usar IA para:

- gerar a solução completa do desafio;
- escrever, completar ou corrigir o código do desafio;
- transformar o enunciado em uma implementação pronta para entrega;
- copiar uma solução produzida por outra pessoa ou ferramenta.

A atividade foi planejada para exercitar a fixação de conceitos fundamentais de
POO e fluxo de controle. O código deve ser pensado e escrito pelo próprio
estudante. Se surgir uma dúvida, formule uma pergunta conceitual, estude a
resposta e implemente a ideia por conta própria.

## 2. PRÉ-REQUISITOS DO AMBIENTE

Antes da sessão, certifique-se de que os seguintes itens estão disponíveis:

- **Java JDK 17 ou superior** instalado e configurado no `PATH`;
- **Apache Maven** instalado e configurado no `PATH`;
- uma IDE: **IntelliJ IDEA**, **Eclipse** ou **VS Code** com suporte a Java;
- uma **conta no Telegram** instalada no celular ou acessível pelo Telegram
  Web/Desktop;
- acesso à internet para que o bot possa se comunicar com a API do Telegram.

Verifique as ferramentas no terminal:

```bash
java -version
mvn -version
```

O primeiro comando deve indicar a versão 17 ou superior. O Maven também deve
mostrar que está utilizando um JDK compatível.

## 3. GUIA PASSO A PASSO: CRIANDO O BOT NO TELEGRAM COM @BotFather

O `@BotFather` é o bot oficial usado para registrar e administrar bots no
Telegram.

### 3.1 Abrir o BotFather

1. Abra o Telegram.
2. Pesquise por **@BotFather**.
3. Confirme que o perfil é o oficial, identificado pelo selo de verificação.
4. Abra a conversa e clique em **Start** ou envie `/start`.

### 3.2 Criar um novo bot

1. Envie o comando `/newbot`.
2. Quando o BotFather pedir o **nome** do bot, informe um nome visível para
   as pessoas, por exemplo:
   `Gerenciador de Tarefas da Turma`.
3. Em seguida, informe o **username** do bot. Ele deve ser único no Telegram e
   obrigatoriamente terminar com `bot`, por exemplo:
   `tarefas_turma_bot`.
4. Se o username já estiver em uso, tente outro até o BotFather aceitá-lo.

### 3.3 Copiar o token de acesso

Depois do cadastro, o BotFather exibirá uma mensagem contendo o token de
acesso da API, semelhante a:

```text
123456789:AAExampleTokenGeradoPeloBotFather
```

Copie esse token e guarde-o em local seguro. Ele funciona como uma senha: quem
possui o token pode controlar o bot. **Nunca publique o token no GitHub, em
mensagens, em capturas de tela ou em código compartilhado.**

Também anote o username sem o caractere `@`. Por exemplo, para
`@tarefas_turma_bot`, o valor usado no Java será
`tarefas_turma_bot`.

> ### Atalho de Contingência
>
> Caso você enfrente bloqueios, limitações ou qualquer dificuldade para criar
> um bot no Telegram durante a sessão, solicite ao condutor um **token de
> teste pré-criado**. Não compartilhe esse token fora da atividade e não o
> publique no repositório.

## 4. CONFIGURAÇÃO DO POM.XML (MAVEN)

Na branch `telegram-integration`, o projeto deve usar Java 17 e as
dependências do DoBot.chat e do Telegram. No seu projeto Maven, adicione o
repositório institucional:

```xml
<repositories>
    <repository>
        <id>projetos</id>
        <name>Repositório de Projetos</name>
        <url>https://maven.vps.rodrigor.com/repository/projetos/</url>
    </repository>
</repositories>
```

Adicione as dependências:

```xml
<dependencies>
    <dependency>
        <groupId>chat.dobot.bot</groupId>
        <artifactId>DoBot.chat</artifactId>
        <version>1.1.1</version>
    </dependency>

    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots</artifactId>
        <version>6.9.7.1</version>
    </dependency>
</dependencies>
```

Confira também se o compilador Maven está configurado para Java 17:

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

A configuração mínima pode ficar dentro de um `pom.xml` com `groupId`,
`artifactId` e `version` próprios da sua atividade. Depois de salvar o
arquivo, baixe as dependências e compile:

```bash
mvn clean compile
```

## 5. EXEMPLO MÍNIMO FUNCIONAL (HELLO WORLD)

Crie o arquivo
`src/main/java/exemplo/AppBot.java`. O nome do arquivo deve ser igual ao nome
da classe pública. Substitua os valores de `token` e `username` pelos dados
obtidos com o BotFather.

> **Importante:** para uma demonstração rápida, o exemplo usa o token
> diretamente no código. Em um projeto real, prefira uma variável de ambiente
> e nunca versionar credenciais.

```java
package exemplo;

import chat.dobot.bot.Contexto;
import chat.dobot.bot.DoBotConfig;
import chat.dobot.bot.annotations.Config;
import chat.dobot.bot.annotations.DoBotChat;
import chat.dobot.bot.annotations.EstadoChat;
import chat.dobot.bot.telegram.DoBotTelegramApp;

@DoBotChat(id = "meuBot", nome = "Bot de Teste")
public class AppBot {

    public static void main(String[] args) {
        DoBotTelegramApp.novoBot()
                .token("COLE_SEU_TOKEN_AQUI")
                .username("seu_bot_username")
                .botClass(AppBot.class)
                .start();
    }

    @Config
    public void config(DoBotConfig config) {
        config.setMensagemInicial(
                "Olá! Eu sou o Bot de Teste. Envie uma mensagem para conversar comigo."
        );
    }

    @EstadoChat(inicial = true)
    public void inicio(Contexto ctx) {
        String mensagem = ctx.getMensagemUsuario();
        ctx.responder("Você escreveu: " + mensagem);
    }
}
```

### 5.1 Entendendo o exemplo

- `@DoBotChat` identifica a classe como um chatbot DoBot.
- `DoBotTelegramApp.novoBot()` cria a aplicação para o canal Telegram.
- `.token(...)` autentica o bot e `.username(...)` informa seu username, sem
  `@`.
- `.botClass(AppBot.class)` seleciona explicitamente a classe do bot.
- `.start()` inicia o long polling e passa a buscar mensagens no Telegram.
- `@Config` marca o método que configura a mensagem inicial.
- `@EstadoChat(inicial = true)` marca o método executado no primeiro estado da
  conversa.
- `ctx.getMensagemUsuario()` lê o texto recebido.
- `ctx.responder(...)` envia uma resposta ao usuário.

### 5.2 Executar e testar

1. Salve o arquivo e substitua as credenciais.
2. Na IDE, execute o método `main` da classe `AppBot`.
3. No Telegram, abra o bot pelo username cadastrado.
4. Envie `/start` e depois uma mensagem, como `Oi`.
5. Confira se o bot responde com o texto recebido.

Para interromper a execução, use o botão de parada da IDE ou `Ctrl+C` no
terminal. Se o bot não responder, confirme o token, o username e se o processo
Java continua em execução.

## 6. GUIA RÁPIDO DA API DO DOBOT.CHAT

### a) Transição de estados conversacionais

Uma conversa pode ser dividida em estados. Cada método representa um estado
quando anotado com `@EstadoChat`. O estado inicial é marcado com
`inicial = true`; os demais podem receber um nome explícito em `estado`.

```java
@EstadoChat(inicial = true)
public void menu(Contexto ctx) {
    ctx.responder("Escolha uma opção.");
    ctx.proximoEstado("processarOpcao");
}

@EstadoChat(estado = "processarOpcao")
public void processarOpcao(Contexto ctx) {
    String opcao = ctx.getMensagemUsuario();

    if (opcao.equals("continuar")) {
        ctx.proximoEstado("proximaEtapa");
    } else {
        ctx.proximoEstado("menu");
    }
}

@EstadoChat(estado = "proximaEtapa")
public void proximaEtapa(Contexto ctx) {
    ctx.responder("Você chegou à próxima etapa.");
}
```

`ctx.proximoEstado("nomeMetodo")` define para qual método a próxima mensagem
será encaminhada. O texto usado deve corresponder ao nome do método ou ao valor
de `estado` na anotação. Use transições para organizar o fluxo, em vez de
concentrar toda a conversa em um único método.

### b) Teclados interativos de resposta

No Telegram, `responderComBotoes` envia uma mensagem acompanhada de botões de
resposta. Quando o usuário toca em um botão, o texto do botão chega como uma
mensagem comum ao próximo estado:

```java
ctx.responderComBotoes(
        "O que você deseja fazer?",
        "➕ Adicionar",
        "📋 Listar"
);
```

Os rótulos dos botões devem ser tratados como entradas do usuário. Portanto,
compare-os com cuidado e considere remover o teclado quando ele não for mais
necessário:

```java
ctx.removerTeclado("Certo! Vamos continuar.");
```

### c) Dados isolados por usuário na sessão

O contexto mantém dados da sessão de cada usuário. Use `setDado` para salvar
um valor e `getDado` para recuperá-lo:

```java
ctx.setDado("nome", "Ana");

String nome = ctx.getDado("nome", String.class);
ctx.responder("Olá, " + nome + "!");
```

Também é possível armazenar uma lista:

```java
List<String> tarefas = new ArrayList<>();
ctx.setDado("tarefas", tarefas);

List<String> salvas = ctx.getDado("tarefas", List.class);
```

Cada usuário do Telegram possui sua própria sessão, portanto os dados de uma
pessoa não são misturados com os dados de outra. Ainda assim, é importante
usar chaves claras e verificar se um valor ainda não existe antes de
inicializá-lo:

```java
List<String> tarefas = ctx.getDado("tarefas", List.class);
if (tarefas == null) {
    tarefas = new ArrayList<>();
    ctx.setDado("tarefas", tarefas);
}
```

Em código com tipos mais específicos, prefira trabalhar com a variável local
tipada e validar valores nulos antes de utilizá-los.

## 7. O DESAFIO PRÁTICO (GERENCIADOR DE TAREFAS)

Agora implemente um chatbot que funcione como um pequeno gerenciador de
tarefas. O objetivo é aplicar os estados conversacionais, os botões de
resposta e o armazenamento por usuário apresentados neste tutorial.

### Enunciado

Crie uma classe de bot anotada com `@DoBotChat` que apresente um menu inicial
com os botões:

- `➕ Adicionar Tarefa`;
- `📋 Listar Tarefas`;
- `🗑️ Limpar`.

O menu deve ser exibido no início da conversa e novamente sempre que uma ação
terminar.

### Requisitos funcionais

1. **Adicionar tarefa**
   - Ao escolher `➕ Adicionar Tarefa`, peça ao usuário o nome da tarefa.
   - No estado seguinte, leia a mensagem recebida.
   - Salve o nome em uma lista armazenada no contexto da sessão usando
     `ctx.setDado(...)`.
   - Confirme que a tarefa foi adicionada.
   - Retorne automaticamente ao menu inicial.

2. **Listar tarefas**
   - Ao escolher `📋 Listar Tarefas`, recupere a lista usando
     `ctx.getDado(...)`.
   - Informe as tarefas ao usuário, uma por linha ou numeradas.
   - Para uma lista vazia, mostre uma mensagem amigável explicando que ainda
     não há tarefas.
   - Retorne ao menu inicial.

3. **Limpar**
   - Ao escolher `🗑️ Limpar`, remova ou esvazie a lista da sessão.
   - Confirme a limpeza para o usuário.
   - Retorne automaticamente ao menu inicial.

4. **Robustez do fluxo**
   - Mensagens que não correspondem a uma opção devem receber uma orientação
     clara e voltar ao menu.
   - O estado de adição deve aceitar o texto da tarefa sem confundi-lo com
     uma opção do menu.
   - As tarefas de usuários diferentes devem permanecer isoladas.

### Roteiro sugerido para a implementação

Antes de escrever código, desenhe no papel os estados e transições:

```text
menu
 ├── Adicionar Tarefa -> receberTarefa -> menu
 ├── Listar Tarefas   -> menu
 └── Limpar           -> menu
```

Em seguida:

1. crie a classe e configure o token de teste;
2. implemente apenas o menu inicial;
3. teste os botões no Telegram;
4. implemente o estado de recebimento de tarefa;
5. adicione a lista no contexto da sessão;
6. implemente a listagem e o tratamento de lista vazia;
7. implemente a limpeza;
8. teste cada fluxo e também usuários ou sessões diferentes.

Não se preocupe em criar uma interface gráfica, banco de dados ou autenticação
para este desafio. O foco é praticar classes, métodos, anotações, listas,
condicionais, transições de estado e dados por sessão.

### Critérios de conclusão

O desafio estará concluído quando:

- o bot iniciar sem erros;
- os três botões aparecerem no menu;
- seja possível adicionar mais de uma tarefa;
- a listagem mostre todas as tarefas salvas na sessão correta;
- a limpeza remova as tarefas;
- cada ação retorne ao menu;
- uma sessão de usuário não consiga visualizar as tarefas de outra sessão.

