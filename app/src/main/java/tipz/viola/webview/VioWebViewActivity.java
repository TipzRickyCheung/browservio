/*
 * Copyright (C) 2022-2023 Tipz Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tipz.viola.webview;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.CallSuper;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Objects;

import tipz.viola.Application;
import tipz.viola.BrowservioActivity;
import tipz.viola.R;
import tipz.viola.settings.SettingsKeys;
import tipz.viola.settings.SettingsUtils;
import tipz.viola.utils.CommonUtils;

public class VioWebViewActivity extends BrowservioActivity {
    public SharedPreferences pref;

    public VioWebView webview;
    public AppCompatImageView favicon;
    public CircularProgressIndicator faviconProgressBar;
    public LinearProgressIndicator progressBar;
    public SwipeRefreshLayout swipeRefreshLayout;
    public View startPageLayout;

    public AppBarLayout appbar;
    public RelativeLayout toolsContainer;
    public RelativeLayout webviewContainer;

    private boolean swipeRefreshLayoutEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = ((Application) getApplicationContext()).pref;
    }

    @Override
    public void onStart() {
        appbar = findViewById(R.id.appbar);
        webviewContainer = findViewById(R.id.webviewContainer);

        /* Init VioWebView */
        webview.doSettingsCheck();

        // Setup swipe refresh layout
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> webview.webviewReload());
            swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        }

        // Setup start page
        if (startPageLayout != null)
            startPageLayout.findViewById(R.id.startPageEditText)
                    .setOnClickListener(v ->onStartPageEditTextPressed());

        // Setup favicon
        if (favicon != null && faviconProgressBar != null)
            faviconProgressBar.setOnClickListener(_view -> favicon.performClick());
        super.onStart();
    }

    /**
     * When back button is pressed, go back in history or finish activity
     */
    @Override
    public void onBackPressed() {
        if (webview.canGoBack())
            webview.goBack();
        else
            finish();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            webview.freeMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webview != null) {
            webview.stopLoading();
            webview.setWebViewClient(null);
            webview.setWebChromeClient(null);
            // According to the doc of WebView#destroy(), webview should be removed from the view
            // system before calling the WebView#destroy().
            ((ViewGroup) webview.getParent()).removeView(webview);
            webview.destroy();
        }
        if (!isChangingConfigurations()) {
            // For removing all WebView thread
            System.exit(0);
        }
    }

    /**
     * Need Load Info Receiver
     * <p>
     * Receive needLoadUrl for loading.
     */
    public final ActivityResultLauncher<Intent> mGetNeedLoad = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                doSettingsCheck();
                webview.loadUrl(result.getData() != null ? result.getData().getStringExtra("needLoadUrl") : null);
            });

    /**
     * Config Checker
     * <p>
     * Used to check if anything has been changed
     * after returning from settings.
     */
    @Override
    public void doSettingsCheck() {
        super.doSettingsCheck();

        // Pull to Refresh
        if (swipeRefreshLayout != null && swipeRefreshLayoutEnabled) {
            swipeRefreshLayout.setEnabled(CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.enableSwipeRefresh)));
        }

        // Favicon
        if (favicon != null && faviconProgressBar != null) {
            favicon.setVisibility(CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.showFavicon)) ? View.VISIBLE : View.GONE);
            if (CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.showFavicon))
                    && faviconProgressBar.getVisibility() == View.VISIBLE)
                favicon.setVisibility(View.GONE);
        }

        // Reach mode
        reachModeCheck();
    }

    public void reachModeCheck() {
        CoordinatorLayout.LayoutParams appBarParams =
                (CoordinatorLayout.LayoutParams) appbar.getLayoutParams();
        CoordinatorLayout.LayoutParams toolsContainerParams = null;
        CoordinatorLayout.LayoutParams webviewContainerParams =
                (CoordinatorLayout.LayoutParams) webviewContainer.getLayoutParams();

        if (toolsContainer != null)
            toolsContainerParams = (CoordinatorLayout.LayoutParams) toolsContainer.getLayoutParams();

        // FIXME: These are hardcoded values
        int actionBarSize = (int) CommonUtils.getDisplayMetrics(
                VioWebViewActivity.this, 52);
        int toolsContainerSize = (int) CommonUtils.getDisplayMetrics(
                VioWebViewActivity.this, 36);
        int margin = actionBarSize;
        if (toolsContainer != null && toolsContainer.getVisibility() == View.VISIBLE)
            margin = margin + toolsContainerSize;

        if (CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.reverseLayout))) {
            if (CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.reverseOnlyActionBar))) {
                appBarParams.gravity = Gravity.TOP;
                if (toolsContainerParams != null) {
                    toolsContainer.setVisibility(View.VISIBLE);
                    toolsContainerParams.setMargins(0, 0, 0, 0);
                }
                webviewContainerParams.setMargins(0, actionBarSize, 0, toolsContainerSize);
            } else {
                appBarParams.gravity = Gravity.BOTTOM;
                if (toolsContainerParams != null)
                    toolsContainerParams.setMargins(0, 0, 0, actionBarSize);
                webviewContainerParams.setMargins(0, 0, 0, margin);
            }

            if (toolsContainerParams != null)
                toolsContainerParams.gravity = Gravity.BOTTOM;
        } else {
            appBarParams.gravity = Gravity.TOP;
            if (toolsContainerParams != null) {
                toolsContainerParams.gravity = Gravity.TOP;
                toolsContainerParams.setMargins(0, actionBarSize, 0, 0);
            }
            webviewContainerParams.setMargins(0, margin, 0, 0);
        }

        appbar.setLayoutParams(appBarParams);
        appbar.invalidate();
        if (toolsContainer != null) {
            toolsContainer.setLayoutParams(toolsContainerParams);
            toolsContainer.invalidate();
        }
        webviewContainer.setLayoutParams(webviewContainerParams);
        webviewContainer.invalidate();
    }

    public void onUrlUpdated(String url) {

    }

    public void onUrlUpdated(String url, int position) {

    }

    @CallSuper
    public void onTitleUpdated(String title) {
        if (CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.updateRecentsIcon))
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription description =
                    new ActivityManager.TaskDescription(title);
            this.setTaskDescription(description);
        }
    }

    public void onDropDownDismissed() {

    }

    public void onStartPageEditTextPressed() {

    }

    @CallSuper
    public void onFaviconUpdated(Bitmap icon, boolean checkInstance) {
        if (favicon != null && faviconProgressBar != null) {
            if (checkInstance && (favicon.getDrawable() instanceof BitmapDrawable))
                return;

            if (icon == null)
                favicon.setImageResource(R.drawable.default_favicon);
            else
                favicon.setImageBitmap(icon);
        }

        if (CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.updateRecentsIcon))
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription description =
                    new ActivityManager.TaskDescription(webview.getTitle(), icon);
            this.setTaskDescription(description);
        }
    }

    @CallSuper
    public void onFaviconProgressUpdated(boolean isLoading) {
        if (favicon != null && faviconProgressBar != null) {
            if (!CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.showFavicon)))
                return;

            if (isLoading) {
                favicon.setVisibility(View.GONE);
                faviconProgressBar.setVisibility(View.VISIBLE);
            } else {
                favicon.setVisibility(View.VISIBLE);
                faviconProgressBar.setVisibility(View.GONE);
            }
        }
    }

    @CallSuper
    public void onSwipeRefreshLayoutRefreshingUpdated(boolean isRefreshing) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(isRefreshing);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webview.evaluateJavascript("getComputedStyle(document.body).getPropertyValue('overflow-y')", value1 -> {
                    updateSwipeRefreshLayoutEnabled(!Objects.equals(getTrueCSSValue(value1), "hidden"));
                    if (swipeRefreshLayoutEnabled)
                        webview.evaluateJavascript("getComputedStyle(document.body).getPropertyValue('overscroll-behavior-y')", value2 ->
                                updateSwipeRefreshLayoutEnabled(Objects.equals(getTrueCSSValue(value2), "auto")));
                });
            }
        }
    }

    private void updateSwipeRefreshLayoutEnabled(boolean isEnabled) {
        swipeRefreshLayoutEnabled = isEnabled;
        if (CommonUtils.isIntStrOne(SettingsUtils.getPrefNum(pref, SettingsKeys.enableSwipeRefresh)))
            swipeRefreshLayout.setEnabled(swipeRefreshLayoutEnabled);
    }

    @CallSuper
    public void onPageLoadProgressChanged(int progress) {
        if (progressBar != null)
            progressBar.setProgress(progress == 100 ? 0 : progress);
    }

    private String getTrueCSSValue(String rawValue) {
        String[] arrayValue;
        if (rawValue.contains("\""))
            rawValue = rawValue.replace("\"", "");

        if (rawValue.equals("null"))
            return "auto";

        arrayValue = rawValue.split(" ");
        return arrayValue[arrayValue.length - 1];
    }
}