/*
 * Tipo_Etiqueta.java
 *
 * Created on 20 de enero de 2008, 0:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Etiqueta {
    public int puntero;
    public String nombre_completo;
    public String nombre_corto;
    public String abreviatura;
    
    /** Creates a new instance of Tipo_Etiqueta */
    /*public Tipo_Etiqueta(String completo,String corto,String abrev) {
        nombre_completo=completo;
        nombre_corto=corto;
        abreviatura=abrev;
    }*/
    public Tipo_Etiqueta(int puntero_LBL) { //la etiqueta se crea vacía, se rellenan los datos tras ordenar de menor a mayor puntero
        puntero=puntero_LBL;
        nombre_completo="";
        nombre_corto="";
        abreviatura="";
    }
    
}
