package com.example.boletindigital;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GestionMesasActivity extends AppCompatActivity {

    private ImageView ivVolverMesasGestion;
    private TextView tvBadgeRolMesas;
    private NestedScrollView scrollMesasGestion;

    private Spinner spnFiltroSedeMesas;
    private TextView tvResumenMesasGestion;

    private TextView tvTituloFormMesa;
    private Spinner spnSedeFormMesa;
    private TextInputEditText etMateriaMesa;
    private TextInputEditText etCursoMesa;
    private Spinner spnMesMesa;
    private Spinner spnTurnoMesa;
    private TextInputEditText etDocentesMesa;
    private TextInputEditText etFechaMesa;
    private TextInputEditText etHoraMesa;
    private Spinner spnEstadoMesa;
    private MaterialButton btnGuardarMesa;
    private MaterialButton btnCancelarEdicionMesa;

    private TextView tvCantidadMesasGestion;
    private ProgressBar pbCargandoMesasGestion;
    private TextView tvSinMesasGestion;
    private RecyclerView rvMesasGestion;

    private String token;
    private String rol;
    private String nombre;

    private String mesaEditandoId = "";
    private int mesaEditandoIdDb = 0;

    private final List<JSONObject> todasLasMesas = new ArrayList<>();
    private final List<JSONObject> mesasFiltradas = new ArrayList<>();
    private MesasGestionAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final String[] sedesFiltro = {
            "Todas las sedes",
            "EPES N° 19 (Formosa)",
            "E.E.S. N° 5 (Bs. As.)"
    };

    private final String[] sedesForm = {
            "EPES N° 19 (Formosa)",
            "E.E.S. N° 5 (Bs. As.)"
    };

    private final String[] mesesExamen = {
            "Diciembre",
            "Febrero / Marzo",
            "Julio",
            "Octubre",
            "Marzo"
    };

    private final String[] turnosExamen = {
            "Turno Diciembre 2026",
            "Turno Febrero / Marzo 2027",
            "Mesa Especial de Previas"
    };

    private final String[] estadosMesa = {
            "Abierta (Habilitada)",
            "Cerrada"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_mesas);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        vincularVistas();
        configurarSpinners();
        configurarPickers();
        configurarRecyclerView();

        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombre);
        DocenteUiHelper.aplicarBadgeRol(tvBadgeRolMesas, rol);

        if (savedInstanceState != null) {
            etMateriaMesa.setText(savedInstanceState.getString("materia", ""));
            etCursoMesa.setText(savedInstanceState.getString("curso", "Ciclo Básico"));
            etDocentesMesa.setText(savedInstanceState.getString("docentes", ""));
            etFechaMesa.setText(savedInstanceState.getString("fecha", "2026-12-15"));
            etHoraMesa.setText(savedInstanceState.getString("hora", "08:00"));
            mesaEditandoId = savedInstanceState.getString("editId", "");
            mesaEditandoIdDb = savedInstanceState.getInt("editIdDb", 0);
            actualizarModoEdicionUi();
        }

        ivVolverMesasGestion.setOnClickListener(v -> finish());
        btnGuardarMesa.setOnClickListener(v -> guardarMesa());
        btnCancelarEdicionMesa.setOnClickListener(v -> limpiarFormulario());

        cargarMesasDesdeApi();
    }

    private void vincularVistas() {
        ivVolverMesasGestion = findViewById(R.id.ivVolverMesasGestion);
        tvBadgeRolMesas = findViewById(R.id.tvBadgeRolMesas);
        scrollMesasGestion = findViewById(R.id.scrollMesasGestion);

        spnFiltroSedeMesas = findViewById(R.id.spnFiltroSedeMesas);
        tvResumenMesasGestion = findViewById(R.id.tvResumenMesasGestion);

        tvTituloFormMesa = findViewById(R.id.tvTituloFormMesa);
        spnSedeFormMesa = findViewById(R.id.spnSedeFormMesa);
        etMateriaMesa = findViewById(R.id.etMateriaMesa);
        etCursoMesa = findViewById(R.id.etCursoMesa);
        spnMesMesa = findViewById(R.id.spnMesMesa);
        spnTurnoMesa = findViewById(R.id.spnTurnoMesa);
        etDocentesMesa = findViewById(R.id.etDocentesMesa);
        etFechaMesa = findViewById(R.id.etFechaMesa);
        etHoraMesa = findViewById(R.id.etHoraMesa);
        spnEstadoMesa = findViewById(R.id.spnEstadoMesa);
        btnGuardarMesa = findViewById(R.id.btnGuardarMesa);
        btnCancelarEdicionMesa = findViewById(R.id.btnCancelarEdicionMesa);

        tvCantidadMesasGestion = findViewById(R.id.tvCantidadMesasGestion);
        pbCargandoMesasGestion = findViewById(R.id.pbCargandoMesasGestion);
        tvSinMesasGestion = findViewById(R.id.tvSinMesasGestion);
        rvMesasGestion = findViewById(R.id.rvMesasGestion);
    }

    private void configurarSpinners() {
        DocenteUiHelper.configurarSpinner(this, spnFiltroSedeMesas, sedesFiltro);
        DocenteUiHelper.configurarSpinner(this, spnSedeFormMesa, sedesForm);
        DocenteUiHelper.configurarSpinner(this, spnMesMesa, mesesExamen);
        DocenteUiHelper.configurarSpinner(this, spnTurnoMesa, turnosExamen);
        DocenteUiHelper.configurarSpinner(this, spnEstadoMesa, estadosMesa);

        int idInstActiva = InstitucionManager.getIdInstitucionActiva(this);
        spnFiltroSedeMesas.setSelection(idInstActiva == InstitucionManager.ID_PBA ? 2 : 1, false);
        spnSedeFormMesa.setSelection(idInstActiva == InstitucionManager.ID_PBA ? 1 : 0, false);

        spnFiltroSedeMesas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltroSede();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void configurarPickers() {
        etFechaMesa.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            String actual = textoDe(etFechaMesa);
            if (actual.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] p = actual.split("-");
                cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            }
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> etFechaMesa.setText(
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        etHoraMesa.setOnClickListener(v -> {
            int hora = 8;
            int min = 0;
            String actual = textoDe(etHoraMesa);
            if (actual.matches("\\d{2}:\\d{2}")) {
                String[] p = actual.split(":");
                hora = Integer.parseInt(p[0]);
                min = Integer.parseInt(p[1]);
            }
            new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> etHoraMesa.setText(
                            String.format(Locale.US, "%02d:%02d", hourOfDay, minute)),
                    hora,
                    min,
                    true
            ).show();
        });
    }

    private void configurarRecyclerView() {
        rvMesasGestion.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MesasGestionAdapter(mesasFiltradas);
        rvMesasGestion.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("materia", textoDe(etMateriaMesa));
        outState.putString("curso", textoDe(etCursoMesa));
        outState.putString("docentes", textoDe(etDocentesMesa));
        outState.putString("fecha", textoDe(etFechaMesa));
        outState.putString("hora", textoDe(etHoraMesa));
        outState.putString("editId", mesaEditandoId);
        outState.putInt("editIdDb", mesaEditandoIdDb);
    }

    private void cargarMesasDesdeApi() {
        pbCargandoMesasGestion.setVisibility(View.VISIBLE);
        tvSinMesasGestion.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                JSONArray res = ApiClient.listarMesas(0);
                List<JSONObject> parsed = new ArrayList<>();
                for (int i = 0; i < res.length(); i++) {
                    parsed.add(res.getJSONObject(i));
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoMesasGestion.setVisibility(View.GONE);
                    todasLasMesas.clear();
                    todasLasMesas.addAll(parsed);
                    aplicarFiltroSede();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoMesasGestion.setVisibility(View.GONE);
                    tvSinMesasGestion.setText("No se pudieron cargar las mesas de examen: " + e.getMessage());
                    tvSinMesasGestion.setVisibility(mesasFiltradas.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private void aplicarFiltroSede() {
        int filtroPos = spnFiltroSedeMesas.getSelectedItemPosition();
        mesasFiltradas.clear();
        int totalInscriptos = 0;

        for (JSONObject m : todasLasMesas) {
            int idInst = m.optInt("id_institucion", 1);
            if (filtroPos == 1 && idInst != 1) continue;
            if (filtroPos == 2 && idInst != 2) continue;

            mesasFiltradas.add(m);
            JSONArray insc = m.optJSONArray("inscriptos");
            if (insc != null) totalInscriptos += insc.length();
        }

        adapter.notifyDataSetChanged();
        tvCantidadMesasGestion.setText(mesasFiltradas.size() == 1 ? "1 Mesa" : mesasFiltradas.size() + " Mesas");
        tvResumenMesasGestion.setText(mesasFiltradas.size() + " Mesas • " + totalInscriptos + " Inscriptos");
        tvSinMesasGestion.setText("No hay mesas de examen registradas para esta sede.");
        tvSinMesasGestion.setVisibility(mesasFiltradas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void guardarMesa() {
        String materia = textoDe(etMateriaMesa);
        String curso = textoDe(etCursoMesa);
        String docentes = textoDe(etDocentesMesa);
        String fecha = textoDe(etFechaMesa);
        String hora = textoDe(etHoraMesa);

        if (materia.isEmpty()) {
            etMateriaMesa.setError("Ingresá el nombre de la materia");
            etMateriaMesa.requestFocus();
            return;
        }
        if (curso.isEmpty()) curso = "Ciclo Básico";
        if (docentes.isEmpty()) docentes = "A designar";
        if (fecha.isEmpty()) fecha = "2026-12-15";
        if (hora.isEmpty()) hora = "08:00";

        int idInst = spnSedeFormMesa.getSelectedItemPosition() == 1 ? 2 : 1;
        String mes = mesesExamen[spnMesMesa.getSelectedItemPosition()];
        String turno = turnosExamen[spnTurnoMesa.getSelectedItemPosition()];
        String estado = spnEstadoMesa.getSelectedItemPosition() == 1 ? "cerrada" : "abierta";

        btnGuardarMesa.setEnabled(false);
        btnGuardarMesa.setText("GUARDANDO...");

        String finalCurso = curso;
        String finalDocentes = docentes;
        String finalFecha = fecha;
        String finalHora = hora;

        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("accion", "guardar_mesa");
                if (!mesaEditandoId.isEmpty()) payload.put("id", mesaEditandoId);
                if (mesaEditandoIdDb > 0) payload.put("id_db", mesaEditandoIdDb);
                payload.put("materia", materia);
                payload.put("curso", finalCurso);
                payload.put("mes", mes);
                payload.put("turno", turno);
                payload.put("docentes", finalDocentes);
                payload.put("fecha", finalFecha);
                payload.put("hora", finalHora);
                payload.put("estado", estado);
                payload.put("id_institucion", idInst);

                ApiClient.guardarMesaExamen(payload, token);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarMesa.setEnabled(true);
                    Toast.makeText(this, "Mesa de " + materia + " guardada correctamente", Toast.LENGTH_SHORT).show();
                    limpiarFormulario();
                    cargarMesasDesdeApi();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarMesa.setEnabled(true);
                    actualizarModoEdicionUi();
                    Toast.makeText(this, "Error al guardar mesa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void cargarMesaEnFormulario(JSONObject mesa) {
        mesaEditandoId = mesa.optString("id", "");
        mesaEditandoIdDb = mesa.optInt("id_db", 0);

        spnSedeFormMesa.setSelection(mesa.optInt("id_institucion", 1) == 2 ? 1 : 0);
        etMateriaMesa.setText(mesa.optString("materia", ""));
        etCursoMesa.setText(mesa.optString("curso", "Ciclo Básico"));
        DocenteUiHelper.seleccionarValor(spnMesMesa, mesesExamen, mesa.optString("mes", "Diciembre"));
        DocenteUiHelper.seleccionarValor(spnTurnoMesa, turnosExamen, mesa.optString("turno", "Turno Diciembre 2026"));
        etDocentesMesa.setText(mesa.optString("docentes", "A designar"));
        etFechaMesa.setText(mesa.optString("fecha", "2026-12-15"));
        etHoraMesa.setText(mesa.optString("hora", "08:00"));
        spnEstadoMesa.setSelection("cerrada".equalsIgnoreCase(mesa.optString("estado", "abierta")) ? 1 : 0);

        actualizarModoEdicionUi();
        if (scrollMesasGestion != null) {
            scrollMesasGestion.post(() -> scrollMesasGestion.smoothScrollTo(0, 0));
        }
    }

    private void limpiarFormulario() {
        mesaEditandoId = "";
        mesaEditandoIdDb = 0;
        int idInstActiva = InstitucionManager.getIdInstitucionActiva(this);
        spnSedeFormMesa.setSelection(idInstActiva == InstitucionManager.ID_PBA ? 1 : 0);
        etMateriaMesa.setText("");
        etMateriaMesa.setError(null);
        etCursoMesa.setText("Ciclo Básico");
        etDocentesMesa.setText("");
        etFechaMesa.setText("2026-12-15");
        etHoraMesa.setText("08:00");
        spnEstadoMesa.setSelection(0);
        actualizarModoEdicionUi();
    }

    private void actualizarModoEdicionUi() {
        boolean editando = !mesaEditandoId.isEmpty() || mesaEditandoIdDb > 0;
        tvTituloFormMesa.setText(editando ? "EDITAR MESA DE EXAMEN" : "CARGAR NUEVA MESA DE EXAMEN");
        btnGuardarMesa.setText(editando ? "GUARDAR CAMBIOS" : "GUARDAR MESA DE EXAMEN");
        btnCancelarEdicionMesa.setVisibility(editando ? View.VISIBLE : View.GONE);
    }

    private void confirmarEliminarMesa(JSONObject mesa) {
        String materia = mesa.optString("materia", "Mesa");
        String curso = mesa.optString("curso", "");
        JSONArray insc = mesa.optJSONArray("inscriptos");
        int cant = insc != null ? insc.length() : 0;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Mesa de Examen")
                .setMessage("¿Deseás eliminar la mesa de " + materia + " (" + curso + ")?\n"
                        + (cant > 0 ? "Atención: tiene " + cant + " alumno(s) inscripto(s)." : "No tiene alumnos inscriptos."))
                .setPositiveButton("Sí, Eliminar", (d, w) -> ejecutarEliminarMesa(mesa))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarEliminarMesa(JSONObject mesa) {
        String id = mesa.optString("id", "");
        int idDb = mesa.optInt("id_db", 0);

        executor.execute(() -> {
            try {
                ApiClient.eliminarMesaExamen(id, idDb, token);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Mesa eliminada correctamente", Toast.LENGTH_SHORT).show();
                    if (id.equals(mesaEditandoId)) limpiarFormulario();
                    cargarMesasDesdeApi();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "No se pudo eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void mostrarDialogoInscriptos(JSONObject mesa) {
        String mesaId = mesa.optString("id", "");
        String materia = mesa.optString("materia", "Mesa");
        JSONArray inscriptos = mesa.optJSONArray("inscriptos");

        if (inscriptos == null || inscriptos.length() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Inscriptos: " + materia)
                    .setMessage("Aún no hay alumnos inscriptos en esta mesa de examen.")
                    .setPositiveButton("Cerrar", null)
                    .show();
            return;
        }

        String[] labels = new String[inscriptos.length()];
        String[] dnis = new String[inscriptos.length()];
        String[] nombres = new String[inscriptos.length()];

        for (int i = 0; i < inscriptos.length(); i++) {
            JSONObject alum = inscriptos.optJSONObject(i);
            String dni = alum != null ? alum.optString("dni", "-") : "-";
            String nom = alum != null ? alum.optString("nombre", "Estudiante") : "Estudiante";
            String cur = alum != null ? alum.optString("curso", "-") : "-";
            String cond = alum != null ? alum.optString("condicion", "Regular") : "Regular";

            dnis[i] = dni;
            nombres[i] = nom;
            labels[i] = nom + "\nDNI " + dni + " • " + cur + " • " + cond + " (tocar para dar de baja)";
        }

        new AlertDialog.Builder(this)
                .setTitle("Inscriptos: " + materia + " (" + inscriptos.length() + ")")
                .setItems(labels, (dialog, which) -> confirmarBajaInscripto(mesaId, materia, dnis[which], nombres[which]))
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void confirmarBajaInscripto(String mesaId, String materia, String dniAlum, String nombreAlum) {
        new AlertDialog.Builder(this)
                .setTitle("Dar de Baja Inscripción")
                .setMessage("¿Remover a " + nombreAlum + " (DNI " + dniAlum + ") de la mesa de " + materia + "?")
                .setPositiveButton("Dar de Baja", (d, w) -> executor.execute(() -> {
                    try {
                        ApiClient.bajaInscripcionMesa(mesaId, dniAlum, token);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            Toast.makeText(this, "Inscripción dada de baja", Toast.LENGTH_SHORT).show();
                            cargarMesasDesdeApi();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(this, "Error al dar de baja: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String textoDe(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private class MesasGestionAdapter extends RecyclerView.Adapter<MesasGestionAdapter.MesaViewHolder> {

        private final List<JSONObject> items;

        MesasGestionAdapter(List<JSONObject> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public MesaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mesa_gestion, parent, false);
            return new MesaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MesaViewHolder holder, int position) {
            JSONObject mesa = items.get(position);
            String materia = mesa.optString("materia", "Materia");
            String curso = mesa.optString("curso", "Ciclo Básico");
            int idInst = mesa.optInt("id_institucion", 1);
            String mes = mesa.optString("mes", "Diciembre");
            String turno = mesa.optString("turno", "Turno Diciembre 2026");
            String fecha = DocenteUiHelper.formatearFecha(mesa.optString("fecha", ""));
            String hora = mesa.optString("hora", "08:00");
            String docentes = mesa.optString("docentes", "A designar");
            boolean abierta = !"cerrada".equalsIgnoreCase(mesa.optString("estado", "abierta"));
            JSONArray insc = mesa.optJSONArray("inscriptos");
            int cantInsc = insc != null ? insc.length() : 0;

            holder.tvMateriaMesaItem.setText(materia);
            holder.tvCursoMesaItem.setText(curso);
            holder.tvSedeMesaItem.setText(idInst == 2 ? "E.E.S. N° 5" : "EPES N° 19");
            holder.tvMesTurnoMesaItem.setText("Mesa de " + mes + " • " + turno);
            holder.tvFechaHoraMesaItem.setText(fecha + " • " + hora + " hs");
            holder.tvDocentesMesaItem.setText("Tribunal: " + docentes);
            holder.btnVerInscriptosMesa.setText("Inscriptos (" + cantInsc + ")");

            if (abierta) {
                holder.vFranjaEstadoMesa.setBackgroundColor(ContextCompat.getColor(GestionMesasActivity.this, R.color.color_warning));
                holder.tvEstadoMesaItem.setText("Abierta");
                holder.tvEstadoMesaItem.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GestionMesasActivity.this, R.color.color_tea_bg)));
                holder.tvEstadoMesaItem.setTextColor(ContextCompat.getColor(GestionMesasActivity.this, R.color.color_tea_text));
            } else {
                holder.vFranjaEstadoMesa.setBackgroundColor(ContextCompat.getColor(GestionMesasActivity.this, R.color.text_dim));
                holder.tvEstadoMesaItem.setText("Cerrada");
                holder.tvEstadoMesaItem.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(GestionMesasActivity.this, R.color.bg_surface)));
                holder.tvEstadoMesaItem.setTextColor(ContextCompat.getColor(GestionMesasActivity.this, R.color.text_muted));
            }

            holder.btnVerInscriptosMesa.setOnClickListener(v -> mostrarDialogoInscriptos(mesa));
            holder.btnEditarMesa.setOnClickListener(v -> cargarMesaEnFormulario(mesa));
            holder.btnEliminarMesa.setOnClickListener(v -> confirmarEliminarMesa(mesa));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class MesaViewHolder extends RecyclerView.ViewHolder {
            View vFranjaEstadoMesa;
            TextView tvMateriaMesaItem;
            TextView tvEstadoMesaItem;
            TextView tvCursoMesaItem;
            TextView tvSedeMesaItem;
            TextView tvMesTurnoMesaItem;
            TextView tvFechaHoraMesaItem;
            TextView tvDocentesMesaItem;
            TextView btnVerInscriptosMesa;
            ImageView btnEditarMesa;
            ImageView btnEliminarMesa;

            MesaViewHolder(@NonNull View itemView) {
                super(itemView);
                vFranjaEstadoMesa = itemView.findViewById(R.id.vFranjaEstadoMesa);
                tvMateriaMesaItem = itemView.findViewById(R.id.tvMateriaMesaItem);
                tvEstadoMesaItem = itemView.findViewById(R.id.tvEstadoMesaItem);
                tvCursoMesaItem = itemView.findViewById(R.id.tvCursoMesaItem);
                tvSedeMesaItem = itemView.findViewById(R.id.tvSedeMesaItem);
                tvMesTurnoMesaItem = itemView.findViewById(R.id.tvMesTurnoMesaItem);
                tvFechaHoraMesaItem = itemView.findViewById(R.id.tvFechaHoraMesaItem);
                tvDocentesMesaItem = itemView.findViewById(R.id.tvDocentesMesaItem);
                btnVerInscriptosMesa = itemView.findViewById(R.id.btnVerInscriptosMesa);
                btnEditarMesa = itemView.findViewById(R.id.btnEditarMesa);
                btnEliminarMesa = itemView.findViewById(R.id.btnEliminarMesa);
            }
        }
    }
}

