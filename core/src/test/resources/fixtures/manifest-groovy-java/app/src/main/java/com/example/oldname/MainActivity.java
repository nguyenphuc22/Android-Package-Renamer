package com.example.oldname;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.oldname.util.Helper;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Helper helper = new Helper();
        helper.tag();
    }
}
