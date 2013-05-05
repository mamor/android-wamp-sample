package net.madroom.wampsample;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.tavendo.autobahn.Autobahn;
import de.tavendo.autobahn.AutobahnConnection;

public class MainActivity extends Activity {

    /**
     * WAMPコネクション用
     */
    private static final String WAMP_URI = "ws://fuelratchet.madroom.org/socket_wamp_api";
    private AutobahnConnection mConnection;

    /**
     * PubSub用
     */
    private Spinner mTopicSpinner;
    private Spinner mPubsubSpinner;
    private EditText mMessageField;
    private Button mPubsubButton;

    /**
     * RPC用
     */
    private Spinner mMethodSpinner;
    private Button mRpcButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * WAMPコネクションの初期化
         */
        mConnection = new AutobahnConnection();

        mConnection.connect(WAMP_URI, new Autobahn.SessionHandler() {

            @Override
            public void onOpen() {
                Log.d("APP", "Connected!");
            }

            @Override
            public void onClose(int code, String reason) {
                Log.d("APP", "Connection closed:");
                Log.d("APP", "code => " + code);
                Log.d("APP", "reason => " + reason);
            }

        });

        /**
         * PubSubの初期化
         */
        mTopicSpinner = (Spinner) findViewById(R.id.topic_spinner);
        mPubsubSpinner = (Spinner) findViewById(R.id.pubsub_spinner);
        mMessageField = (EditText) findViewById(R.id.message_field);
        mPubsubButton = (Button) findViewById(R.id.pubsub_button);

        mPubsubButton.setOnClickListener(new Button.OnClickListener() {  

            public void onClick(View v) {
                final String topic = mTopicSpinner.getSelectedItem().toString();
                final String pubsub = mPubsubSpinner.getSelectedItem().toString();

                /**
                 * 購読
                 * 
                 * TODO: 購読解除後に再購読してもonEventに入らない
                 * https://github.com/tavendo/AutobahnAndroid/issues/24
                 */
                if (pubsub.equals("subscribe")) {
                    Log.d("APP", "Subscribe:");
                    Log.d("APP", "Topic => " + topic);

                    mConnection.subscribe(topic, String.class,
                        new Autobahn.EventHandler() {
                            @Override
                            public void onEvent(String topic, Object event) {
                                Log.d("APP", "Received:");
                                Log.d("APP", "Topic => " + topic);
                                Log.d("APP", "Event => " + event);
                            }
                        }
                    );

                /**
                 * 購読解除
                 */
                } else if (pubsub.equals("unsubscribe")) {
                    Log.d("APP", "Unsubscribe:");
                    Log.d("APP", "Topic => " + topic);
                    mConnection.unsubscribe(topic);

                /**
                 * 配信
                 */
                } else if (pubsub.equals("publish")) {

                    final String message = mMessageField.getText().toString().trim();

                    if (message.length() == 0) {
                        Toast.makeText(getApplicationContext(), "Message is empty.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("APP", "Publish:");
                        Log.d("APP", "Topic => " + topic);
                        Log.d("APP", "Msg => " + message);

                        try {
                            final JSONObject json = new JSONObject("{\"msg\": \"" + message + "\"}");
                            mConnection.publish(topic, json.toString());
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }

                    mMessageField.setText("");

                } else {
                    //TODO:
                }
            }
        });

        /**
         * RPCの初期化
         */
        mMethodSpinner = (Spinner) findViewById(R.id.method_spinner);
        mRpcButton = (Button) findViewById(R.id.rpc_button);

        mRpcButton.setOnClickListener(new Button.OnClickListener() {  

            public void onClick(View v) {
                final String method = mMethodSpinner.getSelectedItem().toString();

                Log.d("APP", "RPC:");
                Log.d("APP", "Method => " + method);

                mConnection.call(method, Object.class,
                    new Autobahn.CallHandler() {

                        @Override
                        public void onResult(Object result) {
                            Log.d("APP", "RPC Result:");
                            Log.d("APP", "Result => " + result);
                        }

                        @Override
                        public void onError(String error, String info) {
                            Log.d("APP", "RPC Error:");
                            Log.d("APP", "Error: " + error);
                            Log.d("APP", "Info: " + info);
                        }
                    }
                );
            }

        });

        /**
         * ping送信
         * 
         * TODO: 放置するとコネクションが切れるので、暫定的な対策
         */
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("APP", "Send ping.");
                mConnection.call("ping", Object.class,
                    new Autobahn.CallHandler() {
                        @Override
                        public void onResult(Object result) {
                        }
                        @Override
                        public void onError(String error, String info) {
                        }
                    }
                );
            }
        }, 1000, 30000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override 
    protected void onDestroy() { 
        System.exit(0);
    }

}
