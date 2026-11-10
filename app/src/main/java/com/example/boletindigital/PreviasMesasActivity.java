package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PreviasMesasActivity extends AppCompatActivity {

    private ImageView ivVolver;
    private TextView btnInscribirMesa1;
    private TextView btnInscribirMesa2;

    private View tabNavBoletin;
    private View tabNavAsistencia;
    private View tabNavAvisos;

    private String dni = "48763736";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previas_mesas);

        ivVolver = findViewById(R.id.ivVolver);
        btnInscribirMesa1 = findViewById(R.id.btnInscribirMesa1);
        btnInscribirMesa2 = findViewById(R.id.btnInscribirMesa2);

        tabNavBoletin = findViewById(R.id.tabNavBoletin);
        tabNavAsistencia = findViewById(R.id.tabNavAsistencia);
        tabNavAvisos = findViewById(R.id.tabNavAvisos);

        if (getIntent().hasExtra("dni")) {
            dni = getIntent().getStringExtra("dni");
        }

        if (ivVolver != null) {
            ivVolver.setOnClickListener(v -> finish());
        }

        // Inscripción a Mesas de Examen
        if (btnInscribirMesa1 != null) {
            btnInscribirMesa1.setOnClickListener(v -> mostrarDialogoInscripcion("Físico-Química (3° Año)"));
        }

        if (btnInscribirMesa2 != null) {
            btnInscribirMesa2.setOnClickListener(v -> mostrarDialogoInscripcion("Geografía (2° Año)"));
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

        if (tabNavAsistencia != null) {
            tabNavAsistencia.setOnClickListener(v -> {
                Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
                intent.putExtra("dni", dni);
                startActivity(intent);
                finish();
            });
        }

        if (tabNavAvisos != null) {
            tabNavAvisos.setOnClickListener(v -> {
                Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
                intent.putExtra("dni", dni);
                startActivity(intent);
                finish();
            });
        }
    }

    private void mostrarDialogoInscripcion(String materia) {
        new AlertDialog.Builder(this)
                .setTitle("Inscripción a Mesa Evaluadora")
                .setMessage("¿Confirmás tu solicitud de inscripción para rendir la materia previa " + materia + " en el período Diciembre 2026?")
                .setPositiveButton("Confirmar Inscripción", (dialog, which) -> {
                    Toast.makeText(this, "Inscripción registrada con éxito para " + materia, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}

