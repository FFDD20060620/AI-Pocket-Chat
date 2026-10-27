package com.situ.aichat.prompt

import com.situ.aichat.data.model.PersonaGains
import com.situ.aichat.data.model.PersonaVocab

/**
 * 「她吃哪套」敏感点行（图纸 2026-09-03 §3 · **修订自活人感内核修缮卷 §3.7（2026-09-03 用户拍板）**）：
 * 把增益档位写成**反差句 0–3 行 + 平铺行 0–1 行**，接在 Growth 段性格块末尾
 * （`PromptBuilderGrowth.buildPersonalityDescription` 的 `extraLines`），让聊天模型直接知道她在意什么、又对什么无感。
 *
 * **修订了修缮卷 §3.7 四项**：席位 敏感 3→系统 5（专属项全留、不设上限） / 无感 2→3 · 字数刹车 60→150 ·
 * 单行 `String`→多行 `List<String>` · 平铺行标签改用[入句变体][gainInlineLabel]。其余（回归钉、挂载位置、
 * `growthSystemEnabled` 门控）逐条继承。
 *
 * **为什么要反差句**：平铺行把她的矛盾拆成两半（「重视清单」与「敷衍清单」），模型不会主动把两半配对成「她很拧巴」；
 * 反差句把配对表 [PersonaGainsPairs.PAIRS] 里命中的矛盾直接写成一句带转折的话，信息密度最高。
 *
 * **步骤序锁定**（图纸 §3.2 / §9 ④，不许调换）：建集合 → 配对判定（用**完整**档位数据，不受席位限制）→
 * 剔除被反差句消费掉的项 → 取席位 → 渲染 → 刹车。刹车**只砍平铺行、反差句一行不砍**（P4）。
 */
internal fun buildPersonaGainsLines(gains: PersonaGains, userName: String): List<String> {
    // 步骤 1 · 建集合。专属项用原文 label（自由文本、不参与配对·N4）——入句前按本行人称换算（见 [customGainInlineLabel]）；
    // 系统项恒按 GAIN_KEYS 序（日常 → 极端）。
    val customSensitive = gains.custom.filter { it.level == PersonaVocab.LEVEL_SENSITIVE }.map { customGainInlineLabel(it.label, userName) }
    var sysSens = PersonaVocab.GAIN_KEYS.filter { gains.system[it] == PersonaVocab.LEVEL_SENSITIVE }
    var sysNumb = PersonaVocab.GAIN_KEYS.filter { gains.system[it] == PersonaVocab.LEVEL_NUMB }
    // 回归钉：增益全默认 ⇒ 两行都不出 ⇒ Growth 段输出逐字节同修缮前。
    if (customSensitive.isEmpty() && sysSens.isEmpty() && sysNumb.isEmpty()) return emptyList()

    // 步骤 2 · 配对判定（P1：拿完整集合判，不许先截席位——排在第 6 位不代表这个人格不成立）。
    // PAIRS 声明序即优先级（tier 1 → 2 → 3），此处**绝不** sortedBy：运行时排序会掩盖声明序错误。
    val sensKeys = sysSens.toSet()
    val numbKeys = sysNumb.toSet()
    val chosen = PersonaGainsPairs.PAIRS
        .filter { pair -> sensKeys.containsAll(pair.sensitive) && numbKeys.containsAll(pair.numb) }
        .take(MAX_PAIRS)

    // 步骤 3 · 剔除：反差句已经说过的项不再进平铺行（同一件事不说两遍）。
    val consumed = chosen.flatMapTo(mutableSetOf()) { it.sensitive + it.numb }
    sysSens = sysSens - consumed
    sysNumb = sysNumb - consumed

    // 步骤 4/5 · 取席位并解析成入句标签（未知 key 在 GAIN_KEYS.filter 已滤掉，mapNotNull 是第二道）。
    val sensitiveOut = (
        customSensitive + sysSens.take(MAX_SYS_SENSITIVE).mapNotNull { gainInlineLabel(it, userName) }
        ).toMutableList()
    val numbOut = sysNumb.take(MAX_SYS_NUMB).mapNotNull { gainInlineLabel(it, userName) }.toMutableList()

    /** 平铺行三形态；两侧都被反差句吃光 ⇒ null（不输出空句子·P5）。 */
    fun renderFlat(): String? {
        val sPart = "你特别在意${sensitiveOut.joinToString("、")}"
        val nPart = "对${numbOut.joinToString("、")}不太吃这套"
        return when {
            sensitiveOut.isNotEmpty() && numbOut.isNotEmpty() -> "- $sPart，$nPart。"
            sensitiveOut.isNotEmpty() -> "- $sPart。"
            numbOut.isNotEmpty() -> "- 你$nPart。"
            else -> null
        }
    }

    // 步骤 6 · 刹车：两行合计 > MAX_TOTAL_CHARS 时依次去 numb 末项、再去 sensitive 末项（各至少留 1，仍超则保留）。
    val pairLines = chosen.map { it.render(userName) }
    val pairChars = pairLines.sumOf { it.length }
    var flatLine = renderFlat()
    while (flatLine != null && pairChars + flatLine.length > MAX_TOTAL_CHARS) {
        when {
            numbOut.size > 1 -> numbOut.removeAt(numbOut.lastIndex)
            sensitiveOut.size > 1 -> sensitiveOut.removeAt(sensitiveOut.lastIndex)
            else -> break
        }
        flatLine = renderFlat()
    }
    return pairLines + listOfNotNull(flatLine)
}

/**
 * 专属敏感点入句前的人称换算（2026-09-29 体检后续·微图纸 docs/handoff/2026-09-29-体检小毛病修复.md §7.2）。
 * 标签按编辑页的口径存——那里「她」= 角色、「你」= 用户（编辑页写着「她在意的事」）；编译读出来的多写「自己」「对方」。
 * 而这一行是写给角色看的：「你」= 角色、用户叫名字。原样拼进「你特别在意…」人称就对调了（体检实录：男角色的
 * 「认真试玩她的游戏」→「你特别在意认真试玩她的游戏」，游戏成了别人的）。换算：`对方` → 用户名；`你`（不含「你们」）→ 用户名；
 * `她 / 他 / TA`（不含「她们 / 他们 / 其他 / 他人 / 吉他」）→ `你`。「自己」「你们」原样（本就指得对）。
 * 只用在聊天这一行；成长分析提示词里专属标签仍原文照抄（`custom_hits` 要按原标签对回去）。
 */
internal fun customGainInlineLabel(label: String, userName: String): String =
    label
        .replace("对方", USER_SLOT)
        .replace(USER_PRONOUN, USER_SLOT)
        .replace(CHARACTER_PRONOUN, "你")
        .replace(USER_SLOT, userName)

/** 换算中转占位（先把指用户的词收起来，再把指角色的词换成「你」，最后放回用户名——避免「你」被二次换算）。 */
private const val USER_SLOT = "\u0000"
private val USER_PRONOUN = Regex("你(?!们)")
private val CHARACTER_PRONOUN = Regex("""(?<![其吉])[她他](?![们人])|(?<![A-Za-z])(?:TA|Ta|ta)(?![A-Za-z]) ?""")

/** 图纸 §9 ② 锁定：反差句 ≤ 3 行 · 系统敏感 ≤ 5 项（专属项**不设上限**）· 不吃这套 ≤ 3 项 · 两行合计 ≤ 150 字。 */
private const val MAX_PAIRS = 3
private const val MAX_SYS_SENSITIVE = 5
private const val MAX_SYS_NUMB = 3
private const val MAX_TOTAL_CHARS = 150
