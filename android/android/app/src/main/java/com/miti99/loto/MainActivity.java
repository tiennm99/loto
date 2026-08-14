package com.miti99.loto;

import android.media.AudioManager;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = webView();
        if (webView != null) {
            // The player card is a fixed 9-column grid, so the system font
            // scale (users routinely run 130-200%) clips two-digit numbers
            // out of their cells. Pinning the zoom is only defensible
            // because the app ships its own size control in Settings
            // ("Cỡ chữ bảng") as the accessible replacement.
            webView.getSettings().setTextZoom(100);
        }

        // Calling numbers aloud is the app's whole job, so the volume rocker
        // should reach the media stream even before the first clip plays.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Android 16 (targetSdk 36) no longer calls onBackPressed() and no
        // longer dispatches KEYCODE_BACK, so back has to come through the
        // dispatcher. Each open overlay pushes one history entry (see
        // web/src/lib/overlay-history.js), which makes "the WebView can go
        // back" mean exactly "an overlay is open".
        getOnBackPressedDispatcher()
            .addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        WebView wv = webView();
                        if (wv != null && wv.canGoBack()) {
                            wv.goBack();
                            return;
                        }
                        confirmExit();
                    }
                }
            );
    }

    /** Null-safe accessor — the bridge is built during super.onCreate(). */
    private WebView webView() {
        Bridge bridge = getBridge();
        return bridge == null ? null : bridge.getWebView();
    }

    /**
     * Auto-call runs untouched for minutes at a time, so a stray back press
     * at the root would otherwise drop the caller out of a live round.
     */
    private void confirmExit() {
        // Explicit AppCompat dialog theme: the activity runs on
        // AppTheme.NoActionBarLaunch (parent Theme.SplashScreen), which is
        // not guaranteed to be AppCompat-derived at dialog time.
        new AlertDialog.Builder(this, R.style.AppTheme_ExitDialog)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setNegativeButton(R.string.exit_cancel, null)
            .setPositiveButton(R.string.exit_confirm, (dialog, which) -> finish())
            .show();
    }
}
