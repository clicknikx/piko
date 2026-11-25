/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.ghostMode

import app.crimera.patches.instagram.utils.Constants.PATCHES_DESCRIPTOR
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

internal const val MANUAL_CHAT_SEEN_EXTENSION = "$PATCHES_DESCRIPTOR/dm/ManualChatSeen;"

// This fingerprint is also used in MarkAsRead patch.
object DMSeenFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    filters =
        listOf(
            string("mark_thread_seen-"),
        ),
)

internal object ManualChatSeenSendReceiptFingerprint : Fingerprint(
    definingClass = MANUAL_CHAT_SEEN_EXTENSION,
    name = "sendReceipt",
)
