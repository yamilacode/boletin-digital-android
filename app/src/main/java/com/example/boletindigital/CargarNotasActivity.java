package com.example.boletindigital;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CargarNotasActivity extends AppCompatActivity {

    private ImageView ivVolver;
    private TextView tvBadgeRolNotas;
    private Spinner spnCurso;
    private Spinner spnMateria;
    private Spinner spnPeriodo;
    private TextView tvTituloPlanilla;
    private TextView tvCantidadAlumnos;
    private ProgressBar pbCargandoPlanilla;
    private RecyclerView rvAlumnosNotas;
    private TextView tvSinAlumnosPlanilla;
    private MaterialButton btnGuardarNotas;

    private String token;
    private String rol;
    private String nombre;
    private int idInstActiva;

    private final List<InstitucionManager.CursoOption> opcionesCurso = new ArrayList<>();
    private final List<String> opcionesMateria = new ArrayList<>();
    private String[] periodos = new String[0];

    private final List<JSONObject> todosLosAlumnosJson = new ArrayList<>();
    private final List<JSONObject> todasLasNotasJson = new ArrayList<>();
    private final List<AlumnoItemCarga> listaAlumnos = new ArrayList<>();
    private AlumnoCargaAdapter adapter;

    private boolean actualizandoSpinners = false;
    private String cicloActualCargado = "";
    private String pendingCursoId = null;
    private String pendingMateria = null;
    private String pendingPeriodo = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class AlumnoItemCarga implements Serializable {
        String dni;
        String nombreCompleto;
        String inicial;
        String notaNumerica = "";
        String valoracionConceptual = "";
        String observacion = "";
        boolean tieneNotaGuardada = false;
        int notaIdDb = 0;
        String materiaDb = "";
        String periodoDb = "";

        public AlumnoItemCarga(String dni, String nombreCompleto) {
            this.dni = dni;
            this.nombreCompleto = nombreCompleto;
            this.inicial = (nombreCompleto != null && !nombreCompleto.isEmpty())
                    ? String.valueOf(nombreCompleto.charAt(0)).toUpperCase(Locale.ROOT)
                    : "A";
        }
    }

    public interface AccionNotaListener {
        void onEditarNota(AlumnoItemCarga alumno, int position);
        void onEliminarNota(AlumnoItemCarga alumno, int position);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cargar_notas);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");
        idInstActiva = InstitucionManager.getIdInstitucionActiva(this);

        vincularVistas();
        configurarRecyclerView();
        configurarSpinnersIniciales();
        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombre);

        if (savedInstanceState != null) {
            pendingCursoId = savedInstanceState.getString("sel_curso_id");
            pendingMateria = savedInstanceState.getString("sel_materia");
            pendingPeriodo = savedInstanceState.getString("sel_periodo");
        }

        btnGuardarNotas.setOnClickListener(v -> guardarCalificaciones());
        ivVolver.setOnClickListener(v -> finish());

        cargarDatosDesdeApi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int instActual = InstitucionManager.getIdInstitucionActiva(this);
        if (instActual != idInstActiva) {
            idInstActiva = instActual;
            pendingCursoId = null;
            pendingMateria = null;
            pendingPeriodo = null;
            cicloActualCargado = "";
            configurarSpinnersIniciales();
            cargarDatosDesdeApi();
        }
    }

    private void vincularVistas() {
        ivVolver = findViewById(R.id.ivVolver);
        tvBadgeRolNotas = findViewById(R.id.tvBadgeRolNotas);
        spnCurso = findViewById(R.id.spnCurso);
        spnMateria = findViewById(R.id.spnMateria);
        spnPeriodo = findViewById(R.id.spnPeriodo);
        tvTituloPlanilla = findViewById(R.id.tvTituloPlanilla);
        tvCantidadAlumnos = findViewById(R.id.tvCantidadAlumnos);
        pbCargandoPlanilla = findViewById(R.id.pbCargandoPlanilla);
        rvAlumnosNotas = findViewById(R.id.rvAlumnosNotas);
        tvSinAlumnosPlanilla = findViewById(R.id.tvSinAlumnosPlanilla);
        btnGuardarNotas = findViewById(R.id.btnGuardarNotas);

        DocenteUiHelper.aplicarBadgeRol(tvBadgeRolNotas, rol != null ? rol : "PRECEPTOR");
    }

    private void configurarRecyclerView() {
        adapter = new AlumnoCargaAdapter(
                listaAlumnos,
                idInstActiva == InstitucionManager.ID_PBA,
                new AccionNotaListener() {
                    @Override
                    public void onEditarNota(AlumnoItemCarga alumno, int position) {
                        abrirDialogoEditarNota(alumno, position);
                    }

                    @Override
                    public void onEliminarNota(AlumnoItemCarga alumno, int position) {
                        confirmarEliminarNota(alumno, position);
                    }
                }
        );
        rvAlumnosNotas.setLayoutManager(new LinearLayoutManager(this));
        rvAlumnosNotas.setAdapter(adapter);
    }

    private void configurarSpinnersIniciales() {
        actualizandoSpinners = true;
        opcionesCurso.clear();
        opcionesCurso.addAll(InstitucionManager.getCursosPorDefectoDetalle(idInstActiva));
        aplicarAdapterCursos(obtenerCursoInicialPorDefecto());

        cicloActualCargado = "";
        actualizarMateriasSegunCursoSeleccionado();

        periodos = InstitucionManager.getPeriodosNotas(idInstActiva);
        DocenteUiHelper.configurarSpinner(this, spnPeriodo, periodos);
        actualizandoSpinners = false;

        spnCurso.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (actualizandoSpinners) return;
                actualizarMateriasSegunCursoSeleccionado();
                refiltrarPlanillaCursoYMateria();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        AdapterView.OnItemSelectedListener materiaPeriodoListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (actualizandoSpinners) return;
                aplicarNotasDeMateriaYPeriodo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spnMateria.setOnItemSelectedListener(materiaPeriodoListener);
        spnPeriodo.setOnItemSelectedListener(materiaPeriodoListener);
    }

    private String obtenerCursoInicialPorDefecto() {
        return idInstActiva == InstitucionManager.ID_PBA ? "pba-4-u" : "2-3";
    }

    private void aplicarAdapterCursos(String idPreferido) {
        DocenteUiHelper.configurarSpinner(this, spnCurso, opcionesCurso);

        int pos = 0;
        if (idPreferido != null && !idPreferido.isEmpty()) {
            for (int i = 0; i < opcionesCurso.size(); i++) {
                if (idPreferido.equalsIgnoreCase(opcionesCurso.get(i).id)
                        || idPreferido.equalsIgnoreCase(opcionesCurso.get(i).etiqueta)) {
                    pos = i;
                    break;
                }
            }
        }
        if (pos < spnCurso.getCount()) {
            spnCurso.setSelection(pos, false);
        }
    }

    private void actualizarMateriasSegunCursoSeleccionado() {
        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();
        String ciclo = cursoOpt != null ? cursoOpt.ciclo : "basico";
        if (ciclo.equals(cicloActualCargado) && !opcionesMateria.isEmpty()) {
            return;
        }
        cicloActualCargado = ciclo;

        String prevMateria = pendingMateria != null ? pendingMateria : getMateriaSeleccionada();
        String[] materiasCiclo = InstitucionManager.getMateriasPorCiclo(idInstActiva, ciclo);

        boolean prevFlag = actualizandoSpinners;
        actualizandoSpinners = true;
        opcionesMateria.clear();
        Collections.addAll(opcionesMateria, materiasCiclo);

        DocenteUiHelper.configurarSpinner(this, spnMateria, opcionesMateria);

        int selPos = 0;
        if (prevMateria != null && !prevMateria.isEmpty()) {
            for (int i = 0; i < opcionesMateria.size(); i++) {
                if (InstitucionManager.sonMateriasEquivalentes(opcionesMateria.get(i), prevMateria)) {
                    selPos = i;
                    break;
                }
            }
        }
        if (selPos < spnMateria.getCount()) {
            spnMateria.setSelection(selPos, false);
        }
        actualizandoSpinners = prevFlag;
    }

    private void cargarDatosDesdeApi() {
        final int instConsulta = idInstActiva;
        pbCargandoPlanilla.setVisibility(View.VISIBLE);
        tvCantidadAlumnos.setText("Cargando...");

        executor.execute(() -> {
            List<InstitucionManager.CursoOption> cursosApi = null;
            List<JSONObject> alumnosApi = new ArrayList<>();
            List<JSONObject> notasApi = new ArrayList<>();
            boolean falloAlumnos = false;

            try {
                try {
                    JSONArray insts = ApiClient.obtenerInstituciones();
                    for (int i = 0; i < insts.length(); i++) {
                        JSONObject inst = insts.getJSONObject(i);
                        if (inst.optInt("id_db") == instConsulta) {
                            cursosApi = InstitucionManager.extraerOpcionesCurso(inst, instConsulta);
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }

                JSONArray resAlum = ApiClient.listarAlumnos(instConsulta);
                for (int i = 0; i < resAlum.length(); i++) {
                    JSONObject obj = resAlum.optJSONObject(i);
                    if (obj != null) alumnosApi.add(obj);
                }

                try {
                    JSONArray resNotas = ApiClient.listarNotas(instConsulta, "", "");
                    for (int i = 0; i < resNotas.length(); i++) {
                        JSONObject obj = resNotas.optJSONObject(i);
                        if (obj != null) notasApi.add(obj);
                    }
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                falloAlumnos = true;
            }

            if (falloAlumnos) {
                alumnosApi.addAll(crearAlumnosOfflineFallback(instConsulta));
            }

            final List<InstitucionManager.CursoOption> cursosFinal = cursosApi;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || instConsulta != idInstActiva) return;
                pbCargandoPlanilla.setVisibility(View.GONE);

                InstitucionManager.CursoOption cursoActual = getCursoSeleccionado();
                String idCursoObjetivo = pendingCursoId != null
                        ? pendingCursoId
                        : (cursoActual != null ? cursoActual.id : obtenerCursoInicialPorDefecto());

                if (cursosFinal != null && !cursosFinal.isEmpty()) {
                    actualizandoSpinners = true;
                    opcionesCurso.clear();
                    opcionesCurso.addAll(cursosFinal);
                    aplicarAdapterCursos(idCursoObjetivo);
                    cicloActualCargado = "";
                    actualizarMateriasSegunCursoSeleccionado();
                    if (pendingPeriodo != null) {
                        seleccionarPeriodoSiExiste(pendingPeriodo);
                    }
                    actualizandoSpinners = false;
                } else {
                    actualizandoSpinners = true;
                    aplicarAdapterCursos(idCursoObjetivo);
                    actualizarMateriasSegunCursoSeleccionado();
                    if (pendingPeriodo != null) {
                        seleccionarPeriodoSiExiste(pendingPeriodo);
                    }
                    actualizandoSpinners = false;
                }

                pendingCursoId = null;
                pendingMateria = null;
                pendingPeriodo = null;

                todosLosAlumnosJson.clear();
                todosLosAlumnosJson.addAll(alumnosApi);

                todasLasNotasJson.clear();
                todasLasNotasJson.addAll(notasApi);

                refiltrarPlanillaCursoYMateria();
            });
        });
    }

    private List<JSONObject> crearAlumnosOfflineFallback(int inst) {
        List<JSONObject> lista = new ArrayList<>();
        try {
            if (inst == InstitucionManager.ID_PBA) {
                lista.add(crearAlumnoJson("51234567", "Milagros Belén", "Benítez", "1°", "Única"));
                lista.add(crearAlumnoJson("51123456", "Lucas Nahuel", "Giménez", "2°", "Única"));
                lista.add(crearAlumnoJson("51345678", "Joaquín Alejandro", "Ramírez", "3°", "Única"));
                lista.add(crearAlumnoJson("48763736", "Uriel Luciano", "Pleininger", "4°", "Única"));
                lista.add(crearAlumnoJson("51456789", "Camila Agustina", "Sosa", "5°", "Única (CS)"));
                lista.add(crearAlumnoJson("51567890", "Thiago Mateo", "Torres", "6°", "Única (CS)"));
            } else {
                lista.add(crearAlumnoJson("49123456", "Sofía Belén", "Benítez", "1°", "I"));
                lista.add(crearAlumnoJson("30456789", "Analía Soledad", "Rodríguez", "1°", "II"));
                lista.add(crearAlumnoJson("34567890", "Mateo", "Gómez", "2°", "III"));
                lista.add(crearAlumnoJson("49901234", "Julieta Milagros", "Romero", "4°", "I"));
                lista.add(crearAlumnoJson("50345678", "Joaquín Alejandro", "Medina", "5°", "I (CN)"));
                lista.add(crearAlumnoJson("50234567", "Milagros Ayelén", "Silva", "5°", "I (CS)"));
            }
        } catch (Exception ignored) {
        }
        return lista;
    }

    private JSONObject crearAlumnoJson(String dni, String nombre, String apellido, String curso, String seccion) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("dni", dni);
        obj.put("nombre", nombre);
        obj.put("apellido", apellido);
        obj.put("curso", curso);
        obj.put("seccion", seccion);
        return obj;
    }

    private void refiltrarPlanillaCursoYMateria() {
        rvAlumnosNotas.clearFocus();
        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();

        listaAlumnos.clear();
        for (JSONObject obj : todosLosAlumnosJson) {
            if (InstitucionManager.perteneceAlCurso(obj, cursoOpt)) {
                String dni = textoSeguroJson(obj, "dni");
                String nom = textoSeguroJson(obj, "nombre");
                String ap = textoSeguroJson(obj, "apellido");
                String nombreCompleto = ap.isEmpty()
                        ? nom
                        : (ap + (nom.isEmpty() ? "" : ", " + nom));
                if (nombreCompleto.isEmpty()) {
                    nombreCompleto = "Alumno DNI " + dni;
                }
                listaAlumnos.add(new AlumnoItemCarga(dni, nombreCompleto));
            }
        }

        Collections.sort(listaAlumnos, (a, b) -> a.nombreCompleto.compareToIgnoreCase(b.nombreCompleto));
        aplicarNotasDeMateriaYPeriodo();
    }

    private void aplicarNotasDeMateriaYPeriodo() {
        rvAlumnosNotas.clearFocus();
        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();
        String materiaSel = getMateriaSeleccionada();
        String periodoSel = getPeriodoSeleccionado();
        String periodoClaveSel = normalizarPeriodoClave(periodoSel);

        for (AlumnoItemCarga alumno : listaAlumnos) {
            alumno.notaNumerica = "";
            alumno.valoracionConceptual = "";
            alumno.observacion = "";
            alumno.tieneNotaGuardada = false;
            alumno.notaIdDb = 0;
            alumno.materiaDb = "";
            alumno.periodoDb = "";

            for (JSONObject n : todasLasNotasJson) {
                String dniNota = textoSeguroJson(n, "dni_estudiante");
                if (!dniNota.equals(alumno.dni)) continue;

                String matNota = textoSeguroJson(n, "materia");
                String perNota = textoSeguroJson(n, "periodo");
                if (InstitucionManager.sonMateriasEquivalentes(matNota, materiaSel)
                        && normalizarPeriodoClave(perNota).equals(periodoClaveSel)) {
                    alumno.tieneNotaGuardada = true;
                    alumno.notaIdDb = n.optInt("id", 0);
                    alumno.materiaDb = matNota;
                    alumno.periodoDb = perNota;

                    if (!n.isNull("nota_numerica")) {
                        String notaRaw = String.valueOf(n.opt("nota_numerica")).trim();
                        if (!"null".equalsIgnoreCase(notaRaw)) {
                            alumno.notaNumerica = notaRaw;
                        }
                    }
                    alumno.valoracionConceptual = textoSeguroJson(n, "valoracion_conceptual");
                    alumno.observacion = textoSeguroJson(n, "observacion");
                    break;
                }
            }
        }

        adapter.setEsPba(idInstActiva == InstitucionManager.ID_PBA);
        adapter.notifyDataSetChanged();

        boolean sinAlumnos = listaAlumnos.isEmpty();
        tvSinAlumnosPlanilla.setVisibility(sinAlumnos ? View.VISIBLE : View.GONE);
        if (sinAlumnos) {
            String nombreCurso = cursoOpt != null ? cursoOpt.etiqueta : "este curso";
            tvSinAlumnosPlanilla.setText("No hay alumnos registrados en " + nombreCurso
                    + ".\nPodés agregar estudiantes en Gestión de Alumnos.");
        }
        btnGuardarNotas.setEnabled(!sinAlumnos);
        btnGuardarNotas.setAlpha(sinAlumnos ? 0.5f : 1.0f);

        String etiquetaCursoCorto = cursoOpt != null ? cursoOpt.nombre : "";
        tvCantidadAlumnos.setText(listaAlumnos.size()
                + (listaAlumnos.size() == 1 ? " Alumno" : " Alumnos")
                + (etiquetaCursoCorto.isEmpty() ? "" : " • " + etiquetaCursoCorto));
        actualizarTituloPlanilla();
    }

    private void actualizarTituloPlanilla() {
        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();
        String curso = cursoOpt != null ? cursoOpt.nombre : "Curso";
        String materia = getMateriaSeleccionada();
        String periodo = getPeriodoSeleccionado();
        tvTituloPlanilla.setText("Planilla: " + materia + " — " + curso + " (" + periodo + ")");
    }

    private InstitucionManager.CursoOption getCursoSeleccionado() {
        Object sel = spnCurso != null ? spnCurso.getSelectedItem() : null;
        if (sel instanceof InstitucionManager.CursoOption) {
            return (InstitucionManager.CursoOption) sel;
        }
        return !opcionesCurso.isEmpty() ? opcionesCurso.get(0) : null;
    }

    private String getMateriaSeleccionada() {
        Object sel = spnMateria != null ? spnMateria.getSelectedItem() : null;
        return sel != null ? sel.toString() : "Matemática";
    }

    private String getPeriodoSeleccionado() {
        Object sel = spnPeriodo != null ? spnPeriodo.getSelectedItem() : null;
        return sel != null ? sel.toString() : (idInstActiva == InstitucionManager.ID_PBA ? "1° Cuatrimestre" : "1° Trimestre");
    }

    private void seleccionarPeriodoSiExiste(String periodoObjetivo) {
        if (periodoObjetivo == null || spnPeriodo == null) return;
        for (int i = 0; i < periodos.length; i++) {
            if (periodos[i].equalsIgnoreCase(periodoObjetivo)) {
                spnPeriodo.setSelection(i, false);
                return;
            }
        }
    }

    private void guardarCalificaciones() {
        if (listaAlumnos.isEmpty()) {
            Toast.makeText(this, "No hay alumnos en el curso seleccionado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, R.string.error_sin_sesion, Toast.LENGTH_LONG).show();
            return;
        }

        rvAlumnosNotas.clearFocus();

        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();
        String materia = getMateriaSeleccionada();
        String periodo = getPeriodoSeleccionado();
        String cursoNombre = cursoOpt != null ? cursoOpt.nombre : "";

        JSONArray listaJson = new JSONArray();
        int totalConNota = 0;

        for (AlumnoItemCarga alumno : listaAlumnos) {
            String notaStr = alumno.notaNumerica != null ? alumno.notaNumerica.trim() : "";
            String valStr = alumno.valoracionConceptual != null ? alumno.valoracionConceptual.trim() : "";
            String obsStr = alumno.observacion != null ? alumno.observacion.trim() : "";

            if (!notaStr.isEmpty() || !valStr.isEmpty() || !obsStr.isEmpty()) {
                if (!notaStr.isEmpty()) {
                    try {
                        int num = Integer.parseInt(notaStr);
                        if (num < 1 || num > 10) {
                            Toast.makeText(this, "Nota inválida para " + alumno.nombreCompleto + " (Debe ser de 1 a 10)", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Nota numérica inválida para " + alumno.nombreCompleto, Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                totalConNota++;
                try {
                    String materiaEnvio = !alumno.materiaDb.isEmpty() ? alumno.materiaDb : materia;
                    listaJson.put(construirPayloadNota(alumno, cursoNombre, materiaEnvio, periodo));
                } catch (Exception ignored) {
                }
            }
        }

        if (totalConNota == 0) {
            Toast.makeText(this, "Ingresá al menos una calificación o valoración para guardar la planilla", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardarNotas.setEnabled(false);
        btnGuardarNotas.setText("GUARDANDO CALIFICACIONES...");
        pbCargandoPlanilla.setVisibility(View.VISIBLE);

        final int notasAEnviar = totalConNota;
        final int instConsulta = idInstActiva;

        executor.execute(() -> {
            try {
                JSONObject respuesta = ApiClient.guardarNotas(listaJson, token);
                List<JSONObject> notasRefrescadas = recargarNotasSilencioso(instConsulta);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnGuardarNotas.setEnabled(true);
                    btnGuardarNotas.setText("GUARDAR CALIFICACIONES DE LA MATERIA");
                    pbCargandoPlanilla.setVisibility(View.GONE);

                    if (notasRefrescadas != null) {
                        todasLasNotasJson.clear();
                        todasLasNotasJson.addAll(notasRefrescadas);
                        aplicarNotasDeMateriaYPeriodo();
                    }

                    String msg = respuesta.optString("mensaje", "Se guardaron " + notasAEnviar + " calificaciones correctamente.");
                    new AlertDialog.Builder(this)
                            .setTitle("Calificaciones Guardadas")
                            .setMessage("Curso: " + (cursoOpt != null ? cursoOpt.etiqueta : cursoNombre)
                                    + "\nMateria: " + materia
                                    + "\nPeríodo: " + periodo
                                    + "\nRegistros guardados: " + notasAEnviar + "\n\n" + msg)
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
                            .setMessage("No se pudieron registrar las calificaciones:\n" + errorMsg)
                            .setPositiveButton("Cerrar", null)
                            .show();
                });
            }
        });
    }

    private JSONObject construirPayloadNota(AlumnoItemCarga alumno, String cursoNombre, String materia, String periodo) throws Exception {
        JSONObject item = new JSONObject();
        item.put("id_institucion", idInstActiva);
        item.put("dni_estudiante", alumno.dni);
        item.put("curso", cursoNombre);
        item.put("materia", materia);
        item.put("periodo", periodo);

        String notaStr = alumno.notaNumerica != null ? alumno.notaNumerica.trim() : "";
        if (!notaStr.isEmpty()) {
            item.put("nota_numerica", Integer.parseInt(notaStr));
        } else {
            item.put("nota_numerica", JSONObject.NULL);
        }

        String val = alumno.valoracionConceptual != null ? alumno.valoracionConceptual.trim() : "";
        String obs = alumno.observacion != null ? alumno.observacion.trim() : "";
        item.put("valoracion_conceptual", val.isEmpty() ? JSONObject.NULL : val);
        item.put("observacion", obs.isEmpty() ? "" : obs);
        return item;
    }

    private void abrirDialogoEditarNota(AlumnoItemCarga alumno, int position) {
        rvAlumnosNotas.clearFocus();

        final boolean esPba = (idInstActiva == InstitucionManager.ID_PBA);
        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();
        String cursoNombre = cursoOpt != null ? cursoOpt.nombre : "";
        String materiaSel = getMateriaSeleccionada();
        String periodoSel = getPeriodoSeleccionado();

        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(pad, Math.round(12 * getResources().getDisplayMetrics().density), pad, Math.round(4 * getResources().getDisplayMetrics().density));

        TextView tvInfo = new TextView(this);
        tvInfo.setText(alumno.nombreCompleto + " (DNI " + alumno.dni + ")\n"
                + (cursoNombre.isEmpty() ? "" : cursoNombre + " • ") + materiaSel + " • " + periodoSel);
        tvInfo.setTextSize(13f);
        tvInfo.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvInfo.setPadding(0, 0, 0, Math.round(8 * getResources().getDisplayMetrics().density));
        contenedor.addView(tvInfo);

        TextView lblNota = crearLabelDialogo("Nota Numérica (1 a 10)");
        contenedor.addView(lblNota);

        EditText etNotaDlg = new EditText(this);
        etNotaDlg.setInputType(InputType.TYPE_CLASS_NUMBER);
        etNotaDlg.setHint("Ej: 8");
        etNotaDlg.setText(alumno.notaNumerica);
        etNotaDlg.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        etNotaDlg.setHintTextColor(ContextCompat.getColor(this, R.color.text_dim));
        etNotaDlg.setBackgroundResource(R.drawable.bg_input_web);
        int pField = Math.round(10 * getResources().getDisplayMetrics().density);
        etNotaDlg.setPadding(pField, pField, pField, pField);
        contenedor.addView(etNotaDlg);

        TextView lblVal = crearLabelDialogo(esPba ? "Valoración R.I.T.E." : "Concepto Oficial");
        contenedor.addView(lblVal);

        final String[] opcionesVal = esPba
                ? new String[]{"", "TEA", "TEP", "TED"}
                : new String[]{"", "SO", "MB", "B", "R", "I"};
        final String[] etiquetasVal = esPba
                ? new String[]{"Sin especificar", "TEA (Avanzada)", "TEP (En Proceso)", "TED (Discontinua)"}
                : new String[]{"Sin especificar", "SO (10)", "MB (8-9)", "B (6-7)", "R (4-5)", "I (1-3)"};

        AppCompatSpinner spnValDlg = new AppCompatSpinner(this);
        DocenteUiHelper.configurarSpinner(this, spnValDlg, etiquetasVal);
        for (int i = 0; i < opcionesVal.length; i++) {
            if (opcionesVal[i].equalsIgnoreCase(alumno.valoracionConceptual)) {
                spnValDlg.setSelection(i);
                break;
            }
        }
        contenedor.addView(spnValDlg);

        etNotaDlg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String txt = s != null ? s.toString().trim() : "";
                if (txt.isEmpty()) return;
                try {
                    int n = Integer.parseInt(txt);
                    String auto = calcularValoracionPorDefecto(n, esPba);
                    if (!auto.isEmpty()) {
                        for (int i = 0; i < opcionesVal.length; i++) {
                            if (opcionesVal[i].equalsIgnoreCase(auto)) {
                                spnValDlg.setSelection(i);
                                break;
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });

        TextView lblObs = crearLabelDialogo("Observación pedagógica (opcional)");
        contenedor.addView(lblObs);

        EditText etObsDlg = new EditText(this);
        etObsDlg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etObsDlg.setHint("Ingresá una observación...");
        etObsDlg.setText(alumno.observacion);
        etObsDlg.setTextColor(ContextCompat.getColor(this, R.color.text_main));
        etObsDlg.setHintTextColor(ContextCompat.getColor(this, R.color.text_dim));
        etObsDlg.setBackgroundResource(R.drawable.bg_input_web);
        etObsDlg.setPadding(pField, pField, pField, pField);
        contenedor.addView(etObsDlg);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(contenedor);

        new AlertDialog.Builder(this)
                .setTitle("Editar Calificación")
                .setView(scroll)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String notaNueva = etNotaDlg.getText() != null ? etNotaDlg.getText().toString().trim() : "";
                    String valNueva = opcionesVal[spnValDlg.getSelectedItemPosition()];
                    String obsNueva = etObsDlg.getText() != null ? etObsDlg.getText().toString().trim() : "";

                    if (notaNueva.isEmpty() && valNueva.isEmpty() && obsNueva.isEmpty()) {
                        Toast.makeText(this, "Ingresá nota numérica o valoración", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!notaNueva.isEmpty()) {
                        try {
                            int num = Integer.parseInt(notaNueva);
                            if (num < 1 || num > 10) {
                                Toast.makeText(this, "La nota debe ser de 1 a 10", Toast.LENGTH_LONG).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Nota numérica inválida", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    guardarNotaIndividual(alumno, position, cursoNombre, materiaSel, periodoSel, notaNueva, valNueva, obsNueva);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private TextView crearLabelDialogo(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(12f);
        tv.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
        int top = Math.round(10 * getResources().getDisplayMetrics().density);
        int bot = Math.round(4 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, top, 0, bot);
        return tv;
    }

    private void guardarNotaIndividual(
            AlumnoItemCarga alumno,
            int position,
            String cursoNombre,
            String materiaSel,
            String periodoSel,
            String notaNueva,
            String valNueva,
            String obsNueva
    ) {
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, R.string.error_sin_sesion, Toast.LENGTH_LONG).show();
            return;
        }

        alumno.notaNumerica = notaNueva;
        alumno.valoracionConceptual = valNueva;
        alumno.observacion = obsNueva;

        String materiaEnvio = !alumno.materiaDb.isEmpty() ? alumno.materiaDb : materiaSel;
        pbCargandoPlanilla.setVisibility(View.VISIBLE);
        final int instConsulta = idInstActiva;

        executor.execute(() -> {
            try {
                JSONArray array = new JSONArray();
                array.put(construirPayloadNota(alumno, cursoNombre, materiaEnvio, periodoSel));
                ApiClient.guardarNotas(array, token);

                List<JSONObject> notasRefrescadas = recargarNotasSilencioso(instConsulta);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoPlanilla.setVisibility(View.GONE);
                    alumno.tieneNotaGuardada = true;
                    alumno.materiaDb = materiaEnvio;
                    alumno.periodoDb = periodoSel;

                    if (notasRefrescadas != null) {
                        todasLasNotasJson.clear();
                        todasLasNotasJson.addAll(notasRefrescadas);
                        aplicarNotasDeMateriaYPeriodo();
                    } else if (position >= 0 && position < listaAlumnos.size()) {
                        adapter.notifyItemChanged(position);
                    }
                    Toast.makeText(this, "Calificación de " + alumno.nombreCompleto + " guardada", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoPlanilla.setVisibility(View.GONE);
                    String err = e.getMessage() != null ? e.getMessage() : "Error al guardar calificación";
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmarEliminarNota(AlumnoItemCarga alumno, int position) {
        rvAlumnosNotas.clearFocus();

        boolean tieneAlgo = alumno.tieneNotaGuardada
                || !alumno.notaNumerica.trim().isEmpty()
                || !alumno.valoracionConceptual.trim().isEmpty()
                || !alumno.observacion.trim().isEmpty();

        if (!tieneAlgo) {
            Toast.makeText(this, "Este alumno no tiene calificación cargada en " + getMateriaSeleccionada(), Toast.LENGTH_SHORT).show();
            return;
        }

        String materiaSel = getMateriaSeleccionada();
        String periodoSel = getPeriodoSeleccionado();

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Calificación")
                .setMessage("¿Deseás eliminar la calificación de " + alumno.nombreCompleto
                        + " en " + materiaSel + " (" + periodoSel + ")?")
                .setPositiveButton("Eliminar", (dialog, which) -> ejecutarEliminarNota(alumno, position, materiaSel, periodoSel))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarEliminarNota(AlumnoItemCarga alumno, int position, String materiaSel, String periodoSel) {
        if (!alumno.tieneNotaGuardada) {
            alumno.notaNumerica = "";
            alumno.valoracionConceptual = "";
            alumno.observacion = "";
            if (position >= 0 && position < listaAlumnos.size()) {
                adapter.notifyItemChanged(position);
            }
            Toast.makeText(this, "Borrador eliminado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, R.string.error_sin_sesion, Toast.LENGTH_LONG).show();
            return;
        }

        String materiaDelete = !alumno.materiaDb.isEmpty() ? alumno.materiaDb : materiaSel;
        String periodoDelete = !alumno.periodoDb.isEmpty() ? alumno.periodoDb : periodoSel;
        final int instConsulta = idInstActiva;

        pbCargandoPlanilla.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                ApiClient.eliminarNota(alumno.dni, materiaDelete, periodoDelete, instConsulta, token);
                List<JSONObject> notasRefrescadas = recargarNotasSilencioso(instConsulta);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoPlanilla.setVisibility(View.GONE);

                    if (notasRefrescadas != null) {
                        todasLasNotasJson.clear();
                        todasLasNotasJson.addAll(notasRefrescadas);
                        aplicarNotasDeMateriaYPeriodo();
                    } else {
                        removerNotaDeCacheLocal(alumno.dni, materiaDelete, periodoDelete);
                        alumno.notaNumerica = "";
                        alumno.valoracionConceptual = "";
                        alumno.observacion = "";
                        alumno.tieneNotaGuardada = false;
                        alumno.notaIdDb = 0;
                        alumno.materiaDb = "";
                        alumno.periodoDb = "";
                        if (position >= 0 && position < listaAlumnos.size()) {
                            adapter.notifyItemChanged(position);
                        }
                    }

                    Toast.makeText(this, "Calificación eliminada correctamente", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    pbCargandoPlanilla.setVisibility(View.GONE);
                    String err = e.getMessage() != null ? e.getMessage() : "Error al eliminar calificación";
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private List<JSONObject> recargarNotasSilencioso(int idInst) {
        try {
            JSONArray res = ApiClient.listarNotas(idInst, "", "");
            List<JSONObject> parsed = new ArrayList<>();
            for (int i = 0; i < res.length(); i++) {
                JSONObject obj = res.optJSONObject(i);
                if (obj != null) parsed.add(obj);
            }
            return parsed;
        } catch (Exception e) {
            return null;
        }
    }

    private void removerNotaDeCacheLocal(String dni, String materia, String periodo) {
        String perKey = normalizarPeriodoClave(periodo);
        for (int i = todasLasNotasJson.size() - 1; i >= 0; i--) {
            JSONObject n = todasLasNotasJson.get(i);
            if (dni.equals(textoSeguroJson(n, "dni_estudiante"))
                    && InstitucionManager.sonMateriasEquivalentes(textoSeguroJson(n, "materia"), materia)
                    && normalizarPeriodoClave(textoSeguroJson(n, "periodo")).equals(perKey)) {
                todasLasNotasJson.remove(i);
            }
        }
    }

    private static String calcularValoracionPorDefecto(int nota, boolean esPba) {
        if (nota < 1 || nota > 10) return "";
        if (esPba) {
            if (nota >= 7) return "TEA";
            if (nota >= 4) return "TEP";
            return "TED";
        }
        if (nota == 10) return "SO";
        if (nota >= 8) return "MB";
        if (nota >= 6) return "B";
        if (nota >= 4) return "R";
        return "I";
    }

    private static String normalizarPeriodoClave(String periodo) {
        if (periodo == null) return "";
        String p = periodo.toLowerCase(Locale.ROOT).trim();
        if (p.equals("c1") || (p.contains("1") && p.contains("cuat"))) return "c1";
        if (p.equals("c2") || (p.contains("2") && p.contains("cuat"))) return "c2";
        if ((p.contains("rit") && p.contains("dic")) || (p.contains("intensif") && p.contains("dic"))) return "rit_dic";
        if ((p.contains("rit") && p.contains("feb")) || (p.contains("intensif") && p.contains("feb"))) return "rit_feb";
        if (p.equals("t1") || (p.contains("1") && p.contains("trim") && !p.contains("cuat"))) return "t1";
        if (p.equals("t2") || (p.contains("2") && p.contains("trim") && !p.contains("cuat"))) return "t2";
        if (p.equals("t3") || (p.contains("3") && p.contains("trim") && !p.contains("cuat"))) return "t3";
        if (p.equals("rec_dic") || (p.contains("rec") && p.contains("dic")) || (p.contains("complem") && p.contains("dic"))) return "rec_dic";
        if (p.equals("pair") || p.contains("pair")) return "pair";
        return p;
    }

    private static String textoSeguroJson(JSONObject obj, String clave) {
        if (obj == null || obj.isNull(clave)) return "";
        String val = obj.optString(clave, "");
        return "null".equalsIgnoreCase(val) ? "" : val.trim();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        InstitucionManager.CursoOption cursoOpt = getCursoSeleccionado();
        outState.putString("sel_curso_id", cursoOpt != null ? cursoOpt.id : "");
        outState.putString("sel_materia", getMateriaSeleccionada());
        outState.putString("sel_periodo", getPeriodoSeleccionado());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    public static class AlumnoCargaAdapter extends RecyclerView.Adapter<AlumnoCargaAdapter.ViewHolder> {

        private final List<AlumnoItemCarga> datos;
        private final AccionNotaListener listener;
        private boolean esPba;

        public AlumnoCargaAdapter(List<AlumnoItemCarga> datos, boolean esPba, AccionNotaListener listener) {
            this.datos = datos;
            this.esPba = esPba;
            this.listener = listener;
        }

        public void setEsPba(boolean esPba) {
            this.esPba = esPba;
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
            holder.vincular(item, esPba, listener);
        }

        @Override
        public int getItemCount() {
            return datos.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvInicialAlumno;
            TextView tvNombreAlumno;
            TextView tvDniAlumno;
            TextView tvEstadoNotaItem;
            ImageView btnEditarNotaItem;
            ImageView btnEliminarNotaItem;
            TextInputEditText etNotaNumerica;
            View layoutChipsPba;
            TextView btnChipTea;
            TextView btnChipTep;
            TextView btnChipTed;
            View layoutChipsFormosa;
            TextView btnChipSo;
            TextView btnChipMb;
            TextView btnChipB;
            TextView btnChipR;
            TextView btnChipI;
            TextInputEditText etObservacion;

            TextWatcher watcherNota;
            TextWatcher watcherObs;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInicialAlumno = itemView.findViewById(R.id.tvInicialAlumno);
                tvNombreAlumno = itemView.findViewById(R.id.tvNombreAlumno);
                tvDniAlumno = itemView.findViewById(R.id.tvDniAlumno);
                tvEstadoNotaItem = itemView.findViewById(R.id.tvEstadoNotaItem);
                btnEditarNotaItem = itemView.findViewById(R.id.btnEditarNotaItem);
                btnEliminarNotaItem = itemView.findViewById(R.id.btnEliminarNotaItem);
                etNotaNumerica = itemView.findViewById(R.id.etNotaNumerica);
                layoutChipsPba = itemView.findViewById(R.id.layoutChipsPba);
                btnChipTea = itemView.findViewById(R.id.btnChipTea);
                btnChipTep = itemView.findViewById(R.id.btnChipTep);
                btnChipTed = itemView.findViewById(R.id.btnChipTed);
                layoutChipsFormosa = itemView.findViewById(R.id.layoutChipsFormosa);
                btnChipSo = itemView.findViewById(R.id.btnChipSo);
                btnChipMb = itemView.findViewById(R.id.btnChipMb);
                btnChipB = itemView.findViewById(R.id.btnChipB);
                btnChipR = itemView.findViewById(R.id.btnChipR);
                btnChipI = itemView.findViewById(R.id.btnChipI);
                etObservacion = itemView.findViewById(R.id.etObservacion);
            }

            public void vincular(AlumnoItemCarga alumno, boolean esPba, AccionNotaListener listener) {
                tvInicialAlumno.setText(alumno.inicial);
                tvNombreAlumno.setText(alumno.nombreCompleto);
                tvDniAlumno.setText("DNI: " + alumno.dni);

                if (layoutChipsPba != null && layoutChipsFormosa != null) {
                    layoutChipsPba.setVisibility(esPba ? View.VISIBLE : View.GONE);
                    layoutChipsFormosa.setVisibility(esPba ? View.GONE : View.VISIBLE);
                }

                if (watcherNota != null) etNotaNumerica.removeTextChangedListener(watcherNota);
                if (watcherObs != null) etObservacion.removeTextChangedListener(watcherObs);

                etNotaNumerica.setText(alumno.notaNumerica);
                etObservacion.setText(alumno.observacion);

                actualizarEstadoChips(alumno.valoracionConceptual, esPba);
                actualizarBadgeEstado(alumno);

                watcherNota = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String txt = s.toString().trim();
                        alumno.notaNumerica = txt;
                        if (!txt.isEmpty()) {
                            try {
                                int val = Integer.parseInt(txt);
                                String calc = calcularValoracionPorDefecto(val, esPba);
                                if (!calc.isEmpty()) {
                                    alumno.valoracionConceptual = calc;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        actualizarEstadoChips(alumno.valoracionConceptual, esPba);
                        actualizarBadgeEstado(alumno);
                    }
                };
                etNotaNumerica.addTextChangedListener(watcherNota);

                watcherObs = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        alumno.observacion = s.toString();
                    }
                };
                etObservacion.addTextChangedListener(watcherObs);

                configurarClickChip(btnChipTea, "TEA", alumno, esPba);
                configurarClickChip(btnChipTep, "TEP", alumno, esPba);
                configurarClickChip(btnChipTed, "TED", alumno, esPba);

                configurarClickChip(btnChipSo, "SO", alumno, esPba);
                configurarClickChip(btnChipMb, "MB", alumno, esPba);
                configurarClickChip(btnChipB, "B", alumno, esPba);
                configurarClickChip(btnChipR, "R", alumno, esPba);
                configurarClickChip(btnChipI, "I", alumno, esPba);

                if (btnEditarNotaItem != null) {
                    btnEditarNotaItem.setOnClickListener(v -> {
                        int pos = getAdapterPosition();
                        if (listener != null && pos != RecyclerView.NO_POSITION) {
                            listener.onEditarNota(alumno, pos);
                        }
                    });
                }

                if (btnEliminarNotaItem != null) {
                    btnEliminarNotaItem.setOnClickListener(v -> {
                        int pos = getAdapterPosition();
                        if (listener != null && pos != RecyclerView.NO_POSITION) {
                            listener.onEliminarNota(alumno, pos);
                        }
                    });
                }
            }

            private void configurarClickChip(TextView chip, String codigo, AlumnoItemCarga alumno, boolean esPba) {
                if (chip == null) return;
                chip.setOnClickListener(v -> {
                    alumno.valoracionConceptual = codigo.equalsIgnoreCase(alumno.valoracionConceptual) ? "" : codigo;
                    actualizarEstadoChips(alumno.valoracionConceptual, esPba);
                    actualizarBadgeEstado(alumno);
                });
            }

            private void actualizarBadgeEstado(AlumnoItemCarga alumno) {
                if (tvEstadoNotaItem == null) return;
                boolean tieneBorrador = !alumno.notaNumerica.trim().isEmpty() || !alumno.valoracionConceptual.trim().isEmpty();
                if (alumno.tieneNotaGuardada) {
                    String resumen = "Registrada";
                    if (!alumno.notaNumerica.trim().isEmpty() || !alumno.valoracionConceptual.trim().isEmpty()) {
                        resumen += " • " + (!alumno.notaNumerica.trim().isEmpty() ? alumno.notaNumerica.trim() : "")
                                + (!alumno.valoracionConceptual.trim().isEmpty() ? " " + alumno.valoracionConceptual.trim() : "");
                    }
                    tvEstadoNotaItem.setText(resumen.trim());
                    tvEstadoNotaItem.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.color_tea_bg)));
                    tvEstadoNotaItem.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_tea_text));
                } else if (tieneBorrador) {
                    tvEstadoNotaItem.setText("Sin guardar");
                    tvEstadoNotaItem.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.color_tep_bg)));
                    tvEstadoNotaItem.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_tep_text));
                } else {
                    tvEstadoNotaItem.setText("Pendiente de carga");
                    tvEstadoNotaItem.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.bg_surface)));
                    tvEstadoNotaItem.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_muted));
                }
            }

            private void actualizarEstadoChips(String valoracion, boolean esPba) {
                int colorTeaBg = ContextCompat.getColor(itemView.getContext(), R.color.color_tea_bg);
                int colorTeaText = ContextCompat.getColor(itemView.getContext(), R.color.color_tea_text);
                int colorTepBg = ContextCompat.getColor(itemView.getContext(), R.color.color_tep_bg);
                int colorTepText = ContextCompat.getColor(itemView.getContext(), R.color.color_tep_text);
                int colorTedBg = ContextCompat.getColor(itemView.getContext(), R.color.color_ted_bg);
                int colorTedText = ContextCompat.getColor(itemView.getContext(), R.color.color_ted_text);
                int colorBlueBg = ContextCompat.getColor(itemView.getContext(), R.color.color_badge_blue_bg);
                int colorPrimary = ContextCompat.getColor(itemView.getContext(), R.color.color_primary);

                if (esPba) {
                    pintarChip(btnChipTea, "TEA".equalsIgnoreCase(valoracion), colorTeaBg, colorTeaText);
                    pintarChip(btnChipTep, "TEP".equalsIgnoreCase(valoracion), colorTepBg, colorTepText);
                    pintarChip(btnChipTed, "TED".equalsIgnoreCase(valoracion), colorTedBg, colorTedText);
                } else {
                    pintarChip(btnChipSo, "SO".equalsIgnoreCase(valoracion), colorTeaBg, colorTeaText);
                    pintarChip(btnChipMb, "MB".equalsIgnoreCase(valoracion), colorTeaBg, colorTeaText);
                    pintarChip(btnChipB, "B".equalsIgnoreCase(valoracion), colorBlueBg, colorPrimary);
                    pintarChip(btnChipR, "R".equalsIgnoreCase(valoracion), colorTepBg, colorTepText);
                    pintarChip(btnChipI, "I".equalsIgnoreCase(valoracion), colorTedBg, colorTedText);
                }
            }

            private void pintarChip(TextView chip, boolean activo, int bgActivo, int textActivo) {
                if (chip == null) return;
                int bg = activo ? bgActivo : ContextCompat.getColor(itemView.getContext(), R.color.bg_surface);
                int txt = activo ? textActivo : ContextCompat.getColor(itemView.getContext(), R.color.text_muted);
                chip.getBackground().mutate().setTint(bg);
                chip.setTextColor(txt);
            }
        }
    }
}
