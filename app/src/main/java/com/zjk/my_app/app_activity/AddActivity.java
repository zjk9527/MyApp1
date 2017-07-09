package com.zjk.my_app.app_activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zjk.my_app.R;
import com.zjk.my_app.constant.Constant;
import com.zjk.my_app.entity.Trends;
import com.zjk.my_app.entity.User;
import com.zjk.my_app.fragment.CollectiFragment;
import com.zjk.my_app.util.ImageCompressHelper;
import com.zjk.my_app.util.LogUtils;
import com.zjk.my_app.util.ToastUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UploadBatchListener;

/**
 * 添加圈子消息的界面
 *    camera_photo 为点击camera控件后的添加照片呈现的控件
 * */

public class AddActivity extends BaseActivity {
    @BindView(R.id.et_content) EditText mContent;//输入的内容
    @BindView(R.id.tv_tip) TextView mTip;//以输入的字数
    @BindView(R.id.layout_img) LinearLayout mLayoutImg;//点击上传照片的照片的容器
    @BindView(R.id.ed_phone) EditText mPhone;
   // @BindView(R.id.tv_publish) TextView mPublish;
    //@BindView(R.id.iv_back) TextView mBack;

    private android.support.v7.app.AlertDialog photoDialog;
    private ProgressDialog progressDialog;
    List<String> listImg = new ArrayList<>();
    public static final int CAMERA_REQUEST_CODE = 100;
    public static final int IMAGE_REQUEST_CODE = 101;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 10;

    private String tempFileCameraPath; // 临时拍照保存路径
    private File tempFileCamera; // 临时拍照file
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        mContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTip.setText(s.length() + "/64");
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    /**
     * 添加图片（加号图片） 点击事件
     */
    @OnClick(R.id.img_plus)
    public void onForget(View view) {
        if (listImg.size() == 1) {
            // 兼容的 Material Design AlertDialog
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setMessage("图片最多为一张")
                    // .setCancelable(false) // 设置点击Dialog以外的界面不消失，按返回键也不起作用
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }
        showDialog();
    }

    /**
     * 选择相机/相册的提示对话框
     */
    private void showDialog() {
        photoDialog = new android.support.v7.app.AlertDialog.Builder(this).create();
        photoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        photoDialog.show();
        Window window = photoDialog.getWindow();
        window.setContentView(R.layout.dialog_photo); // 修改整个dialog窗口的显示
        window.setGravity(Gravity.BOTTOM);

        WindowManager.LayoutParams lp = photoDialog.getWindow().getAttributes();
        DisplayMetrics dm = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(dm);
        lp.width = dm.widthPixels;
        photoDialog.getWindow().setAttributes(lp); // 设置宽度

        photoDialog.findViewById(R.id.btn_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toCamera();
            }
        });
        photoDialog.findViewById(R.id.btn_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toPicture();
            }
        });
        photoDialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoDialog.dismiss();
            }
        });
    }

    /**
     * 跳转相机
     */
    public void toCamera() {
        requestWESPermission(); // 安卓6.0以上需要申请权限
        photoDialog.dismiss();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // 调用系统的拍照功能
        // 指定调用相机拍照后照片的储存路径
        tempFileCameraPath = getTempPhotoFileName();
        tempFileCamera = new File(tempFileCameraPath);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFileCamera));
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    /**
     * 使用系统当前日期加以调整作为照片的名称
     */
    private String getTempPhotoFileName() {
        // 创建一个以当前时间为名称的文件
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss");
        return Environment.getExternalStorageDirectory() + "/" + dateFormat.format(date) + ".jpg";
    }

    /**
     * 跳转相册
     */
    private void toPicture() {
        photoDialog.dismiss();
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case IMAGE_REQUEST_CODE: // 相册数据
                // 做非空判断，当我们觉得不满意想重新剪裁的时候便不会报异常，下同
                if (data != null) {
                    // 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
                    ContentResolver resolver = getContentResolver();
                    Uri originalUri = data.getData(); // 获得图片的uri
                    String[] proj = {MediaStore.Images.Media.DATA};
                    // 好像是android多媒体数据库的封装接口，具体的看Android文档
                    Cursor cursor = getContentResolver().query(originalUri, proj, null, null, null);
                    // 按我个人理解 这个是获得用户选择的图片的索引值
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    // 将光标移至开头 ，这个很重要，不小心很容易引起越界
                    cursor.moveToFirst();
                    // 最后根据索引值获取图片路径
                    String path = cursor.getString(column_index);
                    listImg.add(ImageCompressHelper.getInstance().compressIMG(this,path));
                    // listImg.add(path);
                    updateImg();
                }
                break;
            case CAMERA_REQUEST_CODE: // 相机数据
                if(ImageCompressHelper.getInstance().compressIMG(this,tempFileCameraPath)!=null) {
                    listImg.add(ImageCompressHelper.getInstance().compressIMG(this,tempFileCameraPath));
                    // listImg.add(tempFileCameraPath);
                    updateImg();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 更新显示图片
     */
    private void updateImg() {
        mLayoutImg.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < listImg.size(); i++) {
            RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.layout_pic, null);
            ImageView imageView = (ImageView) layout.findViewById(R.id.img);
            ImageView imageClose = (ImageView) layout.findViewById(R.id.img_delete);
            imageClose.setTag(i);

            Glide.with(this).load(listImg.get(i)).into(imageView);
            imageClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Object obj = view.getTag();
                    int tag = Integer.parseInt(String.valueOf(obj));
                    listImg.remove(tag);
                    updateImg();
                }
            });
            mLayoutImg.addView(layout);

        }
    }
    /**
     * 发布新鲜事
     */
    @OnClick(R.id.tv_publish)
    public  void onClick1() {
        String commitContent = mContent.getText().toString().trim();

        String phone=mPhone.getText().toString().trim();
        if (TextUtils.isEmpty(commitContent)) {
            ToastUtils.showShort(this,"亲，随便说两句吧");
            return;
        }
        showPublishDialog();



        if (mLayoutImg == null) {
            publishWithoutFigure(commitContent,phone,null);
        } else {
            publish(commitContent,phone);
        }
    }

    /**
     * 发表带图片的新鲜事
     */
    private void publish(final String commitContent, final String phone ) {

        // 批量上传图片到Bomb
        BmobFile.uploadBatch((String[])listImg.toArray(new String[listImg.size()]), new UploadBatchListener() {
            @Override
            public void onSuccess(List<BmobFile> files, List<String> urls) {
                // 1.files-上传完成后的BmobFile集合，是为了方便大家对其上传后的数据进行操作，例如你可以将该文件保存到表中
                // 2.urls-上传文件的完整url地址
                if(urls.size()==listImg.size()){ // 如果数量相等，则代表文件全部上传完成
                    // do something
                    // LogUtils.i("JAVA", "URL:"+urls.toString());
                    publishWithoutFigure(commitContent,phone,files);
                }
            }
            @Override
            public void onError(int statuscode, String errormsg) {
                progressDialog.dismiss();
                LogUtils.i("JAVA", "错误码"+statuscode +",错误描述："+errormsg);
            }

            @Override
            public void onProgress(int curIndex, int curPercent, int total,int totalPercent) {
                // 1.curIndex--表示当前第几个文件正在上传
                // 2.curPercent--表示当前上传文件的进度值（百分比）
                // 3.total--表示总的上传文件数
                // 4.totalPercent--表示总的上传进度（百分比）
            }
        });
    }
    /**
     * 显示发布时的Dialog
     */
    private void showPublishDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("发布中...");
        progressDialog.setCancelable(false); // 设置点击Dialog以外的界面不消失，按返回键也不起作用
        progressDialog.show();
    }
    private void publishWithoutFigure(final String commitContent,final String phone, final List<BmobFile> files) {
        User user = BmobUser.getCurrentUser(User.class);
       Trends trends=new Trends();
        trends.setAuthor(user);
        trends.setContent(commitContent);
        trends.setPhone(phone);
        trends.setClasses(user.getClasses());
        trends.setImg(files.get(0));
        trends.save(new SaveListener<String>() {
            @Override
            public void done(String s, BmobException e) {
                progressDialog.dismiss();
                // 发布成功
                ToastUtils.showShort(getApplicationContext(),"发布成功");
                AddActivity.this.setResult(Constant.RESULT_UPDATE_INFO, new Intent());
                finish();
            }
        });
    }
    /**
     * 动态申请权限
     */
    private void requestWESPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                // 判断是否需要 向用户解释，为什么要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    ToastUtils.showShort(this,"Need write external storage permission.");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_BLUETOOTH_PERMISSION);
                return;
            } else {
            }
        } else {
        }
    }
@OnClick(R.id.iv_back)
public void onClick(){
    Intent intent=new Intent(AddActivity.this,CollectiFragment.class);
    onBackPressed();
}
    /**
     * 授权回调处理
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case REQUEST_BLUETOOTH_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 授权成功
                } else {
                    // 授权拒绝
                }
                break;
        }
    }
}
