package com.example.yunli.hackthon_yun;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.android.gms.maps.GoogleMap;
import com.shopcurbside.curbsidesdk.CSErrorCode;
import com.shopcurbside.curbsidesdk.CSMobileSession;
import com.shopcurbside.curbsidesdk.CSSite;
import com.shopcurbside.curbsidesdk.credentialProvider.TokenCurbsideCredentialProvider;
import com.shopcurbside.curbsidesdk.event.Event;
import com.shopcurbside.curbsidesdk.event.Path;
import com.shopcurbside.curbsidesdk.event.Status;
import com.shopcurbside.curbsidesdk.event.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import rx.functions.Action1;

public class MapsActivity extends AppCompatActivity {

    private GoogleMap mMap;
    private Button startTracking = null;
    private Button notifyAssociate = null;
    private Button cancelTracking = null;
    private Button stopTracking = null;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MAPS ACTIVILY";
    private static final String SITE_ID = "yun_6315";
    private static final List<String> trackTokens = Arrays.asList("ccaldfkaj");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Request for location permissions
        if (ActivityCompat.checkSelfPermission(this /*context*/, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this /*context*/, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE /*int requestCode*/);
        }

        String USAGE_TOKEN = "";
        CSMobileSession.init(this /*context*/, new TokenCurbsideCredentialProvider(USAGE_TOKEN));

        CSMobileSession.getInstance().registerTrackingIdentifier("yun_li");
        setupUI();
    }

    public void setupUI() {
        startTracking = (Button) findViewById(R.id.startTracking);
        startTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTracking();
            }
        });

        notifyAssociate = (Button) findViewById(R.id.notifyAssociate);
        notifyAssociate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyAssociate();
            }
        });

        stopTracking = (Button) findViewById(R.id.stopTracking);
        stopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTracking();
            }
        });

        cancelTracking = (Button) findViewById(R.id.cancelTracking);
        cancelTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelTracking();
            }
        });
    }

    // Request permissions results
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                Arrays.sort(grantResults);
                if (Arrays.binarySearch(grantResults, PackageManager.PERMISSION_DENIED) >= 0) {
                    //Location permission not given by the user. Can try showing rationale to the user.
                } else {
                    //Location permissions given by the user.
                }
                break;
        }
    }

    public void startTracking() {
        CSSite site = new CSSite(SITE_ID, trackTokens);

        final Action1<Event> trackingInfoEventObserver = new Action1<com.shopcurbside.curbsidesdk.event.Event>() {
            @Override
            public void call(com.shopcurbside.curbsidesdk.event.Event event) {
                if (event.status == Status.FAILURE) {
                    Log.d(TAG, String.format("Failure in start track due to: %s", (CSErrorCode)event.object));
                }else if (event.status == Status.SUCCESS) {
                    Log.d(TAG, String.format("Success in tracking"));
                }else if (event.status == Status.COMPLETED) {
                    Log.d(TAG, String.format("Completed trip"));
                }
            }
        };
        CSMobileSession.getInstance().startTrackingSite(site);

        CSMobileSession.getInstance().getEventBus().getObservable(Path.USER, Type.START_TRACKING)
                .subscribe(trackingInfoEventObserver);
    }

    public void notifyAssociate() {
        // Check if user is near to any site to notify the site associate of that site
        Set<String> siteIdentifiers = CSMobileSession.getInstance().getSitesToNotifyOpsOfArrival();
        if (siteIdentifiers.size() > 0) {
            //We can notify the associates of these sites
            String trackingIdentifier = CSMobileSession.getInstance().getTrackingIdentifier();
            for (String siteIdentifier : siteIdentifiers) {
                CSMobileSession.getInstance().getNotifyAssociateManager().notifyAssociate(trackingIdentifier,
                        siteIdentifier);
            }
            Log.d(TAG, String.format("Successfully Notified Associate"));
        } else {
            //User is not near to any site
            Location current_location = CSMobileSession.getInstance().getCurrentLocation();
            Log.d(TAG, String.format("Not arrived, current location: " + current_location.toString() ));
        }

    }

    public void stopTracking() {
        CSSite site = new CSSite(SITE_ID);
        CSMobileSession.getInstance().stopTrackingSite(site);
        Log.d(TAG, String.format("Stopped tracking"));
    }

    public void cancelTracking() {
        CSSite site = new CSSite(SITE_ID, trackTokens);
        CSMobileSession.getInstance().cancelTrackingSite(site);
        Log.d(TAG, String.format("Cancelled tracking"));
    }


}
