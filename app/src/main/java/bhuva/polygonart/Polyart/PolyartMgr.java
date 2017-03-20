package bhuva.polygonart.Polyart;

import android.app.usage.UsageEvents;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Debug;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import bhuva.polygonart.Common.Pair;
import bhuva.polygonart.Graphics.Generic;
import bhuva.polygonart.Graphics.Vec2D;
import bhuva.polygonart.UI.SelectionCircle;

/**
 * Created by bhuva on 5/8/2016.
 */
public class PolyartMgr {

    private static List<Polygon> polygons;

    private static boolean polygonCreationInProgress = false;
    private static boolean isMotionDownEventActive = false;

    private Paint paint;

    private static int curColor = Color.MAGENTA;
    private static int curBrushSize = 50;
    private static int curPolygonSides = 3;

    private static List<SelectionCircle> selCircles;

    private Random rnd = new Random();
    private Point screenDim = new Point();

    public enum Mode{CreationMode, EditingMode, RemoveMode};
    private static Mode curMode;

    private String TAG = "POLYART_MGR";

    public PolyartMgr(Context context){
        polygons = new ArrayList<Polygon>();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(screenDim);
        Log.d(TAG,"Dim:"+screenDim.x+", "+screenDim.y);
        setupPaint();
        curMode = Mode.CreationMode;
        selCircles = new ArrayList<>();
    }

    public void onTouchEvent(MotionEvent event){
        float curX = event.getX();
        float curY = event.getY();
        if( isWithinScreenDim(curX,curY)) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_OUTSIDE:
                    break;

                case MotionEvent.ACTION_DOWN:
                    isMotionDownEventActive = true;
                    if (curMode == Mode.CreationMode) {
                        createNew(curX, curY, false);
                    } else if (curMode == Mode.EditingMode) {
                        //editing of triangle
                        Log.d(TAG, "In Editing mode");
                    } else if(curMode == Mode.RemoveMode){
                        removePolygonAt(curX, curY);
                    }
                    isMotionDownEventActive = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (curMode == Mode.CreationMode && !isMotionDownEventActive) {
                        if (!polygonCreationInProgress) {
                            createNew(curX, curY, true);
                        }
                    } else if (curMode == Mode.EditingMode) {
                        //editing of triangle
                        Log.d(TAG, "In Editing mode");
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    break;
            }
        }
    }

    public void drawOnCanvas(Canvas canvas){
        Path path = new Path();
        for (Polygon t : polygons) {

            if(t.isVisible()) {
                path.reset();
                path = t.draw(path);
                path.close();

                paint.setColor(t.getColor());
                canvas.drawPath(path, paint);
            }
        }

        for(SelectionCircle c : selCircles){
            c.draw(canvas);
        }
    }

    public void createNew(float x, float y, boolean appending){
        PointF touchPoint = new PointF(x, y);
        if(!appending) {
            Polygon polygon = new Polygon(curPolygonSides, touchPoint, curBrushSize, curColor, true);
            polygons.add(polygon);
        }else{
            if(polygons.size()>0) {
                Polygon neighbor = polygons.get(polygons.size() - 1);
                float dist = neighbor.distToCCenter(touchPoint);
                if (dist > 1.5 * curBrushSize && dist < 20 * curBrushSize) {
                    polygonCreationInProgress = true;
                    Polygon generated = generateNeighbor(polygons.get(polygons.size() - 1), touchPoint);
                    polygons.add(generated);
                    polygonCreationInProgress = false;
                }
            }
        }
    }

    public void removePolygonAt(float x, float y){
        PointF touchPoint = new PointF(x, y);
        for(int i=polygons.size()-1; i>=0; i--){
            Polygon p = polygons.get(i);
            if( p.contains(touchPoint) && p.isVisible()){
                p.setVisible(false);
                break;
            }
        }
    }

    private Polygon generateNeighbor(Polygon t, PointF touchPoint){
        List<PointF> axisPoints = t.getVerticesOfNearestSide(touchPoint);
        Polygon polygon = Polygon.generateNeighbor(t, axisPoints);
        return polygon;
    }

    private boolean isInsideATriangle(float x, float y){
        PointF p = new PointF(x,y);
        for(int i=polygons.size()-1; i>=0; --i){
            Polygon t = polygons.get(i);
            if(t.contains(p)){
                return true;
            }
        }
        return false;
    }

    private Triangle createNewTriangle(float x, float y){
        Triangle t = Triangle.randomUnitTriangle();
        t.translate(new PointF(x, y));
        t.scale(curBrushSize);
        t.setColor(curColor);

        //selCircles.add( new SelectionCircle(t.getCCenter()));

        return t;
    }

    private Polygon createNewTriangleWith(Polygon t, PointF touchPoint){
        /*
        List<PointF> verts = t.getVerticesOfNearestSide(touchPoint); //verts has 0,1 as nearest vertices in terms of side facing and 2 index as the farthest vertex
        Log.d(TAG, "near1:"+verts.get(0).toString()+", near2:"+verts.get(1).toString());

        PointF mid = new PointF((verts.get(0).x+verts.get(1).x)/2.0f, (verts.get(0).y+verts.get(1).y)/2.0f);
        Log.d(TAG, "mid:"+mid.toString());
        Vec2D baseSide = new Vec2D(verts.get(0), verts.get(1));
        Log.d(TAG,"baseSide: "+baseSide.string());
        float adjByHyp = (float)Generic.dist(verts.get(0),verts.get(1))/(2*curBrushSize);
        adjByHyp = adjByHyp>0.75?0.75f:adjByHyp;
        float sinval = (float) Math.sin( Math.acos(adjByHyp));
        Log.d(TAG, "sinval:" + sinval);
        float len = curBrushSize * sinval;
        Log.d(TAG, "len: " + len);

        Vec2D dir = new Vec2D(t.getCenter(), mid);
        dir.norm();
        PointF newVert = dir.translatePointBy(mid, len+curBrushSize);
        Log.d(TAG,"final new vert: "+newVert.x+", "+newVert.y);
        Triangle newT = new Triangle(verts.get(0), verts.get(1), newVert);
        newT.setColor(Color.argb(255, Color.red(curColor) + rnd.nextInt(2), Color.green(curColor)+rnd.nextInt(2), Color.blue(curColor)+rnd.nextInt(2)));
        return newT;
        */
        List<PointF> axisPoints = t.getVerticesOfNearestSide(touchPoint);
        Polygon ne = Polygon.generateNeighbor(t, axisPoints);

        //selCircles.add(new SelectionCircle(axisPoints.get(0),Color.RED));
        //selCircles.add(new SelectionCircle(axisPoints.get(1), Color.BLACK));
        //selCircles.add(new SelectionCircle(ne.getCCenter()));

        return ne;
    }

    private void setupPaint(){
        paint = new Paint();
        paint.setColor(curColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
    }

    public static void clearAll(){
        curBrushSize = 50;
        curColor = Color.RED;
        curPolygonSides = 3;
        polygons.clear();
        polygonCreationInProgress =false;
        selCircles.clear();
        isMotionDownEventActive = false;
    }

    public static void setBrushSize(int size){
        curBrushSize = size;
    }

    public static void setColor(int color){
        curColor = color;
    }

    public static void done(){
        //saving
        curPolygonSides++;
    }

    public static void setMode(Mode mode){
        curMode = mode;
    }

    public static Mode getMode(){
        return curMode;
    }

    public static int getCurBrushSize(){
        return curBrushSize;
    }

    public static int getCurColor(){
        return curColor;
    }

    private boolean isWithinScreenDim(float x, float y){
        if (x >=0 && x<=screenDim.x && y>=0 && y<=screenDim.y)
            return true;
        return false;
    }

    private boolean isWithinScreenDim(PointF p){
        if (p.x >=0 && p.x<=screenDim.x && p.y>=0 && p.y<=screenDim.y)
            return true;
        return false;
    }
}
