import javax.microedition.location.*;
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
public class GPS_jsr179 implements LocationListener{
    //public class GPS_jsr179 {
    //gestión de coordenadas a través de JSR179
    LocationProvider provider;
    public float latitud;
    public float longitud;
    public boolean navegando=false;
    
    /** Creates a new instance of GPS_jsr179 */
    public GPS_jsr179() {
    }
    
    public int iniciar() {
        try {
            provider = LocationProvider.getInstance(null);
            provider.setLocationListener(this, -1, -1, -1); 
        } catch (LocationException ex) {
            ex.printStackTrace();
            return 1; //error la inicialización
        } 
        return 0; //GPS interno arrancado
        
    }
    public void terminar() {
        //quita la callback 
        provider.setLocationListener(null,-1,-1,-1);
        provider=null; 
    }
    public void locationUpdated(LocationProvider provider, Location location) {
        if (location.isValid()) {
            QualifiedCoordinates coordenadas;
            coordenadas=location.getQualifiedCoordinates();
            longitud=(float)coordenadas.getLongitude();
            latitud=(float)coordenadas.getLatitude(); 
            navegando=true; 
        } else {
            navegando=false;
        }
        
  
    }
    
    
    public void providerStateChanged(LocationProvider provider, int newState) {
        
    } 
    
}
