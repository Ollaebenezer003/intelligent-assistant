package com.example.olla.smartassistant;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpClassListview extends ArrayAdapter<String> {

    private String [] fruitname;
    private String[] desc;
    private Integer[] imgId;
    private Activity context;


    public HelpClassListview(Activity context, String [] fruitname, String[] desc, Integer[] imgId) {
        super(context, R.layout.listview_layout, fruitname);

        this.context = context;
        this.fruitname = fruitname;
        this.desc = desc;
        this.imgId = imgId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View r = convertView;
        ViewHolder viewHolder = null;
        if (r == null){


            LayoutInflater layoutInflater = context.getLayoutInflater();
            r = layoutInflater.inflate(R.layout.listview_layout, null, true);
            viewHolder = new ViewHolder(r);
            r.setTag(viewHolder);


        } else {
            viewHolder = (ViewHolder) r.getTag();
        }

        viewHolder.ivw.setImageResource(imgId[position]);
        viewHolder.tvw1.setText(fruitname[position]);
        viewHolder.tvw2.setText(desc[position]);

        return r;


    }

    class ViewHolder{

        TextView tvw1;
        TextView tvw2;
        ImageView ivw;

        ViewHolder(View v){
            tvw1 = v.findViewById(R.id.pictureName);
            tvw2 = v.findViewById(R.id.descr);
            ivw = v.findViewById(R.id.imageView);
        }
    }

}
