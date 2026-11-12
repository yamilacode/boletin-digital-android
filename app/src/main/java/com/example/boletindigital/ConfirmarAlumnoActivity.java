
package com.example.boletindigital;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ConfirmarAlumnoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_confirmar_alumno);

        TextView tvTituloConfirmar =
                findViewById(R.id.tvTituloConfirmar);

        TextView tvConfirmacion =
                findViewById(R.id.tvConfirmacion);

        Button btnConfirmar =
                findViewById(R.id.btnConfirmar);

        Button btnCorregir =
                findViewById(R.id.btnCorregir);

        Intent datos = getIntent();

        String nombre = datos.getStringExtra("nombre");
        String apellido = datos.getStringExtra("apellido");
        String dni = datos.getStringExtra("dni");
        String claveProvisoria = datos.getStringExtra("clave_provisoria");
        boolean modoEdicion = datos.getBooleanExtra("modo_edicion", false);

        DocenteUiHelper.configurarFooterDocente(
                this,
                datos.getStringExtra("token"),
                datos.getStringExtra("rol"),
                datos.getStringExtra("nombre_usuario"));

        if (modoEdicion && tvTituloConfirmar != null) {
            tvTituloConfirmar.setText("Confirmar Cambios");
        }

        String resumen = (modoEdicion ? "¿Confirmás la actualización?\n\n" : "¿Confirmás el registro?\n\n")
                + nombre + " " + apellido
                + "\nDNI: " + dni;
        if (!modoEdicion && claveProvisoria != null && !claveProvisoria.trim().isEmpty()) {
            resumen += "\n\nAcceso al portal:\nUsuario: " + dni + "\nClave inicial provisoria: " + claveProvisoria;
        }
        tvConfirmacion.setText(resumen);

        btnConfirmar.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        btnCorregir.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
}

