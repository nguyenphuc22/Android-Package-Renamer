package com.example.oldname

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.oldname.util.Helper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Helper.show()
    }
}
