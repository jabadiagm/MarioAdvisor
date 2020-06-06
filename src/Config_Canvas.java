import javax.microedition.lcdui.*;
/*
 * Config_Canvas.java
 *
 * Created on 22 de marzo de 2008, 12:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Config_Canvas implements ItemStateListener,CommandListener{
    //crea un GUI para crear/editar la configuración del programa
    public Form formulario_configuracion; //contenedor de los campos de configuración
    private TextField tx_gps_url;
    private TextField tx_factor_mapa;
    private TextField tx_detalle_minimo_mapa_general;
    private TextField nose;
    private Configuracion configuracion;
    private Command cmd_atras;
    private Command cmd_guardar;
    private Visor_IMG midlet;
    
    
    /** Creates a new instance of Config_Canvas */
    public Config_Canvas(Configuracion config,Visor_IMG Midlet) {
        midlet=Midlet;
        configuracion=config;
        formulario_configuracion=new Form("Settings");
        tx_gps_url=new TextField("GPS URL","666",100,TextField.ANY); //los textfiels no se pueden definir vacíos en los sony ericsson
        tx_factor_mapa=new TextField("MAP SIZE FACTOR","66",2,TextField.NUMERIC);
        tx_detalle_minimo_mapa_general=new TextField("OVERVIEW MAP DETAIL","66",2,TextField.NUMERIC);
        //nose=new TextField("Lon",new Float(configuracion.centro_longitud_inicial).toString(),5,TextField.ANY);
        formulario_configuracion.append(tx_gps_url);
        formulario_configuracion.append(tx_factor_mapa);
        formulario_configuracion.append(tx_detalle_minimo_mapa_general);
        //formulario_configuracion.append(nose);
        cmd_atras=new Command("Back",Command.EXIT,0);
        cmd_guardar=new Command("Save",Command.OK,0);
        formulario_configuracion.addCommand(cmd_atras);
        formulario_configuracion.addCommand(cmd_guardar);
        if (configuracion.estado==configuracion.Estado_OK) { //carga los valores en los controles
            tx_gps_url.setString(configuracion.GPS_url);
            tx_factor_mapa.setString(new Integer(configuracion.factor_mapa).toString());
            tx_detalle_minimo_mapa_general.setString(new Integer(configuracion.detalle_minimo_mapa_general).toString());
        }
        formulario_configuracion.setCommandListener(this);
        
    }
    
    public void itemStateChanged(Item item) {
        //gestón de eventos del formulario
    }
    
    public void commandAction(Command command, Displayable displayable) {
        
        if (command==cmd_atras) {
            midlet.mostrar_img_canvas();
        } else if (command==cmd_guardar) {
            int valor_int;
            String cadena;
            String GPS_url_antigua;
            int factor_mapa_antiguo;
            int detalle_minimo_mapa_general_antiguo;
            //guarda los valores anteriores
            GPS_url_antigua=configuracion.GPS_url;
            factor_mapa_antiguo=configuracion.factor_mapa;
            detalle_minimo_mapa_general_antiguo=configuracion.detalle_minimo_mapa_general;
            configuracion.GPS_url=tx_gps_url.getString(); //carga la dirección del GPS
            cadena=tx_factor_mapa.getString();
            if (cadena==null | cadena.length()==0) {
                midlet.mensaje_error("Invalid MAP FACTOR.");
                return;
            }
            valor_int=Integer.valueOf(cadena).intValue();
            if (valor_int<2 || valor_int>10) {
                midlet.mensaje_error("Invalid MAP FACTOR.");
                return;
            }
            configuracion.factor_mapa=valor_int; //carga el nuevo factor de mapa
            cadena=tx_detalle_minimo_mapa_general.getString();
            if (cadena==null | cadena.length()==0) {
                midlet.mensaje_error("Invalid OVERVIEW MAP DETAIL.");
                return;
            }
            valor_int=Integer.valueOf(cadena).intValue();
            if (valor_int==0 || valor_int>4) {
                midlet.mensaje_error("Invalid OVERVIEW MAP DETAIL.");
                return;
            }            
            configuracion.detalle_minimo_mapa_general=valor_int;
            //antes de guardar la configuración, comprueba que haya cambiado
            if (hay_cambios_configuracion(GPS_url_antigua,factor_mapa_antiguo,detalle_minimo_mapa_general_antiguo)==true) {
                midlet.mensaje_advertencia("You must restart for changes to take effect.");
                valor_int=configuracion.guardar_configuracion();
                if (valor_int!=0) {
                    midlet.mensaje_error("Error saving config.");
                }
            }
            
        }
    }

    private boolean hay_cambios_configuracion(String GPS_url_antigua, int factor_mapa_antiguo, int detalle_minimo_mapa_general_antiguo) {
        //devuelve true si alguno de los valores es distinto del presente en la configuración actual
        if (GPS_url_antigua.compareTo(configuracion.GPS_url)!=0 || factor_mapa_antiguo!=configuracion.factor_mapa || detalle_minimo_mapa_general_antiguo!=configuracion.detalle_minimo_mapa_general) {
            return true;
        }
        return false;
    }

}
