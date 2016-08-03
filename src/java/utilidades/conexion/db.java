package utilidades.conexion;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class db {
    
     private static Connection cns = null;
    
    private static String host = "192.168.150.100";
    private static String dataBase = "pruebas";
    private static String user = "postgres";
    private static String pass = "postgres";

    public static Connection getConectar() {
        conectar();
        return cns;
    }

    public static void conectar() {
        try {
            if (cns == null) {
                 conexion();
            } else {
                if (cns.isClosed()) {
                     conexion();
                }
            }
        } catch (ClassNotFoundException ex) {
            System.out.print("Error Interno! Registro de Coneccion falló");
           
        } catch (SQLException ex) {
            System.out.print("Acceso denegado!! Usuario NO Autorizado");           
        }
    }
    private static void conexion() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://" + host + ":5432/" + dataBase; 
        cns = DriverManager.getConnection(url, user, pass);
    }
    public static void cerrarConexion() throws SQLException {
        if (cns != null) {
            if (!cns.isClosed()) {
                cns.close();
            }
        }
    }
}