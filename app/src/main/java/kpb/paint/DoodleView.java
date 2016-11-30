package kpb.paint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

// предоставляет функции рисования, сохранения и печати

public class DoodleView extends View {
    // смещение, необходиме для продолжения рисования
    private final float TOUCH_RANGE = 10;
    // область рисования для вывода или сохранения
    private Bitmap bitmap;
    // используется для рисования на Bitmap
    private Canvas canvas;
    // используется для вывода Bitmap на экран
    private Paint paintScreen;
    // для рисования линий на Bitmap
    private Paint paintLine;
    // данные нарисованных контуров Path и содержащих в них точек
    private final Map<Integer, Path> pathMap = new HashMap<>();
    private final Map<Integer, Point> previousPointMap =  new HashMap<>();


    // конструктор
    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintScreen =  new Paint();

        // исходные параметры рисуемых линий
        paintLine = new Paint();
        // сглаживание краёв
        paintLine.setAntiAlias(true);
        //по умолчанию чёрный цвет
        paintLine.setColor(Color.BLACK);
        // сплошная линия
        paintLine.setStyle(Paint.Style.STROKE);
        // толщина линии по умолчанию
        paintLine.setStrokeWidth(5);
        // закруглённые концы
        paintLine.setStrokeCap(Paint.Cap.ROUND);
    }

    // определяем объект Bitmap именно здесь, а не в onCreate(), т.к. View должно быть заполненно
    // и добавленно в MainActivity
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // в последнем аргументе передается кодировка Bitmap. Константа ARGB_8888 означает,
        // что цвет каждого байта хранится в 4 байтах(по одному байту для альфа-канала, красной, зеленой,
        // и синей составляющих)
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        // инициализация канвы для рисования на Bitmap
        canvas = new Canvas(bitmap);
        // Bitmap задаётся белый цвет
        bitmap.eraseColor(Color.WHITE);
    }
    // обработка события касания
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // тип события
        int action = event.getActionMasked();
        // указатель (палец)
        int fingerIndex = event.getActionIndex();
        // если пользователь коснулся первым пальцем || вторым, третьим и т.д.
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN){
            touchStarted(event.getX(fingerIndex), event.getY(fingerIndex), event.getPointerId(fingerIndex));
        }
        // если пользователь убрал палец с экрана
        else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP){
            touchEnded(event.getPointerId(fingerIndex));
        }
        // для рисования линий
        else {
            touchMoved(event);
        }
        // перерисовать изображение, по сути, вызвать onDraw()
        invalidate();
        return true;
    }
    private void touchStarted(float x, float y, int fingerIndex){
        // для хранения контура с заданным идентификатором
        Path path;
        // для хранения последней точки в контуре
        Point point;
        // создания контура с точкой касания, переход к ней, занесения данных в Map
        path = new Path();
        pathMap.put(fingerIndex, path);
        point = new Point();
        point.x = (int)x;
        point.y = (int)y;
        previousPointMap.put(fingerIndex, point);
        // переход к координатам касания
        path.moveTo(x, y);
    }

    public void touchMoved(MotionEvent event){
        // для каждого укзателя(пальца) в объекте MotionEvent
        for (int i = 0; i < event.getPointerCount(); i++){
            // получить позицию касания в MotionEvent и индекс этого касания для получения его данных
            int pointerID = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerID);
            // если существует объект Path, связанный с указателем(пальцем)
            if (pathMap.containsKey(pointerID)){
                // получить новые координаты указателя
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);
                // получить объект Path и объект Point, связанный с указателем
                Path path = pathMap.get(pointerID);
                Point point = previousPointMap.get(pointerID);
                // вычислить величину смещения от последнего обновления
                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);
                // если расстояние достаточно велико
                if (deltaX >= TOUCH_RANGE || deltaY >= TOUCH_RANGE){
                    // расширение контура до новой точки
                    path.quadTo(point.x, point.y, (newX + point.x) / 2,
                            (newY + point.y) / 2);
                }
                // сохранение новых координат
                point.x = (int) newX;
                point.y = (int) newY;
            }
        }
    }
    public void touchEnded(int fingerIndex){
        // получение объекта Path
        Path path = pathMap.get(fingerIndex);
        // рисование на canvas
        canvas.drawPath(path, paintLine);
        // сброс объекта Path
        path.reset();
    }
    // назначение цвета рисуемой линии
    public void setDrawingColor(int color) {
        paintLine.setColor(color);
    }

    // получение цвета рисуемой линии
    public int getDrawingColor() {
        return paintLine.getColor();
    }

    // назначение толщины рисуемой линии
    public void setLineWidth(int width) {
        paintLine.setStrokeWidth(width);
    }

    // получение толщины рисуемой линии
    public int getLineWidth() {
        return (int) paintLine.getStrokeWidth();
    }

    // перерисовка при обновлении DoodleView на экране, т.е. рисует до отрывания пальца от экрана
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // перерисовка фона
        canvas.drawBitmap(bitmap, 0, 0, paintScreen);
        // для каждой линии
        for (Integer key : pathMap.keySet()){
            canvas.drawPath(pathMap.get(key), paintLine);
        }
    }
    public void clear(){
        // удалить все контуры
        pathMap.clear();
        // удалить все предыдущие точки
        previousPointMap.clear();
        // очистка изображения
        bitmap.eraseColor(Color.WHITE);
        // перерисовать изображение, по сути, вызвать onDraw()
        invalidate();
    }
    // сохранение изображения в галерее
    public void saveImage(){
        final String name = "Doodlz" + System.currentTimeMillis() + ".jpg";
        // сохранение в галерее
        // объект ContentResolver, используемый методом для определения места хранения изображения на устройстве
        String location = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, name, "Doodlz drawing");
        // тостер об успешном сохранении
        if (location != null){
            Toast toast = Toast.makeText(getContext(), R.string.message_saved, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            Toast toast = Toast.makeText(getContext(), R.string.message_error_saving, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
