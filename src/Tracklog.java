import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.*;
/*
 * Tracklog.java
 *
 * Created on 20 de septiembre de 2008, 11:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tracklog {
    //objeto que recibe las posiciones indicadas por el GPS y las almacena en memoria/disco
    private Configuracion configuracion;
    private int puntero=0; //posición del siguiente espacio libre
    private int tamaño=0; //número de posiciones almacenadas. máximo=configuración.tamaño_tracklog
    private int puntero_segmento=0; //se cambia de segmento al perder cobertura GPS ó al cerrar y volver a entrar
    //variables de almacenamiento de las posiciones
    private long [] tiempo; //tiempo en milisegundos desde medianoche del 1 de enero de 1970
    private int [] segmento; //la pérdida de cobertura ó el amagado da lugar a un nuevo tramo de medidas
    private float [] longitud;
    private float [] latitud;
    private int [] velocidad; //en m/s
    private int [] rumbo; //grados, con el 0 en el norte
    private int [] altura; // en m, sobre el nivel del mar
    private int [] cellid; //identificador del repetidor
    private int [] lac; //identificador de área según el repetidor
    //variables para manejo de archivos
    private OutputStream stream_tracklog_salida;
    
    /** Creates a new instance of Tracklog */
    public Tracklog(Configuracion configuracion) {
        this.configuracion=configuracion;
        if (configuracion.tracklog_activado==true) {
            //hay que reservar espacio para los datos
            tiempo=new long [configuracion.tamaño_tracklog];
            segmento=new int [configuracion.tamaño_tracklog];
            longitud=new float [configuracion.tamaño_tracklog];
            latitud=new float [configuracion.tamaño_tracklog];
            velocidad=new int [configuracion.tamaño_tracklog];
            rumbo=new int [configuracion.tamaño_tracklog];
            altura=new int [configuracion.tamaño_tracklog];
            //opcionalmente se puede guardar la información de los repetidores
            if (configuracion.recoger_cellid==true) {
                cellid=new int [configuracion.tamaño_tracklog];
                lac=new int [configuracion.tamaño_tracklog];
            }
        }
    }
    public void notificar_nueva_posicion(Tipo_Posicion posicion) {
        //recibe la información de posición y la almacena
        tiempo[puntero]=posicion.tiempo;
        segmento[puntero]=puntero_segmento;
        longitud[puntero]=posicion.longitud;
        latitud[puntero]=posicion.latitud;
        velocidad[puntero]=posicion.velocidad;
        rumbo[puntero]=posicion.rumbo;
        altura[puntero]=posicion.altura;
        if (configuracion.recoger_cellid==true) {
            cellid[puntero]=posicion.cellid;
            lac[puntero]=posicion.lac;
        }
        //incremento de los punteros. si se llega a la capacidad máxima, se van borrando
        //los datos más antiguos
        puntero++;
        if (puntero==configuracion.tamaño_tracklog) puntero=0;
        tamaño++;
        if (tamaño> configuracion.tamaño_tracklog) tamaño=configuracion.tamaño_tracklog;
        
    }
    public boolean guardar_tracklog() {
        //guarda un archivo con los datos almacenados
        FileConnection archivo_tracklog;
        String ruta_tracklog;
        ruta_tracklog=configuracion.ruta_carpeta_archivos;
        if (ruta_tracklog.endsWith("/")==false) ruta_tracklog+="/";
        ruta_tracklog+="tracklog.plt";
        try {
            archivo_tracklog=(FileConnection)Connector.open(ruta_tracklog);
            if (archivo_tracklog.exists()==true) {
                archivo_tracklog.delete(); //borra el archivo para que no se mezcle con los nuevos valores
                archivo_tracklog.close(); //cierra la conexión para volver a abrirla
                archivo_tracklog=(FileConnection)Connector.open(ruta_tracklog);
            }
            archivo_tracklog.create();
            stream_tracklog_salida=archivo_tracklog.openOutputStream();
            escribir_tracklog();
            stream_tracklog_salida.close();
            archivo_tracklog.close();
            return true;
        } catch (Exception ex){
            ex.printStackTrace();
            return false; //error en el guardado
        }
    }
    private void escribir_tracklog() throws IOException {
        escribir_cabecera();
        int contador;
        if (this.tamaño<configuracion.tamaño_tracklog) { //tracklog empieza en cero
            for (contador=0;contador<this.tamaño;contador++) {
                escribir_registro(contador);
            }
        } else { //el tracklog está lleno, y puede empezar en cualquier posición
            for (contador=puntero;contador<configuracion.tamaño_tracklog;contador++) {
                escribir_registro(contador);
            }
            for (contador=0;contador<puntero;contador++) {
                escribir_registro(contador);
            }
        }
    }
    private void escribir_cabecera() throws IOException {
        //escribe las primeras líneas de un .PLT
        escribir_linea("OziExplorer Track Point File Version 2.1");
        escribir_linea("WGS 84");
        escribir_linea("Altitude is in Feet");
        escribir_linea("Reserved 3");
        escribir_linea("0,2,16711680,Tracklog,0,0,2,8421376");
        escribir_linea("666");
        
    }
    private void escribir_registro(int registro) throws IOException {
        //escribe la posición del tracklog indicada en formato .PLT para oziexplorer
        //tiempo,segmento,longitud,latitud,velocidad,rumbo,altura,cellid,lac
        //1 foot = 0.3048 meters
        String cadena;
        String fecha;
        String hora;
        String linea;
        double tiempo_delphi; //medida de tiempo en formato delphi, empezando desde 30/12/1899
        cadena=new Date(tiempo[registro]).toString(); //convierte los milisegundos a una fecha
        fecha=cadena.substring(8,10)+"-"+cadena.substring(4,7)+"-"+cadena.substring(cadena.length()-2);
        hora=cadena.substring(11,19);
        //conversión del tiempo java el tiempo delphi
        tiempo_delphi=(double)tiempo[registro]/1000/3600/24+25569;
        /* //sin formato
        cadena=Long.toString(tiempo[registro])+",";
        cadena+=Integer.toString(segmento[registro])+",";
        cadena+=Float.toString(longitud[registro])+",";
        cadena+=Float.toString(latitud[registro])+",";
        cadena+=Integer.toString(velocidad[registro])+",";
        cadena+=Integer.toString(rumbo[registro])+",";
        cadena+=Integer.toString(altura[registro]);
        if (configuracion.recoger_cellid==true) {
            cadena+=","+Integer.toString(cellid[registro])+",";
            cadena+=Integer.toString(lac[registro]);
        } */
        cadena=Float.toString(latitud[registro])+",";
        cadena+=Float.toString(longitud[registro])+",";
        cadena+=Integer.toString(segmento[registro])+",";
        cadena+=Integer.toString((int)(altura[registro]/0.3048))+",";
        cadena+=Double.toString(tiempo_delphi)+",";
        cadena+=fecha+",";
        cadena+=hora;
        //campos extra
        cadena+=","+Integer.toString(velocidad[registro])+",";
        cadena+=Integer.toString(rumbo[registro]);
        if (configuracion.recoger_cellid==true) { //código del repetidor
            cadena+=","+Integer.toString(cellid[registro])+",";
            cadena+=Integer.toString(lac[registro]);
        }        
        escribir_linea(cadena);
      
    }
    private void escribir_linea(String linea) throws IOException {
        linea=linea+(char)13+(char)10; //añade fin de línea
        stream_tracklog_salida.write(linea.getBytes());
    }
    
}
