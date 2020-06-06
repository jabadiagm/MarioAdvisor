import java.util.Vector;
/*
 * Tipo_Busqueda.java
 *
 * Created on 3 de junio de 2008, 21:41
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author javier
 */
public class Tipo_Busqueda {
    //gestiona la búsqueda de elementos en mapas IMG
    //objetos externos necesarios para la búsqueda
    private Gestor_Mapas gestor_mapas;
    public Tipo_Criterios_Busqueda criterios_busqueda;
    private Buscar_Canvas buscar_canvas;
    
    
    //variables públicas
    public int estado; //variable de control de la búsqueda
    public Vector resultados_busqueda;
    //constantes
    public static final int estado_inactivo=0;
    public static final int estado_buscando=1;
    
    /** Creates a new instance of Tipo_Busqueda */
    public Tipo_Busqueda(Tipo_Criterios_Busqueda criterios_busqueda,Gestor_Mapas gestor_mapas,Buscar_Canvas buscar_canvas) {
        this.criterios_busqueda=criterios_busqueda;
        this.gestor_mapas=gestor_mapas;
        this.buscar_canvas=buscar_canvas; //necesario para avisar de resultados y cambios
    }
    public int buscar() {
        //arranca la búsqueda
        if (this.estado==this.estado_buscando) return 1; //ya está ocupado
        resultados_busqueda=new Vector();
        this.estado=this.estado_buscando;
        gestor_mapas.buscar(this);
        return 0;
    }
    public void añadir_resultados(Vector parcial) {
        //esto deberá ir en el gestor de resultados
        int contador;
        if (parcial==null) return;
        for (contador=0;contador<parcial.size();contador++) {
            resultados_busqueda.addElement(parcial.elementAt(contador));
        }
        ordenar_resultados(); //se ordenan antes de mostrarlos en pantalla
    }
    public void cancelar_busqueda() {
        //avisa del deseo de cancelar la búsqueda, y espera a que termine para volver
        gestor_mapas.cancelar_busqueda();
        while (this.estado==this.estado_buscando);
        return;
    }
    public void notificar_nuevos_resultados() {
        //evento lanzado desde el gestor de mapas para indicar que ha añadido más resultados
        buscar_canvas.notificar_nuevos_resultados();
    }
    public void notificar_final_busqueda() {
        //evento lanzado desde el gestor de mapas para indicar que ha terminado al búsqueda
        this.estado=this.estado_inactivo;
        buscar_canvas.notificar_final_busqueda();
    }
    private void ordenar_resultados() {
        //ordena los resultados por orden alfabético o por distancia
        if (criterios_busqueda.ordenar_por_distancia==false) { //orden alfabético
            ordenar_alfabeticamente (resultados_busqueda,0,resultados_busqueda.size()-1);
        } else {
            ordenar_por_distancia (resultados_busqueda,0,resultados_busqueda.size()-1);
        }
    }
    private void ordenar_alfabeticamente(Vector src,int left,int right) {
        if (right > left) {
            String o1 = ((Tipo_Resultado_Busqueda)src.elementAt(right)).etiqueta;
            int i = left - 1;
            int j = right;
            while (true) {
                while (((Tipo_Resultado_Busqueda)src.elementAt(++i)).etiqueta.compareTo(o1)<0);
                while (j > 0)
                    if (((Tipo_Resultado_Busqueda)src.elementAt(--j)).etiqueta.compareTo(o1)<=0)
                        break;
                if (i >= j)
                    break;
                swap(src, i, j);
            }
            swap(src, i, right);
            ordenar_alfabeticamente(src, left, i - 1);
            ordenar_alfabeticamente(src, i + 1, right);
        }        
    }
    private static void ordenar_por_distancia(Vector src, int left, int right) {
        if (right > left) {
            Tipo_Resultado_Busqueda o1 = (Tipo_Resultado_Busqueda)src.elementAt(right);
            int i = left - 1;
            int j = right;
            while (true) {
                while (((Tipo_Resultado_Busqueda)src.elementAt(++i)).distancia<o1.distancia);
                while (j > 0)
                    if (((Tipo_Resultado_Busqueda)src.elementAt(--j)).distancia<=o1.distancia)
                        break;
                if (i >= j)
                    break;
                swap(src, i, j);
            }
            swap(src, i, right);
            ordenar_por_distancia(src, left, i - 1);
            ordenar_por_distancia(src, i + 1, right);
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
}
