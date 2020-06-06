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
//formulario con datos de entrada y resultados de b�squeda    
    public Form frm_buscar;
    //contenido del formulario
    private ChoiceGroup ch_puntos_lineas_poligonos; //tipo de elementos a buscar
    private ChoiceGroup ch_identificador_elemento; //ID de los puntos/l�neas o pol�gonos a buscar. dos bytes para puntos, y 1 para resto
    private ChoiceGroup ch_buscar_por_distancia; //checkbox para buscar por nombre o por distancia
    private TextField tx_buscar; //texto a buscar
    private ChoiceGroup ch_resultados; //resultados de la b�squeda
    //private StringItem st_linea_estado; //informaci�n del proceso de b�squeda � n�mero de resultados
    //comandos de men�
    private Command cmd_buscar; //bot�n de comienzo de b�squeda
    private Command cmd_cancelar; //bot�n de cancelaci�n de b�squeda
    private Command cmd_volver; //vuelta a img_canvas
    //private Vector resultados; //los resultados de b�squeda deben ser p�blicos dentro del objeto
    //objetos externos necesarios para funcionar
    private Gestor_Mapas gestor_mapas;
    private Visor_IMG midlet;
    private Tipo_Busqueda busqueda;
    //coordenadas actuales, para b�squedas por distancia
    private float longitud_actual;
    private float latitud_actual;
    public Buscar_Canvas(Visor_IMG midlet,Gestor_Mapas gestor_mapas)  {
        this.midlet=midlet;
        this.gestor_mapas=gestor_mapas;
        frm_buscar=new Form("Search"); //crea el contenedor
        //creaci�n de los controles
        ch_puntos_lineas_poligonos=new ChoiceGroup("Search for",Choice.POPUP,new String [] {"Cities/Points","Roads/Streets","Areas"},null);
        ch_identificador_elemento=new ChoiceGroup("Element type",Choice.POPUP); //el contenido se carga de acuerdo al valor del control anterior
        ch_buscar_por_distancia=new ChoiceGroup("Search Options",Choice.MULTIPLE,new String [] {"Find nearest"},null);
        tx_buscar=new TextField("Search String","",32,TextField.ANY);
        //st_linea_estado=new StringItem("Linea_Estado","Inactive",Item.PLAIN);
        ch_resultados=new ChoiceGroup("No Results Found",Choice.MULTIPLE);
        //creaci�n de los comandos
        cmd_buscar=new Command("Search",Command.EXIT,0);
        cmd_cancelar=new Command("Cancel",Command.EXIT,0);
        cmd_volver=new Command("Back",Command.EXIT,0);
        //se a�aden los controles al formulario
        frm_buscar.append(tx_buscar);
        //frm_buscar.append(ch_puntos_lineas_poligonos);
        frm_buscar.append(ch_identificador_elemento);
        //frm_buscar.append(ch_buscar_por_distancia);
        frm_buscar.append(ch_resultados);
        //frm_buscar.append(st_linea_estado);
        colocar_comandos_buscar();
        
        //por defecto carga los datos de los puntos
        cargar_tipos_puntos();
        //gesti�n de eventos
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
        if (item==ch_puntos_lineas_poligonos) { //cambio en selecci�n de elementos a buscar
            if (ch_puntos_lineas_poligonos.getSelectedIndex()==0) cargar_tipos_puntos();
            if (ch_puntos_lineas_poligonos.getSelectedIndex()==1) cargar_tipos_lineas();
        } else if (item==ch_resultados) { //se ha marcado un elemento
            //por ahora, al pulsar, directamente se salta a las coordenadas del sitio
            int contador; //�ndice del elemento seleccionado
            Tipo_Resultado_Busqueda resultado;
            for (contador=0;contador<ch_resultados.size();contador++){
                if (ch_resultados.isSelected(contador)==true) break; //elemento selccionado encontrado. s�lo deber�a haber uno
            }
            resultado=(Tipo_Resultado_Busqueda) busqueda.resultados_busqueda.elementAt(contador);
            midlet.mostrar_img_canvas_con_cambio_coordenadas(resultado.longitud,resultado.latitud,5);
            
        }
    }

    public void commandAction(Command command, Displayable displayable) {
        
        if (command==cmd_volver) midlet.mostrar_img_canvas(); //vuelve al programa principal
        else if (command==cmd_buscar) { //crea los datos de b�squeda y llama al gestor de mapas
            
            Tipo_Criterios_Busqueda criterios_busqueda;
            colocar_linea_estado("Searching...");
            criterios_busqueda=rellenar_criterios_busqueda();
            busqueda=new Tipo_Busqueda(criterios_busqueda,gestor_mapas,this); //define el objeto de b�squeda
            //quita el bot�n de buscar y lo reemplaza por el de cancelar. hay que quitarlos todos para que no cambie el orden
            frm_buscar.removeCommand(cmd_buscar);
            frm_buscar.removeCommand(cmd_volver);
            colocar_comandos_cancelar();
            busqueda.buscar(); //arranca la b�squeda
        } else if (command==cmd_cancelar) { 
            busqueda.cancelar_busqueda();
            frm_buscar.removeCommand(cmd_cancelar);
            frm_buscar.removeCommand(cmd_volver);
            colocar_comandos_buscar();
        }
    }
    private void cargar_tipos_puntos() {
        //coloca en el choicegroup de tipos de elementos los valores correspondientes a puntos.
        //s�lo se colocan tipos generales
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
        //coloca en el choicegroup de tipos de elementos los valores correspondientes a polil�neas.
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
        if (ch_buscar_por_distancia.isSelected(0)==true) ordenar_por_distancia=true;
        codigo_elementos=ch_identificador_elemento.getSelectedIndex();
        if (codigo_elementos==0) { //se busca cualquier tipo
            buscar_por_codigo=false;
        } else { //se ha especificado un tipo hay que definir el c�digo
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
        criterios_busqueda=new Tipo_Criterios_Busqueda(codigo_elementos,tx_buscar.getString(),ordenar_por_distancia,elementos_a_buscar,buscar_por_codigo);
        return criterios_busqueda;
    }
    private void colocar_resultados(Vector resultados) {
        //coloca los resultados en el control. si no hay ninguno, 
        //lo indica en la l�nea de estado
        int contador;
        ch_resultados.deleteAll();
        if (resultados==null || resultados.size()==0) {
            colocar_linea_estado ("No Results Found.");
            return;
        }
        colocar_linea_estado ("Searching. "+new Integer(resultados.size()).toString()+" Found"); //cuenta el n�mero de resuldados
        //si hay valores validos en el vector de entrada, los coloca
        for (contador=0;contador<resultados.size();contador++) {
            ch_resultados.append(((Tipo_Resultado_Busqueda)resultados.elementAt(contador)).etiqueta,null);
        }
    }
    private void colocar_linea_estado(String texto) {
        //coloca el texto dado en la l�nea de estado del formulario de b�squeda
        //st_linea_estado.setText(texto);
        ch_resultados.setLabel(texto);
    }
    public void notificar_nuevos_resultados() {
        //funci�n llamada desde el objeto de b�squeda para indicar que hay m�s resultados
        colocar_resultados (busqueda.resultados_busqueda);
    }
    public void notificar_final_busqueda() {
        //funci�n llamada desde el objeto de b�squeda para indicar que la b�squeda ha terminado
        colocar_linea_estado("Done. "+new Integer (busqueda.resultados_busqueda.size()).toString()+" Found");
        //vuelve a dejar los comandos como al principio
        frm_buscar.removeCommand(cmd_cancelar);
        frm_buscar.removeCommand(cmd_volver);
        colocar_comandos_buscar();
        
    }
}