package com.situ.aichat.prompt

/**
 * 诊断开关（**临时**，排查完删除）。
 *
 * [CLEAN_BASELINE] = true 时整版关闭本次新增的去 AI 味能力：两个新模块不装配、
 * 三旋钮不掷骰子、学到的说法不注入、运行时不做删句、也不写语风库。
 * 用来出「对照包」判断卡顿是否由这批改动引起；其余代码一行不动。
 */
internal object DiagnosticSwitches {

    const val CLEAN_BASELINE: Boolean = true

    /** 本次新增的两个系统模块，对照包里不参与装配。 */
    fun hides(type: SystemModuleType): Boolean =
        CLEAN_BASELINE && (type == SystemModuleType.VOICE_RULES || type == SystemModuleType.ANTI_SLOP)
}
