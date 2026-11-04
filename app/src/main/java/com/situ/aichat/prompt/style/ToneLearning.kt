package com.situ.aichat.prompt.style

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 去 AI 味时**学到**的三样东西（活人感二期 D 件·方案 §7）：改写对照、负例、语风样本。
 *
 * 三样的价值不同，注入顺序也不同：
 * - [rewrites] 最贵：用户亲手把「AI 说成 A」改成「应该说成 B」，一对就是一次免费的 few-shot；
 * - [negatives] 次之：只告诉模型「别这么说」，没有正面示范；
 * - [styleSamples] 最轻：从用户自己打的句子里学口气，不当内容用。
 *
 * 全部只存本地 DataStore，**不进备份**（`BackupExporter` 只序列化 AppSettings）、不进日志、不参与向量检索。
 */
@Serializable
data class ToneRewrite(
    /** AI 原句（截到 [ToneLearned.MAX_SNIPPET]，只留被改的那句）。 */
    val original: String,
    /** 用户改后的句子。 */
    val edited: String,
    val at: Long,
)

@Serializable
data class ToneNegative(
    val snippet: String,
    val at: Long,
)

@Serializable
data class StyleSample(
    val text: String,
    val at: Long,
)

@Serializable
data class ToneLearned(
    val rewrites: List<ToneRewrite> = emptyList(),
    val negatives: List<ToneNegative> = emptyList(),
    val styleSamples: List<StyleSample> = emptyList(),
) {
    /** 追加一对改写（同一原句只留最新一条，FIFO 上限 [MAX_REWRITES]）。 */
    fun withRewrite(original: String, edited: String, at: Long): ToneLearned {
        val next = ToneRewrite(
            original = original.trim().take(MAX_SNIPPET),
            edited = edited.trim().take(MAX_SNIPPET),
            at = at,
        )
        if (next.original.isEmpty() || next.edited.isEmpty()) return this
        val kept = rewrites.filterNot { it.original == next.original }.toMutableList()
        kept.add(0, next)
        return copy(rewrites = kept.take(MAX_REWRITES))
    }

    /** 追加一条负例（同句去重，FIFO 上限 [MAX_NEGATIVES]）。 */
    fun withNegative(snippet: String, at: Long): ToneLearned {
        val text = snippet.trim().take(MAX_SNIPPET)
        if (text.isEmpty()) return this
        val kept = negatives.filterNot { it.snippet == text }.toMutableList()
        kept.add(0, ToneNegative(text, at))
        return copy(negatives = kept.take(MAX_NEGATIVES))
    }

    /** 追加一条语风样本（不合格 / 重复直接返回 this，调用方不必再判）。 */
    fun withStyleSample(text: String, at: Long): ToneLearned {
        val trimmed = text.trim()
        if (!StyleSampler.isEligible(trimmed)) return this
        if (styleSamples.any { it.text == trimmed }) return this
        val kept = styleSamples.toMutableList()
        kept.add(0, StyleSample(trimmed, at))
        return copy(styleSamples = kept.take(MAX_STYLE_SAMPLES))
    }

    /** 注入时取用：改写全部带，负例只带最早进入的三条（越新越可能是同一种毛病，带多了只是重复）。 */
    fun forPrompt(): ToneLearned = copy(
        rewrites = rewrites.take(MAX_REWRITES),
        negatives = negatives.take(3),
        styleSamples = styleSamples.take(3),
    )

    val isEmpty: Boolean
        get() = rewrites.isEmpty() && negatives.isEmpty() && styleSamples.isEmpty()

    companion object {
        const val MAX_REWRITES = 5
        const val MAX_NEGATIVES = 100
        const val MAX_STYLE_SAMPLES = 30

        /** 单条片段上限：只留命中的那句，别把整条回复搬进提示词。 */
        const val MAX_SNIPPET = 60

        val EMPTY = ToneLearned()

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** 读坏的 JSON 一律回落空（宁可不学，也不能让提示词装配崩）。 */
        fun parse(raw: String?): ToneLearned {
            if (raw.isNullOrBlank()) return EMPTY
            return runCatching { json.decodeFromString(serializer(), raw) }.getOrDefault(EMPTY)
        }

        fun encode(value: ToneLearned): String = json.encodeToString(serializer(), value)
    }
}

/**
 * 语风取样判据（纯函数·可单测）：只收「像日常说话」的短句。
 *
 * 排除的四类都是会教坏模型的东西：指令（「帮我写个…」）、提问（「怎么…？」）、链接、卡片哨兵 `[...]`。
 * 长短卡在 4~40 字：太短没有口气、太长多半是正经内容。
 */
object StyleSampler {
    private val bannedPhrases = listOf(
        "帮我", "请你", "怎么", "为什么", "能不能", "可不可以", "要不要", "是不是",
    )

    fun isEligible(text: String): Boolean {
        val t = text.trim()
        if (t.length !in 4..40) return false
        if (t.contains('\n')) return false
        if (t.contains("http") || t.contains("www.")) return false
        if (t.contains('[') || t.contains(']')) return false
        if (t.contains('?') || t.contains('？')) return false
        return bannedPhrases.none { t.contains(it) }
    }
}
