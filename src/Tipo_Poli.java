/*
 * Tipo_Poli.java
 *
 * Created on 12 de diciembre de 2007, 19:56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/** Tipo para polilíneas y polígonos
 *
 * @author javier
 */
public class Tipo_Poli {
    public int tipo; //tipo+subtipo en bytes 1 y 0 del entero
    public int offset_etiqueta;
    //public String etiqueta;
    public float[] puntos_X;
    public float[] puntos_Y;
    public boolean[] punto_es_nodo;
    public boolean sentido_unico; //calle de una sola dirección
    public boolean datos_en_NET;
    public Tipo_Etiqueta etiqueta;
    public Tipo_Etiqueta_NET etiqueta_NET;
    
    /** Creates a new instance of Tipo_Poli */
    public Tipo_Poli(int tipo_poly,int offset,float[] X,float [] Y) {
        //el contenido de la etiqueta se lee más tarde, no puede aparecer en el contructor
        tipo=tipo_poly;
        offset_etiqueta=offset;
        puntos_X=X;
        puntos_Y=Y;        
    }
    //constructor vacío
    public Tipo_Poli(){
        
    }
        
}
