package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;
    private FirebaseFirestore db;
    private int touchSlop;
    private static final float SWIPE_TRIGGER_DP = 80f;
    private CollectionReference citiesRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("Cities");
        citiesRef.addSnapshotListener((value,error) -> {
            if(error != null){
                Log.e("Firststore",error.toString());
            }
            if (value != null) {
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        cityListView.setOnTouchListener(new DeleteListener());



        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

    }
    private void DeleteBU(City city, View itemView) {
        new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Delete city?").setMessage("Delete " + city.getName() + " " + city.getProvince() + " ?")
                .setNegativeButton("Cancel", (d, w) -> {
                    itemView.animate().translationX(0).alpha(1f).setDuration(200).start();
                })
                .setPositiveButton("Delete", (d, w) -> {
                    itemView.animate()
                            .translationX(itemView.getWidth())
                            .alpha(0f)
                            .setDuration(250)
                            .withEndAction(() -> {
                                citiesRef.document(city.getName()).delete();
                            })
                            .start();
                })
                .show();
    }
    //citation: The following function are suggested by ChatGPT, OpenAI."Can u give me some suggestions about if i want to achieve swipe to delete?"
    private class DeleteListener implements View.OnTouchListener {
        private float downX, downY;
        private int downPosition = -1;
        private View downView = null;
        private boolean swiping = false;
        private float swipeThreshold;
        private float dpToPx(float dp) {
            return dp * getResources().getDisplayMetrics().density;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (cityListView.getAdapter() == null) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    downY = event.getY();
                    downPosition = cityListView.pointToPosition((int) downX, (int) downY);
                    if (downPosition == ListView.INVALID_POSITION) return false;
                    downView = cityListView.getChildAt(
                            downPosition - cityListView.getFirstVisiblePosition()
                    );
                    swipeThreshold = dpToPx(100);
                    swiping = false;
                    return false;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (downView == null) return false;
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;
                    if (dx > touchSlop && Math.abs(dy) < touchSlop * 2) {
                        swiping = true;
                        downView.setTranslationX(dx);
                        return true;
                    }
                    return false;
                }
                case MotionEvent.ACTION_UP: {
                    if (!swiping || downView == null) return false;
                    float totalDx = event.getX() - downX;
                    if (totalDx > swipeThreshold) {
                        City city = cityArrayAdapter.getItem(downPosition);
                        if (city != null) {
                            DeleteBU(city, downView);
                        }
                    } else {
                        downView.animate()
                                .translationX(0)
                                .setDuration(200)
                                .start();
                    }
                    reset();
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    if (downView != null) {
                        downView.animate()
                                .translationX(0)
                                .setDuration(200)
                                .start();
                    }
                    reset();
                    return false;
                }
            }
            return false;
        }
        private void reset() {
            downX = downY = 0;
            downPosition = -1;
            downView = null;
            swiping = false;
        }
    }
    @Override
    public void updateCity(City city, String title, String year) {
        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // Updating the database using delete + addition
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}