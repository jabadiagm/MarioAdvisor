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
public class Visor_IMG extends MIDlet implements CommandListener{
    public Display pantalla;
    private IMG_Canvas img_Canvas;
    private Configuracion configuracion;
    private Config_Canvas config_canvas;
    private BT_Canvas bt_canvas;
    //variables para el formulario de creación de carpeta
    public Form formulario_carpeta; //selección del directorio de trabajo
    private ChoiceGroup seleccion_raiz;
    private Command cmd_crear_carpeta;
    private Command cmd_salir;
    
    
    
    /** Creates a new instance of Visor_IMG */
    public Visor_IMG() {
        pantalla=getDisplay();
        configuracion=new Configuracion();
        formulario_carpeta=new Form("Select Program Folder");
        seleccion_raiz=new ChoiceGroup("Roots",ChoiceGroup.EXCLUSIVE);
        formulario_carpeta.append(seleccion_raiz);
        formulario_carpeta.setCommandListener(this);
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
        } else { //fallo al cargar la configuración.muestra el formulario
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
        }
    }
    private void arrancar_img_canvas() {
        //activa la tarea principal
        img_Canvas=new IMG_Canvas(this,configuracion);
        pantalla.setCurrent(img_Canvas);
        img_Canvas.inicializar();
    }
    public void mostrar_configuracion() {
        //muestra la pantalla de configuración
        if (config_canvas==null) config_canvas=new Config_Canvas(configuracion,this);
        pantalla.setCurrent(config_canvas.formulario_configuracion);
    }
    public void mostrar_img_canvas() {
        if (config_canvas!=null) config_canvas=null; //si se viene del formulario de configuración se borra
        if (bt_canvas!=null) bt_canvas=null; //si se viene del formulario de selección de GPS, se borra
        //muestra la pantallaprincipal
        if (img_Canvas.pausar==true) img_Canvas.pausar=false;
        pantalla.setCurrent(img_Canvas);
    }
    public void mostrar_BT_canvas() {
        //muestra el formulario de selección de GPS bluetooth
        if (bt_canvas==null) bt_canvas=new BT_Canvas(configuracion,this);
        pantalla.setCurrent(bt_canvas.formulario_BT);
        
    }
    public void pauseApp() {
    }
    
    public void destroyApp(boolean unconditional) {
    }
    
    public void commandAction(Command command, Displayable displayable) {
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
    }
private void crear_configuracion_defecto_y_arrancar() {
    //intenta crear un archivo de configuración por defecto. si lo consigue, arranca. y si no, se sale.
    int retorno;
    retorno=configuracion.cargar_configuracion_defecto();
    if (retorno!=0) {
        mensaje_error("Failed to create default config.");
        notifyDestroyed();
        return;
    }
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
