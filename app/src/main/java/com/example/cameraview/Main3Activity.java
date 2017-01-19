package com.example.cameraview;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Main3Activity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        TextView tv1= (TextView) findViewById(R.id.name);
        tv1.setText(getIntent().getExtras().getString("user5"));
        TextView tv2= (TextView) findViewById(R.id.id3);
        tv2.setText(getIntent().getExtras().getString("user6"));
        TextView tv3= (TextView) findViewById(R.id.location_t);
        tv3.setText(getIntent().getExtras().getString("user7"));
        TextView tv4= (TextView) findViewById(R.id.duration_t);
        tv4.setText(getIntent().getExtras().getString("user8"));

        Button b= (Button) findViewById(R.id.next);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Main3Activity.this,Main2Activity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }
        });

    }


}
