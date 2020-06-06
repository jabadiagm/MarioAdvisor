import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/*
 * Gestor_Mapas.java
 *
 * Created on 27 de diciembre de 2007, 17:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public  class Gestor_Mapas {
    //valores permitidos de zoom, en metros. un valor de zoom representa la quinta parte de la latitud visible en el mapa
    private int [] zoom={12,20,30,50,80,120,200,300,500,800,1200,2000,3000,5000,8000,12000,20000,30000,50000,80000,120000,200000,300000,500000,800000};
    //nivel de detalle recomendado para cada nivel de zoom (cuanto más bajo, más detalle se permite)
    private int [] detalle={0,0,0,0,0,0,0,0,1,1,1,2,2,3,3,4,4,4,5,5,5,5,5,6,6,7};
    private int detalle_minimo_mapa_general; //nivel a partir del cual sólo dibuja mapas genéricos
    private IMG_Parser archivo_IMG;
    private String [] lista_mapas;
    private Tipo_Rectangulo [] limites_mapas;
    private boolean [] mapa_general; //true cuando hay un mapa general. si el zoom pedido es superior al mínimo, es el único que muestra
    private boolean mapa_general_presente;
    private boolean gestion_mapa_general_permitida=true;
    private FileConnection carpeta_archivos;
    private Cache_IMG cache;
    public int estado; //indica si está o no dispuesto a funcionar estado=0 : OK

    
    /** Creates a new instance of Gestor_Mapas */
    public Gestor_Mapas(String ruta_archivos,Graphics grafico,IMG_Canvas img_canvas,int detalle_minimo) {
        //rastrea la ruta indicada buscando archivos IMG. hace una lista con los presentes y
        //obtiene los límites de cada uno de ellos, para saber cuáles debe utilizar al
        //navegar.
        String ruta_mapas;
        Enumeration retorno_dir;
        String nombre_plataforma; //nombre de la máquina corriendo java
        int contador;
        int retorno;
        int ancho_pantalla; 
        int alto_pantalla;
        Image logo;
        Vector vector_temporal=new Vector(); //buffer temporal para almecenar el resultado del DIR
        cache=new Cache_IMG(); //lista de archivos abiertos listos para generar mapas
        try {
            /*
            nombre_plataforma=System.getProperty("microedition.platform");
            if (nombre_plataforma.compareTo("SunMicrosystems_wtk")==0) { //emulador
                ruta_mapas="file:///root1/";
            } else { //k610i
                ruta_mapas="file:///e:/temp/";
            }*/
            detalle_minimo_mapa_general=detalle_minimo;
            if (detalle_minimo<0) { //no hay gestión de mapa general
                gestion_mapa_general_permitida=false;
            }
            ruta_mapas=ruta_archivos;
            
            carpeta_archivos=(FileConnection)Connector.open(ruta_mapas,Connector.READ);
            retorno_dir=carpeta_archivos.list("*.img",false);
            while (retorno_dir.hasMoreElements()==true) {
                vector_temporal.addElement(retorno_dir.nextElement());
            }
            if (vector_temporal.size()==0) {
                estado=2; //no se han encontrado archivos de mapa
            } else {
                lista_mapas= new String[vector_temporal.size()];
                limites_mapas=new Tipo_Rectangulo[vector_temporal.size()];
                mapa_general=new boolean [vector_temporal.size()];
                ancho_pantalla=img_canvas.getWidth();
                alto_pantalla=img_canvas.getHeight();
                grafico.setColor(0,0,0);
                grafico.fillRect(0,0,ancho_pantalla,alto_pantalla);
                logo=Image.createImage("/logo.png");
                grafico.drawImage(logo,ancho_pantalla/2,alto_pantalla/2,Graphics.HCENTER+Graphics.VCENTER);
                //recorre los mapas y obtiene sus límites
                for (contador=vector_temporal.size()-1;contador>=0;contador--) {
                    lista_mapas[contador]=ruta_mapas+(String)vector_temporal.elementAt(contador);
                    archivo_IMG=new IMG_Parser();
                    retorno=archivo_IMG.abrir_mapa(lista_mapas[contador]);
                    if (retorno!=0) {
                        estado=2; //error abriendo archivo
                        return;
                    }
                    limites_mapas[contador]=archivo_IMG.leer_limites(detalle_minimo_mapa_general);
                    if ((gestion_mapa_general_permitida==true) && archivo_IMG.mapa_general==true) {
                        mapa_general[contador]=true;
                        mapa_general_presente=true;
                    }
                    retorno=archivo_IMG.cerrar_mapa();
                    //reduce el área dibujable a la zona inferior, donde se va a escribir
                    grafico.setClip(0,alto_pantalla-40,ancho_pantalla,40);
                    grafico.setColor(0,0,0);
                    grafico.fillRect(0,alto_pantalla-30,ancho_pantalla,alto_pantalla);
                    grafico.setColor(0xff,0xff,0xff);
                    grafico.drawString("Loading "+archivo_IMG.descripcion_mapa,10,alto_pantalla-30,0);
                    grafico.drawString(new Integer(contador).toString()+" maps left",10,alto_pantalla-20,0);
                    img_canvas.flushGraphics();
                }
                grafico.setColor(0,0,0);
                grafico.fillRect(0,alto_pantalla-30,ancho_pantalla,alto_pantalla);
                grafico.setColor(0xff,0xff,0xff);
                grafico.drawString("Generating Map",10,alto_pantalla-30,0);
                img_canvas.flushGraphics();
                grafico.setClip(0,0,ancho_pantalla,alto_pantalla); //vuelve a usar toda la pantalla
            }
            
        } catch (IOException ex) {
            estado=1; //error en el acceso a la ruta
            ex.printStackTrace();
        }
        
    }
    public Mapa_IMG[] generar_mapa (Tipo_Rectangulo limites,int nivel_zoom){
        String ruta_mapa;
        String nombre_plataforma; //nombre de la máquina corriendo java
        int retorno;
        Mapa_IMG [] mapas; //mapas visibles
        float altura_mapa;
        float anchura_mapa;
        Vector vector_temporal;
        int contador;
        int mapas_visibles[]; //lista de índices de mapas visibles
        if (this.estado!=0) return null; //no hay archivos IMG disponibles

        //recorre la lista de mapas buscando los que tengan áreas visibles
        vector_temporal=new Vector();
        for (contador=limites_mapas.length-1;contador>=0;contador--) {
            if (interseccion_limites(limites,limites_mapas[contador])==true) { //mapa visible. se añade a la lista
                vector_temporal.addElement(new Integer(contador));
            }
        }
        if (vector_temporal.size()==0) return null; //no hay mapas disponibles
        mapas_visibles=new int[vector_temporal.size()];
        for (contador=vector_temporal.size()-1;contador>=0;contador--) {
            mapas_visibles[contador]=((Integer)vector_temporal.elementAt(contador)).intValue();
        }
        mapas=new Mapa_IMG[mapas_visibles.length]; //define el array de mapas
        //recorre los mapas encontrados
        for (contador=mapas_visibles.length-1;contador>=0;contador--) {
            if (mapa_general_presente==true) {
                if (detalle[nivel_zoom]>=detalle_minimo_mapa_general) {
                    //sólo procesará el mapa si es el general
                    if (mapa_general[mapas_visibles[contador]]==true) {
                        archivo_IMG=cache.abrir_archivo(lista_mapas[mapas_visibles[contador]]);
                        mapas[contador]=archivo_IMG.generar_mapa(limites,(byte)detalle[nivel_zoom]);
                    } else {
                        mapas[contador]=null;
                    }
                } else { //para detalles bajos, no procesa lor mapas generales
                    if (mapa_general[mapas_visibles[contador]]==false) {
                        archivo_IMG=cache.abrir_archivo(lista_mapas[mapas_visibles[contador]]);
                        mapas[contador]=archivo_IMG.generar_mapa(limites,(byte)detalle[nivel_zoom]);
                    } else {
                        mapas[contador]=null;
                    }
                }
            } else {
                archivo_IMG=cache.abrir_archivo(lista_mapas[mapas_visibles[contador]]);
                mapas[contador]=archivo_IMG.generar_mapa(limites,(byte)detalle[nivel_zoom]);
                
            }
        }
        return mapas;
    }
  private boolean interseccion_limites(Tipo_Rectangulo rectangulo1,Tipo_Rectangulo rectangulo2){
      //devuelve true si hay interferencia entre los dos rectángulos
      boolean contenido_X=false;
      boolean contenido_Y=false;
      if ((rectangulo1.oeste>=rectangulo2.oeste && rectangulo1.oeste<=rectangulo2.este) || 
              (rectangulo1.este>=rectangulo2.oeste && rectangulo1.este<=rectangulo2.este) ||
              (rectangulo2.oeste>=rectangulo1.oeste && rectangulo2.oeste<=rectangulo1.este) ||
              (rectangulo2.este>=rectangulo1.oeste && rectangulo2.este<=rectangulo1.este)) contenido_X=true;
      if (contenido_X==false) return false;
      if ((rectangulo1.sur>=rectangulo2.sur && rectangulo1.sur<=rectangulo2.norte) || 
              (rectangulo1.norte>=rectangulo2.sur && rectangulo1.norte<=rectangulo2.norte) ||
              (rectangulo2.sur>=rectangulo1.sur && rectangulo2.sur<=rectangulo1.norte) ||
              (rectangulo2.norte>=rectangulo1.sur && rectangulo2.norte<=rectangulo1.norte)) contenido_Y=true;      
      if (contenido_Y==true) return true;
      return false;
  }    
  public  class Cache_IMG {
      final int capacidad; //nº de archivos que pueden estar abiertos como máximo
      private IMG_Parser [] cache; //lista de archivos
      private int [] puntuacion; //nº de accesos a cada archivo del caché
      private int numero_elementos; //nº de archivos abiertos. el máximo es la capacidad
      private String [] ruta_elementos; //ruta+nombre del archivo correspondiente
      public Cache_IMG() { //
          capacidad=8;
          cache=new IMG_Parser[capacidad];
          puntuacion=new int[capacidad];
          ruta_elementos=new String[capacidad];
          numero_elementos=0;
      }
      public IMG_Parser abrir_archivo(String nombre_archivo) {
          //comprueba si el archivo ya está en el caché
          int retorno;
          retorno=archivo_presente(nombre_archivo);
          if (retorno>=0) { //archivo presente
              return cache[retorno]; //devuelve el objeto, para generación de mapas
          }
          //archivo nuevo. hay que ver si cabe sin tener que cerrar elementos
          if (numero_elementos<capacidad) { //quedan huecos por rellenar
              preprocesar(numero_elementos,nombre_archivo);
              numero_elementos++;
              return cache[numero_elementos-1];
          }
          //cache llena, hay que cerrar el elemento de menor puntuación para
          //que el nuevo sustituya su lugar
          retorno=indice_menor_puntuacion();
          cache[retorno].cerrar_mapa();
          cache[retorno]=null;
          ruta_elementos[retorno]=null;
          System.gc();  //intenta liberar memoria antes de abrir el nuevo archivo
          puntuacion[retorno]=0;
          preprocesar(retorno,nombre_archivo);
          return cache[retorno];
      }
      private void preprocesar(int indice,String nombre_archivo) {
          //coloca el archivo indicado en el caché, y lo preprocesa
          int retorno;
          cache[indice]=new IMG_Parser();
          ruta_elementos[indice]=nombre_archivo;
          retorno=cache[indice].abrir_mapa(nombre_archivo);
          retorno=cache[indice].procesar_tre(detalle_minimo_mapa_general);
          retorno=cache[indice].procesar_rgn();
          puntuacion[indice]++;
          
      }
      private int archivo_presente(String ruta_archivo) {
          //comprueba si el arhcivo indicado ya está en el cache. si es así,
          //devuelve su índice
          int contador;
          boolean encontrado=false;
          for (contador=0;contador<numero_elementos;contador++) {
              if (ruta_elementos[contador].compareTo(ruta_archivo)==0) {
                  encontrado=true;
                  break;
              }
          }
          if (encontrado==true) { //si existe, devuelve el índice
              return contador;
          } else return -1; 
      }
      private int indice_menor_puntuacion() {
          //busca el elementos con menos accesos
          int contador;
          int menor;
          int indice_menor;
          menor=puntuacion[0];
          indice_menor=0;
          for (contador=1;contador<numero_elementos;contador++) {
              if (puntuacion[contador]<menor) {
                  menor=puntuacion[contador];
                  indice_menor=contador;
              }
          }
          return indice_menor;
      }
  }
  public int leer_detalle(int nivel_zoom) {
      //devuelve el detalle correspondiente a un nivel de zoom
      return detalle[nivel_zoom];
  }
}
