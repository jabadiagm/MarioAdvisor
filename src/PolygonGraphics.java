import javax.microedition.lcdui.Graphics;

/**
 * Polygon rendering for J2ME MIDP 1.0.
 *
 * <p>This has its own fillTriangle() method because of the 
 * absence of that method in MIDP 1.0 (unlike MIDP 2.0). </p>
 *
 * @author <a href="mailto:simonturner@users.sourceforge.net">Simon Turner</a> 
 * @version $Id: PolygonGraphics.java,v 1.1.1.1.2.2 2006/04/08 12:58:06 simonturner Exp $
 */
public class PolygonGraphics {



    /**
     * Draw a polygon
     *
     * @param g         The Graphics object to draw the polygon onto
     * @param xPoints   The x-points of the polygon
     * @param yPoints   The y-points of the polygon
     */
    public static void drawPolygon(Graphics g, int[] xPoints, int[] yPoints) {
        int max = xPoints.length - 1;
        for (int i=0; i<max; i++) {
            g.drawLine(xPoints[i], yPoints[i], xPoints[i+1], yPoints[i+1]);
        }
        g.drawLine(xPoints[max], yPoints[max], xPoints[0], yPoints[0]);
    }
    public static void drawPolyline(Graphics g, int[] xPoints, int[] yPoints) {
        int max = xPoints.length - 1;
        for (int i=0; i<max; i++) {
            g.drawLine(xPoints[i], yPoints[i], xPoints[i+1], yPoints[i+1]);
        }
    }
    public static void dibujar_polilinea2(Graphics g, int[] xPoints, int[] yPoints) {
        int max = xPoints.length - 1;
        for (int i=0; i<max; i++) {
            linea_grosor2(g,xPoints[i], yPoints[i], xPoints[i+1], yPoints[i+1]);
        }
    }
    /**
     * Fill a polygon
     *
     * @param g         The Graphics object to draw the polygon onto
     * @param xPoints   The x-points of the polygon
     * @param yPoints   The y-points of the polygon
     */
    public static void fillPolygon(Graphics g, int[] xPoints, int[] yPoints) {
        while (xPoints.length > 2) {
            // a, b & c represents a candidate triangle to draw. 
            // a is the left-most point of the polygon
            int a = GeomUtils.indexOfLeast(xPoints);
            // b is the point after a
            int b = (a + 1) % xPoints.length;
            // c is the point before a
            int c = (a > 0) ? a - 1 : xPoints.length - 1;
            // The value leastInternalIndex holds the index of the left-most 
            // polygon point found within the candidate triangle, if any.
            int leastInternalIndex = -1;
            boolean leastInternalSet = false;
            // If only 3 points in polygon, skip the tests
            if (xPoints.length > 3) {
                // Check if any of the other points are within the candidate triangle
                for (int i=0; i<xPoints.length; i++) {
                    if (i != a && i != b && i != c) {
                        if (GeomUtils.withinBounds(xPoints[i], yPoints[i], 
                                                   xPoints[a], yPoints[a],
                                                   xPoints[b], yPoints[b],
                                                   xPoints[c], yPoints[c])) {
                            // Is this point the left-most point within the candidate triangle?
                            if (!leastInternalSet || xPoints[i] < xPoints[leastInternalIndex]) {
                                leastInternalIndex = i;
                                leastInternalSet = true;
                            }
                        }
                    }
                }
            }
            // No internal points found, fill the triangle, and reservoir-dog the polygon
            if (!leastInternalSet) {
                g.fillTriangle(xPoints[a], yPoints[a], xPoints[b], yPoints[b], xPoints[c], yPoints[c]);
                int[][] trimmed = GeomUtils.trimEar(xPoints, yPoints, a);
                xPoints = trimmed[0];
                yPoints = trimmed[1];
            // Internal points found, split the polygon into two, using the line between
            // "a" (left-most point of the polygon) and leastInternalIndex (left-most  
            // polygon-point within the candidate triangle) and recurse with each new polygon
            } else {
                int[][][] split = GeomUtils.split(xPoints, yPoints, a, leastInternalIndex);
                int[][] poly1 = split[0];
                int[][] poly2 = split[1];
                fillPolygon(g, poly1[0], poly1[1]);
                fillPolygon(g, poly2[0], poly2[1]);
                break;
            }
        }
    }
    public static void linea_grosor2(Graphics g,int x1, int y1,int x2,int y2) {
        //dibuja una línea de dos píxeles de grosor, formada por dos líneas. la
        //segunda línea se coloca de 4 formas posibles:
        //   *****       ***       ***      *X
        //   XXXXX     ***XXX     XXX***    *X
        //              XXX         XXX     *X
        //para definir la secundaria se calcula la pendiente de la recta. si ésta es pequeña,
        //significa que la línea es cercana a la horizontal. si es grande, se acerca a la vertical
        double pendiente;
        int delta_x;
        int delta_y;
        if (x1==x2) { //línea vertical
            delta_x=-1;
            delta_y=0;
            g.drawLine(x1,y1,x2,y2);
            g.drawLine(x1+delta_x,y1+delta_y,x2+delta_x,y2+delta_y);

        } else {
            pendiente=(y2-y1)/(x2-x1);
            if (Math.abs(pendiente)<0.4142) { //línea casi horizontal
                delta_x=0;
                delta_y=-1;
                g.drawLine(x1,y1,x2,y2);
                g.drawLine(x1+delta_x,y1+delta_y,x2+delta_x,y2+delta_y);
                
            } else if (Math.abs(pendiente)>2.4142) { //línea casi vertical
                delta_x=-1;
                delta_y=0;
                g.drawLine(x1,y1,x2,y2);
                g.drawLine(x1+delta_x,y1+delta_y,x2+delta_x,y2+delta_y);
                
            } else {
                if (pendiente<0) { //45º hacia la arriba (y crece hacia abajo)
                    delta_x=-1;
                    delta_y=-1;
                } else { //45º hacia abajo
                    delta_x=1;
                    delta_y=-1;
                }
                g.drawLine(x1,y1,x2,y2);
                g.drawLine(x1+delta_x,y1+delta_y,x2+delta_x,y2+delta_y);
                g.drawLine(x1,y1+delta_y,x2,y2+delta_y); //hueco entre líneas
                
            }
            
        }
    }

}
