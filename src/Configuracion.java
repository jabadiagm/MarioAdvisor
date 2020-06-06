import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.*;
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
    //variables públicas
    public int estado;
    //constantes de estado
    public final int Estado_OK=0;
    public final int Estado_Error_No_JSR75=1;
    public final int Estado_Pendiente_Leer_Configuracion=2;
    public final int Estado_Archivo_Configuracion_No_Encontrado=3;
    public final int Estado_Error_Acceso_Archivos=4;
    public final int Estado_Error_Carpeta_Archivos_Inexistente=5;
    //variables de acceso a directorios y archivo de configuración
    private String ruta_archivo_configuracion;
    private final String nombre_archivo_configuracion="config.txt";
    private final String nombre_carpeta_archivos="mario"; //nombre del directorio donde se guardan los datos
    public String ruta_carpeta_archivos;
    private FileConnection archivo_configuracion;
    private FileConnection carpeta_archivos;
    private InputStream stream_configuracion_entrada;
    private OutputStream stream_configuracion_salida;
    public String [] raices;//raíces del sistema de archivos del equipo
    //variables de trabajo del programa
    public final boolean depuracion=false; //true para pruebas
    public boolean pantalla_completa=true; //arranque
    //gotland
    //public float centro_longitud_inicial=18.3f;
    //public float centro_latitud_inicial=57.64f;
    //madriz
    //public float centro_longitud_inicial=-3.7f;
    //public float centro_latitud_inicial=40.417f;
    //public float centro_longitud_inicial=0f;
    //public float centro_latitud_inicial=51.5f;
    //pamplona
    public float centro_longitud_inicial;//=-1.64f;
    public float centro_latitud_inicial;//=42.82f;
    //canarias
    //public float centro_longitud_inicial=-16.5f;
    //public float centro_latitud_inicial=28.22f;
    //galicia profunda
    //public float centro_longitud_inicial=-8.8f;
    //public float centro_latitud_inicial=43f;    
    public int nivel_zoom_inicial;//=11;
    public int factor_mapa;//=3;
    public int detalle_minimo_mapa_general;//=3; //nivel a partir del cual sólo dibuja mapas genéricos. si es <0, desactiva el uso de mapas generales
    public String GPS_url;//="btspp://00027815ECBB:1;authenticate=false;encrypt=false;master=true"; //ruta de conexión del GPS
    
    /** Creates a new instance of Configuracion */
    public Configuracion() {
        String cadena;
        //averigua si está disponible el acceso a archivos (JSR75)
        cadena=System.getProperty("microedition.io.file.FileConnection.version");
        if (cadena==null) {
            estado=this.Estado_Error_No_JSR75; //el programa no puede continuar, no hay acceso a archivos
            return;
        }
        estado=this.Estado_Pendiente_Leer_Configuracion; //listo para cargar archivo de configuración
    }
    public int inicializar() {
        //obtiene la lista de raíces del sistema de archivos y busca el archivo de configuración
        Enumeration lista_raices;
        String raiz;
        Vector vector_raices;
        int retorno;
        int contador;
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
        for (contador=0;contador<raices.length;contador++) {
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
            } catch (IOException ex) {
                ex.printStackTrace();
                estado=this.Estado_Error_Carpeta_Archivos_Inexistente;
                return estado;
            }
            
        }
        estado= this.Estado_Archivo_Configuracion_No_Encontrado;
        return estado;
    }
public int crear_carpeta_datos (int indice_raiz) {
    //crea un directorio de datos en la raíz del sistema de archivos indicada por el índice dado
    FileConnection ruta_configuracion;
    if (raices==null) return 1; // el objeto está sin inicializar o hay algún problema obteniendo las raíces
    try {
        ruta_configuracion=(FileConnection)Connector.open(raices[indice_raiz]+this.nombre_carpeta_archivos);
        ruta_configuracion.mkdir();
        ruta_configuracion.close();
        return 0; //carpeta creada con éxito
    } catch (IOException ex) {
        ex.printStackTrace();
        return 2; //error el la creación de carpeta
    } 
}
public int cargar_configuracion_defecto() {
    //define unos valores iniciales por si no existe un archivo de configuración
    if (this.ruta_carpeta_archivos==null) return 1; //no existe la carpeta de archivos
    this.centro_longitud_inicial=0;
    this.centro_latitud_inicial=0;
    this.nivel_zoom_inicial=10;
    this.detalle_minimo_mapa_general=3;
    this.factor_mapa=3;
    this.pantalla_completa=false;
    this.GPS_url="";
    //define el nombre del archivo para poder guardarlo
    this.ruta_archivo_configuracion=this.ruta_carpeta_archivos+this.nombre_archivo_configuracion;


    return 0;
}
private int cargar_configuracion() {
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
private int procesar_configuracion(String [] nombres_parametros,String [] valores_parametros) {
    //carga los valores de los parámetros
    int contador;
    try {
        if (nombres_parametros[0].compareTo("VERSION1")==0) {
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
                }
            }
        }
        return 0;
    } catch (Exception ex){
        return 1; //error en la conversión
    }
    
}
private int separar_parametros_configuracion(Vector contenido,String [] nombres_parametros,String [] valores_parametros) {
    //toma el contenido del archivo de configuración y lo separa en dos partes, tomando el "=" como carácter de separación
    String linea;
    String nombre,valor; //contenido separado de la línea
    int contador;
    int posicion_igual; //número de caracter de la cadena en el que aparece el signo "="
    for(contador=0;contador<contenido.size();contador++) {
        linea=(String)contenido.elementAt(contador);
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
private String leer_linea() {
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

        
        stream_configuracion_salida.close();
        archivo_configuracion.close();
        estado= this.Estado_OK;
        return estado;
    } catch (IOException ex) {
        ex.printStackTrace();
        estado=this.Estado_Error_Acceso_Archivos;
        return estado;
    }
}
private void escribir_linea(String linea) throws IOException {
    linea=linea+(char)13+(char)10; //añade fin de línea
    stream_configuracion_salida.write(linea.getBytes());
}
}
