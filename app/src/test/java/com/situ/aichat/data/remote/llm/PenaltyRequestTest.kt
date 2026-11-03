package com.situ.aichat.data.remote.llm

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 采样惩罚（活人感二期 A 件·2026-11-03，方案 §4/§16.2）。
 *
 * 两件事必须钉死：①线格式——null 时**一个 key 都不能出现**（否则所有老服务商都会收到陌生字段）；
 * ②400 分类——只有真的发过 penalty 才值得为它重试，且优先级排在「去温度」之后。
 */
class PenaltyRequestTest {

    /** 与 NetworkModule.provideJson 同款：null 省略、默认值不发。 */
    private val json = Json { explicitNulls = false; encodeDefaults = false; ignoreUnknownKeys = true }

    private val penaltyRejection =
        """{"error":{"message":"presence_penalty is not supported for this model","type":"invalid_request_error","param":"presence_penalty"}}"""

    private fun plan(body: String?, sentPenalty: Boolean) =
        LlmClient.firstCall400RetryPlan(LlmError.Http(400, body), requestedMaxTokens = null, sentTemperature = null, sentPenalty = sentPenalty)

    @Test
    fun `填了惩罚_线格式带 snake_case 两个 key`() {
        val dto = ChatRequestDto(
            model = "deepseek-v4",
            messages = emptyList(),
            stream = true,
            presencePenalty = 0.15,
            frequencyPenalty = 0.2,
        )
        val body = json.encodeToString(ChatRequestDto.serializer(), dto)
        assertTrue(body, body.contains("\"presence_penalty\":0.15"))
        assertTrue(body, body.contains("\"frequency_penalty\":0.2"))
    }

    @Test
    fun `惩罚为空_不发 null 字段`() {
        val dto = ChatRequestDto(model = "deepseek-v4", messages = emptyList(), stream = true)
        val body = json.encodeToString(ChatRequestDto.serializer(), dto)
        assertFalse(body, body.contains("penalty"))
    }

    @Test
    fun `方言点名 penalty 且确实发过_去惩罚重试`() {
        assertEquals(FirstCall400RetryPlan.DROP_PENALTIES, plan(penaltyRejection, sentPenalty = true))
    }

    @Test
    fun `点名 penalty 但从没发过_不重试`() {
        assertEquals(FirstCall400RetryPlan.NONE, plan(penaltyRejection, sentPenalty = false))
    }

    @Test
    fun `温度与惩罚同时点名_先去温度`() {
        val both = """{"error":{"message":"temperature and presence_penalty are not supported"}}"""
        assertEquals(
            FirstCall400RetryPlan.DROP_TEMPERATURE,
            LlmClient.firstCall400RetryPlan(LlmError.Http(400, both), requestedMaxTokens = null, sentTemperature = 0.8, sentPenalty = true),
        )
    }
}
