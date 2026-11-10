
package com.example.boletindigital;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    // URL del backend PHP en Railway
    private static final String BASE_URL =
            "https://boletin-digital-escolar-production.up.railway.app/api/";

    // Clase para manejar los errores de la API
    public static class ApiException extends Exception {

        private final int codigo;

        public ApiException(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }

        public int getCodigo() {
            return codigo;
        }
    }

    // Metodo general para realizar peticiones HTTP
    private static String solicitar(
            String metodo,
            String ruta,
            Object datos,
            String token
    ) throws Exception {

        HttpURLConnection conexion = null;

        try {
            URL url = new URL(BASE_URL + ruta);

            conexion = (HttpURLConnection) url.openConnection();

            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(10000);
            conexion.setReadTimeout(10000);

            conexion.setRequestProperty(
                    "Accept", "application/json");

            // Agregar token cuando sea necesario
            if (token != null && !token.isEmpty()) {
                conexion.setRequestProperty(
                        "Authorization", "Bearer " + token);
            }

            // Enviar datos en formato JSON
            if (datos != null) {

                conexion.setDoOutput(true);

                conexion.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8");

                try (OutputStream salida =
                             conexion.getOutputStream()) {

                    byte[] contenido =
                            datos.toString().getBytes(
                                    StandardCharsets.UTF_8);

                    salida.write(contenido);
                }
            }

            // Obtener el codigo de respuesta
            int codigo = conexion.getResponseCode();

            InputStream entrada =
                    codigo >= 200 && codigo < 300
                            ? conexion.getInputStream()
                            : conexion.getErrorStream();

            StringBuilder respuesta = new StringBuilder();

            // Leer la respuesta del servidor
            if (entrada != null) {

                try (BufferedReader lector =
                             new BufferedReader(
                                     new InputStreamReader(
                                             entrada,
                                             StandardCharsets.UTF_8))) {

                    String linea;

                    while ((linea = lector.readLine()) != null) {
                        respuesta.append(linea);
                    }
                }
            }

            // Comprobar errores de la API
            if (codigo < 200 || codigo >= 300) {

                String mensaje = "Error del servidor";

                try {
                    JSONObject error =
                            new JSONObject(respuesta.toString());

                    mensaje = error.optString(
                            "error", mensaje);

                } catch (Exception ignorado) {

                    if (codigo == 404) {
                        mensaje = "Recurso no encontrado";
                    }
                }

                throw new ApiException(codigo, mensaje);
            }

            return respuesta.toString();

        } catch (SocketTimeoutException e) {

            throw new IOException(
                    "No hubo respuesta del servidor a tiempo.", e);

        } finally {

            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    // POST: iniciar sesion
    public static JSONObject iniciarSesion(
            String email,
            String password
    ) throws Exception {

        JSONObject datos = new JSONObject();

        datos.put("email", email);
        datos.put("password", password);

        String respuesta = solicitar(
                "POST",
                "auth.php",
                datos,
                null
        );

        return new JSONObject(respuesta);
    }

    // GET: listar todos los alumnos
    public static JSONArray listarAlumnos() throws Exception {

        String respuesta = solicitar(
                "GET",
                "alumnos.php",
                null,
                null
        );

        return new JSONArray(respuesta);
    }

    // GET: consultar un alumno por ID
    public static JSONObject obtenerAlumno(
            int id
    ) throws Exception {

        String respuesta = solicitar(
                "GET",
                "alumnos.php?id=" + id,
                null,
                null
        );

        return new JSONObject(respuesta);
    }

    // POST: registrar un nuevo alumno
    public static JSONObject crearAlumno(
            JSONObject alumno,
            String token
    ) throws Exception {

        String respuesta = solicitar(
                "POST",
                "alumnos.php",
                alumno,
                token
        );

        return new JSONObject(respuesta);
    }

    // GET: consultar el boletín académico oficial por DNI
    public static JSONObject obtenerBoletin(
            String dni
    ) throws Exception {

        String respuesta = solicitar(
                "GET",
                "boletin.php?dni=" + dni,
                null,
                null
        );

        return new JSONObject(respuesta);
    }

    // POST: guardar calificaciones en lote (Docente / Preceptor)
    public static JSONObject guardarNotas(
            JSONArray listaNotas,
            String token
    ) throws Exception {

        String respuesta = solicitar(
                "POST",
                "notas.php",
                listaNotas,
                token
        );

        return new JSONObject(respuesta);
    }
}

