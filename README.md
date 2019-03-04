## 图片大图预览
> 在我现在的项目当中，也存在大图预览的功能，但其实现过于繁重，采用一个Activity实现，并且在图片展示的过程中会产生卡顿感，整体感觉很是不好，正巧项目也在重构过程中，所以决定将这一功能写成一个成型的控件。话不多说，先上图看下效果。  
  
![image](https://github.com/cedear/Cedear.github.io/blob/master/%E5%9B%BE%E7%89%87%E6%96%87%E4%BB%B6%E5%A4%B9/%E4%B8%8B%E8%BD%BD.gif?raw=true)  
### 整体实现思路  
> 图片展示：PhotoView（大图支持双击放大）  
图片加载：Glide(加载网络图片、本地图片、资源文件)  
小图变大图时的实现：动画  
图片的下载：插入系统相册   

> 该控件采用自定义View的方式，通过一些基本的控件的组合，来形成一个具有大图预览的控件。上代码

### 使用方法
(1)在布局文件中引用该view
```
<com.demo.gallery.view.GalleryView
        android:id="@+id/photo_gallery_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone"
        app:animDuration="300"
        app:saveText="保存至相册"
        app:saveTextColor="#987622"/>  
```  
(2)具体使用方法  
GalleryView galleryView = findViewById(R.id.photo_gallery_view);  
galleryView.showPhotoGallery(index, List, ImageView);
> 到这里就结束了，就是这么简单！
### 具体实现  
  
(1)先从showPhotoGallery(index, List, ImageView)这个方法讲起  
> int index：我们想要展示的一个图片列表中的第几个  
List<GalleryPhotoModel> list: 我们要传入的要展示的图片类型list（支持网络图片、资源图片、本地图片（本地图片与网络图片其实都是一个字符串地址））  

```
public class GalleryPhotoModel {

    public Object photoSource;

    public GalleryPhotoModel(@DrawableRes int drawableRes) {
        this.photoSource = drawableRes;
    }

    public GalleryPhotoModel(String path) {
        this.photoSource = path;
    }

}
```
> ImageView:即你点击想要展示的那个图片  
  
(2)对传入GalleryView的数据进行处理  

```
/**
     * @param index             想要展示的图片的索引值
     * @param photoList         图片集合（URL、Drawable、Bitmap）
     * @param clickImageView    点击的第一个图片
     */
    public void showPhotoGallery(int index, List<GalleryPhotoModel> photoList, ImageView clickImageView) {
        GalleryPhotoParameterModel photoParameter = new GalleryPhotoParameterModel();
        //图片
        photoParameter.photoObj = photoList.get(index).photoSource;
        //图片在list中的索引
        photoParameter.index = index;
        int[] locationOnScreen = new int[2];
        //图片位置参数
        clickImageView.getLocationOnScreen(locationOnScreen);
        photoParameter.locOnScreen = locationOnScreen;
        //图片的宽高
        int width = clickImageView.getDrawable().getBounds().width();
        int height = clickImageView.getDrawable().getBounds().height();
        photoParameter.imageWidth = clickImageView.getWidth();
        photoParameter.imageHeight = clickImageView.getHeight();
        photoParameter.photoHeight = height;
        photoParameter.photoWidth = width;
        //scaleType
        photoParameter.scaleType = clickImageView.getScaleType();
        //将第一个点击的图片参数连同整个图片列表传入
        this.setVisibility(View.VISIBLE);
        post(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
        setGalleryPhotoList(photoList, photoParameter);
    }
```  
> 通过传递进来的ImageView,获取被点击View参数，并拼装成参数model，再进行数据的相关处理。  
  
(3)GalleryView的实现机制 
> 该View的实现思路主要是：最外层是一个RelativeLayout，内部有一个充满父布局的ImageView和ViewPager。ImageView用来进行图片的动画缩放，ViewPager用来进行最后的图片的展示。其实该View最主要的地方就是通过点击ImageView到最后ViewPager的展示的动画。接下来主要是讲解一下这个地方。先看一下被点击ImageView的参数Model。GalleryPhotoParameterModel  

```
public class GalleryPhotoParameterModel {

    //索引
    public int index;
    // 图片的类型
    public Object photoObj;
    // 在屏幕上的位置
    public int[] locOnScreen = new int[]{-1, -1};
    // 图片的宽
    public int photoWidth = 0;
    // 图片的高
    public int photoHeight = 0;
    // ImageView的宽
    public int imageWidth = 0;
    // ImageView的高
    public int imageHeight = 0;
    // ImageView的缩放类型
    public ImageView.ScaleType scaleType;

}
```  
3.1图片放大操作  

```
private void handleZoomAnimation() {
        // 屏幕的宽高
        this.mScreenRect = GalleryScreenUtil.getDisplayPixes(getContext());
        //将被缩放的图片放在一个单独的ImageView上进行单独的动画处理。
        Glide.with(getContext()).load(firstClickItemParameterModel.photoObj).into(mScaleImageView);
        //开启动画
        mScaleImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //开始放大操作
                calculateScaleAndStartZoomInAnim(firstClickItemParameterModel);
                //
                mScaleImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }
```  

```
/**
     * 计算放大比例，开启放大动画
     *
     * @param photoData
     */
    private void calculateScaleAndStartZoomInAnim(final GalleryPhotoParameterModel photoData) {
        mScaleImageView.setVisibility(View.VISIBLE);

        // 放大动画参数
        int translationX = (photoData.locOnScreen[0] + photoData.imageWidth / 2) - (int) (mScreenRect.width() / 2);
        int translationY = (photoData.locOnScreen[1] + photoData.imageHeight / 2) - (int) ((mScreenRect.height() + GalleryScreenUtil.getStatusBarHeight(getContext())) / 2);
        float scale = getImageViewScale(photoData);
        // 开启放大动画
        executeZoom(mScaleImageView, translationX, translationY, scale, true, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                showOtherViews();
                tvPhotoSize.setText(String.format("%d/%d", viewPager.getCurrentItem() + 1, photoList.size()));
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
```    
3.2 图片缩小操作  

```
/**
     * 计算缩小比例，开启缩小动画
     */
    private void calculateScaleAndStartZoomOutAnim() {
        hiedOtherViews();

        // 缩小动画参数
        int translationX = (firstClickItemParameterModel.locOnScreen[0] + firstClickItemParameterModel.imageWidth / 2) - (int) (mScreenRect.width() / 2);
        int translationY = (firstClickItemParameterModel.locOnScreen[1] + firstClickItemParameterModel.imageHeight / 2) - (int) ((mScreenRect.height() + GalleryScreenUtil.getStatusBarHeight(getContext())) / 2);
        float scale = getImageViewScale(firstClickItemParameterModel);
        // 开启缩小动画
        executeZoom(mScaleImageView, translationX, translationY, scale, false, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                mScaleImageView.setImageDrawable(null);
                mScaleImageView.setVisibility(GONE);
                setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
    }
```  
3.3 计算图片缩放的比例
```
private float getImageViewScale(GalleryPhotoParameterModel photoData) {
        float scale;
        float scaleX = photoData.imageWidth / mScreenRect.width();
        float scaleY = photoData.photoHeight * 1.0f / mScaleImageView.getHeight();

        // 横向图片
        if (photoData.photoWidth > photoData.photoHeight) {
            // 图片的宽高比
            float photoScale = photoData.photoWidth * 1.0f / photoData.photoHeight;
            // 执行动画的ImageView宽高比
            float animationImageScale = mScaleImageView.getWidth() * 1.0f / mScaleImageView.getHeight();

            if (animationImageScale > photoScale) {
                // 动画ImageView宽高比大于图片宽高比的时候，需要用图片的高度除以动画ImageView高度的比例尺
                scale = scaleY;
            }
            else {
                scale = scaleX;
            }
        }
        // 正方形图片
        else if (photoData.photoWidth == photoData.photoHeight) {
            if (mScaleImageView.getWidth() > mScaleImageView.getHeight()) {
                scale = scaleY;
            }
            else {
                scale = scaleX;
            }
        }
        // 纵向图片
        else {
            scale = scaleY;
        }
        return scale;
    }
```  
3.4 执行动画的缩放
```
 /**
     * 执行缩放动画
     * @param scaleImageView
     * @param translationX
     * @param translationY
     * @param scale
     * @param isEnlarge
     */
    private void executeZoom(final ImageView scaleImageView, int translationX, int translationY, float scale, boolean isEnlarge, Animator.AnimatorListener listener) {
        float startTranslationX, startTranslationY, endTranslationX, endTranslationY;
        float startScale, endScale, startAlpha, endAlpha;

        // 放大
        if (isEnlarge) {
            startTranslationX = translationX;
            endTranslationX = 0;
            startTranslationY = translationY;
            endTranslationY = 0;
            startScale = scale;
            endScale = 1;
            startAlpha = 0f;
            endAlpha = 0.75f;
        }
        // 缩小
        else {
            startTranslationX = 0;
            endTranslationX = translationX;
            startTranslationY = 0;
            endTranslationY = translationY;
            startScale = 1;
            endScale = scale;
            startAlpha = 0.75f;
            endAlpha = 0f;
        }

        //-------缩小动画--------
        AnimatorSet set = new AnimatorSet();
        set.play(
                ObjectAnimator.ofFloat(scaleImageView, "translationX", startTranslationX, endTranslationX))
                .with(ObjectAnimator.ofFloat(scaleImageView, "translationY", startTranslationY, endTranslationY))
                .with(ObjectAnimator.ofFloat(scaleImageView, "scaleX", startScale, endScale))
                .with(ObjectAnimator.ofFloat(scaleImageView, "scaleY", startScale, endScale))
                // ---Alpha动画---
                // mMaskView伴随着一个Alpha减小动画
                .with(ObjectAnimator.ofFloat(maskView, "alpha", startAlpha, endAlpha));
        set.setDuration(animDuration);
        if (listener != null) {
            set.addListener(listener);
        }
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }
```  
> 改View的主要实现如上，在图片进行缩放的时候，要考虑的情况：短边适配、图片原尺寸的宽高、展示图片的ImageView的宽高比、横竖屏时屏幕的尺寸。在此非常感谢震哥的帮助、抱拳了！老铁。如有更多想法的小伙伴。请移步我的github  [GalleryView地址](https://github.com/cedear/GalleryView)
