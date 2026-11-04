package com.situ.aichat.prompt

/**
 * 运行时去 AI 腔（活人感二期 C 件·方案 §6）。
 *
 * 只做一件事：把**一眼就是模板**的句子删掉，其余一个字不动。
 * 刻意保持高精度、低覆盖——宁可漏掉一句，也不要把正常的话剪碎（B13/B14）。
 *
 * 三条硬约束：
 * 1. 纯函数、无 IO、无随机：同一个输入永远同一个输出，单测才能断言；
 * 2. 只在普通聊天调用，且用户开了「自动删掉 AI 腔」才跑；
 * 3. 删空了**不算数**——调用方拿回空串时必须退回原文（回合不能变空，否则触发重试链）。
 */
internal object AiToneDetector {

    internal enum class Rule(val label: String) {
        CUSTOMER_SERVICE("客服腔"),
        META_AI("元叙述"),
        ELEVATION("升华结尾"),
        TEMPLATE_OPENER("模板开场"),
        PREACHING("说教"),
        WAITING_TAIL("等待式结尾"),
        ECHO("话语余波"),
        REPORT_TONE("报告腔"),
    }

    internal data class Hit(val rule: Rule, val snippet: String)

    /** 整行命中即删（这些句子没有信息量，留着只会更假）。 */
    private val cannedPatterns: List<Pair<Rule, Regex>> = listOf(
        Rule.CUSTOMER_SERVICE to Regex("有什么(可以|能)(帮|为)(您|你)"),
        Rule.CUSTOMER_SERVICE to Regex("(希望)?(这个回答|这些内容)?对(您|你)(有所)?帮助"),
        Rule.CUSTOMER_SERVICE to Regex("还有(什么|别的)(可以|需要)(帮|问)"),
        Rule.CUSTOMER_SERVICE to Regex("随时(找|联系)(我|我们)"),
        Rule.META_AI to Regex("作为(一个|一名)?(AI|人工智能|语言模型)"),
        Rule.META_AI to Regex("我(无法|不能)(访问|提供|代替)"),
        Rule.ELEVATION to Regex("愿你(一直|永远|每天)?"),
        Rule.ELEVATION to Regex("相信(你|您)(一定|可以|能)"),
        Rule.TEMPLATE_OPENER to Regex("^(首先|其次|再次|最后|另外|此外|总之|综上)[，,、：:]"),
        Rule.TEMPLATE_OPENER to Regex("值得(注意|一提)的是"),
        Rule.PREACHING to Regex("(记住|要记住|请记住)[，,]"),
        // 等待式结尾（酒馆《狐神抚》去欧规范 §31：整句只等回复 = 最典型的 AI 收尾）
        Rule.WAITING_TAIL to Regex("^(我)?在等(你|您)(的)?(回复|开口|回答|回应|决定|说话)[。！!]?$"),
        Rule.WAITING_TAIL to Regex("^等你(的)?(回复|开口|决定|说话)[。！!]?$"),
        Rule.WAITING_TAIL to Regex("^等着你[^。！？!?]{0,6}[。！!]?$"),
        // 报告腔（禁机械词语 §5）
        Rule.REPORT_TONE to Regex("呈现出.{0,10}的状态"),
        Rule.REPORT_TONE to Regex("^从某种意义上"),
        Rule.REPORT_TONE to Regex("^综上所述"),
        Rule.REPORT_TONE to Regex("^值得注意的是"),
    )

    /** 句尾命中就截掉（说教式收尾，前面的话还留着）。 */
    private val tailPatterns: List<Regex> = listOf(
        Regex("[，,]?希望(你|您)(今天|以后|每天)?(都)?(能|可以)?(开心|好好的|顺利|加油)[^。！？!?]*[。！!]?$"),
        Regex("[，,]?(加油|共勉|祝你好运)[！!。]?$"),
        Regex("[，,]?相信(你|您)(一定|可以|能)[^。！？!?]*[。！!]?$"),
        // 等待式结尾（长句里拖一句「在等你回复」→ 截掉，前半句保留）
        Regex("[，,]?(我)?在等(你|您)(的)?(回复|回答|回应|开口|决定|说话)[^。！？!?]*[。！!]?$"),
        Regex("[，,]?等你(的)?(回复|开口|决定|说话)[^。！？!?]*[。！!]?$"),
        Regex("[，,]?等着你[^。！？!?]*[。！!]?$"),
        // 话语余波（去欧规范 §15/§31）
        Regex("[，,]?这句话(落下|传开|散开)[^。！？!?]*[。！!]?$"),
        Regex("[，,]?话说完[，,]?[^。！？!?]*[。！!]?$"),
    )

    /** 命中明细（只用于日志与诊断，不参与「删不删」的判定）。 */
    internal fun detect(text: String): List<Hit> = buildList {
        text.split('\n').forEach { line ->
            val hit = firstCanned(line)
            if (hit != null) add(Hit(hit.first, hit.second))
        }
    }

    /**
     * 删掉模板句、截掉模板尾巴。
     *
     * @return 剩下的正文；**全被删光时返回空串**，由调用方决定退回原文。
     */
    internal fun strip(text: String): String {
        val kept = text.split('\n').mapNotNull { line ->
            if (firstCanned(line) != null) return@mapNotNull null
            cutTail(line).trim().takeIf { it.isNotEmpty() }
        }
        return kept.joinToString("\n").trim()
    }

    private fun firstCanned(line: String): Pair<Rule, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        // 整行必须是「短 + 命中」才算模板；长句里夹一句模板是正常说话。
        val plain = trimmed.replace(Regex("[\\s，,。.！!？?、；;：:]"), "")
        if (plain.length > MAX_CANNED_LENGTH) return null
        cannedPatterns.forEach { (rule, regex) ->
            regex.find(trimmed)?.let { return rule to it.value }
        }
        return null
    }

    private fun cutTail(line: String): String {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return ""
        var out = trimmed
        tailPatterns.forEach { regex ->
            regex.find(out)?.let { m ->
                // 只在句尾（后半段）截，防把开头就说「加油」的整句截没。
                if (m.range.first >= out.length / 2) out = out.substring(0, m.range.first)
            }
        }
        return out.trim().trimEnd('，', ',', '、', ' ')
    }

    /** 超过这个长度的行不当模板（普通话不会两句话都是套话）。 */
    private const val MAX_CANNED_LENGTH = 30
}
