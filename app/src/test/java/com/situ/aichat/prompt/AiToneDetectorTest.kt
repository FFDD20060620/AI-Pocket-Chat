package com.situ.aichat.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C 件（方案 §6）的纯函数测试：只断言「删得准」，不断言「删得多」——
 * 漏删只是少赚，误删会把正常回复剪碎（B13）。
 */
class AiToneDetectorTest {

    @Test
    fun cannedCustomerServiceLine_isDropped() {
        val out = AiToneDetector.strip("今天下雨了，记得带伞。\n有什么可以帮您的吗")
        assertEquals("今天下雨了，记得带伞。", out)
    }

    @Test
    fun elevationTail_isCut() {
        val out = AiToneDetector.strip("药吃了没，别忘了，希望你今天好好的")
        assertEquals("药吃了没，别忘了", out)
    }

    @Test
    fun normalMessage_isUntouched() {
        val text = "刚下班，累死了\n你吃了吗"
        assertEquals(text, AiToneDetector.strip(text))
    }

    @Test
    fun everythingCanned_returnsEmpty() {
        val out = AiToneDetector.strip("希望对你有所帮助")
        assertTrue(out.isEmpty())
    }

    @Test
    fun longLineWithTemplate_inside_isKept() {
        val text = "我今天在公司被领导说了，当时就想找个地方躲起来，后来想想也没什么大不了的，反正他也不是第一次这样"
        assertEquals(text, AiToneDetector.strip(text))
    }

    @Test
    fun waitingTailLine_isDropped() {
        val out = AiToneDetector.strip("我先去洗个澡\n在等你回复")
        assertEquals("我先去洗个澡", out)
    }

    @Test
    fun inlineWaitingTail_isCut() {
        val out = AiToneDetector.strip("我先去忙了，在等你回复")
        assertEquals("我先去忙了", out)
    }

    @Test
    fun reportToneLine_isDropped() {
        val out = AiToneDetector.strip("值得注意的是，这件事并不简单")
        assertTrue(out.isEmpty())
    }

    @Test
    fun detect_reportsRule() {
        val hits = AiToneDetector.detect("作为一个人工智能，我无法访问你的相册")
        assertTrue(hits.any { it.rule == AiToneDetector.Rule.META_AI })
    }
}
