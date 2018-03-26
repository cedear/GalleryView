package gallery.demo.com.galleryview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import gallery.demo.com.gallery.model.GalleryPhotoModel;
import gallery.demo.com.gallery.view.GalleryView;

public class MainActivity extends AppCompatActivity {


    private GridView urlTypeGridView, drawableTypeGridView;
    private GalleryView photoGalleryView;
    private LayoutInflater inflater;

    private List<GalleryPhotoModel> urlList = new ArrayList<>();
    private List<GalleryPhotoModel> resourceList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.photo_gallery_activity_main);
        initData();
        initView();
    }

    private void initData() {
        inflater = LayoutInflater.from(this);
        urlList.add(new GalleryPhotoModel("http://img0.imgtn.bdimg.com/it/u=2715862021,3180817113&fm=27&gp=0.jpg"));
        urlList.add(new GalleryPhotoModel("http://img4.imgtn.bdimg.com/it/u=1121460335,1804591133&fm=27&gp=0.jpg"));
        urlList.add(new GalleryPhotoModel("http://img5.imgtn.bdimg.com/it/u=3926003122,1701882005&fm=27&gp=0.jpg"));
        urlList.add(new GalleryPhotoModel("http://img2.imgtn.bdimg.com/it/u=2956928272,541387468&fm=27&gp=0.jpg"));
        urlList.add(new GalleryPhotoModel("http://img3.imgtn.bdimg.com/it/u=3524652925,4234956354&fm=27&gp=0.jpg"));
        urlList.add(new GalleryPhotoModel("https://imgs.genshuixue.com/49509748_tkiz8i0z.jpeg"));

        resourceList.add(new GalleryPhotoModel("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1521717658692&di=8897bcf879d0da55db484e293a44ca12&imgtype=0&src=http%3A%2F%2Fimg.redocn.com%2Fsheji%2F20140924%2Fliushiwuzhounianguoqingshuxingzhanban_3128211.jpg"));
        resourceList.add(new GalleryPhotoModel(R.drawable.emoji_cry));
        resourceList.add(new GalleryPhotoModel(R.drawable.emoji_doubt));
        resourceList.add(new GalleryPhotoModel(R.drawable.emoji_giggle));
        resourceList.add(new GalleryPhotoModel(R.drawable.emoji_heartstopper));
        resourceList.add(new GalleryPhotoModel(R.drawable.emoji_heartstopper_n));
    }


    private void initView() {
        photoGalleryView = (GalleryView) findViewById(R.id.photo_gallery_view);
        urlTypeGridView = (GridView) findViewById(R.id.url_type_gridView);
        drawableTypeGridView = (GridView) findViewById(R.id.drawabl_type_gridView);
        urlTypeGridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return urlList.size();
            }

            @Override
            public Object getItem(int position) {
                return urlList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup viewGroup) {
                ViewHoder viewHoder = null;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.photo_gallery_item_photo_show, null);
                    viewHoder = new ViewHoder();
                    viewHoder.imageView = (ImageView) convertView.findViewById(R.id.item_photo);
                    convertView.setTag(viewHoder);
                } else {
                    viewHoder = (ViewHoder) convertView.getTag();
                }
                Glide.with(MainActivity.this).load(urlList.get(position).photoSource).into(viewHoder.imageView);
                final ViewHoder finalViewHoder = viewHoder;
                viewHoder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        photoGalleryView.showPhotoGallery(position, urlList, finalViewHoder.imageView);
                    }
                });

                return convertView;
            }
        });
        drawableTypeGridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return resourceList.size();
            }

            @Override
            public Object getItem(int i) {
                return resourceList.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup viewGroup) {
                ViewHoder viewHoder = null;
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.photo_gallery_item_photo_show, null);
                    viewHoder = new ViewHoder();
                    viewHoder.imageView = (ImageView) convertView.findViewById(R.id.item_photo);
                    convertView.setTag(viewHoder);
                } else {
                    viewHoder = (ViewHoder) convertView.getTag();
                }
                Glide.with(MainActivity.this).load(resourceList.get(position).photoSource).into(viewHoder.imageView);
                final ViewHoder finalViewHoder = viewHoder;
                viewHoder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        photoGalleryView.showPhotoGallery(position, resourceList, finalViewHoder.imageView);
                    }
                });

                return convertView;
            }
        });
    }

    class ViewHoder {
        private ImageView imageView;
    }

}
