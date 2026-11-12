package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsistenciaAvisosActivity extends AppCompatActivity {

    private ImageView ivVolver;
    private TextView tvBadgeTotalFaltas;
    private TextView tvResumenAsistencia;
    private ProgressBar pbInasistencias;

    private View tabNavBoletin;
    private View tabNavPrevias;
    private View btnProbarNotificacion;

    private String dni = "48763736";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asistencia_avisos);

        ivVolver = findViewById(R.id.ivVolver);
        tvBadgeTotalFaltas = findViewById(R.id.tvBadgeTotalFaltas);
        tvResumenAsistencia = findViewById(R.id.tvResumenAsistencia);
        pbInasistencias = findViewById(R.id.pbInasistencias);

        tabNavBoletin = findViewById(R.id.tabNavBoletin);
        tabNavPrevias = findViewById(R.id.tabNavPrevias);
        btnProbarNotificacion = findViewById(R.id.btnProbarNotificacion);

        if (getIntent().hasExtra("dni")) {
            dni = getIntent().getStringExtra("dni");
        }

        if (ivVolver != null) {
            ivVolver.setOnClickListener(v -> finish());
        }

        // Suscripcion automatica a topicos de Firebase Cloud Messaging (FCM - R4)
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("avisos_generales");
            FirebaseMessaging.getInstance().subscribeToTopic("mesas_examen");
            FirebaseMessaging.getInstance().subscribeToTopic("inasistencias");
        } catch (Throwable ignored) {
        }

        // Boton interactivo para probar la notificacion en el celular
        if (btnProbarNotificacion != null) {
            btnProbarNotificacion.setOnClickListener(v -> mostrarDialogoPruebaNotificacion());
        }

        // Navegacion inferior
        if (tabNavBoletin != null) {
            tabNavBoletin.setOnClickListener(v -> {
                Intent intent = new Intent(this, BoletinAlumnoActivity.class);
                intent.putExtra("dni", dni);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (tabNavPrevias != null) {
            tabNavPrevias.setOnClickListener(v -> {
                Intent intent = new Intent(this, PreviasMesasActivity.class);
                intent.putExtra("dni", dni);
                startActivity(intent);
                finish();
            });
        }

        cargarDatosAsistencia();
    }

    private void mostrarDialogoPruebaNotificacion() {
        String[] opciones = {
                "Alerta de Inasistencias (17 de 20 faltas)",
                "Mesas de Examen y Previas (Diciembre 2026)",
                "Boletin y Calificaciones (1° Cuatrimestre)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Lanzar Notificacion al Celular")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        BoletinFirebaseMessagingService.lanzarNotificacion(
                                this,
                                "Alerta de Inasistencias",
                                "El estudiante registra 17 inasistencias sobre un limite institucional de 20.",
                                "asistencia"
                        );
                        Toast.makeText(this, "Notificacion enviada a la barra de estado", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        BoletinFirebaseMessagingService.lanzarNotificacion(
                                this,
                                "Mesas Evaluadoras Habilitadas",
                                "Se encuentra abierta la inscripcion para materias previas (Fisico-Quimica y Geografia).",
                                "mesas"
                        );
                        Toast.makeText(this, "Notificacion de examen enviada", Toast.LENGTH_SHORT).show();
                    } else {
                        BoletinFirebaseMessagingService.lanzarNotificacion(
                                this,
                                "Boletin de Calificaciones Publicado",
                                "Ya estan disponibles las valoraciones conceptuales (TEA/TEP/TED) oficiales.",
                                "boletin"
                        );
                        Toast.makeText(this, "Notificacion de boletin enviada", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cargarDatosAsistencia() {
        executor.execute(() -> {
            try {
                JSONObject json = ApiClient.obtenerBoletin(dni);
                if (json.has("asistencia")) {
                    JSONObject asis = json.getJSONObject("asistencia");
                    int totalClases = asis.optInt("total_clases", 82);
                    int presentu = asis.optInt("presentu", 65);
                    int inasistencias = asis.optInt("ausentes_injustificados", 17) + asis.optInt("ausentes_justificados", 0);

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        tvBadgeTotalFaltas.setText("Total: " + inasistencias);
                        tvResumenAsistencia.setText("Dias Habiles: " + totalClases + " • Asistencias: " + presentu + " • Inasistencias: " + inasistencias);
                        pbInasistencias.setProgress(inasistencias);
                    });
                }
            } catch (Throwable ignored) {
                // Mantiene los valores por defecto del wireframe
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}