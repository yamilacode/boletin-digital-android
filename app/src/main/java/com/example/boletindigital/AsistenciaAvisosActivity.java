package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        if (getIntent().hasExtra("dni")) {
            dni = getIntent().getStringExtra("dni");
        }

        if (ivVolver != null) {
            ivVolver.setOnClickListener(v -> finish());
        }

        // Navegación inferior
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
                        tvResumenAsistencia.setText("Días Hábiles: " + totalClases + " • Asistencias: " + presentu + " • Inasistencias: " + inasistencias);
                        pbInasistencias.setProgress(inasistencias);
                    });
                }
            } catch (Exception ignored) {
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

