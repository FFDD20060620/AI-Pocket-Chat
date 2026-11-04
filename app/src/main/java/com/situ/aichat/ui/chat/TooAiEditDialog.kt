package com.situ.aichat.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.situ.aichat.R

/**
 * 「这句太 AI 了」的改写弹窗（方案 §7.2）。
 *
 * 预填 AI 原句，用户直接改成自己想听的说法；保存后替换那条消息、同时记一对「原句 → 改写」。
 * 取消 = 什么都不写（不改正文、不记负例）。
 *
 * 三个约束都写在这里，别下放到调用方：300 字上限、空内容禁用保存、保存前 trim。
 */
@Composable
internal fun TooAiEditDialog(
    original: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(original) { mutableStateOf(original) }
    val trimmed = text.trim()
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.tone_edit_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.tone_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_EDIT_LENGTH) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text(stringResource(R.string.tone_edit_placeholder)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed) },
                enabled = trimmed.isNotEmpty(),
            ) { Text(stringResource(R.string.tone_edit_save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.tone_edit_cancel)) }
        },
    )
}

/** 改写长度上限：超过这个长度的多半不是「一句话」，而是让 AI 重写一条（那是 regenerate 的事）。 */
private const val MAX_EDIT_LENGTH = 300
