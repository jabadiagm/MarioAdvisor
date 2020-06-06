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
    private Vector lista_etiquetas_colocadas; 
    public final int TIPO_PUNTO_INDEXADO=0;
    public final int TIPO_PUNTO=1;
    public final int TIPO_CARRETERA=2; //tipo especial, el texto va en un cuadrado blanco
    public final int TIPO_AUTOPISTA=3; //tipo espacial, texto en un cuadrado azul con bordes superior e inferior de color rojo
    public  final int TIPO_POLILINEA=4;
    public final int TIPO_CALLE=5; //tipo especial, se colocan en diagonal y no se verifican interferencias
    public final int TIPO_POLIGONO=6;
    /** Creates a new instance of Gestor_Etiquetas */
    public Gestor_Etiquetas() {
        lista_etiquetas=new Vector();
        lista_etiquetas_colocadas=new Vector();
    }
    public void a�adir_etiqueta(String texto,int X,int Y,Font fuente,int tipo,int pendiente) {
        //define el �rea que ocupa una etiqueta, y la a�ade a la lista
        //seg�n el tipo de elemento, la etiqueta tendr� una forma y colocaci�n distintas.
        int tama�o_x;
        int tama�o_y;
        Tipo_Etiqueta etiqueta;
        if (texto.compareTo("")==0) {
            return; //puede haber etiquetas vac�as
        }
        //la posici�n X,Y se supone el v�rtice superior izquierdo del rect�ngulo
        tama�o_x=fuente.stringWidth(texto);
        tama�o_y=fuente.getHeight();
        //seg�n el tipo de etiqueta,cambia el �rea a ocupar y la forma de dibujarla
        if (tipo==this.TIPO_POLILINEA || tipo==this.TIPO_PUNTO || tipo==this.TIPO_PUNTO_INDEXADO) { //caso general. el punto de anclaje es la esquina superior izquierda del texto
            etiqueta=new Tipo_Etiqueta(texto,fuente,X,Y,X+tama�o_x,Y+tama�o_y,tipo,0);
        } else if (tipo==TIPO_POLIGONO) { //el punto de anclaje es el centro del texto
            etiqueta=new Tipo_Etiqueta(texto,fuente,X-tama�o_x/2,Y-tama�o_y/2,X+tama�o_x/2,Y+tama�o_y/2,tipo,0);
        } else if (tipo==this.TIPO_CALLE) { //no hay comprobaci�n de inteferencia para las calles. el texto va inclinado, y el punto de anclaje es el centro inferior
            etiqueta=new Tipo_Etiqueta(texto,fuente,X,Y,0,0,tipo,pendiente);
        } else if (tipo==this.TIPO_CARRETERA) { //texto centrado en un recuadro blanco con borde negro
            etiqueta=new Tipo_Etiqueta(texto,fuente,X-tama�o_x/2-2,Y-tama�o_y/2-1,X+tama�o_x/2+2,Y+tama�o_y/2+1,tipo,0);
        } else { //suponiendo que no hay errores, s�lo puede ser de TIPO_AUTOPISTA. cuadrado azul con l�neas rojas en sus bordes
            etiqueta=new Tipo_Etiqueta(texto,fuente,X-tama�o_x/2-1,Y-tama�o_y/2-3,X+tama�o_x/2+1,Y+tama�o_y/2+3,tipo,0);
        }
        lista_etiquetas.addElement(etiqueta);
    }
    public void dibujar_etiquetas(Graphics imagen) {
        //coloca las etiquetas en la imagen dada
        //empieza dibujando los textos de los puntos, despu�s sigue con las carreteras y autopistas.
        //luego las polil�neas,las calles y los pol�gonos

        int contador;
        int tipo;
        Tipo_Etiqueta etiqueta;
        for (tipo=0; tipo<=6;tipo++) {
            for (contador=0;contador<lista_etiquetas.size();contador++) {
                etiqueta=(Tipo_Etiqueta) lista_etiquetas.elementAt(contador);
                if (etiqueta.tipo==tipo) {
                    dibujar_etiqueta(etiqueta,imagen); //si es del tipo indicado, la dibuja
                }
            }
            
        }
    }
    private void dibujar_etiqueta(Tipo_Etiqueta etiqueta,Graphics imagen) {
        //dibuja la etiqueta dada, teniendo en cuenta el tipo y comprobando que el �rea que ocupar� est� disponible
        if (etiqueta.tipo==this.TIPO_POLILINEA || etiqueta.tipo==this.TIPO_PUNTO || etiqueta.tipo==this.TIPO_PUNTO_INDEXADO) { //caso general. el punto de anclaje es la esquina superior izquierda del texto
            if (this.area_disponible(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax,etiqueta.ymax)==false) return;
            imagen.setFont(etiqueta.fuente);
            imagen.setColor(0);
            imagen.drawString(etiqueta.texto,etiqueta.xmin,etiqueta.ymin,Graphics.TOP | Graphics.LEFT);
            lista_etiquetas_colocadas.addElement(etiqueta); //reserva el �rea ocupada por la etiqueta

        } else if (etiqueta.tipo==TIPO_POLIGONO) { //el punto de anclaje es el centro del texto
            if (this.area_disponible(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax,etiqueta.ymax)==false) return;
            imagen.setFont(etiqueta.fuente);
            imagen.setColor(0);
            imagen.drawString(etiqueta.texto,etiqueta.xmin,etiqueta.ymin,Graphics.TOP | Graphics.LEFT);
            lista_etiquetas_colocadas.addElement(etiqueta); //reserva el �rea ocupada por la etiqueta
        } else if (etiqueta.tipo==this.TIPO_CALLE) { //no hay comprobaci�n de interferencia para las calles. el texto va inclinado, y el punto de anclaje es el centro inferior
            imagen.setFont(etiqueta.fuente);
            imagen.setColor(0);
            dibujar_texto_inclinado(etiqueta.xmin,etiqueta.ymin,etiqueta.texto,etiqueta.fuente,etiqueta.pendiente,imagen);
        } else if (etiqueta.tipo==this.TIPO_CARRETERA) { //texto centrado en un recuadro blanco con borde negro
            if (this.area_disponible(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax,etiqueta.ymax)==false) return;
            imagen.setFont(etiqueta.fuente);
            //dibuja el recuadro blanco
            imagen.setColor(0xffffff); 
            imagen.fillRect(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax-etiqueta.xmin,etiqueta.ymax-etiqueta.ymin);
            //dibuja el borde negro
            imagen.setColor(0); 
            imagen.drawRect(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax-etiqueta.xmin,etiqueta.ymax-etiqueta.ymin);
            imagen.drawString(etiqueta.texto,etiqueta.xmin+2,etiqueta.ymin+1,Graphics.TOP | Graphics.LEFT);
            lista_etiquetas_colocadas.addElement(etiqueta); //reserva el �rea ocupada por la etiqueta
        } else { //suponiendo que no hay errores, s�lo puede ser de TIPO_AUTOPISTA. cuadrado azul con l�neas rojas en sus bordes
            if (this.area_disponible(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax,etiqueta.ymax)==false) return;
            imagen.setFont(etiqueta.fuente);
            //dibuja el recuadro rojo
            imagen.setColor(0xff0000); 
            imagen.fillRect(etiqueta.xmin,etiqueta.ymin,etiqueta.xmax-etiqueta.xmin,etiqueta.ymax-etiqueta.ymin);
            //dibuja el recuadro azul
            imagen.setColor(0x0000ff); 
            imagen.fillRect(etiqueta.xmin,etiqueta.ymin+2,etiqueta.xmax-etiqueta.xmin,etiqueta.ymax-etiqueta.ymin-4);
            //letra blanca
            imagen.setColor(0xffffff); 
            imagen.drawString(etiqueta.texto,etiqueta.xmin+1,etiqueta.ymin+3,Graphics.TOP | Graphics.LEFT);
            lista_etiquetas_colocadas.addElement(etiqueta); //reserva el �rea ocupada por la etiqueta

        }
        
    }
    public void dibujar_texto_inclinado (int x, int y,String texto,Font fuente,int pendiente,Graphics imagen) {
        //dibuja un texto con la inclinaci�n indicada. el punto dado pertenece al centro de la l�nea base 
        int anchura_caracter; //tama�o horizontal de la letra "A" de la fuente
        int altura_caracter; //tama�o vertical de la fuente
        int contador;
        double x_actual,y_actual;
        int len_cadena;
        double incremento_x;
        double incremento_y;
        int correccion=0; //valor a restar a la anchura del car�cter cuando se inclina el texto
        int pendiente_absoluta;
        int longitud_texto; //tama�o de la cadena, en p�xeles
        double coseno; //coseno del �ngulo definido por la pendiente
        anchura_caracter=fuente.stringWidth("A")+1;
        altura_caracter=fuente.getHeight()-1;
        len_cadena=texto.length();
        pendiente_absoluta=Math.abs(pendiente);
        if (pendiente_absoluta<1000) { //no es texto vertical
            //avances de cada car�cter en vertical y horizontal
            coseno=1/Math.sqrt(1+pendiente*pendiente/10000);
            longitud_texto=len_cadena*anchura_caracter;
            incremento_x=Math.ceil(longitud_texto*coseno/len_cadena);
            //ajusta la posici�n de la primera letra
            x_actual=x-len_cadena*incremento_x/2;
            y_actual=y-len_cadena*incremento_x/2*pendiente/100;
            incremento_y=incremento_x*pendiente/100;
        } else { //texto vertical, o casi
            x_actual=x;
            y_actual=y-altura_caracter*texto.length()/2;
            incremento_x=0;
            incremento_y=altura_caracter-1;
        }
        for (contador=0;contador<texto.length();contador++) {
            imagen.drawSubstring(texto,contador,1,(int)x_actual,(int)y_actual,Graphics.BASELINE+Graphics.HCENTER);
            x_actual+=incremento_x;
            y_actual+=incremento_y;
        }
        
        
    }
    private boolean area_disponible(int xmin,int ymin,int xmax,int ymax) {
        //comprueba todas las etiquetas presentes para ver si alguna solapa el �rea indicada.
        //si no hay ninguna intersaecci�n, devuelve true
        int contador;
        boolean contenido_x;
        boolean contenido_y;
        Tipo_Etiqueta etiqueta;
        for (contador=lista_etiquetas_colocadas.size()-1;contador>=0;contador--) {
            etiqueta=(Tipo_Etiqueta) lista_etiquetas_colocadas.elementAt(contador);
            contenido_x=false;
            contenido_y=false;
            if ((xmin >= etiqueta.xmin && xmin <= etiqueta.xmax) || (xmax >= etiqueta.xmin && xmax <= etiqueta.xmax) || (etiqueta.xmin >= xmin && etiqueta.xmin <= xmax) || (etiqueta.xmax >= xmin && etiqueta.xmax <= xmax)) {
                contenido_x = true; //un marco vertical est� entre dos marcos
            }
            if ((ymin >= etiqueta.ymin && ymin <= etiqueta.ymax) || (ymax >= etiqueta.ymin && ymax <= etiqueta.ymax) || (etiqueta.ymin >= ymin && etiqueta.ymin <= ymax) || (etiqueta.ymax >= ymin && etiqueta.ymax <= ymax)) {
                contenido_y = true; //un marco vertical est� entre dos marcos
            }
            if (contenido_x==true && contenido_y==true) return false;
            
        }
        return true;
    }
    private class Tipo_Etiqueta {
        //contiene el texto a mostrar y los l�mites
        public String texto;
        public Font fuente;
        public int xmin;
        public int ymin;
        public int xmax;
        public int ymax;
        public int tipo;
        public int pendiente;
        public Tipo_Etiqueta(String texto,Font fuente,int xmin,int ymin, int xmax,int ymax,int tipo,int pendiente) {
            this.texto=texto;
            this.fuente=fuente;
            this.xmin=xmin;
            this.ymin=ymin;
            this.xmax=xmax;
            this.ymax=ymax;
            this.tipo=tipo;
            this.pendiente=pendiente;
        }
    }
}
