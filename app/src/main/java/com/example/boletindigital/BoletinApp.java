package com.example.boletindigital;

import android.app.Application;
import android.content.Context;

/**
 * Application propia de la app. Guarda el Context de aplicación para que
 * clases sin Activity (como ApiClient) puedan consultar el estado de la red
 * antes de intentar una conexión HTTP (control de modo avión / sin señal - R3).
 */
public class BoletinApp extends Application {

    private static Context contextoApp;

    @Override
    public void onCreate() {
        super.onCreate();
        contextoApp = getApplicationContext();
    }

    public static Context getContexto() {
        return contextoApp;
    }
}
