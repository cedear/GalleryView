package gallery.demo.com.gallery.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

public class GalleryDownloadThread implements Runnable{


    private Object photoObj;
    private Context context;
    private GalleryDownLoadCallBack callBack;
    private boolean isSucceed;

    public GalleryDownloadThread(Context context, Object photoObj, GalleryDownLoadCallBack callBack) {
        this.context = context;
        this.photoObj = photoObj;
        this.callBack = callBack;
    }


    @Override
    public void run() {
        Bitmap bitmap = null;
        try {
            bitmap = Glide.with(context)
                    .asBitmap()
                    .load(photoObj)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();
            if (bitmap != null) {
                saveImageToGallery(context, bitmap);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            if (bitmap != null && isSucceed) {
                callBack.onDownLoadSuccess();
            } else {
                callBack.onDownLoadFailed();
            }
        }
    }


    public void saveImageToGallery(final Context context, final Bitmap bitmap) {
        //确认有没有SD卡
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            String imgName = System.currentTimeMillis() + ".jpg";
            String s = null;
            Cursor cursor = null;
            try{
                s = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, imgName, null);
                String[] proj = {MediaStore.Images.Media.DATA};
                cursor = ((Activity) context).managedQuery(Uri.parse(s), proj, null, null, null);
                if (cursor == null) {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(s)));
                } else {
                    //兼容华为4x（型号） 5.0系统问题（保存进文件后，相册并没有刷新）
                    int actual_image_column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    String img_path = cursor.getString(actual_image_column_index);
                    MediaScannerConnection.scanFile(context, new String[]{img_path}, null, null);
                }
                isSucceed = true;
            } catch (Exception e) {
                isSucceed = false;
            }
        }
    }

}
