import java.util.Vector;
import javax.microedition.lcdui.*;
/*
 * Buscar_Canvas.java
 *
 * Created on 28 de mayo de 2008, 17:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Buscar_Canvas implements ItemStateListener,CommandListener{
//formulario con datos de entrada y resultados de búsqueda    
    public Form frm_buscar;
    //contenido del formulario
    private ChoiceGroup ch_puntos_lineas_poligonos; //tipo de elementos a buscar
    private ChoiceGroup ch_identificador_elemento; //ID de los puntos/líneas o polígonos a buscar. dos bytes para puntos, y 1 para resto
    private ChoiceGroup ch_buscar_por_distancia; //checkbox para buscar por nombre o por distancia
    private TextField tx_buscar; //texto a buscar
    private ChoiceGroup ch_resultados; //resultados de la búsqueda
    //private StringItem st_linea_estado; //información del proceso de búsqueda ó número de resultados
    //comandos de menú
    private Command cmd_buscar; //botón de comienzo de búsqueda
    private Command cmd_cancelar; //botón de cancelación de búsqueda
    private Command cmd_volver; //vuelta a img_canvas
    //private Vector resultados; //los resultados de búsqueda deben ser públicos dentro del objeto
    //objetos externos necesarios para funcionar
    private Gestor_Mapas gestor_mapas;
    private Visor_IMG midlet;
    private IMG_Canvas img_canvas; //acceso a las coordenadas del centro de pantalla
    private Tipo_Busqueda busqueda;
    //coordenadas actuales, para búsquedas por distancia
    private float longitud_actual;
    private float latitud_actual;
    
    private Tipo_Resultado_Busqueda resultado;
    public Buscar_Canvas(Visor_IMG midlet,Gestor_Mapas gestor_mapas,IMG_Canvas img_canvas)  {
        this.midlet=midlet;
        this.gestor_mapas=gestor_mapas;
        this.img_canvas=img_canvas;
        frm_buscar=new Form("Search"); //crea el contenedor
        //creación de los controles
        ch_puntos_lineas_poligonos=new ChoiceGroup("Search for",Choice.POPUP,new String [] {"Cities/Points","Roads/Streets","Areas"},null);
        ch_identificador_elemento=new ChoiceGroup("Element type",Choice.POPUP); //el contenido se carga de acuerdo al valor del control anterior
        ch_buscar_por_distancia=new ChoiceGroup("Search Options",Choice.MULTIPLE,new String [] {"Find nearest"},null);
        tx_buscar=new TextField("Search String","",32,TextField.ANY);
        //st_linea_estado=new StringItem("Linea_Estado","Inactive",Item.PLAIN);
        ch_resultados=new ChoiceGroup("No Results Found",Choice.MULTIPLE);
        //creación de los comandos
        cmd_buscar=new Command("Search",Command.EXIT,0);
        cmd_cancelar=new Command("Cancel",Command.EXIT,0);
        cmd_volver=new Command("Back",Command.EXIT,0);
        //se añaden los controles al formulario
        frm_buscar.append(tx_buscar);
        //frm_buscar.append(ch_puntos_lineas_poligonos);
        frm_buscar.append(ch_identificador_elemento);
        frm_buscar.append(ch_buscar_por_distancia);
        frm_buscar.append(ch_resultados);
        ch_buscar_por_distancia.setSelectedIndex(0,true); //marca por defecto la búsqueda por distancia
        //frm_buscar.append(st_linea_estado);
        colocar_comandos_buscar();
        
        //por defecto carga los datos de los puntos
        cargar_tipos_puntos();
        //gestión de eventos
        frm_buscar.setCommandListener(this);
        frm_buscar.setItemStateListener(this);
    }
    private void colocar_comandos_buscar () {
        //coloca los comandos de buscar y volver
        frm_buscar.addCommand(cmd_buscar);
        frm_buscar.addCommand(cmd_volver);
    }
     private void colocar_comandos_cancelar () {
        //coloca los comandos de cancelar y volver
        frm_buscar.addCommand(cmd_cancelar);
        frm_buscar.addCommand(cmd_volver);
    }   

    public void itemStateChanged(Item item) {
        if (item==ch_puntos_lineas_poligonos) { //cambio en selección de elementos a buscar
            if (ch_puntos_lineas_poligonos.getSelectedIndex()==0) cargar_tipos_puntos();
            if (ch_puntos_lineas_poligonos.getSelectedIndex()==1) cargar_tipos_lineas();
        } else if (item==ch_resultados) { //se ha marcado un elemento
            //por ahora, al pulsar, directamente se salta a las coordenadas del sitio
            //si se está en medio de una búsqueda, antes hay que cancelarla
            if (busqueda.estado==Tipo_Busqueda.estado_buscando) {
                busqueda.cancelar_busqueda();
                while (busqueda.estado!=Tipo_Busqueda.estado_inactivo);
            }
            int contador; //índice del elemento seleccionado
            //Tipo_Resultado_Busqueda resultado;
            for (contador=0;contador<ch_resultados.size();contador++){
                if (ch_resultados.isSelected(contador)==true) break; //elemento selccionado encontrado. sólo debería haber uno
            }
            resultado=(Tipo_Resultado_Busqueda) busqueda.resultados_busqueda.elementAt(contador);
            new Thread(new Runnable() {
                public void run() {
                    midlet.mostrar_img_canvas_con_cambio_coordenadas(resultado.longitud,resultado.latitud,5);
                }
            }).start();
        }
    }

    public void commandAction(Command command, Displayable displayable) {
        
        if (command==cmd_volver) {
            if (busqueda!=null) { //hay un objeto de búsqueda, hay que ver si está trabajando
                if (busqueda.estado==Tipo_Busqueda.estado_buscando) busqueda.cancelar_busqueda();
                busqueda=null;
            }
            midlet.mostrar_img_canvas(); //vuelve al programa principal
        }
        else if (command==cmd_buscar) { //crea los datos de búsqueda y llama al gestor de mapas
            //hay un bug en los sony ericsson alquitar los resultados.
            //se quita del formulario antes de borrarlos, para luego volver a añadirlo
            //frm_buscar.delete(frm_buscar.size()-1); 
            ch_resultados.deleteAll(); //limpia los resultados que pudiera haber
            //frm_buscar.append(ch_resultados);
            Tipo_Criterios_Busqueda criterios_busqueda;
            colocar_linea_estado("Searching...");
            criterios_busqueda=rellenar_criterios_busqueda();
            busqueda=new Tipo_Busqueda(criterios_busqueda,gestor_mapas,this); //define el objeto de búsqueda
            //quita el botón de buscar y lo reemplaza por el de cancelar. hay que quitarlos todos para que no cambie el orden
            frm_buscar.removeCommand(cmd_buscar);
            frm_buscar.removeCommand(cmd_volver);
            colocar_comandos_cancelar();
            busqueda.buscar(); //arranca la búsqueda
        } else if (command==cmd_cancelar) { 
            busqueda.cancelar_busqueda();
            while (busqueda.estado!=Tipo_Busqueda.estado_inactivo);
            frm_buscar.removeCommand(cmd_cancelar);
            frm_buscar.removeCommand(cmd_volver);
            colocar_comandos_buscar();
        }
    }
    private void cargar_tipos_puntos() {
        //coloca en el choicegroup de tipos de elementos los valores correspondientes a puntos.
        //sólo se colocan tipos generales
        ch_identificador_elemento.deleteAll(); //borra lo que hubiera
        ch_identificador_elemento.append("Any",null);
        ch_identificador_elemento.append("City",null);
        ch_identificador_elemento.append("Food & Drink",null);
        ch_identificador_elemento.append("Lodging",null);
        ch_identificador_elemento.append("Attraction",null);
        ch_identificador_elemento.append("Entertainment",null);
        ch_identificador_elemento.append("Shopping",null);
        ch_identificador_elemento.append("Service",null);
        ch_identificador_elemento.append("Emergency/Government",null);
        //ch_identificador_elemento.append("Man-made Feature",null);
        //ch_identificador_elemento.append("Water Feature",null);
        //ch_identificador_elemento.append("Land Feature",null);
    }
    private void cargar_tipos_lineas() {
        //coloca en el choicegroup de tipos de elementos los valores correspondientes a polilíneas.
        ch_identificador_elemento.deleteAll(); //borra lo que hubiera
        ch_identificador_elemento.append("Any",null);
        ch_identificador_elemento.append("calle",null);
        ch_identificador_elemento.append("carretera",null);
        ch_identificador_elemento.append("autopista",null);
    }    
    private Tipo_Criterios_Busqueda rellenar_criterios_busqueda() {
        //define los criterios basados en los controles de pantalla
        Tipo_Criterios_Busqueda criterios_busqueda;
        int elementos_a_buscar;
        int codigo_elementos;
        boolean ordenar_por_distancia=false;
        boolean buscar_por_codigo;
        float longitud_origen=0,latitud_origen=0;
        elementos_a_buscar=ch_puntos_lineas_poligonos.getSelectedIndex();
        switch (elementos_a_buscar) {
            case 0:
                elementos_a_buscar=Tipo_Criterios_Busqueda.Tipo_Busqueda_Puntos;
                break;
            case 1:
                elementos_a_buscar=Tipo_Criterios_Busqueda.Tipo_Busqueda_Polilineas;
                break;
            case 2:
                elementos_a_buscar=Tipo_Criterios_Busqueda.Tipo_Busqueda_Poligonos;
                break;
        }
        //hacen falta las coordenadas del centro de búsqueda
        longitud_origen=(float)img_canvas.centro_pantalla_Lon;
        latitud_origen=(float)img_canvas.centro_pantalla_Lat;
        if (ch_buscar_por_distancia.isSelected(0)==true) {
            ordenar_por_distancia=true;
        }
        codigo_elementos=ch_identificador_elemento.getSelectedIndex();
        if (codigo_elementos==0) { //se busca cualquier tipo
            buscar_por_codigo=false;
        } else { //se ha especificado un tipo hay que definir el código
            buscar_por_codigo=true;
            int [] codigos_elementos={0,0,0x2a,0x2b,0x2c,0x2d,0x2e,0x2f,0x30,0x64,0x65,0x66};
            //0x2a = comida y bebida
            //0x2b = alojamiento
            //0x2c = atracciones
            //0x2d = entretenimiento
            //0x2e = tiendas
            //0x2f = servicios
            //0x30 = emergencia / gubernamentales
            codigo_elementos=codigos_elementos[codigo_elementos];
        }
        criterios_busqueda=new Tipo_Criterios_Busqueda(codigo_elementos,tx_buscar.getString(),ordenar_por_distancia,elementos_a_buscar,buscar_por_codigo,longitud_origen,latitud_origen,gestor_mapas.radio_busqueda);
        return criterios_busqueda;
    }
    private void colocar_resultados(Vector resultados) {
        //coloca los resultados en el control. si no hay ninguno, 
        //lo indica en la línea de estado
        int contador;
        float distancia;
        String cadena="";
        ch_resultados.deleteAll();
        if (resultados==null || resultados.size()==0) {
            colocar_linea_estado ("No Results Found.");
            return;
        }
        colocar_linea_estado ("Searching. "+new Integer(resultados.size()).toString()+" Found"); //cuenta el número de resultados
        //si hay valores validos en el vector de entrada, los coloca
        for (contador=0;contador<resultados.size();contador++) {
            if (ch_resultados.size()>255) break; //parche para que funcione en sony ericsson
            distancia=((Tipo_Resultado_Busqueda)resultados.elementAt(contador)).distancia;
            //ajusta el formato de la distancia
            if (distancia>=100) { //sólo la parte entera
                cadena=new Integer((int)distancia).toString()+" Km";;
            } else if (distancia>=10) { //parte entera más in decimal
                cadena=new Float(distancia).toString().substring(0,4)+" Km";;
            } else if (distancia>=1) { //parte entera más in decimal
                cadena=new Float(distancia).toString().substring(0,3)+" Km";;
            } else { //distancia en metros
                cadena=new Integer((int)(distancia*1000)).toString()+" m";;
            }
            cadena=((Tipo_Resultado_Busqueda)resultados.elementAt(contador)).etiqueta+" "+cadena;
            ch_resultados.append(cadena,null);
        }
    }
    private void colocar_linea_estado(String texto) {
        //coloca el texto dado en la línea de estado del formulario de búsqueda
        //st_linea_estado.setText(texto);
        ch_resultados.setLabel(texto);
    }
    public void notificar_nuevos_resultados() {
        //función llamada desde el objeto de búsqueda para indicar que hay más resultados
        colocar_resultados (busqueda.resultados_busqueda);
    }
    public void notificar_final_busqueda() {
        //función llamada desde el objeto de búsqueda para indicar que la búsqueda ha terminado
        colocar_linea_estado("Done. "+new Integer (busqueda.resultados_busqueda.size()).toString()+" Found");
        //vuelve a dejar los comandos como al principio
        frm_buscar.removeCommand(cmd_cancelar);
        frm_buscar.removeCommand(cmd_volver);
        colocar_comandos_buscar();
        
    }
}
