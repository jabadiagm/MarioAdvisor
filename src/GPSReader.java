/*****************************************************************************
 *
 * Description: This class is used to read the data from a GPS receiver using the NMEA protocol.
 *
 * Created By: Oscar Vivall 2005-06-28
 *
 * @file        GPSReader.java
 *
 * COPYRIGHT All rights reserved Sony Ericsson Mobile Communications AB 2004.
 * The software is the copyrighted work of Sony Ericsson Mobile Communications AB.
 * The use of the software is subject to the terms of the end-user license
 * agreement which accompanies or is included with the software. The software is
 * provided "as is" and Sony Ericsson specifically disclaim any warranty or
 * condition whatsoever regarding merchantability or fitness for a specific
 * purpose, title or non-infringement. No warranty of any kind is made in
 * relation to the condition, suitability, availability, accuracy, reliability,
 * merchantability and/or non-infringement of the software provided herein.
 *
 *****************************************************************************/

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

import javax.microedition.io.*;

import java.io.*;
import java.util.*;

public class GPSReader implements Runnable{
    
    private String url = "";
    
    // This is the data retreived from the GPS
    public static double SPEED_KMH = 0;
    public static String SPEED_KMH_STR = "";
    public static int NUM_SATELITES = 0;
    public static String NUM_SATELITES_STR = "";
    public static float latitud;
    public static float longitud;
    public static int velocidad;
    public static int rumbo;
    public static int altura;
    private String []result = new String[25];
    
    private Vector speedVector = new Vector();
    
    public static String TYPE=""; // Used for debugging.
    private String exc = ""; // Used for debugging.
    
    // Array used to interpolate the speed.
    private String []sp = new String[9];
    private int spIndex = 0;
    public int estado=0;
    private boolean terminar=false; //pasa a true para avisar del cierre del proceso run
    public String ultimo_error; //texto del último error que ha aparecido
    private Gestor_GPS gestor_GPS; //referencia al objeto padre, para comunicación de eventos
    /*
     * The url to the bluetooth device is the String parameter.
     */
    public GPSReader(String u,Gestor_GPS gestor_GPS){
        this.gestor_GPS=gestor_GPS;
        estado=Gestor_GPS.estado_GPS_OFF; //inactivo
        url = u; // Connection string to the bluetooth device.
        Thread t = new Thread(this);
        t.start();
    }
    private float [] extraer_coordenadas(String []data,int posicion) {
        //obtiene LON/LAT a partir de una lína GGA ó RMC, (posición 1 ó 2, respectivamente)
        //resultado[0]=longitud, resultado[1]=latitud
        float resultado[] =new float [2];
        String cadena;
        float temporal;
        cadena=data[posicion].substring(0,2); //grados de latitud
        resultado[1]=Float.parseFloat(cadena);
        cadena=data[posicion].substring(2); //minutos de latitud
        temporal=Float.parseFloat(cadena);
        temporal/=60; //pasa los minutos a grados
        resultado[1]+=temporal;
        if (data[posicion+1].compareTo("S")==0) resultado[1]=-resultado[1]; //la latitud sur es negativa
        cadena=data[posicion+2].substring(0,3); //grados de longitud
        resultado[0]=Float.parseFloat(cadena);
        cadena=data[posicion+2].substring(3); //minutos de longitud
        temporal=Float.parseFloat(cadena);
        temporal/=60; //pasa los minutos a grados
        resultado[0]+=temporal;
        if (data[posicion+3].compareTo("W")==0) resultado[0]=-resultado[0]; //la longitud oeste es negativa
        return resultado;
    }
    public void cerrar() {
        //cierra las conexiones abiertas
        if (estado==Gestor_GPS.estado_GPS_OK_No_Listo || estado==Gestor_GPS.estado_GPS_ON_Listo) {
            terminar=true;
            while (terminar==true); //espera a que se cierre la tarea run
            estado=gestor_GPS.estado_GPS_OFF;
        }
    }
    public void run() {
        
        StreamConnection conn = null;
        InputStream is = null;
        String err = ""; // used for debugging
        String cadena;
        float valor_float;
        float [] resultado=new float[2];
        try{
            String []data;
            conn = (StreamConnection)Connector.open(url);
            is = conn.openInputStream();
            estado=Gestor_GPS.estado_GPS_OK_No_Listo;
            
            int i=0;
            char c;
            String s = "";
            //String []data;
            String DATA_STRING;
            
            // Start reading the data from the GPS
            do{
                i = is.read(); // read one byte at the time.
                c = (char)i;
                s += c;
                if(i==36){ // Every sentence starts with a '$'
                    if(s.length()>5){
                        DATA_STRING = s.substring(5, s.length());
                        TYPE = s.substring(2, 5);
                        
                        // Check the gps data type and convert the information to a more readable format.
                        if(s.substring(0, 5).compareTo("GPGGA") == 0){
                            //data = splitString(",000133.046,,,,,0,00,,,M,0.0,M,,0000*55\r\n$");
                            data = splitString(DATA_STRING);
                            NUM_SATELITES_STR = data[6];
                            try{
                                NUM_SATELITES = Integer.parseInt(data[6]);
                                if (NUM_SATELITES>2) {
                                    estado=Gestor_GPS.estado_GPS_ON_Listo;
                                    resultado=extraer_coordenadas(data,1);
                                    longitud=resultado[0];
                                    latitud=resultado[1];
                                } else {
                                    estado=Gestor_GPS.estado_GPS_OK_No_Listo;
                                }
                                if (data[8].length()>0) { //puede ser una cadena vacía
                                    try {
                                        valor_float=(float)(Float.parseFloat(data[8]));
                                        altura=(int) valor_float;
                                    }catch(Exception e){
                                        ultimo_error=e.toString()+"float"+DATA_STRING;
                                        exc = "<RMC>" + e.getMessage();
                                        estado=Gestor_GPS.estado_GPS_Error;
                                    }
                                } else velocidad=0;                                
                                exc = "";
                                gestor_GPS.notificar_evento_GPS_bluetooth();
                            }catch(Exception e){
                                ultimo_error=e.toString()+"int"+data[6];
                                exc = "<GGA>" + e.getMessage();
                                NUM_SATELITES = 0;
                                estado=Gestor_GPS.estado_GPS_Error;
                                gestor_GPS.notificar_evento_GPS_bluetooth();
                            }
                        }else if(s.substring(0, 5).compareTo("GPRMC") == 0){
                            data = splitString(DATA_STRING);
                            //data = splitString(",123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A\r\n$");
                            cadena=data[1];
                            if (cadena.compareTo("A")==0) { //medida válida
                                estado=Gestor_GPS.estado_GPS_ON_Listo;
                                resultado=extraer_coordenadas(data,2);
                                longitud=resultado[0];
                                latitud=resultado[1];
                                //intenta obtener la velocidad
                                if (data[6].length()>0) { //puede ser una cadena vacía
                                    try {
                                        valor_float=(float)(Float.parseFloat(data[6])*1.852); //velocidad pasada de nudos a km/h
                                        velocidad=(int) valor_float;
                                    }catch(Exception e){
                                        ultimo_error=e.toString()+"float"+DATA_STRING;
                                        exc = "<RMC>" + e.getMessage();
                                        estado=Gestor_GPS.estado_GPS_Error;
                                        gestor_GPS.notificar_evento_GPS_bluetooth();
                                    }
                                } else velocidad=0;
                                //intenta obtener el rumbo
                                if (data[7].length()>0) { //puede ser una cadena vacía
                                    try {
                                        valor_float=Float.parseFloat(data[7]);
                                        rumbo=(int) valor_float;
                                    }catch(Exception e){
                                        ultimo_error=e.toString()+"float"+DATA_STRING;
                                        exc = "<RMC>" + e.getMessage();
                                        estado=Gestor_GPS.estado_GPS_Error;
                                        gestor_GPS.notificar_evento_GPS_bluetooth();
                                    }
                                } else rumbo=0;
                                gestor_GPS.notificar_evento_GPS_bluetooth();
                            } else { //medida no válida
                                estado=Gestor_GPS.estado_GPS_OK_No_Listo;
                                gestor_GPS.notificar_evento_GPS_bluetooth();
                            }
                            
                            
                        }/*else if(s.substring(0, 5).compareTo("GPVTG") == 0){
                            try{
                                //data = splitString(DATA_STRING);
                                data = splitString(",000133.046,,,,,0,00,,,M,0.0,M,,0000*55\r\n$");
                                
                                SPEED_KMH_STR = data[6];
                                
                                speedVector.addElement(data[6]);
                                spIndex = spIndex==8?0:spIndex+1;
                                
                                if(speedVector.size()==10){
                                    speedVector.removeElementAt(0);
                                }
                                double tot =0;
                                String tmp = "";
                                
                                for(int n=0; n<speedVector.size(); n++){
                                    tmp  = (String)speedVector.elementAt(n);
                                    exc = "";
                                    try{
                                        tot += Double.parseDouble(tmp);
                                    }catch(Exception e){
                                        ultimo_error=e.toString()+" "+tmp;
                                        exc = "<VTG>" + e.getMessage();
                                        tot +=0;
                                    }
                                }
                                SPEED_KMH = tot/(double)speedVector.size();
                                
                            }catch(Exception e){
                                ultimo_error=e.toString();
                                SPEED_KMH = 0;
                            }
                        }*/
                    }
                    
                    TYPE +=  ":" + exc;
                    s = "";
                }
            }while(i != -1 && terminar==false);
            terminar=false;
            is.close();
            conn.close();
        }catch(Exception e){
            ultimo_error=e.toString();
            err = e.toString();
            System.out.println(e);
            estado=Gestor_GPS.estado_GPS_Error;
        }
        TYPE = "EXITED!! : " + err;
    }
    
    /*
     * Split the gps data string and return a string array.
     * Example of GGA string: 123519,4807.038,N,01131.324,E,1,08,0.9,545.4,M,46.9,M, , *42
     */
    private String[] splitString(String s) throws Exception{
        int i=0;
        int pos=0;
        int nextPos=0;
        
        // check how big the array is.
        while(pos>-1){
            pos = s.indexOf(",", pos);
            if(pos<0){
                continue;
            }
            pos++;
            i++;
        }
        
        if(i>25){
            throw new Exception("to big:" + i);
        }
        
        i=0;
        pos=0;
        try {
            // Start splitting the string, search for each ','
            while(pos>-1){
                pos = s.indexOf(",", pos);
                if(pos<0){
                    continue;
                }
                
                nextPos = s.indexOf(",", pos+1);
                if(nextPos<0){
                    nextPos = s.length();
                }
                
                result[i] = s.substring(pos+1, nextPos);
                i++;
                if(pos>-1){
                    pos++;
                }
            }
            
            return result;
            
        } catch (Exception e) {
            return result;
        }
    }
    
}
