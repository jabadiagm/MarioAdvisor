/*****************************************************************************

 Description: This class is used to read the data from a GPS receiver using the NMEA protocol.

 Created By: Oscar Vivall 2005-06-28
 
 @file        GPSReader.java

 COPYRIGHT All rights reserved Sony Ericsson Mobile Communications AB 2004.
 The software is the copyrighted work of Sony Ericsson Mobile Communications AB.
 The use of the software is subject to the terms of the end-user license 
 agreement which accompanies or is included with the software. The software is 
 provided "as is" and Sony Ericsson specifically disclaim any warranty or 
 condition whatsoever regarding merchantability or fitness for a specific 
 purpose, title or non-infringement. No warranty of any kind is made in 
 relation to the condition, suitability, availability, accuracy, reliability, 
 merchantability and/or non-infringement of the software provided herein.

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
    private String []result = new String[25];
    
    private Vector speedVector = new Vector();
    
    public static String TYPE=""; // Used for debugging.
    private String exc = ""; // Used for debugging.

    // Array used to interpolate the speed.
    private String []sp = new String[9];
    private int spIndex = 0;
    public int estado=0;
    public final int INACTIVO=0;
    public final int CONECTADO=1;
    private boolean terminar=false; //pasa a true para avisar del cierre del proceso run
    /*
     * The url to the bluetooth device is the String parameter.
    */
    public GPSReader(String u){
        estado=this.INACTIVO; //inactivo
        url = u; // Connection string to the bluetooth device.
        Thread t = new Thread(this);
        t.start();
    }
private float [] extraer_coordenadas(String []data) {
    //obtiene la posición de una lína GGA
    //resultado[0]=longitud, resultado[1]=latitud
    float resultado[] =new float [2];
    String cadena;
    float temporal;
    cadena=data[1].substring(0,2); //grados de latitud
    resultado[1]=Float.parseFloat(cadena);
    cadena=data[1].substring(2); //minutos de latitud
    temporal=Float.parseFloat(cadena);
    temporal/=60; //pasa los minutos a grados
    resultado[1]+=temporal;
    if (data[2].compareTo("S")==0) resultado[1]=-resultado[1]; //la latitud sur es negativa
    cadena=data[3].substring(0,3); //grados de longitud
    resultado[0]=Float.parseFloat(cadena);
    cadena=data[3].substring(3); //minutos de longitud
    temporal=Float.parseFloat(cadena);
    temporal/=60; //pasa los minutos a grados
    resultado[0]+=temporal;    
    if (data[4].compareTo("W")==0) resultado[0]=-resultado[0]; //la longitud oeste es negativa
    return resultado;
}
public void cerrar() {
    //cierra las conexiones abiertas
    if (estado==this.CONECTADO) {
        terminar=true;
        while (terminar==true); //espera a que se cierre la tarea run
        estado=this.INACTIVO;
    }
}
    public void run() {

        StreamConnection conn = null;
        InputStream is = null;
        String err = ""; // used for debugging
        float [] resultado=new float[2];
        try{
            conn = (StreamConnection)Connector.open(url);
            is = conn.openInputStream();
            estado=this.CONECTADO;

            int i=0;
            char c;
            String s = "";
            String []data;
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
                            try{
                                data = splitString(DATA_STRING);

                                NUM_SATELITES_STR = data[6];
                                NUM_SATELITES = Integer.parseInt(data[6]);
                                if (NUM_SATELITES>2) {
                                    resultado=extraer_coordenadas(data);
                                    longitud=resultado[0];
                                    latitud=resultado[1];
                                }                                
                                exc = "";
                            }catch(Exception e){
                                exc = "<GGA>" + e.getMessage();
                                NUM_SATELITES = 0;
                            }

                        }else if(s.substring(0, 5).compareTo("GPVTG") == 0){
                            try{
                                data = splitString(DATA_STRING);

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
                                        exc = "<VTG>" + e.getMessage();
                                        tot +=0;
                                    }
                                }
                                SPEED_KMH = tot/(double)speedVector.size();

                            }catch(Exception e){
                                SPEED_KMH = 0;
                            }
                        }
                    }

                    TYPE +=  ":" + exc;
                    s = "";
                }
            }while(i != -1 && terminar==false);
            terminar=false;
            is.close();
            conn.close();
        }catch(Exception e){
            err = e.toString();
            System.out.println(e);
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
    }    
    
}
