
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

        // Texto tomado de strings.xml (sin hardcodear)
        tvConfirmacion.setText(getString(
                R.string.confirmar_registro,
                nombre, apellido, dni));

        // Al tocar cualquiera de los dos se bloquean ambos
        // para evitar envíos duplicados por doble toque
        btnConfirmar.setOnClickListener(v -> {
            bloquearBotones(btnConfirmar, btnCorregir);
            setResult(RESULT_OK);
            finish();
        });

        btnCorregir.setOnClickListener(v -> {
            bloquearBotones(btnConfirmar, btnCorregir);
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private void bloquearBotones(Button... botones) {
        for (Button boton : botones) {
            boton.setEnabled(false);
        }
    }
}

