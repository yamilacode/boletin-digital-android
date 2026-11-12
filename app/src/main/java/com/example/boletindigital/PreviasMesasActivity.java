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

    private String dni;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String rol = getIntent().getStringExtra("rol");
        dni = getIntent().getStringExtra("dni");
        boolean rolNoEstudiante = rol != null && !rol.trim().isEmpty()
                && !"estudiante".equalsIgnoreCase(rol.trim())
                && !"tutor".equalsIgnoreCase(rol.trim())
                && !"alumno".equalsIgnoreCase(rol.trim())
                && !"padre".equalsIgnoreCase(rol.trim());

        if (rolNoEstudiante || dni == null || dni.trim().isEmpty()) {
            Toast.makeText(this, "Sección exclusiva para sesión de Estudiante o Tutor.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_previas_mesas);

        ivVolver = findViewById(R.id.ivVolver);
        btnInscribirMesa1 = findViewById(R.id.btnInscribirMesa1);
        btnInscribirMesa2 = findViewById(R.id.btnInscribirMesa2);

        tabNavBoletin = findViewById(R.id.tabNavBoletin);
        tabNavAsistencia = findViewById(R.id.tabNavAsistencia);
        tabNavAvisos = findViewById(R.id.tabNavAvisos);

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

        // Navegación inferior exclusiva de sesión estudiante validada
        if (tabNavBoletin != null) {
            tabNavBoletin.setOnClickListener(v -> abrirTabBoletin(1));
        }

        if (tabNavAsistencia != null) {
            tabNavAsistencia.setOnClickListener(v -> abrirTabBoletin(2));
        }

        if (tabNavAvisos != null) {
            tabNavAvisos.setOnClickListener(v -> abrirTabBoletin(4));
        }
    }

    private void abrirTabBoletin(int tab) {
        Intent intent = new Intent(this, BoletinAlumnoActivity.class);
        intent.putExtra("dni", dni);
        intent.putExtra("tab", tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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

