package com.example.boletindigital;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestionPreviasActivity extends AppCompatActivity {

    private ImageView ivVolverPreviasGestion;
    private TextView tvBadgeRolPrevias;
    private NestedScrollView scrollPreviasGestion;

    private TextInputEditText etBuscarAlumnoPrevia;
    private Spinner spnAlumnoPrevia;
    private TextView tvInfoAlumnoPrevia;

    private TextView tvTituloFormPrevia;
    private TextInputEditText etMateriaPrevia;
    private Spinner spnCursoOrigenPrevia;
    private Spinner spnEstadoPrevia;
    private TextInputEditText etCalificacionPrevia;
    private TextView tvLabelFechaPrevia;
    private TextInputEditText etFechaPrevia;
    private MaterialButton btnGuardarPrevia;
    private MaterialButton btnCancelarEdicionPrevia;

    private TextView tvCantidadPreviasGestion;
    private ProgressBar pbCargandoPreviasGestion;
    private TextView tvSinPreviasGestion;
    private RecyclerView rvPreviasGestion;

    private String token;
    private String rol;
    private String nombre;

    private int previaEditandoId = 0;

    private static class AlumnoOpcion {
        String dni;
        String nombreCompleto;
        String curso;
        int idInstitucion;

        AlumnoOpcion(String dni, String nombreCompleto, String curso, int idInstitucion) {
            this.dni = dni;
            this.nombreCompleto = nombreCompleto;
            this.curso = curso;
            this.idInstitucion = idInstitucion;
        }

        @NonNull
        @Override
        public String toString() {
            return nombreCompleto + " (" + dni + " • " + curso + ")";
        }
    }

    private final List<AlumnoOpcion> todosLosAlumnos = new ArrayList<>();
    private final List<AlumnoOpcion> alumnosFiltrados = new ArrayList<>();
    private ArrayAdapter<AlumnoOpcion> adapterAlumnos;

    private final List<JSONObject> listaPrevias = new ArrayList<>();
    private PreviasGestionAdapter adapterPrevias;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final String[] cursosOrigen = {
            "1° Año", "2° Año", "3° Año", "4° Año", "5° Año", "6° Año"
    };

    private final String[] estadosPrevia = {
            "Pendiente", "En Curso", "Aprobada"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_previas);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        vincularVistas();
        configurarControles();
        configurarRecyclerView();

        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombre);
        DocenteUiHelper.aplicarBadgeRol(tvBadgeRolPrevias, rol);

        if (savedInstanceState != null) {
            etMateriaPrevia.setText(savedInstanceState.getString("materia", ""));
            etCalificacionPrevia.setText(savedInstanceState.getString("nota", ""));
            etFechaPrevia.setText(savedInstanceState.getString("fecha", ""));
            previaEditandoId = savedInstanceState.getInt("editId", 0);
            actualizarModoEdicionUi();
        }

        ivVolverPreviasGestion.setOnClickListener(v -> finish());
        btnGuardarPrevia.setOnClickListener(v -> guardarPrevia());
        btnCancelarEdicionPrevia.setOnClickListener(v -> limpiarFormulario());

        cargarAlumnosDesdeApi();
    }

    private void vincularVistas() {
        ivVolverPreviasGestion = findViewById(R.id.ivVolverPreviasGestion);
        tvBadgeRolPrevias = findViewById(R.id.tvBadgeRolPrevias);
        scrollPreviasGestion = findViewById(R.id.scrollPreviasGestion);

        etBuscarAlumnoPrevia = findViewById(R.id.etBuscarAlumnoPrevia);
        spnAlumnoPrevia = findViewById(R.id.spnAlumnoPrevia);
        tvInfoAlumnoPrevia = findViewById(R.id.tvInfoAlumnoPrevia);

        tvTituloFormPrevia = findViewById(R.id.tvTituloFormPrevia);
        etMateriaPrevia = findViewById(R.id.etMateriaPrevia);
        spnCursoOrigenPrevia = findViewById(R.id.spnCursoOrigenPrevia);
        spnEstadoPrevia = findViewById(R.id.spnEstadoPrevia);
        etCalificacionPrevia = findViewById(R.id.etCalificacionPrevia);
        tvLabelFechaPrevia = findViewById(R.id.tvLabelFechaPrevia);
        etFechaPrevia = findViewById(R.id.etFechaPrevia);
        btnGuardarPrevia = findViewById(R.id.btnGuardarPrevia);
        btnCancelarEdicionPrevia = findViewById(R.id.btnCancelarEdicionPrevia);

        tvCantidadPreviasGestion = findViewById(R.id.tvCantidadPreviasGestion);
        pbCargandoPreviasGestion = findViewById(R.id.pbCargandoPreviasGestion);
        tvSinPreviasGestion = findViewById(R.id.tvSinPreviasGestion);
        rvPreviasGestion = findViewById(R.id.rvPreviasGestion);
    }

    private void configurarControles() {
        DocenteUiHelper.configurarSpinner(this, spnCursoOrigenPrevia, cursosOrigen);
        DocenteUiHelper.configurarSpinner(this, spnEstadoPrevia, estadosPrevia);

        DocenteUiHelper.estilizarSpinnerWeb(spnAlumnoPrevia);
        adapterAlumnos = DocenteUiHelper.crearAdapterSpinner(
                this,
                spnAlumnoPrevia,
                alumnosFiltrados
        );
        spnAlumnoPrevia.setAdapter(adapterAlumnos);

        spnAlumnoPrevia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AlumnoOpcion alumno = obtenerAlumnoSeleccionado();
                if (alumno != null) {
                    String sede = alumno.idInstitucion == 2 ? "E.E.S. N° 5 (Bs. As.)" : "EPES N° 19 (Formosa)";
                    tvInfoAlumnoPrevia.setText("DNI " + alumno.dni + " • " + alumno.curso + " • " + sede);
                    cargarPreviasDelAlumno(alumno.dni);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spnEstadoPrevia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String estado = estadosPrevia[position];
                boolean aprobada = "Aprobada".equalsIgnoreCase(estado);
                tvLabelFechaPrevia.setText(aprobada ? "Fecha de Aprobación" : "Turno / Fecha Estimada");
                if (aprobada && textoDe(etFechaPrevia).isEmpty()) {
                    etFechaPrevia.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        etBuscarAlumnoPrevia.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarAlumnos(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarRecyclerView() {
        rvPreviasGestion.setLayoutManager(new LinearLayoutManager(this));
        adapterPrevias = new PreviasGestionAdapter(listaPrevias);
        rvPreviasGestion.setAdapter(adapterPrevias);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("materia", textoDe(etMateriaPrevia));
        outState.putString("nota", textoDe(etCalificacionPrevia));
        outState.putString("fecha", textoDe(etFechaPrevia));
        outState.putInt("editId", previaEditandoId);
    }

    private void cargarAlumnosDesdeApi() {
        final int idInstActiva = InstitucionManager.getIdInstitucionActiva(this);
        executor.execute(() -> {
            try {
                JSONArray res = ApiClient.listarAlumnos(idInstActiva);
                List<AlumnoOpcion> parsed = new ArrayList<>();
                for (int i = 0; i < res.length(); i++) {
                    JSONObject obj = res.getJSONObject(i);
                    String dni = obj.optString("dni", "");
                    if (dni.isEmpty()) continue;
                    String apellido = obj.optString("apellido", "").trim();
                    String nom = obj.optString("nombre", "").trim();
                    String nombreComp = (!apellido.isEmpty() && !nom.isEmpty())
                            ? apellido + ", " + nom
                            : (apellido + nom).trim();
                    String curso = (obj.optString("curso", "") + " " + obj.optString("seccion", "")).trim();
                    int idInst = obj.optInt("id_institucion", idInstActiva);
                    parsed.add(new AlumnoOpcion(dni, nombreComp, curso, idInst));
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    todosLosAlumnos.clear();
                    todosLosAlumnos.addAll(parsed);
                    filtrarAlumnos(textoDe(etBuscarAlumnoPrevia));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvInfoAlumnoPrevia.setText("Error al cargar alumnos: " + e.getMessage());
                });
            }
        });
    }

    private void filtrarAlumnos(String consulta) {
        String q = consulta.trim().toLowerCase(Locale.ROOT);
        alumnosFiltrados.clear();
        for (AlumnoOpcion a : todosLosAlumnos) {
            if (q.isEmpty()
                    || a.nombreCompleto.toLowerCase(Locale.ROOT).contains(q)
                    || a.dni.contains(q)
                    || a.curso.toLowerCase(Locale.ROOT).contains(q)) {
                alumnosFiltrados.add(a);
            }
        }
        adapterAlumnos.notifyDataSetChanged();

        AlumnoOpcion actual = obtenerAlumnoSeleccionado();
        if (actual != null) {
            String sede = actual.idInstitucion == 2 ? "E.E.S. N° 5 (Bs. As.)" : "EPES N° 19 (Formosa)";
            tvInfoAlumnoPrevia.setText("DNI " + actual.dni + " • " + actual.curso + " • " + sede);
            cargarPreviasDelAlumno(actual.dni);
        } else {
            tvInfoAlumnoPrevia.setText("No hay estudiantes que coincidan con el filtro");
            listaPrevias.clear();
            adapterPrevias.notifyDataSetChanged();
            tvCantidadPreviasGestion.setText("0 Materias");
        }
    }

    private AlumnoOpcion obtenerAlumnoSeleccionado() {
        int pos = spnAlumnoPrevia.getSelectedItemPosition();
        if (pos >= 0 && pos < alumnosFiltrados.size()) {
            return alumnosFiltrados.get(pos);
        }
        return null;
    }

    private void cargarPreviasDelAlumno(String dni) {
        if (dni == null || dni.isEmpty()) return;
        pbCargandoPreviasGestion.setVisibility(View.VISIBLE);
        tvSinPreviasGestion.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                JSONArray res = ApiClient.listarPrevias(dni);
                List<JSONObject> parsed = new ArrayList<>();
                for (int i = 0; i < res.length(); i++) {
                    parsed.add(res.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoPreviasGestion.setVisibility(View.GONE);
                    listaPrevias.clear();
                    listaPrevias.addAll(parsed);
                    adapterPrevias.notifyDataSetChanged();
                    tvCantidadPreviasGestion.setText(
                            listaPrevias.size() == 1 ? "1 Materia" : listaPrevias.size() + " Materias");
                    tvSinPreviasGestion.setVisibility(listaPrevias.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoPreviasGestion.setVisibility(View.GONE);
                    tvSinPreviasGestion.setText("No se pudieron cargar las previas: " + e.getMessage());
                    tvSinPreviasGestion.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void guardarPrevia() {
        AlumnoOpcion alumno = obtenerAlumnoSeleccionado();
        if (alumno == null) {
            Toast.makeText(this, "Seleccioná primero un estudiante", Toast.LENGTH_SHORT).show();
            return;
        }

        String materia = textoDe(etMateriaPrevia);
        if (materia.isEmpty()) {
            etMateriaPrevia.setError("Ingresá el nombre de la materia");
            etMateriaPrevia.requestFocus();
            return;
        }

        String cursoOrigen = cursosOrigen[spnCursoOrigenPrevia.getSelectedItemPosition()];
        String estado = estadosPrevia[spnEstadoPrevia.getSelectedItemPosition()];
        String nota = textoDe(etCalificacionPrevia);
        String fecha = textoDe(etFechaPrevia);

        boolean aprobada = "Aprobada".equalsIgnoreCase(estado);
        String fechaAprobacion = aprobada
                ? (fecha.isEmpty()
                ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())
                : fecha)
                : "";

        btnGuardarPrevia.setEnabled(false);
        btnGuardarPrevia.setText("GUARDANDO...");

        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("dni_estudiante", alumno.dni);
                payload.put("materia", materia);
                payload.put("calificacion", nota.isEmpty() ? JSONObject.NULL : nota);
                payload.put("estado", estado);
                payload.put("curso_origen", cursoOrigen);
                payload.put("fecha", fecha.isEmpty() ? cursoOrigen : fecha);
                payload.put("fecha_aprobacion", aprobada ? fechaAprobacion : JSONObject.NULL);
                payload.put("id_institucion", alumno.idInstitucion);

                if (previaEditandoId > 0) {
                    payload.put("id", previaEditandoId);
                    ApiClient.actualizarPrevia(payload, token);
                } else {
                    ApiClient.crearPrevia(payload, token);
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarPrevia.setEnabled(true);
                    Toast.makeText(this, "Materia previa guardada para " + alumno.nombreCompleto, Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                    cargarPreviasDelAlumno(alumno.dni);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarPrevia.setEnabled(true);
                    actualizarModoEdicionUi();
                    Toast.makeText(this, "Error al guardar previa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void cargarPreviaEnFormulario(JSONObject previa) {
        previaEditandoId = previa.optInt("id", 0);
        etMateriaPrevia.setText(previa.optString("materia", ""));
        DocenteUiHelper.seleccionarValor(spnCursoOrigenPrevia, cursosOrigen, previa.optString("curso_origen", "1° Año"));
        DocenteUiHelper.seleccionarValor(spnEstadoPrevia, estadosPrevia, previa.optString("estado", "Pendiente"));

        String nota = previa.isNull("calificacion") ? "" : previa.optString("calificacion", "");
        etCalificacionPrevia.setText("null".equalsIgnoreCase(nota) ? "" : nota);

        String fechaAprob = previa.isNull("fecha_aprobacion") ? "" : previa.optString("fecha_aprobacion", "");
        String fecha = previa.isNull("fecha") ? "" : previa.optString("fecha", "");
        etFechaPrevia.setText(!fechaAprob.isEmpty() && !"null".equalsIgnoreCase(fechaAprob) ? fechaAprob : fecha);

        actualizarModoEdicionUi();
        if (scrollPreviasGestion != null) {
            scrollPreviasGestion.post(() -> scrollPreviasGestion.smoothScrollTo(0, 0));
        }
    }

    private void limpiarFormulario() {
        previaEditandoId = 0;
        etMateriaPrevia.setText("");
        etMateriaPrevia.setError(null);
        spnCursoOrigenPrevia.setSelection(0);
        spnEstadoPrevia.setSelection(0);
        etCalificacionPrevia.setText("");
        etFechaPrevia.setText("");
        actualizarModoEdicionUi();
    }

    private void actualizarModoEdicionUi() {
        boolean editando = previaEditandoId > 0;
        tvTituloFormPrevia.setText(editando ? "EDITAR MATERIA PREVIA" : "CARGAR MATERIA PREVIA");
        btnGuardarPrevia.setText(editando ? "ACTUALIZAR PREVIA" : "GUARDAR MATERIA PREVIA");
        btnCancelarEdicionPrevia.setVisibility(editando ? View.VISIBLE : View.GONE);
    }

    private void confirmarEliminarPrevia(JSONObject previa) {
        int id = previa.optInt("id", 0);
        String materia = previa.optString("materia", "Materia previa");

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Materia Previa")
                .setMessage("¿Deseás eliminar la materia previa \"" + materia + "\" del legajo del alumno?")
                .setPositiveButton("Sí, Eliminar", (d, w) -> ejecutarEliminarPrevia(id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarEliminarPrevia(int idPrevia) {
        executor.execute(() -> {
            try {
                ApiClient.eliminarPrevia(idPrevia, token);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Materia previa eliminada", Toast.LENGTH_SHORT).show();
                    if (idPrevia == previaEditandoId) limpiarFormulario();
                    AlumnoOpcion alumno = obtenerAlumnoSeleccionado();
                    if (alumno != null) cargarPreviasDelAlumno(alumno.dni);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudo eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String textoDe(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private class PreviasGestionAdapter extends RecyclerView.Adapter<PreviasGestionAdapter.PreviaViewHolder> {

        private final List<JSONObject> items;

        PreviasGestionAdapter(List<JSONObject> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PreviaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_previa_gestion, parent, false);
            return new PreviaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PreviaViewHolder holder, int position) {
            JSONObject p = items.get(position);
            String materia = p.optString("materia", "Materia");
            String estado = p.optString("estado", "Pendiente");
            String cursoOrigen = p.optString("curso_origen", "1° Año");
            String nota = p.isNull("calificacion") ? "" : p.optString("calificacion", "");
            String fecha = p.isNull("fecha") ? "" : p.optString("fecha", "");
            String fechaAprob = p.isNull("fecha_aprobacion") ? "" : p.optString("fecha_aprobacion", "");

            holder.tvMateriaPreviaItem.setText(materia);
            holder.tvEstadoPreviaItem.setText(estado);
            holder.tvCursoOrigenPreviaItem.setText("Curso Origen: " + cursoOrigen);
            holder.tvNotaPreviaItem.setText("Nota: " + (nota.isEmpty() || "null".equalsIgnoreCase(nota) ? "-" : nota));

            boolean aprobada = "Aprobada".equalsIgnoreCase(estado);
            boolean enCurso = estado.toLowerCase(Locale.ROOT).contains("curso");

            if (aprobada) {
                holder.vFranjaEstadoPrevia.setBackgroundColor(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_success));
                holder.tvEstadoPreviaItem.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_tea_bg)));
                holder.tvEstadoPreviaItem.setTextColor(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_tea_text));
                String detalleFecha = !fechaAprob.isEmpty() && !"null".equalsIgnoreCase(fechaAprob) ? fechaAprob : fecha;
                holder.tvFechaPreviaItem.setText("Aprobación: " + (detalleFecha.isEmpty() ? "Registrada" : detalleFecha));
            } else if (enCurso) {
                holder.vFranjaEstadoPrevia.setBackgroundColor(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_primary));
                holder.tvEstadoPreviaItem.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_badge_blue_bg)));
                holder.tvEstadoPreviaItem.setTextColor(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_primary));
                holder.tvFechaPreviaItem.setText("Turno / Período: " + (fecha.isEmpty() ? "En intensificación" : fecha));
            } else {
                holder.vFranjaEstadoPrevia.setBackgroundColor(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_danger));
                holder.tvEstadoPreviaItem.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_tep_bg)));
                holder.tvEstadoPreviaItem.setTextColor(ContextCompat.getColor(GestionPreviasActivity.this, R.color.color_tep_text));
                holder.tvFechaPreviaItem.setText("Turno: " + (fecha.isEmpty() ? "A rendir" : fecha));
            }

            holder.btnEditarPreviaItem.setOnClickListener(v -> cargarPreviaEnFormulario(p));
            holder.btnEliminarPreviaItem.setOnClickListener(v -> confirmarEliminarPrevia(p));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class PreviaViewHolder extends RecyclerView.ViewHolder {
            View vFranjaEstadoPrevia;
            TextView tvMateriaPreviaItem;
            TextView tvEstadoPreviaItem;
            TextView tvCursoOrigenPreviaItem;
            TextView tvNotaPreviaItem;
            TextView tvFechaPreviaItem;
            ImageView btnEditarPreviaItem;
            ImageView btnEliminarPreviaItem;

            PreviaViewHolder(@NonNull View itemView) {
                super(itemView);
                vFranjaEstadoPrevia = itemView.findViewById(R.id.vFranjaEstadoPrevia);
                tvMateriaPreviaItem = itemView.findViewById(R.id.tvMateriaPreviaItem);
                tvEstadoPreviaItem = itemView.findViewById(R.id.tvEstadoPreviaItem);
                tvCursoOrigenPreviaItem = itemView.findViewById(R.id.tvCursoOrigenPreviaItem);
                tvNotaPreviaItem = itemView.findViewById(R.id.tvNotaPreviaItem);
                tvFechaPreviaItem = itemView.findViewById(R.id.tvFechaPreviaItem);
                btnEditarPreviaItem = itemView.findViewById(R.id.btnEditarPreviaItem);
                btnEliminarPreviaItem = itemView.findViewById(R.id.btnEliminarPreviaItem);
            }
        }
    }
}

