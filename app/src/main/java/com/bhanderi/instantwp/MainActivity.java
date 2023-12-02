package com.bhanderi.instantwp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bhanderi.instantwp.AdInfo;
import com.bhanderi.instantwp.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AdInfo adInfo = new AdInfo();
    private InterstitialAd interstitialAd;

    private EditText editTextPhoneNumber;
    private EditText editTextMessage;

    int impression = 0;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeAds();
        setWhatsAppClickListener();
    }

    private void initializeViews() {
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextMessage = findViewById(R.id.editTextMessage);
    }

    private void initializeAds() {
        MobileAds.initialize(this, initializationStatus -> {
            loadBannerAd();
            getAdId();
        });
    }

    private void loadBannerAd() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void getAdId() {
        db.collection("ads").document("nXLLdw7QzOLVW0VmUGbc").get()
                .addOnSuccessListener(this::loadInterstitialAd)
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching ad data from Firestore: " + e.getMessage()));
    }

    private void loadInterstitialAd(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot.exists()) {
            adInfo.setBannerId(documentSnapshot.getString("banner"));
            adInfo.setIntertialId(documentSnapshot.getString("inertial"));
            loadInterstitialAd(adInfo.getIntertialId());
        } else {
            Log.d(TAG, "No ad data found in Firestore");
        }
    }

    private void loadInterstitialAd(String interstitialAdId) {
        if (interstitialAdId != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(this, interstitialAdId, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAdLocal) {
                    interstitialAd = interstitialAdLocal;
                    Log.i(TAG, "Interstitial ad loaded successfully");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    interstitialAd = null;
                    Log.d(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                }
            });
        } else {
            Log.d(TAG, "Interstitial AdUnitId is null");
        }
    }

    private void setWhatsAppClickListener() {
        Button button = findViewById(R.id.openWhatsapp);
        button.setOnClickListener(v -> {
            if (interstitialAd != null) {
                interstitialAd.show(MainActivity.this);
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    // Implement FullScreenContentCallback methods...
                });
                openWhatsApp();
            } else {
                loadInterstitialAd(adInfo.getIntertialId());
                Log.d(TAG, "Interstitial ad is not ready yet");
                openWhatsApp();
            }
            loadInterstitialAd(adInfo.getIntertialId());
        });
    }

    private void openWhatsApp() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String message = editTextMessage.getText().toString();

        String encodedMessage = "";
        try {
            encodedMessage = URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if (isValidPhoneNumber(phoneNumber)) {
            String whatsappUrl = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + encodedMessage;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(whatsappUrl));
            startActivity(intent);

            Map<String, Object> number = new HashMap<>();
            number.put("contact", phoneNumber);

            db.collection("contacts").add(number).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "Number added successfully");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to add number: " + e.getMessage());
                }
            });
        } else {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return Patterns.PHONE.matcher(phoneNumber).matches();
    }
}
