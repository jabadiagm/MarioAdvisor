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
    private Command cmd_buscador_GPS_BT;
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
    boolean tecla_almohadilla_bloqueda=false;
    
    //valores permitidos de zoom, en metros. un valor de zoom representa la quinta parte de la latitud visible en el mapa
    private int [] zoom={12,20,30,50,80,120,200,300,500,800,1200,2000,3000,5000,8000,12000,20000,30000,50000,80000,120000,200000,300000,500000,800000};
    private String [] leyenda_zoom={"12 m","20 m","30 m","50 m","80 m","120 m","200 m","300 m","500 m","800 m","1.2 Km","2.0 km","3.0 km","5.0 km","8.0 km","12 km","20 km","30 km","50 km","80 km","120 km","200 km","300 km","500 km","800 km"};
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
    private boolean proceso_etiquetas_NET=false; //el proceso de estas etiqutas consume mucho tiempo y espacio, al menos por ahora. el programa arranca sin ese proceso
    
    
    private long tiempo;//medición de tiempos de tareas
    private Auxiliar auxiliar; //clase encargada de cargar las propiedades de los puntos
    private Image [] iconos; //almacén de iconos de los POI's
    private Mapa_IMG [] mapas; //definición vectorial de los mapas de pantalla
    private Gestor_Etiquetas gestor_etiquetas;
    private Configuracion configuracion;
    Graphics g;
    
    private int [] colores_poligonos={0x000000,0xa4b094,0xa4b094,0xa4b094,0xF0F0F0,0xF0F0F0,0xF0F0F0,0xf8B880,0xF0F0F0,0xF0F0F0,0xf0B880,0xf8B880,0xa0a0a0,0xF0F0F0,0xF0F0F0,0x000000,
    0x000000,0x000000,0x000000,0xcc9900,0xF0F0F0,0xF0F0F0,0xF0F0F0,0x90C000,0xF0F0F0,0xf8B880,0x00FF00,0x000000,0x000000,0x000000,0xb7E999,0xb7E999,
    0xb7E999,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x0080ff,0xF0F0F0,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,
    0x000000,0x000000,0xFF8000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0x000000,0xF0F0F0,0x0080ff,0x0080ff,0x0080ff,0x0080ff,
    0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0x0080ff,0xF0F0F0,0xFFFFFF,0xF0F0F0,0xF0F0F0,0xF0F0F0,0xF0F0F0,
    0xb7e999,0xF0F0F0,0xF0F0F0,0xF0F0F0};
    private int [] colores_polilineas={0x000000,0x0000ff,0xff0000,0xff0000,0x101010,0x000000,0x808080,0x808080,0x101010,0x808080,0x808080,0x101010,0x101010,0x000000,0x000000,0x000000,
    0x000000,0x000000,0x000000,0x000000,0x000000,0x101010,0x101010,0x000000,0x3c9Dff,0x101010,0x000000,0x000000,0x101010,0x101010,0x808080,0x3c9Dff,
    0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010,0x101010};
    private int nose=0;
    
    /** Creates a new instance of IMG_Canvas */
    public IMG_Canvas(Visor_IMG m, Configuracion config) {
        super(false); //permite el procesador de todas las teclas en keyPressed/keyReleased...
        configuracion=config;
        midlet=m;
    }
    private void ajustar_parametros_pantalla() {
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
        cmd_stop=new Command("Exit",Command.ITEM,7);
        this.addCommand(cmd_stop);
        cmd_configuracion=new Command("Settings",Command.ITEM,5);
        this.addCommand(cmd_configuracion);
        cmd_pantalla_completa=new Command("Full Screen ON/OFF",Command.ITEM,1);
        this.addCommand(cmd_pantalla_completa);
        cmd_GPS_On=new Command("GPS On",Command.ITEM,2);
        cmd_GPS_Off=new Command("GPS Off",Command.ITEM,2); //este control queda sin añadir
        this.addCommand(cmd_GPS_On);
        cmd_buscador_GPS_BT=new Command ("BT GPS Finder",Command.ITEM,6);
        this.addCommand(cmd_buscador_GPS_BT);
        this.setCommandListener(this);
        fuente_pequeña=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL);
        fuente_mediana=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_MEDIUM);
        fuente_grande=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_LARGE);
        g=this.getGraphics();
        gestor_mapas=new Gestor_Mapas(configuracion.ruta_carpeta_archivos,g,this,configuracion.detalle_minimo_mapa_general,configuracion.tamaño_cache_mapas,fuente_grande,configuracion.cache_etiquetas);
        //si se ha cargado la configuración por defecto, lon y lat valen cero.
        //entonces, se coloca la posición sugerida por gestor_mapas
        if (configuracion.centro_latitud_inicial==0 && configuracion.centro_longitud_inicial==0) {
            Tipo_Rectangulo rectangulo;
            rectangulo=gestor_mapas.sugerir_coordenadas_iniciales();
            if (rectangulo!=null) { //sólo si hay mapas disponibles
                configuracion.centro_longitud_inicial=(rectangulo.oeste+rectangulo.este)/2;
                configuracion.centro_latitud_inicial=(rectangulo.norte+rectangulo.sur)/2;
            }
            
        }
        
        try { //carga la cruz central
            cruz_central=Image.createImage("/cruz.png");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        auxiliar=new Auxiliar(); //carga de las propiedades de puntos
        cargar_iconos();
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
            regenerar_mapa(nivel_zoom,false); //vuelve a cargar el mapa
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
            
        } else if (c==cmd_buscador_GPS_BT) {
            this.pausar=true;
            midlet.mostrar_BT_canvas();
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
        regenerar_mapa(nivel_zoom,true);
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
                if (delta_X>2) {
                    delta_X--;
                    delta_X--;
                    delta_X--;
                    centro_pantalla_Lon-=3/this.escala_X;
                } else { //se ha llegado al borde, hay que solicitar un nuevo mapa
                    regenerar_mapa(nivel_zoom,false);
                }
            } else if ((estado_teclas & RIGHT_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_X<(delta_X_max-2)) {
                    delta_X++;
                    delta_X++;
                    delta_X++;
                    centro_pantalla_Lon+=3/this.escala_X;
                } else {
                    regenerar_mapa(nivel_zoom,false);
                }
            } else if ((estado_teclas & DOWN_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_Y<(delta_Y_max-2)) {
                    delta_Y++;
                    delta_Y++;
                    delta_Y++;
                    centro_pantalla_Lat-=3/this.escala_Y;
                } else {
                    regenerar_mapa(nivel_zoom,false);
                }
            } else if ((estado_teclas & UP_PRESSED) !=0) {
                navegacion_manual=true;
                redibujar=true;
                if (delta_Y>2) {
                    delta_Y--;
                    delta_Y--;
                    delta_Y--;
                    centro_pantalla_Lat+=3/this.escala_Y;
                } else {
                    regenerar_mapa(nivel_zoom,false);
                }
            } else if ((estado_teclas & FIRE_PRESSED) !=0) {
                if (nivel_zoom>0) {
                    redibujar=true;
                    regenerar_mapa(--nivel_zoom,false);
                }
            }
            //comprobación del resto de teclas
            if (tecla_0_pulsada==true) { //zoom out
                if (nivel_zoom<(zoom.length-1)) {
                    redibujar=true;
                    regenerar_mapa(++nivel_zoom,false);
                }
            }
            if (tecla_asterisco_pulsada==true) { //vuelta al control por GPS
                if (navegacion_GPS_on==true && navegacion_manual==true) {
                    navegacion_manual=false;
                    //centra la pantalla donde diga el GPS
                    centro_pantalla_Lon=GPS.longitud;
                    centro_pantalla_Lat=GPS.latitud;
                    regenerar_mapa(nivel_zoom,false);
                    redibujar=true;
                }
            }
            if (tecla_almohadilla_pulsada==true) {
                if (tecla_almohadilla_bloqueda==false) { //para evitar que al mantener pulsada la tecla cambie varias veces
                    if (this.proceso_etiquetas_NET==false) {
                        this.proceso_etiquetas_NET=true; //habilita el proceso completo de etiquetas
                    } else {
                        this.proceso_etiquetas_NET=false; //inhabilita el proceso completo de etiquetas
                    }
                    tecla_almohadilla_bloqueda=true;
                    regenerar_mapa(nivel_zoom,true);
                    redibujar=true;
                    
                }
            }
            //navegación GPS. si está activada, el encuadre depende de la posición
            if (navegacion_GPS_on==true && navegacion_manual==false) {
                //la diferencia debe ser superior a 1 píxel
                int gps_delta_x,gps_delta_y; //deriva en píxeles de la posición marcada por el GPS respecto del centro original del mapa
                if (GPS_virtual==true) {
                    GPS.latitud+=0.0001;
                    GPS.longitud-=0.00001;
                }
                //deltas si la posición la marca el GPS
                gps_delta_x=(int)((GPS.longitud-centro_Lon)*escala_X)+(int)((float)(configuracion.factor_mapa-1)*((float)pantalla_X/2));
                gps_delta_y=(int)((centro_Lat-GPS.latitud)*escala_Y)+(int)((float)(configuracion.factor_mapa-1)*((float)pantalla_Y/2));
                if (Math.abs(gps_delta_x-delta_X)>2 || Math.abs(gps_delta_y-delta_Y)*escala_Y>2) { //existe un desplazamiento >2 píxeles en algún eje
                    redibujar=true;
                    //ajusta las nuevas deltas
                    centro_pantalla_Lon+=(gps_delta_x-delta_X)/this.escala_X; 
                    centro_pantalla_Lat-=(gps_delta_y-delta_Y)/this.escala_Y;
                    delta_X=gps_delta_x;
                    delta_Y=gps_delta_y;

                    //si las deltas hacen salirse del mapa cargado, hay que que regenerar
                    if (delta_X<0 || delta_Y<0 || delta_X>=delta_X_max || delta_Y>delta_Y_max) {
                        regenerar_mapa(nivel_zoom,false);
                    }

                    
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
    private void regenerar_pantalla() {
        //coloca el mapa en pantalla, junto con el resto de elementos visibles
        g.drawRegion(lienzo,0+delta_X,0+delta_Y,pantalla_X,pantalla_Y,Sprite.TRANS_NONE,0,0,Graphics.TOP | Graphics.LEFT);
        colocar_elementos_auxiliares(g);
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
        g.setFont(fuente_pequeña);
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
            g.drawString("GPS ON",this.pantalla_X-5,4,Graphics.RIGHT | Graphics.TOP);
        }
        dibujar_escala(g,0.08f,0.3f);
        //si no hay mapas lo avisa en la línea de estado
        if (mapas==null) dibujar_linea_estado("No Map Data available.",fuente_mediana);
        if (configuracion.depuracion==true) {
            g.drawString(new Integer(nivel_zoom).toString(),10,10,0);
            g.drawString((new Long(tiempo).toString()),10,20,0);
        }
        if (configuracion.depuracion==true && mapas!=null) { //información adicional
            contador2=40; //coordenada y a partir de la cual se sigue escribiendo
            g.setColor(0);
            for (contador=0;contador<mapas.length;contador++) {
                if (mapas[contador]!=null) {
                    g.drawString(mapas[contador].nombre_archivo+"("+mapas[contador].descripcion+")",10,contador2,0);
                    contador2+=10;
                }
                
            }
        }
        
    }
    private void dibujar_escala(Graphics g,float x,float y) {
        //dibuja la escala en la coordenada adimensional x,y
        //el tamaño de la recta es 1/5 de la pantalla, se coloca en la vertical,
        //el extremo inferior en el punto marcado. los ribetes de la escala
        //son 1/50 de la pantalla
        String texto_escala;
        int origen_x, origen_y;
        texto_escala=leyenda_zoom[nivel_zoom];
        origen_x=(int)(x*this.pantalla_X);
        origen_y=(int)(y*this.pantalla_Y);
        g.setColor(0);
        g.setFont(fuente_pequeña);
        //línea principal
        g.drawLine(origen_x,origen_y,origen_x,origen_y-this.pantalla_Y/5);
        //ribetes
        g.drawLine(origen_x-this.pantalla_X/50,origen_y-this.pantalla_Y/5,origen_x+this.pantalla_Y/50,origen_y-this.pantalla_Y/5);
        g.drawLine(origen_x-this.pantalla_X/50,origen_y,origen_x+this.pantalla_Y/50,origen_y);
        g.drawString(texto_escala,origen_x-this.pantalla_X/25,origen_y+fuente_pequeña.getHeight()/2,Graphics.TOP|Graphics.LEFT);
    }
    private void dibujar_linea_estado (String texto,Font fuente) {
        //coloca el texto en la parte inferior de la pantalla, con un degradado para darle más contraste
        g.setColor(0);
        g.setFont(fuente);
        g.drawString(texto,this.pantalla_X/2,this.pantalla_Y-3,Graphics.BOTTOM|Graphics.HCENTER);
        flushGraphics();
        
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
        decimal=(int)((Math.abs(coordenada)-entero)*10000);
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
    private void regenerar_mapa(int nuevo_nivel_zoom,boolean forzar_regenerar) {
        //obtiene un nuevo mapa centrado en el punto indicado y prepara la pantalla para su visualización
        // a no ser que en el mapa actual quepa el recuadro solicitado y el nivel de detalle sea el mismo
        int contador;
        Tipo_Rectangulo limites;
        tiempo=System.currentTimeMillis();
        if (forzar_regenerar==true || mapa_nuevo_necesario(mapas,(float)centro_pantalla_Lon,(float)centro_pantalla_Lat,nuevo_nivel_zoom)==true) {
            this.tecla_0_pulsada=false; //evita que se vuelva loco al hacer zoom- cuando aparecen los mensajes de confirmación de permiso de lectura
            //antes de pedir el nuevo mapa libera la memoria ocupada por el anterior
            if (mapas!=null) { //puede ser un mapa nulo, como en el inicio
                for (contador=mapas.length-1;contador>=0;contador--) {
                    mapas[contador]=null;
                }
                System.gc();
            }
            dibujar_linea_estado("Generating Map...",fuente_pequeña);
            limites=centro_zoom_2_rectangulo((float)centro_pantalla_Lon,(float)centro_pantalla_Lat,nuevo_nivel_zoom);
            mapas=gestor_mapas.generar_mapa(limites,nuevo_nivel_zoom,proceso_etiquetas_NET);
        }
        this.definir_bordes_mapa((double)centro_pantalla_Lon,(double)centro_pantalla_Lat,nuevo_nivel_zoom);
        //la pantalla debe caer en el recuadro central
        this.delta_X=(int)((float)(configuracion.factor_mapa-1)*((float)pantalla_X/2)); //casting para tamaños de pantalla impares
        this.delta_Y=(int)((float)(configuracion.factor_mapa-1)*((float)pantalla_Y/2));
        dibujar_mapas(mapas);
        tiempo=System.currentTimeMillis()-tiempo;
        System.gc();
    }
    private boolean mapa_nuevo_necesario(Mapa_IMG [] mapas,float longitud,float latitud, int nuevo_nivel_zoom) {
        //devuelve true si el rectángulo y el nivel de detalle solicidados no caben en el mapa actual
        int contador;
        boolean encontrado=false;
        Tipo_Rectangulo nuevos_limites;
        if (nose==1) {
            return false;
        } else {
            nose=0;
        }
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
        if (rectangulo_interior_o_casi(mapas[contador].limites,nuevos_limites)==false) return true; //los nuevos límites no caben, hay que recalcular
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
                tecla_almohadilla_bloqueda=false;
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
    public boolean rectangulo_interior(Tipo_Rectangulo grande,Tipo_Rectangulo pequeño) {
        //devuelve true si el rectangulo pequeño está contenido en el grande
        if (grande.norte>=pequeño.norte && grande.sur <=pequeño.sur && grande.oeste <=pequeño.oeste && grande.este >=pequeño.este) {
            return true;
        } else return false;
    }
    public boolean rectangulo_interior_o_casi(Tipo_Rectangulo grande,Tipo_Rectangulo pequeño) {
        //devuelve true si el 90% del rectangulo pequeño está contenido en el grande
        double delta_altura;
        double delta_anchura;
        delta_altura=0.1*(pequeño.norte-pequeño.sur);
        delta_anchura=0.1*(pequeño.este-pequeño.oeste);
        if (grande.norte>=(pequeño.norte-delta_altura) && grande.sur <=(pequeño.sur+delta_altura) && grande.oeste <=(pequeño.oeste+delta_anchura) && grande.este >=(pequeño.este-delta_anchura)) {
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
    public void dibujar_punto_icono(Tipo_Punto punto,boolean indexado){
        //convierte las coordenadas lon/lat en puntos de pantalla, y dibuja el punto con el icono
        //que le corresponde
        int x,y;
        Tipo_Propiedades_Punto propiedades;
        propiedades=auxiliar.leer_propiedades_punto(punto.tipo);
        x=(int)((punto.longitud-scaleleft)*escala_X);
        y=(int)((scaletop-punto.latitud)*escala_Y);
        //graphics_lienzo.drawArc(x,y,5,5,0,360);
        dibujar_icono(x,y,propiedades.indice);
        if (punto.etiqueta!=null || punto.es_POI==true) { //si hay alguna etiqueta definida, ya sea normal o de POI
            if (indexado==true) {
                if (punto.es_POI==false) { //según sea POI o no, la etiqueta se guarda en un situo u otro
                    gestor_etiquetas.añadir_etiqueta(punto.etiqueta.nombre_completo,x+1,y+1,fuente_pequeña,gestor_etiquetas.TIPO_PUNTO_INDEXADO,0);
                } else if (punto.etiqueta_POI.etiquetas!=null) {
                    gestor_etiquetas.añadir_etiqueta(punto.etiqueta_POI.etiquetas[0].nombre_completo,x+1,y+1,fuente_pequeña,gestor_etiquetas.TIPO_PUNTO_INDEXADO,0);
                }
                
            } else {
                if (punto.es_POI==false) {
                    gestor_etiquetas.añadir_etiqueta(punto.etiqueta.nombre_completo,x+1,y+1,fuente_pequeña,gestor_etiquetas.TIPO_PUNTO,0);
                } else if (punto.etiqueta_POI.etiquetas!=null) {
                    gestor_etiquetas.añadir_etiqueta(punto.etiqueta_POI.etiquetas[0].nombre_completo,x+1,y+1,fuente_pequeña,gestor_etiquetas.TIPO_PUNTO,0);
                }
            }
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
        int indice_punto_central;
        int xmin=0,xmax=0,ymin=0,ymax=0; //límites de la polilínea, en píxeles
        int distancia=0; //suma de componentes vertical y horizontal de los segmentos que formal la polilinea
        String cadena="";
        //reserva espacio para las cooredenas en píxeles
        x=new int [polilinea.puntos_X.length];
        y=new int [polilinea.puntos_Y.length];
        if (polilinea.tipo==6){ //el bucle incluye cálculo de límites para ver si la etiqueta cabe
            for (contador=0; contador<polilinea.puntos_X.length;contador++) {
                x[contador] = (int)((polilinea.puntos_X[contador] - scaleleft) * escala_X);
                y[contador] = (int)((scaletop - polilinea.puntos_Y[contador]) * escala_Y);
                if (contador==0) { //inicializa las variables límites
                    xmin=x[0];
                    xmax=x[0];
                    ymin=y[0];
                    ymax=y[0];
                } else {
                    if (x[contador]<xmin ) xmin=x[contador];
                    if (x[contador]>xmax) xmax=x[contador];
                    if (y[contador]>ymax) ymax=y[contador];
                    if (y[contador]<ymin) ymin=y[contador];
                    distancia+=Math.abs(x[contador]-x[contador-1]);
                    distancia+=Math.abs(y[contador]-y[contador-1]);
                }
            }
        } else { //polilínea normal, el gestor de etiquetas se encarga de las interferencias
            for (contador=0; contador<polilinea.puntos_X.length;contador++) {
                x[contador] = (int)((polilinea.puntos_X[contador] - scaleleft) * escala_X);
                y[contador] = (int)((scaletop - polilinea.puntos_Y[contador]) * escala_Y);
            }
        }
        //dibuja la polilinea
        tipo=polilinea.tipo;
        graphics_lienzo.setColor(colores_polilineas[tipo]); //ajusta el color de la pluma
        if (tipo==0 | tipo==1 | tipo==2 | tipo==3) {
            PolygonGraphics.dibujar_polilinea2(graphics_lienzo,x,y);
        } else {
            PolygonGraphics.drawPolyline(graphics_lienzo,x,y);
        }
        
        indice_punto_central=x.length/2-1;
        //proceso de la etiqueta
        if (polilinea.etiqueta!=null) { //etiqueta en LBL
            graphics_lienzo.setColor(0x000000);
            cadena=polilinea.etiqueta.nombre_completo;
        } else if (polilinea.etiqueta_NET!=null ) { //etiquetas en NET
            if (polilinea.etiqueta_NET.etiquetas==null) return; //puede estar vacía
            
            //recorre las etiquetas y las suma
            //for (contador=polilinea.etiqueta_NET.etiquetas.length-1;contador>=0;contador--) {
            //    cadena=cadena+polilinea.etiqueta_NET.etiquetas[contador].nombre_completo+" ";
            //}
            cadena=polilinea.etiqueta_NET.etiquetas[0].nombre_corto;
        }
        graphics_lienzo.setColor(0x000000);
        //si es una calle, la marca. ### hay que añadir el ángulo
        if (polilinea.tipo==6) { //calle. la etiqueta sigue la dirección de la calle
            int anchura_texto,altura_texto; //tamaño del texto de la etiqueta
            int xcentro,ycentro; //punto en el que se va a dibujar la etiqueta
            int pendiente; //pendiente de la recta sobre la que se va a colocar la etiqueta, en %
            //comprobación previa de que la etiqueta cabe
            if ((xmax-xmin+ymax-ymin)<30) return; //en un recuadro tan pequeño no puede caber mucho
            altura_texto=fuente_pequeña.getHeight();
            anchura_texto=fuente_pequeña.stringWidth(cadena);
            if (distancia<(altura_texto+anchura_texto)*2) return; //segunda comprobación de que cabe
            xcentro=(x[indice_punto_central]+x[indice_punto_central+1])/2;
            ycentro=(y[indice_punto_central]+y[indice_punto_central+1])/2;
            if (x[indice_punto_central]==x[indice_punto_central+1]) { //línea vertical
                pendiente=999999; //infinito
            } else { //línea inclinada u horizontal
                pendiente=100*(y[indice_punto_central+1]-y[indice_punto_central])/(x[indice_punto_central+1]-x[indice_punto_central]);
            }
            gestor_etiquetas.añadir_etiqueta(cadena,xcentro,ycentro,fuente_pequeña,gestor_etiquetas.TIPO_CALLE,pendiente);
        } else { //caso general
            int xcentro,ycentro; //punto en el que se va a dibujar la etiqueta
            xcentro=(x[indice_punto_central]+x[indice_punto_central+1])/2;
            ycentro=(y[indice_punto_central]+y[indice_punto_central+1])/2;
            //gestor_etiquetas.añadir_etiqueta(cadena,xcentro,ycentro,fuente_pequeña,gestor_etiquetas.TIPO_POLILINEA,0);
            procesar_y_añadir_etiqueta_polilinea(polilinea.tipo,cadena,xcentro,ycentro,fuente_pequeña,gestor_etiquetas);
        }
    }
    private void procesar_y_añadir_etiqueta_polilinea(int tipo,String texto,int x,int y,Font fuente,Gestor_Etiquetas gestor_etiquetas) {
        //según el tipo de polilínea, las etiquetas se colocan de una u otra forma.el texto puede incluir
        //caracteres de control [0x2d], [0x2e] y [0x2f]
        //la polilínea de tipo calle se procesa en dibujar_polilinea, por ser un caso especial
        String cadena;
        if (texto.indexOf("[0x2e]")!=-1 || texto.indexOf("[0x2f]")!=-1) {//carretera general
            cadena=quitar_texto_cadena(texto,"[0x2e]");
            cadena=quitar_texto_cadena(cadena,"[0x2f]");
            gestor_etiquetas.añadir_etiqueta(cadena,x,y,fuente,gestor_etiquetas.TIPO_CARRETERA,0);
            return;
        }
        if (texto.indexOf("[0x2d")!=-1 ) {//autopista
            cadena=quitar_texto_cadena(texto,"[0x2d]");
            gestor_etiquetas.añadir_etiqueta(cadena,x,y,fuente,gestor_etiquetas.TIPO_AUTOPISTA,0);
            return;
        }
        //si se ha llegado hasta aquí, se trata de una polilínea general
        gestor_etiquetas.añadir_etiqueta(texto,x,y,fuente,gestor_etiquetas.TIPO_POLILINEA,0);
        
    }
    private String quitar_texto_cadena(String cadena,String texto) {
        //busca el texto indicado en la cadena dada y lo quita
        String resultado;
        int posicion;
        posicion=cadena.indexOf(texto);
        if (posicion==-1) return cadena; //el texto no aparece
        if (posicion==0) { //está al comienzo
            resultado=cadena.substring(6);
        } else {
            resultado=cadena.substring(0,posicion)+cadena.substring(posicion+6);
        }
        
        return resultado;
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
        int xmin=0,xmax=0,ymin=0,ymax=0;
        int contador;
        int nose=0;
        if (poligono.offset_etiqueta==6015) {
            nose=0;
        }
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
            if (contador==0) { //inicializa las variables límites
                xmin=x[0];
                xmax=x[0];
                ymin=y[0];
                ymax=y[0];
            } else {
                if (x[contador]<xmin ) xmin=x[contador];
                if (x[contador]>xmax) xmax=x[contador];
                if (y[contador]>ymax) ymax=y[contador];
                if (y[contador]<ymin) ymin=y[contador];
            }
        }
        //dibuja la polilinea
        PolygonGraphics.fillPolygon(graphics_lienzo,x,y);
        if (poligono.etiqueta!=null) { //etiqueta en LBL
            graphics_lienzo.setColor(0x000000);
            //coloca la etiqueta en el centro del área que ocupa el polígono
            //comprueba que la etiqueta cabe en el área ocupada por el polígono
            if (fuente_pequeña.stringWidth(poligono.etiqueta.nombre_completo)>(xmax-xmin)) return;
            //si todo está bien, añade la etiqueta a la lista a visualizar
            gestor_etiquetas.añadir_etiqueta(poligono.etiqueta.nombre_completo,(xmin+xmax)/2,(ymin+ymax)/2,fuente_pequeña,gestor_etiquetas.TIPO_POLIGONO,0);
        } else if (poligono.etiqueta_NET!=null) { //etiquetas en NET
            String cadena="";
            //recorre las etiquetas y las suma
            for (contador=poligono.etiqueta_NET.etiquetas.length-1;contador>=0;contador--) {
                cadena=cadena+poligono.etiqueta_NET.etiquetas[contador].nombre_completo+" ";
            }
            graphics_lienzo.setColor(0x000000);
            graphics_lienzo.drawString(cadena,x[0],y[0],0);
        }
        
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
        if (mapas==null) {
            return; //si no hay mapa, deja la pantalla en blanco y termina
        }
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
            System.gc();
            for (contador=0;contador<mapas[contador2].Polilineas.size();contador++) { //recorre los puntos
                dibujar_polilinea((Tipo_Poli)mapa.Polilineas.elementAt(contador));
            }
            System.gc();
            for (contador=0;contador<mapa.Puntos.size();contador++) { //recorre los puntos
                dibujar_punto_icono((Tipo_Punto)mapa.Puntos.elementAt(contador),false);
            }
            System.gc();
            graphics_lienzo.setColor(0x000000);
            
            for (contador_punto=1;contador_punto<auxiliar.numero_propiedades_puntos;contador_punto++){
                //recorre los puntos de menor a mayor índice. esto hace que las etiquetas de las ciudades
                //se coloquen dando prioridad a las de más habitantes
                tipo_punto=auxiliar.tipo[contador_punto]; //lee el tipo correspondiente
                //recorre todos los puntos en busca del tipo adecuado
                //for (contador=mapa.Puntos_Indexados.size()-1;contador>=0;contador--){
                for (contador=0;contador<mapa.Puntos_Indexados.size();contador++){  //se le da la vuelta para que el primer idioma en aparecer sea el adecuado
                    punto=(Tipo_Punto)mapa.Puntos_Indexados.elementAt(contador);
                    if (punto.tipo==tipo_punto) dibujar_punto_icono(punto,true);
                }
            }
            System.gc();
            //coloca las etiquetas
            gestor_etiquetas.dibujar_etiquetas(graphics_lienzo);
            System.gc();
        }
    }
}
