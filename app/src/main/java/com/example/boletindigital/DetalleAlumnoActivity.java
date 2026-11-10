package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetalleAlumnoActivity extends AppCompatActivity {

    private TextView tvNombreAlumno;
    private TextView tvDniAlumno;
    private TextView tvCursoSeccion;
    private TextView tvJurisdiccion;
    private TextView tvInstitucion;
    private TextView tvTutor;
    private TextView tvDetalle;
    private ProgressBar pbCargandoDetalle;
    private Button btnVerBoletin;
    private Button btnVolver;

    private String dniActual = "";

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_alumno);

        tvNombreAlumno = findViewById(R.id.tvNombreAlumno);
        tvDniAlumno = findViewById(R.id.tvDniAlumno);
        tvCursoSeccion = findViewById(R.id.tvCursoSeccion);
        tvJurisdiccion = findViewById(R.id.tvJurisdiccion);
        tvInstitucion = findViewById(R.id.tvInstitucion);
        tvTutor = findViewById(R.id.tvTutor);
        tvDetalle = findViewById(R.id.tvDetalle);
        pbCargandoDetalle = findViewById(R.id.pbCargandoDetalle);
        btnVerBoletin = findViewById(R.id.btnVerBoletin);
        btnVolver = findViewById(R.id.btnVolver);

        if (btnVolver != null) {
            btnVolver.setOnClickListener(v -> finish());
        }

        if (btnVerBoletin != null) {
            btnVerBoletin.setOnClickListener(v -> {
                Intent intent = new Intent(this, BoletinAlumnoActivity.class);
                if (!dniActual.isEmpty()) {
                    intent.putExtra("dni", dniActual);
                }
                startActivity(intent);
            });
        }

        int idAlumno = getIntent().getIntExtra("id_alumno", -1);

        if (idAlumno <= 0) {
            tvNombreAlumno.setText(R.string.error_id);
            return;
        }

        cargarDetalle(idAlumno);
    }

    private void cargarDetalle(int id) {

        tvNombreAlumno.setText(R.string.cargando);
        if (pbCargandoDetalle != null) {
            pbCargandoDetalle.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {

            try {
                JSONObject respuesta = ApiClient.obtenerAlumno(id);
                Alumno alumno = new Alumno(respuesta);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (pbCargandoDetalle != null) {
                        pbCargandoDetalle.setVisibility(View.GONE);
                    }

                    tvNombreAlumno.setText(alumno.getNombreCompleto());
                    dniActual = alumno.getDni();
                    tvDniAlumno.setText("DNI: " + dniActual);
                    tvCursoSeccion.setText(alumno.getCurso() + " " + alumno.getSeccion());
                    tvJurisdiccion.setText(alumno.getJurisdiccion());
                    tvInstitucion.setText(String.valueOf(alumno.getIdInstitucion()));
                    tvTutor.setText(alumno.getNombreTutor());
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (pbCargandoDetalle != null) {
                        pbCargandoDetalle.setVisibility(View.GONE);
                    }

                    String mensaje = e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : e instanceof IOException
                            ? getString(R.string.error_conexion)
                            : getString(R.string.error_respuesta);

                    tvNombreAlumno.setText("Error al cargar");
                    if (tvDetalle != null) {
                        tvDetalle.setVisibility(View.VISIBLE);
                        tvDetalle.setText(mensaje);
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
