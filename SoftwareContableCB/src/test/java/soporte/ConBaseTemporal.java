package soporte;

import config.Conexion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * Base para tests que usan la base de datos: cada test arranca con una base SQLite
 * nueva y vacia en una carpeta temporal. Nunca toca ~/.contabilidad_app.
 */
public abstract class ConBaseTemporal {

    @TempDir
    protected Path carpeta;

    protected Path archivoBase;

    @BeforeEach
    void abrirBaseTemporal() {
        Conexion.cerrar();
        archivoBase = carpeta.resolve("test.db");
        System.setProperty(Conexion.PROPIEDAD_RUTA, archivoBase.toString());
    }

    @AfterEach
    void cerrarBaseTemporal() {
        Conexion.cerrar();
        System.clearProperty(Conexion.PROPIEDAD_RUTA);
    }
}
