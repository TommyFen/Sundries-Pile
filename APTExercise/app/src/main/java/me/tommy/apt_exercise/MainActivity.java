package me.tommy.apt_exercise;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import me.tommy.autoviewbind.AutoBind;
import me.tommy.autoviewbind.launcher.AutoBindView;

public class MainActivity extends AppCompatActivity {

    @AutoBind(R.id.tv_apt_test)
    public TextView tvAptTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AutoBindView.getInstance().inject(this);

        tvAptTest.setText("APT 测试");
    }
}