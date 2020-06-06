/*
 * Propiedades_Punto.java
 *
 * Created on 11 de enero de 2008, 18:53
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Propiedades_Punto {
    public int tipo;
    public String descripcion;
    public int indice; //nº de archivo .png
    /** Creates a new instance of Propiedades_Punto */
    public Tipo_Propiedades_Punto(int tipo,String descripcion,int indice) {
        this.tipo=tipo;
        this.descripcion=descripcion;
        this.indice=indice;
    }
    
}
