package com.example.boletindigital;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CargarNotasActivity extends AppCompatActivity {

    private ImageView ivVolver;
    private Spinner spnCurso;
    private Spinner spnMateria;
    private Spinner spnPeriodo;
    private TextView tvTituloPlanilla;
    private TextView tvCantidadAlumnos;
    private ProgressBar pbCargandoPlanilla;
    private RecyclerView rvAlumnosNotas;
    private MaterialButton btnGuardarNotas;

    private String token;
    private String rol;
    private String nombre;

    private final List<AlumnoItemCarga> listaAlumnos = new ArrayList<>();
    private AlumnoCargaAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Model interno para retener el estado de ediciÃ³n por alumno
    public static class AlumnoItemCarga implements Serializable {
        String dni;
        String nombreCompleto;
        String inicial;
        String notaNumerica = "";
        String valoracionConceptual = ""; // "TEA", "TEP", "TED" o vacÃ­o
        String observacion = "";

        public AlumnoItemCarga(String dni, String nombreCompleto) {
            this.dni = dni;
            this.nombreCompleto = nombreCompleto;
            this.inicial = (nombreCompleto != null && !nombreCompleto.isEmpty())
                    ? String.valueOf(nombreCompleto.charAt(0)).toUpperCase()
                    : "A";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cargar_notas);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        vincularVistas();
        configurarSpinners();
        configurarRecyclerView();

        if (savedInstanceState != null) {
            restaurarEstado(savedInstanceState);
        } else {
            cargarAlumnosDesdeApi();
        }

        btnGuardarNotas.setOnClickListener(v -> guardarCalificaciones());
        ivVolver.setOnClickListener(v -> finish());
    }

    private void vincularVistas() {
        ivVolver = findViewById(R.id.ivVolver);
        spnCurso = findViewById(R.id.spnCurso);
        spnMateria = findViewById(R.id.spnMateria);
        spnPeriodo = findViewById(R.id.spnPeriodo);
        tvTituloPlanilla = findViewById(R.id.tvTituloPlanilla);
        tvCantidadAlumnos = findViewById(R.id.tvCantidadAlumnos);
        pbCargandoPlanilla = findViewById(R.id.pbCargandoPlanilla);
        rvAlumnosNotas = findViewById(R.id.rvAlumnosNotas);
        btnGuardarNotas = findViewById(R.id.btnGuardarNotas);
    }

    private void configurarSpinners() {
        // Cursos oficiales E.E.S. NÂ° 5
        String[] cursos = {
                "4Â° Ãšnica (Secundario Orientado)",
                "1Â° I (Ciclo BÃ¡sico)",
                "2Â° I (Ciclo BÃ¡sico)",
                "3Â° I (Ciclo BÃ¡sico)",
                "5Â° I (Secundario Orientado)",
                "6Â° I (Secundario Orientado)"
        };

        // Materias de la currÃ­cula oficial
        String[] materias = {
                "MatemÃ¡tica",
                "Literatura",
                "Historia",
                "GeografÃ­a",
                "BiologÃ­a",
                "IntroducciÃ³n a la FÃ­sica",
                "EducaciÃ³n FÃ­sica",
                "InglÃ©s",
                "NTICx",
                "PsicologÃ­a",
                "ConstrucciÃ³n de CiudadanÃ­a"
        };

        // PerÃ­odos de evaluaciÃ³n R.I.T.E.
        String[] periodos = {
                "1Â° Cuatrimestre",
                "2Â° Cuatrimestre",
                "RITE Diciembre",
                "RITE Febrero"
        };

        ArrayAdapter<String> adapterCurso = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cursos);
        spnCurso.setAdapter(adapterCurso);

        ArrayAdapter<String> adapterMateria = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, materias);
        spnMateria.setAdapter(adapterMateria);

        ArrayAdapter<String> adapterPeriodo = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, periodos);
        spnPeriodo.setAdapter(adapterPeriodo);

        AdapterView.OnItemSelectedListener selectorListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarTituloPlanilla();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        spnCurso.setOnItemSelectedListener(selectorListener);
        spnMateria.setOnItemSelectedListener(selectorListener);
        spnPeriodo.setOnItemSelectedListener(selectorListener);
    }

    private void actualizarTituloPlanilla() {
        String materia = spnMateria.getSelectedItem() != null ? spnMateria.getSelectedItem().toString() : "Materia";
        String periodo = spnPeriodo.getSelectedItem() != null ? spnPeriodo.getSelectedItem().toString() : "1Â° Cuatrimestre";
        tvTituloPlanilla.setText("Planilla: " + materia + " (" + periodo + ")");
    }

    private void configurarRecyclerView() {
        adapter = new AlumnoCargaAdapter(listaAlumnos);
        rvAlumnosNotas.setLayoutManager(new LinearLayoutManager(this));
        rvAlumnosNotas.setAdapter(adapter);
    }

    private void cargarAlumnosDesdeApi() {
        pbCargandoPlanilla.setVisibility(View.VISIBLE);
        tvCantidadAlumnos.setText("Cargando lista...");

        executor.execute(() -> {
            List<AlumnoItemCarga> cargados = new ArrayList<>();
            try {
                JSONArray res = ApiClient.listarAlumnos();
                for (int i = 0; i < res.length(); i++) {
                    JSONObject obj = res.getJSONObject(i);
                    String dni = obj.optString("dni");
                    String nombre = obj.optString("nombre");
                    String apellido = obj.optString("apellido");
                    String nombreCompleto = (apellido + ", " + nombre).trim();
                    cargados.add(new AlumnoItemCarga(dni, nombreCompleto));
                }
            } catch (Exception e) {
                // Fallback institucional en caso de pruebas offline o API vacÃ­a
                cargados.add(new AlumnoItemCarga("48763736", "PLEININGER, URIEL LUCIANO"));
                cargados.add(new AlumnoItemCarga("48111222", "RODRÃ GUER, CAMILA"));
                cargados.add(new AlumnoItemCarga("47888999", "MONLEÃ“N, LUCRECIA"));
                cargados.add(new AlumnoItemCarga("46555444", "ALMAZÃ N, ALEJANDRO"));
                cargados.add(new AlumnoItemCarga("45333222", "GÃ“MEZ, JULIÃ N"));
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                pbCargandoPlanilla.setVisibility(View.GONE);
                listaAlumnos.clear();
                listaAlumnos.addAll(cargados);
                adapter.notifyDataSetChanged();
                tvCantidadAlumnos.setText(listaAlumnos.size() + " Alumnos");
                actualizarTituloPlanilla();
            });
        });
    }

    private void guardarCalificaciones() {
        // Validar y armar JSON
        JSONArray listaJson = new JSONArray();
        String materia = spnMateria.getSelectedItem() != null ? spnMateria.getSelectedItem().toString() : "MatemÃ¡tica";
        String periodo = spnPeriodo.getSelectedItem() != null ? spnPeriodo.getSelectedItem().toString() : "1Â° Cuatrimestre";

        int totalConNota = 0;
        for (AlumnoItemCarga alumno : listaAlumnos) {
            String notaStr = alumno.notaNumerica.trim();
            if (!notaStr.isEmpty() || !alumno.valoracionConceptual.isEmpty()) {
                totalConNota++;
                JSONObject item = new JSONObject();
                try {
                    item.put("dni_estudiante", alumno.dni);
                    item.put("materia", materia);
                    item.put("periodo", periodo);

                    if (!notaStr.isEmpty()) {
                        int num = Integer.parseInt(notaStr);
                        if (num < 1 || num > 10) {
                            Toast.makeText(this, "Nota invÃ¡lida para " + alumno.nombreCompleto + " (Debe ser de 1 a 10)", Toast.LENGTH_LONG).show();
                            return;
                        }
                        item.put("nota_numerica", num);
                    } else {
                        item.put("nota_numerica", JSONObject.NULL);
                    }

                    item.put("valoracion_conceptual", alumno.valoracionConceptual.isEmpty() ? JSONObject.NULL : alumno.valoracionConceptual);
                    item.put("observacion", alumno.observacion.isEmpty() ? JSONObject.NULL : alumno.observacion);
                    listaJson.put(item);
                } catch (Exception ignored) {
                }
            }
        }

        if (totalConNota == 0) {
            Toast.makeText(this, "IngresÃ¡ al menos una calificaciÃ³n o valoraciÃ³n para guardar la planilla", Toast.LENGTH_SHORT).show();
            return;
        }

        // Doble envÃ­o protegido: deshabilitar botÃ³n mientras guarda
        btnGuardarNotas.setEnabled(false);
        btnGuardarNotas.setText("GUARDANDO CALIFICACIONES...");
        pbCargandoPlanilla.setVisibility(View.VISIBLE);

        final int notasAEnviar = totalConNota;
        executor.execute(() -> {
            try {
                JSONObject respuesta = ApiClient.guardarNotas(listaJson, token);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarNotas.setEnabled(true);
                    btnGuardarNotas.setText("GUARDAR CALIFICACIONES DE LA MATERIA");
                    pbCargandoPlanilla.setVisibility(View.GONE);

                    String msg = respuesta.optString("mensaje", "Se guardaron " + notasAEnviar + " calificaciones correctamente en el servidor.");
                    new AlertDialog.Builder(this)
                            .setTitle("Calificaciones Guardadas")
                            .setMessage("Materia: " + materia + "\nPerÃ­odo: " + periodo + "\nRegistros guardados: " + notasAEnviar + "\n\n" + msg)
                            .setPositiveButton("Aceptar", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarNotas.setEnabled(true);
                    btnGuardarNotas.setText("GUARDAR CALIFICACIONES DE LA MATERIA");
                    pbCargandoPlanilla.setVisibility(View.GONE);

                    String errorMsg = e.getMessage() != null ? e.getMessage() : "Error al guardar calificaciones";
                    new AlertDialog.Builder(this)
                            .setTitle("Error de Guardado")
                            .setMessage("No se pudieron registrar las calificaciones en la nube de Railway:\n" + errorMsg)
                            .setPositiveButton("Cerrar", null)
                            .show();
                });
            }
        });
    }

    // Persistencia por rotaciÃ³n de pantalla (Requerimiento R6 CÃ¡tedra)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pos_curso", spnCurso.getSelectedItemPosition());
        outState.putInt("pos_materia", spnMateria.getSelectedItemPosition());
        outState.putInt("pos_periodo", spnPeriodo.getSelectedItemPosition());
        outState.putSerializable("alumnos_editados", new ArrayList<>(listaAlumnos));
    }

    @SuppressWarnings("unchecked")
    private void restaurarEstado(Bundle savedInstanceState) {
        int posCurso = savedInstanceState.getInt("pos_curso", 0);
        int posMateria = savedInstanceState.getInt("pos_materia", 0);
        int posPeriodo = savedInstanceState.getInt("pos_periodo", 0);

        spnCurso.setSelection(posCurso);
        spnMateria.setSelection(posMateria);
        spnPeriodo.setSelection(posPeriodo);

        ArrayList<AlumnoItemCarga> recuperados = (ArrayList<AlumnoItemCarga>) savedInstanceState.getSerializable("alumnos_editados");
        if (recuperados != null && !recuperados.isEmpty()) {
            listaAlumnos.clear();
            listaAlumnos.addAll(recuperados);
            adapter.notifyDataSetChanged();
            tvCantidadAlumnos.setText(listaAlumnos.size() + " Alumnos");
            actualizarTituloPlanilla();
        } else {
            cargarAlumnosDesdeApi();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // Adaptador de RecyclerView para Alumnos en Planilla
    public static class AlumnoCargaAdapter extends RecyclerView.Adapter<AlumnoCargaAdapter.ViewHolder> {

        private final List<AlumnoItemCarga> datos;

        public AlumnoCargaAdapter(List<AlumnoItemCarga> datos) {
            this.datos = datos;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alumno_carga_nota, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AlumnoItemCarga item = datos.get(position);
            holder.vincular(item);
        }

        @Override
        public int getItemCount() {
            return datos.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvInicialAlumno;
            TextView tvNombreAlumno;
            TextView tvDniAlumno;
            TextInputEditText etNotaNumerica;
            TextView btnChipTea;
            TextView btnChipTep;
            TextView btnChipTed;
            TextInputEditText etObservacion;

            TextWatcher watcherNota;
            TextWatcher watcherObs;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInicialAlumno = itemView.findViewById(R.id.tvInicialAlumno);
                tvNombreAlumno = itemView.findViewById(R.id.tvNombreAlumno);
                tvDniAlumno = itemView.findViewById(R.id.tvDniAlumno);
                etNotaNumerica = itemView.findViewById(R.id.etNotaNumerica);
                btnChipTea = itemView.findViewById(R.id.btnChipTea);
                btnChipTep = itemView.findViewById(R.id.btnChipTep);
                btnChipTed = itemView.findViewById(R.id.btnChipTed);
                etObservacion = itemView.findViewById(R.id.etObservacion);
            }

            public void vincular(AlumnoItemCarga alumno) {
                tvInicialAlumno.setText(alumno.inicial);
                tvNombreAlumno.setText(alumno.nombreCompleto);
                tvDniAlumno.setText("DNI: " + alumno.dni);

                // Remover watchers anteriores antes de settear textos reciclados
                if (watcherNota != null) etNotaNumerica.removeTextChangedListener(watcherNota);
                if (watcherObs != null) etObservacion.removeTextChangedListener(watcherObs);

                etNotaNumerica.setText(alumno.notaNumerica);
                etObservacion.setText(alumno.observacion);

                actualizarEstadoChips(alumno.valoracionConceptual);

                // Auto-cÃ¡lculo de TEA / TEP / TED segÃºn nota numÃ©rica
                watcherNota = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String txt = s.toString().trim();
                        alumno.notaNumerica = txt;
                        if (!txt.isEmpty()) {
                            try {
                                int val = Integer.parseInt(txt);
                                if (val >= 7 && val <= 10) {
                                    alumno.valoracionConceptual = "TEA";
                                } else if (val >= 4 && val <= 6) {
                                    alumno.valoracionConceptual = "TEP";
                                } else if (val >= 1 && val <= 3) {
                                    alumno.valoracionConceptual = "TED";
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                        actualizarEstadoChips(alumno.valoracionConceptual);
                    }
                };
                etNotaNumerica.addTextChangedListener(watcherNota);

                watcherObs = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        alumno.observacion = s.toString();
                    }
                };
                etObservacion.addTextChangedListener(watcherObs);

                // Permite seleccionar / conmutar manualmente TEA, TEP, TED
                btnChipTea.setOnClickListener(v -> {
                    alumno.valoracionConceptual = "TEA".equals(alumno.valoracionConceptual) ? "" : "TEA";
                    actualizarEstadoChips(alumno.valoracionConceptual);
                });

                btnChipTep.setOnClickListener(v -> {
                    alumno.valoracionConceptual = "TEP".equals(alumno.valoracionConceptual) ? "" : "TEP";
                    actualizarEstadoChips(alumno.valoracionConceptual);
                });

                btnChipTed.setOnClickListener(v -> {
                    alumno.valoracionConceptual = "TED".equals(alumno.valoracionConceptual) ? "" : "TED";
                    actualizarEstadoChips(alumno.valoracionConceptual);
                });
            }

            private void actualizarEstadoChips(String valoracion) {
                int colorTeaBg = ContextCompat.getColor(itemView.getContext(), R.color.color_tea_bg);
                int colorTeaText = ContextCompat.getColor(itemView.getContext(), R.color.color_tea_text);
                int colorTepBg = ContextCompat.getColor(itemView.getContext(), R.color.color_tep_bg);
                int colorTepText = ContextCompat.getColor(itemView.getContext(), R.color.color_tep_text);
                int colorTedBg = ContextCompat.getColor(itemView.getContext(), R.color.color_ted_bg);
                int colorTedText = ContextCompat.getColor(itemView.getContext(), R.color.color_ted_text);

                int colorSurface = ContextCompat.getColor(itemView.getContext(), R.color.bg_surface);
                int colorMuted = ContextCompat.getColor(itemView.getContext(), R.color.text_muted);

                // Chip TEA
                if ("TEA".equals(valoracion)) {
                    btnChipTea.getBackground().mutate().setTint(colorTeaBg);
                    btnChipTea.setTextColor(colorTeaText);
                } else {
                    btnChipTea.getBackground().mutate().setTint(colorSurface);
                    btnChipTea.setTextColor(colorMuted);
                }

                // Chip TEP
                if ("TEP".equals(valoracion)) {
                    btnChipTep.getBackground().mutate().setTint(colorTepBg);
                    btnChipTep.setTextColor(colorTepText);
                } else {
                    btnChipTep.getBackground().mutate().setTint(colorSurface);
                    btnChipTep.setTextColor(colorMuted);
                }

                // Chip TED
                if ("TED".equals(valoracion)) {
                    btnChipTed.getBackground().mutate().setTint(colorTedBg);
                    btnChipTed.setTextColor(colorTedText);
                } else {
                    btnChipTed.getBackground().mutate().setTint(colorSurface);
                    btnChipTed.setTextColor(colorMuted);
                }
            }
        }
    }
}
