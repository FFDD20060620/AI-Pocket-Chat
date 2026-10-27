package com.situ.aichat.prompt

import com.situ.aichat.data.model.CustomGain
import com.situ.aichat.data.model.PersonaGains
import com.situ.aichat.data.model.PersonaVocab
import com.situ.aichat.diagnostics.ContextLogService
import com.situ.aichat.prompt.growth.buildSensitivitySection
import com.situ.aichat.prompt.persona.PersonaCompileInput
import com.situ.aichat.prompt.persona.PersonaCompileService
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 体检后续·人称按上下文理顺（微图纸 docs/handoff/2026-09-29-体检小毛病修复.md §7.2）。
 * 四处上下文的「你」：编辑页 = 用户（「她」= 角色）· 聊天提示词 = 角色（用户叫名字）· 编译 / 分析提示词 = 模型自己（角色 = TA）。
 * 期望值从 §7.2 换算规则独立反推。
 */
class PersonaGainsPronounTest {

    private val user = "小棠"

    // MARK: - 聊天 · 专属标签入句换算

    @Test
    fun characterPronoun_becomesYou() {
        // 体检实录：男角色陆屿被编出「认真试玩她的游戏」（她 = 角色本人），老数据不用重新生成也读对。
        assertEquals("认真试玩你的游戏", customGainInlineLabel("认真试玩她的游戏", user))
        assertEquals("别人夸你的画", customGainInlineLabel("别人夸他的画", user))
        assertEquals("你的作品被认真对待", customGainInlineLabel("TA 的作品被认真对待", user))
    }

    @Test
    fun userReferences_becomeUserName() {
        assertEquals("小棠和别人走太近", customGainInlineLabel("对方和别人走太近", user))
        // 编辑页口径手写：「你」= 用户、「她」= 角色 → 两头对调回来。
        assertEquals("小棠忘了你的生日", customGainInlineLabel("你忘了她的生日", user))
    }

    @Test
    fun guards_untouched() {
        for (label in listOf("被叫全名", "自己的作品被认真对待", "被拿来和其他人比较", "被他人议论", "听人弹吉他", "你们吵架", "她们聚会不叫她们")) {
            assertEquals(label, customGainInlineLabel(label, user))
        }
    }

    @Test
    fun gainsLine_rendersConvertedCustomLabel() {
        val lines = buildPersonaGainsLines(
            PersonaGains(custom = listOf(CustomGain(id = "u1", label = "认真试玩她的游戏", level = 2), CustomGain(id = "u2", label = "对方和别人走太近", level = 2))),
            user,
        )
        assertEquals(listOf("- 你特别在意认真试玩你的游戏、小棠和别人走太近。"), lines)
    }

    // MARK: - 编译 / 分析 · 27 项清单与分析段

    @Test
    fun g09_g21_neutralInPromptLines_uiLabelsUnchanged() {
        assertEquals("g09 对方记得 TA 说过的小事", PersonaVocab.gainPromptLine("g09"))
        assertEquals("g21 对方向 TA 坦白脆弱", PersonaVocab.gainPromptLine("g21"))
        // 编辑页用的原标签（与 zh 资源同源的那张表）一字不动。
        assertEquals("你记得她说过的小事", PersonaVocab.GAIN_PROMPT_LABELS["g09"])
        assertEquals("你对她坦白脆弱", PersonaVocab.GAIN_PROMPT_LABELS["g21"])
        assertEquals("g13 吵架 · 被凶", PersonaVocab.gainPromptLine("g13"))
    }

    @Test
    fun compileAndAnalysisPrompts_haveNoUiPerspectivePronouns() {
        val (compile, _) = PersonaCompileService(mockk<ContextLogService>(relaxed = true))
            .buildCompilePrompt(PersonaCompileInput(name = "陆屿", personalityDescription = "直球，话多"))
        assertTrue(compile.contains("g09 对方记得 TA 说过的小事"))
        assertTrue(compile.contains("g21 对方向 TA 坦白脆弱"))
        assertFalse(compile.contains("你记得她说过的小事"))
        assertFalse(compile.contains("你对她坦白脆弱"))
        val section = buildSensitivitySection(listOf("自己做的游戏被认真试玩"))
        assertFalse("分析提示词敏感点段不再有「她」", section.contains("她"))
        assertTrue(section.contains("- 下面是 TA 可能在意的事。"))
        // 专属标签在分析提示词里仍原文照抄（custom_hits 要按原标签对回去），不做聊天那边的换算。
        assertTrue(section.contains("TA 的专属敏感点（原文照抄标签）：自己做的游戏被认真试玩"))
    }
}
