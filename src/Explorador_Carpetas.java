import java.io.*;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;
/*
 * Explorador_Carpetas.java
 *
 * Created on 14 de junio de 2008, 20:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Explorador_Carpetas  implements CommandListener{
    //se encarga de explorar el sistema de archivos para seleccionar
    //la carpeta de mapas. el código se ha extraído de los ejemplos
    //que vienen con el wtk2 de sony ericsson
    /* special string denotes upper directory */
    private static final String UP_DIRECTORY = "..";
    
    /* special string that denotes upper directory accessible by this browser.
     * this virtual directory contains all roots.
     */
    private static final String MEGA_ROOT = "/";
    
    /* separator string as defined by FC specification */
    private static final String SEP_STR = "/";
    private Command view = new Command("View", Command.ITEM, 1);
    private Command back = new Command("Back", Command.BACK, 2);
    private Command seleccionar = new Command("Select", Command.OK, 1);
    
    /* separator character as defined by FC specification */
    private static final char SEP = '/';
    private String currDirName;
    private Image dirIcon;
    private Image fileIcon;
    private Image[] iconList;
    //acceso a objetos externos
    Visor_IMG midlet;
    //Configuracion configuracion;
    Config_Canvas config_canvas;
    /** Creates a new instance of Explorador_Carpetas */
    public Explorador_Carpetas(Visor_IMG midlet,Config_Canvas config_canvas) {
        this.midlet=midlet;
        //this.configuracion=configuracion;
        this.config_canvas=config_canvas;
        currDirName = MEGA_ROOT;
        try {
            dirIcon = Image.createImage("/res/dir.png");
        } catch (IOException e) {
            dirIcon = null;
        }
        try {
            fileIcon = Image.createImage("/res/file.png");
        } catch (IOException e) {
            fileIcon = null;
        }
        iconList = new Image[] { fileIcon, dirIcon };
    }
    public void explorar() {
        try {
            showCurrDir();
        } catch (SecurityException e) {
            Alert alert =
                    new Alert("Error", "You are not authorized to access the restricted API", null,
                    AlertType.ERROR);
            alert.setTimeout(Alert.FOREVER);
            
            Form form = new Form("Cannot access FileConnection");
            form.append(new StringItem(null,
                    "You cannot run this MIDlet with the current permissions. " +
                    "Sign the MIDlet suite, or run it in a different security domain"));
            form.addCommand(back);
            form.setCommandListener(this);
            midlet.pantalla.setCurrent(alert, form);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void showCurrDir() {
        Enumeration e;
        FileConnection currDir = null;
        List browser;
        
        try {
            if (MEGA_ROOT.equals(currDirName)) {
                e = FileSystemRegistry.listRoots();
                browser = new List(currDirName, List.IMPLICIT);
            } else {
                currDir = (FileConnection)Connector.open("file://localhost/" + currDirName,Connector.READ);
                e = currDir.list();
                browser = new List(currDirName, List.IMPLICIT);
                // not root - draw UP_DIRECTORY
                browser.append(UP_DIRECTORY, dirIcon);
            }
            
            while (e.hasMoreElements()) {
                String fileName = (String)e.nextElement();
                
                if (fileName.charAt(fileName.length() - 1) == SEP) {
                    // This is directory
                    browser.append(fileName, dirIcon);
                } else {
                    // this is regular file
                    //browser.append(fileName, fileIcon);
                }
            }
            
            browser.setSelectCommand(view);
            
            //Do not allow creating files/directories beside root
            if (!MEGA_ROOT.equals(currDirName)) {
                browser.addCommand(seleccionar);
            }
            browser.addCommand(back);
            browser.setCommandListener(this);
            
            if (currDir != null) {
                currDir.close();
            }
            
            midlet.pantalla.setCurrent(browser);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    public void commandAction(Command c, Displayable d) {
        if (c == view) {
            List curr = (List)d;
            final String currFile = curr.getString(curr.getSelectedIndex());
            new Thread(new Runnable() {
                public void run() {
                    if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY)) {
                        traverseDirectory(currFile);
                    } else {
                        // Show file contents
                        //showFile(currFile);
                    }
                }
            }).start();
        } else if (c==seleccionar) {
            //actualiza la configuración y vuelve al formulario
            //configuracion.ruta_carpeta_archivos="file://localhost/"+currDirName;
            config_canvas.definir_nueva_ruta_archivos("file://localhost/"+currDirName);
            midlet.mostrar_configuracion();
            
        } else if (c==back) {
            //vuelve al formulario de configuración, sin haber hecho cambios
            midlet.mostrar_configuracion();
        }
    }
    void traverseDirectory(String fileName) {
        /* In case of directory just change the current directory
         * and show it
         */
        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {
                // can not go up from MEGA_ROOT
                return;
            }
            
            currDirName = fileName;
        } else if (fileName.equals(UP_DIRECTORY)) {
            // Go up one directory
            // TODO use setFileConnection when implemented
            int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);
            
            if (i != -1) {
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = MEGA_ROOT;
            }
        } else {
            currDirName = currDirName + fileName;
        }
        
        showCurrDir();
    }
}
