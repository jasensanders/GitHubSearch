package com.enrandomlabs.jasensanders.v1.githubsearch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;


public class ListItemAdapter extends RecyclerView.Adapter<ListItemAdapter.ViewHolder>  {

    private ArrayList<String[]> mData;
    final private Context mContext;
    private OnItemClickedListener mClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView artImage;
        public final TextView title;
        public final TextView byline;
        public final TextView stars;
        public final TextView subText;

        //Set references for all the views
        public ViewHolder(View view){
            super(view);

            artImage = view.findViewById(R.id.thumbnail);
            title = view.findViewById(R.id.headline_title);
            byline = view.findViewById(R.id.byline);
            stars = view.findViewById(R.id.stars);
            subText = view.findViewById(R.id.sub_text);
            view.setOnClickListener(this);

        }
        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            String[] current = mData.get(adapterPosition);
            String repoUrl = current[0];

            // Redirect to GitHub Source HTML Page.
            Intent result = new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl));
            result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if(result.resolveActivity(mContext.getPackageManager()) != null){

                mContext.startActivity(result);
            }


        }

    }

    public ListItemAdapter(Context context){
        mContext = context;
    }

    @Override
    public ListItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        //Inflate the new view
        if(parent instanceof RecyclerView){
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_main, parent, false);
            view.setFocusable(true);
            return new ViewHolder(view);
        }else{
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(final ListItemAdapter.ViewHolder holder, int position) {
        String[] item = mData.get(position);

        //Load the imageView
        String artUrl = item[1];
        if(MainActivity.isNetworkAvailable(mContext)){
            Glide.with(mContext).load(artUrl).into(holder.artImage);
        }

        //Get data from array:
        // Item name
        String title = item[2];
        // Description
        String byline = item[3];
        // Date added
        String stars = "\u2605"+ " Stars";
        // Number of Starrs
        String subText = item[4];



        //Set data into views
        holder.title.setText(title);
        holder.byline.setText(byline);
        holder.stars.setText(stars);
        holder.subText.setText(subText);


    }

    @Override
    public int getItemCount() {
        if ( null == mData ) return 0;
        return mData.size();
    }

    public void swapData(ArrayList<String[]> newData) {
        mData = newData;
        this.notifyDataSetChanged();

    }

    public interface OnItemClickedListener{
        void onItemClicked(Uri data, String status);
    }

    public void setOnItemClickedListener(OnItemClickedListener listener){
        mClickListener = listener;
    }

    public ArrayList<String[]> getData() {
        return mData;
    }


}
