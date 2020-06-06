/*
 * Tipo_Criterios_Busqueda.java
 *
 * Created on 21 de mayo de 2008, 17:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Criterios_Busqueda {
    //contiene los parámetros de búsqueda de elementos dentro de un archivo IMG
    //contantes
    public static final int Tipo_Busqueda_Puntos=0;
    public static final int Tipo_Busqueda_Polilineas=1;
    public static final int Tipo_Busqueda_Poligonos=2;
    //varibles
    public int tipo; //incluye tipo y subtipo, para puntos
    public String texto; //texto contenido en la etiqueta
    public boolean ordenar_por_distancia; //forma de ordenar los resultados
    public int tipo_busqueda; //elementos a buscar
    public boolean incluir_tipo; //si es false, busca cualquier elemento. si es true, busca sólo los que tengal el tipo indicado
    
    
    /** Creates a new instance of Tipo_Criterios_Busqueda */
    public Tipo_Criterios_Busqueda(int tipo,String texto,boolean ordenar_por_distancia,int tipo_busqueda,boolean incluir_tipo) {
        this.tipo=tipo;
        this.texto=texto;
        this.ordenar_por_distancia=ordenar_por_distancia;
        this.tipo_busqueda=tipo_busqueda;
        this.incluir_tipo=incluir_tipo;
    }
    
}
