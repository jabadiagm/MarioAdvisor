/*
 * Poly.java
 *
 * Created on 13 de noviembre de 2007, 18:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * Polil�nea / pol�gono
 */
public class Poly {
    public int tipo; //tipo+subtipo en bytes 1 y 0 del entero
    public String etiqueta;
    public float[] puntos_X;
    public float[] puntos_Y;
    
    /** Creates a new instance of Polilinea */
    public Poly(int tipo_poly,String etiqueta,float[] X,float [] Y) {
        //el contenido de la etiqueta se lee m�s tarde, no puede aparecer en el contructor
        tipo=tipo_poly;
        etiqueta=etiqueta;
        puntos_X=X;
        puntos_Y=Y;
    }
    
}
