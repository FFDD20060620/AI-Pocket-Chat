package com.situ.aichat.ui.offline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.situ.aichat.data.local.entity.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 见面剧场给浮件 chrome 让位（微图纸 2026-09-29-琉璃见面剧场让位浮件·Robolectric + Compose UI 测试）——
 * 驱动**生产 [OfflineModeView]**：琉璃脸的顶栏 / 输入区是 overlay、不占剧场布局高度，修复前剧场只留自带 20dp
 * 底 → 末条内容压在输入区下（真机截图）。这里不猜内容块的内边距，用**差分**钉住：同一帧内容、只改占位，
 * 末条 / 顶部装饰必须恰好挪动占位那么多（反转锚定 = 底边钉住，所以底占位长高时末条整体上移）。
 * 另钉暖陶不变：不传占位时末条底边在视口底 − 20dp 之上（原 contentPadding 口径）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineModeViewChromeInsetsTest {

    @get:Rule
    val compose = createComposeRule()

    private val viewport = 400.dp

    private fun msg(i: Int, ts: Long) = MessageEntity(
        messageUUID = "off-$i",
        conversationUuid = "conv",
        roleRaw = if (i % 2 == 0) "user" else "assistant",
        content = "见面消息第${i}句，足够长的一段话保证内容块有真实渲染高度。",
        timestamp = ts,
        isOfflineMode = true,
        offlineSessionId = "s1",
    )

    /** 时间戳全在进屏之前 → 揭示走「已播放直显」，无动画时钟依赖（同 [OfflineModeViewReentryScrollTest]）。 */
    private fun messages(count: Int): List<MessageEntity> {
        val past = System.currentTimeMillis() - 60_000L
        return List(count) { msg(it, past + it * 1000L) }
    }

    private var insets by mutableStateOf(PaddingValues(0.dp))

    private fun setTheater(messages: List<MessageEntity>) {
        compose.setContent {
            OfflineModeView(
                offlineMessages = messages,
                isWaitingForContent = false,
                characterName = "角色",
                characterAvatarPath = null,
                userName = "我",
                userAvatarPath = null,
                themeColorHex = null,
                chatWallpaperPath = null,
                entryAnimationsEnabled = true,
                playingVoiceId = null,
                voiceProgress = { 0f },
                onVoiceToggle = {},
                onEndMeeting = {},
                onContinueMeeting = {},
                modifier = Modifier.fillMaxWidth().height(viewport),
                chromeInsets = insets,
            )
        }
        compose.waitForIdle()
    }

    private fun bottomOf(text: String): Dp =
        compose.onNodeWithText(text, substring = true).getUnclippedBoundsInRoot().bottom

    private fun topOf(text: String): Dp =
        compose.onNodeWithText(text, substring = true).getUnclippedBoundsInRoot().top

    private fun assertDpEquals(expected: Dp, actual: Dp) =
        assertEquals(expected.value, actual.value, 0.5f)

    @Test
    fun bottomInset_liftsLatestMessageAboveOverlayInput() {
        setTheater(messages(30))
        val latest = "见面消息第29句"
        val bottomWithout = bottomOf(latest)
        // 暖陶口径（不传占位）：末条落在视口底 − 20dp 之上，且贴底（不是停在半空）。
        assertTrue("末条底 $bottomWithout 越过了视口底 − 20dp", bottomWithout <= viewport - 20.dp + 0.5.dp)
        assertTrue("末条底 $bottomWithout 没贴底", bottomWithout > viewport - 80.dp)

        // 琉璃：输入区 + 导航栏 80dp 浮在剧场底部 → 末条必须整体上移 80dp，落在浮件顶 − 20dp 之上。
        insets = PaddingValues(top = 90.dp, bottom = 80.dp)
        compose.waitForIdle()
        val bottomWith = bottomOf(latest)
        assertDpEquals(bottomWithout - 80.dp, bottomWith)
        assertTrue("末条底 $bottomWith 压进了浮件区", bottomWith <= viewport - 80.dp - 20.dp + 0.5.dp)

        // 输入区长高（引用条 / 多行）→ 占位跟着涨，末条同量再上移。
        insets = PaddingValues(top = 90.dp, bottom = 140.dp)
        compose.waitForIdle()
        assertDpEquals(bottomWithout - 140.dp, bottomOf(latest))
    }

    @Test
    fun topInset_keepsShortTheaterBelowTopBar() {
        // 内容不满一屏（锁 Top 贴顶）：「线下见面」装饰与首条要让出顶栏，而不是钻进它下面。
        setTheater(messages(1))
        val headerTopWithout = topOf("线下见面")
        val firstTopWithout = topOf("见面消息第0句")

        insets = PaddingValues(top = 90.dp, bottom = 80.dp)
        compose.waitForIdle()
        assertDpEquals(headerTopWithout + 90.dp, topOf("线下见面"))
        assertDpEquals(firstTopWithout + 90.dp, topOf("见面消息第0句"))
        assertTrue(topOf("线下见面") >= 90.dp)
    }
}
