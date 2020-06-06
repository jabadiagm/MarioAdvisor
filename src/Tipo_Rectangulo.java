/*
 * Tipo_Rectangulo.java
 *
 * Created on 5 de diciembre de 2007, 11:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * define los cuatro límites de una área, en lon/lat
 * @author javier
 */
public class Tipo_Rectangulo {
    public float norte;
    public float sur;
    public float este;
    public float oeste;
    
    /** Creates a new instance of Tipo_Rectangulo */
    public Tipo_Rectangulo(float Norte,float Sur,float Este,float Oeste) {
        norte=Norte;
        sur=Sur;
        este=Este;
        oeste=Oeste;
    }
    public Tipo_Rectangulo() {
        
    }
}
