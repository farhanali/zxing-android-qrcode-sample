package in.farhanali.qrreadersample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.FileNotFoundException;
import java.io.InputStream;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Ref: https://github.com/dm77/barcodescanner?utm_source=android-arsenal.com&utm_medium=referral&utm_campaign=387
 */
public class QrScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    public static final String TAG = "QrScannerActivity";

    private static final int SELECT_IMAGE_REQUEST = 100;

    private ZXingScannerView scannerView;

    // TODO: IMPORTANT
    // Require camera permission - Confirm camera permission is acquired during runtime
    // before loading this activity.
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        scannerView = (ZXingScannerView) findViewById(R.id.content_frame);
    }

    @Override
    public void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        toastMessage("Contents = " + rawResult.getText() +
                ", Format = " + rawResult.getBarcodeFormat().toString());

        // Note:
        // * Wait 2 seconds to resume the preview.
        // * On older devices continuously stopping and resuming camera preview can result in freezing the app.
        // * I don't know why this is the case but I don't have the time to figure out.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scannerView.resumeCameraPreview(QrScannerActivity.this);
            }
        }, 2000);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_IMAGE_REQUEST) {
                Uri imageUri = data.getData();

                try {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);
                    Bitmap resizedBitmap = resizeBitmap(imageBitmap, 500, 500);
                    String qrContent = readQrImage(imageBitmap);
                    toastMessage("QR Image Content: " + qrContent);
                } catch (FileNotFoundException e) {
                    toastMessage("Unable to open image");
                    e.printStackTrace();
                }
            }
        }
    }

    void onGalleryClick(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE_REQUEST);
    }

    void onRecentClick(View view) {
        toastMessage("Todo: Recent QR's");
    }

    void onFlashClick(View view) {
        scannerView.setFlash(!scannerView.getFlash());
    }

    private String readQrImage(Bitmap bMap) {
        String contents = null;

        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();// use this otherwise ChecksumException
        try {
            Result result = reader.decode(bitmap);
            contents = result.getText();
            //byte[] rawBytes = result.getRawBytes();
            //BarcodeFormat format = result.getBarcodeFormat();
            //ResultPoint[] points = result.getResultPoints();
        } catch (NotFoundException | FormatException | ChecksumException e) {
            e.printStackTrace();
            toastMessage("Error decoding barcode");
        }

        return contents;
    }

    private Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        Log.v(TAG, "Before scaling: " + width + " x " + height);

        if (width > height) {
            // landscape
            float ratio = (float) width / maxWidth;
            width = maxWidth;
            height = (int)(height / ratio);
        } else if (height > width) {
            // portrait
            float ratio = (float) height / maxHeight;
            height = maxHeight;
            width = (int)(width / ratio);
        } else {
            // square
            height = maxHeight;
            width = maxWidth;
        }

        Log.v(TAG, "After scaling: " + width + " x " + height);
        image = Bitmap.createScaledBitmap(image, width, height, true);

        return image;
    }

    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
