package com.situ.aichat.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 聊天体检小毛病修复（微图纸 docs/handoff/2026-09-29-体检小毛病修复.md §5）·T1。
 * 用例原文 = 体检里真模型吐出来的原句（RESULTS-2026-09-29-checkup.md 第 8 条），期望值从微图纸 §4 锁定规则独立反推。
 * [pipeline] 照 `ChatReplyDeliverer` 的顺序：先 parseMood 再 sanitize（非线下）。
 */
class ReplyParserCheckupFixTest {

    private fun pipeline(raw: String, offline: Boolean = false): String =
        ReplyParser.sanitizeAssistantResponse(
            ReplyParser.parseMood(raw, preserveOfflineTags = offline).cleanText,
            preserveOfflineTags = offline,
            characterName = "陆屿",
        )

    // MARK: - F1 情绪标签写歪

    @Test
    fun f1_bareMoodLine_isParsedAndStripped() {
        val r = ReplyParser.parseMood("那你做完给我报个平安,就说一声\"我出来了\"\n\n不然我那天一上午都得惦记着\n\nmood:🥺|yellow|有点担心")
        assertEquals("有点担心", r.text)
        assertEquals("yellow", r.colorName)
        assertEquals("🥺", r.emoji)
        assertFalse("整行情绪标签不能留在正文里", r.cleanText.contains("mood"))
        assertTrue(r.cleanText.endsWith("不然我那天一上午都得惦记着"))
    }

    @Test
    fun f1_spacedBracketTag_isParsedAndStripped() {
        val r = ReplyParser.parseMood("行，那说好了\n\n[ mood:😊 | green | 安心 ]")
        assertEquals("安心", r.text)
        assertEquals("green", r.colorName)
        assertEquals("😊", r.emoji)
        assertEquals("行，那说好了", r.cleanText)
    }

    @Test
    fun f1_fullWidthColon_isParsed() {
        val r = ReplyParser.parseMood("[mood：😢|red|难过]嗯")
        assertEquals("难过", r.text)
        assertEquals("red", r.colorName)
        assertEquals("嗯", r.cleanText)
    }

    @Test
    fun f1_bracketThenBareLine_lastByPositionWins() {
        val r = ReplyParser.parseMood("[mood:😀|green|高兴]先说这个\n后来又想了想\nmood:😢|red|难过")
        assertEquals("难过", r.text)
        assertEquals("red", r.colorName)
    }

    @Test
    fun f1_strayMoodWord_isStripped() {
        assertEquals("代码都跑一半了", pipeline("代码都跑一半了\n\n（mood）"))
        assertEquals("好", pipeline("好(MOOD)"))
    }

    @Test
    fun f1_guard_moodWordInsideSentence_untouched() {
        val raw = "我今天 mood: 一般，你呢"
        val r = ReplyParser.parseMood(raw)
        assertEquals("", r.text)
        assertEquals(raw, r.cleanText)
    }

    @Test
    fun f1_guard_bareLineWithUnknownColor_notRecognized() {
        // 整行无括号版只认 green / yellow / red（微图纸 §4）：别的颜色词不当情绪标签。
        val r = ReplyParser.parseMood("好\nmood:😶|purple|平静")
        assertEquals("", r.text)
        assertTrue(r.cleanText.contains("purple"))
    }

    @Test
    fun f1_guard_standardTagBehaviourUnchanged() {
        val r = ReplyParser.parseMood("[mood:😊|green|开心] 你好呀")
        assertEquals("你好呀", r.cleanText)
        assertEquals("开心", r.text)
        assertEquals("😊", r.emoji)
    }

    // MARK: - F2 编造的系统标记

    @Test
    fun f2_fakeVoiceBar_removed() {
        val out = pipeline("下周三？这么快\n\n[时长 6 秒]\n\n说真的面之前那天别熬夜")
        assertFalse(out.contains("时长"))
        assertEquals("下周三？这么快\n说真的面之前那天别熬夜", out)
        assertEquals("好", pipeline("好[语音 12\"]"))
        assertEquals("好", pipeline("好[时长6s]"))
    }

    @Test
    fun f2_fakeReceipt_removed() {
        assertEquals("去吧去吧,睡个好觉~", pipeline("去吧去吧,睡个好觉~\n\n[结果:已回复]"))
    }

    @Test
    fun f2_numberOnlyBracket_removed() {
        assertEquals("我这边再看两章就停。\n嗯,说好的都早点。", pipeline("我这边再看两章就停。\n\n[0.5]嗯,说好的都早点。"))
    }

    @Test
    fun f2_guard_calendarRefAndStickerTag_untouched() {
        assertEquals("[#E1] 周五开会别忘了", pipeline("[#E1] 周五开会别忘了"))
        assertEquals("[sticker:abc123]", pipeline("[sticker:abc123]"))
    }

    // MARK: - F3 整行括号旁白

    @Test
    fun f3_wholeLineSquareNarration_unwrapped() {
        assertEquals(
            "其实我也是。\n现在整屏代码我一个字都看不进去了",
            pipeline("其实我也是。\n\n[现在整屏代码我一个字都看不进去了]"),
        )
    }

    @Test
    fun f3_wholeLineParenNarration_unwrapped() {
        assertEquals("先吃面，不急着答", pipeline("（先吃面，不急着答）"))
        assertEquals(
            "明天中午见，热乎的饭带来，顺便当我的拆箱苦力~",
            pipeline("（明天中午见，热乎的饭带来，顺便当我的拆箱苦力~）"),
        )
        assertEquals("行\n先吃面，不急着答", pipeline("行\n(先吃面，不急着答)"))
    }

    @Test
    fun f3_guard_shortBracketsAndEmojiCodes_untouched() {
        assertEquals("[捂脸]", pipeline("[捂脸]"))
        assertEquals("[破涕为笑]", pipeline("[破涕为笑]"))
        assertEquals("（小声）", pipeline("（小声）"))
        assertEquals("（我没生气）", pipeline("（我没生气）"))
    }

    @Test
    fun f3_guard_inlineParenthesis_untouched() {
        assertEquals("好啊（其实我早就想去了）走吧", pipeline("好啊（其实我早就想去了）走吧"))
    }

    @Test
    fun f3_guard_appTagsAndSystemRecord_untouched() {
        assertEquals("[sticker:开心的猫猫表情包]", ReplyParser.unwrapWholeLineNarration("[sticker:开心的猫猫表情包]"))
        assertEquals(
            "[系统记录：夏晴子向阿远发出了线下见面邀约|状态=还没回应]",
            ReplyParser.unwrapWholeLineNarration("[系统记录：夏晴子向阿远发出了线下见面邀约|状态=还没回应]"),
        )
        assertEquals("[他说[你好呀]了然后就走了]", ReplyParser.unwrapWholeLineNarration("[他说[你好呀]了然后就走了]"))
    }

    @Test
    fun f3_guard_offlineModeKeepsParentheticals() {
        val raw = "[叙述]雨下大了\n（她把伞往你那边斜了斜，自己半边肩膀都湿了）"
        val out = pipeline(raw, offline = true)
        assertTrue("线下模式整行括号原样保留", out.contains("（她把伞往你那边斜了斜，自己半边肩膀都湿了）"))
    }

    @Test
    fun f3_guard_parenWithNarrationSignal_stillDroppedNotUnwrapped() {
        // 既有 isMetaLine 规则（整行括号 + 旁白关键词 → 整行丢弃）先于 F3，顺序不变。
        assertEquals("嗯", pipeline("嗯\n（发来一条语音，背景音里有雨声）"))
    }
}
