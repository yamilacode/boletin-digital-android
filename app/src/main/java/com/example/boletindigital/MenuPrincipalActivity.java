package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
            String etiqueta = (nombre != null ? nombre : "USUARIO") + " • ROL: " + rol.toUpperCase();
            tvSaludoRol.setText(etiqueta);
        }

        // Hero Card: Carga de Calificaciones (Docente / Preceptor)
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

        // Módulo 1: Alumnos
        View cardModuloAlumnos = findViewById(R.id.cardModuloAlumnos);
        if (cardModuloAlumnos != null) {
            cardModuloAlumnos.setOnClickListener(v -> abrirAlumnos());
        }

        // Barra de búsqueda directa hacia Alumnos
        View cardBuscador = findViewById(R.id.cardBuscador);
        if (cardBuscador != null) {
            cardBuscador.setOnClickListener(v -> abrirAlumnos());
        }

        // Módulo 2: Asistencia
        View cardModuloAsistencia = findViewById(R.id.cardModuloAsistencia);
        if (cardModuloAsistencia != null) {
            cardModuloAsistencia.setOnClickListener(v -> {
                Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
                startActivity(intent);
            });
        }

        // Módulo 3: Mesas de Examen
        View cardModuloMesas = findViewById(R.id.cardModuloMesas);
        if (cardModuloMesas != null) {
            cardModuloMesas.setOnClickListener(v -> {
                Intent intent = new Intent(this, PreviasMesasActivity.class);
                startActivity(intent);
            });
        }

        // Módulo 4: Institución Escolar (Sedes, autoridades y régimen)
        View cardModuloInstitucion = findViewById(R.id.cardModuloInstitucion);
        if (cardModuloInstitucion != null) {
            cardModuloInstitucion.setOnClickListener(v -> {
                Intent intent = new Intent(this, InstitucionActivity.class);
                intent.putExtra("token", token);
                intent.putExtra("rol", rol);
                intent.putExtra("nombre", nombre);
                startActivity(intent);
            });
        }

        // Pestaña Avisos (Firebase Cloud)
        View tabAvisos = findViewById(R.id.tabAvisos);
        if (tabAvisos != null) {
            tabAvisos.setOnClickListener(v -> {
                Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
                startActivity(intent);
            });
        }

        // Pestaña Perfil y Ajustes
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
                .setTitle("Sesión Activa")
                .setMessage("Usuario: " + (nombre != null ? nombre : "Institucional")
                        + "\nRol: " + (rol != null ? rol : "Sin definir")
                        + "\nServidor: Railway Cloud API")
                .setPositiveButton("Cerrar Sesión", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Continuar", null)
                .show();
    }
}