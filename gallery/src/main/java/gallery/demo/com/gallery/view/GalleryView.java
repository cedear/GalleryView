package gallery.demo.com.gallery.view;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import gallery.demo.com.gallery.R;
import gallery.demo.com.gallery.adapter.GalleryPhotoAdapter;
import gallery.demo.com.gallery.model.GalleryPhotoModel;
import gallery.demo.com.gallery.model.GalleryPhotoParameterModel;
import gallery.demo.com.gallery.util.GalleryDownLoadCallBack;
import gallery.demo.com.gallery.util.GalleryDownloadThread;
import gallery.demo.com.gallery.util.GalleryScreenUtil;

/**
 * Created by bjhl on 2018/3/26.
 */

public class GalleryView extends RelativeLayout {

    private static final int DEFAULT_ANIM_DURATION = 300; // 毫秒

    // 用于缩放的ImageView
    private ImageView mScaleImageView = null;
    // 阴影背景
    private View maskView;
    // 屏幕的宽高
    private RectF mScreenRect = null;
    //viewpager滑到的位置
    private int position;
    private int animDuration;
    private int saveTextSize;
    private int saveTextColor;
    private String saveText;
    private ViewPager viewPager;
    private TextView tvPhotoSize;
    private TextView tvPhotoSave;
    private List<GalleryPhotoView> viewList;
    private List<GalleryPhotoModel> photoList;
    private GalleryPhotoParameterModel firstClickItemParameterModel;
    private GalleryPhotoAdapter adapter;
    private final static int WHAT_SAVE_SUCCESS = 1;
    private final static int WHAT_SAVE_FAILED = 2;
    private RxPermissions rxPermissions;


    private android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_SAVE_SUCCESS:
                    Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
                    break;
                case WHAT_SAVE_FAILED:
                    Toast.makeText(getContext(), "保存失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public GalleryView(Context context) {
        this(context, null);
    }

    public GalleryView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GalleryView);
            animDuration = typedArray.getInt(R.styleable.GalleryView_animDuration, DEFAULT_ANIM_DURATION);
            saveText = typedArray.getString(R.styleable.GalleryView_saveText);
            saveTextSize = typedArray.getDimensionPixelSize(R.styleable.GalleryView_saveTextSize, 0);
            saveTextColor = typedArray.getColor(R.styleable.GalleryView_saveTextColor, 0);
        }
        else {
            animDuration = DEFAULT_ANIM_DURATION;
        }

        setFocusable(true);
        setFocusableInTouchMode(true);
        initView();
        // 拦截单击事件，防止其他view被点击
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && getVisibility() == VISIBLE) {
            calculateScaleAndStartZoomOutAnim();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * @param index             想要展示的图片的索引值
     * @param photoList         图片集合（URL、Drawable、Bitmap）
     * @param clickImageView    点击的第一个图片
     */
    public void showPhotoGallery(int index, List<GalleryPhotoModel> photoList, ImageView clickImageView) {
        GalleryPhotoParameterModel photoParameter = new GalleryPhotoParameterModel();
        //图片地址
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

    /**
     * 设置缩放时动画持续时间
     * @param duration 毫秒
     */
    public void setAnimDuration(int duration) {
        animDuration = duration;
    }

    public void setSaveText(String text) {
        if (tvPhotoSave != null) {
            tvPhotoSave.setText(text);
        }
    }

    public void setSaveTextSize(int size, int... unit) {
        if (tvPhotoSave != null) {
            if (unit != null) {
                tvPhotoSave.setTextSize(unit[0], size);
            }
            else {
                tvPhotoSave.setTextSize(size);
            }
        }
    }

    public void setSaveTextColoc(@ColorInt int color) {
        if (tvPhotoSave != null) {
            tvPhotoSave.setTextColor(color);
        }
    }

    private void setGalleryPhotoList(List<GalleryPhotoModel> list, GalleryPhotoParameterModel parameterModel) {
//        boolean isFocus = requestFocus();
        if (list != null && parameterModel != null) {
            if (photoList != null && photoList.size() != 0) {
                photoList.clear();
            } else {
                photoList = new ArrayList<>();
            }
            photoList.addAll(list);
            //获取第一个被点击的图片的具体参数
            firstClickItemParameterModel = parameterModel;
            initData();
            //处理放大动画
            handleZoomAnimation();
        }
    }

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


    /**
     * 初始化视图
     */
    private void initView() {
        View view = View.inflate(getContext(), R.layout.gallery_view_main, this);
        maskView = view.findViewById(R.id.gallery_view_main_mask_View);
        mScaleImageView = (ImageView) view.findViewById(R.id.gallery_view_main_scale_imageView);
        viewPager = (ViewPager) view.findViewById(R.id.gallery_view_main_viewpager);
        tvPhotoSize = (TextView) view.findViewById(R.id.gallery_view_main_photo_size_tv);
        tvPhotoSave = (TextView) view.findViewById(R.id.gallery_view_main_photo_save);

        if (!TextUtils.isEmpty(saveText)) {
            tvPhotoSave.setText(saveText);
        }

        if (saveTextSize != 0) {
            tvPhotoSave.setTextSize(saveTextSize);
        }

        if (saveTextColor != 0) {
            tvPhotoSave.setTextColor(saveTextColor);
        }
    }

    /**
     * 初始化数据，设置事件监听
     */
    private void initData() {
        if (viewList != null && viewList.size() != 0) {
            viewList.clear();
        } else {
            viewList = new ArrayList<>();
        }
        for (int i = 0; i < photoList.size(); i++) {
            GalleryPhotoView galleryPhoto = new GalleryPhotoView(getContext(), photoList.get(i));
            viewList.add(galleryPhoto);
            galleryPhoto.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    //隐藏当前viewpager
                    calculateScaleAndStartZoomOutAnim();
                }
            });
        }
        adapter = new GalleryPhotoAdapter(viewList);
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                GalleryView.this.position = position;
                tvPhotoSize.setText(viewPager.getCurrentItem() + 1 + "/" + photoList.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setCurrentItem(firstClickItemParameterModel.index);

        tvPhotoSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context = getContext();
                if (context instanceof Activity) {
                    checkPermission((Activity) context);
                }
            }
        });
    }

    private void showOtherViews() {
        mScaleImageView.setVisibility(GONE);
        tvPhotoSave.setVisibility(VISIBLE);
        tvPhotoSize.setVisibility(VISIBLE);
        viewPager.setVisibility(VISIBLE);
    }

    private void hiedOtherViews() {
        mScaleImageView.setVisibility(VISIBLE);
        tvPhotoSave.setVisibility(GONE);
        tvPhotoSize.setVisibility(GONE);
        viewPager.setVisibility(GONE);
    }

    private void checkPermission(final Activity activity) {
        if (rxPermissions == null) {
            rxPermissions = new RxPermissions(activity);
        }
        if (rxPermissions.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            saveBitmapToLocation();
        } else {
            rxPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted) {
                            saveBitmapToLocation();
                        } else {
                            Toast.makeText(activity, R.string.gallery_save_picture_permission_request_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        }

    private void saveBitmapToLocation() {
        GalleryDownloadThread download = new GalleryDownloadThread(getContext(), photoList.get(position).photoSource, new GalleryDownLoadCallBack() {
            @Override
            public void onDownLoadSuccess() {
                handler.sendEmptyMessage(WHAT_SAVE_SUCCESS);
            }

            @Override
            public void onDownLoadFailed() {
                handler.sendEmptyMessage(WHAT_SAVE_FAILED);
            }
        });
        new Thread(download).start();
    }

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


}
