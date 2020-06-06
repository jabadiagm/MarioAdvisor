/*
 * Visor_IMG.java
 *
 * Created on 7 de noviembre de 2007, 19:32
 */

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 *
 * @author javier
 */
public class Visor_IMG extends MIDlet  {
    //elementos visuales del mislet
    public Display pantalla;
    private Gestor_Mapas gestor_mapas;
    private IMG_Canvas img_Canvas;
    private Configuracion configuracion;
    private Config_Canvas config_canvas;
    private BT_Canvas bt_canvas;
    private Buscar_Canvas buscar_canvas;
    private Explorador_Carpetas explorador_carpetas;
    private Tracklog tracklog;
    private Info_Mapas_Canvas info_mapas_canvas;
    //variables para el formulario de creación de carpeta
    //public Form formulario_carpeta; //selección del directorio de trabajo
    //private ChoiceGroup seleccion_raiz;
    //private Command cmd_crear_carpeta;
    //private Command cmd_salir;
    
    
    
    /** Creates a new instance of Visor_IMG */
    public Visor_IMG() {
        pantalla=this.getDisplay();
        configuracion=new Configuracion();
        //formulario_carpeta=new Form("Select Program Folder");
        //seleccion_raiz=new ChoiceGroup("Roots",ChoiceGroup.EXCLUSIVE);
        //formulario_carpeta.append(seleccion_raiz);
        //formulario_carpeta.setCommandListener(this);
        initialize();
        
    }
    
//GEN-LINE:MVDFields
    
//GEN-LINE:MVDMethods
    
    /** This method initializes UI of the application.//GEN-BEGIN:MVDInitBegin
     */
    private void initialize() {//GEN-END:MVDInitBegin
        // Insert pre-init code here
//GEN-LINE:MVDInitInit
        // Insert post-init code here
    }//GEN-LINE:MVDInitEnd
    
    /**
     * This method should return an instance of the display.
     */
    public Display getDisplay() {//GEN-FIRST:MVDGetDisplay
        return Display.getDisplay(this);
    }//GEN-LAST:MVDGetDisplay
    
    /**
     * This method should exit the midlet.
     */
    public void exitMIDlet() {//GEN-FIRST:MVDExitMidlet
        getDisplay().setCurrent(null);
        destroyApp(true);
        notifyDestroyed();
    }//GEN-LAST:MVDExitMidlet
    
    public void startApp() {
        int retorno;
        int contador;
        if (configuracion.estado!=configuracion.Estado_Pendiente_Leer_Configuracion) { //error al cargar el objeto configuración
            notifyDestroyed();
            return;
        }
        retorno=configuracion.inicializar();
        
        if (retorno==configuracion.Estado_OK) { //listo para comenzar
            arrancar_img_canvas();
        } else if (configuracion.estado==Configuracion.Estado_Error_Acceso_RMS) {
            mensaje_error("RMS access Error.");
            this.exitMIDlet();
        } else if (configuracion.estado==Configuracion.Estado_Configuracion_No_Encontrada) {
            crear_configuracion_defecto_y_arrancar();
        }
            
/*        } else { //fallo al cargar la configuración.muestra el formulario
            if (configuracion.ruta_carpeta_archivos==null) { //ruta no disponible, hay que crearla
                for (contador=0;contador<configuracion.raices.length;contador++) {
                    seleccion_raiz.append(configuracion.raices[contador],null);
                }
                cmd_salir=new Command("Exit",Command.EXIT,0);
                formulario_carpeta.addCommand(cmd_salir);
                cmd_crear_carpeta=new Command("Create",Command.OK,0);
                formulario_carpeta.addCommand(cmd_crear_carpeta);
                pantalla.setCurrent(formulario_carpeta);
            } else {
                crear_configuracion_defecto_y_arrancar();
                
            } 
        }*/
    }
    public void arrancar_img_canvas() {
        //activa la tarea principal
        //si el tracklog está activado, crea el objeto
        inicializar_tracklog();
        if (img_Canvas!=null) { //esta no es la primera vez que se arranca, hay que liberar memoria antes
            img_Canvas=null;
            System.gc();
        }
        gestor_mapas=new Gestor_Mapas(configuracion.ruta_carpeta_archivos,pantalla,configuracion.detalle_minimo_mapa_general,configuracion.tamaño_cache_mapas,configuracion.cache_etiquetas,configuracion.acceso_archivos_habilitado);
        img_Canvas=new IMG_Canvas(this,gestor_mapas,configuracion,tracklog);
        img_Canvas.inicializar(); 
        pantalla.setCurrent(img_Canvas);
    }
    public void reiniciar() {
        //crea un nuevo objeto img_canvas
        if (config_canvas!=null) config_canvas=null; //si se viene del formulario de configuración se borra
        if (img_Canvas!=null) {
            img_Canvas.cerrar(); //cierra los objetos principales
            img_Canvas=null;
            gestor_mapas=null;
            System.gc();
            gestor_mapas=new Gestor_Mapas(configuracion.ruta_carpeta_archivos,pantalla,configuracion.detalle_minimo_mapa_general,configuracion.tamaño_cache_mapas,configuracion.cache_etiquetas,configuracion.acceso_archivos_habilitado);
            img_Canvas=new IMG_Canvas(this,gestor_mapas,configuracion,tracklog);
            pantalla.setCurrent(img_Canvas);
            img_Canvas.inicializar();
        }
        
    }
    public void ajustar_parametros_pantalla() {
        //acceso a la función ajustar_parametros_pantalla del objeto img_canvas
        Displayable display_anterior;
        display_anterior=pantalla.getCurrent();
        pantalla.setCurrent(img_Canvas);
        img_Canvas.flushGraphics();
        if (configuracion.pantalla_completa==true) img_Canvas.setFullScreenMode(true);
        img_Canvas.ajustar_parametros_pantalla();
        pantalla.setCurrent(display_anterior);
    }
    public void inicializar_tracklog() {
        //crea el objeto tracklog o nó, según lo que diga la configuración
         if (configuracion.tracklog_activado==true) {
             if (tracklog!=null) { //el objeto ya existía. se borra y se libera memoria antes de reasignarlo
                 tracklog=null;
                 System.gc();
             }
            tracklog=new Tracklog(configuracion);
            //si hay un objeto gestor_GPS, hay que avisarle de que hay nuevo tracklog
            if (img_Canvas!=null) {
                img_Canvas.notificar_nuevo_tracklog(tracklog);
            }
        }  else { //si no debe existir, y existe, se anula
             tracklog=null;
             System.gc();
        }
    }
    public void mostrar_configuracion() {
        //muestra la pantalla de configuración
        if (config_canvas==null) config_canvas=new Config_Canvas(configuracion,this);
        pantalla.setCurrent(config_canvas.formulario_configuracion);
    }
    public void mostrar_img_canvas() {
        if (config_canvas!=null) config_canvas=null; //si se viene del formulario de configuración se borra
        if (bt_canvas!=null) bt_canvas=null; //si se viene del formulario de selección de GPS, se borra
        if (buscar_canvas!=null) buscar_canvas=null; //si viene de la pantalla de búsqueda, la quita
        if (explorador_carpetas!=null) explorador_carpetas=null; //si viene del explorador de carpetas, lo borra
        //muestra la pantallaprincipal
        pantalla.setCurrent(img_Canvas);
        //al volver de algún formulacio deja de ir a pantalla completa. se le hace ir manualmente
        if (configuracion.pantalla_completa==true) img_Canvas.setFullScreenMode(true);
    }
    public void mostrar_img_canvas_con_cambio_coordenadas(float longitud,float latitud,int nivel_zoom) {
        //muestra el mapa cambiando antes las coordenadas del centro. creado para ver resultados de búsqueda
        if (buscar_canvas!=null) buscar_canvas=null; //si viene de la pantalla de búsqueda, la quita
        pantalla.setCurrent(img_Canvas);
        //al volver de algún formulacio deja de ir a pantalla completa. se le hace ir manualmente
        if (configuracion.pantalla_completa==true) img_Canvas.setFullScreenMode(true);
        img_Canvas.regenerar_mapa_publico(longitud,latitud,nivel_zoom);
    }
    public void regenerar_mapa() {
        //regenera el mapa actual en la posición  actual con el zoom actual. pensado para cambios del lienzo
        img_Canvas.regenerar_mapa_publico();
    }
    public void mostrar_BT_canvas() {
        //muestra el formulario de selección de GPS bluetooth
        if (bt_canvas==null) bt_canvas=new BT_Canvas(configuracion,this);
        pantalla.setCurrent(bt_canvas.formulario_BT);
        
    }
    public void mostrar_buscar_canvas(Gestor_Mapas gestor_mapas) {
        //muestra el formulario de búsqueda de elementos del mapa
        if (buscar_canvas==null) buscar_canvas=new Buscar_Canvas(this,gestor_mapas,img_Canvas);
        pantalla.setCurrent(buscar_canvas.frm_buscar);
    }
    public void mostrar_explorador() {
        //muestra el formulario de selección de carpeta
        if (explorador_carpetas==null) explorador_carpetas=new Explorador_Carpetas (this,config_canvas);
        explorador_carpetas.explorar();
    }
    public void mostrar_info_mapas(Gestor_Mapas gestor_mapas) {
        //muestra el formulario con la lista de mapas cargados
        if (info_mapas_canvas==null) info_mapas_canvas=new Info_Mapas_Canvas (this,gestor_mapas);
        pantalla.setCurrent(info_mapas_canvas.frm_info_mapas);
    }
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
    }
    
    /*public void commandAction(Command command, Displayable displayable) {
        int retorno;
        if (command==cmd_salir) {
            this.exitMIDlet();
        } else if (command==cmd_crear_carpeta) {
            retorno=configuracion.crear_carpeta_datos(seleccion_raiz.getSelectedIndex());
            if (retorno==0) { //carpeta creada con éxito. intenta inicializar
                retorno=configuracion.inicializar();
                if (retorno==configuracion.Estado_Archivo_Configuracion_No_Encontrado) { //carpeta creada. hay que crear el archivo de configuracion
                    crear_configuracion_defecto_y_arrancar();
                } else { //ha habido un error extraño, apaga
                    mensaje_error("Failed to create default config.");
                }
            } else {
                mensaje_error("Failed to save default config");
            }
        }
    } */
private void crear_configuracion_defecto_y_arrancar() {
    //intenta crear un archivo de configuración por defecto. si lo consigue, arranca. y si no, se sale.
    int retorno;
    configuracion.cargar_configuracion_defecto();
    retorno=configuracion.guardar_configuracion();
    if (retorno!=0) {
        mensaje_error("Failed to save default config.");
        notifyDestroyed();
        return;
    }
    arrancar_img_canvas();
    
}
public void mensaje_error(String texto) {
    //muestra el mensaje de error indicado
    Alert mensaje;
    mensaje=new Alert("Error",texto,null,AlertType.ERROR);
    mensaje.setTimeout(Alert.FOREVER);
    pantalla.setCurrent(mensaje);
}
public void mensaje_advertencia(String texto) {
    //muestra un mensaje informativo
    Alert mensaje;
    mensaje=new Alert("Warning",texto,null,AlertType.WARNING);
    mensaje.setTimeout(Alert.FOREVER);
    pantalla.setCurrent(mensaje);
    
}
}
