import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
/*
 * Gestor_Etiquetas.java
 *
 * Created on 31 de enero de 2008, 19:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Gestor_Etiquetas {
    //gestiona los textos que deben aparecer en pantalla. si una etiqueta no tiene
    //sitio, no se dibuja. una vez almacenadas, se dibujan de una vez
    private Vector lista_etiquetas;
    /** Creates a new instance of Gestor_Etiquetas */
    public Gestor_Etiquetas() {
        lista_etiquetas=new Vector();
    }
    public void añadir_etiqueta(String texto,int X,int Y,Font fuente) {
        //comprueba si hay sitio para colocar el texto en la posición indicada.
        //si es así, crea una nueva etiqueta
        int tamaño_x;
        int tamaño_y;
        Tipo_Etiqueta etiqueta;
        //la posición X,Y se supone el vértice superior izquierdo del rectángulo
        tamaño_x=fuente.stringWidth(texto);
        tamaño_y=fuente.getHeight();
        if (area_disponible(X,Y,X+tamaño_x,Y+tamaño_y)==true) { //área libre, añade la etiqueta
            etiqueta=new Tipo_Etiqueta(texto,fuente,X,Y,X+tamaño_x,Y+tamaño_y);
            lista_etiquetas.addElement(etiqueta);
        }
    }
    public void dibujar_etiquetas(Graphics imagen) {
        //coloca las etiquetas en la imagen dada
        int contador;
        Tipo_Etiqueta etiqueta;
        for (contador=lista_etiquetas.size()-1;contador>=0;contador--) {
            etiqueta=(Tipo_Etiqueta) lista_etiquetas.elementAt(contador);
            imagen.setFont(etiqueta.fuente);
            imagen.drawString(etiqueta.texto,etiqueta.xmin,etiqueta.ymin,0);
        }
    }
    private boolean area_disponible(int xmin,int ymin,int xmax,int ymax) {
        //comprueba todas las etiquetas presentes para ver si alguna solapa el área indicada.
        //si no hay ninguna intersaección, devuelve true
        int contador;
        boolean contenido_x;
        boolean contenido_y;
        Tipo_Etiqueta etiqueta;
        for (contador=lista_etiquetas.size()-1;contador>=0;contador--) {
            etiqueta=(Tipo_Etiqueta) lista_etiquetas.elementAt(contador);
            contenido_x=false;
            contenido_y=false;
            if ((xmin >= etiqueta.xmin && xmin <= etiqueta.xmax) || (xmax >= etiqueta.xmin && xmax <= etiqueta.xmax) || (etiqueta.xmin >= xmin && etiqueta.xmin <= xmax) || (etiqueta.xmax >= xmin && etiqueta.xmax <= xmax)) {
                contenido_x = true; //un marco vertical está entre dos marcos
            }
            if ((ymin >= etiqueta.ymin && ymin <= etiqueta.ymax) || (ymax >= etiqueta.ymin && ymax <= etiqueta.ymax) || (etiqueta.ymin >= ymin && etiqueta.ymin <= ymax) || (etiqueta.ymax >= ymin && etiqueta.ymax <= ymax)) {
                contenido_y = true; //un marco vertical está entre dos marcos
            }
            if (contenido_x==true && contenido_y==true) return false;
            
        }
        return true;
    }
    private class Tipo_Etiqueta {
        //contiene el texto a mostrar y los límites
        public String texto;
        public Font fuente;
        public int xmin;
        public int ymin;
        public int xmax;
        public int ymax;
        public Tipo_Etiqueta(String texto,Font fuente,int xmin,int ymin, int xmax,int ymax) {
            this.texto=texto;
            this.fuente=fuente;
            this.xmin=xmin;
            this.ymin=ymin;
            this.xmax=xmax;
            this.ymax=ymax;
        }
    }
}
