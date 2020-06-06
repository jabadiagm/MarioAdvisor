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
    //crea un GUI para crear/editar la configuraci�n del programa
    public Form formulario_configuracion; //contenedor de los campos de configuraci�n
    //controles del formulario
    private TextField tx_gps_url;
    private TextField tx_factor_mapa;
    private TextField tx_detalle_minimo_mapa_general;
    private TextField tx_tama�o_cache_mapas;
    private ChoiceGroup ch_habilitar_acceso_externo;
    private ChoiceGroup ch_ruta_acceso_externo;
    private ChoiceGroup ch_opciones_tracklog;
    private TextField tx_tama�o_tracklog;
    //comandos
    private Command cmd_atras;
    private Command cmd_guardar;
    //nueva ruta de archivos
    private String nueva_ruta_archivos;
    //objetos externos
    private Configuracion configuracion;
    private Visor_IMG midlet;
    
    
    /** Creates a new instance of Config_Canvas */
    public Config_Canvas(Configuracion config,Visor_IMG Midlet) {
        midlet=Midlet;
        configuracion=config;
        formulario_configuracion=new Form("Settings");
        //creaci�n de los controles
        tx_gps_url=new TextField("GPS URL","666",100,TextField.ANY); //los textfiels no se pueden definir vac�os en los sony ericsson
        tx_factor_mapa=new TextField("MAP SIZE FACTOR","66",2,TextField.NUMERIC);
        tx_detalle_minimo_mapa_general=new TextField("OVERVIEW MAP DETAIL","66",2,TextField.NUMERIC);
        tx_tama�o_cache_mapas=new TextField("MAPS CACHE SIZE","66",2,TextField.NUMERIC);
        ch_habilitar_acceso_externo=new ChoiceGroup("External File Access",ChoiceGroup.MULTIPLE);
        ch_ruta_acceso_externo=new ChoiceGroup("External Data Folder",ChoiceGroup.EXCLUSIVE);
        ch_opciones_tracklog=new ChoiceGroup("Tracklog Options",ChoiceGroup.MULTIPLE);
        tx_tama�o_tracklog=new TextField("Tracklog Size","66",5,TextField.NUMERIC);
        formulario_configuracion.append(tx_gps_url);
        formulario_configuracion.append(tx_factor_mapa);
        formulario_configuracion.append(tx_detalle_minimo_mapa_general);
        formulario_configuracion.append(tx_tama�o_cache_mapas);
        formulario_configuracion.append(ch_habilitar_acceso_externo);
        formulario_configuracion.append(ch_ruta_acceso_externo);
        formulario_configuracion.append(ch_opciones_tracklog);
        formulario_configuracion.append(tx_tama�o_tracklog);
        //formulario_configuracion.append(nose);
        cmd_atras=new Command("Back",Command.EXIT,0);
        cmd_guardar=new Command("Save",Command.OK,0);
        formulario_configuracion.addCommand(cmd_atras);
        formulario_configuracion.addCommand(cmd_guardar);
        if (configuracion.estado==configuracion.Estado_OK) { //carga los valores en los controles
            tx_gps_url.setString(configuracion.GPS_url);
            tx_factor_mapa.setString(new Integer(configuracion.factor_mapa).toString());
            tx_detalle_minimo_mapa_general.setString(new Integer(configuracion.detalle_minimo_mapa_general).toString());
            tx_tama�o_cache_mapas.setString(new Integer(configuracion.tama�o_cache_mapas).toString());
            ch_habilitar_acceso_externo.append("Enabled",null); //a�ade el �nico elemento de la lista
            if (configuracion.acceso_archivos_habilitado==true) { //marca la casilla
                ch_habilitar_acceso_externo.setSelectedIndex(0,true);
            }
            ch_ruta_acceso_externo.append(new Character((char)34).toString()+configuracion.ruta_carpeta_archivos+new Character((char)34).toString(),null);
            if (configuracion.acceso_archivos_habilitado==true) { //permite seleccionar carpeta
                ch_ruta_acceso_externo.append("Change",null);
            }
            //opciones del tracklog
            ch_opciones_tracklog.append("Enable",null);
            ch_opciones_tracklog.append("Save on Exit",null);
            ch_opciones_tracklog.append("Collect CellID Data",null);
            if (configuracion.tracklog_activado==true) ch_opciones_tracklog.setSelectedIndex(0,true);
            if (configuracion.guardar_tracklog_salir==true) ch_opciones_tracklog.setSelectedIndex(1,true);
            if (configuracion.recoger_cellid==true) ch_opciones_tracklog.setSelectedIndex(2,true);
            tx_tama�o_tracklog.setString(new Integer(configuracion.tama�o_tracklog).toString());
            
        }
        formulario_configuracion.setCommandListener(this);
        formulario_configuracion.setItemStateListener(this);
        
    }
    
    public void itemStateChanged(Item item) {
        //gesti�n de eventos del formulario
        if (item==ch_ruta_acceso_externo && ch_habilitar_acceso_externo.isSelected(0)==true) {
            midlet.mostrar_explorador();
        } else if (item==ch_habilitar_acceso_externo) {
            if (configuracion.JSR75_disponible==false) {//no hay posibilidad de acceso externo
                ch_habilitar_acceso_externo.setSelectedIndex(0,false);
            } else {
                if (ch_habilitar_acceso_externo.isSelected(0)==true && ch_ruta_acceso_externo.size()==1) { //permite seleccionar carpeta
                    ch_ruta_acceso_externo.append("Change",null);
                } else if (ch_habilitar_acceso_externo.isSelected(0)==false && ch_ruta_acceso_externo.size()==2) {
                    ch_ruta_acceso_externo.delete(1);
                }
            }
        }
    }
    
    public void commandAction(Command command, Displayable displayable) {
        
        if (command==cmd_atras) {
            midlet.mostrar_img_canvas();
        } else if (command==cmd_guardar) {
            int valor_int;
            String cadena;
            String GPS_url_antigua;
            String ruta_acceso_externo_antigua;
            int factor_mapa_antiguo;
            int detalle_minimo_mapa_general_antiguo;
            int tama�o_cache_mapas_antiguo;
            int tama�o_tracklog_antiguo;
            boolean acceso_archivos_habilitado_antiguo;
            boolean tracklog_activado_antiguo;
            boolean guardar_tracklog_salir_antiguo;
            boolean recoger_cellid_antiguo;
            boolean tracklog_visible_antiguo;
            //guarda los valores anteriores
            GPS_url_antigua=configuracion.GPS_url;
            factor_mapa_antiguo=configuracion.factor_mapa;
            detalle_minimo_mapa_general_antiguo=configuracion.detalle_minimo_mapa_general;
            tama�o_cache_mapas_antiguo=configuracion.tama�o_cache_mapas;
            acceso_archivos_habilitado_antiguo=configuracion.acceso_archivos_habilitado;
            ruta_acceso_externo_antigua=configuracion.ruta_carpeta_archivos;
            tracklog_activado_antiguo=configuracion.tracklog_activado;
            guardar_tracklog_salir_antiguo=configuracion.guardar_tracklog_salir;
            recoger_cellid_antiguo=configuracion.recoger_cellid;
            tracklog_visible_antiguo=configuracion.tracklog_visible;
            tama�o_tracklog_antiguo=configuracion.tama�o_tracklog;
            configuracion.GPS_url=tx_gps_url.getString(); //carga la direcci�n del GPS
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
            if (valor_int==0 || valor_int>7) {
                midlet.mensaje_error("Invalid OVERVIEW MAP DETAIL.");
                return;
            }
            configuracion.detalle_minimo_mapa_general=valor_int;
            cadena=tx_tama�o_cache_mapas.getString();
            if (cadena==null | cadena.length()==0) {
                midlet.mensaje_error("Invalid MAPS CACHE SIZE.");
                return;
            }
            valor_int=Integer.valueOf(cadena).intValue();
            if (valor_int<1) {
                midlet.mensaje_error("Invalid MAPS CACHE SIZE.");
                return;
            }
            configuracion.tama�o_cache_mapas=valor_int;
            if (ch_habilitar_acceso_externo.isSelected(0)==true) {
                configuracion.acceso_archivos_habilitado=true;
            } else {
                configuracion.acceso_archivos_habilitado=false;
            }
            if (ch_opciones_tracklog.isSelected(0)==true) {
                configuracion.tracklog_activado=true;
            } else {
                configuracion.tracklog_activado=false;
            }    
            if (ch_opciones_tracklog.isSelected(1)==true) {
                configuracion.guardar_tracklog_salir=true;
            } else {
                configuracion.guardar_tracklog_salir=false;
            } 
            if (ch_opciones_tracklog.isSelected(2)==true) {
                configuracion.recoger_cellid=true;
            } else {
                configuracion.recoger_cellid=false;
            } 
            cadena=tx_tama�o_tracklog.getString();
            valor_int=Integer.valueOf(cadena).intValue();
            if (valor_int<1) {
                midlet.mensaje_error("Invalid TRACKLOG SIZE.");
                return;
            }     
            configuracion.tama�o_tracklog=valor_int;
            if (nueva_ruta_archivos!=null) configuracion.ruta_carpeta_archivos=nueva_ruta_archivos;
            //antes de guardar la configuraci�n, comprueba que haya cambiado
            if (hay_cambios_configuracion(GPS_url_antigua,factor_mapa_antiguo,detalle_minimo_mapa_general_antiguo,tama�o_cache_mapas_antiguo,acceso_archivos_habilitado_antiguo,ruta_acceso_externo_antigua,tracklog_activado_antiguo,guardar_tracklog_salir_antiguo,recoger_cellid_antiguo,tracklog_visible_antiguo,tama�o_tracklog_antiguo)==true)  {
                valor_int=configuracion.guardar_configuracion();
                if (valor_int!=0) {
                    midlet.mensaje_error("Error saving config.");
                }
                //midlet.arrancar_img_canvas();
                //proceso provisional de cambios. algunos obligan a reiniciar el programa, otros 
                //necesitan alg�n ajuste y otros no necesitan nada
                //opciones que afectan a la visualizaci�n
                if (factor_mapa_antiguo!=configuracion.factor_mapa) { //cambio de tama�o del lienzo
                    midlet.ajustar_parametros_pantalla();
                    midlet.regenerar_mapa();
                }
                //opciones del tracklog
                if (tracklog_activado_antiguo!=configuracion.tracklog_activado || guardar_tracklog_salir_antiguo!=configuracion.guardar_tracklog_salir || recoger_cellid_antiguo!=configuracion.recoger_cellid ||tama�o_tracklog_antiguo!=configuracion.tama�o_tracklog) {
                    midlet.inicializar_tracklog();
                }
                //opciones que afectan al gestor de mapas
                if (detalle_minimo_mapa_general_antiguo!=configuracion.detalle_minimo_mapa_general || 
                        tama�o_cache_mapas_antiguo!=configuracion.tama�o_cache_mapas || 
                        acceso_archivos_habilitado_antiguo!=configuracion.acceso_archivos_habilitado || 
                        ruta_acceso_externo_antigua.compareTo(configuracion.ruta_carpeta_archivos)!=0) { //por ahora hay que reiniciar para que esto funcione
                    midlet.mensaje_advertencia("You must restart for changes to take effect.");
                }

            }
            
        }
    }
    
    private boolean hay_cambios_configuracion(String GPS_url_antigua, int factor_mapa_antiguo, 
              int detalle_minimo_mapa_general_antiguo,int tama�o_cache_mapas_antiguo,boolean acceso_archivos_habilitado_antiguo,
              String ruta_acceso_externo_antigua,boolean tracklog_activado_antiguo,boolean guardar_tracklog_salir_antiguo,
              boolean recoger_cellid_antiguo,boolean tracklog_visible_antiguo,int tama�o_tracklog_antiguo) {
        //devuelve true si alguno de los valores es distinto del presente en la configuraci�n actual
        if (GPS_url_antigua.compareTo(configuracion.GPS_url)!=0 ||
                factor_mapa_antiguo!=configuracion.factor_mapa ||
                detalle_minimo_mapa_general_antiguo!=configuracion.detalle_minimo_mapa_general ||
                tama�o_cache_mapas_antiguo!=configuracion.tama�o_cache_mapas || 
                acceso_archivos_habilitado_antiguo!=configuracion.acceso_archivos_habilitado ||
                ruta_acceso_externo_antigua.compareTo(configuracion.ruta_carpeta_archivos)!=0 || 
                tracklog_activado_antiguo!=configuracion.tracklog_activado || 
                guardar_tracklog_salir_antiguo!=configuracion.guardar_tracklog_salir  ||
                recoger_cellid_antiguo!=configuracion.recoger_cellid ||
                tracklog_visible_antiguo!=configuracion.tracklog_visible ||
                tama�o_tracklog_antiguo!=configuracion.tama�o_tracklog){
            return true;
        }
        return false;
    }
    public void definir_nueva_ruta_archivos(String nueva_ruta) {
        nueva_ruta_archivos=nueva_ruta;
        ch_ruta_acceso_externo.deleteAll();
        ch_ruta_acceso_externo.append(new Character((char)34).toString()+nueva_ruta+new Character((char)34).toString(),null);
        ch_ruta_acceso_externo.append("Change",null);
        
    }
    
}
