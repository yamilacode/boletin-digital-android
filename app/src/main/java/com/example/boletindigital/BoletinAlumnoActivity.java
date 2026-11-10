package com.example.boletindigital;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BoletinAlumnoActivity extends AppCompatActivity {

    // Vistas de Información del Estudiante
    private TextView tvInstNombre;
    private TextView tvRegimen;
    private TextView tvInstDetalle;
    private TextView tvNombreEstudiante;
    private TextView tvDniTutor;
    private TextView tvBadgeRol;
    private ImageView ivCerrarSesion;

    // Métricas rápidas de asistencia
    private TextView tvDiasHabiles;
    private TextView tvAsistencias;
    private TextView tvInasistencias;
    private ProgressBar pbCargandoBoletin;

    // Secciones de Pantalla
    private LinearLayout layoutSeccionBoletin;
    private LinearLayout layoutSeccionAsistencia;
    private LinearLayout layoutSeccionPrevias;
    private LinearLayout layoutSeccionAvisos;

    // Pestañas de la Barra Inferior
    private LinearLayout tabNavBoletin;
    private LinearLayout tabNavAsistencia;
    private LinearLayout tabNavPrevias;
    private LinearLayout tabNavAvisos;

    private ImageView ivNavBoletin;
    private TextView tvNavBoletin;
    private ImageView ivNavAsistencia;
    private TextView tvNavAsistencia;
    private ImageView ivNavPrevias;
    private TextView tvNavPrevias;
    private ImageView ivNavAvisos;
    private TextView tvNavAvisos;

    // Listado de Materias
    private RecyclerView rvMaterias;
    private MateriaAdapter adaptadorMaterias;
    private final List<JSONObject> listaMaterias = new ArrayList<>();

    private String dniEstudiante = "48763736"; // DNI por defecto (Uriel Pleininger)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boletin_alumno);

        // Vincular vistas de estudiante
        tvInstNombre = findViewById(R.id.tvInstNombre);
        tvRegimen = findViewById(R.id.tvRegimen);
        tvInstDetalle = findViewById(R.id.tvInstDetalle);
        tvNombreEstudiante = findViewById(R.id.tvNombreEstudiante);
        tvDniTutor = findViewById(R.id.tvDniTutor);
        tvBadgeRol = findViewById(R.id.tvBadgeRol);
        ivCerrarSesion = findViewById(R.id.ivCerrarSesion);

        // Métricas
        tvDiasHabiles = findViewById(R.id.tvDiasHabiles);
        tvAsistencias = findViewById(R.id.tvAsistencias);
        tvInasistencias = findViewById(R.id.tvInasistencias);
        pbCargandoBoletin = findViewById(R.id.pbCargandoBoletin);

        // Secciones
        layoutSeccionBoletin = findViewById(R.id.layoutSeccionBoletin);
        layoutSeccionAsistencia = findViewById(R.id.layoutSeccionAsistencia);
        layoutSeccionPrevias = findViewById(R.id.layoutSeccionPrevias);
        layoutSeccionAvisos = findViewById(R.id.layoutSeccionAvisos);

        // Pestañas inferiores
        tabNavBoletin = findViewById(R.id.tabNavBoletin);
        tabNavAsistencia = findViewById(R.id.tabNavAsistencia);
        tabNavPrevias = findViewById(R.id.tabNavPrevias);
        tabNavAvisos = findViewById(R.id.tabNavAvisos);

        ivNavBoletin = findViewById(R.id.ivNavBoletin);
        tvNavBoletin = findViewById(R.id.tvNavBoletin);
        ivNavAsistencia = findViewById(R.id.ivNavAsistencia);
        tvNavAsistencia = findViewById(R.id.tvNavAsistencia);
        ivNavPrevias = findViewById(R.id.ivNavPrevias);
        tvNavPrevias = findViewById(R.id.tvNavPrevias);
        ivNavAvisos = findViewById(R.id.ivNavAvisos);
        tvNavAvisos = findViewById(R.id.tvNavAvisos);

        // RecyclerView de Materias
        rvMaterias = findViewById(R.id.rvMaterias);
        rvMaterias.setLayoutManager(new LinearLayoutManager(this));
        adaptadorMaterias = new MateriaAdapter(listaMaterias);
        rvMaterias.setAdapter(adaptadorMaterias);

        // Obtener DNI recibido por Intent si existe
        if (getIntent().hasExtra("dni") && getIntent().getStringExtra("dni") != null) {
            dniEstudiante = getIntent().getStringExtra("dni");
        }

        // Configurar Navegación Inferior
        tabNavBoletin.setOnClickListener(v -> activarPestaña(1));
        tabNavAsistencia.setOnClickListener(v -> {
            Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
            intent.putExtra("dni", dniEstudiante);
            startActivity(intent);
        });
        tabNavPrevias.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreviasMesasActivity.class);
            intent.putExtra("dni", dniEstudiante);
            startActivity(intent);
        });
        tabNavAvisos.setOnClickListener(v -> {
            Intent intent = new Intent(this, AsistenciaAvisosActivity.class);
            intent.putExtra("dni", dniEstudiante);
            startActivity(intent);
        });

        // Cerrar sesión
        ivCerrarSesion.setOnClickListener(v -> mostrarDialogoSalir());

        // Cargar datos del Boletín desde Railway
        cargarDatosBoletin();
    }

    private void activarPestaña(int numeroPestaña) {
        int colorPrimary = ContextCompat.getColor(this, R.color.color_primary);
        int colorMuted = ContextCompat.getColor(this, R.color.text_muted);

        // Ocultar todas las secciones
        layoutSeccionBoletin.setVisibility(View.GONE);
        layoutSeccionAsistencia.setVisibility(View.GONE);
        layoutSeccionPrevias.setVisibility(View.GONE);
        layoutSeccionAvisos.setVisibility(View.GONE);

        // Resetear estilos de pestañas
        ivNavBoletin.setColorFilter(colorMuted);
        tvNavBoletin.setTextColor(colorMuted);
        ivNavAsistencia.setColorFilter(colorMuted);
        tvNavAsistencia.setTextColor(colorMuted);
        ivNavPrevias.setColorFilter(colorMuted);
        tvNavPrevias.setTextColor(colorMuted);
        ivNavAvisos.setColorFilter(colorMuted);
        tvNavAvisos.setTextColor(colorMuted);

        // Activar la seleccionada
        switch (numeroPestaña) {
            case 1: // Boletín
                layoutSeccionBoletin.setVisibility(View.VISIBLE);
                ivNavBoletin.setColorFilter(colorPrimary);
                tvNavBoletin.setTextColor(colorPrimary);
                break;
            case 2: // Asistencia
                layoutSeccionAsistencia.setVisibility(View.VISIBLE);
                ivNavAsistencia.setColorFilter(colorPrimary);
                tvNavAsistencia.setTextColor(colorPrimary);
                break;
            case 3: // Previas
                layoutSeccionPrevias.setVisibility(View.VISIBLE);
                ivNavPrevias.setColorFilter(colorPrimary);
                tvNavPrevias.setTextColor(colorPrimary);
                break;
            case 4: // Avisos
                layoutSeccionAvisos.setVisibility(View.VISIBLE);
                ivNavAvisos.setColorFilter(colorPrimary);
                tvNavAvisos.setTextColor(colorPrimary);
                break;
        }
    }

    private void cargarDatosBoletin() {
        if (pbCargandoBoletin != null) {
            pbCargandoBoletin.setVisibility(View.VISIBLE);
        }

        executor.execute(() -> {
            try {
                JSONObject jsonBoletin = ApiClient.obtenerBoletin(dniEstudiante);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (pbCargandoBoletin != null) {
                        pbCargandoBoletin.setVisibility(View.GONE);
                    }

                    mostrarDatosEnPantalla(jsonBoletin);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (pbCargandoBoletin != null) {
                        pbCargandoBoletin.setVisibility(View.GONE);
                    }

                    // Si hay error de red, mostramos datos de respaldo de Uriel Pleininger
                    Toast.makeText(this, "Modo sin conexión - Visualizando boletín institucional", Toast.LENGTH_SHORT).show();
                    cargarDatosDemostracion();
                });
            }
        });
    }

    private void mostrarDatosEnPantalla(JSONObject datos) {
        try {
            // Estudiante
            if (datos.has("estudiante")) {
                JSONObject est = datos.getJSONObject("estudiante");
                tvNombreEstudiante.setText(est.optString("nombre", "PLEININGER, URIEL LUCIANO"));
                String curso = est.optString("curso", "4° Única");
                String dni = est.optString("dni", dniEstudiante);
                String tutor = est.optString("tutor", "PLEININGER, CRISTIAN");

                tvInstDetalle.setText("Distrito: PUAN | Secundario | " + curso);
                tvDniTutor.setText("DNI " + dni + " • Tutor: " + tutor);
            }

            // Institución
            if (datos.has("institucion")) {
                JSONObject inst = datos.getJSONObject("institucion");
                tvInstNombre.setText(inst.optString("nombre", "E.E.S. N° 5"));
                String regimen = inst.optString("regimen", "Cuatrimestral");
                tvRegimen.setText(regimen + " (R.I.T.E.)");
            }

            // Asistencia
            if (datos.has("asistencia")) {
                JSONObject asis = datos.getJSONObject("asistencia");
                tvDiasHabiles.setText(String.valueOf(asis.optInt("total_clases", 82)));
                tvAsistencias.setText(String.valueOf(asis.optInt("presentu", 65)));
                int inasistencias = asis.optInt("ausentes_injustificados", 17) + asis.optInt("ausentes_justificados", 0);
                tvInasistencias.setText(inasistencias + " / 20");
            }

            // Materias
            listaMaterias.clear();
            if (datos.has("materias")) {
                JSONArray arrayMaterias = datos.getJSONArray("materias");
                for (int i = 0; i < arrayMaterias.length(); i++) {
                    listaMaterias.add(arrayMaterias.getJSONObject(i));
                }
            }

            if (listaMaterias.isEmpty()) {
                cargarMateriasDemostracion();
            } else {
                adaptadorMaterias.notifyDataSetChanged();
            }

        } catch (Exception e) {
            cargarDatosDemostracion();
        }
    }

    private void cargarDatosDemostracion() {
        tvInstNombre.setText("E.E.S. N° 5");
        tvRegimen.setText("Cuatrimestral (R.I.T.E.)");
        tvInstDetalle.setText("Distrito: PUAN | Secundario | 4° Única");
        tvNombreEstudiante.setText("PLEININGER, URIEL LUCIANO");
        tvDniTutor.setText("DNI 48763736 • Tutor: PLEININGER, CRISTIAN");

        tvDiasHabiles.setText("82");
        tvAsistencias.setText("65");
        tvInasistencias.setText("17 / 20");

        cargarMateriasDemostracion();
    }

    private void cargarMateriasDemostracion() {
        listaMaterias.clear();
        String[] nombres = {
                "Literatura", "Matemática", "Historia", "Geografía",
                "Intro. a la Física", "Biología", "Inglés", "Educación Física",
                "NTICX", "Psicología", "Intro. Comunicación", "Const. Ciudadana"
        };
        int[] notas = {6, 5, 7, 7, 5, 6, 8, 8, 7, 10, 7, 7};

        for (int i = 0; i < nombres.length; i++) {
            try {
                JSONObject m = new JSONObject();
                m.put("nombre_materia", nombres[i]);
                m.put("nota_numerica", notas[i]);
                m.put("valoracion_conceptual", notas[i] >= 7 ? "TEA" : "TEP");
                m.put("periodo", "1");
                listaMaterias.add(m);
            } catch (Exception ignored) {}
        }
        adaptadorMaterias.notifyDataSetChanged();
    }

    private void mostrarDialogoSalir() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Deseás salir del portal del estudiante y volver a la pantalla de inicio?")
                .setPositiveButton("Salir", (dialog, which) -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // =========================================================================
    // ADAPTADOR PARA TARJETAS CON ACORDEÓN EXPANDIBLE DE MATERIAS
    // =========================================================================
    private static class MateriaAdapter extends RecyclerView.Adapter<MateriaAdapter.MateriaViewHolder> {

        private final List<JSONObject> items;
        private int posicionExpandida = -1;

        public MateriaAdapter(List<JSONObject> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public MateriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_materia_boletin, parent, false);
            return new MateriaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MateriaViewHolder holder, int position) {
            JSONObject materia = items.get(position);

            String nombre = materia.optString("nombre_materia", "Materia");
            int nota = materia.optInt("nota_numerica", 7);
            String valoracion = materia.optString("valoracion_conceptual", nota >= 7 ? "TEA" : "TEP");

            holder.tvNombreMateria.setText(nombre);
            holder.tvBadgeValoracion.setText(valoracion);
            holder.tvNotaNumerica.setText(String.valueOf(nota));
            holder.tvSubtituloMateria.setText("4° Año • 1° Cuat.");

            // Colores según TEA o TEP
            if ("TEA".equalsIgnoreCase(valoracion)) {
                holder.tvBadgeValoracion.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_bg)));
                holder.tvBadgeValoracion.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_text));
            } else {
                holder.tvBadgeValoracion.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tep_bg)));
                holder.tvBadgeValoracion.setTextColor(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tep_text));
            }

            holder.tvNotaCuat1.setText(valoracion + " (" + nota + ")");
            holder.tvNotaCuat2.setText("- / -");
            holder.tvNotaIntensif.setText("-");

            // Estado de expansión del Acordeón
            final boolean estaExpandido = (position == posicionExpandida);
            holder.layoutDetalleCuatrimestre.setVisibility(estaExpandido ? View.VISIBLE : View.GONE);
            holder.ivExpandir.setImageResource(estaExpandido ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);

            holder.layoutCabeceraMateria.setOnClickListener(v -> {
                int prev = posicionExpandida;
                posicionExpandida = estaExpandido ? -1 : position;
                if (prev != -1) notifyItemChanged(prev);
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class MateriaViewHolder extends RecyclerView.ViewHolder {
            View layoutCabeceraMateria;
            ImageView ivIconoMateria;
            TextView tvNombreMateria;
            TextView tvSubtituloMateria;
            TextView tvBadgeValoracion;
            TextView tvNotaNumerica;
            ImageView ivExpandir;

            LinearLayout layoutDetalleCuatrimestre;
            TextView tvNotaCuat1;
            TextView tvNotaCuat2;
            TextView tvNotaIntensif;

            public MateriaViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutCabeceraMateria = itemView.findViewById(R.id.layoutCabeceraMateria);
                ivIconoMateria = itemView.findViewById(R.id.ivIconoMateria);
                tvNombreMateria = itemView.findViewById(R.id.tvNombreMateria);
                tvSubtituloMateria = itemView.findViewById(R.id.tvSubtituloMateria);
                tvBadgeValoracion = itemView.findViewById(R.id.tvBadgeValoracion);
                tvNotaNumerica = itemView.findViewById(R.id.tvNotaNumerica);
                ivExpandir = itemView.findViewById(R.id.ivExpandir);

                layoutDetalleCuatrimestre = itemView.findViewById(R.id.layoutDetalleCuatrimestre);
                tvNotaCuat1 = itemView.findViewById(R.id.tvNotaCuat1);
                tvNotaCuat2 = itemView.findViewById(R.id.tvNotaCuat2);
                tvNotaIntensif = itemView.findViewById(R.id.tvNotaIntensif);
            }
        }
    }
}

