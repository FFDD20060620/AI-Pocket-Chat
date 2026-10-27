package com.situ.aichat.data.model

import androidx.annotation.StringRes
import com.situ.aichat.R

/**
 * 人设编译的**封闭词表**（活人感内核·卷一图纸 §3.2 / §3.3 全量逐字锁定）。
 *
 * 三张表：
 * - **增益 27 项**（[GAIN_KEYS] `g01`–`g27`，按 [GAIN_GROUPS] 九组顺序）——「什么事对她影响大」
 * - **条件 12 项**（[CONDITIONS] `c01`–`c12`）与**动作 10 项**（[ACTIONS] `a01`–`a10`）——算子的两端
 *
 * **key 恒为 `gNN`/`cNN`/`aNN`，中文标签走 zh 资源**（总图纸 §3.3：落库的是 key，改文案不会失配）。
 * 编译器输出落在这三张表之外的条目**整条丢弃并计数**，绝不静默吞（图纸 Y-6）。
 *
 * 档位存整数 0/1/2，系数只在 [gainFactor] 里、**不入库**（图纸 Y-7）；`system` map 里缺席的项即 [LEVEL_NORMAL]。
 *
 * ⚠️ 项数、key 连续性与三档系数由 `PersonaVocabTest` 逐条钉死——增删条目必须同步改那边的项数断言。
 */
object PersonaVocab {

    // MARK: - 增益三档（图纸 §3.2 锁定）

    /** 不吃这套。 */
    const val LEVEL_NUMB = 0

    /** 正常（`system` map 缺席即此档）。 */
    const val LEVEL_NORMAL = 1

    /** 很敏感（新专属项的默认档·D-10）。 */
    const val LEVEL_SENSITIVE = 2

    /** 档位 → 影响系数（锁定 `0.4 / 1.0 / 1.8`）。越界档位按「正常」处理，绝不放大。 */
    fun gainFactor(level: Int): Float = when (level) {
        LEVEL_NUMB -> 0.4f
        LEVEL_SENSITIVE -> 1.8f
        else -> 1.0f
    }

    /** 档位 → 中文标签资源（下标即档位；越界回落「正常」）。 */
    @StringRes
    fun levelLabelRes(level: Int): Int = LEVEL_LABEL_RES.getOrElse(level) { LEVEL_LABEL_RES[LEVEL_NORMAL] }

    private val LEVEL_LABEL_RES = listOf(
        R.string.persona_gain_level_numb,
        R.string.persona_gain_level_normal,
        R.string.persona_gain_level_sensitive,
    )

    /** 算子条数上限（图纸 §3.3：超出按顺序截断并计数）。 */
    const val MAX_OPERATORS = 8

    // MARK: - 增益 27 项 × 九组

    /** 一组增益：组标题 + 该组的 key（组内顺序即 UI 展示顺序）。 */
    data class GainGroup(@StringRes val labelRes: Int, val keys: List<String>)

    val GAIN_GROUPS: List<GainGroup> = listOf(
        GainGroup(R.string.persona_gain_group_1, listOf("g01", "g02", "g03")),
        GainGroup(R.string.persona_gain_group_2, listOf("g04", "g05", "g06")),
        GainGroup(R.string.persona_gain_group_3, listOf("g07", "g08", "g09")),
        GainGroup(R.string.persona_gain_group_4, listOf("g10", "g11", "g12")),
        GainGroup(R.string.persona_gain_group_5, listOf("g13", "g14", "g15")),
        GainGroup(R.string.persona_gain_group_6, listOf("g16", "g17", "g18")),
        GainGroup(R.string.persona_gain_group_7, listOf("g19", "g20", "g21")),
        GainGroup(R.string.persona_gain_group_8, listOf("g22", "g23", "g24")),
        GainGroup(R.string.persona_gain_group_9, listOf("g25", "g26", "g27")),
    )

    /** key → 中文标签资源。 */
    val GAINS: Map<String, Int> = mapOf(
        "g01" to R.string.persona_gain_g01,
        "g02" to R.string.persona_gain_g02,
        "g03" to R.string.persona_gain_g03,
        "g04" to R.string.persona_gain_g04,
        "g05" to R.string.persona_gain_g05,
        "g06" to R.string.persona_gain_g06,
        "g07" to R.string.persona_gain_g07,
        "g08" to R.string.persona_gain_g08,
        "g09" to R.string.persona_gain_g09,
        "g10" to R.string.persona_gain_g10,
        "g11" to R.string.persona_gain_g11,
        "g12" to R.string.persona_gain_g12,
        "g13" to R.string.persona_gain_g13,
        "g14" to R.string.persona_gain_g14,
        "g15" to R.string.persona_gain_g15,
        "g16" to R.string.persona_gain_g16,
        "g17" to R.string.persona_gain_g17,
        "g18" to R.string.persona_gain_g18,
        "g19" to R.string.persona_gain_g19,
        "g20" to R.string.persona_gain_g20,
        "g21" to R.string.persona_gain_g21,
        "g22" to R.string.persona_gain_g22,
        "g23" to R.string.persona_gain_g23,
        "g24" to R.string.persona_gain_g24,
        "g25" to R.string.persona_gain_g25,
        "g26" to R.string.persona_gain_g26,
        "g27" to R.string.persona_gain_g27,
    )

    /** 27 项 key，按九组顺序展平（= 词表的规范序，编译提示词与 UI 都用它）。 */
    val GAIN_KEYS: List<String> = GAIN_GROUPS.flatMap { it.keys }

    // MARK: - 算子两端（条件 12 × 动作 10）

    /** 条件 key → 中文标签资源。卷一只存不求值（求值在卷三/卷四）。 */
    val CONDITIONS: Map<String, Int> = mapOf(
        "c01" to R.string.persona_cond_c01,
        "c02" to R.string.persona_cond_c02,
        "c03" to R.string.persona_cond_c03,
        "c04" to R.string.persona_cond_c04,
        "c05" to R.string.persona_cond_c05,
        "c06" to R.string.persona_cond_c06,
        "c07" to R.string.persona_cond_c07,
        "c08" to R.string.persona_cond_c08,
        "c09" to R.string.persona_cond_c09,
        "c10" to R.string.persona_cond_c10,
        "c11" to R.string.persona_cond_c11,
        "c12" to R.string.persona_cond_c12,
    )

    /** 动作 key → 中文标签资源。 */
    val ACTIONS: Map<String, Int> = mapOf(
        "a01" to R.string.persona_act_a01,
        "a02" to R.string.persona_act_a02,
        "a03" to R.string.persona_act_a03,
        "a04" to R.string.persona_act_a04,
        "a05" to R.string.persona_act_a05,
        "a06" to R.string.persona_act_a06,
        "a07" to R.string.persona_act_a07,
        "a08" to R.string.persona_act_a08,
        "a09" to R.string.persona_act_a09,
        "a10" to R.string.persona_act_a10,
    )

    // MARK: - 提示词专用中文标签（活人感内核卷三 §3.7·硬编码中文·与 zh 资源 persona_gain_gNN 同源）

    /**
     * 27 项增益的**提示词专用**中文标签（卷三图纸 §3.7 逐字锁定 = zh 资源 `persona_gain_gNN` 现值）。
     * 提示词是 LLM 读的产品资产、恒中文、不走资源（对齐既有口径）；UI 仍读 [GAINS] 的资源。
     * ⚠️ 与 zh 资源**同源双写**：改一处必改另一处——`AffectCoefficientsTest` 用 Robolectric 读 zh 资源逐字比对，改了一边必红。
     * 消费点：人设编译提示词（K-14·修卷一「只给 key 不给标签」缺口）与成长分析提示词「敏感点命中」段。
     */
    val GAIN_PROMPT_LABELS: Map<String, String> = mapOf(
        "g01" to "被关心问候",
        "g02" to "被冷落 · 已读不回",
        "g03" to "被黏得太紧",
        "g04" to "被夸奖肯定",
        "g05" to "被批评否定",
        "g06" to "被小瞧 · 被当空气",
        "g07" to "被真正听懂",
        "g08" to "被误解",
        "g09" to "你记得她说过的小事",
        "g10" to "被逗笑",
        "g11" to "一起做点什么",
        "g12" to "例行公事 · 没新鲜感",
        "g13" to "吵架 · 被凶",
        "g14" to "冷战 · 冷暴力",
        "g15" to "道歉与和好",
        "g16" to "收到礼物",
        "g17" to "被照顾",
        "g18" to "被爽约 · 被辜负",
        "g19" to "被隐瞒欺骗",
        "g20" to "承诺被兑现",
        "g21" to "你对她坦白脆弱",
        "g22" to "被撩 · 暧昧试探",
        "g23" to "身体亲密（线下）",
        "g24" to "亲密被拒绝",
        "g25" to "独处 · 深夜",
        "g26" to "意外与变化",
        "g27" to "被抛弃的信号",
    )

    /**
     * 提示词里的一行：`g13 吵架 · 被凶`（key 与中文标签并列，LLM 不用再猜编号）。未知 key 只回 key 本身。
     * 消费点 = 人设编译提示词与成长分析提示词——两处的「你」都是模型自己、角色叫 TA，而 g09 / g21 的 UI 标签带
     * 「你（= 用户）/ 她（= 角色）」，原样放进去会人称对调，这两条换成 [PROMPT_NEUTRAL_LABELS]（2026-09-29 体检后续）。
     */
    fun gainPromptLine(key: String): String = (PROMPT_NEUTRAL_LABELS[key] ?: GAIN_PROMPT_LABELS[key])?.let { "$key $it" } ?: key

    /** 编译 / 分析提示词专用的中性说法（只影响 [gainPromptLine]；[GAIN_PROMPT_LABELS] 与 zh 资源、编辑页零改）。 */
    private val PROMPT_NEUTRAL_LABELS: Map<String, String> = mapOf(
        "g09" to "对方记得 TA 说过的小事",
        "g21" to "对方向 TA 坦白脆弱",
    )

    /** 修缮卷 §3.7：只要中文标签（含 ` · ` 原样）——敏感点行 `PersonaGainsLine` 用；未知 key ⇒ null。 */
    fun gainLabel(key: String): String? = GAIN_PROMPT_LABELS[key]

    /**
     * 算子两端的**提示词专用**中文标签（体检小毛病修复 2026-09-29·F5a：编译提示词原先只列 `c01`/`a01` 编号，
     * 模型看不懂只能留空，体检里三个角色的「固定反应」全空）。与 zh 资源 `persona_cond_cNN` / `persona_act_aNN`
     * **同源双写**：改一处必改另一处——`AffectCoefficientsTest` 用 Robolectric 读 zh 资源逐字比对。
     */
    val CONDITION_PROMPT_LABELS: Map<String, String> = mapOf(
        "c01" to "想道歉的时候",
        "c02" to "想被哄的时候",
        "c03" to "想试探的时候",
        "c04" to "想躲的时候",
        "c05" to "想分享的时候",
        "c06" to "想确认的时候",
        "c07" to "被夸的时候",
        "c08" to "被批评的时候",
        "c09" to "被冷落的时候",
        "c10" to "深夜一个人的时候",
        "c11" to "情绪很差的时候",
        "c12" to "关系刚有进展的时候",
    )

    /** 见 [CONDITION_PROMPT_LABELS]。 */
    val ACTION_PROMPT_LABELS: Map<String, String> = mapOf(
        "a01" to "不直说，绕着表达",
        "a02" to "转移话题",
        "a03" to "话变少",
        "a04" to "更想找人说话",
        "a05" to "嘴上否认，行动上在意",
        "a06" to "先冷一下再回",
        "a07" to "用玩笑掩饰",
        "a08" to "反问回去",
        "a09" to "主动找点别的事做",
        "a10" to "说反话",
    )

    /** 编译提示词里的一行：`c04 想躲的时候`。未知 key 只回 key 本身。 */
    fun conditionPromptLine(key: String): String = CONDITION_PROMPT_LABELS[key]?.let { "$key $it" } ?: key

    /** 编译提示词里的一行：`a05 嘴上否认，行动上在意`。未知 key 只回 key 本身。 */
    fun actionPromptLine(key: String): String = ACTION_PROMPT_LABELS[key]?.let { "$key $it" } ?: key
}
