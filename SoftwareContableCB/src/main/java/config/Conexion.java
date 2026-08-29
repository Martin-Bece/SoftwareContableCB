package config;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexion {

    private static Connection conexion = null;

    public static Connection getConexion() {
        if (conexion == null) {
            try {
                // 1. Definir y crear directorio del sistema si no existe
                String userHome = System.getProperty("user.home");
                String folderPath = userHome + File.separator + ".contabilidad_app";
                File folder = new File(folderPath);

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                // 2. Definir ruta del archivo .db
                String dbPath = folderPath + File.separator + "contabilidad.db";
                String url = "jdbc:sqlite:" + dbPath;

                // 3. Conectar (SQLite crea el archivo si no existe)
                conexion = DriverManager.getConnection(url);

                // Activar el soporte de Claves Foráneas en SQLite
                try (Statement stmt = conexion.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }

                // 4. Inicializar las tablas del sistema automáticamente
                inicializarTablas();

                System.out.println("Base de datos conectada e inicializada correctamente.");

            } catch (SQLException e) {
                System.err.println("Error al conectar con SQLite: " + e.getMessage());
            }
        }
        return conexion;
    }

    private static void inicializarTablas() {
        // Sentencias DDL ajustadas según el modelo Java
        String sqlCliente = """
            CREATE TABLE IF NOT EXISTS cliente (
                cuit TEXT PRIMARY KEY,
                razonSocial TEXT NOT NULL,
                nombreFantasia TEXT,
                direccion TEXT,
                actividades TEXT,
                impuestosActivos TEXT,
                tieneEmpleados NUMERIC NOT NULL
            );
            """;

        String sqlContacto = """
            CREATE TABLE IF NOT EXISTS contacto (
                cuit TEXT PRIMARY KEY,
                razonSocial TEXT NOT NULL,
                tipo TEXT NOT NULL
            );
            """;

        String sqlComprobante = """
            CREATE TABLE IF NOT EXISTS comprobante (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                clienteCuit TEXT NOT NULL,
                contactoCuit TEXT NOT NULL,
                tipoOperacion TEXT NOT NULL,
                fecha TEXT NOT NULL,
                tipoComprobante TEXT NOT NULL,
                montoNeto REAL NOT NULL,
                montoIva REAL NOT NULL,
                montoTotal REAL NOT NULL,
                FOREIGN KEY (clienteCuit) REFERENCES cliente(cuit) ON DELETE CASCADE,
                FOREIGN KEY (contactoCuit) REFERENCES contacto(cuit) ON DELETE RESTRICT
            );
            """;

        try (Statement stmt = conexion.createStatement()) {
            // Ejecutamos en orden para respetar las dependencias de Claves Foráneas
            stmt.execute(sqlCliente);
            stmt.execute(sqlContacto);
            stmt.execute(sqlComprobante);
        } catch (SQLException e) {
            System.err.println("Error al crear la estructura inicial de tablas: " + e.getMessage());
        }
    }
}