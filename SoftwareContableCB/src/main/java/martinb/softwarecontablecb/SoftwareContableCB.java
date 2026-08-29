/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package martinb.softwarecontablecb;

import config.Conexion;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Martin
 */
public class SoftwareContableCB {

    public static void main(String[] args) {
        System.out.println("Prueba de Conexion...");
        
        Connection con = Conexion.getConexion();
        
        if (con != null) {
            try {
                // 2. Verificar datos de la base de datos
                DatabaseMetaData metaData = con.getMetaData();
                System.out.println("--> Conectado exitosamente a: " + metaData.getDriverName());

                // 3. Verificar que las tablas se hayan creado en SQLite
                System.out.println("\nTablas creadas en la base de datos:");
                ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
                
                while (rs.next()) {
                    String nombreTabla = rs.getString("TABLE_NAME");
                    // Omitimos la tabla interna autogenerada por SQLite
                    if (!nombreTabla.equals("sqlite_sequence")) {
                        System.out.println(" - " + nombreTabla);
                    }
                }

            } catch (SQLException e) {
                System.err.println("Error al consultar metadatos de la base: " + e.getMessage());
            }
        } else {
            System.err.println("La conexión es nula. Algo falló en la clase Conexion.");
        }
        
    }
}
