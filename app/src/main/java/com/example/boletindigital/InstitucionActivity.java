package com.example.boletindigital;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InstitucionActivity extends AppCompatActivity {

    private ImageView ivVolver;
    private TextView tvBadgeRolInstitucion;
    private TextView btnSedePba;
    private TextView btnSedeFormosa;

    private TextView tvEstadoActivaInst;
    private TextView tvDbAlumnosCount;
    private TextView tvDbMesasCount;
    private TextView tvDbAvisosCount;
    private TextView tvDbCursosResumen;

    private TextView tvNombreInstitucion;
    private TextView tvDistritoInstitucion;
    private TextView tvCueInstitucion;
    private TextView tvNivelInstitucion;
    private TextView tvRegimenInstitucion;

    private TextView tvDetalleCicloBasico;
    private TextView tvDetalleCicloOrientado;
    private TextView tvTituloCriterio;
    private TextView tvDetalleCriterio;

    private TextView tvDirectora;
    private TextView tvVicedirector;
    private TextView tvSecretaria;

    private String token;
    private String rol;
    private String nombre;
    private String sedeActiva = "formosa"; // "pba" o "formosa"

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private JSONObject datosPba;
    private JSONObject datosFormosa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institucion);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");
        nombre = getIntent().getStringExtra("nombre");

        vincularVistas();
        DocenteUiHelper.configurarFooterDocente(this, token, rol, nombre);

        if (tvBadgeRolInstitucion != null && rol != null) {
            tvBadgeRolInstitucion.setText(rol.toUpperCase());
        }

        if (savedInstanceState != null) {
            sedeActiva = savedInstanceState.getString("sede_activa", InstitucionManager.getJurisdiccionActiva(this));
        } else {
            sedeActiva = InstitucionManager.getJurisdiccionActiva(this);
        }

        configurarListeners();
        actualizarInterfaz();
        cargarDatosDesdeApi();
        cargarResumenDbEnVivo(InstitucionManager.ID_PBA == getIdDeSedeActiva() ? InstitucionManager.ID_PBA : InstitucionManager.ID_FORMOSA);
    }

    private int getIdDeSedeActiva() {
        return "pba".equals(sedeActiva) ? InstitucionManager.ID_PBA : InstitucionManager.ID_FORMOSA;
    }

    private void vincularVistas() {
        ivVolver = findViewById(R.id.ivVolver);
        tvBadgeRolInstitucion = findViewById(R.id.tvBadgeRolInstitucion);
        btnSedePba = findViewById(R.id.btnSedePba);
        btnSedeFormosa = findViewById(R.id.btnSedeFormosa);

        tvEstadoActivaInst = findViewById(R.id.tvEstadoActivaInst);
        tvDbAlumnosCount = findViewById(R.id.tvDbAlumnosCount);
        tvDbMesasCount = findViewById(R.id.tvDbMesasCount);
        tvDbAvisosCount = findViewById(R.id.tvDbAvisosCount);
        tvDbCursosResumen = findViewById(R.id.tvDbCursosResumen);

        tvNombreInstitucion = findViewById(R.id.tvNombreInstitucion);
        tvDistritoInstitucion = findViewById(R.id.tvDistritoInstitucion);
        tvCueInstitucion = findViewById(R.id.tvCueInstitucion);
        tvNivelInstitucion = findViewById(R.id.tvNivelInstitucion);
        tvRegimenInstitucion = findViewById(R.id.tvRegimenInstitucion);

        tvDetalleCicloBasico = findViewById(R.id.tvDetalleCicloBasico);
        tvDetalleCicloOrientado = findViewById(R.id.tvDetalleCicloOrientado);
        tvTituloCriterio = findViewById(R.id.tvTituloCriterio);
        tvDetalleCriterio = findViewById(R.id.tvDetalleCriterio);

        tvDirectora = findViewById(R.id.tvDirectora);
        tvVicedirector = findViewById(R.id.tvVicedirector);
        tvSecretaria = findViewById(R.id.tvSecretaria);
    }

    private void configurarListeners() {
        ivVolver.setOnClickListener(v -> finish());

        btnSedePba.setOnClickListener(v -> seleccionarYPersistirSede("pba", InstitucionManager.ID_PBA, datosPba));

        btnSedeFormosa.setOnClickListener(v -> seleccionarYPersistirSede("formosa", InstitucionManager.ID_FORMOSA, datosFormosa));
    }

    private void seleccionarYPersistirSede(String juris, int idInst, JSONObject json) {
        sedeActiva = juris;
        if (json != null) {
            InstitucionManager.setInstitucionActiva(this, json);
        } else {
            InstitucionManager.setInstitucionActiva(this, idInst);
        }
        Toast.makeText(
                this,
                "Institución activa: " + InstitucionManager.getNombreCorto(idInst),
                Toast.LENGTH_SHORT
        ).show();
        actualizarInterfaz();
        cargarResumenDbEnVivo(idInst);
    }

    private void cargarDatosDesdeApi() {
        executor.execute(() -> {
            try {
                JSONArray lista = ApiClient.obtenerInstituciones();
                for (int i = 0; i < lista.length(); i++) {
                    JSONObject inst = lista.getJSONObject(i);
                    String juris = inst.optString("jurisdiccion", "");
                    if ("pba".equalsIgnoreCase(juris) || inst.optInt("id_db") == 2) {
                        datosPba = inst;
                    } else {
                        datosFormosa = inst;
                    }
                }
            } catch (Exception ignored) {
                // Si la red falla, la UI se poblará con la información local predeterminada
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                actualizarInterfaz();
            });
        });
    }

    private void cargarResumenDbEnVivo(int idInstSolicitado) {
        if (tvDbAlumnosCount != null) tvDbAlumnosCount.setText("...");
        if (tvDbMesasCount != null) tvDbMesasCount.setText("...");
        if (tvDbAvisosCount != null) tvDbAvisosCount.setText("...");

        executor.execute(() -> {
            int countAlumnos = 0;
            int countMesas = 0;
            int countAvisos = 0;
            boolean ok = true;

            try {
                JSONArray al = ApiClient.listarAlumnos(idInstSolicitado);
                countAlumnos = al.length();
            } catch (Exception e) {
                ok = false;
            }
            try {
                JSONArray ms = ApiClient.listarMesas(idInstSolicitado);
                countMesas = ms.length();
            } catch (Exception ignored) {
            }
            try {
                JSONArray av = ApiClient.listarNotificaciones(idInstSolicitado, "");
                countAvisos = av.length();
            } catch (Exception ignored) {
            }

            final int fa = countAlumnos;
            final int fm = countMesas;
            final int fav = countAvisos;
            final boolean fok = ok;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (getIdDeSedeActiva() != idInstSolicitado) return;
                if (tvDbAlumnosCount != null) tvDbAlumnosCount.setText(fok ? String.valueOf(fa) : "-");
                if (tvDbMesasCount != null) tvDbMesasCount.setText(String.valueOf(fm));
                if (tvDbAvisosCount != null) tvDbAvisosCount.setText(String.valueOf(fav));
            });
        });
    }

    private void actualizarInterfaz() {
        int colorActivoBg = ContextCompat.getColor(this, R.color.color_badge_blue_bg);
        int colorActivoText = ContextCompat.getColor(this, R.color.color_primary);
        int colorInactivoBg = ContextCompat.getColor(this, R.color.bg_surface);
        int colorInactivoText = ContextCompat.getColor(this, R.color.text_muted);

        int idInst = getIdDeSedeActiva();
        JSONObject jsonActivo = "pba".equals(sedeActiva) ? datosPba : datosFormosa;
        String[] cursosInst = InstitucionManager.extraerCursosFormateados(jsonActivo, idInst);
        StringBuilder sbCursos = new StringBuilder();
        int maxMostrar = Math.min(cursosInst.length, 6);
        for (int i = 0; i < maxMostrar; i++) {
            if (i > 0) sbCursos.append(" • ");
            sbCursos.append(cursosInst[i]);
        }
        if (cursosInst.length > maxMostrar) {
            sbCursos.append(" (+").append(cursosInst.length - maxMostrar).append(" más)");
        }
        if (tvDbCursosResumen != null) {
            tvDbCursosResumen.setText("Cursos (" + cursosInst.length + "): " + sbCursos);
        }

        if ("pba".equals(sedeActiva)) {
            if (tvEstadoActivaInst != null) {
                tvEstadoActivaInst.setText("Institución seleccionada: E.E.S. N° 5 (Buenos Aires)");
            }
            // Estilos de los selectores superiores
            btnSedePba.getBackground().mutate().setTint(colorActivoBg);
            btnSedePba.setTextColor(colorActivoText);
            btnSedeFormosa.getBackground().mutate().setTint(colorInactivoBg);
            btnSedeFormosa.setTextColor(colorInactivoText);

            // Datos oficiales PBA
            String nombre = datosPba != null ? datosPba.optString("nombre", "E.E.S. N° 5") : "E.E.S. N° 5";
            String distrito = datosPba != null ? datosPba.optString("subtitulo", "Distrito PUAN | Secundaria") : "Distrito PUAN | Secundaria";
            String cue = datosPba != null ? datosPba.optString("cue", "0604123-00") : "0604123-00";
            String nivel = datosPba != null ? datosPba.optString("nivel", "Secundario Común") : "Secundario Común";

            tvNombreInstitucion.setText(nombre);
            tvDistritoInstitucion.setText(distrito);
            tvCueInstitucion.setText(cue);
            tvNivelInstitucion.setText(nivel);
            tvRegimenInstitucion.setText("Provincia de Buenos Aires • Régimen R.I.T.E. (TEA / TEP / TED)");

            tvDetalleCicloBasico.setText("1°, 2° y 3° Año • Divisiones 1° I, 2° I, 3° I • Turno Mañana");
            tvDetalleCicloOrientado.setText("4°, 5° y 6° Año • Ciencias Sociales y Ciencias Naturales • Turno Tarde");
            tvTituloCriterio.setText("Régimen Institucional de Trayectorias Educativas (R.I.T.E.)");
            tvDetalleCriterio.setText("TEA (Avanzada 7 a 10) • TEP (En Proceso 4 a 6) • TED (Discontinua 1 a 3)");

            tvDirectora.setText("Lic. Marcela Benítez");
            tvVicedirector.setText("Prof. Gustavo Méndez");
            tvSecretaria.setText("Prof. Claudia Fernández • Prof. Darío Almirón");
        } else {
            if (tvEstadoActivaInst != null) {
                tvEstadoActivaInst.setText("Institución seleccionada: EPES N° 19 (Formosa)");
            }
            // Estilos de los selectores superiores
            btnSedeFormosa.getBackground().mutate().setTint(colorActivoBg);
            btnSedeFormosa.setTextColor(colorActivoText);
            btnSedePba.getBackground().mutate().setTint(colorInactivoBg);
            btnSedePba.setTextColor(colorInactivoText);

            // Datos oficiales Formosa
            String nombre = datosFormosa != null ? datosFormosa.optString("nombre", "EPES N° 19 Vuelta de Obligado") : "EPES N° 19 Vuelta de Obligado";
            String distrito = datosFormosa != null ? datosFormosa.optString("subtitulo", "Delegación Zonal Pilcomayo | Laguna Naineck") : "Delegación Zonal Pilcomayo | Laguna Naineck";
            String cue = datosFormosa != null ? datosFormosa.optString("cue", "3400124-00") : "3400124-00";
            String nivel = datosFormosa != null ? datosFormosa.optString("nivel", "Secundaria Común") : "Secundaria Común";

            tvNombreInstitucion.setText(nombre);
            tvDistritoInstitucion.setText(distrito);
            tvCueInstitucion.setText(cue);
            tvNivelInstitucion.setText(nivel);
            tvRegimenInstitucion.setText("Formosa • 3 Trimestres con PAIR y Recuperación Diciembre");

            tvDetalleCicloBasico.setText("1° y 2° Año CBS • Divisiones 1° I, 2° I • Turno Mañana");
            tvDetalleCicloOrientado.setText("3°, 4°, 5° y 6° Año • Bachiller en Economía y Administración");
            tvTituloCriterio.setText("Sistema de Calificación Trimestral con PAIR");
            tvDetalleCriterio.setText("Aprobación con 6 o más • PAIR (Período de Apoyo e Intensificación)");

            tvDirectora.setText("Prof. Nilda Coronel");
            tvVicedirector.setText("Lic. Héctor Giménez");
            tvSecretaria.setText("Secretaría Docente Formosa • Preceptoría General");
        }
    }

    // Persistencia por rotación de pantalla (Requerimiento R6)
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("sede_activa", sedeActiva);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}