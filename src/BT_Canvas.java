import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
/*
 * BT_Canvas.java
 *
 * Created on 8 de mayo de 2008, 17:37
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class BT_Canvas implements CommandListener,Runnable{
//coloca en pantalla una lista de GPS's blouetooth y permite elegir entre ellos para añadirlo a la configuración
    /** Creates a new instance of BT_Canvas */
    public Form formulario_BT; //selección del dispositivo bluetooth
    private ChoiceGroup seleccion_GPS;
    private BTConnector btconnector; //localizador de dispositivos bluetooth
    private Command cmd_volver;
    private Command cmd_seleccionar;
    private Visor_IMG midlet;
    private Configuracion configuracion;
    private String ultimo_error;
    public BT_Canvas(Configuracion config,Visor_IMG midlet) {
        this.midlet=midlet;
        this.configuracion=config;
        formulario_BT=new Form("BT GPS Selection");
        seleccion_GPS=new ChoiceGroup("Searching...",ChoiceGroup.EXCLUSIVE);
        cmd_volver=new Command("Back",Command.EXIT,0);
        cmd_seleccionar=new Command("Select",Command.OK,0);
        formulario_BT.append(seleccion_GPS);
        formulario_BT.addCommand(cmd_volver);
        formulario_BT.addCommand(cmd_seleccionar);
        formulario_BT.setCommandListener(this);
        btconnector=new BTConnector(); //arranca el buscador de dispositivos
        Thread t = new Thread(this);
        //t.setPriority(Thread.MAX_PRIORITY);
        t.start(); //ejecuta la tarea que revisa los resultados de búsqueda
        
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command==cmd_volver) {
            midlet.mostrar_img_canvas();
        } else if (command==cmd_seleccionar && seleccion_GPS.size()>=1) {
            btconnector.connect(seleccion_GPS.getSelectedIndex());
            synchronized(this){ // resume the thread.
                this.notify();
            }
        }
    }

    public void run() {
        String [] dispositivos=null; //lista de dispositivos encontrador
        int contador;
        int encontrados; //número de dispositivos encontrador
        String error_bt;
        dispositivos=btconnector.getDeviceNames();
        //comprueba si aparecen nuevos dispositivos mientras dura la búsqueda
        while (btconnector.doneSearchingDevices()==false && btconnector.get_last_error()==null) {
            try{ //sale de la tarea para que el resto funcione
                Thread.sleep(100);
            }catch(Exception e){}
        }
        error_bt=btconnector.get_last_error();
        if (error_bt!=null) midlet.mensaje_error(error_bt);
        //ha terminado de buscar, se cambia la layenda de la lista
        seleccion_GPS.setLabel("Search Complete");
        for (contador=0;contador<dispositivos.length;contador++) {
            if (dispositivos[contador]!=null) {
                seleccion_GPS.append(dispositivos[contador],null);
            }
        }
        // Pause the thread until the user selects another bt device to connect to.
        synchronized(this){
            try{
                wait();
            }catch(Exception e){
            }
        }
        while (btconnector.doneSearchingServices()==false && ultimo_error==null) {
            ultimo_error=btconnector.get_last_error();
            try{ //sale de la tarea para que el resto funcione
                Thread.sleep(100);
            }catch(Exception e){}
            
        }
        if (ultimo_error!=null) {//no se ha podido obtener la dirección del dispositivo
            configuracion.GPS_url="";
            midlet.mensaje_error(ultimo_error);
        } else {
            configuracion.GPS_url=btconnector.url;
        }
        
        midlet.mostrar_img_canvas();
    }
    
}
