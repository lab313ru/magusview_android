package com.magusview.android;

public final class MagusViewApplication extends android.app.Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashDiagnostics.initialize(this);
    }
}
