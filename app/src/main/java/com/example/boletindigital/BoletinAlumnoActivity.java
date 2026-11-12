package com.example.boletindigital;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

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

    private boolean esCuentaGoogle = false;
    private boolean enPadron = true;
    private boolean esPreceptorVista = false;
    private String googleNombre = "";
    private String googleEmail = "";

    private ProgressBar pbCargandoBoletin;

    // Secciones de Pantalla
    private NestedScrollView scrollViewBoletin;
    private LinearLayout layoutSeccionBoletin;
    private LinearLayout layoutSeccionAsistencia;
    private LinearLayout layoutSeccionPrevias;
    private LinearLayout layoutSeccionAvisos;
    private LinearLayout layoutSeccionPerfil;

    // Pestañas de la Barra Inferior (4 pestañas: Boletín, Asistencia, Previas, Avisos)
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

    // Elementos de la Barra Superior y Perfil
    private View btnBadgePerfil;
    private ImageView ivBadgeRolIcon;
    private ImageView ivPerfilAvatar;
    private TextView tvPerfilNombreUsuario;
    private TextView tvPerfilIdentificador;
    private LinearLayout badgePerfilRol;
    private ImageView ivPerfilRolBadgeIcon;
    private TextView tvPerfilBadgeRolTexto;
    private TextView tvPerfilDetalleAlumno;
    private TextView tvPerfilDetalleEscuela;
    private TextView tvPerfilDetalleCurso;
    private TextView tvPerfilDetalleRegimen;
    private TextView tvPerfilDetalleTutor;
    private MaterialButton btnPerfilCerrarSesion;
    private MaterialButton btnPerfilVolverBoletin;

    // Listado de Materias
    private RecyclerView rvMaterias;
    private MateriaAdapter adaptadorMaterias;
    private final List<JSONObject> listaMaterias = new ArrayList<>();

    // Listado de Avisos
    private RecyclerView rvAvisosEstudiante;
    private LinearLayout layoutAvisosFallback;
    private TextView tvBadgeAvisosNav;
    private TextView tvBadgeCantAvisosEst;
    private AvisoEstudianteAdapter adaptadorAvisos;
    private final List<JSONObject> listaAvisos = new ArrayList<>();

    // Listado de Materias Previas y Mesas Evaluadoras
    private RecyclerView rvPreviasEstudiante;
    private TextView tvCantidadPrevias;
    private TextView tvSinPreviasEstudiante;
    private PreviaAlumnoAdapter adaptadorPrevias;
    private final List<JSONObject> listaPrevias = new ArrayList<>();

    private RecyclerView rvMesasEstudiante;
    private TextView tvCantidadMesasEstudiante;
    private TextView tvSinMesasEstudiante;
    private ProgressBar pbCargandoMesasEst;
    private MesaAlumnoAdapter adaptadorMesas;
    private final List<JSONObject> listaMesas = new ArrayList<>();

    private String dniEstudiante = "34567890";
    private String nombreAlumnoActual = "";
    private String cursoAlumnoActual = "";
    private int idInstitucionEstudiante = 1;
    private boolean buscarEnTodasLasSedesAlInicio = false;
    private int pestañaActiva = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boletin_alumno);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    emitirNotificacionPruebaLocal();
                }
        );

        // Vincular vistas de estudiante e institución (vista directa 1-a-1)
        tvInstNombre = findViewById(R.id.tvInstNombre);
        tvRegimen = findViewById(R.id.tvRegimen);
        tvInstDetalle = findViewById(R.id.tvInstDetalle);
        tvNombreEstudiante = findViewById(R.id.tvNombreEstudiante);
        tvDniTutor = findViewById(R.id.tvDniTutor);
        tvBadgeRol = findViewById(R.id.tvBadgeRol);
        ivCerrarSesion = findViewById(R.id.ivCerrarSesion);

        pbCargandoBoletin = findViewById(R.id.pbCargandoBoletin);

        // Secciones
        scrollViewBoletin = findViewById(R.id.scrollViewBoletin);
        layoutSeccionBoletin = findViewById(R.id.layoutSeccionBoletin);
        layoutSeccionAsistencia = findViewById(R.id.layoutSeccionAsistencia);
        layoutSeccionPrevias = findViewById(R.id.layoutSeccionPrevias);
        layoutSeccionAvisos = findViewById(R.id.layoutSeccionAvisos);
        layoutSeccionPerfil = findViewById(R.id.layoutSeccionPerfil);

        // Pestañas inferiores (4 secciones escolares)
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

        // Vistas de la Sección Perfil
        btnBadgePerfil = findViewById(R.id.btnBadgePerfil);
        ivBadgeRolIcon = findViewById(R.id.ivBadgeRolIcon);
        ivPerfilAvatar = findViewById(R.id.ivPerfilAvatar);
        tvPerfilNombreUsuario = findViewById(R.id.tvPerfilNombreUsuario);
        tvPerfilIdentificador = findViewById(R.id.tvPerfilIdentificador);
        badgePerfilRol = findViewById(R.id.badgePerfilRol);
        ivPerfilRolBadgeIcon = findViewById(R.id.ivPerfilRolBadgeIcon);
        tvPerfilBadgeRolTexto = findViewById(R.id.tvPerfilBadgeRolTexto);
        tvPerfilDetalleAlumno = findViewById(R.id.tvPerfilDetalleAlumno);
        tvPerfilDetalleEscuela = findViewById(R.id.tvPerfilDetalleEscuela);
        tvPerfilDetalleCurso = findViewById(R.id.tvPerfilDetalleCurso);
        tvPerfilDetalleRegimen = findViewById(R.id.tvPerfilDetalleRegimen);
        tvPerfilDetalleTutor = findViewById(R.id.tvPerfilDetalleTutor);
        btnPerfilCerrarSesion = findViewById(R.id.btnPerfilCerrarSesion);
        btnPerfilVolverBoletin = findViewById(R.id.btnPerfilVolverBoletin);

        // RecyclerView de Materias
        rvMaterias = findViewById(R.id.rvMaterias);
        if (rvMaterias != null) {
            rvMaterias.setLayoutManager(new LinearLayoutManager(this));
            adaptadorMaterias = new MateriaAdapter(listaMaterias);
            rvMaterias.setAdapter(adaptadorMaterias);
        }

        // RecyclerView de Avisos
        rvAvisosEstudiante = findViewById(R.id.rvAvisosEstudiante);
        layoutAvisosFallback = findViewById(R.id.layoutAvisosFallback);
        tvBadgeAvisosNav = findViewById(R.id.tvBadgeAvisosNav);
        tvBadgeCantAvisosEst = findViewById(R.id.tvBadgeCantAvisosEst);
        if (rvAvisosEstudiante != null) {
            rvAvisosEstudiante.setLayoutManager(new LinearLayoutManager(this));
            adaptadorAvisos = new AvisoEstudianteAdapter(listaAvisos);
            rvAvisosEstudiante.setAdapter(adaptadorAvisos);
        }

        // RecyclerViews de Previas y Mesas Evaluadoras
        rvPreviasEstudiante = findViewById(R.id.rvPreviasEstudiante);
        tvCantidadPrevias = findViewById(R.id.tvCantidadPrevias);
        tvSinPreviasEstudiante = findViewById(R.id.tvSinPreviasEstudiante);
        if (rvPreviasEstudiante != null) {
            rvPreviasEstudiante.setLayoutManager(new LinearLayoutManager(this));
            adaptadorPrevias = new PreviaAlumnoAdapter(listaPrevias);
            rvPreviasEstudiante.setAdapter(adaptadorPrevias);
        }

        rvMesasEstudiante = findViewById(R.id.rvMesasEstudiante);
        tvCantidadMesasEstudiante = findViewById(R.id.tvCantidadMesasEstudiante);
        tvSinMesasEstudiante = findViewById(R.id.tvSinMesasEstudiante);
        pbCargandoMesasEst = findViewById(R.id.pbCargandoMesasEst);
        if (rvMesasEstudiante != null) {
            rvMesasEstudiante.setLayoutManager(new LinearLayoutManager(this));
            adaptadorMesas = new MesaAlumnoAdapter(listaMesas, () -> dniEstudiante, this::gestionarInscripcionMesa);
            rvMesasEstudiante.setAdapter(adaptadorMesas);
        }

        // Inicializar institución activa y DNI desde Intent o estado guardado
        int idInstIntent = getIntent().getIntExtra("id_institucion", -1);
        idInstitucionEstudiante = idInstIntent > 0
                ? idInstIntent
                : InstitucionManager.getIdInstitucionActiva(this);
        boolean dniExplicito = getIntent().getBooleanExtra("dni_explicito", false);

        String dniIntent = getIntent().getStringExtra("dni");
        if (dniIntent != null && dniIntent.trim().matches("\\d+")) {
            dniEstudiante = dniIntent.trim();
            buscarEnTodasLasSedesAlInicio = dniExplicito && idInstIntent <= 0;
        } else {
            dniEstudiante = InstitucionManager.getDniAlumnoPorDefecto(idInstitucionEstudiante);
            buscarEnTodasLasSedesAlInicio = false;
        }

        if (savedInstanceState != null) {
            dniEstudiante = savedInstanceState.getString("dniEstudiante", dniEstudiante);
            idInstitucionEstudiante = savedInstanceState.getInt("idInstitucionEstudiante", idInstitucionEstudiante);
            buscarEnTodasLasSedesAlInicio = false;
        }

        esCuentaGoogle = getIntent().getBooleanExtra("es_google_cuenta", false);
        enPadron = getIntent().getBooleanExtra("en_padron", true);
        String gNom = getIntent().getStringExtra("google_nombre");
        if (gNom == null || gNom.trim().isEmpty()) {
            gNom = getIntent().getStringExtra("nombre");
        }
        googleNombre = gNom != null ? gNom.trim() : "";
        String gEmail = getIntent().getStringExtra("google_email");
        googleEmail = gEmail != null ? gEmail.trim() : "";

        String rolIntent = getIntent().getStringExtra("rol");
        String rolLower = rolIntent != null ? rolIntent.trim().toLowerCase() : "";
        esPreceptorVista = "preceptor".equals(rolLower) || "docente".equals(rolLower)
                || "administrador".equals(rolLower) || "admin".equals(rolLower);

        if (tvBadgeRol != null) {
            if (esPreceptorVista) {
                tvBadgeRol.setText("Vista Preceptor");
            } else if ("tutor".equals(rolLower) || "padre".equals(rolLower) || esCuentaGoogle) {
                tvBadgeRol.setText("Tutor");
                if (ivBadgeRolIcon != null) {
                    ivBadgeRolIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_group));
                }
            } else {
                tvBadgeRol.setText("Alumno");
                if (ivBadgeRolIcon != null) {
                    ivBadgeRolIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_person));
                }
            }
        }

        if (esCuentaGoogle && !esPreceptorVista) {
            aplicarEncabezadoGoogleSiCorresponde("4° Única");
        }

        // Configurar Navegación Inferior (1: Boletín, 2: Asistencia, 3: Previas, 4: Avisos)
        if (tabNavBoletin != null) tabNavBoletin.setOnClickListener(v -> activarPestaña(1));
        if (tabNavAsistencia != null) tabNavAsistencia.setOnClickListener(v -> activarPestaña(2));
        if (tabNavPrevias != null) tabNavPrevias.setOnClickListener(v -> activarPestaña(3));
        if (tabNavAvisos != null) tabNavAvisos.setOnClickListener(v -> activarPestaña(4));

        // Acceso al Perfil desde el Badge del Header
        if (btnBadgePerfil != null) btnBadgePerfil.setOnClickListener(v -> activarPestaña(5));
        if (btnPerfilCerrarSesion != null) btnPerfilCerrarSesion.setOnClickListener(v -> mostrarDialogoSalir());
        if (btnPerfilVolverBoletin != null) btnPerfilVolverBoletin.setOnClickListener(v -> activarPestaña(1));

        // Botón interactivo para probar notificaciones push (Firebase FCM - R4)
        View btnProbarNotificacionBoletin = findViewById(R.id.btnProbarNotificacionBoletin);
        if (btnProbarNotificacionBoletin != null) {
            btnProbarNotificacionBoletin.setOnClickListener(v -> probarNotificacionConPermiso());
        }

        // Botón Perfil del Header (reemplazo de flecha por icono de perfil)
        if (ivCerrarSesion != null) {
            ivCerrarSesion.setOnClickListener(v -> {
                if (esPreceptorVista) {
                    finish();
                } else if (pestañaActiva != 5) {
                    activarPestaña(5);
                } else {
                    mostrarDialogoSalir();
                }
            });
        }

        // Botón atrás del celular: vuelve al Boletín si está en Asistencia/Previas/Avisos/Perfil
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pestañaActiva != 1) {
                    activarPestaña(1);
                } else {
                    mostrarDialogoSalir();
                }
            }
        });

        // Restaurar pestaña o usar tab de Intent
        if (savedInstanceState != null) {
            pestañaActiva = savedInstanceState.getInt("pestañaActiva", 1);
            activarPestaña(pestañaActiva);
        } else {
            int tabInicial = getIntent().getIntExtra("tab", 1);
            if (tabInicial >= 1 && tabInicial <= 5) {
                activarPestaña(tabInicial);
            } else {
                activarPestaña(1);
            }
        }

        // Cargar datos del Boletín, Asistencia completa y Avisos desde Railway
        cargarDatosBoletin();
        cargarAvisosDesdeApi();
    }

    private void aplicarEncabezadoGoogleSiCorresponde(String cursoRef) {
        if (!esCuentaGoogle || esPreceptorVista) return;
        String tutorNombre = !googleNombre.isEmpty() ? googleNombre : "Tutor Google";
        String emailMostrar = !googleEmail.isEmpty() ? googleEmail : "Cuenta Google";
        if (tvDniTutor != null) {
            tvDniTutor.setText("DNI " + dniEstudiante + " • Tutor: " + tutorNombre + " (" + emailMostrar + ")");
        }
        if (tvBadgeRol != null) {
            tvBadgeRol.setText("Tutor");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra("tab")) {
            int tab = intent.getIntExtra("tab", 1);
            if (tab >= 1 && tab <= 4) {
                activarPestaña(tab);
            }
        }
    }

    private void probarNotificacionConPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        emitirNotificacionPruebaLocal();
    }

    private void emitirNotificacionPruebaLocal() {
        BoletinFirebaseMessagingService.lanzarNotificacion(
                this,
                "Comunicado • " + InstitucionManager.getNombreCorto(idInstitucionEstudiante),
                "Notificación institucional de prueba activa para " + InstitucionManager.getNombreEscuelaActiva(this) + ".",
                "avisos"
        );
        Toast.makeText(this, "Notificación institucional emitida en este celular", Toast.LENGTH_SHORT).show();
    }

    private void activarPestaña(int numeroPestaña) {
        this.pestañaActiva = numeroPestaña;
        int colorPrimary = ContextCompat.getColor(this, R.color.color_primary);
        int colorMuted = ContextCompat.getColor(this, R.color.text_muted);

        if (layoutSeccionBoletin != null) layoutSeccionBoletin.setVisibility(View.GONE);
        if (layoutSeccionAsistencia != null) layoutSeccionAsistencia.setVisibility(View.GONE);
        if (layoutSeccionPrevias != null) layoutSeccionPrevias.setVisibility(View.GONE);
        if (layoutSeccionAvisos != null) layoutSeccionAvisos.setVisibility(View.GONE);
        if (layoutSeccionPerfil != null) layoutSeccionPerfil.setVisibility(View.GONE);

        if (ivNavBoletin != null) ivNavBoletin.setColorFilter(colorMuted);
        if (tvNavBoletin != null) tvNavBoletin.setTextColor(colorMuted);
        if (ivNavAsistencia != null) ivNavAsistencia.setColorFilter(colorMuted);
        if (tvNavAsistencia != null) tvNavAsistencia.setTextColor(colorMuted);
        if (ivNavPrevias != null) ivNavPrevias.setColorFilter(colorMuted);
        if (tvNavPrevias != null) tvNavPrevias.setTextColor(colorMuted);
        if (ivNavAvisos != null) ivNavAvisos.setColorFilter(colorMuted);
        if (tvNavAvisos != null) tvNavAvisos.setTextColor(colorMuted);
        if (ivCerrarSesion != null) ivCerrarSesion.setColorFilter(numeroPestaña == 5 ? colorPrimary : ContextCompat.getColor(this, R.color.text_main));

        if (scrollViewBoletin != null) {
            scrollViewBoletin.post(() -> scrollViewBoletin.smoothScrollTo(0, 0));
        }

        switch (numeroPestaña) {
            case 2:
                if (layoutSeccionAsistencia != null) layoutSeccionAsistencia.setVisibility(View.VISIBLE);
                if (ivNavAsistencia != null) ivNavAsistencia.setColorFilter(colorPrimary);
                if (tvNavAsistencia != null) tvNavAsistencia.setTextColor(colorPrimary);
                break;
            case 3:
                if (layoutSeccionPrevias != null) layoutSeccionPrevias.setVisibility(View.VISIBLE);
                if (ivNavPrevias != null) ivNavPrevias.setColorFilter(colorPrimary);
                if (tvNavPrevias != null) tvNavPrevias.setTextColor(colorPrimary);
                cargarPreviasDesdeApi();
                cargarMesasDesdeApi();
                break;
            case 4:
                if (layoutSeccionAvisos != null) layoutSeccionAvisos.setVisibility(View.VISIBLE);
                if (ivNavAvisos != null) ivNavAvisos.setColorFilter(colorPrimary);
                if (tvNavAvisos != null) tvNavAvisos.setTextColor(colorPrimary);
                break;
            case 5:
                if (layoutSeccionPerfil != null) layoutSeccionPerfil.setVisibility(View.VISIBLE);
                actualizarDatosPerfil();
                break;
            case 1:
            default:
                this.pestañaActiva = 1;
                if (layoutSeccionBoletin != null) layoutSeccionBoletin.setVisibility(View.VISIBLE);
                if (ivNavBoletin != null) ivNavBoletin.setColorFilter(colorPrimary);
                if (tvNavBoletin != null) tvNavBoletin.setTextColor(colorPrimary);
                break;
        }
    }

    private void actualizarDatosPerfil() {
        String rolIntent = getIntent().getStringExtra("rol");
        String rolLower = rolIntent != null ? rolIntent.trim().toLowerCase() : "";
        boolean esTutor = "tutor".equals(rolLower) || "padre".equals(rolLower) || esCuentaGoogle;
        String nombreSesion = getIntent().getStringExtra("nombre");
        if (nombreSesion == null || nombreSesion.trim().isEmpty()) {
            nombreSesion = esTutor ? (!googleNombre.isEmpty() ? googleNombre : "Tutor / Familiar") : nombreAlumnoActual;
        }

        if (tvPerfilNombreUsuario != null) {
            tvPerfilNombreUsuario.setText(nombreSesion);
        }

        if (tvPerfilIdentificador != null) {
            if (esCuentaGoogle && !googleEmail.isEmpty()) {
                tvPerfilIdentificador.setText("Cuenta Google: " + googleEmail);
            } else if (esTutor) {
                tvPerfilIdentificador.setText("Acceso Tutor: " + (!googleEmail.isEmpty() ? googleEmail : "Correo Oficial"));
            } else {
                tvPerfilIdentificador.setText("DNI del Alumno: " + dniEstudiante);
            }
        }

        int colorPrimary = ContextCompat.getColor(this, R.color.color_primary);
        int colorSuccessBg = ContextCompat.getColor(this, R.color.color_tea_bg);
        int colorSuccessText = ContextCompat.getColor(this, R.color.color_tea_text);
        int colorBadgeBlueBg = ContextCompat.getColor(this, R.color.color_badge_blue_bg);

        if (badgePerfilRol != null) {
            badgePerfilRol.setBackgroundTintList(ColorStateList.valueOf(esTutor ? colorBadgeBlueBg : colorSuccessBg));
        }
        if (ivPerfilRolBadgeIcon != null) {
            ivPerfilRolBadgeIcon.setImageDrawable(ContextCompat.getDrawable(this, esTutor ? R.drawable.ic_group : R.drawable.ic_person));
            ivPerfilRolBadgeIcon.setImageTintList(ColorStateList.valueOf(esTutor ? colorPrimary : colorSuccessText));
        }
        if (tvPerfilBadgeRolTexto != null) {
            tvPerfilBadgeRolTexto.setText(esTutor ? "TUTOR RESPONSABLE" : "ALUMNO REGULAR");
            tvPerfilBadgeRolTexto.setTextColor(esTutor ? colorPrimary : colorSuccessText);
        }

        if (ivPerfilAvatar != null) {
            ivPerfilAvatar.setImageDrawable(ContextCompat.getDrawable(this, esTutor ? R.drawable.ic_group : R.drawable.ic_school));
            ivPerfilAvatar.setImageTintList(ColorStateList.valueOf(esTutor ? colorPrimary : colorSuccessText));
        }

        if (tvPerfilDetalleAlumno != null) {
            tvPerfilDetalleAlumno.setText(nombreAlumnoActual + " (DNI " + dniEstudiante + ")");
        }
        if (tvPerfilDetalleEscuela != null) {
            String escuela = (tvInstNombre != null && !tvInstNombre.getText().toString().isEmpty())
                    ? tvInstNombre.getText().toString()
                    : InstitucionManager.getNombreCorto(idInstitucionEstudiante);
            tvPerfilDetalleEscuela.setText(escuela);
        }
        if (tvPerfilDetalleCurso != null) {
            tvPerfilDetalleCurso.setText(cursoAlumnoActual);
        }
        if (tvPerfilDetalleRegimen != null) {
            String reg = (tvRegimen != null && !tvRegimen.getText().toString().isEmpty())
                    ? tvRegimen.getText().toString()
                    : (idInstitucionEstudiante == InstitucionManager.ID_PBA ? "Secundaria Cuatrimestral (R.I.T.E.)" : "Secundaria 3 Trimestres (PAIR)");
            tvPerfilDetalleRegimen.setText(reg);
        }
        if (tvPerfilDetalleTutor != null) {
            String tutorInfo = (tvDniTutor != null && tvDniTutor.getText().toString().contains("Tutor: "))
                    ? tvDniTutor.getText().toString().substring(tvDniTutor.getText().toString().indexOf("Tutor: ") + 7)
                    : (idInstitucionEstudiante == InstitucionManager.ID_PBA ? "Pleininger Cristian" : "Gómez Pedro");
            tvPerfilDetalleTutor.setText(tutorInfo);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pestañaActiva", pestañaActiva);
        outState.putString("dniEstudiante", dniEstudiante);
        outState.putInt("idInstitucionEstudiante", idInstitucionEstudiante);
    }

    private void cargarAvisosDesdeApi() {
        final int idInstConsulta = idInstitucionEstudiante;
        final String dniConsulta = dniEstudiante;
        executor.execute(() -> {
            try {
                JSONArray res = ApiClient.listarNotificaciones(idInstConsulta, dniConsulta);
                List<JSONObject> parsed = new ArrayList<>();
                for (int i = 0; i < res.length(); i++) {
                    JSONObject item = res.getJSONObject(i);
                    String estado = item.optString("estado", "activa");
                    if (!"archivada".equalsIgnoreCase(estado)) {
                        parsed.add(item);
                    }
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (idInstConsulta != idInstitucionEstudiante) return;

                    if (rvAvisosEstudiante != null && adaptadorAvisos != null) {
                        listaAvisos.clear();
                        listaAvisos.addAll(parsed);
                        adaptadorAvisos.notifyDataSetChanged();

                        if (listaAvisos.isEmpty()) {
                            rvAvisosEstudiante.setVisibility(View.GONE);
                            if (layoutAvisosFallback != null) {
                                layoutAvisosFallback.setVisibility(View.VISIBLE);
                            }
                            if (tvBadgeAvisosNav != null) {
                                tvBadgeAvisosNav.setVisibility(View.GONE);
                            }
                            if (tvBadgeCantAvisosEst != null) {
                                tvBadgeCantAvisosEst.setText("0 Avisos");
                            }
                        } else {
                            rvAvisosEstudiante.setVisibility(View.VISIBLE);
                            if (layoutAvisosFallback != null) {
                                layoutAvisosFallback.setVisibility(View.GONE);
                            }
                            if (tvBadgeAvisosNav != null) {
                                tvBadgeAvisosNav.setText(String.valueOf(listaAvisos.size()));
                                tvBadgeAvisosNav.setVisibility(View.VISIBLE);
                            }
                            if (tvBadgeCantAvisosEst != null) {
                                tvBadgeCantAvisosEst.setText(listaAvisos.size() == 1 ? "1 Aviso" : listaAvisos.size() + " Avisos");
                            }
                        }
                    }
                });
            } catch (Exception ignored) {
                // Conserva los avisos institucionales de respaldo cuando no hay conexión
            }
        });
    }

    private void cargarDatosBoletin() {
        if (pbCargandoBoletin != null) {
            pbCargandoBoletin.setVisibility(View.VISIBLE);
        }

        final String dniConsulta = dniEstudiante;
        final int idInstConsulta = buscarEnTodasLasSedesAlInicio ? 0 : idInstitucionEstudiante;

        executor.execute(() -> {
            try {
                JSONObject jsonBoletin = ApiClient.obtenerBoletin(dniConsulta, idInstConsulta);
                JSONArray jsonAsistencias = null;
                try {
                    jsonAsistencias = ApiClient.obtenerAsistenciasPorDni(dniConsulta, idInstConsulta);
                } catch (Exception ignored) {}

                final JSONArray asistenciasFinal = jsonAsistencias;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!dniConsulta.equals(dniEstudiante)) return;

                    if (pbCargandoBoletin != null) {
                        pbCargandoBoletin.setVisibility(View.GONE);
                    }

                    mostrarDatosEnPantalla(jsonBoletin, asistenciasFinal);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!dniConsulta.equals(dniEstudiante)) return;

                    if (pbCargandoBoletin != null) {
                        pbCargandoBoletin.setVisibility(View.GONE);
                    }

                    Toast.makeText(this, "Modo sin conexión - Visualizando boletín institucional", Toast.LENGTH_SHORT).show();
                    cargarDatosDemostracion();
                });
            }
        });
    }

    private void mostrarDatosEnPantalla(JSONObject datos, JSONArray asistenciasApi) {
        try {
            // Estudiante
            if (datos.has("estudiante")) {
                JSONObject est = datos.getJSONObject("estudiante");
                nombreAlumnoActual = est.optString("nombre", "PLEININGER, URIEL LUCIANO");
                cursoAlumnoActual = est.optString("curso", "4° Única");
                tvNombreEstudiante.setText(nombreAlumnoActual);
                String dni = est.optString("dni", dniEstudiante);
                String tutor = est.optString("tutor", "PLEININGER, CRISTIAN");

                tvInstDetalle.setText("Secundario | " + cursoAlumnoActual);
                tvDniTutor.setText("DNI " + dni + " • Tutor: " + tutor);
            }
            aplicarEncabezadoGoogleSiCorresponde(cursoAlumnoActual);

            // Institución
            if (datos.has("institucion")) {
                JSONObject inst = datos.getJSONObject("institucion");
                String nombreInst = inst.optString("nombre", "E.E.S. N° 5");
                tvInstNombre.setText(nombreInst);
                String regimen = inst.optString("regimen", "Cuatrimestral").trim();
                if (regimen.isEmpty()) {
                    regimen = "Cuatrimestral";
                }
                if (!regimen.toUpperCase().contains("R.I.T.E") && !regimen.toUpperCase().contains("PAIR")) {
                    regimen = regimen + (regimen.toLowerCase().contains("trim") ? " (PAIR)" : " (R.I.T.E.)");
                }
                tvRegimen.setText(regimen);

                int instAnterior = idInstitucionEstudiante;
                int idInstServidor = inst.optInt("id_db", 0);
                String jur = datos.optString("jurisdiccion", "").trim().toLowerCase();
                if (idInstServidor > 0) {
                    idInstitucionEstudiante = idInstServidor;
                } else if ("pba".equals(jur)) {
                    idInstitucionEstudiante = InstitucionManager.ID_PBA;
                } else if ("formosa".equals(jur)) {
                    idInstitucionEstudiante = InstitucionManager.ID_FORMOSA;
                } else {
                    String instUp = nombreInst.toUpperCase();
                    idInstitucionEstudiante = (instUp.contains("E.E.S") || instUp.contains("PUAN"))
                            ? InstitucionManager.ID_PBA
                            : InstitucionManager.ID_FORMOSA;
                }
                InstitucionManager.setInstitucionActiva(this, idInstitucionEstudiante);
                if (buscarEnTodasLasSedesAlInicio && instAnterior != idInstitucionEstudiante) {
                    buscarEnTodasLasSedesAlInicio = false;
                    cargarAvisosDesdeApi();
                }
                buscarEnTodasLasSedesAlInicio = false;
            }

            // Asistencia: priorizar el acumulado por períodos de asistencia.php si devolvió filas
            int totalClases = 0;
            int presentes = 0;
            int totalFaltas = 0;
            int justificadas = 0;

            if (asistenciasApi != null && asistenciasApi.length() > 0) {
                for (int i = 0; i < asistenciasApi.length(); i++) {
                    JSONObject row = asistenciasApi.optJSONObject(i);
                    if (row == null) continue;
                    totalClases += Math.max(0, row.optInt("dias_habiles", 0));
                    presentes += Math.max(0, row.optInt("asistencias", 0));
                    totalFaltas += Math.max(0, row.optInt("inasistencias", 0));
                    justificadas += Math.max(0, row.optInt("justificadas", 0));
                }
            } else if (datos.has("asistencia")) {
                JSONObject asis = datos.getJSONObject("asistencia");
                totalClases = Math.max(0, asis.optInt("total_clases", 0));
                presentes = Math.max(0, asis.optInt("presentes", asis.optInt("presentu", 0)));
                totalFaltas = Math.max(0, asis.optInt("ausentes_injustificados", 0));
                justificadas = Math.max(0, asis.optInt("ausentes_justificados", 0));
            }

            if (justificadas > totalFaltas) {
                justificadas = totalFaltas;
            }
            int injustificadas = Math.max(0, totalFaltas - justificadas);

            actualizarResumenYSeccionAsistencia(totalClases, presentes, justificadas, injustificadas, totalFaltas);

            // Materias
            listaMaterias.clear();
            if (datos.has("materias")) {
                JSONArray arrayMaterias = datos.getJSONArray("materias");
                for (int i = 0; i < arrayMaterias.length(); i++) {
                    listaMaterias.add(arrayMaterias.getJSONObject(i));
                }
            }

            if (adaptadorMaterias != null) {
                adaptadorMaterias.notifyDataSetChanged();
            }

            // Materias Previas desde bloqueInferior.items
            listaPrevias.clear();
            JSONObject bloqueInferior = datos.optJSONObject("bloqueInferior");
            if (bloqueInferior != null && bloqueInferior.has("items")) {
                JSONArray arrayPrevias = bloqueInferior.optJSONArray("items");
                if (arrayPrevias != null) {
                    for (int i = 0; i < arrayPrevias.length(); i++) {
                        JSONObject itemPrevia = arrayPrevias.optJSONObject(i);
                        if (itemPrevia != null) listaPrevias.add(itemPrevia);
                    }
                }
            }
            actualizarVistaPrevias();
            cargarMesasDesdeApi();

        } catch (Exception e) {
            cargarDatosDemostracion();
        }
    }

    private void cargarDatosDemostracion() {
        if (idInstitucionEstudiante == InstitucionManager.ID_PBA) {
            nombreAlumnoActual = "PLEININGER, URIEL LUCIANO";
            cursoAlumnoActual = "4° Única";
            tvInstNombre.setText("E.E.S. N° 5");
            tvRegimen.setText("Cuatrimestral (R.I.T.E.)");
            tvInstDetalle.setText("Distrito: PUAN | Secundario | 4° Única");
            tvNombreEstudiante.setText(nombreAlumnoActual);
            tvDniTutor.setText("DNI 48763736 • Tutor: PLEININGER, CRISTIAN");
            actualizarResumenYSeccionAsistencia(82, 65, 0, 17, 17);
        } else {
            nombreAlumnoActual = "GÓMEZ, MATEO";
            cursoAlumnoActual = "2° III";
            tvInstNombre.setText("EPES N° 19 Vuelta de Obligado");
            tvRegimen.setText("Trimestral (PAIR)");
            tvInstDetalle.setText("Laguna Naineck | Secundario | 2° III");
            tvNombreEstudiante.setText(nombreAlumnoActual);
            tvDniTutor.setText("DNI 34567890 • Tutor: GÓMEZ, PEDRO");
            actualizarResumenYSeccionAsistencia(96, 86, 1, 9, 10);
        }

        aplicarEncabezadoGoogleSiCorresponde(cursoAlumnoActual);
        cargarMateriasDemostracion();
        actualizarVistaPrevias();
        actualizarVistaMesas();
    }

    private void actualizarResumenYSeccionAsistencia(int habiles, int presentes, int justif, int injust, int totalFaltas) {
        int porcentaje = habiles > 0
                ? (int) Math.max(0, Math.min(100, Math.round(((double) presentes / habiles) * 100)))
                : 100;

        int colorSuccess = ContextCompat.getColor(this, R.color.color_success);
        int colorSuccessBg = ContextCompat.getColor(this, R.color.color_tea_bg);
        int colorSuccessText = ContextCompat.getColor(this, R.color.color_tea_text);
        int colorWarning = ContextCompat.getColor(this, R.color.color_warning);
        int colorWarningBg = ContextCompat.getColor(this, R.color.color_warning_bg);
        int colorWarningBorder = ContextCompat.getColor(this, R.color.color_warning_border);
        int colorDanger = ContextCompat.getColor(this, R.color.color_danger);
        int colorDangerBg = ContextCompat.getColor(this, R.color.color_danger_bg);
        int colorPrimary = ContextCompat.getColor(this, R.color.color_primary);

        // Actualizar métricas detalladas de la Sección 2 (Asistencia completa)
        TextView tvSecAsisDiasHabiles = findViewById(R.id.tvSecAsisDiasHabiles);
        TextView tvSecAsisPresentes = findViewById(R.id.tvSecAsisPresentes);
        TextView tvSecAsisJustificadas = findViewById(R.id.tvSecAsisJustificadas);
        TextView tvSecAsisInjustificadas = findViewById(R.id.tvSecAsisInjustificadas);
        TextView tvSecAsisPorcentaje = findViewById(R.id.tvSecAsisPorcentaje);
        ProgressBar pbSecAsisPorcentaje = findViewById(R.id.pbSecAsisPorcentaje);

        if (tvSecAsisDiasHabiles != null) tvSecAsisDiasHabiles.setText(habiles + " Días");
        if (tvSecAsisPresentes != null) tvSecAsisPresentes.setText(presentes + " Días");
        if (tvSecAsisJustificadas != null) tvSecAsisJustificadas.setText(justif + " Faltas");
        if (tvSecAsisInjustificadas != null) tvSecAsisInjustificadas.setText(injust + " Faltas");
        if (tvSecAsisPorcentaje != null) {
            tvSecAsisPorcentaje.setText(porcentaje + "%");
            tvSecAsisPorcentaje.setTextColor(porcentaje >= 85 ? colorSuccess : (porcentaje >= 75 ? colorWarning : colorDanger));
        }
        if (pbSecAsisPorcentaje != null) {
            pbSecAsisPorcentaje.setProgress(porcentaje);
            pbSecAsisPorcentaje.setProgressTintList(ColorStateList.valueOf(colorPrimary));
        }

        // Actualizar badges y banner de alerta según reglas institucionales (15 alerta / 20 límite)
        TextView tvSecAsisBadgeEstado = findViewById(R.id.tvSecAsisBadgeEstado);
        MaterialCardView cardSecAsisAlerta = findViewById(R.id.cardSecAsisAlerta);
        ImageView ivSecAsisAlertaIcon = findViewById(R.id.ivSecAsisAlertaIcon);
        TextView tvSecAsisAlertaMensaje = findViewById(R.id.tvSecAsisAlertaMensaje);

        if (totalFaltas <= 15) {
            int margen = 15 - totalFaltas;
            if (tvSecAsisBadgeEstado != null) {
                tvSecAsisBadgeEstado.setText("Asistencia Regular");
                tvSecAsisBadgeEstado.setBackgroundTintList(ColorStateList.valueOf(colorSuccessBg));
                tvSecAsisBadgeEstado.setTextColor(colorSuccessText);
            }
            if (cardSecAsisAlerta != null) {
                cardSecAsisAlerta.setCardBackgroundColor(colorSuccessBg);
                cardSecAsisAlerta.setStrokeColor(colorSuccess);
            }
            if (ivSecAsisAlertaIcon != null) ivSecAsisAlertaIcon.setColorFilter(colorSuccessText);
            if (tvSecAsisAlertaMensaje != null) {
                tvSecAsisAlertaMensaje.setTextColor(colorSuccessText);
                tvSecAsisAlertaMensaje.setText("El estudiante acumula " + totalFaltas + " faltas en total. Mantiene regularidad normal (restan " + margen + " faltas antes de la alerta de 15).");
            }
        } else if (totalFaltas <= 20) {
            int margen = 20 - totalFaltas;
            if (tvSecAsisBadgeEstado != null) {
                tvSecAsisBadgeEstado.setText("Alerta Preventiva");
                tvSecAsisBadgeEstado.setBackgroundTintList(ColorStateList.valueOf(colorWarningBg));
                tvSecAsisBadgeEstado.setTextColor(colorWarning);
            }
            if (cardSecAsisAlerta != null) {
                cardSecAsisAlerta.setCardBackgroundColor(colorWarningBg);
                cardSecAsisAlerta.setStrokeColor(colorWarningBorder);
            }
            if (ivSecAsisAlertaIcon != null) ivSecAsisAlertaIcon.setColorFilter(colorWarning);
            if (tvSecAsisAlertaMensaje != null) {
                tvSecAsisAlertaMensaje.setTextColor(colorWarning);
                tvSecAsisAlertaMensaje.setText("¡Atención! El estudiante acumula " + totalFaltas + " faltas (zona de alerta). Restan " + margen + " falta(s) antes del límite de reincorporación (20 faltas).");
            }
        } else {
            if (tvSecAsisBadgeEstado != null) {
                tvSecAsisBadgeEstado.setText("Límite Excedido");
                tvSecAsisBadgeEstado.setBackgroundTintList(ColorStateList.valueOf(colorDangerBg));
                tvSecAsisBadgeEstado.setTextColor(colorDanger);
            }
            if (cardSecAsisAlerta != null) {
                cardSecAsisAlerta.setCardBackgroundColor(colorDangerBg);
                cardSecAsisAlerta.setStrokeColor(colorDanger);
            }
            if (ivSecAsisAlertaIcon != null) ivSecAsisAlertaIcon.setColorFilter(colorDanger);
            if (tvSecAsisAlertaMensaje != null) {
                tvSecAsisAlertaMensaje.setTextColor(colorDanger);
                tvSecAsisAlertaMensaje.setText("Límite excedido: El estudiante acumula " + totalFaltas + " faltas (superó el máximo de 20). Requiere solicitud formal de reincorporación.");
            }
        }
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
        if (adaptadorMaterias != null) {
            adaptadorMaterias.notifyDataSetChanged();
        }
    }

    private void mostrarDialogoSalir() {
        if (esPreceptorVista) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Deseás salir del portal y volver a la pantalla de inicio?")
                .setPositiveButton("Salir", (dialog, which) -> {
                    try {
                        FirebaseAuth.getInstance().signOut();
                    } catch (Exception ignored) {}
                    try {
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build();
                        GoogleSignIn.getClient(this, gso).signOut();
                    } catch (Exception ignored) {}

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
            String periodoRaw = materia.optString("periodo", "").trim();
            String periodoMostrar = periodoRaw.isEmpty()
                    ? "1° Cuat."
                    : ("1".equals(periodoRaw) ? "1° Cuat." : periodoRaw);

            holder.tvNombreMateria.setText(nombre);
            holder.tvBadgeValoracion.setText(valoracion);
            holder.tvNotaNumerica.setText(String.valueOf(nota));
            holder.tvSubtituloMateria.setText("Ciclo 2026 • " + periodoMostrar);

            boolean esPositiva = "TEA".equalsIgnoreCase(valoracion)
                    || "SO".equalsIgnoreCase(valoracion)
                    || "MB".equalsIgnoreCase(valoracion)
                    || "B".equalsIgnoreCase(valoracion)
                    || (nota >= 7 && !"TEP".equalsIgnoreCase(valoracion) && !"TED".equalsIgnoreCase(valoracion));

            if (esPositiva) {
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

    // =========================================================================
    // ADAPTADOR PARA COMUNICADOS OFICIALES DEL ESTUDIANTE Y TUTOR
    // =========================================================================
    private static class AvisoEstudianteAdapter extends RecyclerView.Adapter<AvisoEstudianteAdapter.AvisoViewHolder> {

        private final List<JSONObject> items;

        public AvisoEstudianteAdapter(List<JSONObject> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public AvisoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_aviso_gestion, parent, false);
            return new AvisoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AvisoViewHolder holder, int position) {
            JSONObject item = items.get(position);

            String titulo = item.optString("titulo", "Comunicado Oficial");
            String fecha = item.optString("fecha", "Reciente");
            String emisor = item.optString("emisor", "");
            String categoria = item.optString("categoria", "Aviso Institucional");
            String contenido = item.optString("contenido", "");
            String prioridad = item.optString("prioridad", "normal");

            holder.tvTituloItemAviso.setText(titulo);
            holder.tvFechaItemAviso.setText(emisor.isEmpty() ? fecha : fecha + " • " + emisor);
            holder.tvCategoriaItemAviso.setText("Categoría: " + categoria);
            holder.tvContenidoItemAviso.setText(contenido);

            int colorStripe;
            if ("urgente".equalsIgnoreCase(prioridad)) {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_danger);
            } else if ("alta".equalsIgnoreCase(prioridad) || "media".equalsIgnoreCase(prioridad)) {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_warning);
            } else {
                colorStripe = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_primary);
            }
            holder.vFranjaPrioridadAviso.setBackgroundColor(colorStripe);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class AvisoViewHolder extends RecyclerView.ViewHolder {
            View vFranjaPrioridadAviso;
            TextView tvTituloItemAviso;
            TextView tvFechaItemAviso;
            TextView tvCategoriaItemAviso;
            TextView tvContenidoItemAviso;

            public AvisoViewHolder(@NonNull View itemView) {
                super(itemView);
                vFranjaPrioridadAviso = itemView.findViewById(R.id.vFranjaPrioridadAviso);
                tvTituloItemAviso = itemView.findViewById(R.id.tvTituloItemAviso);
                tvFechaItemAviso = itemView.findViewById(R.id.tvFechaItemAviso);
                tvCategoriaItemAviso = itemView.findViewById(R.id.tvCategoriaItemAviso);
                tvContenidoItemAviso = itemView.findViewById(R.id.tvContenidoItemAviso);
            }
        }
    }

    // =========================================================================
    // MÉTODOS DE CARGA E INSCRIPCIÓN PARA PREVIAS Y MESAS (MODO ESTUDIANTE)
    // =========================================================================
    private void cargarPreviasDesdeApi() {
        executor.execute(() -> {
            try {
                JSONArray array = ApiClient.listarPrevias(dniEstudiante);
                List<JSONObject> nuevas = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj == null) continue;
                    int inst = obj.optInt("id_institucion", 0);
                    if (inst <= 0 || inst == idInstitucionEstudiante) {
                        nuevas.add(obj);
                    }
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    listaPrevias.clear();
                    listaPrevias.addAll(nuevas);
                    actualizarVistaPrevias();
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void actualizarVistaPrevias() {
        int cant = listaPrevias.size();
        if (tvCantidadPrevias != null) {
            tvCantidadPrevias.setText(cant == 1 ? "1 Materia" : cant + " Materias");
        }
        if (tvSinPreviasEstudiante != null && rvPreviasEstudiante != null) {
            if (listaPrevias.isEmpty()) {
                tvSinPreviasEstudiante.setVisibility(View.VISIBLE);
                rvPreviasEstudiante.setVisibility(View.GONE);
            } else {
                tvSinPreviasEstudiante.setVisibility(View.GONE);
                rvPreviasEstudiante.setVisibility(View.VISIBLE);
            }
        }
        if (adaptadorPrevias != null) {
            adaptadorPrevias.notifyDataSetChanged();
        }
    }

    private void cargarMesasDesdeApi() {
        if (pbCargandoMesasEst != null && listaMesas.isEmpty()) {
            pbCargandoMesasEst.setVisibility(View.VISIBLE);
        }
        executor.execute(() -> {
            try {
                JSONArray array = ApiClient.listarMesas(idInstitucionEstudiante);
                List<JSONObject> nuevas = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj != null) nuevas.add(obj);
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbCargandoMesasEst != null) pbCargandoMesasEst.setVisibility(View.GONE);
                    listaMesas.clear();
                    listaMesas.addAll(nuevas);
                    actualizarVistaMesas();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbCargandoMesasEst != null) pbCargandoMesasEst.setVisibility(View.GONE);
                    actualizarVistaMesas();
                });
            }
        });
    }

    private void actualizarVistaMesas() {
        int cant = listaMesas.size();
        if (tvCantidadMesasEstudiante != null) {
            tvCantidadMesasEstudiante.setText(cant == 1 ? "1 Mesa" : cant + " Mesas");
        }
        if (tvSinMesasEstudiante != null && rvMesasEstudiante != null) {
            if (listaMesas.isEmpty()) {
                tvSinMesasEstudiante.setVisibility(View.VISIBLE);
                rvMesasEstudiante.setVisibility(View.GONE);
            } else {
                tvSinMesasEstudiante.setVisibility(View.GONE);
                rvMesasEstudiante.setVisibility(View.VISIBLE);
            }
        }
        if (adaptadorMesas != null) {
            adaptadorMesas.notifyDataSetChanged();
        }
    }

    private void gestionarInscripcionMesa(JSONObject mesa, boolean yaInscripto) {
        String mesaId = mesa.optString("id", mesa.optString("codigo_mesa", ""));
        String materia = mesa.optString("materia", "Materia");
        String turno = mesa.optString("turno", "Mesa Evaluadora");
        String estado = mesa.optString("estado", "abierta");

        if (yaInscripto) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar Inscripción")
                    .setMessage("¿Deseás dar de baja tu inscripción en la mesa de " + materia + " (" + turno + ")?")
                    .setPositiveButton("Sí, dar de baja", (d, w) -> ejecutarAccionMesa(() -> {
                        ApiClient.bajaInscripcionMesa(mesaId, dniEstudiante, null);
                        return "Inscripción dada de baja correctamente.";
                    }))
                    .setNegativeButton("Volver", null)
                    .show();
            return;
        }

        if ("cerrada".equalsIgnoreCase(estado)) {
            Toast.makeText(this, "Esta mesa se encuentra cerrada para nuevas inscripciones.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] condiciones = {"Regular", "Libre"};
        final int[] seleccion = {0};
        new AlertDialog.Builder(this)
                .setTitle("Inscribirme a " + materia)
                .setSingleChoiceItems(condiciones, 0, (d, which) -> seleccion[0] = which)
                .setPositiveButton("Confirmar Inscripción", (d, w) -> ejecutarAccionMesa(() -> {
                    JSONObject body = new JSONObject();
                    body.put("accion", "inscribir");
                    body.put("mesa_id", mesaId);
                    body.put("dni", dniEstudiante);
                    body.put("nombre", nombreAlumnoActual);
                    body.put("curso", cursoAlumnoActual);
                    body.put("condicion", condiciones[seleccion[0]]);
                    body.put("id_institucion", idInstitucionEstudiante);
                    ApiClient.inscribirAMesa(body, null);
                    return "Inscripción confirmada (" + condiciones[seleccion[0]] + ") en " + materia + ".";
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private interface MesaCallable {
        String run() throws Exception;
    }

    private void ejecutarAccionMesa(MesaCallable task) {
        if (pbCargandoMesasEst != null) pbCargandoMesasEst.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String msg = task.run();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    cargarMesasDesdeApi();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pbCargandoMesasEst != null) pbCargandoMesasEst.setVisibility(View.GONE);
                    Toast.makeText(this, "No se pudo completar la operación: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // =========================================================================
    // ADAPTADOR PARA MATERIAS PREVIAS DEL ESTUDIANTE
    // =========================================================================
    private static class PreviaAlumnoAdapter extends RecyclerView.Adapter<PreviaAlumnoAdapter.PreviaViewHolder> {

        private final List<JSONObject> items;

        PreviaAlumnoAdapter(List<JSONObject> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PreviaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_previa_alumno, parent, false);
            return new PreviaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PreviaViewHolder holder, int position) {
            JSONObject item = items.get(position);

            String materia = item.optString("materia", item.optString("nombre_materia", "Materia"));
            String estado = item.optString("estado", "Pendiente");
            String cursoOrigen = item.optString("curso_origen", "Pendiente de asignar");
            String fechaAprob = item.optString("fecha_aprobacion", "");
            String fecha = item.optString("fecha", "");
            String calificacion = item.optString("calificacion", "").trim();

            holder.tvPreviaOrden.setText("Previa • #" + (position + 1));
            holder.tvPreviaMateria.setText(materia);
            holder.tvPreviaEstado.setText(estado);

            String detalle = "Curso Origen: " + (cursoOrigen.isEmpty() ? "—" : cursoOrigen);
            if (!fechaAprob.isEmpty() && !"null".equalsIgnoreCase(fechaAprob)) {
                detalle += " • Aprobada: " + DocenteUiHelper.formatearFecha(fechaAprob);
            } else if (!fecha.isEmpty() && !"null".equalsIgnoreCase(fecha)) {
                detalle += " • Fecha: " + DocenteUiHelper.formatearFecha(fecha);
            }
            holder.tvPreviaCursoOrigen.setText(detalle);

            int bgEstado;
            int txtEstado;
            if ("Aprobada".equalsIgnoreCase(estado)) {
                bgEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_bg);
                txtEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_text);
            } else if ("En Curso".equalsIgnoreCase(estado)) {
                bgEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_badge_blue_bg);
                txtEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_primary);
            } else {
                bgEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tep_bg);
                txtEstado = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tep_text);
            }
            holder.tvPreviaEstado.setBackgroundTintList(ColorStateList.valueOf(bgEstado));
            holder.tvPreviaEstado.setTextColor(txtEstado);

            if (calificacion.isEmpty() || "null".equalsIgnoreCase(calificacion) || "0".equals(calificacion)) {
                holder.tvPreviaNota.setText("—");
                holder.tvPreviaNota.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted));
            } else {
                holder.tvPreviaNota.setText(calificacion);
                double notaNum = 0;
                try {
                    notaNum = Double.parseDouble(calificacion.replace(",", "."));
                } catch (Exception ignored) {}
                int colorNota = notaNum >= 6.0
                        ? ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_text)
                        : ContextCompat.getColor(holder.itemView.getContext(), R.color.color_ted_text);
                holder.tvPreviaNota.setTextColor(colorNota);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PreviaViewHolder extends RecyclerView.ViewHolder {
            TextView tvPreviaOrden;
            TextView tvPreviaEstado;
            TextView tvPreviaMateria;
            TextView tvPreviaCursoOrigen;
            TextView tvPreviaNota;

            PreviaViewHolder(@NonNull View itemView) {
                super(itemView);
                tvPreviaOrden = itemView.findViewById(R.id.tvPreviaOrden);
                tvPreviaEstado = itemView.findViewById(R.id.tvPreviaEstado);
                tvPreviaMateria = itemView.findViewById(R.id.tvPreviaMateria);
                tvPreviaCursoOrigen = itemView.findViewById(R.id.tvPreviaCursoOrigen);
                tvPreviaNota = itemView.findViewById(R.id.tvPreviaNota);
            }
        }
    }

    // =========================================================================
    // ADAPTADOR PARA MESAS EVALUADORAS DEL ESTUDIANTE
    // =========================================================================
    private static class MesaAlumnoAdapter extends RecyclerView.Adapter<MesaAlumnoAdapter.MesaViewHolder> {

        interface DniProvider {
            String getDni();
        }

        interface MesaActionListener {
            void onAccion(JSONObject mesa, boolean yaInscripto);
        }

        private final List<JSONObject> items;
        private final DniProvider dniProvider;
        private final MesaActionListener listener;

        MesaAlumnoAdapter(List<JSONObject> items, DniProvider dniProvider, MesaActionListener listener) {
            this.items = items;
            this.dniProvider = dniProvider;
            this.listener = listener;
        }

        @NonNull
        @Override
        public MesaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mesa_alumno, parent, false);
            return new MesaViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MesaViewHolder holder, int position) {
            JSONObject mesa = items.get(position);
            String dniActual = dniProvider.getDni();

            String materia = mesa.optString("materia", "Mesa de Examen");
            String curso = mesa.optString("curso", "");
            String mes = mesa.optString("mes", "");
            String turno = mesa.optString("turno", "");
            String docentes = mesa.optString("docentes", "");
            String fecha = DocenteUiHelper.formatearFecha(mesa.optString("fecha", ""));
            String hora = mesa.optString("hora", "");
            String estado = mesa.optString("estado", "abierta");

            holder.tvMesaItemTitulo.setText(materia);
            String cursoTurno = (curso.isEmpty() ? "General" : curso)
                    + (turno.isEmpty() ? "" : " • " + turno)
                    + (mes.isEmpty() ? "" : " (" + mes + ")");
            holder.tvMesaItemCursoTurno.setText(cursoTurno);
            holder.tvMesaItemFechaHora.setText("Fecha: " + fecha + (hora.isEmpty() ? "" : " • " + hora + " hs"));
            holder.tvMesaItemDocentes.setText("Tribunal: " + (docentes.isEmpty() ? "A confirmar" : docentes));

            boolean yaInscripto = false;
            String condicionInsc = "Regular";
            JSONArray inscriptos = mesa.optJSONArray("inscriptos");
            if (inscriptos != null && dniActual != null) {
                for (int i = 0; i < inscriptos.length(); i++) {
                    JSONObject insc = inscriptos.optJSONObject(i);
                    if (insc != null && dniActual.trim().equals(insc.optString("dni", "").trim())) {
                        yaInscripto = true;
                        condicionInsc = insc.optString("condicion", "Regular");
                        break;
                    }
                }
            }

            boolean cerrada = "cerrada".equalsIgnoreCase(estado);
            int colorPrimary = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_primary);
            int colorBlueBg = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_badge_blue_bg);
            int colorTeaBg = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_bg);
            int colorTeaTxt = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_tea_text);
            int colorTedBg = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_ted_bg);
            int colorTedTxt = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_ted_text);
            int colorDanger = ContextCompat.getColor(holder.itemView.getContext(), R.color.color_danger);
            int colorMuted = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted);

            if (yaInscripto) {
                holder.tvMesaItemEstado.setText("Ya Inscripto");
                holder.tvMesaItemEstado.setBackgroundTintList(ColorStateList.valueOf(colorBlueBg));
                holder.tvMesaItemEstado.setTextColor(colorPrimary);

                holder.tvMesaItemCondicionInscripto.setVisibility(View.VISIBLE);
                holder.tvMesaItemCondicionInscripto.setText("Inscripción confirmada • Condición: " + condicionInsc);

                holder.btnAccionMesaAlumno.setEnabled(true);
                holder.btnAccionMesaAlumno.setText("Cancelar Inscripción");
                holder.btnAccionMesaAlumno.setTextColor(colorDanger);
                holder.btnAccionMesaAlumno.setStrokeColor(ColorStateList.valueOf(colorDanger));
            } else if (cerrada) {
                holder.tvMesaItemEstado.setText("Cerrada");
                holder.tvMesaItemEstado.setBackgroundTintList(ColorStateList.valueOf(colorTedBg));
                holder.tvMesaItemEstado.setTextColor(colorTedTxt);

                holder.tvMesaItemCondicionInscripto.setVisibility(View.GONE);

                holder.btnAccionMesaAlumno.setEnabled(false);
                holder.btnAccionMesaAlumno.setText("Inscripción Cerrada");
                holder.btnAccionMesaAlumno.setTextColor(colorMuted);
                holder.btnAccionMesaAlumno.setStrokeColor(ColorStateList.valueOf(colorMuted));
            } else {
                holder.tvMesaItemEstado.setText("Habilitada");
                holder.tvMesaItemEstado.setBackgroundTintList(ColorStateList.valueOf(colorTeaBg));
                holder.tvMesaItemEstado.setTextColor(colorTeaTxt);

                holder.tvMesaItemCondicionInscripto.setVisibility(View.GONE);

                holder.btnAccionMesaAlumno.setEnabled(true);
                holder.btnAccionMesaAlumno.setText("Inscribirme a esta Mesa");
                holder.btnAccionMesaAlumno.setTextColor(colorPrimary);
                holder.btnAccionMesaAlumno.setStrokeColor(ColorStateList.valueOf(colorPrimary));
            }

            final boolean estadoInscripto = yaInscripto;
            holder.btnAccionMesaAlumno.setOnClickListener(v -> listener.onAccion(mesa, estadoInscripto));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class MesaViewHolder extends RecyclerView.ViewHolder {
            TextView tvMesaItemTitulo;
            TextView tvMesaItemEstado;
            TextView tvMesaItemCursoTurno;
            TextView tvMesaItemFechaHora;
            TextView tvMesaItemDocentes;
            TextView tvMesaItemCondicionInscripto;
            MaterialButton btnAccionMesaAlumno;

            MesaViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMesaItemTitulo = itemView.findViewById(R.id.tvMesaItemTitulo);
                tvMesaItemEstado = itemView.findViewById(R.id.tvMesaItemEstado);
                tvMesaItemCursoTurno = itemView.findViewById(R.id.tvMesaItemCursoTurno);
                tvMesaItemFechaHora = itemView.findViewById(R.id.tvMesaItemFechaHora);
                tvMesaItemDocentes = itemView.findViewById(R.id.tvMesaItemDocentes);
                tvMesaItemCondicionInscripto = itemView.findViewById(R.id.tvMesaItemCondicionInscripto);
                btnAccionMesaAlumno = itemView.findViewById(R.id.btnAccionMesaAlumno);
            }
        }
    }
}
