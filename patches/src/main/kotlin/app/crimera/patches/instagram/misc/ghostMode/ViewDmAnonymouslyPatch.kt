/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.ghostMode

import app.crimera.patches.instagram.misc.actionBar.chatActionBarButton.chatActionBarButtonPatch
import app.crimera.patches.instagram.misc.actionBar.inboxActionBarButton.inboxActionBarButtonPatch
import app.crimera.patches.instagram.misc.settings.settingsPatch
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.Constants.PREF_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.USER_SESSION_CLASS
import app.crimera.patches.instagram.utils.enableSettings
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.getFreeRegisterProvider

@Suppress("unused")
val viewDmAnonymouslyPatch =
    patch(
        name = "View DMs anonymously",
        default = true,
    ) {
        dependsOn(settingsPatch, chatActionBarButtonPatch, inboxActionBarButtonPatch)
        compatibleWith(COMPATIBILITY_INSTAGRAM)

        execute {
            val seen = DMSeenFingerprint.method
            if (seen.parameterTypes.size != 5 || seen.parameterTypes[0] != USER_SESSION_CLASS ||
                !seen.parameterTypes[1].startsWith("L") ||
                seen.parameterTypes.drop(2).any { it != "Ljava/lang/String;" }
            ) {
                throw PatchException("Unexpected DM seen request parameters")
            }

            ManualChatSeenSendReceiptFingerprint.method.addInstructions(
                0,
                """
                check-cast p1, ${seen.parameterTypes[1]}
                invoke-static/range {p0 .. p4}, $seen
                """.trimIndent(),
            )

            seen.apply {
                val shouldDisableRegister =
                    getFreeRegisterProvider(
                        index = 1,
                        numberOfFreeRegistersNeeded = 1,
                    ).getFreeRegister()

                addInstructionsWithLabels(
                    1,
                    """
                    invoke-static {}, $PREF_DESCRIPTOR->viewDmAnonymously()Z
                    move-result v$shouldDisableRegister
                    if-eqz v$shouldDisableRegister, :piko_continue
                    return-void
                    """.trimIndent(),
                    ExternalLabel("piko_continue", getInstruction(1)),
                )

                addInstructions(
                    0,
                    "invoke-static/range {p0 .. p4}, $MANUAL_CHAT_SEEN_EXTENSION->onSeenRequest(${USER_SESSION_CLASS}Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                )
            }
            enableSettings("viewDmAnonymously")
        }
    }
