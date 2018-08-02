package edu.uncw.signalflare;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private String guid;

    private static final String PREFS = "flare-preferences";

    private boolean isLocationPermitted;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);


        guid = settings.getString("guid", null);
        if(guid == null) {
            guid = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getString(R.string.guid_key), guid);
            editor.commit();
        }

        setContentView(R.layout.activity_main);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }


    public void clearClicked(View view) {
        RadioGroup type = findViewById(R.id.type);
        type.setBackgroundResource(0);
        type.clearCheck();
        findViewById(R.id.type_error_text).setVisibility(View.GONE);

        RadioGroup severity = findViewById(R.id.severity);
        severity.setBackgroundResource(0);
        severity.clearCheck();
        findViewById(R.id.severity_error_text).setVisibility(View.GONE);

    }

    public void reportClicked(View view) {

        boolean hasError = false;

        RadioGroup type = findViewById(R.id.type);
        if(type.getCheckedRadioButtonId() == -1) {
            type.setBackgroundResource(R.drawable.border);
            findViewById(R.id.type_error_text).setVisibility(View.VISIBLE);
            hasError = true;
        }
        else {
            type.setBackgroundResource(0);
            findViewById(R.id.type_error_text).setVisibility(View.GONE);
        }

        RadioGroup severity = findViewById(R.id.severity);
        if(severity.getCheckedRadioButtonId() == -1) {
            severity.setBackgroundResource(R.drawable.border);
            findViewById(R.id.severity_error_text).setVisibility(View.VISIBLE);
            hasError = true;
        }
        else {
            severity.setBackgroundResource(0);
            findViewById(R.id.severity_error_text).setVisibility(View.GONE);
        }

        if (!hasError) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.location_permission_request);
                    builder.create().show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
            } else {
                getLocation(this);
            }
        }

    }

    private void getLocation(final Context context) {
        mFusedLocationClient.getLastLocation()
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        TextView resultView = findViewById(R.id.reportResult);
                        resultView.setText(R.string.location_failure);
                        resultView.setTextColor(getResources().getColor(R.color.colorError));
                    }
                })
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            RequestQueue queue = Volley.newRequestQueue(context);
                            String url = getString(R.string.ENDPOINT_URL);
                            HashMap<String, String> params = new HashMap();
                            params.put("latitude", String.valueOf(location.getLatitude()));
                            params.put("longitude", String.valueOf(location.getLongitude()));
                            params.put("guid", guid);
                            int severity = ((RadioGroup)findViewById(R.id.severity)).getCheckedRadioButtonId();
                            switch(severity) {
                                case R.id.high:
                                    params.put("severity", "high");
                                    break;
                                case R.id.low:
                                    params.put("severity", "low");
                                    break;
                            }

                            int type = ((RadioGroup)findViewById(R.id.type)).getCheckedRadioButtonId();
                            switch(type) {
                                case R.id.fire:
                                    params.put("type", "fire");
                                    break;
                                case R.id.flood:
                                    params.put("type", "flood");
                                    break;
                            }

                            final Map<String, String> mparams = Collections.unmodifiableMap(params);

                            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url,
                                    new JSONObject(mparams),
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            TextView resultView = findViewById(R.id.reportResult);
                                            resultView.setText(R.string.report_post_success);
                                            resultView.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
                                            Log.i(TAG, response.toString());

                                            findViewById(R.id.severity_error_text).setVisibility(View.INVISIBLE);
                                            RadioGroup severity = findViewById(R.id.severity);
                                            severity.setBackgroundResource(0);
                                            severity.clearCheck();
                                            findViewById(R.id.type_error_text).setVisibility(View.INVISIBLE);

                                            RadioGroup type = findViewById(R.id.type);
                                            type.setBackgroundResource(0);
                                            type.clearCheck();

                                            Toast.makeText(context,  R.string.report_post_success, Toast.LENGTH_LONG).show();
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.e(TAG, error.toString());
                                            TextView resultView = findViewById(R.id.reportResult);
                                            resultView.setTextColor(getResources().getColor(R.color.colorError));

                                            if (error instanceof TimeoutError || error instanceof NoConnectionError || error instanceof
                                            NetworkError) {
                                                resultView.setText(R.string.report_error_network);
                                            } else if (error instanceof AuthFailureError) {
                                                resultView.setText(R.string.report_error_auth);
                                            } else if (error instanceof ServerError) {
                                                resultView.setText(R.string.report_error_server);
                                            } else if (error instanceof ParseError) {
                                                resultView.setText(R.string.report_error_parse);
                                            }
                                        }
                            }) {
                                @Override
                                public Map<String, String> getHeaders() throws AuthFailureError {
                                    Map<String, String> params = new HashMap<String, String>();
                                    params.put("Content-Type", "application/json");
                                    return params;
                                }
                            };
                            TextView resultView = findViewById(R.id.reportResult);
                            resultView.setText(R.string.report_post_in_progress);
                            resultView.setTextColor(getResources().getColor(android.R.color.tab_indicator_text));
                            queue.add(jsonRequest);
                        }
                        else {
                            TextView resultView = findViewById(R.id.reportResult);
                            resultView.setText(R.string.location_failure);
                            resultView.setTextColor(getResources().getColor(R.color.colorError));
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getLocation(this);
                } else {
                    // TODO:Disable the functionality that depends on this permission.
                }
                return;
            }
        }
    }
}

