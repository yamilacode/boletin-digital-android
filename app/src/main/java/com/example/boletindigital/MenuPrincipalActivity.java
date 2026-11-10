package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MenuPrincipalActivity extends AppCompatActivity {

    private String token;
    private String rol;
    private String nombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        TextView tvSaludoRol = findViewById(R.id.tvSaludoRol);
        if (tvSaludoRol != null && rol != null) {
            String etiqueta = (nombre != null ? nombre : "USUARIO") + " â€¢ ROL: " + rol.toUpperCase();
            tvSaludoRol.setText(etiqueta);
        }

                // MÃ³dulo Principal: Carga de Calificaciones (Planilla escolar RITE)
        View cardModuloCargaNotas = findViewById(R.id.cardModuloCargaNotas);
        if (cardModuloCargaNotas != null) {
            cardModuloCargaNotas.setOnClickListener(v -> {
                Intent intent = new Intent(this, CargarNotasActivity.class);
                intent.putExtra("token", token);
                intent.putExtra("rol", rol);
                intent.putExtra("nombre", nombre);
                startActivity(intent);
            });
        }

        // MÃ³dulo 1: Alumnos (Abre el listado desarrollado por Pochy y rediseÃ±ado)
        View cardModuloAlumnos = findViewById(R.id.cardModuloAlumnos);
        if (cardModuloAlumnos != null) {
            cardModuloAlumnos.setOnClickListener(v -> abrirAlumnos());
        }

        // Barra de bÃºsqueda directa hacia Alumnos
        View cardBuscador = findViewById(R.id.cardBuscador);
        if (cardBuscador != null) {
            cardBuscador.setOnClickListener(v -> abrirAlumnos());
        }

        // MÃ³dulo 2: Asistencia
        View cardModuloAsistencia = findViewById(R.id.cardModuloAsistencia);
        if (cardModuloAsistencia != null) {
            cardModuloAsistencia.setOnClickListener(v -> {
                Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
                startActivity(intent);
            });
        }

        // MÃ³dulo 3: Mesas de Examen
        View cardModuloMesas = findViewById(R.id.cardModuloMesas);
        if (cardModuloMesas != null) {
            cardModuloMesas.setOnClickListener(v -> {
                Intent intent = new Intent(this, PreviasMesasActivity.class);
                startActivity(intent);
            });
        }

        // MÃ³dulo 4: InstituciÃ³n
        View cardModuloInstitucion = findViewById(R.id.cardModuloInstitucion);
        if (cardModuloInstitucion != null) {
            cardModuloInstitucion.setOnClickListener(v ->
                    Toast.makeText(this, "InstituciÃ³n Escolar: E.E.S. NÂ° 5 â€¢ R.I.T.E.", Toast.LENGTH_SHORT).show()
            );
        }

        // PestaÃ±a Avisos (Firebase Cloud)
        View tabAvisos = findViewById(R.id.tabAvisos);
        if (tabAvisos != null) {
            tabAvisos.setOnClickListener(v -> {
                Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
                startActivity(intent);
            });
        }

        // PestaÃ±a Perfil y Ajustes
        View tabPerfil = findViewById(R.id.tabPerfil);
        View btnAjustes = findViewById(R.id.btnAjustes);

        View.OnClickListener perfilListener = v -> mostrarMenuPerfil();

        if (tabPerfil != null) tabPerfil.setOnClickListener(perfilListener);
        if (btnAjustes != null) btnAjustes.setOnClickListener(perfilListener);
    }

    private void abrirAlumnos() {
        Intent intent = new Intent(this, AlumnosActivity.class);
        intent.putExtra("token", token);
        intent.putExtra("rol", rol);
        intent.putExtra("nombre", nombre);
        startActivity(intent);
    }

    private void mostrarMenuPerfil() {
        new AlertDialog.Builder(this)
                .setTitle("SesiÃ³n Activa")
                .setMessage("Usuario: " + (nombre != null ? nombre : "Institucional")
                        + "\nRol: " + (rol != null ? rol : "Sin definir")
                        + "\nServidor: Railway Cloud API")
                .setPositiveButton("Cerrar SesiÃ³n", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Continuar", null)
                .show();
    }
}

