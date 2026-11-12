package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MenuPrincipalActivity extends AppCompatActivity {

    private String token;
    private String rol;
    private String nombre;
    private TextView tvSedeActivaMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_principal);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        actualizarSaludoRol();

        tvSedeActivaMenu = findViewById(R.id.tvSedeActivaMenu);
        vincularModulo(R.id.cardSedeActivaMenu, InstitucionActivity.class);

        // Módulo 1: Carga de Calificaciones (Docente / Preceptor)
        vincularModulo(R.id.cardModuloCargaNotas, CargarNotasActivity.class);

        // Módulo 2: Alumnos
        vincularModulo(R.id.cardModuloAlumnos, AlumnosActivity.class);

        // Módulo 3: Carga de Asistencia Escolar (Preceptor / Docente)
        vincularModulo(R.id.cardModuloAsistencia, CargarAsistenciaActivity.class);

        // Módulo 4: Mesas de Examen (Preceptor / Docente)
        vincularModulo(R.id.cardModuloMesas, GestionMesasActivity.class);

        // Módulo 5: Materias Previas por Alumno (Preceptor / Docente)
        vincularModulo(R.id.cardModuloPrevias, GestionPreviasActivity.class);

        // Módulo 6: Institución Escolar (Sedes, autoridades y régimen)
        vincularModulo(R.id.cardModuloInstitucion, InstitucionActivity.class);

        // Pestaña Avisos: Gestión y Publicación de Comunicados (Preceptor / Docente)
        vincularModulo(R.id.tabAvisos, GestionAvisosActivity.class);

        // Pestaña Perfil y Ajustes
        vincularModulo(R.id.tabPerfil, PerfilDocenteActivity.class);
        vincularModulo(R.id.btnAjustes, PerfilDocenteActivity.class);

        actualizarBadgeSedeActiva();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarBadgeSedeActiva();
        actualizarSaludoRol();
    }

    private void actualizarSaludoRol() {
        TextView tvSaludoRol = findViewById(R.id.tvSaludoRol);
        if (tvSaludoRol != null && rol != null) {
            String nombreActivo = PerfilDocenteActivity.obtenerNombreActivo(this, nombre);
            String etiqueta = nombreActivo + " • ROL: " + rol.toUpperCase();
            tvSaludoRol.setText(etiqueta);
        }
    }

    private void actualizarBadgeSedeActiva() {
        if (tvSedeActivaMenu != null) {
            int idInst = InstitucionManager.getIdInstitucionActiva(this);
            String texto = InstitucionManager.getNombreCorto(idInst) + " • "
                    + InstitucionManager.getRegimenCorto(idInst) + " ›";
            tvSedeActivaMenu.setText(texto);
        }
    }

    private void vincularModulo(int viewId, Class<?> destino) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> {
                Intent intent = new Intent(this, destino);
                intent.putExtra("token", token);
                intent.putExtra("rol", rol);
                intent.putExtra("nombre", PerfilDocenteActivity.obtenerNombreActivo(this, nombre));
                startActivity(intent);
            });
        }
    }
}
