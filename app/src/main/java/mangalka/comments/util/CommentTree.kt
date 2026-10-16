package mangalka.comments.util

import mangalka.comments.model.CommentPage
import mangalka.comments.model.UnifiedComment

/**
 * Приводит плоский список комментариев к дереву по [UnifiedComment.parentId].
 *
 * Требования, которые здесь решаются, потому что источники грязные:
 *  - `parentId`, указывающий на несуществующий комментарий (удалённый родитель),
 *    не должен «съедать» ветку — такой узел поднимается в корень;
 *  - циклы (`a -> b -> a`) не должны вешать построитель;
 *  - порядок корней и порядок внутри ветки сохраняется как во входном списке;
 *  - [UnifiedComment.depth] проставляется от 0.
 *
 * Уже готовые деревья (адаптер сам вернул `children`) проходят функцию без изменений,
 * если у детей не выставлен `parentId`.
 */
fun buildCommentTree(flat: List<UnifiedComment>): List<UnifiedComment> {
    if (flat.isEmpty()) return emptyList()

    // Последний с данным id выигрывает — источники иногда дублируют комментарий
    // одновременно в root[] и в replies[].
    val byId = LinkedHashMap<String, UnifiedComment>(flat.size)
    for (c in flat) byId[c.id] = c

    val childIds = LinkedHashMap<String, MutableList<String>>()
    val rootIds = ArrayList<String>()

    for (c in byId.values) {
        val parent = c.parentId
        if (parent == null || parent == c.id || parent !in byId) {
            rootIds += c.id
        } else {
            childIds.getOrPut(parent) { ArrayList() } += c.id
        }
    }

    // Разрываем циклы: узел, недостижимый из корней, становится корнем.
    val reachable = HashSet<String>(byId.size)
    run {
        val stack = ArrayDeque(rootIds)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!reachable.add(id)) continue
            childIds[id]?.let { stack.addAll(it) }
        }
    }
    if (reachable.size != byId.size) {
        for (id in byId.keys) {
            if (id !in reachable) {
                rootIds += id
                reachable += id
                childIds[byId[id]!!.parentId]?.remove(id)
                val stack = ArrayDeque(childIds[id].orEmpty())
                while (stack.isNotEmpty()) {
                    val n = stack.removeLast()
                    if (reachable.add(n)) childIds[n]?.let { stack.addAll(it) }
                }
            }
        }
    }

    fun assemble(id: String, depth: Int): UnifiedComment {
        val node = byId.getValue(id)
        val kids = childIds[id].orEmpty().map { assemble(it, depth + 1) }
        return node.copy(depth = depth, children = kids)
    }

    return rootIds.map { assemble(it, 0) }
}

/** Разворачивает дерево в плоский список в порядке отображения (pre-order). */
fun List<UnifiedComment>.flattenForDisplay(): List<UnifiedComment> {
    val out = ArrayList<UnifiedComment>()
    fun walk(nodes: List<UnifiedComment>) {
        for (n in nodes) {
            out += n
            walk(n.children)
        }
    }
    walk(this)
    return out
}

/**
 * Оставляет только ветви, относящиеся к конкретной странице главы.
 * Родитель без `pageNumber == pageNumber` сохраняется как контекст, если внутри
 * него есть подходящий потомок. Благодаря этому ответ не превращается в
 * непонятный корневой комментарий после фильтрации.
 */
fun List<UnifiedComment>.filterForPage(pageNumber: Int): List<UnifiedComment> {
    fun filterNode(node: UnifiedComment): UnifiedComment? {
        val children = node.children.mapNotNull(::filterNode)
        val matches = node.pageNumber == pageNumber
        return if (matches || children.isNotEmpty()) {
            node.copy(children = children)
        } else {
            null
        }
    }
    return mapNotNull(::filterNode)
}

/** Число комментариев, непосредственно помеченных указанной страницей. */
fun List<UnifiedComment>.countForPage(pageNumber: Int): Int =
    flattenForDisplay().count { it.pageNumber == pageNumber }

/**
 * Склеивает соседние страницы треда без потери вложенности.
 *
 * API разных движков может повторять корневой комментарий на следующей странице
 * вместе с новыми ответами. Поэтому сначала разворачиваем оба дерева, убираем
 * готовые children/depth, дедуплицируем по id и строим дерево заново.
 */
fun mergeCommentPages(current: CommentPage, next: CommentPage): CommentPage {
    val byId = LinkedHashMap<String, UnifiedComment>()
    (current.comments.flattenForDisplay() + next.comments.flattenForDisplay()).forEach { comment ->
        byId[comment.id] = comment.copy(children = emptyList(), depth = 0)
    }
    return CommentPage(
        comments = buildCommentTree(byId.values.toList()),
        page = maxOf(current.page, next.page),
        hasNextPage = next.hasNextPage,
        totalCount = next.totalCount ?: current.totalCount,
        providerId = next.providerId,
    )
}
