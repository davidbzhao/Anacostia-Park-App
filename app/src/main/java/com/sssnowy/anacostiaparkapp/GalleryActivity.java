package com.sssnowy.anacostiaparkapp;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GalleryActivity extends Activity {
    private ArrayList data = new ArrayList();
    private String[] imageURLs = {
            "https://pixabay.com/static/uploads/photo/2014/07/27/20/29/landscape-403165_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2015/12/21/06/41/landscape-1102117_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2015/10/26/23/51/landscape-1008154_960_720.jpg",
            "http://www.publicdomainpictures.net/pictures/60000/nahled/autumn-landscape-1379696322ccb.jpg",
            "https://c2.staticflickr.com/6/5738/23929500196_b6a1ce1dfb_b.jpg",
            "http://res.freestockphotos.biz/pictures/9/9149-morning-sun-with-a-tree-in-the-foreground-pv.jpg",
            "https://pixabay.com/static/uploads/photo/2015/12/08/00/37/mountains-landscape-1081889_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2015/05/30/19/59/landscape-790644_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2014/07/27/20/29/landscape-403165_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2015/12/21/06/41/landscape-1102117_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2015/10/26/23/51/landscape-1008154_960_720.jpg",
            "http://www.publicdomainpictures.net/pictures/60000/nahled/autumn-landscape-1379696322ccb.jpg",
            "https://c2.staticflickr.com/6/5738/23929500196_b6a1ce1dfb_b.jpg",
            "http://res.freestockphotos.biz/pictures/9/9149-morning-sun-with-a-tree-in-the-foreground-pv.jpg",
            "https://pixabay.com/static/uploads/photo/2015/12/08/00/37/mountains-landscape-1081889_960_720.jpg",
            "https://pixabay.com/static/uploads/photo/2015/05/30/19/59/landscape-790644_960_720.jpg"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        for(int cnt = 0; cnt < imageURLs.length; cnt++){
            data.add(new GalleryImage("Image " + cnt, imageURLs[cnt]));
        }

        RecyclerView galleryRecyclerView = (RecyclerView) findViewById(R.id.galleryRecyclerView);
        galleryRecyclerView.setHasFixedSize(true);
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(GalleryActivity.this, 3));

        RecyclerAdapter recyclerAdapter = new RecyclerAdapter(data, R.id.galleryRecyclerView);

        galleryRecyclerView.setAdapter(recyclerAdapter);
    }

    class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
        private ArrayList<GalleryImage> images;
        private int galleryImageLayout;

        public RecyclerAdapter(ArrayList<GalleryImage> images, int galleryImageLayout){
            this.images = images;
            this.galleryImageLayout = galleryImageLayout;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_image_view, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Glide.with(GalleryActivity.this).load(images.get(position).getUrl())
                    .thumbnail(0.5f)
                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.getImageView());
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }

            public ImageView getImageView() {
                return imageView;
            }

            public void setImageView(ImageView imageView) {
                this.imageView = imageView;
            }
        }
    }

}

