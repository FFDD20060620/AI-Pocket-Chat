package com.situ.aichat.prompt.style

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 三旋钮骰子（活人感二期 B 件·2026-11-03，方案 §5.9）。
 *
 * 为什么值得钉死：骰子一旦不确定，同一条 seed 两次跑出不同结果 → 提示词不可复现、排查没法对账；
 * 而「light 档绝不掷 FREE」是产品决策（太放的写法会被当成敷衍），不是实现细节。
 */
class StyleRollerTest {

    private val uuid = "8F14E45F-EA8B-4C9C-9D3A-2B7C1E5A9D42"

    @Test
    fun `同一 seed 与角色_结果完全一致`() {
        val a = StyleRoller.roll(uuid, seed = 42L, intensity = "standard")
        val b = StyleRoller.roll(uuid, seed = 42L, intensity = "standard")
        assertEquals(a, b)
    }

    @Test
    fun `不同 seed 会掷出不同结果`() {
        val results = (0L until 50L).map { StyleRoller.roll(uuid, it) }.toSet()
        assertTrue("50 个种子至少出现 3 种组合，实际 ${results.size}", results.size >= 3)
    }

    @Test
    fun `light 档永远不出现 FREE`() {
        repeat(500) { seed ->
            val roll = StyleRoller.roll(uuid, seed.toLong(), intensity = "light")
            assertNotEquals("seed=$seed", LogicTone.FREE, roll.logic)
        }
    }

    @Test
    fun `standard 档能掷出 FREE`() {
        val hasFree = (0L until 800L).any {
            StyleRoller.roll(uuid, it, intensity = "standard").logic == LogicTone.FREE
        }
        assertTrue("standard 档 800 次都没掷出 FREE，说明概率写死成了 0", hasFree)
    }

    @Test
    fun `人设标点偏好按角色稳定`() {
        repeat(10) { assertEquals(StyleRoller.personaPunctuation(uuid), StyleRoller.personaPunctuation(uuid)) }
    }

    @Test
    fun `标点总体偏随意_落在合理区间`() {
        val casual = (0L until 1000L).count {
            StyleRoller.roll(uuid, it).punctuation == PunctuationTone.CASUAL
        }
        assertTrue("1000 次里随意档 $casual 次，超出 55%~95% 的合理区间", casual in 550..950)
    }

    @Test
    fun `长短至少三档都会出现`() {
        // OPEN 只占 3%，1000 个固定种子里未必命中；这里只要求分布真的分开了（不是写死成一个值）。
        val seen = (0L until 1000L).map { StyleRoller.roll(uuid, it).length }.toSet()
        assertTrue("只出现 $seen", seen.size >= 3)
        assertTrue(LengthTone.ONE_LINE in seen)
    }
}
