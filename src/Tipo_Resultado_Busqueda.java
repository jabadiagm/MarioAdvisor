/*
 * Tipo_Resultado_Busqueda.java
 *
 * Created on 21 de mayo de 2008, 17:36
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Resultado_Busqueda {
    //posición y etiqueta de un elemento encontrado
    public float longitud;
    public float latitud;
    public String etiqueta;
    /** Creates a new instance of Tipo_Resultado_Busqueda */
    public Tipo_Resultado_Busqueda(float longitud,float latitud,String etiqueta) {
        this.longitud=longitud;
        this.latitud=latitud;
        this.etiqueta=etiqueta;
    }
    
}
