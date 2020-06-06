import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
/*
 * Info_Mapas_Canvas.java
 *
 * Created on 23 de octubre de 2008, 17:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Info_Mapas_Canvas implements ItemStateListener,CommandListener{
    //muestra la lista de mapas, indicando si han sido cargados sin problemas. permite saltar de uno a otro
    public Form frm_info_mapas;
    //contenido del formulario
    private ChoiceGroup ch_lista_mapas; //tipo de elementos a buscar
    //comandos del menú
    private Command cmd_volver; //vuelta a img_canvas
    //objetos externos necesarios para funcionar
    private Gestor_Mapas gestor_mapas;
    private Visor_IMG midlet;
    
    /** Creates a new instance of Info_Mapas_Canvas */
    public Info_Mapas_Canvas(Visor_IMG midlet,Gestor_Mapas gestor_mapas) {
        String [] info_mapas;
        //ajusta los objetos externos
        this.midlet=midlet;
        this.gestor_mapas=gestor_mapas;
        //ajusta la apariencia del formulario
        frm_info_mapas=new Form("Maps Info"); //crea el contenedor
        cmd_volver=new Command("Back",Command.EXIT,0); //crea el menú
        frm_info_mapas.addCommand(cmd_volver);
        //lee la información a mostrar
        info_mapas=procesar_lista_mapas();
        ch_lista_mapas=new ChoiceGroup("List",Choice.EXCLUSIVE,info_mapas,null); //el contenido se carga de acuerdo al valor del control anterior
        frm_info_mapas.append(ch_lista_mapas);
        //gestión de eventos
        frm_info_mapas.setCommandListener(this);
        frm_info_mapas.setItemStateListener(this);
    }
    private String [] procesar_lista_mapas() {
        //toma la información del gestor de mapas y la condensa en una cadena por mapa
        int contador;
        String cadena;
        String [] resultado=new String[gestor_mapas.lista_mapas.length];
        for (contador=0;contador<gestor_mapas.lista_mapas.length;contador++) {
            cadena=extraer_nombre_mapa(gestor_mapas.lista_mapas[contador]);
            if (gestor_mapas.mapa_valido[contador]==false) { //mapa no legible
                cadena+="(failed)";
            } else { //si el mapa es válido, se añade su descripción
                cadena+=" ("+gestor_mapas.descripcion_mapa[contador].trim()+")";
            }
            resultado[contador]=cadena;
        }
        return resultado;
        
    }
    public String extraer_nombre_mapa(String mapa) {
        //quita la ruta del nombre del mapa
        int posicion;
        posicion=mapa.lastIndexOf("/".charAt(0));
        if (posicion>=0) { //hay un separador
            return mapa.substring(posicion+1);
        } else { //no hay separador (?).devuelve la misma cadena
            return mapa;
        }
        
    }
    public void itemStateChanged(Item item) {
        //por ahora, al pulsar, directamente se salta a las coordenadas del centro del mapa
        int contador; //índice del elemento seleccionado
        float longitud,latitud;
        for (contador=0;contador<ch_lista_mapas.size();contador++){
            if (ch_lista_mapas.isSelected(contador)==true) break; //elemento selccionado encontrado. sólo debería haber uno
        }
        //comprueba antes que sea un mapa válido
        if (gestor_mapas.mapa_valido[contador]==false) return;
        //obtiene las coordenadas del centro del mapa seleccionado
        longitud=(gestor_mapas.limites_mapas[contador].este+gestor_mapas.limites_mapas[contador].oeste)/2;
        latitud=(gestor_mapas.limites_mapas[contador].norte+gestor_mapas.limites_mapas[contador].sur)/2;
        midlet.mostrar_img_canvas_con_cambio_coordenadas(longitud,latitud,5);
    }
    
    public void commandAction(Command command, Displayable displayable) {
        //sólo hay una opción, que es la de volver
        midlet.mostrar_img_canvas(); //vuelve al programa principal
    }
    
}
