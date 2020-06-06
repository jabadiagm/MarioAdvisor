import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.*;
/*
 * IMG_Parser.java
 *
 * Created on 23 de noviembre de 2007, 19:38
 *
 * Intérprete de archivos IMG de Garmin
 */

/**
 *
 * @author javier
 */
public class IMG_Parser {
    public boolean mapa_general=false; //true cuando el nivel de zoom más bajo no es cero
    private String ruta_archivo; //nombre del archivo que contiene el mapa
    private FileConnection archivo_IMG;
    private InputStream stream_IMG;
    private String ultimo_error; //texto correspondiente al error detectado en la acción anterior
    private boolean reset_soportado; //indica que se puede resetear el puntero del archivo sin cerrar y abrir
    private boolean archivo_abierto; //indica que hay un mapa cargado
    private boolean cabecera_tre_procesada=false; //indica que la cabecera y los niveles de zoom ya han sido leídos
    private boolean tre_procesado; //indica que la información de las subdivisiones ya ha sido leída
    private boolean rgn_procesado; //indica que los punteros dentro del subarchivo RGN han sido definidos
    private boolean lbl_procesado; //indica que la cabecera del subarchivo LBL ha sido leída
    private int xor_byte;
    private int puntero; //posición actual dentro del archivo (byte)
    private int punto_reinicio; //posición a la que volverá el puntero de archivo al resetear
    public String descripcion_mapa; //descripción incluida en la cabecera
    private int tamaño_bloque; //bloque mínimo de bytes del archivo
    private int posicion_primer_subarchivo;
    private int [] potencias_2={1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608,16777216};
    private byte puntero_bit=7; //usado en la lectura de bits de las etiquetas
    private int buffer_lectura_bits; //para lectura de bits se usa un buffer que evita el avance y reseteo del puntero de bytes
    Tipo_Subarchivo tre,rgn,lbl,nod,net; //punteros de inicio, sectores de inicio/final, tamaño
    Tipo_TRE TRE;
    Tipo_RGN RGN;
    Tipo_LBL LBL;
    int nose;
    
    /** Creates a new instance of IMG_Parser */
    public IMG_Parser() {
        tre=new Tipo_Subarchivo();
        lbl=new Tipo_Subarchivo();
        rgn=new Tipo_Subarchivo();
        nod=new Tipo_Subarchivo();
        net=new Tipo_Subarchivo();
        TRE=new Tipo_TRE();
        LBL=new Tipo_LBL();
    }

    public String leer_ultimo_error(){
        return ultimo_error;
    }
    public int abrir_mapa(String ruta_mapa) {
        //intenta abrir el archivo especificado y leer su cabecera
        int e1,e2;
        int valor_int=1;
        byte fat_flag;
        String fat_nombre_subarchivo;
        String fat_tipo_subarchivo;
        int fat_tamaño_subarchivo;
        int fat_parte;
        int fat_bloque_inicio;
        int fat_bloque_final=0;
        int posicion_fat;
        String tipo_subarchivo_anterior="";
        
        try {
            archivo_IMG = (FileConnection) Connector.open(ruta_mapa,Connector.READ);
            if (!archivo_IMG.exists()) {
                ultimo_error="El archivo no existe";
                return 2;
            }
            stream_IMG=archivo_IMG.openInputStream();
            if (stream_IMG.markSupported()==false) {
                reset_soportado=false;
                //ultimo_error="No se permite el reinicio del puntero de archivo";
                //return 3;
            } else {
                reset_soportado=true; //esto permite mover el puntero hacia atrás mucho más rápido
            }
        } catch (IOException ex) {
            ultimo_error="Error de entrada/salida";
            return 1; //error de lectura
        }
        this.archivo_abierto=true;
        this.ruta_archivo=ruta_mapa;
        //marca el origen para poder hacer saltos
        this.ajustar_punto_reinicio();
        xor_byte=leer_byte_int();
        this.ajustar_puntero(0x49);
        this.descripcion_mapa=this.leer_cadena(20);
        this.avanzar_puntero(4);
        e1=this.leer_byte();
        e2=this.leer_byte();
        e2+=e1;
        while (e2!=0) { //calcula 2^(e1+e2) (jo!)
            valor_int*=2;
            e2--;
        }
        this.tamaño_bloque=valor_int;
        this.ajustar_puntero(0x400);
        //en los últimos archivos esta zona puede no estar en 0x400, sino más adelante
        while (leer_byte_int()==0) {
            this.avanzar_puntero(511);
        }
        this.avanzar_puntero(11);
        posicion_primer_subarchivo=leer_quad();
        this.avanzar_puntero(496); //se coloca en el principio de la FAT
        //procesa la FAT
        while (puntero<posicion_primer_subarchivo) {
            posicion_fat=puntero;
            fat_flag=leer_byte();
            fat_nombre_subarchivo=leer_cadena(8);
            fat_tipo_subarchivo=leer_cadena(3);
            fat_tamaño_subarchivo=leer_quad();
            this.avanzar_puntero(1);
            fat_parte=leer_palabra();
            this.avanzar_puntero(13);
            fat_bloque_inicio=leer_palabra();
            fat_bloque_final=leer_bloque_final();
            this.ajustar_puntero(posicion_fat+512); //salta a la siguiente entrada
            //comprueba que los subarchivos tienen una estructura continua.
            //la estructura de la FAT permite que un archivo tenga bloques discontinuos,
            //y si fuera así, se complicaría el parseo.
            if (fat_parte==0) { //guarda el nombre del archivo cuando empieza
                tipo_subarchivo_anterior=fat_tipo_subarchivo;
            } else { //a partir de la segunda y las siguientes partes se comprueba que no ha cambiado de tipo
                if (tipo_subarchivo_anterior.compareTo(fat_tipo_subarchivo)!=0) {
                    ultimo_error="Los subarchivos son discontinuos.";
                    return 4;
                }
            }
            //se rellena la información del subarchivo correspondiente
            if (fat_tipo_subarchivo.compareTo("TRE")==0) {
                procesar_bloque_fat(tre,fat_nombre_subarchivo,fat_tamaño_subarchivo,fat_bloque_inicio,fat_bloque_final);
            }else if (fat_tipo_subarchivo.compareTo("RGN")==0) {
                procesar_bloque_fat(rgn,fat_nombre_subarchivo,fat_tamaño_subarchivo,fat_bloque_inicio,fat_bloque_final);
            } else if (fat_tipo_subarchivo.compareTo("LBL")==0) {
                procesar_bloque_fat(lbl,fat_nombre_subarchivo,fat_tamaño_subarchivo,fat_bloque_inicio,fat_bloque_final);
            } else if (fat_tipo_subarchivo.compareTo("NOD")==0) {
                procesar_bloque_fat(nod,fat_nombre_subarchivo,fat_tamaño_subarchivo,fat_bloque_inicio,fat_bloque_final);
            } else if (fat_tipo_subarchivo.compareTo("NET")==0) {
                procesar_bloque_fat(net,fat_nombre_subarchivo,fat_tamaño_subarchivo,fat_bloque_inicio,fat_bloque_final);
            }
        }
        
        ultimo_error="";
        return 0; //archivo de mapa abierto y leído
    }
    private void procesar_bloque_fat(Tipo_Subarchivo subarchivo,String nombre,int tamaño,int bloque_inicial,int bloque_final) {
        //copia la información dada en el subarchivo indicado. si se indica un tamaño,
        //es un subarchivo nuevo
        int contador_final;
        int byte_inicio;
        int byte_final;
        byte_inicio = this.tamaño_bloque * bloque_inicial;
        byte_final = this.tamaño_bloque * (bloque_final + 1) - 1;
        contador_final = byte_final - byte_inicio;
        if (tamaño!=0) {
            subarchivo.tamaño=tamaño;
            subarchivo.nombre=nombre;
            subarchivo.puntero_inicio=byte_inicio;
            subarchivo.bloque_inicial=bloque_inicial;
        }
        subarchivo.bloque_final=bloque_final;
        subarchivo.tamaño_temporal+=contador_final+1;
        
    }
    public int cerrar_mapa(){
        if (archivo_abierto==false) {
            ultimo_error="No hay ningún mapa cargado.";
            return 1;
        }
        try {
            stream_IMG.close();
            archivo_IMG.close();
        } catch (IOException ex) {
            ultimo_error="Error de entrada/salida";
            return 2;
        }
        archivo_abierto=false;
        ultimo_error="";
        return 0; //archivo de mapa cerrado sin problemas
    }
    private int procesar_cabecera_tre(int detalle_general){
        //lee los límites del mapa actual y loz niveles de zoom
        //detalle_general es el valor a partir del cual se dibuja el plano general. 
        //si el mapa general tiene niveles con menor zoom, se ajustan para que no haya solapes
        
        int tamaño_cabecera;
        String cadena,cadena2;
        byte valor_byte;
        if (archivo_abierto==false) {
            ultimo_error="No hay ningún mapa cargado.";
            return 1;
        }
        //coloca el puntero en el comienzo del subarchivo TRE
        this.ajustar_puntero(tre.puntero_inicio);
        tamaño_cabecera=leer_palabra();
        cadena=leer_cadena(10);
        if (cadena.substring(0,6).compareTo("GARMIN")!=0) {
            ultimo_error="Error de parseo";
            return 2;
        }
        valor_byte=leer_byte();
        valor_byte=leer_byte();
        if (valor_byte==0x80) { //mapa bloqueado, no se puede leer
            ultimo_error="Archivo bloqueado.";
            return 3;
        }
        this.avanzar_puntero(7); //salto a los limites del mapa
        TRE.limite_norte=trio_2_latlon(leer_trio());
        TRE.limite_este=trio_2_latlon(leer_trio());
        TRE.limite_sur=trio_2_latlon(leer_trio());
        TRE.limite_oeste=trio_2_latlon(leer_trio());
        TRE.offset_niveles_TRE1=leer_quad();
        TRE.tamaño_niveles_TRE1=leer_quad();
        TRE.offset_subdivisiones_TRE2=leer_quad();
        TRE.tamaño_subdivisiones_TRE2=leer_quad();
        //asigna espacio para TRE1 y lo rellena
        TRE.TRE1 =new Tipo_Niveles_TRE1 [TRE.tamaño_niveles_TRE1/4];
        rellenar_TRE1(detalle_general);
        cabecera_tre_procesada=true;
        ultimo_error="";
        return 0;
    }
    public Tipo_Rectangulo leer_limites(int detalle_general){
        int valor_int;
        if (cabecera_tre_procesada==false) {
            valor_int=procesar_cabecera_tre(detalle_general);
            if (valor_int!=0) return null;
        }
        Tipo_Rectangulo frontera=new Tipo_Rectangulo(TRE.limite_norte,TRE.limite_sur,TRE.limite_este,TRE.limite_oeste);
        return frontera;
    }
    public int procesar_tre(int detalle_general){
        //carga la información de los niveles de zoom y las subdivisiones, 
        //leyendo el contenido del subarchivo tre
        //detalle_general es el valor a partir del cual se dibuja el plano general. 
        //si el mapa general tiene niveles con menor zoom, se ajustan para que no haya solapes
        int valor_int;
        if (cabecera_tre_procesada==false) {
            valor_int=procesar_cabecera_tre(detalle_general);
            if (valor_int!=0) return valor_int; //archivo no abierto ó error de acceso
        }
        //asigna espacio para TRE2
        TRE.TRE2=new Vector(TRE.tamaño_subdivisiones_TRE2/14); //tamaño aproximado (algo mayor) de la sección de subdivisiones
        rellenar_TRE2();
        tre_procesado=true;
        ultimo_error="";
        return 0;
    }
    void rellenar_TRE1(int detalle_general){
        //lee la zona de niveles de detalle, TRE1
        int contador;
        int numero_niveles;
        int subdivisiones_acumuladas=0; //suma de subdivisiones de los niveles anteriores
        int zoom_minimo=666; //si el nivel mínimo no es cero, supone mapa general
        Tipo_Niveles_TRE1 temporal;
        //coloca el puntero al comienzo de TRE1
        this.ajustar_puntero(tre.puntero_inicio+TRE.offset_niveles_TRE1);
        numero_niveles=TRE.tamaño_niveles_TRE1/4;
        for (contador=0;contador<numero_niveles;contador++) {
            temporal=new Tipo_Niveles_TRE1();
            TRE.TRE1[contador]=temporal;
            TRE.TRE1[contador].zoom=(byte)(leer_byte() & 0x0f);
            TRE.TRE1[contador].bits_coordenada=leer_byte();
            TRE.TRE1[contador].subdivisiones=leer_palabra();
            TRE.TRE1[contador].puntero_primera_subdivision=subdivisiones_acumuladas;
            subdivisiones_acumuladas+=TRE.TRE1[contador].subdivisiones;
            TRE.numero_subdivisiones=subdivisiones_acumuladas;
            if (TRE.TRE1[contador].zoom<zoom_minimo) zoom_minimo=TRE.TRE1[contador].zoom;
            //ajusta el zoom mínimo
            }
        if (zoom_minimo>0) { //mapa general
            this.mapa_general=true;
            if (zoom_minimo<detalle_general) { //hay que corregir los niveles de zoom
                for (contador=0;contador<numero_niveles;contador++) {
                    TRE.TRE1[contador].zoom+=detalle_general-zoom_minimo;
                }
            }
        }
        
    }
    void rellenar_TRE2(){ //procesa las subdivisiones
        int contador, contador2;
        byte nivel;
        byte n_bits;
        Tipo_Subdivisiones_TRE2 subdivision;
        byte valor_byte;
        float centro_longitud;
        float centro_latitud;
        float anchura; //ancho del mapa, en grados
        float altura;
        int valor_int;
        boolean maximo_nivel_con_geometria_asignado=false;
        this.ajustar_puntero(tre.puntero_inicio+TRE.offset_subdivisiones_TRE2);
        for (contador=0;contador<TRE.TRE1.length;contador++) { //recorre los niveles
            nivel=TRE.TRE1[contador].zoom;
            n_bits=TRE.TRE1[contador].bits_coordenada;
            for (contador2=1;contador2<=TRE.TRE1[contador].subdivisiones;contador2++) { //recorre las subdivisiones de cada nivel
                subdivision= new Tipo_Subdivisiones_TRE2();
                subdivision.puntero_RGN=leer_trio();
                valor_byte=leer_byte(); //objetos del mapa
                if (valor_byte!=0) { // hay geometría en esta subdivisión
                    if (maximo_nivel_con_geometria_asignado==false) { //define el nivel como el primero con geometría
                        maximo_nivel_con_geometria_asignado=true;
                        TRE.indice_maximo_nivel_con_geometria=contador;
                    }
                    subdivision.tipo_objetos=valor_byte;
                    if ((valor_byte & 0x10)!=0) subdivision.objetos_puntos=true;
                    if ((valor_byte & 0x20)!=0) subdivision.objetos_puntos_indexados=true;
                    if ((valor_byte & 0x40)!=0) subdivision.objetos_polilineas=true;
                    if ((valor_byte & 0x80)!=0) subdivision.objetos_poligonos=true;
                }
                centro_longitud=trio_2_latlon(leer_trio());
                centro_latitud=trio_2_latlon(leer_trio());
                valor_int=leer_palabra();
                if ((valor_int & 0x00008000)!=0) {
                    subdivision.flag_ultimo=true;
                    valor_int&=0x00007fff;
                }
                anchura=unidades_2_latlon(valor_int,n_bits);
                valor_int=leer_palabra();
                altura=unidades_2_latlon(valor_int,n_bits);
                //rellena los bordes de la subdivisión
                subdivision.centro_longitud=centro_longitud;
                subdivision.centro_latitud=centro_latitud;
                subdivision.limite_norte=centro_latitud+altura;
                subdivision.limite_sur=centro_latitud-altura;
                subdivision.limite_oeste=centro_longitud-anchura;
                subdivision.limite_este=centro_longitud+anchura;
                if (nivel!=TRE.TRE1[TRE.TRE1.length-1].zoom) { //el último nivel no tiene por qué ser el 0
                    subdivision.subdivision_siguiente_nivel=leer_palabra(); //información presente en todos los ni veles menos el último
                }
                TRE.TRE2.addElement(subdivision); //añade la información al vector de subdivisiones
                        
            }
          
        }
    }
    public int procesar_rgn() {
        //obtiene los punteros a las zonas de datos de todas las subdivisiones
        int valor_int;
        int offset_rgn;
        int contador;
        int contador2;
        int numero_secciones;
        int [] punteros=new int[5]; //punteros a las secciones de cada subdivisión
        int tamaño_rgn;
        Tipo_Subdivisiones_TRE2 subdivision;
        if (tre_procesado==false) { //no hay información de subdivisiones disponible
            ultimo_error="Falta el proceso previo del subarchivo TRE.";
            return 1;
        }
        RGN = new Tipo_RGN(TRE.numero_subdivisiones);
        this.ajustar_puntero(rgn.puntero_inicio+0x15);
        offset_rgn=leer_quad(); //offset dentro de RGN al comienzo de datos
        tamaño_rgn=leer_quad();
        for (contador=0;contador<TRE.numero_subdivisiones;contador++) {
            subdivision=(Tipo_Subdivisiones_TRE2) TRE.TRE2.elementAt(contador);
            //cuenta elnúmero de secciones que contiene la subdivisión
            numero_secciones=0;
            if (subdivision.objetos_puntos==true) numero_secciones++;
            if (subdivision.objetos_puntos_indexados==true) numero_secciones++;
            if (subdivision.objetos_polilineas==true) numero_secciones++;
            if (subdivision.objetos_poligonos==true) numero_secciones++;
            if (numero_secciones==0) continue; //no hay geometría, pasa a la siguiente subdivisión
            if (numero_secciones>1) {//si hay más de una sección, habrá que leer del subarchivo, así que hay que ubicar el puntero
                this.ajustar_puntero(rgn.puntero_inicio+offset_rgn+subdivision.puntero_RGN);
            }
            //lectura de los punteros
            punteros[0]=2*(numero_secciones-1); //no hay puntero a la primera sección
            for (contador2=1;contador2<numero_secciones;contador2++) {
                punteros[contador2]=leer_palabra();
            }
            //el último puntero se rellena con la posición de comienzo de la siguiente subdivisión.
            //si ya se está en la última, hay que usar el tamaño del subarchivo
            if (contador<(TRE.numero_subdivisiones-1)) {
                punteros[numero_secciones]=((Tipo_Subdivisiones_TRE2)TRE.TRE2.elementAt(contador+1)).puntero_RGN-subdivision.puntero_RGN;
            } else {
                //si se trata de la última sección, hay que restar al tamaño del subarchivo la posición de la última subdivisión
                punteros[numero_secciones]=tamaño_rgn-subdivision.puntero_RGN;
            }
            //los punteros se ajustan a posiciones absolutas dentro del archivo IMG
            for (contador2=0;contador2<=numero_secciones;contador2++) {
                punteros[contador2]+=rgn.puntero_inicio+offset_rgn+subdivision.puntero_RGN;
            }
            numero_secciones = 0; //se vuelve a utilizar la variable para asignar los punteros a sus zonas
            if (subdivision.objetos_puntos==true) {
                RGN.puntero_puntos[contador]=punteros[numero_secciones];
                RGN.final_puntos[contador]=punteros[numero_secciones+1]-1;
                numero_secciones++;
            }
             if (subdivision.objetos_puntos_indexados==true) {
                RGN.puntero_puntos_indexados[contador]=punteros[numero_secciones];
                RGN.final_puntos_indexados[contador]=punteros[numero_secciones+1]-1;
                numero_secciones++;
            }           
            if (subdivision.objetos_polilineas==true) {
                RGN.puntero_polilineas[contador]=punteros[numero_secciones];
                RGN.final_polilineas[contador]=punteros[numero_secciones+1]-1;
                numero_secciones++;
            }            
            if (subdivision.objetos_poligonos==true) {
                RGN.puntero_poligonos[contador]=punteros[numero_secciones];
                RGN.final_poligonos[contador]=punteros[numero_secciones+1]-1;
                numero_secciones++;
            }            
        }
        rgn_procesado=true;
        ultimo_error="";
        return 0;
    }
    private int procesar_cabecera_LBL() {
        int tamaño_cabecera;
        int valor_int;
        byte valor_byte;
        String cadena;
        if (archivo_abierto==false) {
            ultimo_error="No hay ningún mapa cargado.";
            return 1;
        }
        //coloca el puntero en el comienzo del subarchivo TRE
        this.ajustar_puntero(lbl.puntero_inicio);
        tamaño_cabecera=leer_palabra();
        cadena=leer_cadena(10);
        if (cadena.substring(0,6).compareTo("GARMIN")!=0) {
            ultimo_error="Error de parseo";
            return 2;
        }        
        valor_byte=leer_byte();
        valor_byte=leer_byte();
        if (valor_byte==0x80) { //mapa bloqueado, no se puede leer
            ultimo_error="Archivo bloqueado.";
            return 3;
        }
        this.avanzar_puntero(7); //salto al comienzo de los punteros de LBL
        LBL.LBL1_etiquetas_offset=leer_quad();
        LBL.LBL1_etiquetas_tamaño=leer_quad();
        LBL.LBL1_etiquetas_multiplicador_offset=leer_byte();
        LBL.formato_etiquetas=leer_byte();
        LBL.LBL2_paises_offset=leer_quad();
        LBL.LBL2_paises_tamaño=leer_quad();
        LBL.LBL2_paises_tamaño_registro=leer_palabra();
        valor_int=leer_quad(); //00 00 00 00
        LBL.LBL3_regiones_offset=leer_quad();
        LBL.LBL3_regiones_tamaño=leer_quad();
        LBL.LBL3_regiones_tamaño_registro=leer_palabra();
        valor_int=leer_quad();
        LBL.LBL4_ciudades_offset=leer_quad();
        LBL.LBL4_ciudades_tamaño=leer_quad();
        LBL.LBL4_ciudades_tamaño_registro=leer_palabra();
        valor_int=leer_quad();
        LBL.LBL5_POI_indices_offset=leer_quad();
        LBL.LBL5_POI_indices_tamaño=leer_quad();
        LBL.LBL5_POI_indices_tamaño_registro=leer_palabra();
        valor_int=leer_quad();
        LBL.LBL6_POI_propiedades_Offset=leer_quad();
        LBL.LBL6_POI_propiedades_tamaño=leer_quad();
        valor_byte=leer_byte();
        if (valor_byte!=0) LBL.LBL6_POI_propiedades_ZIP_Bit_Is_Phone_If_No_Phone_Bit=true;
        LBL.LBL6_POI_propiedades_mascara_global=leer_byte();
        valor_int=leer_palabra(); //00 00
        valor_byte=leer_byte(); //00
        LBL.LBL7_POI_tipos_Offset=leer_quad();
        LBL.LBL7_POI_tipos_tamaño=leer_quad();
        LBL.LBL7_POI_tipos_tamaño_registro=leer_palabra();
        valor_int=leer_quad();
        LBL.LBL8_ZIP_Offset=leer_quad();
        LBL.LBL8_ZIP_tamaño=leer_quad();
        LBL.LBL8_ZIP_tamaño_registro=leer_palabra();
        this.lbl_procesado=true;
        return 0;
    }
    
    public Mapa_IMG generar_mapa(Tipo_Rectangulo limites, byte nivel) {
        //devuelve todos los elementos gráficos contenidos en el rectángulo dado
        //y que pertenezcan el nivel de detalle indicado.
        //para ello busca intersecciones con todas las subdivisiones del nivel.
        int contador;
        int [] subdivisiones_visibles; //índices de las subdivisiones que intersectan con los límites
        int numero_subdivisiones_visibles=0;
        byte nivel_real; //si se pide un nivel no válido, se ajusta al máximo permitido
        int puntero_nivel;
        int max_subdiv; //última subdivisión del nivel a generar
        byte n_bits; //número de bits del nivel actual
        Tipo_Subdivisiones_TRE2 subdivision;
        if (rgn_procesado==false) return null; //el subarchivo rgn no ha sido leído
        Mapa_IMG mapa=new Mapa_IMG();
        nivel_real=corregir_nivel(nivel);
        //rellena los parámetros del mapa
        mapa.limites=limites;
        mapa.nivel_detalle=nivel_real;
        mapa.descripcion=descripcion_mapa;
        mapa.nombre_archivo=this.ruta_archivo;
        puntero_nivel=obtener_puntero_nivel(nivel_real);
        if (puntero_nivel<0) return null; //nivel inexistente
        n_bits=TRE.TRE1[puntero_nivel].bits_coordenada;
        subdivisiones_visibles=new int[TRE.TRE1[puntero_nivel].subdivisiones]; //como máximo pueden ser visibles todas las del nivel
        //recorre las subdivisiones del nivel
        max_subdiv=TRE.TRE1[puntero_nivel].puntero_primera_subdivision+TRE.TRE1[puntero_nivel].subdivisiones;
        for (contador=TRE.TRE1[puntero_nivel].puntero_primera_subdivision;contador<max_subdiv;contador++) {
            subdivision=(Tipo_Subdivisiones_TRE2) TRE.TRE2.elementAt(contador); //en el vector la primera subdivisión es la 0
            if (interseccion_subdivision_rectangulo(contador,limites)==true) { //área visible en la subdivisión
                subdivisiones_visibles[numero_subdivisiones_visibles]=contador; //guarda el número
                numero_subdivisiones_visibles++;
            }
            
        }
        if (numero_subdivisiones_visibles==0) return null; // no hay información visible con estos límites
        for (contador=0;contador<numero_subdivisiones_visibles;contador++) {
            subdivision=(Tipo_Subdivisiones_TRE2) TRE.TRE2.elementAt(subdivisiones_visibles[contador]); //en el vector la primera subdivisión es la 0
            if (subdivision.objetos_puntos==true) {
                //comienzo de la sección de puntos de la subdivisión
                this.ajustar_puntero(RGN.puntero_puntos[subdivisiones_visibles[contador]]);
                while (this.puntero<RGN.final_puntos[subdivisiones_visibles[contador]]) {
                    añadir_punto(false,mapa,n_bits,subdivision.centro_longitud,subdivision.centro_latitud);
                }
            }
            if (subdivision.objetos_puntos_indexados==true) {
                //comienzo de la sección de puntos indexados de la subdivisión
                this.ajustar_puntero(RGN.puntero_puntos_indexados[subdivisiones_visibles[contador]]);
                while (this.puntero<RGN.final_puntos_indexados[subdivisiones_visibles[contador]]) {
                    añadir_punto(true,mapa,n_bits,subdivision.centro_longitud,subdivision.centro_latitud);
                }
            }
            if (subdivision.objetos_polilineas==true) {
                //comienzo de la sección de polilíneas de la subdivisión
                this.ajustar_puntero(RGN.puntero_polilineas[subdivisiones_visibles[contador]]);
                while (this.puntero<RGN.final_polilineas[subdivisiones_visibles[contador]]) {
                    añadir_poli(false,mapa,n_bits,subdivision.centro_longitud,subdivision.centro_latitud);
                }
            }
            if (subdivision.objetos_poligonos==true) {
                //comienzo de la sección de polígonos de la subdivisión
                this.ajustar_puntero(RGN.puntero_poligonos[subdivisiones_visibles[contador]]);
                while (this.puntero<RGN.final_poligonos[subdivisiones_visibles[contador]]) {
                    añadir_poli(true,mapa,n_bits,subdivision.centro_longitud,subdivision.centro_latitud);
                }
            }
        }
        procesar_etiquetas_mapa(mapa); //rellena las etiquetas
        return mapa;
        
    }
    private void procesar_etiquetas_mapa(Mapa_IMG mapa) {
        int contador;
        int puntero_etiqueta_anterior=0;
        Tipo_Punto punto;
        Tipo_Etiqueta etiqueta;
        Vector lista_etiquetas=new Vector();
        //el acceso a la sección LBL del mapa debe hacerse evitando que el puntero vuelva
        //hacia atrás, así que es necesario ordenar los punteros de etiqueta de menor a mayor
        for (contador=mapa.Puntos.size()-1;contador>=0;contador--) { //primero, se recolectan los punteros
            punto=(Tipo_Punto)(mapa.Puntos.elementAt(contador));
            if (punto.es_POI==false ) {
                lista_etiquetas.addElement(punto); //añade el punto a la lista
            }
        }
        for (contador=mapa.Puntos_Indexados.size()-1;contador>=0;contador--) {
            punto=(Tipo_Punto)(mapa.Puntos_Indexados.elementAt(contador));
//            if (punto.es_POI==false && (punto.tipo<=0xc00 || mapa.nivel_detalle<2)) {
                lista_etiquetas.addElement(punto); //añade el punto a la lista
//            }
        }
        //ordena la lista de menor a mayor offset, para que el puntero de archivo no tenga que retroceder
        ordenar_lista_por_puntero_etiqueta(lista_etiquetas,0,lista_etiquetas.size()-1);
        //rellena las etiquetas
        for (contador=lista_etiquetas.size()-1;contador>=0;contador--) {
            punto=(Tipo_Punto)(lista_etiquetas.elementAt(contador));
            //comprueba antes que la etiqueta pedida no sea igual a la anterior
            if (puntero_etiqueta_anterior!=0 && puntero_etiqueta_anterior==punto.puntero_etiqueta)  { //en la primera iteración no se puede
                etiqueta=((Tipo_Punto)(lista_etiquetas.elementAt(contador+1))).etiqueta;
            } else {
                etiqueta=leer_etiqueta(punto.puntero_etiqueta);
            }
            
            punto.etiqueta=etiqueta;
            puntero_etiqueta_anterior=punto.puntero_etiqueta;
        }
        
    }
    private static void ordenar_lista_por_puntero_etiqueta(Vector src, int left, int right) {
        //aplica el quicksort a una lista de puntos, de forma que quede ordenada de mayor a menor
        if (right > left) {
            Tipo_Punto pivote = (Tipo_Punto)src.elementAt(right);
            Tipo_Punto nose;
            int i = left - 1;
            int j = right;
            while (true) {
                while (((Tipo_Punto)src.elementAt(++i)).puntero_etiqueta>pivote.puntero_etiqueta);
                while (j > 0)
                    if (((Tipo_Punto)src.elementAt(--j)).puntero_etiqueta>=pivote.puntero_etiqueta)
                        break;
                if (i >= j)
                    break;
                swap(src, i, j);
            }
            swap(src, i, right);
            ordenar_lista_por_puntero_etiqueta(src, left, i - 1);
            ordenar_lista_por_puntero_etiqueta(src, i + 1, right);
        }
    }
    private static void quickSort(Vector src, int left, int right) {
        if (right > left) {
            Tipo_Punto o1 = (Tipo_Punto)src.elementAt(right);
            Tipo_Punto nose;
            int i = left - 1;
            int j = right;
            while (true) {
                while (((Tipo_Punto)src.elementAt(++i)).puntero_etiqueta<o1.puntero_etiqueta);
                while (j > 0)
                    if (((Tipo_Punto)src.elementAt(--j)).puntero_etiqueta<=o1.puntero_etiqueta)
                        break;
                if (i >= j)
                    break;
                swap(src, i, j);
            }
            swap(src, i, right);
            quickSort(src, left, i - 1);
            quickSort(src, i + 1, right);
        }
    }

    private static void swap(Vector src, int loc1, int loc2)   {
        //intercambia dos elementos en un vector
        Object tmp = src.elementAt(loc1);
        src.setElementAt(src.elementAt(loc2), loc1);
        src.setElementAt(tmp, loc2);
    }

    private void añadir_punto(boolean indexado ,Mapa_IMG mapa,byte n_bits,float centro_longitud,float centro_latitud) {
        //toma el punto presente a partir del puntero actual del archivo y lo
        //añade al mapa dado. se usa la misma función para puntos normales
        //y para puntos indexados
        int tipo=0;
        int subtipo=0;
        int puntero_etiqueta;
        float longitud;
        float latitud;
        boolean es_POI=false;
        boolean tiene_subtipo=false;
        int valor_int;
        tipo=leer_byte();
        puntero_etiqueta=leer_trio();
        if ((puntero_etiqueta & 0x800000)!=0) tiene_subtipo=true; //bit de subtipo
        if ((puntero_etiqueta & 0x400000)!=0) es_POI=true; //bit de POI
        puntero_etiqueta &=0x3fffff;
        valor_int=leer_palabra();
        if (valor_int>=32768) valor_int-=65536; //corrige el signo negativo
        longitud=centro_longitud+unidades_2_latlon(valor_int,n_bits);
        valor_int=leer_palabra();
        if (valor_int>=32768) valor_int-=65536;
        latitud=centro_latitud+unidades_2_latlon(valor_int,n_bits);
        if (tiene_subtipo==true) subtipo=leer_byte();
        tipo=(tipo<<8)+subtipo;
        //añade el punto al mapa
        if (punto_interior(longitud,latitud,mapa.limites)==true) {
            if (indexado==false) {
                mapa.Puntos.addElement(new Tipo_Punto(tipo,es_POI,puntero_etiqueta,longitud,latitud));
            } else {
                mapa.Puntos_Indexados.addElement(new Tipo_Punto(tipo,es_POI,puntero_etiqueta,longitud,latitud));
            }
        }
    }
    private boolean punto_interior(float longitud,float latitud,Tipo_Rectangulo limites) {
        //devuelve true si el punto dado está contenido en los límites
        if (longitud>=limites.oeste && longitud<=limites.este && latitud>=limites.sur && latitud<=limites.norte) {
            return true;
        } else return false;
    }
//funciones de proceso de polilíneas y polígonos
    private void añadir_poli(boolean poligono,Mapa_IMG mapa,byte n_bits,float centro_longitud,float centro_latitud) {
        //añade al mapa la polilinea ó polígono que se encuentre en la posición actual del archivo
        int valor_int;
        int valor_int2;
        float longitud; //primer punto
        float latitud;
        float [] puntos_x;
        float [] puntos_y;
        boolean [] punto_es_nodo;
        boolean mismo_signo_longitud;
        boolean mismo_signo_latitud;  
        boolean signo_negativo_longitud;
        boolean signo_negativo_latitud;
        boolean longitud_2_bytes=false;
        boolean bit_extra=false;
        boolean signo_longitud=false;
        boolean signo_latitud=false;
        boolean parate;
        int[] puntero_bit=new int[1]; //para poder cambiar de valor en la llamada se usa un array
        int n_bits_longitud=0;
        int n_bits_latitud=0;
        int n_bits_cadena;
        int longitud_cadena; //cadena de puntos
        int bits_base_longitud;
        int bits_base_latitud;
        int tamaño_punto; //nº de bits de un punto (bit extra+longitud+latitud)
        int numero_puntos=0; //contador de puntos del polígono
        int contador;
        byte [] cadena_poli_byte;
        Tipo_Poli poli;
        poli=new Tipo_Poli();

        if (mapa.Poligonos.size()==12) {
            parate=true;
        }

        
        valor_int=leer_byte_int();
        if ((valor_int & 0x80)!=0) longitud_2_bytes=true;
        if (poligono==true) {
            poli.tipo=valor_int & 0x7f;
        } else {
            poli.tipo=valor_int & 0x3f;
            if ((valor_int & 0x40)!=0) poli.sentido_unico=true;
        }
        valor_int=leer_trio();
        if ((valor_int & 0x800000)!=0) poli.datos_en_NET=true;
        if ((valor_int & 0x400000)!=0) bit_extra=true;
        poli.offset_etiqueta=valor_int & 0x3fffff;
        valor_int=leer_palabra();
        if (valor_int>=32768) valor_int-=65536;
        longitud=centro_longitud+unidades_2_latlon(valor_int, n_bits);
        valor_int=leer_palabra();
        if (valor_int>=32768) valor_int-=65536;
        latitud=centro_latitud+unidades_2_latlon(valor_int, n_bits);
        if (longitud_2_bytes==true) {
            longitud_cadena=leer_palabra();
        } else {
            longitud_cadena=leer_byte_int();
        }
        valor_int=leer_byte_int();
        bits_base_longitud=valor_int & 0x0f;
        bits_base_latitud=(valor_int & 0xf0)>>4;
        cadena_poli_byte=leer_cadena_bytes(longitud_cadena);
        mismo_signo_longitud=leer_bit_cadena(cadena_poli_byte,puntero_bit);
        if (mismo_signo_longitud==true) {//mismo signo
            signo_longitud=leer_bit_cadena(cadena_poli_byte,puntero_bit);
        }
        mismo_signo_latitud=leer_bit_cadena(cadena_poli_byte,puntero_bit);
        if (mismo_signo_latitud==true) {//mismo signo
            signo_latitud=leer_bit_cadena(cadena_poli_byte,puntero_bit);
        }
        if (bits_base_longitud<=9) {
            n_bits_longitud=2+bits_base_longitud;
        } else {
            n_bits_longitud=2+2*bits_base_longitud-9;
        }
        if (mismo_signo_longitud==false) {
            n_bits_longitud++;
        } 
        if (bits_base_latitud<=9) {
            n_bits_latitud=2+bits_base_latitud;
        } else {
            n_bits_latitud=2+2*bits_base_latitud-9;
        }
        if (mismo_signo_latitud==false) {
            n_bits_latitud++;
        }
        tamaño_punto=n_bits_longitud+n_bits_latitud;
        if (bit_extra==true) tamaño_punto++;
        //ajuste holgado de los arrays con las coordenadas
        n_bits_cadena=cadena_poli_byte.length*8;
        puntos_x=new float [n_bits_cadena/tamaño_punto*2];
        puntos_y=new float [n_bits_cadena/tamaño_punto*2];
        punto_es_nodo=new boolean [n_bits_cadena/tamaño_punto*2];
        //el primer punto viene indicado en la estructura
        puntos_x[0]=longitud;
        puntos_y[0]=latitud;
        //parseo del resto de puntos



        
        while (n_bits_cadena>=(puntero_bit[0]+tamaño_punto)) {
            numero_puntos++;
            if (bit_extra==true) { //el bit extra es un bit antes de las deltas que indica si el punto es un nodo (routing)
                punto_es_nodo[numero_puntos]=leer_bit_cadena(cadena_poli_byte,puntero_bit);
            }
            if(numero_puntos==24) {
                parate=true;
            }
            valor_int=leer_delta(cadena_poli_byte,puntero_bit,n_bits_longitud,mismo_signo_longitud,signo_longitud);
            puntos_x[numero_puntos]=puntos_x[numero_puntos-1]+unidades_2_latlon(valor_int,n_bits);
            valor_int=leer_delta(cadena_poli_byte,puntero_bit,n_bits_latitud,mismo_signo_latitud,signo_latitud);
            puntos_y[numero_puntos]=puntos_y[numero_puntos-1]+unidades_2_latlon(valor_int,n_bits);
        }
        numero_puntos++;
        //hay que copiar el array de puntos al objeto poli
        poli.puntos_X=new float [numero_puntos];
        poli.puntos_Y=new float [numero_puntos];
        poli.punto_es_nodo=new boolean [numero_puntos];
        System.arraycopy(puntos_x,0,poli.puntos_X,0,numero_puntos);
        System.arraycopy(puntos_y,0,poli.puntos_Y,0,numero_puntos);
        System.arraycopy(punto_es_nodo,0,poli.punto_es_nodo,0,numero_puntos);
        //añade el elemento al mapa
        if (poligono==true) {
            mapa.Poligonos.addElement(poli);
        } else { //polilínea
            mapa.Polilineas.addElement(poli);
        }

        
    }
    private boolean leer_bit_cadena(byte [] cadena,int[] n_bit) {
        //devuelve el bit especificado de la cadena. el número se manda en un objeto para poder editarlo
        //el bit 0 es el LSB del primer byte y el último es el bit 7 del último byte
        int numero_byte;
        int valor_byte;
        int bit_local; //número de bit (0-7) del byte a leer
        boolean retorno;
        if (n_bit[0]>=8*cadena.length) {
            return false; //error
        }
        bit_local=n_bit[0]%8;
        numero_byte=(int)(n_bit[0]/8);
        valor_byte=cadena[numero_byte];
        if (( valor_byte & potencias_2[bit_local])!=0) {
            retorno= true;
        }
        else {
            retorno= false;
        }
        n_bit[0]++;
        return retorno;
    }
    private int leer_bits_cadena(byte [] cadena,int [] puntero_bit,int n_bits){
        //lee N_Bits de una cadena cuyo puntero actual es N_Bit
        int contador;
        int resultado=0;
        for (contador=0;contador<n_bits;contador++) {
            if (leer_bit_cadena(cadena,puntero_bit)==true) {
                resultado+=potencias_2[contador];
            }
        }
        return resultado;
    }
    private int leer_delta(byte [] cadena,int[]puntero_bit,int n_bits,boolean mismo_signo,boolean signo){
        //lee una delta de longitud o latitud de una cadena, procesando el signo
        int valor_int;
        int resultado;
        valor_int=leer_bits_cadena(cadena,puntero_bit,n_bits);
        if (mismo_signo==true) {
            if (signo==true) { //signo negativo
                resultado=-valor_int;
            } else {
                resultado=valor_int;
            }
        } else {
            //hay tres posibilidades:
            //011: el bit más significativo es cero. 2^(3-1)=4. 3<4. por tanto, es positivo
            //111: el bit más significativo es uno, el resto es no nulo. 2^(3-1)=4. 7>4. por tanto, es negativo. 7-2*4=-1
            //100: el bit más significativo es uno, el resto es nulo. se hace 1-4+siguiente valor. se hace
            //una llamada recursiva, porque el siguiente también puede ser 1000
            if (valor_int<(potencias_2[n_bits-1])) { //signo positivo
                resultado=valor_int;
            } else if (valor_int>potencias_2[n_bits-1]) { //signo negativo con resto no nulo
                resultado=valor_int-2*potencias_2[n_bits-1];
            } else { //signo negativo con resto nulo. llamada recursiva
                resultado=leer_delta(cadena,puntero_bit,n_bits,mismo_signo,signo);
                if (resultado>0) {
                    resultado=valor_int-1+resultado;
                } else {
                    resultado=resultado-valor_int+1;
                }
            }
        }
        return resultado;
    }

//funciones de obtención de niveles    
    private byte corregir_nivel(byte nivel){
        //dado un nivel, devuelve el valor más cercano que tenga geometría
        byte nuevo_nivel;
        byte nivel_maximo;
        int contador;
        nivel_maximo=TRE.TRE1[TRE.indice_maximo_nivel_con_geometria].zoom;
        if (nivel>nivel_maximo) nuevo_nivel=nivel_maximo; //el nivel pedido no existe o no tiene geometría
        else nuevo_nivel=nivel; //nivel válido
        return nuevo_nivel;
    }
    private int obtener_puntero_nivel(byte nivel) {
        //devuelve el puntero correspondiente al nivel pedido. normalmente,
        //el orden es: nivel3->0,nivel2->1,nivel1->2,nivel0->3
        int contador;
        if (nivel>TRE.TRE1[0].zoom) return 0; //si se pide un nivel que no existe devuelve el máximo
        for (contador=0;contador<TRE.TRE1.length;contador++){
            if (nivel==TRE.TRE1[contador].zoom) return contador;
        }
        return -1; //error extraño
    }
//funciones para la lectura de etiquetas    
    private Tipo_Etiqueta leer_etiqueta (int offset) {
        int offset_absoluto; //posición dentro del archivo IMG
        int retorno;
        int puntero_etiqueta;
        Tipo_Etiqueta etiqueta=null;
        if (lbl_procesado==false) {
            retorno=procesar_cabecera_LBL();
            if (retorno!=0) return null; //error en la apertura de LBL
        }
        puntero_etiqueta=lbl.puntero_inicio+LBL.LBL1_etiquetas_offset+offset* potencias_2[LBL.LBL1_etiquetas_multiplicador_offset];
        this.ajustar_puntero(puntero_etiqueta);
        if (LBL.formato_etiquetas==6) { //etiqueta de 6 bits
            etiqueta=leer_etiqueta_6();
        } else if (LBL.formato_etiquetas==9) { //etiqueta de 8 bits
            etiqueta=leer_etiqueta_8();
        }
        return etiqueta;
    }
    private Tipo_Etiqueta leer_etiqueta_6(){
        //lee una etiqueta con caracteres de 6 bits
        String caracteres_mayusculas= " ABCDEFGHIJKLMNOPQRSTUVWXYZ~~~~~0123456789~~~~~~";
        String caracteres_simbolos= "@!\"#$%&'()*+,-./~~~~~~~~~~:;<=>?~~~~~~~~~~~[\\]^_";
        String caracteres_minusculas="`abcdefghijklmnopqrstuvwxyz~~~~~0123456789~~~~~~";
        String cadena;
        int caracter;
        int contador;
        boolean minuscula=false;
        boolean simbolo=false;
        boolean abreviatura=false;  //si pasa a True, se escribe todo lo que queda en la abreviatura
        Tipo_Etiqueta etiqueta=new Tipo_Etiqueta("","","");
        while (true) {
            caracter=leer_6_bits_IMG();
            if (caracter>0x2f) { //fin de etiqueta
                puntero_bit=7;
                break;
                }
            if (caracter==0x1c) { //el siguiente carácter es un símbolo
                caracter=leer_bits_IMG(6);
                simbolo=true;
            }
            if (caracter==0x1b) { //el siguiente carácter está en minúscula
                caracter=leer_bits_IMG(6);
                minuscula=true;
            }
            if (minuscula==true) {
                cadena=caracteres_minusculas.substring(caracter,caracter+1);
                minuscula=false;
            } else if (simbolo==true) {
                cadena=caracteres_simbolos.substring(caracter,caracter+1);
                simbolo=false;
            } else {
                if (caracter>=0x2a && caracter<=0x2f) { //símbolo de autopista
                    cadena="[0x"+Integer.toHexString(caracter);
                } else {
                    cadena=caracteres_mayusculas.substring(caracter,caracter+1);
                }
            }
            if (caracter==0x1d)  {//todo lo que sigue, es abreviatura
                abreviatura=true;
            } else if (caracter==0x1e) { //cortar en vista de GPS todo lo que había antes
                etiqueta.nombre_completo+= " ";
                etiqueta.nombre_corto="";
            } else if (caracter==0x1f) { //cortar en vista de GPS todo lo que venga después
                etiqueta.nombre_completo+=" ";
                etiqueta.nombre_corto+=String.valueOf(254); //marca el carácter a partir del cual cortar luego
            } else {
                if (abreviatura==false) {
                    etiqueta.nombre_completo+=cadena;
                    etiqueta.nombre_corto+=cadena;
                } else {
                    etiqueta.abreviatura+=cadena;
                }
            }
        }
        contador=etiqueta.nombre_corto.indexOf(254);
        if (contador!=-1) { //existe el carácter de corte
            etiqueta.nombre_corto=etiqueta.nombre_corto.substring(contador+1);
        }
        etiqueta.nombre_completo=etiqueta.nombre_completo.toLowerCase();
        return etiqueta;
    }
    private Tipo_Etiqueta leer_etiqueta_8(){
        //lee una etiqueta con caracteres de 6 bits
        String caracteres_mayusculas= " ABCDEFGHIJKLMNOPQRSTUVWXYZ~~~~~0123456789~~~~~~";
        String caracteres_simbolos= "@!\"#$%&'()*+,-./~~~~~~~~~~:;<=>?~~~~~~~~~~~[\\]^_";
        String caracteres_minusculas="`abcdefghijklmnopqrstuvwxyz~~~~~0123456789~~~~~~";
        String cadena;
        int caracter;
        int contador;
        boolean minuscula=false;
        boolean simbolo=false;
        boolean abreviatura=false;  //si pasa a True, se escribe todo lo que queda en la abreviatura
        Tipo_Etiqueta etiqueta=new Tipo_Etiqueta("","","");
        while (true) {
            caracter=leer_byte_int();
            if (caracter==0) { //fin de etiqueta
                break;
            }
            cadena=new Character((char)caracter).toString();
            if (caracter==0x1d)  {//todo lo que sigue, es abreviatura
                abreviatura=true;
            } else if (caracter==0x1e) { //cortar en vista de GPS todo lo que había antes
                etiqueta.nombre_completo+= " ";
                etiqueta.nombre_corto="";
            } else if (caracter==0x1f) { //cortar en vista de GPS todo lo que venga después
                etiqueta.nombre_completo+=" ";
                etiqueta.nombre_corto+=String.valueOf(254); //marca el carácter a partir del cual cortar luego
            } else {
                if (abreviatura==false) {
                    etiqueta.nombre_completo+=cadena;
                    etiqueta.nombre_corto+=cadena;
                } else {
                    etiqueta.abreviatura+=cadena;
                }
            }
        }
        contador=etiqueta.nombre_corto.indexOf(254);
        if (contador!=-1) { //existe el carácter de corte
            etiqueta.nombre_corto=etiqueta.nombre_corto.substring(contador+1);
        }
        etiqueta.nombre_completo=etiqueta.nombre_completo.toLowerCase();
        return etiqueta;
    }    
    
//funciones de lectura de los bytes del mapa    
    private byte leer_byte() {
      byte[] b=new byte[1];
      try {
          stream_IMG.read(b);
          b[0] =(byte)((int)b[0] ^ xor_byte);
          puntero++;
          return (b[0]);
      } catch (IOException ex) {
          ex.printStackTrace();
          return 0;
      }
  }
    private int leer_byte_int() { //devuelve un entero, para que el signo del byte no incordie
      byte[] b=new byte[1];
      try {
          stream_IMG.read(b);
          b[0] =(byte)((int)b[0] ^ xor_byte);
          puntero++;
          return (int)b[0]& 0x000000ff; //la versión entera de un byte negativo es 0xffffff**
      } catch (IOException ex) {
          ex.printStackTrace();
          return 0;
      }
  }
  private int leer_palabra() {
      int byte1,byte2;
      byte1=leer_byte_int();
      byte2=leer_byte_int()<<8;
      return byte1 | byte2;
  }
  private int leer_trio() {
      int byte1,byte2,byte3;
      byte1=leer_byte_int();
      byte2=leer_byte_int()<<8;
      byte3=leer_byte_int()<<16;
      return byte1 | byte2 | byte3;
  }
    private int leer_quad() {
      int byte1,byte2,byte3,byte4;
      byte1=leer_byte_int();
      byte2=leer_byte_int()<<8;
      byte3=leer_byte_int()<<16;
      byte4=leer_byte_int()<<24;
      return byte1 | byte2|byte3|byte4;
  }
    
  private String leer_cadena(int len_cadena){
      byte[] b=new byte[len_cadena];
      int contador;
      try {
          stream_IMG.read(b);
      } catch (IOException ex) {
          ex.printStackTrace();
      }
      for (contador=0;contador<len_cadena;contador++){
          b[contador]=(byte)((int)b[contador] ^ xor_byte);
      }
      puntero+=len_cadena;
      return new String(b);
  }
  private byte[] leer_cadena_bytes(int len_cadena){
      byte[] b=new byte[len_cadena];
      int contador;
      try {
          stream_IMG.read(b);
      } catch (IOException ex) {
          ex.printStackTrace();
      }
      for (contador=0;contador<len_cadena;contador++){
          b[contador]=(byte)((int)b[contador] ^ xor_byte);
      }
      puntero+=len_cadena;
      return b;
  }
//funciones de lectura de bits del archivo IMG
  private boolean leer_bit_IMG(){
      //lee un bit del byte actual en el archivo IMG
      boolean retorno;
      if (puntero_bit==7) { //el byte actual no ha sido leído. se lee y se guarda en el buffer
          buffer_lectura_bits=leer_byte_int();
      }
      if (( buffer_lectura_bits & potencias_2[puntero_bit])!=0) {
          retorno= true;
      } else {
          retorno= false;
      }
      if (puntero_bit==0) { //se ha leído el último bit, el puntero ha avanzado al siguiente byte...
          puntero_bit=7; //...así que se pasa al siguiente MSB
      } else {
          puntero_bit--;
      }
      return retorno;
  }
  private int leer_bits_IMG(int n_bits) {
      //lee n_bits del IMG abierto
      int contador;
      int resultado=0;
      for (contador=0;contador<n_bits;contador++) {
          if (leer_bit_IMG()==true) {
              resultado=resultado*2+1;
          } else {
              resultado*=2;
          }
      }
      return resultado;
  }
  private int leer_6_bits_IMG() {
      //lee 6 bits del IMG abierto, a no ser que los dos primeros sean dos 1's. como eso significa
      //fin de etiqueta, se evita así cambiar de byte, lo que oligaría a hacer retroceder el puntero
      //cuando haya que leer le etiqueta contigua
      int contador;
      int resultado=0;
      for (contador=0;contador<6;contador++) {
          if (leer_bit_IMG()==true) {
              resultado=resultado*2+1;
          } else {
              resultado*=2;
          }          
          if (contador==1 && resultado==3) { //llevamos dos 1's. fin de etiqueta
              return 0x30;
          }
      }
      return resultado;
  }
  
//funciones de manejo del puntero de archivo
  private void ajustar_punto_reinicio() {
      //ajusta la posición actual, para poder volver al resetear el punto de inicio
      punto_reinicio=puntero;
      try {
          stream_IMG.mark((int)archivo_IMG.fileSize());
      } catch (IOException ex) {
          ex.printStackTrace();
      }
  }
  private void reset_punto_inicio() {
      if (reset_soportado==true) {
          try {
              //vuelve al punto de reinicio fijado
              stream_IMG.reset();
          } catch (IOException ex) {
              ex.printStackTrace();
          }
          puntero=punto_reinicio;
      } else {
            try {
                stream_IMG.close();
                archivo_IMG.close();
                archivo_IMG = (FileConnection) Connector.open(ruta_archivo,Connector.READ);
                stream_IMG=archivo_IMG.openInputStream();
                puntero=0;
                
            } catch (IOException ex) {
                ex.printStackTrace();
            }
      }
  }
  private void avanzar_puntero(int bytes_salto) {
      try {
          //avanza el número de bytes indicado
          stream_IMG.skip(bytes_salto);
          puntero+=bytes_salto;
      } catch (IOException ex) {
          ex.printStackTrace();
      }
  }
  private void ajustar_puntero(int nuevo_puntero) {
      //ajusta la posición de lectura del mapa. si la posición está por delante,
      //se salta la diferencia. si está detrás, hay que resetear y saltar
      if (nuevo_puntero>puntero) {//hay que avanzar
          avanzar_puntero(nuevo_puntero-puntero);
      } else if (nuevo_puntero<puntero) { //hay que resetear y volver a avanzar
          if (nuevo_puntero>punto_reinicio) {
              reset_punto_inicio();
              avanzar_puntero(nuevo_puntero-puntero);
          } else if (nuevo_puntero<punto_reinicio) {
              try {
                  throw new IOException("Retroceso todavía no implementado"); //hay que cerrar y volver a abrir
              } catch (IOException ex) {
                  ex.printStackTrace();
              } //hay que cerrar y volver a abrir
          } else { //nuevo_puntero=punto_reinicio
              reset_punto_inicio();
          }
      } else { //nuevo_puntero=puntero, no hay que hacer nada
          return;
      }
  }
  private int leer_bloque_final() {
      //devuelve el valor del último bloque indicado en la FAT, justo antes de FF FF
      int anterior1=0;
      int anterior2=0;
      int actual1,actual2;
      int contador=0; //dentro de una FAT puede haber 480 bytes de bloques, sin terminar en FF FF
      while (true) {
          actual1 = leer_byte_int();
          actual2 = leer_byte_int();
          contador++;
          if ((actual1 == 255 & actual2 == 255) | contador == 239) {
              break;
          }
          anterior1=actual1;
          anterior2=actual2;
      }
      if ((actual1 != 255 | actual2 != 255) & contador == 239) { //se llega al límite y además no hay marca de fin de bloque
          return (actual1|actual2<<8);
      } else //no se llega al límite, o si se llega, hay una marca de fin
          return (anterior1|anterior2<<8);
  }
  private float trio_2_latlon(int trio) {
    //convierte una coordenada en tres bytes a latitud/longitud
      double aux;
      final int dos_exp_24=16777216; //2^24
      aux=(double)360/(double)dos_exp_24*(double)trio;
      if (aux>180) {
          aux-=360;
      }
      return (float)aux;
  }
  private float unidades_2_latlon(int unidades_mapa, byte n_bits){
      //convierte las unidades de mapa en grados de longitud/latitud
      double aux;
      aux=(double)(unidades_mapa*360)/(double)(potencias_2[n_bits]);
      if (aux>180) aux-=360;
      return ((float)aux);
  }
  private boolean interseccion_subdivision_rectangulo(int n_subdiv,Tipo_Rectangulo rectangulo){
      //devuelve true si hay interferencia entre el rectángulo dado y
      //los límites de la subdivisión dada.
      boolean contenido_X=false;
      boolean contenido_Y=false;
      Tipo_Subdivisiones_TRE2 subdiv;
      subdiv=(Tipo_Subdivisiones_TRE2)TRE.TRE2.elementAt(n_subdiv);
      if ((rectangulo.oeste>=subdiv.limite_oeste && rectangulo.oeste<=subdiv.limite_este) || 
              (rectangulo.este>=subdiv.limite_oeste && rectangulo.este<=subdiv.limite_este) ||
              (subdiv.limite_oeste>=rectangulo.oeste && subdiv.limite_oeste<=rectangulo.este) ||
              (subdiv.limite_este>=rectangulo.oeste && subdiv.limite_este<=rectangulo.este)) contenido_X=true;
      if (contenido_X==false) return false;
      if ((rectangulo.sur>=subdiv.limite_sur && rectangulo.sur<=subdiv.limite_norte) || 
              (rectangulo.norte>=subdiv.limite_sur && rectangulo.norte<=subdiv.limite_norte) ||
              (subdiv.limite_sur>=rectangulo.sur && subdiv.limite_sur<=rectangulo.norte) ||
              (subdiv.limite_norte>=rectangulo.sur && subdiv.limite_norte<=rectangulo.norte)) contenido_Y=true;      
      if (contenido_Y==true) return true;
      return false;
  }
  //estructuras de subarchivos y subestructuras
  private class Tipo_Subarchivo { //clase interna para manejo de los subarchivos
      int bloque_inicial;
      int bloque_final;
      int puntero_inicio;
      int tamaño;
      int tamaño_temporal; //puede ser útil en depuración
      String nombre;
  }
  private class Tipo_TRE { //estructura básica de un subarchivo TRE, que contiene TRE1 y TRE2
      float limite_norte;
      float limite_este;
      float limite_sur;
      float limite_oeste;
      int offset_niveles_TRE1;
      int tamaño_niveles_TRE1;
      int offset_subdivisiones_TRE2;
      int tamaño_subdivisiones_TRE2;
      int numero_subdivisiones;
      Tipo_Niveles_TRE1 [] TRE1;
      int indice_maximo_nivel_con_geometria; //puntero al nivel de menos detalle que tiene geometría
      Vector TRE2;
  }
  private class Tipo_RGN { //punteros a las áreas de datos de cada subdivisión
      int [] puntero_puntos; //posición absoluta dentro del archivo IMG
      int [] final_puntos; //último byte del área
      int [] puntero_puntos_indexados;
      int [] final_puntos_indexados;
      int [] puntero_polilineas;
      int [] final_polilineas;
      int [] puntero_poligonos;
      int [] final_poligonos;
      public Tipo_RGN(int numero_subdivisiones) { //el constructor reseva sitio para los arrays
          puntero_puntos=new int[numero_subdivisiones];
          final_puntos=new int[numero_subdivisiones];
          puntero_puntos_indexados=new int[numero_subdivisiones];
          final_puntos_indexados=new int[numero_subdivisiones];
          puntero_polilineas=new int[numero_subdivisiones];
          final_polilineas=new int[numero_subdivisiones];
          puntero_poligonos=new int[numero_subdivisiones];
          final_poligonos=new int[numero_subdivisiones];
      }
  }
  private class Tipo_Niveles_TRE1{ //definición de los niveles de detalle y su precisión
      byte zoom;
      byte bits_coordenada;
      int subdivisiones;
      int puntero_primera_subdivision; //primera subdivisión del nivel
  }
  private class Tipo_Subdivisiones_TRE2 { //secciones que forman el mapa
      int puntero_RGN;
      byte tipo_objetos;
      boolean objetos_puntos;
      boolean objetos_puntos_indexados;
      boolean objetos_polilineas;
      boolean objetos_poligonos;
      float limite_norte;
      float limite_este;
      float limite_sur;
      float limite_oeste;
      float centro_longitud;
      float centro_latitud;
      boolean flag_ultimo;
      int subdivision_siguiente_nivel;
  }
  private class Tipo_LBL { //definición de la cabecera de un subarchivo LBL
    int LBL1_etiquetas_offset;
    int LBL1_etiquetas_tamaño;
    int LBL1_etiquetas_multiplicador_offset;
    byte formato_etiquetas;
    int LBL2_paises_offset;
    int LBL2_paises_tamaño;
    int LBL2_paises_tamaño_registro;
    int LBL3_regiones_offset;
    int LBL3_regiones_tamaño;
    int LBL3_regiones_tamaño_registro;
    int LBL4_ciudades_offset;
    int LBL4_ciudades_tamaño;
    int LBL4_ciudades_tamaño_registro;
    int LBL5_POI_indices_offset;
    int LBL5_POI_indices_tamaño;
    int LBL5_POI_indices_tamaño_registro;
    int LBL6_POI_propiedades_Offset;
    int LBL6_POI_propiedades_tamaño;
    boolean LBL6_POI_propiedades_ZIP_Bit_Is_Phone_If_No_Phone_Bit;
    byte LBL6_POI_propiedades_mascara_global;
    int LBL7_POI_tipos_Offset;
    int LBL7_POI_tipos_tamaño;
    int LBL7_POI_tipos_tamaño_registro;
    int LBL8_ZIP_Offset;
    int LBL8_ZIP_tamaño;
    int LBL8_ZIP_tamaño_registro;
  }

}
