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
        // Sentencias DDL para la estructura inicial
        String sqlClientes = """
            CREATE TABLE IF NOT EXISTS cliente (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                cuit TEXT UNIQUE,
                telefono TEXT
            );
            """;

        String sqlAsientos = """
            CREATE TABLE IF NOT EXISTS comprobante (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                monto REAL NOT NULL
            );
            """;

        try (Statement stmt = conexion.createStatement()) {
            // Ejecutamos cada creación de tabla
            stmt.execute(sqlClientes);
            stmt.execute(sqlAsientos);
        } catch (SQLException e) {
            System.err.println("Error al crear la estructura inicial de tablas: " + e.getMessage());
        }
    }
}