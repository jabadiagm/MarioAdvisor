/*
 * Tipo_Posicion.java
 *
 * Created on 20 de septiembre de 2008, 11:48
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Posicion {
    //contiene los datos de localización suministrados por un GPS ó un repetidor
    public long tiempo; //tiempo en milisegundos desde medianoche del 1 de enero de 1970
    public float longitud;
    public float latitud;
    public int velocidad; //en m/s
    public int rumbo; //grados, con el 0 en el norte
    public int altura; // en m, sobre el nivel del mar
    public int cellid; //identificador del repetidor
    public int lac; //identificador de área según el repetidor    
    /** Creates a new instance of Tipo_Posicion */
    public Tipo_Posicion(long tiempo,float longitud,float latitud,int velocidad,int rumbo,int altura) {
        this.tiempo=tiempo;
        this.longitud=longitud;
        this.latitud=latitud;
        this.velocidad=velocidad;
        this.rumbo=rumbo;
        this.altura=altura;
    }
    public Tipo_Posicion(long tiempo,float longitud,float latitud,int velocidad,int rumbo,int altura,int cellid,int lac) {
        this.tiempo=tiempo;
        this.longitud=longitud;
        this.latitud=latitud;
        this.velocidad=velocidad;
        this.rumbo=rumbo;
        this.altura=altura;
        this.cellid=cellid;
        this.lac=lac;
    }
    
}
