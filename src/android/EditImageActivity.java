package com.nimble.qualityinspection.uat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;


public class EditImageActivity extends BaseActivity implements OnPhotoEditorListener,
        View.OnClickListener {
    private static final String TAG = EditImageActivity.class.getSimpleName();
    private static final int CAMERA_REQUEST = 52;
    private static final int PICK_REQUEST = 53;
    private PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;
    private String imageUrl;
    private ImageView lineColorView, textColorView;
    public static final int DRAW_TYPE_LINE = 0, DRAW_TYPE_TEXT = 1;
    private int drawType = DRAW_TYPE_LINE;

    public static final int SELECTED_COLOR_1 = 1, SELECTED_COLOR_2 = 2, SELECTED_COLOR_3 = 3, SELECTED_COLOR_4 = 4;
    private int selectedColor = SELECTED_COLOR_2;
    private int textColor = R.color.brush_color_2;
    private Uri uri;
    private int destType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        makeFullScreen();
        setContentView(R.layout.activity_edit_image);

        initViews();

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true) // set flag to make text scalable when pinch
                .build(); // build photo editor sdk

        mPhotoEditor.setOnPhotoEditorListener(this);

        mPhotoEditor.clearAllViews();
        imageUrl = getIntent().getExtras().getString("imageUrl");
        uri = getIntent().getParcelableExtra("uri");
        destType = getIntent().getIntExtra("destType", 0);
        String photoAddress = "", date = "";
        Geocoder geocoder = new Geocoder(this);
        try {
            Matrix mat = new Matrix();
            Bitmap bitmap = BitmapFactory.decodeFile(imageUrl);
            ExifInterface ei = new ExifInterface(imageUrl);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                mat.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                mat.postRotate(180);

            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                mat.postRotate(270);

            }
            Bitmap photo = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
            mPhotoEditorView.getSource().setImageBitmap(photo);
            mPhotoEditor.setBrushDrawingMode(true);
            mPhotoEditor.setBrushColor(getResources().getColor(textColor));
            float[] latLong = new float[2];
            boolean hasAddressInfo = ei.getLatLong(latLong);
            String dateTime = ei.getAttribute(ExifInterface.TAG_DATETIME);
            if (!TextUtils.isEmpty(dateTime)) {
                if (dateTime.length() >= 16) {
                    dateTime = dateTime.substring(0, 4) + "-" + dateTime.substring(5, 7) + "-" + dateTime.substring(8, 16);
                }
                date = dateTime;
            }

            if (hasAddressInfo) {
                List<Address> addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1);
                for (Address address : addresses) {
                    if ("北京市".equals(address.getLocality()) || "天津市".equals(address.getLocality()) ||
                            "重庆市".equals(address.getLocality()) || "上海市".equals(address.getLocality())) {
                        photoAddress = address.getLocality();
                    } else {
                        photoAddress = address.getAdminArea() + address.getLocality();
                    }
                }
            }
            Log.e("address", date + " " + photoAddress);
            //2019:04:16 17:16
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View textRootView = inflater.inflate(R.layout.mark_for_image, null);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_BOTTOM, 1);
            final TextView textView = textRootView.findViewById(R.id.textView);
            textView.setText(date + " " + photoAddress);
            mPhotoEditorView.addView(textRootView, params);
        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.color1).setOnClickListener(this);
        findViewById(R.id.color2).setOnClickListener(this);
        findViewById(R.id.color3).setOnClickListener(this);
        findViewById(R.id.color4).setOnClickListener(this);
        lineColorView = findViewById(R.id.lineColorView);
        lineColorView.setOnClickListener(this);
        textColorView = findViewById(R.id.textColorView);
        textColorView.setOnClickListener(this);
    }

    private void initViews() {
        TextView imgUndo;
        TextView imgSave;
        mPhotoEditorView = findViewById(R.id.photoEditorView);
        imgUndo = findViewById(R.id.imgUndo);
        imgUndo.setOnClickListener(this);
        imgSave = findViewById(R.id.imgSave);
        imgSave.setOnClickListener(this);

    }

    @Override
    public void onEditTextChangeListener(final View rootView, String text, int colorCode) {
        TextEditorDialogFragment textEditorDialogFragment =
                TextEditorDialogFragment.show(this, text, colorCode);
        textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
            @Override
            public void onDone(String inputText, int colorCode) {
                mPhotoEditor.editText(rootView, inputText, colorCode);
            }
        });
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onAddViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onRemoveViewListener(ViewType viewType, int numberOfAddedViews) {
        Log.d(TAG, "onRemoveViewListener() called with: viewType = [" + viewType + "], numberOfAddedViews = [" + numberOfAddedViews + "]");
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [" + viewType + "]");
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.imgUndo) {
            mPhotoEditor.undo();

        } else if (i == R.id.imgSave) {
            saveImage();

        } else if (i == R.id.lineColorView) {
            drawType = DRAW_TYPE_LINE;
            textColorView.setImageResource(R.drawable.text_unselected);
            if (selectedColor == SELECTED_COLOR_1) {
                lineColorView.setImageResource(R.drawable.line_color_1);
            } else if (selectedColor == SELECTED_COLOR_2) {
                lineColorView.setImageResource(R.drawable.line_color_2);
            } else if (selectedColor == SELECTED_COLOR_3) {
                lineColorView.setImageResource(R.drawable.line_color_3);
            } else if (selectedColor == SELECTED_COLOR_4) {
                lineColorView.setImageResource(R.drawable.line_color_4);
            }

        } else if (i == R.id.textColorView) {
            drawType = DRAW_TYPE_TEXT;
            lineColorView.setImageResource(R.drawable.line_unselected);
            if (selectedColor == SELECTED_COLOR_1) {
                textColorView.setImageResource(R.drawable.text_color_1);
                textColor = R.color.brush_color_1;
            } else if (selectedColor == SELECTED_COLOR_2) {
                textColorView.setImageResource(R.drawable.text_color_2);
                textColor = R.color.brush_color_2;
            } else if (selectedColor == SELECTED_COLOR_3) {
                textColorView.setImageResource(R.drawable.text_color_3);
                textColor = R.color.brush_color_3;
            } else if (selectedColor == SELECTED_COLOR_4) {
                textColorView.setImageResource(R.drawable.text_color_4);
                textColor = R.color.brush_color_4;
            }
            TextEditorDialogFragment textEditorDialogFragment = TextEditorDialogFragment.show(this, textColor);
            textEditorDialogFragment.setOnTextEditorListener(new TextEditorDialogFragment.TextEditor() {
                @Override
                public void onDone(String inputText, int colorCode) {
                    mPhotoEditor.addText(inputText, colorCode);
                }
            });

        } else if (i == R.id.color1) {
            selectedColor = SELECTED_COLOR_1;
            mPhotoEditor.setBrushColor(getResources().getColor(R.color.brush_color_1));
            if (drawType == DRAW_TYPE_LINE) {
                lineColorView.setImageResource(R.drawable.line_color_1);
                textColorView.setImageResource(R.drawable.text_unselected);
            } else if (drawType == DRAW_TYPE_TEXT) {
                lineColorView.setImageResource(R.drawable.line_unselected);
                textColorView.setImageResource(R.drawable.text_color_1);
            }

        } else if (i == R.id.color2) {
            selectedColor = SELECTED_COLOR_2;
            mPhotoEditor.setBrushColor(getResources().getColor(R.color.brush_color_2));
            if (drawType == DRAW_TYPE_LINE) {
                lineColorView.setImageResource(R.drawable.line_color_2);
                textColorView.setImageResource(R.drawable.text_unselected);
            } else if (drawType == DRAW_TYPE_TEXT) {
                lineColorView.setImageResource(R.drawable.line_unselected);
                textColorView.setImageResource(R.drawable.text_color_2);
            }

        } else if (i == R.id.color3) {
            selectedColor = SELECTED_COLOR_3;
            mPhotoEditor.setBrushColor(getResources().getColor(R.color.brush_color_3));
            if (drawType == DRAW_TYPE_LINE) {
                lineColorView.setImageResource(R.drawable.line_color_3);
                textColorView.setImageResource(R.drawable.text_unselected);
            } else if (drawType == DRAW_TYPE_TEXT) {
                lineColorView.setImageResource(R.drawable.line_unselected);
                textColorView.setImageResource(R.drawable.text_color_3);
            }

        } else if (i == R.id.color4) {
            selectedColor = SELECTED_COLOR_4;
            mPhotoEditor.setBrushColor(getResources().getColor(R.color.brush_color_4));
            if (drawType == DRAW_TYPE_LINE) {
                lineColorView.setImageResource(R.drawable.line_color_4);
                textColorView.setImageResource(R.drawable.text_unselected);
            } else if (drawType == DRAW_TYPE_TEXT) {
                lineColorView.setImageResource(R.drawable.line_unselected);
                textColorView.setImageResource(R.drawable.text_color_4);
            }

        }
    }

    @SuppressLint("MissingPermission")
    private void saveImage() {
        if (requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showLoading("保存图片中...");
            File file = new File(imageUrl);
            try {
                file.createNewFile();

                SaveSettings saveSettings = new SaveSettings.Builder()
                        .setClearViewsEnabled(true)
                        .setTransparencyEnabled(true)
                        .build();

                mPhotoEditor.saveAsFile(file.getAbsolutePath(), saveSettings, new PhotoEditor.OnSaveListener() {
                    @Override
                    public void onSuccess(@NonNull String imagePath) {
                        hideLoading();
                        Toast.makeText(EditImageActivity.this, "保存图片成功", Toast.LENGTH_SHORT).show();
                        mPhotoEditorView.getSource().setImageURI(Uri.fromFile(new File(imagePath)));
                        Intent intent = new Intent();
                        intent.setData(uri);
                        intent.putExtra("destType", destType);
                        setResult(99, intent);
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        hideLoading();
                        Toast.makeText(EditImageActivity.this, "保存图片失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                hideLoading();
                Toast.makeText(EditImageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST:
                    mPhotoEditor.clearAllViews();
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    mPhotoEditorView.getSource().setImageBitmap(photo);
                    break;
                case PICK_REQUEST:
                    try {
                        mPhotoEditor.clearAllViews();
                        Uri uri = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        mPhotoEditorView.getSource().setImageBitmap(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    @Override
    public void isPermissionGranted(boolean isGranted, String permission) {
        if (isGranted) {
            saveImage();
        }
    }

}
