package kpb.paint;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

//управляет DoodleView и обработкой событий акселерометра

public class MainActivityFragment extends Fragment {

    // предоставляет область рисования
    private DoodleView doodleView;
    // переменные для обработки встряхивания телефона
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    // boolean переменная для обозначения присутствия диалогового окна на экране
    private boolean dialogOnScreen = false;
    // менеджер для работы с датчиками устройства
    SensorManager sensorManager;
    // используется для обнаружения встряхивания устройства
    private static final int ACCELERATION_COUNT = 100000;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // разрешить размещать компоненты фрагмента на панели действий (меню)
        setHasOptionsMenu(true);

        doodleView = (DoodleView) view.findViewById(R.id.doodleView);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        // инициализация параметров ускорения
        acceleration = 0.0f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;

        return view;
    }

    // включение акселерометра, после получения фокуса
    @Override
    public void onResume() {
        super.onResume();
        enableAccelerometerListening();
    }

    // прослушивание событий акселерометра
    public void enableAccelerometerListening(){
        sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }
    // отключение акселерометра
    public void disableAccelerometerListening(){
        sensorManager.unregisterListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }
    // отключение акселерометра при потере фокуса
    @Override
    public void onPause() {
        super.onPause();
        disableAccelerometerListening();
    }

    // объект
    // прослушивания событий акселерометра
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        // проверка встряхивания по показателям акселерометра
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // проверка на наличие диалоговых окон на экране
            if(!dialogOnScreen){
                // получить значения x y z для sensorEvent
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                // вычисление текущего ускорения
                currentAcceleration = x * x + y * y + z * z;
                // вычисление изменения ускорения
                acceleration = currentAcceleration * (currentAcceleration - lastAcceleration);
                // если изменение превышает данный порог
                if(acceleration > ACCELERATION_COUNT){
                    confirmErase();
                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    public void confirmErase(){
        EraseImageDialogFragment fragment = new EraseImageDialogFragment();
        fragment.show(getFragmentManager(), "erase dialog");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // вручную заполняем инфлатер
        inflater.inflate(R.menu.doodle_fragment_menu, menu);
    }
    // обработка выбора команд меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.color:
                ColorDialogFragment colorDialogFragment = new ColorDialogFragment();
                colorDialogFragment.show(getFragmentManager(), "color dialog");
                return true;
            case R.id.line_width:
                LineWidthDialogFragment widthDialog = new LineWidthDialogFragment();
                widthDialog.show(getFragmentManager(), "line width dialog");
                return true;
            case R.id.delete_drawing:
                confirmErase();
                return true;
            case R.id.save:
                doodleView.saveImage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public DoodleView getDoodleView() {
        return doodleView;
    }

    // indicates whether a dialog is displayed
    public void setDialogOnScreen(boolean visible) {
        dialogOnScreen = visible;
    }

}
