import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
/*
 * Configuracion.java
 *
 * Created on 12 de marzo de 2008, 20:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Configuracion {
    //la configuración pasa a estar guardada dentro de los RMS, para evitar
    //el acceso a archivos externos. se han cambiado las funciones de
    //carga y guardado, y el acceso a ficheros queda como opción.
    //variables públicas
    public int estado;
    //constantes de estado
    public static final int Estado_OK=0;
    public static final int Estado_Pendiente_Leer_Configuracion=1;
    public static final int Estado_Configuracion_No_Encontrada=2;
    public static final int Estado_Error_Acceso_Archivos=3;
    public static final int Estado_Error_Carpeta_Archivos_Inexistente=4;
    public static final int Estado_Error_Acceso_Archivos_Denegado=5;
    public static final int Estado_Error_Acceso_RMS=6;
    //variables de acceso a directorios y archivo de configuración
    public String ruta_archivo_configuracion;
    public final String nombre_archivo_configuracion="config.txt";
    private final String nombre_carpeta_archivos="mario"; //nombre del directorio donde se guardan los datos
    private FileConnection archivo_configuracion;
    private FileConnection carpeta_archivos;
    private InputStream stream_configuracion_entrada;
    private OutputStream stream_configuracion_salida;
    public String [] raices;//raíces del sistema de archivos del equipo
    //API's disponibles
    public boolean JSR75_disponible=false; //acceso a archivos
    public boolean JSR82_disponible=false; //bluetooth
    public boolean JSR179_disponible=false; //GPS interno
    //variables de configuración propiamente dichas
    public boolean pantalla_completa=false; //arranque
    public float centro_longitud_inicial;//=-1.64f;
    public float centro_latitud_inicial;//=42.82f;
    public int nivel_zoom_inicial;//=11;
    public int factor_mapa;//=3;
    public int detalle_minimo_mapa_general;//=3; //nivel a partir del cual sólo dibuja mapas genéricos. si es <0, desactiva el uso de mapas generales
    public String ruta_carpeta_archivos="";
    public String GPS_url;//="btspp://00027815ECBB:1;authenticate=false;encrypt=false;master=true"; //ruta de conexión del GPS
    public int tamaño_cache_mapas=1;
    public boolean acceso_archivos_habilitado=false; //pasa a true si la configuración guardad en RMS's lo permite
    public boolean cache_etiquetas=true;
    public final boolean depuracion=false; //true para pruebas
    
    /** Creates a new instance of Configuracion */
    public Configuracion() {
        String cadena;
        //averigua si está disponible el acceso a archivos (JSR75)
        cadena=System.getProperty("microedition.io.file.FileConnection.version");
        if (cadena!=null) {
            JSR75_disponible=true;
        }
        //averigua si está disponible el bluetooth
        cadena=System.getProperty("bluetooth.api.version");
        if (cadena!=null) {
            JSR82_disponible=true;
        } else { 
            try {
                if (javax.bluetooth.LocalDevice.getProperty("bluetooth.api.version")!=null) JSR82_disponible=true;
            } catch (Exception ex) {
                
            }
        }
        //averigua si está disponible el GPS interno
        cadena=System.getProperty("microedition.location.version");
        if (cadena!=null) {
            JSR179_disponible=true;
        }
        
        
        
        estado=this.Estado_Pendiente_Leer_Configuracion; //listo para cargar archivo de configuración
    }
    public int inicializar() {
        //accede al sistema de registros para cargar la configuración
        int retorno;
        retorno=cargar_configuracion();
        if (retorno==0) {
            estado=Estado_OK; //congirucación cargada con éxito
            return estado;
        } else if (retorno==1) {
            estado=Estado_Configuracion_No_Encontrada;
            return estado;
        } else {
            estado=Estado_Error_Acceso_RMS;
            return estado;
        }
    }
    private int cargar_configuracion() {
        //accede al sistema de registros e intenta cargar la configuración
        RecordStore registros;
        int contador;
        int longitud;
        int retorno;
        byte [] registro=new byte [100];
        String [] configuracion; //archivo de configuración en líneas
        String [] nombres_parametros;
        String [] valores_parametros;
        try {
            //abre el registro de configuración
            registros=RecordStore.openRecordStore("Configuracion_Mario_Advisor",true);
            //registros.addRecord("666".getBytes(),0,3);
            
            if (registros.getNumRecords()==0) {
                registros.closeRecordStore();
                return 1; //no existe configuración
            }
            //lee el contenido
            configuracion=new String [registros.getNumRecords()];
            for (contador=registros.getNumRecords();contador>0;contador--) {
                longitud=registros.getRecordSize(contador);
                registro=registros.getRecord(contador);
                configuracion[contador-1]=new String(registro,0,longitud);
            }
            registros.closeRecordStore();
            nombres_parametros=new String [configuracion.length];
            valores_parametros=new String [configuracion.length];
            retorno=separar_parametros_configuracion(configuracion,nombres_parametros,valores_parametros);
            if (retorno!=0) {
                return 2; //error en el proceso de archivo
            }
            retorno=procesar_configuracion(nombres_parametros,valores_parametros);
            if (retorno!=0) {
                return 3; //error en el proceso del contenido
            }
            return 0; //configuración leída con éxito
            
        } catch (RecordStoreException ex) {
            return 1; //error accediendo al sistema de registros
        }
    }
    public void  cargar_configuracion_defecto() {
        //define unos valores iniciales por si no existe un archivo de configuración
        this.centro_longitud_inicial=0;
        this.centro_latitud_inicial=0;
        this.nivel_zoom_inicial=10;
        this.detalle_minimo_mapa_general=3;
        this.factor_mapa=2;
        this.pantalla_completa=false;
        if (this.JSR179_disponible==true) {
            this.GPS_url="jsr179";
        } else {
            this.GPS_url="";
        }
        
        this.tamaño_cache_mapas=1;
        
        
        
        
    }
    
    private int procesar_configuracion(String [] nombres_parametros,String [] valores_parametros) {
        //carga los valores de los parámetros
        int contador;
        try {
            if (nombres_parametros[0].compareTo("VERSION2")==0) {
                for (contador=1;contador<nombres_parametros.length;contador++) {
                    if (nombres_parametros[contador].compareTo("FULL SCREEN")==0) {
                        if (valores_parametros[contador].compareTo("true")==0) {
                            this.pantalla_completa=true;
                        } else if (valores_parametros[contador].compareTo("false")==0) {
                            this.pantalla_completa=false;
                        } else return 1; //error en el proceso
                    } else if (nombres_parametros[contador].compareTo("LONGITUDE")==0) {
                        this.centro_longitud_inicial=Float.valueOf(valores_parametros[contador]).floatValue();
                    } else if (nombres_parametros[contador].compareTo("LATITUDE")==0) {
                        this.centro_latitud_inicial=Float.valueOf(valores_parametros[contador]).floatValue();
                    } else if (nombres_parametros[contador].compareTo("ZOOM LEVEL")==0) {
                        this.nivel_zoom_inicial=Integer.valueOf(valores_parametros[contador]).intValue();
                    } else if (nombres_parametros[contador].compareTo("GPS URL")==0) {
                        this.GPS_url=valores_parametros[contador];
                    } else if (nombres_parametros[contador].compareTo("MAP SIZE FACTOR")==0) {
                        this.factor_mapa=Integer.valueOf(valores_parametros[contador]).intValue();
                    } else if (nombres_parametros[contador].compareTo("OVERVIEW MAP DETAIL")==0) {
                        this.detalle_minimo_mapa_general=Integer.valueOf(valores_parametros[contador]).intValue();
                    }  else if (nombres_parametros[contador].compareTo("MAPS CACHE SIZE")==0) {
                        this.tamaño_cache_mapas=Integer.valueOf(valores_parametros[contador]).intValue();
                    } else if (nombres_parametros[contador].compareTo("ENABLE EXTERNAL FILESYSTEM ACCESS")==0) {
                        if (valores_parametros[contador].compareTo("true")==0) {
                            //el acceso a archivos depende de que el dispositivo tenga esa posibilidad
                            if (this.JSR75_disponible==true) this.acceso_archivos_habilitado=true;
                        } else if (valores_parametros[contador].compareTo("false")==0) {
                            this.acceso_archivos_habilitado=false;
                        } else return 1; //error en el proceso
                    } else if (nombres_parametros[contador].compareTo("EXTERNAL DATA FOLDER")==0) {
                        this.ruta_carpeta_archivos=valores_parametros[contador];
                    }
                }
            }
            return 0;
        } catch (Exception ex){
            return 1; //error en la conversión
        }
        
    }
    private int separar_parametros_configuracion(String [] contenido,String [] nombres_parametros,String [] valores_parametros) {
        //toma el contenido del archivo de configuración y lo separa en dos partes, tomando el "=" como carácter de separación
        String linea;
        String nombre,valor; //contenido separado de la línea
        int contador;
        int posicion_igual; //número de caracter de la cadena en el que aparece el signo "="
        for(contador=0;contador<contenido.length;contador++) {
            linea=contenido[contador];
            posicion_igual=linea.indexOf("=");
            if (posicion_igual==-1) { //no hay signo "=". puede ser el principio del archivo
                if (contador==0) { //es la primera línea
                    nombres_parametros[0]=linea.trim();
                } else {
                    return 1; //error en el contenido del archivo
                }
            } else if (posicion_igual==0 || posicion_igual==linea.length()) { //si el signo está al principio o al final de la línea, no vale
                return 1; //error en el contenido del archivo
            } else { //línea válida, en principio
                nombre=linea.substring(0,posicion_igual);
                valor=linea.substring(posicion_igual+1);
                nombre=nombre.trim();
                valor=valor.trim();
                if (nombre=="" || valor=="") { //si después de quitar los espacios se quedan en blanco, devuelve un error
                    return 1; //error en el contenido del archivo
                }
                nombres_parametros[contador]=nombre;
                valores_parametros[contador]=valor;
            }
        }
        return 0;
    }
    public int guardar_configuracion() {
        //accede al sistema de registros e intenta guardar la configuración
        RecordStore registros;
        int contador;
        int longitud;
        String cadena;
        byte [] registro=new byte [100];
        String [] configuracion; //archivo de configuración en líneas
        try {
            RecordStore.deleteRecordStore("Configuracion_Mario_Advisor");
        } catch (RecordStoreException ex) { //puede que no existe
            
        }
        try {
            //crea el registro de configuración
            registros=RecordStore.openRecordStore("Configuracion_Mario_Advisor",true);
            //escribe la configuración
            cadena="VERSION2";
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="FULL SCREEN=";
            if (this.pantalla_completa==true) {
                cadena+="true";
            } else {
                cadena+="false";
            }
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="LONGITUDE="+new Float(this.centro_longitud_inicial).toString();
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="LATITUDE="+new Float(this.centro_latitud_inicial).toString();
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="ZOOM LEVEL="+new Integer(this.nivel_zoom_inicial).toString();
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="ENABLE EXTERNAL FILESYSTEM ACCESS=";
            if (this.acceso_archivos_habilitado==true) {
                cadena+="true";
            } else {
                cadena+="false";
            }
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="EXTERNAL DATA FOLDER="+this.ruta_carpeta_archivos;
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="GPS URL="+this.GPS_url;
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="MAP SIZE FACTOR="+new Integer(this.factor_mapa).toString();
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="OVERVIEW MAP DETAIL="+new Integer(this.detalle_minimo_mapa_general).toString();
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            cadena="MAPS CACHE SIZE="+new Integer(this.tamaño_cache_mapas).toString();
            registros.addRecord(cadena.getBytes(),0,cadena.length());
            registros.closeRecordStore();
            this.estado=this.Estado_OK; //listo para funcionar
            return 0; //configuración leída con éxito
            
        } catch (RecordStoreException ex) {
            return 2; //error accediendo al sistema de registros
        }
        
    }
/*    private String leer_linea() {
        //lee caracteres hasta llegar a 0d 0a y devuelve los caracteres obtenidos
        int retorno;
        byte [] dato_byte=new byte [1];
        String cadena="";
        try {
            while (true) {
                retorno=stream_configuracion_entrada.read(dato_byte);
                if (retorno==-1) return null; //fin de archivo
                if (dato_byte[0]==0x0d) break; //fin de línea
                cadena+=(char)dato_byte[0];
            }
            //comprueba que el siguiente carácter es 0x0a
            stream_configuracion_entrada.read(dato_byte);
            if (dato_byte[0]!=0x0a) return null; //archivo incorrecto
            return cadena;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null; //error de acceso a archivos
        }
    }
    public int guardar_configuracion() {
        String cadena;
        if (estado!=this.Estado_OK && estado!=this.Estado_Archivo_Configuracion_No_Encontrado) return estado;
        try {
            archivo_configuracion=(FileConnection)Connector.open(ruta_archivo_configuracion);
            if (archivo_configuracion.exists()==true) {
                archivo_configuracion.delete(); //borra el archivo para que no se mezcle con los nuevos valores
                archivo_configuracion.close(); //cierra la conexión para volver a abrirla
                archivo_configuracion=(FileConnection)Connector.open(ruta_archivo_configuracion);
            }
            archivo_configuracion.create();
            stream_configuracion_salida=archivo_configuracion.openOutputStream();
            cadena="VERSION1";
            escribir_linea(cadena);
            cadena="FULL SCREEN=";
            if (this.pantalla_completa==true) {
                cadena+="true";
            } else {
                cadena+="false";
            }
            escribir_linea(cadena);
            cadena="LONGITUDE="+new Float(this.centro_longitud_inicial).toString();
            escribir_linea(cadena);
            cadena="LATITUDE="+new Float(this.centro_latitud_inicial).toString();
            escribir_linea(cadena);
            cadena="ZOOM LEVEL="+new Integer(this.nivel_zoom_inicial).toString();
            escribir_linea(cadena);
            cadena="GPS URL="+this.GPS_url;
            escribir_linea(cadena);
            cadena="MAP SIZE FACTOR="+new Integer(this.factor_mapa).toString();
            escribir_linea(cadena);
            cadena="OVERVIEW MAP DETAIL="+new Integer(this.detalle_minimo_mapa_general).toString();
            escribir_linea(cadena);
            cadena="MAPS CACHE SIZE="+new Integer(this.tamaño_cache_mapas).toString();
            escribir_linea(cadena);
 
 
            stream_configuracion_salida.close();
            archivo_configuracion.close();
            estado= this.Estado_OK;
            return estado;
        } catch (Exception ex) {
            ex.printStackTrace();
            estado=this.Estado_Error_Acceso_Archivos;
            return estado;
        }
    }
    private void escribir_linea(String linea) throws IOException {
        linea=linea+(char)13+(char)10; //añade fin de línea
        stream_configuracion_salida.write(linea.getBytes());
    }
    private int cargar_configuracion_antiguo() {
        //al llegar aquí el archivo ya debe estar abierto
        String linea;
        Vector contenido=new Vector();
        String [] nombres_parametros;
        String [] valores_parametros;
        int retorno;
        try {
            stream_configuracion_entrada=archivo_configuracion.openInputStream();
            while (true) {
                linea=leer_linea();
                if (linea==null) break;
                contenido.addElement(linea);
            }
            stream_configuracion_entrada.close();
            archivo_configuracion.close();
            nombres_parametros=new String [contenido.size()];
            valores_parametros=new String [contenido.size()];
            //separa el contenido del archivo en nombres de parámetros y sus valores.
            retorno=separar_parametros_configuracion(contenido,nombres_parametros,valores_parametros);
            if (retorno!=0) {
                return 2; //error en el proceso de archivo
            }
            retorno=procesar_configuracion(nombres_parametros,valores_parametros);
            if (retorno!=0) {
                return 3; //error en el proceso del contenido
            }
            return 0;
        } catch (IOException ex) {
            ex.printStackTrace();
            return 1; //error en el acceso a archivo
        }
 
    }
     public int inicializar_viejo() {
        //obtiene la lista de raíces del sistema de archivos y busca el archivo de configuración
        Enumeration lista_raices;
        String raiz;
        Vector vector_raices;
        int retorno;
        int contador;
        String texto_excepcion;
        try {
            lista_raices=FileSystemRegistry.listRoots();
            vector_raices=new Vector();
            while (lista_raices.hasMoreElements()) {
                raiz ="file:///"+ (String) lista_raices.nextElement();
                vector_raices.addElement(raiz);
            }
            raices=new String[vector_raices.size()];
            for (contador=0;contador<vector_raices.size();contador++) {
                raices[contador]=(String)vector_raices.elementAt(contador);
                if (raices[contador].endsWith("/")==false) { //añade "/" si no está"
                    raices[contador]+="/";
                }
            }
            for (contador=raices.length-1;contador>=0;contador--) {
                raiz = raices[contador];
                try {
                    carpeta_archivos=(FileConnection)Connector.open(raiz+nombre_carpeta_archivos+"/",Connector.READ);
                    if (carpeta_archivos.exists()==true) { //carpeta encontrada. ahora busca el archivo de configuración
                        ruta_carpeta_archivos=raiz+nombre_carpeta_archivos+"/";
                        carpeta_archivos.close();
                        archivo_configuracion=(FileConnection)Connector.open(raiz+nombre_carpeta_archivos+"/"+nombre_archivo_configuracion,Connector.READ);
                        if (archivo_configuracion.exists()==true) {
                            ruta_archivo_configuracion=ruta_carpeta_archivos+nombre_archivo_configuracion;
                            retorno=cargar_configuracion();
                            if (retorno!=0) {
                                estado=this.Estado_Error_Acceso_Archivos;
                            } else {
                                estado=this.Estado_OK;
                            }
                            return estado;
                        }
                    }
                } catch (Exception ex) {
                    texto_excepcion=ex.toString();
                    //ex.printStackTrace();
                    //estado=this.Estado_Error_Carpeta_Archivos_Inexistente;
                    //return estado;
                }
 
            }
            estado= this.Estado_Archivo_Configuracion_No_Encontrado;
 
        } catch (Exception ex){ //permiso denegado para el acceso a archivos
            this.estado=Estado_Error_Acceso_Archivos_Denegado;
            acceso_archivos_habilitado=false; //inhabilita las siguientes operaciones de acceso a archivos
        }
        return estado;
    } 
    public int crear_carpeta_datos(int indice_raiz) {
        //crea un directorio de datos en la raíz del sistema de archivos indicada por el índice dado
        FileConnection ruta_configuracion;
        if (raices==null) return 1; // el objeto está sin inicializar o hay algún problema obteniendo las raíces
        try {
            ruta_configuracion=(FileConnection)Connector.open(raices[indice_raiz]+this.nombre_carpeta_archivos+"/");
            ruta_configuracion.mkdir();
            ruta_configuracion.close();
            return 0; //carpeta creada con éxito
        } catch (Exception ex) {
            ex.printStackTrace();
            return 2; //error el la creación de carpeta
        }
    } */
    
    
}
