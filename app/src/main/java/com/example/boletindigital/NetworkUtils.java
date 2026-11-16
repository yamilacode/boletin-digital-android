package com.example.boletindigital;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Chequeo de conectividad del dispositivo (R3: Control de red / Modo avión).
 * Permite detectar sin señal o modo avión ANTES de intentar una petición a
 * la API, para mostrar un mensaje claro en vez de esperar un timeout.
 */
public class NetworkUtils {

    private NetworkUtils() {
    }

    public static boolean hayConexion() {
        Context contexto = BoletinApp.getContexto();
        if (contexto == null) {
            // Si todavía no se inicializó el Application, no bloqueamos la petición.
            return true;
        }

        ConnectivityManager administrador = (ConnectivityManager)
                contexto.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (administrador == null) {
            return true;
        }

        Network redActiva = administrador.getActiveNetwork();
        if (redActiva == null) {
            return false;
        }

        NetworkCapabilities capacidades =
                administrador.getNetworkCapabilities(redActiva);

        return capacidades != null
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
