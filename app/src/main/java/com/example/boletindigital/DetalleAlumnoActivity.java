
package com.example.boletindigital;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetalleAlumnoActivity extends AppCompatActivity {

    private TextView tvDetalle;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detalle_alumno);

        tvDetalle = findViewById(R.id.tvDetalle);

        int idAlumno = getIntent().getIntExtra("id_alumno", -1);

        if (idAlumno <= 0) {
            tvDetalle.setText(R.string.error_id);
            return;
        }

        cargarDetalle(idAlumno);
    }

    private void cargarDetalle(int id) {

        tvDetalle.setText(R.string.cargando);

        executor.execute(() -> {

            try {
                JSONObject respuesta = ApiClient.obtenerAlumno(id);
                Alumno alumno = new Alumno(respuesta);

                String informacion =
                        "Nombre: " + alumno.getNombreCompleto()
                                + "\nDNI: " + alumno.getDni()
                                + "\nJurisdicción: " + alumno.getJurisdiccion()
                                + "\nCurso: " + alumno.getCurso()
                                + "\nSección: " + alumno.getSeccion()
                                + "\nTutor: " + alumno.getNombreTutor();

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        tvDetalle.setText(informacion);
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    String mensaje = e instanceof ApiClient.ApiException
                            ? e.getMessage()
                            : e instanceof IOException
                            ? getString(R.string.error_conexion)
                            : getString(R.string.error_respuesta);

                    tvDetalle.setText(mensaje);
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
