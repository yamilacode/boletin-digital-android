
package com.example.boletindigital;

import org.json.JSONObject;

public class Alumno {

    private int id;
    private String dni;
    private String nombre;
    private String apellido;
    private String jurisdiccion;
    private int idInstitucion;
    private String curso;
    private String seccion;
    private String nombreTutor;

    public Alumno(JSONObject json) {
        id = json.optInt("id");
        dni = json.optString("dni");
        nombre = json.optString("nombre");
        apellido = json.optString("apellido");
        jurisdiccion = json.optString("jurisdiccion");
        idInstitucion = json.optInt("id_institucion");
        curso = json.optString("curso");
        seccion = json.optString("seccion");
        nombreTutor = json.optString("nombre_tutor");
    }

    public int getId() {
        return id;
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getJurisdiccion() {
        return jurisdiccion;
    }

    public int getIdInstitucion() {
        return idInstitucion;
    }

    public String getCurso() {
        return curso;
    }

    public String getSeccion() {
        return seccion;
    }

    public String getNombreTutor() {
        return nombreTutor;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}

