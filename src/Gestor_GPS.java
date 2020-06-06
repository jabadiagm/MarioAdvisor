
/*
 * Gestor_GPS.java
 *
 * Created on 29 de julio de 2008, 16:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Gestor_GPS implements Runnable{
    //gestiona los distintos puntos de entrada de posición:
    //*GPS externo bluetooth
    //*GPS interno JSR179
    //*GPS virtual (depuración)
    //la selección del tipo de dispositivo se hace con la dirección dada al constructor
    //por defecto supone bluetooth, a no ser que se usa uno de estos valores
    //jsr179->supone GPS interno
    //virtual->GPS dummy para depuración
    //constantes tipo de estado del objeto
    public static final int estado_GPS_ON_Listo=0;
    public static final int estado_GPS_OK_No_Listo=1; //GPS conectado, pero sin localización
    public static final int estado_GPS_OFF=2; //estado inicial
    public static final int estado_GPS_Error=3;
    //constantes tipo de dispositivo conectado
    private static final int tipo_GPS_bluetooth=0;
    private static final int tipo_GPS_JSR179=1;
    private static final int tipo_GPS_virtual=2;
    //variables públicas
    public int estado;
    public float longitud;
    public float latitud;
    public int velocidad;
    public int rumbo;
    public int altura;
    public String ultimo_error;
    

    //variables privadas
    //dispositivos GPS
    private GPSReader GPS;
    private GPS_jsr179 gps_interno;
    //configuración del sistema
    private Configuracion configuracion;
    //tracklog
    private Tracklog tracklog;
    //tipo de GPS
    private int tipo_GPS;
    private Thread t;
    private boolean terminar_GPS_virtual=false;
    
    /** Creates a new instance of Gestor_GPS */
    public Gestor_GPS(Configuracion configuracion,float longitud_actual,float latitud_actual,Tracklog tracklog) {
        //la creación del objeto lleva consigo la conexión con el dispositivo
        //la posición en el mapa antes de conectar se usa con el gps virtual, 
        //que parte de ahí.
        int retorno;
        this.tracklog=tracklog;
        estado=estado_GPS_OFF;
        this.configuracion=configuracion;
        this.longitud=longitud_actual;
        this.latitud=latitud_actual;
        if (configuracion.GPS_url.toLowerCase().compareTo("virtual")==0) { //URL de GPS virtual
            tipo_GPS=this.tipo_GPS_virtual;
            //arranca la tarea del GPS vistual
            t = new Thread(this);
            t.start();
        } else if (configuracion.GPS_url.toLowerCase().compareTo("jsr179")==0) { //URL de GPS interno
            if (configuracion.JSR179_disponible==false) return; //no hay GPS interno
            tipo_GPS=this.tipo_GPS_JSR179;
            gps_interno=new GPS_jsr179(this);
            retorno=gps_interno.iniciar();
            if (retorno!=0) {
                this.estado=estado_GPS_Error;
                this.ultimo_error="Error initializing Internal GPS.";
                return;
            }
        } else {
            if (configuracion.JSR82_disponible==false) return; //no hay bluetooth
            tipo_GPS=this.tipo_GPS_bluetooth;
            GPS=new GPSReader(configuracion.GPS_url,this);
        }        
        
    }
    public void desconectar() {
        if (estado!=this.estado_GPS_ON_Listo && estado==this.estado_GPS_OK_No_Listo) return; //debe haber un objeto activo
        if (tipo_GPS==this.tipo_GPS_virtual) {
            //cierra la tarea
            terminar_GPS_virtual=true;
            while (this.estado!=Gestor_GPS.estado_GPS_OFF);
            return;
        } else if (tipo_GPS==this.tipo_GPS_bluetooth) { //GPS bluetooth
            GPS.cerrar(); //cierra la tarea interna
            GPS=null;
        } else if (tipo_GPS==this.tipo_GPS_JSR179) { //GPS bluetooth
            gps_interno.terminar();
            gps_interno=null;
        }
    }
    public void notificar_nuevo_traclog(Tracklog tracklog){
        //avisa al objeto de que hay un nuevo objeto tracklog
        this.tracklog=tracklog;
    }
    public void notificar_evento_GPS_bluetooth() {
        //función llamada desde el dispositivo bluetooth para indicar que hay
        //una nueva lectura, o un cambio de estado
        Tipo_Posicion posicion;
        if (GPS==null) return; //error extraño
        this.longitud=GPS.longitud;
        this.latitud=GPS.latitud;
        this.rumbo=GPS.rumbo;
        this.velocidad=GPS.velocidad;
        this.estado=GPS.estado;
        this.altura=GPS.altura;
        if (estado==Gestor_GPS.estado_GPS_ON_Listo) crear_posicion_y_notificar(); //actualiza el tracklog
    }
    public void notificar_evento_GPS_interno() {
        //función llamada desde el dispositivo jsr179 para indicar que hay
        //una nueva lectura, o un cambio de estado
        this.longitud=gps_interno.longitud;
        this.latitud=gps_interno.latitud;
        this.rumbo=gps_interno.rumbo;
        this.velocidad=gps_interno.velocidad;
        this.estado=gps_interno.estado;
        this.altura=gps_interno.altura;
        if (estado==Gestor_GPS.estado_GPS_ON_Listo) crear_posicion_y_notificar(); //actualiza el tracklog
    }

    public void run() {
        //gestión del GPS virtual. por ahora, sólo para depuración
        this.estado=this.estado_GPS_ON_Listo;
        this.altura=100;
        while (terminar_GPS_virtual==false) {
            this.latitud+=0.0001;
            this.longitud-=0.00001;
            this.velocidad++;
            if (this.velocidad>100) this.velocidad=0;
            this.rumbo++;
            if (this.rumbo>359) this.rumbo=0;
            this.altura++;
            if (this.altura>666) this.altura=100;
            crear_posicion_y_notificar(); //actualiza el tracklog
            try {
                t.sleep(500);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        //si se ha llegado aquí, es que se ha dado la orden de salir
        this.estado=Gestor_GPS.estado_GPS_OFF;
    }
    private void crear_posicion_y_notificar () {
        //si el tracklog está activo, hay que notificarle la nueva posición
        Tipo_Posicion posicion;
        String cellid_str;
        String lac_str;
        int cellid;
        int lac;
        if (configuracion.tracklog_activado==true) {
            if (configuracion.recoger_cellid==false) { //sin leer señal de repetidores
                posicion=new Tipo_Posicion(System.currentTimeMillis(),longitud,latitud,velocidad,rumbo,altura);
            } else {
                //primero prueba con funciones para sony ericsson
                cellid_str=System.getProperty("com.sonyericsson.net.cellid");
                lac_str=System.getProperty("com.sonyericsson.net.lac");
                if (cellid_str!=null && lac_str!=null) {
                    cellid=Integer.parseInt(cellid_str,16);
                    lac=Integer.parseInt(lac_str,16);
                    posicion=new Tipo_Posicion(System.currentTimeMillis(),longitud,latitud,velocidad,rumbo,altura,cellid,lac);
                } else { //no se ha podido obtener información del repetidor
                    posicion=new Tipo_Posicion(System.currentTimeMillis(),longitud,latitud,velocidad,rumbo,altura,0,0);
                }
                
            }
            
            tracklog.notificar_nueva_posicion(posicion);
        }        
    }
    
}
