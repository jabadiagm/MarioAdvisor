/*
 * Tipo_Punto.java
 *
 * Created on 12 de diciembre de 2007, 20:04
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Punto {
    public int tipo; //tipo+subtipo en bytes 1 y 0 del entero
    public boolean es_POI;
    public int puntero_etiqueta;
    public float longitud;
    public float latitud;
    public Tipo_Etiqueta etiqueta;
    public Tipo_Etiqueta_NET etiqueta_POI;
    
    /** Creates a new instance of Tipo_Punto */
    public Tipo_Punto(int tipo_punto,boolean es_POI_punto,int puntero_etiqueta_punto,float longitud_punto,float latitud_punto,Tipo_Etiqueta etiqueta_punto,Tipo_Etiqueta_NET etiqueta_POI_punto) {
        tipo=tipo_punto;
        es_POI=es_POI_punto;
        puntero_etiqueta=puntero_etiqueta_punto;
        longitud=longitud_punto;
        latitud=latitud_punto;
        etiqueta=etiqueta_punto; //etiqueta será null cuando sea un POI, para indicar que hay que hacer más cosas antes de leer
        etiqueta_POI=etiqueta_POI_punto;
    }
    
}
