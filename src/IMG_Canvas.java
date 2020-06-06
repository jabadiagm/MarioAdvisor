import java.io.IOException;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.midlet.MIDlet;

/*
 * IMG_Canvas.java
 *
 * Created on 7 de noviembre de 2007, 19:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class IMG_Canvas extends GameCanvas implements CommandListener,Runnable{
    private Visor_IMG midlet;
    private Thread t;
    //elementos del menú
    private Command cmd_stop;
    private Command cmd_configuracion; 
    private Command cmd_pantalla_completa;
    private Command cmd_GPS_On;
    private Command cmd_GPS_Off;
    private Command cmd_prueba;
    //variables de manejo de marco de mapa
    private double scaletop;
    private double scaleleft;
    private double scaleheight;
    private double scalewidth;
    private double scalebottom; //necesarias para evitar operaciones
    private double scaleright;
    private double centro_Lon; //centro del lienzo sobre el que se dibuja el mapa. es un valor que sólo cambia al generar un nuevo mapa
    private double centro_Lat;
    private double escala_X; //píxeles/grados_long
    private double escala_Y; //píxeles/grados_lat
    private int mapa_X; //tamaño del mapa
    private int mapa_Y; //la longitud del mapa total será la parte visible multiplicada por factor_mapa  (factor^2 en superficie)
    private int pantalla_X; //tamaño de la pantalla
    private int pantalla_Y;
    private int delta_X=0;
    private int delta_Y=0;
    //límites de las deltas, que cambian según el tamaño de la pantalla del móvil y el factor de tamaño
    private int delta_X_max;
    private int delta_Y_max;
    
    //coordenadas terrestres del centro de la pantalla actual
    private double centro_pantalla_Lon; // es un valor que cambia cada vez que cambia delta_X
    private double centro_pantalla_Lat; // es un valor que cambia cada vez que cambia delta_Y
    private Font fuente_pequeña,fuente_mediana,fuente_grande;
    //estado del teclado
    boolean tecla_1_pulsada=false;
    boolean tecla_3_pulsada=false;
    boolean tecla_7_pulsada=false;
    boolean tecla_9_pulsada=false;
    boolean tecla_asterisco_pulsada=false;
    boolean tecla_0_pulsada=false;
    boolean tecla_almohadilla_pulsada=false;

    //valores permitidos de zoom, en metros. un valor de zoom representa la quinta parte de la latitud visible en el mapa
    private int [] zoom={12,20,30,50,80,120,200,300,500,800,1200,2000,3000,5000,8000,12000,20000,30000,50000,80000,120000,200000,300000,500000,800000};
    private int nivel_zoom; //puntero al vector de zoom.
    //imagen
    private Image lienzo;
    private Image cruz_central;
    private Graphics graphics_lienzo;
    private Gestor_Mapas gestor_mapas; //=new Gestor_Mapas("prueba");
    private GPSReader GPS;
    //variables de navegación
    private boolean GPS_virtual=false; //true para depuración
    boolean navegacion_GPS_on=false; //si es true, se marca el puntero de dirección en la posición que diga el GPS
    boolean navegacion_manual=true; //si es true, se muestra la brújula y el movimiento lo controla el teclado. si es false, el movimiento es controlado externamente y no se dibuja la cruz
    public boolean pausar=false; //pasa a true desde fuera para pausar el bucle principal

    
    private long tiempo;//medición de tiempos de tareas
    private Auxiliar auxiliar; //clase encargada de cargar las propiedades de los puntos
    private Image [] iconos; //almacén de iconos de los POI's
    private Mapa_IMG [] mapas; //definición vectorial de los mapas de pantalla
    private Gestor_Etiquetas gestor_etiquetas;
    private Configuracion configuracion;
    Graphics g;

    private int [] colores_poligonos={0x000000,0xa4b094,0xa4b094,0xa4b094,0xF0F0F0,0xF0F0F0,0xF0F0F0,0xf8B880,0xF0F0F0,0xF0F0F0,0xf0B880,0xf8B880,0x808080,0xF0F0F0,0xF0F0F0,0x000000, 
                                      0x000000,0x000000,0x000000,0xcc9900,0xF0F0F0,0xF0F0F0,0xF0F0F0,0x90C000,0xF0F0F0,0xf8B880,0x00FF00,0x000000,0x000000,0x000000,0xb7E999,0xb7E999, 
                                      0xb7E999,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x0080ff,0xF0F0F0,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000, 
                                      0x000000,0x000000,0xFF8000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xF0F0F0,0x0080ff,0x0080ff,0x0080ff,0x0080ff,        
                                      0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0xF0F0F0,0xFFFFFF,0xF0F0F0,0xF0F0F0,0xF0F0F0,0xF0F0F0,
                                      0xF0F0F0,0xF0F0F0,0xF0F0F0,0xF0F0F0};
    private int [] colores_polilineas={0x000000,0x0000ff,0xff0000,0xff0000,0x101010,0x000000,0x808080,0x808080,0x101010,0x808080,0x808080,0x101010,0x101010,0x000000,0x000000,0x000000,
                                       0x000000,0x000000,0x000000,0x000000,0x000000,0x101010,0x101010,0x000000,0x3c9Dff,0x101010,0x000000,0x000000,0x101010,0x101010,0x808080,0x3c9Dff,
                                       0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010};


    /** Creates a new instance of IMG_Canvas */
    public IMG_Canvas (Visor_IMG m, Configuracion config) {
        super(false); //permite el procesador de todas las teclas en keyPressed/keyReleased...
        configuracion=config;
        midlet=m;
    }
    private void ajustar_parametros_pantalla () {
        //obtiene el tamaño de la pantalla y define el tamaño del mapa, según el factor
        this.pantalla_X=this.getWidth();
        this.pantalla_Y=this.getHeight();        
        mapa_X=configuracion.factor_mapa*pantalla_X;
        mapa_Y=configuracion.factor_mapa*pantalla_Y;
        lienzo= Image.createImage(mapa_X,mapa_Y);
        graphics_lienzo=lienzo.getGraphics();
        this.delta_X_max=mapa_X-pantalla_X-1;
        this.delta_Y_max=mapa_Y-pantalla_Y-1;
    }
    public void inicializar(){
        //activa pantalla completa, si la configuración lo indica
        if (configuracion.pantalla_completa==true) setFullScreenMode(true);
        ajustar_parametros_pantalla();
        cmd_stop=new Command("Exit",Command.ITEM,0);
        this.addCommand(cmd_stop);
        cmd_configuracion=new Command("Settings",Command.ITEM,5);
        this.addCommand(cmd_configuracion);
        cmd_pantalla_completa=new Command("Full Screen ON/OFF",Command.ITEM,1);
        this.addCommand(cmd_pantalla_completa);
        cmd_GPS_On=new Command("GPS On",Command.ITEM,2);
        cmd_GPS_Off=new Command("GPS Off",Command.ITEM,2); //este control queda sin añadir
        this.addCommand(cmd_GPS_On);
        this.setCommandListener(this);
        g=this.getGraphics();
        g.setFont(fuente_pequeña);
        gestor_mapas=new Gestor_Mapas(configuracion.ruta_carpeta_archivos,g,this,configuracion.detalle_minimo_mapa_general);
        try { //carga la cruz central
            cruz_central=Image.createImage("/cruz_cambiar.png");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        auxiliar=new Auxiliar(); //carga de las propiedades de puntos
        cargar_iconos();
        fuente_pequeña=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL);
        fuente_mediana=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_MEDIUM);
        fuente_grande=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_LARGE);        
        t=new Thread(this);
        t.start();
    }    
    public void commandAction(Command c, Displayable d){
        if (c==cmd_stop) {
            //actualiza la posición actual, el nivel de zoom y guarda la configuración antes de salir
            configuracion.centro_longitud_inicial=(float)this.centro_pantalla_Lon;
            configuracion.centro_latitud_inicial=(float)this.centro_pantalla_Lat;
            configuracion.nivel_zoom_inicial=this.nivel_zoom;
            configuracion.guardar_configuracion();
            midlet.exitMIDlet();
        } else if (c==cmd_pantalla_completa) {
            //si se está a pantalla completa, se vuelve a modo normal, y viceversa
            if (configuracion.pantalla_completa==true) {
                setFullScreenMode(false);
                configuracion.pantalla_completa=false;
            } else {
                setFullScreenMode(true);
                configuracion.pantalla_completa=true;
            }
            g=this.getGraphics();
            ajustar_parametros_pantalla();
            regenerar_mapa(nivel_zoom); //vuelve a cargar el mapa
            regenerar_pantalla();
        } else if (c==cmd_GPS_On) {
            this.removeCommand(cmd_GPS_On);
            this.addCommand(cmd_GPS_Off);
            conectar_GPS();
        } else if (c==cmd_GPS_Off) {
            this.removeCommand(cmd_GPS_Off);
            this.addCommand(cmd_GPS_On);
            desconectar_GPS();
        } else if (c==cmd_configuracion) {
            this.pausar=true;
            midlet.mostrar_configuracion();
            
        }
    }
    private void cargar_iconos() {
        //rellena la matriz de iconos PNG's de los POI's
        int contador;
        String nombre_icono;
        iconos=new Image[auxiliar.numero_iconos];
        for (contador=auxiliar.numero_iconos-1;contador>=0;contador--) {
            nombre_icono=String.valueOf(contador);
            while (nombre_icono.length()<3) { //añade ceros por la izquierda
                nombre_icono="0"+nombre_icono;
            }
            nombre_icono=nombre_icono+".png";
            try {
                iconos[contador]=iconos[contador].createImage("/res/"+nombre_icono);
            } catch (IOException ex) {
                ex.printStackTrace();
            }             
        }
    }
    public void run() {
        int estado_teclas;
        boolean redibujar=false;; //indica si ha habido algún cambio que obligue a colocar de nuevo el lienzo
        Image icono=null;
        //valores iniciales
        nivel_zoom=configuracion.nivel_zoom_inicial;
        centro_pantalla_Lon=configuracion.centro_longitud_inicial;
        centro_pantalla_Lat=configuracion.centro_latitud_inicial;
        regenerar_mapa(nivel_zoom);
        redibujar=true;
        boolean parate=false;
        while(true){
            while (pausar==true) 
                try {t.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            estado_teclas=getKeyStates();
            if ((estado_teclas & LEFT_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_X>1) {
                    delta_X--;
                    delta_X--;
                    centro_pantalla_Lon-=2/this.escala_X;
                } else { //se ha llegado al borde, hay que solicitar un nuevo mapa
                    regenerar_mapa(nivel_zoom);
                }
            } else if ((estado_teclas & RIGHT_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_X<(delta_X_max-1)) {
                    delta_X++;
                    delta_X++;
                    centro_pantalla_Lon+=2/this.escala_X;
                } else {
                    regenerar_mapa(nivel_zoom);
                }
            } else if ((estado_teclas & DOWN_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_Y<(delta_Y_max-1)) {
                    delta_Y++;
                    delta_Y++;
                    centro_pantalla_Lat-=2/this.escala_Y;
                } else {
                    regenerar_mapa(nivel_zoom);
                }
            } else if ((estado_teclas & UP_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_Y>1) {
                    delta_Y--;
                    delta_Y--;
                    centro_pantalla_Lat+=2/this.escala_Y;
                } else {
                    regenerar_mapa(nivel_zoom);
                }
            } else if ((estado_teclas & FIRE_PRESSED) !=0) {
                if (nivel_zoom>0) {
                    redibujar=true;
                    regenerar_mapa(--nivel_zoom);
                }
            }
            //comprobación del resto de teclas
            if (tecla_0_pulsada==true) { //zoom out
                if (nivel_zoom<(zoom.length-1)) {
                    redibujar=true;
                    regenerar_mapa(++nivel_zoom);
                }
            }
            if (tecla_asterisco_pulsada==true) { //vuelta al control por GPS
                if (navegacion_GPS_on==true && navegacion_manual==true) {
                    navegacion_manual=false;
                    //centra la pantalla donde diga el GPS
                    centro_pantalla_Lon=GPS.longitud;
                    centro_pantalla_Lat=GPS.latitud;
                    regenerar_mapa(nivel_zoom);
                    redibujar=true;
                }
            }
            if (tecla_almohadilla_pulsada==true) {
                if (nivel_zoom>0) {
                    redibujar=true;
                    regenerar_mapa(--nivel_zoom);
                }                
            }
            //navegación GPS. si está activada, el encuadre depende de la posición
            if (navegacion_GPS_on==true && navegacion_manual==false) {
                //la diferencia debe ser superior a 1 píxel
                if (GPS_virtual==true) {
                    GPS.latitud+=0.000001;
                    GPS.longitud-=0.0000005;
                }
                if (Math.abs(centro_pantalla_Lon-GPS.longitud)*escala_X>1 || Math.abs(centro_pantalla_Lat-GPS.latitud)*escala_Y>1) {
                    centro_pantalla_Lon=GPS.longitud;
                    centro_pantalla_Lat=GPS.latitud;
                    regenerar_mapa(nivel_zoom);
                    redibujar=true;
                }
            }
            
            if (redibujar==true) {
                //coordenadas del nuevo centro de la pantalla
                regenerar_pantalla();
                redibujar=false;
                
            }
            try {
                
                if (navegacion_GPS_on==false) { //si el control es manual...
                    t.sleep(5); //... interesa poder redibujar rápido
                } else { //si el control es automático...
                    t.sleep(10);  //...interesa dejar tiempo a las tareas que corren en secundario
                }
                
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    private void regenerar_pantalla () {
        //coloca el mapa en pantalla, junto con el resto de elementos visibles
        g.drawRegion(lienzo,0+delta_X,0+delta_Y,pantalla_X,pantalla_Y,Sprite.TRANS_NONE,0,0,Graphics.TOP | Graphics.LEFT);
        colocar_elementos_auxiliares(g);
        if (configuracion.depuracion==true) {
            g.drawString(new Integer(nivel_zoom).toString(),10,10,0);
            g.drawString((new Long(tiempo).toString()),10,20,0);
        }
        //g.drawRegion(lienzo,0+delta_X,0+delta_Y,this.getWidth(),this.getHeight(),Sprite.TRANS_NONE,0,0,Graphics.TOP | Graphics.LEFT);
        flushGraphics();
        
    }
    private void colocar_elementos_auxiliares(Graphics g) {
        int contador;
        int contador2;
        String cadena_longitud;
        String cadena_latitud;
        String GPS_longitud;
        String GPS_latitud;
        String cadena;
        //dibuja el puntero, las coordenadas actuales, y todo lo que se vaya añadiendo
        g.drawImage(cruz_central,pantalla_X/2,pantalla_Y/2,Graphics.HCENTER+Graphics.VCENTER);
        g.setFont(fuente_mediana);
        cadena_longitud=formato_coordenada(centro_pantalla_Lon);
        cadena_latitud=formato_coordenada(centro_pantalla_Lat);
        cadena=cadena_longitud+","+cadena_latitud;
        g.setColor(255,255,255);
       g.drawString(cadena,9,3,Graphics.LEFT | Graphics.TOP);
        g.drawString(cadena,11,3,Graphics.LEFT | Graphics.TOP);
        g.drawString(cadena,9,5,Graphics.LEFT | Graphics.TOP);
        g.drawString(cadena,11,5,Graphics.LEFT | Graphics.TOP);
        g.setColor(0,0,0);
        g.drawString(cadena,10,4,Graphics.LEFT | Graphics.TOP);
        if (this.navegacion_GPS_on==true && GPS.NUM_SATELITES>2) {
            g.drawString("GPS ON",this.pantalla_X-10,4,Graphics.RIGHT | Graphics.TOP);
        }
       
        /*
        contador2=40; //coordenada y a partir de la cual se sigue escribiendo
        for (contador=0;contador<mapas.length;contador++) {
            if (mapas[contador]!=null) {
                g.drawString(mapas[contador].nombre_archivo+"("+mapas[contador].descripcion+")",10,contador2,0);
                contador2+=10;
            }
            
        }*/
        
    }
    private int conectar_GPS() {
        GPS=new GPSReader(configuracion.GPS_url);
        GPS.longitud=(float)centro_pantalla_Lon;
        GPS.latitud=(float)centro_pantalla_Lat;
        navegacion_GPS_on=true;
        navegacion_manual=false;
        return 0; //conexión realizada con éxito

    }
    private int desconectar_GPS() {
        GPS.cerrar(); //cierra la tarea interna
        GPS=null;
        navegacion_GPS_on=false;
        return 0;
    }
    private String formato_coordenada(double coordenada) {
        //devuelve como cadena el valor de una longitud o latitud, con 4 decimales
        int entero;
        int decimal;
        String cadena;
        String cadena_decimal;
        entero= (int) Math.abs(coordenada); //le quita el signo
        decimal=(int)(Math.abs(coordenada-entero)*10000);
        cadena_decimal=new Integer(decimal).toString();
        while (cadena_decimal.length()<4) { //añade ceros por la izquierda
            cadena_decimal="0"+cadena_decimal;
        }
        cadena=new Integer(entero).toString()+"."+cadena_decimal;
        if (coordenada<0) { //le vuelve a poner el signo
            cadena="-"+cadena;
        }
        return cadena;
    }
    private void regenerar_mapa(int nuevo_nivel_zoom) {
        //obtiene un nuevo mapa centrado en el punto indicado y prepara la pantalla para su visualización
        // a no ser que en el mapa actual quepa el recuadro solicitado y el nivel de detalle sea el mismo
        int contador;
        Tipo_Rectangulo limites;
        tiempo=System.currentTimeMillis();
        if (mapa_nuevo_necesario(mapas,(float)centro_pantalla_Lon,(float)centro_pantalla_Lat,nuevo_nivel_zoom)==true) {
            //antes de pedir el nuevo mapa libera la memoria ocupada por el anterior
            if (mapas!=null) { //puede ser un mapa nulo, como en el inicio
                for (contador=mapas.length-1;contador>=0;contador--) {
                    mapas[contador]=null;
                }
                System.gc();
            }
            limites=centro_zoom_2_rectangulo((float)centro_pantalla_Lon,(float)centro_pantalla_Lat,nuevo_nivel_zoom);
            mapas=gestor_mapas.generar_mapa(limites,nuevo_nivel_zoom);
        }
        this.definir_bordes_mapa((double)centro_pantalla_Lon,(double)centro_pantalla_Lat,nuevo_nivel_zoom);
        //la pantalla debe caer en el recuadro central
        this.delta_X=(int)((float)(configuracion.factor_mapa-1)*((float)pantalla_X/2)); //casting para tamaños de pantalla impares
        this.delta_Y=(int)((float)(configuracion.factor_mapa-1)*((float)pantalla_Y/2));
        dibujar_mapas(mapas);
        tiempo=System.currentTimeMillis()-tiempo;
        System.gc();
    }
    private boolean mapa_nuevo_necesario (Mapa_IMG [] mapas,float longitud,float latitud, int nuevo_nivel_zoom) {
        //devuelve true si el rectángulo y el nivel de detalle solicidados no caben en el mapa actual
        int contador;
        boolean encontrado=false;
        Tipo_Rectangulo nuevos_limites;
        nuevos_limites=centro_zoom_2_rectangulo(longitud,latitud,nuevo_nivel_zoom);
        //recorre los mapas en busca de uno no nulo
        if (mapas==null) return true; //mapa vacío
        for (contador=mapas.length-1;contador>=0;contador--) {
            if (mapas[contador]!=null) {
                encontrado=true;
                break;
            }
        }
        if (encontrado==false) return true; //mapa vacío
        if (gestor_mapas.leer_detalle(nuevo_nivel_zoom)!=mapas[contador].nivel_detalle) return true; //cambia el nivel de detalle, hay que recalcular
        if (rectangulo_interior(mapas[contador].limites,nuevos_limites)==false) return true; //los nuevos límites no caben, hay que recalcular
        return false;
    }
    public void keyPressed(int keyCode) { //comprobación de las teclas auxiliares
        switch (keyCode) {
            case KEY_NUM0:
                tecla_0_pulsada=true;
                break;
            case KEY_NUM1:
                tecla_1_pulsada=true;
                break;
            case KEY_NUM3:
                tecla_3_pulsada=true;
                break;
            case KEY_NUM7:
                tecla_7_pulsada=true;
                break;
            case KEY_NUM9:
                tecla_9_pulsada=true;
                break;
            case KEY_STAR:
                tecla_asterisco_pulsada=true;
                break;
            case KEY_POUND:
                tecla_almohadilla_pulsada=true;
                break;
        }
    }
    public void keyRepeated(int keyCode) { //comprobación de las teclas repetidas
        switch (keyCode) {
            
        }
    }
    public void keyReleased(int keyCode) { //comprobación de las teclas auxiliares
        switch (keyCode) {
            case KEY_NUM0:
                tecla_0_pulsada=false;
                break;
            case KEY_NUM1:
                tecla_1_pulsada=false;
                break;
            case KEY_NUM3:
                tecla_3_pulsada=false;
                break;
            case KEY_NUM7:
                tecla_7_pulsada=false;
                break;
            case KEY_NUM9:
                tecla_9_pulsada=false;
                break;
            case KEY_STAR:
                tecla_asterisco_pulsada=false;
                break;
            case KEY_POUND:
                tecla_almohadilla_pulsada=false;
                break;
        }
    }    

    public void definir_bordes_mapa(double longitud_centro,double latitud_centro,int nivel_zoom2){
        Tipo_Rectangulo rectangulo;
        this.nivel_zoom=nivel_zoom2; //hay otra variable global llamada nivel_zoom
        this.centro_Lon=longitud_centro;
        this.centro_Lat=latitud_centro;
        rectangulo=centro_zoom_2_rectangulo(longitud_centro,latitud_centro, nivel_zoom2);
        this.scaleheight=rectangulo.norte-rectangulo.sur; //0.000135*zoom[nivel_zoom2];
        this.scalewidth=rectangulo.este-rectangulo.oeste;//0.000108*zoom[nivel_zoom2]/Math.cos(Math.toRadians(latitud_centro));
        this.scaletop=rectangulo.norte;
        this.scalebottom=rectangulo.sur;
        this.scaleleft=rectangulo.oeste;
        this.scaleright=rectangulo.este;
        this.escala_X = (mapa_X - 1) / scalewidth;
        this.escala_Y = (mapa_Y - 1) / scaleheight;
    }
    public Tipo_Rectangulo centro_zoom_2_rectangulo(double longitud_centro,double latitud_centro,int nivel_zoom2){
        //convierte las coordenadas del centro y el nivel de zoom a un rectángulo con los límites norte, sur, este, oeste
        Tipo_Rectangulo rectangulo=new Tipo_Rectangulo();
        double altura;
        double anchura;
        //altura=0.000135*zoom[nivel_zoom2];
        //anchura=0.000108*zoom[nivel_zoom2]/Math.cos(Math.toRadians(latitud_centro));
        altura=0.000045*configuracion.factor_mapa*zoom[nivel_zoom2];
        anchura=0.000045*configuracion.factor_mapa*zoom[nivel_zoom2]*pantalla_X/(double)pantalla_Y/Math.cos(Math.toRadians(latitud_centro));
        rectangulo.norte=(float)(latitud_centro+altura/2);
        rectangulo.sur=(float)(latitud_centro-altura/2);
        rectangulo.oeste=(float)(longitud_centro-anchura/2);
        rectangulo.este=(float)(longitud_centro+anchura/2);
        return rectangulo;
    }    
    public boolean rectangulo_interior (Tipo_Rectangulo grande,Tipo_Rectangulo pequeño) {
        //devuelve true si el rectangulo pequeño está contenido en el grande
        if (grande.norte>=pequeño.norte && grande.sur <=pequeño.sur && grande.oeste <=pequeño.oeste && grande.este >=pequeño.este) {
            return true;
        } else return false;
    }
    public void dibujar_punto(Tipo_Punto punto){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja el punto como un círculo
        int x,y;
        x=(int)((punto.longitud-scaleleft)*escala_X);
        y=(int)((scaletop-punto.latitud)*escala_Y);
        graphics_lienzo.drawArc(x,y,5,5,0,360);
        if (punto.etiqueta!=null) {
            graphics_lienzo.drawString(punto.etiqueta.nombre_completo,x,y,0);
        }
    }
    public void dibujar_punto_icono(Tipo_Punto punto){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja el punto con el icono
        //que le corresponde
        int x,y;
        Tipo_Propiedades_Punto propiedades;
        propiedades=auxiliar.leer_propiedades_punto(punto.tipo);
        x=(int)((punto.longitud-scaleleft)*escala_X);
        y=(int)((scaletop-punto.latitud)*escala_Y);
        //graphics_lienzo.drawArc(x,y,5,5,0,360);
        dibujar_icono(x,y,propiedades.indice);
        if (punto.etiqueta!=null) {
            gestor_etiquetas.añadir_etiqueta(punto.etiqueta.nombre_completo,x+1,y+1,fuente_pequeña);
            //graphics_lienzo.drawString(punto.etiqueta.nombre_completo,x+1,y+1,0);
        }        
    }    
    public void dibujar_icono(int x,int y,int indice) {
        //coloca en pantalla el png indicado
        if (indice>=auxiliar.numero_iconos) indice=1;
        graphics_lienzo.drawImage(iconos[indice],x-8,y-7,0);
    }
    public void dibujar_polilinea(Poly polilinea){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja la polilínea
        int [] x;
        int [] y;
        int contador;
        //reserva espacio para las cooredenas en píxeles
        x=new int [polilinea.puntos_X.length];
        y=new int [polilinea.puntos_Y.length];
        for (contador=0; contador<polilinea.puntos_X.length;contador++) {
            x[contador] = (int)((polilinea.puntos_X[contador] - scaleleft) * escala_X);
            y[contador] = (int)((scaletop - polilinea.puntos_Y[contador]) * escala_Y);
        }
        //dibuja la polilinea
        PolygonGraphics.drawPolyline(graphics_lienzo,x,y);
    }
    public void dibujar_polilinea(Tipo_Poli polilinea){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja la polilínea
        int [] x;
        int [] y;
        int contador;
        int tipo;
        //reserva espacio para las cooredenas en píxeles
        x=new int [polilinea.puntos_X.length];
        y=new int [polilinea.puntos_Y.length];
        for (contador=0; contador<polilinea.puntos_X.length;contador++) {
            x[contador] = (int)((polilinea.puntos_X[contador] - scaleleft) * escala_X);
            y[contador] = (int)((scaletop - polilinea.puntos_Y[contador]) * escala_Y);
        }
        //dibuja la polilinea
        tipo=polilinea.tipo;
        graphics_lienzo.setColor(colores_polilineas[tipo]); //ajusta el color de la pluma
        if (tipo==0 | tipo==1 | tipo==2 | tipo==3) {
            PolygonGraphics.dibujar_polilinea2(graphics_lienzo,x,y);
        } else {
            PolygonGraphics.drawPolyline(graphics_lienzo,x,y);
        }
        
    }    
        public void dibujar_poligono(Poly polilinea){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja un polígono relleno
        int [] x;
        int [] y;
        int contador;
        //reserva espacio para las cooredenas en píxeles
        x=new int [polilinea.puntos_X.length];
        y=new int [polilinea.puntos_Y.length];
        for (contador=0; contador<polilinea.puntos_X.length;contador++) {
            x[contador] = (int)((polilinea.puntos_X[contador] - scaleleft) * escala_X);
            y[contador] = (int)((scaletop - polilinea.puntos_Y[contador]) * escala_Y);
        }
        //dibuja la polilinea
        PolygonGraphics.fillPolygon(graphics_lienzo,x,y);
    }
        public void dibujar_poligono(Tipo_Poli poligono){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja un polígono relleno
        int [] x;
        int [] y;
        int contador;
        if (poligono.tipo==75 | poligono.tipo==74){
            return;
        }
        //ajusta el color del pincel
        graphics_lienzo.setColor(colores_poligonos[poligono.tipo]);
        //reserva espacio para las coordenadas en píxeles
        x=new int [poligono.puntos_X.length];
        y=new int [poligono.puntos_Y.length];
        for (contador=0; contador<poligono.puntos_X.length;contador++) {
            x[contador] = (int)((poligono.puntos_X[contador] - scaleleft) * escala_X);
            y[contador] = (int)((scaletop - poligono.puntos_Y[contador]) * escala_Y);
        }
        //dibuja la polilinea
        PolygonGraphics.fillPolygon(graphics_lienzo,x,y);
    }        
    private void dibujar_mapas(Mapa_IMG [] mapas){
        int contador;
        int contador2;
        int contador_poligono;
        int contador_punto;
        boolean parate;
        Mapa_IMG mapa;
        int tipo_punto;
        Tipo_Punto punto;
        graphics_lienzo.setColor(255,255,255);
        graphics_lienzo.fillRect(0,0,lienzo.getWidth(),lienzo.getHeight());
        graphics_lienzo.setColor(0,0,0);
        Tipo_Poli poligono;
        if (mapas==null) return; //si no hay mapa, deja la pantalla en blanco y termina
        gestor_etiquetas=new Gestor_Etiquetas();
        for (contador2=mapas.length-1;contador2>=0;contador2--) {
            mapa=mapas[contador2];
            if (mapa==null) continue;
            //los polígonos se dibujan empezando por los de menor índice (áreas urbanas)
            for (contador_poligono=0; contador_poligono<colores_poligonos.length;contador_poligono++) {
                for (contador=mapa.Poligonos.size()-1;contador>=0;contador--) { //recorre los polígonos
                    poligono=(Tipo_Poli)mapa.Poligonos.elementAt(contador);
                    if (poligono.tipo==contador_poligono) {
                        dibujar_poligono(poligono);
                    }
                }
            }
            
            for (contador=0;contador<mapas[contador2].Polilineas.size();contador++) { //recorre los puntos
                dibujar_polilinea((Tipo_Poli)mapa.Polilineas.elementAt(contador));
            }
            for (contador=0;contador<mapa.Puntos.size();contador++) { //recorre los puntos
                dibujar_punto_icono((Tipo_Punto)mapa.Puntos.elementAt(contador));
            }
            graphics_lienzo.setColor(0x000000);
            
            for (contador_punto=1;contador_punto<auxiliar.numero_propiedades_puntos;contador_punto++){
                //recorre los puntos de menor a mayor índice. esto hace que las etiquetas de las ciudades
                //se coloquen dando prioridad a las de más habitantes
                tipo_punto=auxiliar.tipo[contador_punto]; //lee el tipo correspondiente
                //recorre todos los puntos en busca del tipo adecuado
                //for (contador=mapa.Puntos_Indexados.size()-1;contador>=0;contador--){
                for (contador=0;contador<mapa.Puntos_Indexados.size();contador++){  //se le da la vuelta para que el primer idioma en aparecer sea el adecuado
                    punto=(Tipo_Punto)mapa.Puntos_Indexados.elementAt(contador);
                    if (punto.tipo==tipo_punto) dibujar_punto_icono(punto);
                }
            }
            //for (contador=0;contador<mapa.Puntos_Indexados.size();contador++) { //recorre los puntos indexados
            //    dibujar_punto_icono((Tipo_Punto)mapa.Puntos_Indexados.elementAt(contador));
            //}        
            //coloca las etiquetas
            gestor_etiquetas.dibujar_etiquetas(graphics_lienzo);
        }
    }
}
