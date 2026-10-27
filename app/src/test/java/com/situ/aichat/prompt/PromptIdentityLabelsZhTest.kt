package com.situ.aichat.prompt

import com.situ.aichat.data.local.entity.CharacterEntity
import com.situ.aichat.data.local.entity.MessageEntity
import com.situ.aichat.data.model.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 聊天体检小毛病修复 F4（微图纸 docs/handoff/2026-09-29-体检小毛病修复.md §4）：中文界面下，角色资料段不再中英混杂
 * （体检实录 `Gender: 女.` `Age: 29 years old.` `Backstory:`），模板也不再给用户原文补句号（实录「…软。。」「…粽子。.」）。
 * 走真 [PromptBuilder.buildMessages] 装配，期望标签从微图纸锁定项重新打字。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN")
class PromptIdentityLabelsZhTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now: Instant = LocalDate.of(2026, 10, 14).atTime(21, 0).atZone(zone).toInstant()

    private val character = CharacterEntity(
        uuid = "c1", name = "沈砚秋", creationDate = 0L, gender = "女",
        birthday = LocalDate.of(1996, 11, 2).atStartOfDay(zone).toInstant().toEpochMilli(),
        occupation = "古籍修复师",
        appearanceDescription = "齐耳短发,常穿灰色和藏青,戴细框眼镜。",
        personalityDescription = "外冷内热,慢热,话少。嘴上说不出软话。",
        backstory = "在市博物馆的古籍修复室工作。",
        speakingStyle = "句子短,常用句号结尾。",
        catchphrases = "「嗯。」「随你。」",
        initialInterests = "旧书、修复、龟背竹",
    )

    private fun identityBlock(): String {
        val system = PromptBuilder.buildMessages(
            character = character,
            sortedMessages = listOf(
                MessageEntity(messageUUID = "u1", conversationUuid = "conv1", roleRaw = "user", content = "在干嘛", timestamp = now.toEpochMilli() - 60_000),
            ),
            userProfile = null, appSettings = AppSettings(), strings = PromptStrings(RuntimeEnvironment.getApplication()), now = now,
        ).first { it.role == "system" }.content.orEmpty()
        val start = system.indexOf("你是「沈砚秋」。")
        assertTrue("身份段在系统提示词里", start >= 0)
        val end = system.indexOf("兴趣爱好：", start)
        assertTrue("兴趣行在身份段末尾", end > start)
        return system.substring(start, system.indexOf('\n', end).let { if (it < 0) system.length else it })
    }

    @Test
    fun identityLines_areChinese() {
        val block = identityBlock()
        for (line in listOf(
            "性别：女",
            "年龄：29 岁",
            "星座：天蝎座",
            "职业 / 身份：古籍修复师",
            "外貌：齐耳短发,常穿灰色和藏青,戴细框眼镜。",
            "你的性格特点：外冷内热,慢热,话少。嘴上说不出软话。",
            "背景故事：在市博物馆的古籍修复室工作。",
            "说话风格：句子短,常用句号结尾。",
            "口头禅：「嗯。」「随你。」",
            "兴趣爱好：旧书、修复、龟背竹",
        )) {
            assertTrue("缺少行：$line\n实际：\n$block", block.contains(line))
        }
        for (english in listOf("Gender", "Age:", "years old", "Zodiac", "Occupation", "Appearance", "Backstory", "Speaking style", "Catchphrases", "Interests")) {
            assertFalse("中文界面不应再有英文标签 $english", block.contains(english))
        }
    }

    @Test
    fun identityLines_noDoubledPunctuation() {
        val block = identityBlock()
        assertFalse("不应出现「。。」", block.contains("。。"))
        assertFalse("不应出现「。.」", block.contains("。."))
    }
}
