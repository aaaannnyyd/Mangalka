package mangalka.comments.tools

import mangalka.comments.adapter.AutoCommentAdapter
import mangalka.comments.util.flattenForDisplay
import java.io.File

/**
 * Отладочный прогон [AutoCommentAdapter] на сохранённой странице.
 *
 * Нужен потому, что эвристику нельзя считать проверенной, пока она не отработала
 * на настоящем HTML конкретного движка. Сохраняешь страницу тайтла (любым
 * способом, вплоть до «Share → Save page» из WebView внутри приложения) и:
 *
 * ```
 * kotlin -cp "$CP:build/main" mangalka.comments.tools.AutoParserCliKt page.html https://site.tld/manga/1
 * ```
 *
 * Печатает дерево, чтобы глазами увидеть: не утёк ли текст ответов в родителя,
 * правильно ли определилась вложенность, подхватились ли аватары/даты/голоса.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("usage: AutoParserCli <file.html> [baseUrl] [--min-score N]")
        return
    }
    val file = File(args[0])
    require(file.isFile) { "no such file: ${file.absolutePath}" }
    val baseUrl = args.getOrNull(1)?.takeUnless { it.startsWith("--") } ?: "https://example.org"
    val minScore = args.toList().zipWithNext()
        .firstOrNull { it.first == "--min-score" }?.second?.toIntOrNull() ?: 12

    val page = AutoCommentAdapter(minScore = minScore).parse(file.readText(), baseUrl)

    println("provider=${page.providerId} roots=${page.comments.size} total=${page.totalCount} next=${page.hasNextPage}")
    if (page.comments.isEmpty()) {
        println("\nКонтейнер не найден или балл ниже $minScore.")
        println("Попробуй --min-score меньше и проверь, не рендерятся ли комментарии на JS.")
        return
    }
    println()
    page.comments.flattenForDisplay().forEach { c ->
        val pad = "    ".repeat(c.depth)
        val votes = c.votes.effectiveScore?.let { " [$it]" }.orEmpty()
        val spoiler = if (c.hasSpoiler) " {spoiler}" else ""
        val date = c.createdAt?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: "-"
        println("$pad#${c.id} <- ${c.parentId ?: "root"}  @${c.author.name}$votes$spoiler  $date")
        println("$pad  avatar=${c.author.avatarUrl ?: "-"}")
        println("$pad  ${c.plainText.replace("\n", " ").take(160)}")
    }
}
