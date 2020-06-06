import java.util.Vector;
/*
 * Mapa_IMG.java
 *
 * Created on 13 de noviembre de 2007, 19:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Mapa_IMG {
    public Vector Puntos;
    public Vector Puntos_Indexados;
    public Vector Polilineas;
    public Vector Poligonos;
    public Tipo_Rectangulo limites;
    public int nivel_detalle; //0,1,2 para los mapas detallados, 3,... para los generales
    public int nivel_zoom; //0-24. puntero al valor que mide 1/5 de la pantalla visible
    public byte [] niveles_detalle; //lista de niveles de detalle para cada zoom. útil para generar el nivel anterior o posterior
    public String nombre_archivo; //nombre del IMG abierto
    public String descripcion; //descripción contenida en la cabecera del IMG
    public boolean error_memoria; //true si ha habido problemas de memoria
    
    /** Creates a new instance of Mapa_IMG */
    public Mapa_IMG() {
        Puntos=new Vector();
        Puntos_Indexados=new Vector();
        Polilineas=new Vector();
        Poligonos=new Vector();
        error_memoria=false;
    }
    
}
