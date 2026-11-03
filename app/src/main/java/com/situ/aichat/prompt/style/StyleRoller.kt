package com.situ.aichat.prompt.style

import kotlin.random.Random

/**
 * 「像真人打字」三旋钮（活人感二期 B 件·2026-11-03，方案见 2/记录/06-去AI八股方案.md §5.9）。
 *
 * 为什么是三个小旋钮，而不是一条大规则：真人聊天的不完美是**结构性**的——同一句话，标点不同、
 * 长短不同、逻辑松紧不同，读起来就是两个人。模型默认会把每一条都写得「完整、工整、有逻辑」，
 * 那正是八股味。这里每轮由代码掷一次骰子，只改这一条的三个维度，且**只放后缀区**（不进系统提示词，
 * 否则每轮变 → 前缀缓存全废，见方案 B25）。
 *
 * 纯函数、无副作用、可单测：种子由调用方给（生产用 [System.nanoTime]，测试给固定值）。
 */
enum class PunctuationTone { CASUAL, NORMAL }

/** 长度档：55% 一句、30% 两三句、12% 四五句、3% 放开说（方案 §5.9 的 55/30/12/3）。 */
enum class LengthTone { ONE_LINE, TWO_THREE, FOUR_FIVE, OPEN }

/** 逻辑档：60% 顺着说、30% 可以跳、10% 不用讲逻辑。 */
enum class LogicTone { TIGHT, LOOSE, FREE }

data class ToneRoll(
    val punctuation: PunctuationTone,
    val length: LengthTone,
    val logic: LogicTone,
)

object StyleRoller {

    /**
     * 人设型标点偏好：同一个角色**永远**是同一个底子（换一轮不变），否则每轮忽工整忽随意，反而假。
     * 纯派生自 uuid（String.hashCode 跨 JVM 稳定），不落库、不进备份。
     */
    fun personaPunctuation(characterUuid: String): PunctuationTone =
        if ((characterUuid.hashCode().ushr(1) % 100) < 55) PunctuationTone.CASUAL else PunctuationTone.NORMAL

    /**
     * 掷本轮口气。[intensity] = "light"（默认）时不掷「不用讲逻辑」那一档——太放的写法容易让人觉得在敷衍；
     * "standard" 才放全档。种子相同 + uuid 相同 = 结果相同（可测）。
     */
    fun roll(characterUuid: String, seed: Long, intensity: String = "light"): ToneRoll {
        val r = Random(seed xor (characterUuid.hashCode().toLong() shl 32))
        val casualBase = if (personaPunctuation(characterUuid) == PunctuationTone.CASUAL) 85 else 55
        val punctuation = if (r.nextInt(100) < casualBase) PunctuationTone.CASUAL else PunctuationTone.NORMAL

        val lp = r.nextInt(100)
        val length = when {
            lp < 55 -> LengthTone.ONE_LINE
            lp < 85 -> LengthTone.TWO_THREE
            lp < 97 -> LengthTone.FOUR_FIVE
            else -> LengthTone.OPEN
        }

        val gp = r.nextInt(100)
        val logic = when {
            gp < 60 -> LogicTone.TIGHT
            gp < 90 -> LogicTone.LOOSE
            gp < 95 -> if (intensity == "standard") LogicTone.FREE else LogicTone.LOOSE
            else -> if (intensity == "standard") LogicTone.FREE else LogicTone.LOOSE
        }
        return ToneRoll(punctuation = punctuation, length = length, logic = logic)
    }
}
