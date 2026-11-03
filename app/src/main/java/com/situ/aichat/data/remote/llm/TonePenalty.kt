package com.situ.aichat.data.remote.llm

import com.situ.aichat.data.model.AppSettings

/**
 * 普通聊天的采样惩罚取值（活人感二期·A 件，2026-11-03，方案见 2/记录/06-去AI八股方案.md §4）。
 *
 * 只对「普通聊天」生效：工具调用回合、JSON 模式、推理模型一律不发——penalty 会按出现次数线性压低词权重，
 * `{` `}` `"` 这类高频符号最容易被压掉，工具参数会直接缺括号（方案 §16.2 / 风险 B35）。
 * 两个档位都刻意保守：中文里「的 / 了 / 我」出现频率极高，压过头会让句子变碎，反而更像机器。
 */
internal data class TonePenalty(
    val presence: Double,
    val frequency: Double,
) {
    companion object {
        /** 温和档（默认）：推荐先跑这一档，观察一两周再考虑上调。 */
        val LIGHT = TonePenalty(presence = 0.15, frequency = 0.20)

        /** 标准档：方案里的推荐值（frequency 更重，专治复读同一句式）。 */
        val STANDARD = TonePenalty(presence = 0.20, frequency = 0.30)
    }
}

/** 设置 → 惩罚取值：开关关着就是 null（请求里两个字都不出现，与旧行为逐字节一致）。 */
internal fun AppSettings.tonePenalty(): TonePenalty? = when {
    !antiSlopPenaltyEnabled -> null
    antiSlopPenaltyIntensity == "standard" -> TonePenalty.STANDARD
    else -> TonePenalty.LIGHT
}
