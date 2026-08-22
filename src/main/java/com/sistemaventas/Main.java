package com.sistemaventas;

import com.sistemaventas.database.ConexionDB;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {

        try {
            Connection conexion = ConexionDB.conectar();

            System.out.println("Conexion a MySQL exitosa");

            conexion.close();

        } catch (Exception e) {
            System.out.println(
                    "Error de conexion: " + e.getMessage()
            );
        }
    }
}