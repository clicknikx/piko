/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.instagram.patches.dm;

import static app.morphe.extension.instagram.utils.IgStr.str;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import app.morphe.extension.instagram.constants.UI;
import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.MarkChatAsReadScope;
import app.morphe.extension.shared.Utils;

import com.instagram.common.session.UserSession;

/**
 * Captures the blocked DM seen receipt for the currently open chat and sends it on demand
 * when the user long-presses the Eye button in the chat action bar.
 */
@SuppressWarnings("unused")
public final class ManualChatSeen {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();

    private static MarkChatAsReadScope.Operation pendingReceipt;
    private static long lastCapturedTimeMs;
    private static long lastChatDetachedAtMs;
    private static boolean alreadyMarkedCurrentChat;

    private static WeakReference<ViewGroup> activeChatHeader = new WeakReference<>(null);
    private static WeakReference<ImageView> activeChatEye = new WeakReference<>(null);

    private ManualChatSeen() {
    }

    public static void onSeenRequest(UserSession session, Object context,
                                     String threadId, String messageId, String senderId) {
        if (!Pref.viewDmAnonymously() || MarkChatAsReadScope.isActive()) {
            return;
        }
        if (session == null || threadId == null || messageId == null || senderId == null) {
            return;
        }

        synchronized (LOCK) {
            pendingReceipt = () -> sendReceipt(session, context, threadId, messageId, senderId);
            lastCapturedTimeMs = SystemClock.elapsedRealtime();
            alreadyMarkedCurrentChat = false;
        }
        updateActiveEyeIcon(false);
    }

    public static void onInboxOpened() {
        synchronized (LOCK) {
            pendingReceipt = null;
            alreadyMarkedCurrentChat = false;
        }
        activeChatEye = new WeakReference<>(null);
    }

    public static void bindChatHeader(ViewGroup viewGroup, ImageView eyeIcon) {
        if (viewGroup == null || eyeIcon == null) {
            return;
        }

        boolean hasPending;
        boolean alreadyMarked;
        synchronized (LOCK) {
            if (lastChatDetachedAtMs > 0L && lastCapturedTimeMs < lastChatDetachedAtMs) {
                pendingReceipt = null;
                alreadyMarkedCurrentChat = false;
            }
            activeChatHeader = new WeakReference<>(viewGroup);
            activeChatEye = new WeakReference<>(eyeIcon);
            hasPending = pendingReceipt != null;
            alreadyMarked = alreadyMarkedCurrentChat;
        }

        viewGroup.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                synchronized (LOCK) {
                    if (activeChatHeader.get() == viewGroup) {
                        lastChatDetachedAtMs = SystemClock.elapsedRealtime();
                    }
                }
            }
        });

        applyEyeVisual(eyeIcon, !hasPending && alreadyMarked);
    }

    public static boolean markCurrentChatAsSeen() {
        MarkChatAsReadScope.Operation receipt;
        boolean wasAlreadyMarked;
        synchronized (LOCK) {
            receipt = pendingReceipt;
            pendingReceipt = null;
            wasAlreadyMarked = alreadyMarkedCurrentChat;
        }

        if (receipt == null) {
            Utils.showToastShort(str(
                    wasAlreadyMarked ? "piko_dm_already_marked_as_read" : "piko_dm_nothing_to_mark_read"
            ));
            return true;
        }

        try {
            MarkChatAsReadScope.run(receipt);
            synchronized (LOCK) {
                alreadyMarkedCurrentChat = true;
            }
            updateActiveEyeIcon(true);
            Utils.showToastShort(str("piko_dm_marked_as_read"));
        } catch (Exception exception) {
            synchronized (LOCK) {
                if (pendingReceipt == null) {
                    pendingReceipt = receipt;
                }
            }
            Logger.printException(() -> "ManualChatSeen.markCurrentChatAsSeen failed", exception);
        }
        return true;
    }

    private static void updateActiveEyeIcon(boolean marked) {
        ImageView eye = activeChatEye.get();
        if (eye == null) {
            return;
        }
        Runnable task = () -> applyEyeVisual(eye, marked);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            MAIN_HANDLER.post(task);
        }
    }

    private static void applyEyeVisual(ImageView eye, boolean marked) {
        if (marked) {
            UI.setThemedIcon(eye, UI.DRAWABLE_EYE_ICON);
            eye.setAlpha(0.65f);
        } else {
            UI.setThemedIcon(eye, UI.DRAWABLE_EYE_STROKE_ICON);
            eye.setAlpha(1.0f);
        }
    }

    private static void sendReceipt(UserSession session, Object context,
                                    String threadId, String messageId, String senderId) {
    }
}
