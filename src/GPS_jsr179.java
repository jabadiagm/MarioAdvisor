//#if Pruebas_JSR179 | JSR75_JSR82_JSR179 | JSR75_JSR82_JSR179_Halmer
//# import javax.microedition.location.*;
//#endif
/*
 * GPS_jsr179.java
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
//#if Pruebas_JSR179 | JSR75_JSR82_JSR179 | JSR75_JSR82_JSR179_Halmer
//# public class GPS_jsr179 implements LocationListener,Runnable{
//#else
public class GPS_jsr179 {
//#endif
    //public class GPS_jsr179 {
    //gestión de coordenadas a través de JSR179
//#if Pruebas_JSR179 | JSR75_JSR82_JSR179 | JSR75_JSR82_JSR179_Halmer
//#     LocationProvider provider;
//#endif
    public float latitud;
    public float longitud;
    public int velocidad;
    public int rumbo;
    public int estado;
    public int altura;
    public String ultimo_error;
    private Gestor_GPS gestor_gps;
    private Thread t; //tarea para actualización de coordenadas, en secundario
    
    /** Creates a new instance of GPS_jsr179 */
    public GPS_jsr179(Gestor_GPS gestor_gps) {
        this.gestor_gps=gestor_gps;
    }
    
    public int iniciar() {
//#if Pruebas_JSR179 | JSR75_JSR82_JSR179 | JSR75_JSR82_JSR179_Halmer
//#         estado=gestor_gps.estado_GPS_OFF;
//#         try {
//#             provider = LocationProvider.getInstance(null);
//#             provider.setLocationListener(this, -1, -1, -1); 
//#         } catch (LocationException ex) {
//#             ex.printStackTrace();
//#             ultimo_error=ex.toString();
//#             estado=gestor_gps.estado_GPS_Error;
//#             return 1; //error en la inicialización
//#         } 
//#endif
        return 0; //GPS interno arrancado
        
    }
    public void terminar() {
//#if Pruebas_JSR179 | JSR75_JSR82_JSR179 | JSR75_JSR82_JSR179_Halmer
//#         //quita la callback 
//#         provider.reset();
//#         provider.setLocationListener(null,-1,-1,-1);
//#         provider=null; 
//#         estado=gestor_gps.estado_GPS_OFF;
//#endif
    }
//#if Pruebas_JSR179 | JSR75_JSR82_JSR179 | JSR75_JSR82_JSR179_Halmer
//#     public void locationUpdated(LocationProvider provider, Location location) {
//#         if (location.isValid()) {
//#             QualifiedCoordinates coordenadas;
//#             coordenadas=location.getQualifiedCoordinates();
//#             longitud=(float)coordenadas.getLongitude();
//#             latitud=(float)coordenadas.getLatitude(); 
//#             altura=(int)coordenadas.getAltitude();
//#             velocidad=(int)location.getSpeed();
//#             rumbo=(int)location.getCourse();
//#             estado=gestor_gps.estado_GPS_ON_Listo;
//#         } else {
//#             estado=gestor_gps.estado_GPS_OK_No_Listo;
//#         }
//#         t=new Thread(this);
//#         t.start(); //avisa al objeto padre de que hay nuevas coordenadas disponibles 
//#     }
//#     
//#     
//#     public void providerStateChanged(LocationProvider provider, int newState) {
//#         
//#     } 
//# 
//#     public void run() {
//#         gestor_gps.notificar_evento_GPS_interno();
//#     }
//#endif
    
}
