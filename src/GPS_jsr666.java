
/*
 * GPS_jsr666.java
 *
 * Created on 18 de junio de 2008, 17:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class GPS_jsr666 {
    //sucedáneo de GPS_jsr179
    public float latitud;
    public float longitud;
    public int velocidad;
    public int rumbo;
    public int altura;
    public int estado;
    public String ultimo_error;
    
    /** Creates a new instance of GPS_jsr179 */
    public GPS_jsr666(Gestor_GPS gestor_gps) {
    }
    
    public int iniciar() {
        return 0;
    }
    public void terminar() {
    }
    
    
}
