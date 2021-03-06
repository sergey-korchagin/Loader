package com.parse.upload;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.parse.SendCallback;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by User on 10/12/2015.
 */

public class Upload extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    ImageView uploadImage;
    Button uploadBtn;
    Button selectBtn;
    Button deleteBtn;
    private final int REQUEST_CODE_FROM_GALLERY_IMAGE = 1;
    private final int REQUEST_CODE_HIGH_QUALITY_IMAGE = 2;
    private static final String IMAGE_DIRECTORY_NAME = "Hello Camera";
    Bitmap photo = null;
    ProgressDialog progressDialog;
    private static final String ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm";
    int newNum;
    ParseFile file;
    Button sendPush;
    ArrayList<String> deletedIds;
    boolean videoSelected = false;
    int orientation;
    private Uri mHighQualityImageUri = null;
    EditText pushText;
    TextView numOfPics;
    Spinner spinner;
    String imageType = "image";


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.upload_fragment, container, false);
        uploadImage = (ImageView) root.findViewById(R.id.uploadImage);
        uploadBtn = (Button) root.findViewById(R.id.buttonUpload);
        uploadBtn.setOnClickListener(this);
        selectBtn = (Button)root.findViewById(R.id.btnSelect);
        selectBtn.setOnClickListener(this);
        sendPush = (Button) root.findViewById(R.id.buttonPush);
        sendPush.setOnClickListener(this);
        pushText = (EditText) root.findViewById(R.id.pushText);

        numOfPics = (TextView)root.findViewById(R.id.txtQuantity);

        deleteBtn = (Button)root.findViewById(R.id.btnDelete);
        deleteBtn.setOnClickListener(this);
        deletedIds = new ArrayList<>();
        root.clearFocus();
        pushText.clearFocus();
        Utils.hideSoftKeyboard(getActivity(), root);
        setPictureNumber();

        spinner = (Spinner) root.findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getActivity(), R.array.ImageType, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter1);
        spinner.setOnItemSelectedListener(this);
        return root;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == selectBtn.getId()) {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickPhoto, REQUEST_CODE_FROM_GALLERY_IMAGE);
        } else if (v.getId() == uploadBtn.getId()) {
            if (file != null) {
                progressDialog = ProgressDialog.show(getActivity(), "", "Загружается");
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(true).setMessage("Уверен что хочешь залить именно эту картинку? И не забыл выбрать категорию?")
                        .setPositiveButton("Отвечаю!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ParseACL acl = new ParseACL();
                                acl.setPublicReadAccess(true);
                                acl.setPublicWriteAccess(true);
                                ParseObject recipe1 = new ParseObject("picture");
                                recipe1.put("mPicture", file);
                                recipe1.put("isBanner", imageType);
                                recipe1.put("likes", Utils.getRandomInt());
                                recipe1.put("pictureNum",newNum);
                                recipe1.setACL(acl);
                                recipe1.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            progressDialog.dismiss();
                                            Utils.showAlert(getActivity(), "", "Загрузилось на сервер! Спасибо!");
                                            uploadImage.setImageDrawable(getResources().getDrawable(R.drawable.q));
                                            setPictureNumber();
                                        } else {
                                            progressDialog.dismiss();
                                            Utils.showAlert(getActivity(), "ERROR!", e.getLocalizedMessage().toString());

                                        }
                                    }
                                });


                            }
                        })
                        .setNegativeButton("Нет, прогнал!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                Window window = alert.getWindow();
                window.setGravity(Gravity.CENTER);
                alert.show();

            } else {
                Utils.showAlert(getActivity(), "ERROR!!", "Надо выбрать фото биджо!!");
            }

        } else if (v.getId() == sendPush.getId()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(true).setMessage("Уверен что хочешь послать всем ПУШ?")
                    .setPositiveButton("Да!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String message = pushText.getText().toString();
                            if (message.equals("")) {
                                Utils.showAlert(getActivity(), "НАКОСЯЧИЛ", "Нельзя отослать пустой ПУШ!");
                            } else {
                               sendPushNotification(message);
                            }

                        }
                    })
                    .setNegativeButton("Нет, затупил!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            Window window = alert.getWindow();
            window.setGravity(Gravity.CENTER);
            alert.show();

        }else if(deleteBtn.getId() == v.getId()){
            deleteRows();
        }
    }

    public void deleteRows(){
        ParseQuery query = new ParseQuery("picture");
        query.addAscendingOrder("createdAt");
        query.setLimit(20);
        query.findInBackground(new FindCallback() {
            @Override
            public void done(List objects, ParseException e) {
            }

            @Override
            public void done(Object o, Throwable throwable) {
                if (o instanceof List) {
                  final  List<ParseObject> num = (List<ParseObject>) o;

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setCancelable(true).setMessage("Уверен что хочешь стереть 20?")
                            .setPositiveButton("Да!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for(int i=0; i<num.size();i++){
                                        deletedIds.add(num.get(i).getObjectId().toString());
                                        ParseObject.createWithoutData("picture", num.get(i).getObjectId().toString()).deleteEventually();
                                    }
                                    ParseACL acl = new ParseACL();
                                    acl.setPublicReadAccess(true);
                                    acl.setPublicWriteAccess(true);
                                    ParseObject deleted = new ParseObject("deletedIt");
                                    deleted.put("deletedItems", deletedIds);
                                    deleted.setACL(acl);

                                    deleted.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                Utils.showAlert(getActivity(), "Deleted", "deleted 20 last images");

                                            } else {
                                                Utils.showAlert(getActivity(), "ERROR!", e.getLocalizedMessage().toString());

                                            }
                                        }
                                    });

                                }
                            })
                            .setNegativeButton("Нет, затупил!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    AlertDialog alert = builder.create();
                    Window window = alert.getWindow();
                    window.setGravity(Gravity.CENTER);
                    alert.show();


                }
            }
        });
    }

public void setPictureNumber() {
    ParseQuery query = new ParseQuery("picture");
    query.addDescendingOrder("createdAt");
    query.setLimit(1);
    query.findInBackground(new FindCallback() {
        @Override
        public void done(List objects, ParseException e) {
        }

        @Override
        public void done(Object o, Throwable throwable) {
            if (o instanceof List) {
                List<ParseObject> num = (List<ParseObject>) o;
                if (num.get(0).get("pictureNum") != null) {
                    newNum = ((int) num.get(0).get("pictureNum")) + 1;
                   // numOfPics.setText(Integer.toString((newNum-1)));

                    ParseQuery query = new ParseQuery("picture");
                    query.countInBackground(new CountCallback() {
                        @Override
                        public void done(int count, ParseException e) {
                            numOfPics.setText(Integer.toString(count));
                        }
                    });
                }

            }
        }
    });

}



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            Bitmap imageBitmap = null;
            // Bitmap scaledBitmap = null;

            if (resultCode == getActivity().RESULT_OK) {
                if (requestCode == REQUEST_CODE_HIGH_QUALITY_IMAGE) {
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), mHighQualityImageUri);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String picturePath = mHighQualityImageUri.getPath(); //imageCursor.getString(fileColumnIndex);


                    try {
                        ExifInterface exifInterface = new ExifInterface(picturePath);
                        orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (Build.MANUFACTURER.equals("LGE") || (Build.MANUFACTURER.equals("Sony")) || Build.MANUFACTURER.equals("samsung")) {
                        Matrix matrix = new Matrix();
                        if (orientation == 6) {
                            matrix.postRotate(90);
                        } else if (orientation == 3) {
                            matrix.postRotate(180);
                        } else if (orientation == 8) {
                            matrix.postRotate(270);
                        }

                        imageBitmap = Bitmap.createBitmap(Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), true), 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);

                    }
                } else {

                    Uri selectedImage = data.getData();
                    if (!ifVideo(selectedImage)) {
                        videoSelected = false;
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};

                        Cursor cursor = getActivity().getContentResolver().query(
                                selectedImage, filePathColumn, null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        cursor.close();

                        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
                        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
                        bitmapFatoryOptions.inPurgeable = true;

                        imageBitmap = BitmapFactory.decodeFile(filePath, bitmapFatoryOptions);

                        try {

                            ExifInterface exifInterface = new ExifInterface(filePath);
                            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (Build.MANUFACTURER.equals("LGE") || (Build.MANUFACTURER.equals("Sony")) || Build.MANUFACTURER.equals("samsung")) {
                            Matrix matrix = new Matrix();

                            if (orientation == 6) {
                                matrix.postRotate(90);

                            } else if (orientation == 3) {
                                matrix.postRotate(180);

                            } else if (orientation == 8) {
                                matrix.postRotate(270);
                            }
                            imageBitmap = Bitmap.createBitmap(Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), true), 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);

                        }
                    } else {
                        videoSelected = true;
                        Utils.showAlert(getActivity(), "Error!", "Cannot load video!");
                    }
                }
                if (!videoSelected) {
                    photo = imageBitmap;
                    if (photo.getHeight() > 4095 || photo.getWidth() > 4095) {
                        double ratio = 1.0;
                        double tmpHeight = photo.getHeight();
                        double tmpWidth = photo.getWidth();
                        if (photo.getHeight() > photo.getWidth()) {
                            ratio = tmpHeight / tmpWidth;
                        } else if (photo.getWidth() > photo.getHeight()) {
                            ratio = tmpWidth / tmpHeight;
                        }
                        tmpHeight = photo.getHeight() / ratio;
                        tmpWidth = photo.getWidth() / ratio;
                        photo = getResizedBitmap(photo, (int) tmpWidth, (int) tmpHeight);
                    }


                    uploadImage.setImageBitmap(photo);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    //    Compress image to lower quality scale 1 - 100
                    photo.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                    byte[] image = stream.toByteArray();


                    String filename;
                    filename = getRandomString(12);
                    String full_name;
                    full_name = "app_" + filename+".jpeg";
                    // Create the ParseFile
                    file = new ParseFile(full_name, image);


                }
            }
        } catch (OutOfMemoryError e) {
            Utils.showAlert(getActivity(), "Error", "No memory!");
        }
    }

    public void onLargeImageCapture(View v, int photoId) {

        mHighQualityImageUri = generateTimeStampPhotoFileUri();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mHighQualityImageUri);
        startActivityForResult(intent, REQUEST_CODE_HIGH_QUALITY_IMAGE);

    }

    private Uri generateTimeStampPhotoFileUri() {

        Uri photoFileUri = null;
        File outputDir = getPhotoDirectory();
        if (outputDir != null) {
            Time t = new Time();
            t.setToNow();
            File photoFile = new File(outputDir, System.currentTimeMillis()
                    + ".jpg");
            photoFileUri = Uri.fromFile(photoFile);
        }
        return photoFileUri;
    }


    public boolean ifVideo(Uri uri) {
        if (uri.toString().contains("video")) {
            return true;
        }
        return false;
    }

    private File getPhotoDirectory() {
        File outputDir = null;
        String externalStorageStagte = Environment.getExternalStorageState();
        if (externalStorageStagte.equals(Environment.MEDIA_MOUNTED)) {
            File photoDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            outputDir = new File(photoDir, getString(R.string.app_name));
            if (!outputDir.exists())
                if (!outputDir.mkdirs()) {
                    Log.v("Test", "NO Direcrory");
                    outputDir = null;
                }
        }
        return outputDir;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
//        bm.recycle();
        return resizedBitmap;
    }


    private static String getRandomString(final int sizeOfRandomString) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public void sendPushNotification(String message){
        ParsePush push = new ParsePush();

        push.setChannel("photos");
   //     push.setMessage(message);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("title","Нафаня");
            jsonObject.put("alert",message);
            jsonObject.put("action","action.add.badge");
        }catch (Throwable e){
            e.printStackTrace();
        }

        push.setData(jsonObject);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Utils.showAlert(getActivity(), "Красавчег", "Все кто хотел получили ПУШ");
                } else {
                    Utils.showAlert(getActivity(), "Косяк на сервере", "Что то пошло не так!");
                }

            }

        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position == 0) {
                imageType = "image";
            } else {
                imageType = parent.getItemAtPosition(position).toString();

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        imageType = "image";
    }
}
