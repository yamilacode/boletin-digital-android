package com.example.boletindigital;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class BoletinFirebaseMessagingService extends FirebaseMessagingService {

    public static final String CANAL_ID = "boletin_avisos_canal";
    public static final String CANAL_NOMBRE = "Avisos Oficiales - Boletin Escolar";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo = "Boletín Digital Escolar";
        String mensaje = "Nuevo comunicado institucional recibido.";
        String tipo = "general";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                titulo = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                mensaje = remoteMessage.getNotification().getBody();
            }
        }

        if (!remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();
            if (data.containsKey("titulo")) {
                titulo = data.get("titulo");
            }
            if (data.containsKey("mensaje")) {
                mensaje = data.get("mensaje");
            }
            if (data.containsKey("tipo")) {
                tipo = data.get("tipo");
            }
        }

        lanzarNotificacion(this, titulo, mensaje, tipo);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }

    public static void lanzarNotificacion(Context context, String titulo, String mensaje, String tipo) {
        try {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel canal = new NotificationChannel(
                        CANAL_ID,
                        CANAL_NOMBRE,
                        NotificationManager.IMPORTANCE_HIGH
                );
                canal.setDescription("Notificaciones urgentes de inasistencias, mesas de examen y boletines");
                canal.enableLights(true);
                canal.enableVibration(true);
                if (manager != null) {
                    manager.createNotificationChannel(canal);
                }
            }

            Intent destinoIntent = new Intent(context, BoletinAlumnoActivity.class);
            destinoIntent.putExtra("dni", "48763736");
            int tabDestino = 4; // Por defecto abre la pestaña de Avisos
            if (tipo != null) {
                String t = tipo.toLowerCase();
                if (t.contains("boletin") || t.contains("calific")) {
                    tabDestino = 1;
                } else if (t.contains("asist") || t.contains("inasist")) {
                    tabDestino = 2;
                } else if (t.contains("mesa") || t.contains("previa")) {
                    tabDestino = 3;
                }
            }
            destinoIntent.putExtra("tab", tabDestino);
            destinoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            int flagsPending = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
                    destinoIntent,
                    flagsPending
            );

            Uri sonidoDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                    .setAutoCancel(true)
                    .setSound(sonidoDefault)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent);

            if (manager != null) {
                int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                manager.notify(notificationId, builder.build());
            }
        } catch (Throwable ignored) {
        }
    }
}
