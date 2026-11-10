package com.example.boletindigital;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

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
    private String sedeActiva = "pba"; // "pba" o "formosa"

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private JSONObject datosPba;
    private JSONObject datosFormosa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institucion);

        token = getIntent().getStringExtra("token");
        rol = getIntent().getStringExtra("rol");

        vincularVistas();

        if (tvBadgeRolInstitucion != null && rol != null) {
            tvBadgeRolInstitucion.setText(rol.toUpperCase());
        }

        if (savedInstanceState != null) {
            sedeActiva = savedInstanceState.getString("sede_activa", "pba");
        }

        configurarListeners();
        cargarDatosDesdeApi();
    }

    private void vincularVistas() {
        ivVolver = findViewById(R.id.ivVolver);
        tvBadgeRolInstitucion = findViewById(R.id.tvBadgeRolInstitucion);
        btnSedePba = findViewById(R.id.btnSedePba);
        btnSedeFormosa = findViewById(R.id.btnSedeFormosa);

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

        btnSedePba.setOnClickListener(v -> {
            sedeActiva = "pba";
            actualizarInterfaz();
        });

        btnSedeFormosa.setOnClickListener(v -> {
            sedeActiva = "formosa";
            actualizarInterfaz();
        });
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
                // Si la red falla, la UI se poblarÃ¡ con la informaciÃ³n local predeterminada
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                actualizarInterfaz();
            });
        });
    }

    private void actualizarInterfaz() {
        int colorActivoBg = ContextCompat.getColor(this, R.color.color_badge_blue_bg);
        int colorActivoText = ContextCompat.getColor(this, R.color.color_primary);
        int colorInactivoBg = ContextCompat.getColor(this, R.color.bg_surface);
        int colorInactivoText = ContextCompat.getColor(this, R.color.text_muted);

        if ("pba".equals(sedeActiva)) {
            // Estilos de los selectores superiores
            btnSedePba.getBackground().mutate().setTint(colorActivoBg);
            btnSedePba.setTextColor(colorActivoText);
            btnSedeFormosa.getBackground().mutate().setTint(colorInactivoBg);
            btnSedeFormosa.setTextColor(colorInactivoText);

            // Datos oficiales PBA
            String nombre = datosPba != null ? datosPba.optString("nombre", "E.E.S. NÂ° 5") : "E.E.S. NÂ° 5";
            String distrito = datosPba != null ? datosPba.optString("subtitulo", "Distrito PUAN | Secundaria") : "Distrito PUAN | Secundaria";
            String cue = datosPba != null ? datosPba.optString("cue", "0604123-00") : "0604123-00";
            String nivel = datosPba != null ? datosPba.optString("nivel", "Secundario ComÃºn") : "Secundario ComÃºn";

            tvNombreInstitucion.setText(nombre);
            tvDistritoInstitucion.setText(distrito);
            tvCueInstitucion.setText(cue);
            tvNivelInstitucion.setText(nivel);
            tvRegimenInstitucion.setText("Provincia de Buenos Aires â€¢ RÃ©gimen R.I.T.E. (TEA / TEP / TED)");

            tvDetalleCicloBasico.setText("1Â°, 2Â° y 3Â° AÃ±o â€¢ Divisiones 1Â° I, 2Â° I, 3Â° I â€¢ Turno MaÃ±ana");
            tvDetalleCicloOrientado.setText("4Â°, 5Â° y 6Â° AÃ±o â€¢ Ciencias Sociales y Ciencias Naturales â€¢ Turno Tarde");
            tvTituloCriterio.setText("RÃ©gimen Institucional de Trayectorias Educativas (R.I.T.E.)");
            tvDetalleCriterio.setText("TEA (Avanzada 7 a 10) â€¢ TEP (En Proceso 4 a 6) â€¢ TED (Discontinua 1 a 3)");

            tvDirectora.setText("Lic. Marcela BenÃ­tez");
            tvVicedirector.setText("Prof. Gustavo MÃ©ndez");
            tvSecretaria.setText("Prof. Claudia FernÃ¡ndez â€¢ Prof. DarÃ­o AlmirÃ³n");
        } else {
            // Estilos de los selectores superiores
            btnSedeFormosa.getBackground().mutate().setTint(colorActivoBg);
            btnSedeFormosa.setTextColor(colorActivoText);
            btnSedePba.getBackground().mutate().setTint(colorInactivoBg);
            btnSedePba.setTextColor(colorInactivoText);

            // Datos oficiales Formosa
            String nombre = datosFormosa != null ? datosFormosa.optString("nombre", "EPES NÂ° 19 Vuelta de Obligado") : "EPES NÂ° 19 Vuelta de Obligado";
            String distrito = datosFormosa != null ? datosFormosa.optString("subtitulo", "DelegaciÃ³n Zonal Pilcomayo | Laguna Naineck") : "DelegaciÃ³n Zonal Pilcomayo | Laguna Naineck";
            String cue = datosFormosa != null ? datosFormosa.optString("cue", "3400124-00") : "3400124-00";
            String nivel = datosFormosa != null ? datosFormosa.optString("nivel", "Secundaria ComÃºn") : "Secundaria ComÃºn";

            tvNombreInstitucion.setText(nombre);
            tvDistritoInstitucion.setText(distrito);
            tvCueInstitucion.setText(cue);
            tvNivelInstitucion.setText(nivel);
            tvRegimenInstitucion.setText("Formosa â€¢ 3 Trimestres con PAIR y RecuperaciÃ³n Diciembre");

            tvDetalleCicloBasico.setText("1Â° y 2Â° AÃ±o CBS â€¢ Divisiones 1Â° I, 2Â° I â€¢ Turno MaÃ±ana");
            tvDetalleCicloOrientado.setText("3Â°, 4Â°, 5Â° y 6Â° AÃ±o â€¢ Bachiller en EconomÃ­a y AdministraciÃ³n");
            tvTituloCriterio.setText("Sistema de CalificaciÃ³n Trimestral con PAIR");
            tvDetalleCriterio.setText("AprobaciÃ³n con 6 o mÃ¡s â€¢ PAIR (PerÃ­odo de Apoyo e IntensificaciÃ³n)");

            tvDirectora.setText("Prof. Nilda Coronel");
            tvVicedirector.setText("Lic. HÃ©ctor GimÃ©nez");
            tvSecretaria.setText("SecretarÃ­a Docente Formosa â€¢ PreceptorÃ­a General");
        }
    }

    // Persistencia por rotaciÃ³n de pantalla (Requerimiento R6)
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