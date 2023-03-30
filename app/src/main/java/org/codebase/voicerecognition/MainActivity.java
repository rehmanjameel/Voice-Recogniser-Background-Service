package org.codebase.voicerecognition;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;
import net.gotev.speech.SpeechUtil;
import net.gotev.speech.SupportedLanguagesListener;
import net.gotev.speech.TextToSpeechCallback;
import net.gotev.speech.UnsupportedReason;
import net.gotev.speech.ui.SpeechProgressView;

import org.codebase.voicerecognition.services.VoiceBackgroundService;
import org.codebase.voicerecognition.utils.App;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SpeechDelegate {

    private final int PERMISSIONS_REQUEST = 1;
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static ImageButton button;
    private Button speak;
    private EditText text;
    private EditText textToSpeech;
    private static SpeechProgressView progress;
    private static LinearLayout linearLayout;
    private ImageButton selectLanguage, saveText;
    private TextView savedText;
    private CheckBox checkBox;
    private List<String> textStringList = new ArrayList<>();

    private TextToSpeech.OnInitListener mTttsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(final int status) {
            switch (status) {
                case TextToSpeech.SUCCESS:
                    Logger.info(LOG_TAG, "TextToSpeech engine successfully started");
                    break;

                case TextToSpeech.ERROR:
                    Logger.error(LOG_TAG, "Error while initializing TextToSpeech engine!");
                    break;

                default:
                    Logger.error(LOG_TAG, "Unknown TextToSpeech status: " + status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Speech.init(this, getPackageName(), mTttsInitListener);

        Speech.getInstance().setLocale(Locale.forLanguageTag(App.getStringFromSharedPreferences("save_lang")));
        linearLayout = findViewById(R.id.linearLayout);

        button = findViewById(R.id.button);
        button.setOnClickListener(view -> onButtonClick());

        speak = findViewById(R.id.speak);
        speak.setOnClickListener(view -> onSpeakClick());

        text = findViewById(R.id.text);
        textToSpeech = findViewById(R.id.textToSpeech);
        progress = findViewById(R.id.progress);

        selectLanguage = findViewById(R.id.selectLanguage);
        saveText = findViewById(R.id.addText);
        savedText = findViewById(R.id.savedText);

        checkBox = findViewById(R.id.startServiceChecked);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(this)) {
            askPermission();
        }

        // start foreground service for microphone
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    App.serviceRunning("voice_service", true);

                    if (!App.isMyServiceRunning(VoiceBackgroundService.class)) {
                        Intent intent = new Intent(App.getContext(), VoiceBackgroundService.class);
//                        intent.setAction("C.ACTION_START_SERVICE");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Log.e("service01", "how is it");
                            startForegroundService(intent);
                        } else {
                            startService(intent);
                        }
                    }

                } else {
                    App.serviceRunning("voice_service", false);
                    if (App.isMyServiceRunning(VoiceBackgroundService.class)) {
                        stopService(new Intent(App.getContext(), VoiceBackgroundService.class));
                        Speech.getInstance().stopListening();
//                        unregisterReceiver(new VoiceReceiver());
                    }
                }
            }
        });

        checkBox.setChecked(App.isServiceRunning("voice_service"));

        int[] colors = {
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.darker_gray),
                ContextCompat.getColor(this, android.R.color.black),
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        };
        progress.setColors(colors);

        selectLanguage.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= 33) {
                languagesAbove13();
            } else {
                onSetSpeechToTextLanguage();
            }
        });

        // show the last saved text from sharedpreferences after opening the app
        if (!App.getStringFromSharedPreferences("voice_text").isEmpty()) {
            savedText.setText(App.getStringFromSharedPreferences("voice_text"));
        }

        // set check box disabled if audio permission is not allowed
        if (ContextCompat.checkSelfPermission(App.getContext(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkBox.setChecked(false);
            checkBox.setClickable(false);
            checkBox.setEnabled(false);
        }

        // show voice text in textview
        saveText.setOnClickListener(view -> {
            if (!text.getText().toString().isEmpty()) {
//                textStringList.add(text.getText().toString());
//                App.storeDataArrayList2("voice_text", textStringList);
//                savedText.setText(""+App.getDataArrayList2("voice_text"));
                App.saveDataToSharedPreferences("voice_text", text.getText().toString());
                savedText.setText(App.getStringFromSharedPreferences("voice_text"));
                text.setText("");
            }
        });

        linearLayout.setOnClickListener(view -> {

            Speech.getInstance().stopListening();
            button.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);

        });

//        preventStatusBarExpansion(this);
    }

    // get the supported languages for user voice functionality
    // supported upto android 12.
    private void onSetSpeechToTextLanguage() {
        Speech.getInstance().getSupportedSpeechToTextLanguages(new SupportedLanguagesListener() {
            @Override
            public void onSupportedLanguages(List<String> supportedLanguages) {
                CharSequence[] items = new CharSequence[supportedLanguages.size()];
                supportedLanguages.toArray(items);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Current language: " + Locale.forLanguageTag(App.getStringFromSharedPreferences("save_lang")))
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Locale locale;

                                if (Build.VERSION.SDK_INT >= 25) {
                                    locale = Locale.forLanguageTag(supportedLanguages.get(i));
                                } else {
                                    String[] langParts = supportedLanguages.get(i).split("-");

                                    if (langParts.length >= 2) {
                                        locale = new Locale(langParts[0], langParts[1]);
                                    } else {
                                        locale = new Locale(langParts[0]);
                                    }
                                }

                                Speech.getInstance().setLocale(locale);
                                App.saveDataToSharedPreferences("save_lang", items[i].toString());
                                Toast.makeText(MainActivity.this, "Selected: " + items[i], Toast.LENGTH_LONG).show();
                            }
                        })
                        .setPositiveButton("Cancel", null)
                        .create()
                        .show();
            }

            @Override
            public void onNotSupported(UnsupportedReason reason) {
                switch (reason) {
                    case GOOGLE_APP_NOT_FOUND:
                        showSpeechNotSupportedDialog();
                        break;

                    case EMPTY_SUPPORTED_LANGUAGES:
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.set_stt_langs)
                                .setMessage(R.string.no_langs)
                                .setPositiveButton("OK", null)
                                .show();
                        break;
                }
            }
        });
    }

    public void languagesAbove13() {
        //String systemLocale = getString(R.string.system_locale);
        // static list for android 12+ you can use your own desired languages
        List<String> list = new ArrayList<String>();
        list.add("en-US");
        list.add("es-ES");
        list.add("iw-IL");
        list.add("ja-JP");
        list.add("hi-IN");
        final CharSequence[] items = list.toArray(new CharSequence[0]);

//        List<String> items = Arrays.asList("en-US", "es-ES", "iw-IL", "ja-JP", "uk-UA");

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Current language: " + Speech.getInstance().getSpeechToTextLanguage())
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Locale locale;

                        if (Build.VERSION.SDK_INT >= 25) {
                            locale = Locale.forLanguageTag(items[i].toString());
                        } else {
                            String[] langParts = items[i].toString().split("-");

                            if (langParts.length >= 2) {
                                locale = new Locale(langParts[0], langParts[1]);
                            } else {
                                locale = new Locale(langParts[0]);
                            }
                        }

                        Speech.getInstance().setLocale(locale);
                        App.saveDataToSharedPreferences("save_lang", items[i].toString());
                        Toast.makeText(MainActivity.this, "Selected: " + items[i], Toast.LENGTH_LONG).show();
                    }
                })
                .setPositiveButton("Cancel", null)
                .create()
                .show();

    }

    private void onSetTextToSpeechVoice() {
        List<Voice> supportedVoices = Speech.getInstance().getSupportedTextToSpeechVoices();

        if (supportedVoices.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.set_tts_voices)
                    .setMessage(R.string.no_tts_voices)
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Sort TTS voices
        Collections.sort(supportedVoices, (v1, v2) -> v1.toString().compareTo(v2.toString()));

        CharSequence[] items = new CharSequence[supportedVoices.size()];
        Iterator<Voice> iterator = supportedVoices.iterator();
        int i = 0;

        while (iterator.hasNext()) {
            Voice voice = iterator.next();

            items[i] = voice.toString();
            i++;
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Current: " + Speech.getInstance().getTextToSpeechVoice())
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Speech.getInstance().setVoice(supportedVoices.get(i));
                        Toast.makeText(MainActivity.this, "Selected: " + items[i], Toast.LENGTH_LONG).show();
                    }
                })
                .setPositiveButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Speech.getInstance().shutdown();
        Log.e("onDestroy", "is called now");
    }

    public void onButtonClick() {
        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
        } else {
            if (ContextCompat.checkSelfPermission(App.getContext(), android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onRecordAudioPermissionGranted();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                onRecordAudioPermissionGranted();
                checkBox.setClickable(true);
                checkBox.setEnabled(true);
            } else {
                // permission denied, boo!
                Toast.makeText(MainActivity.this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onRecordAudioPermissionGranted() {
        button.setVisibility(View.GONE);
        linearLayout.setVisibility(View.VISIBLE);

        try {
            Speech.getInstance().stopTextToSpeech();
            Speech.getInstance().startListening(progress, MainActivity.this);

        } catch (SpeechRecognitionNotAvailable exc) {
            showSpeechNotSupportedDialog();

        } catch (GoogleVoiceTypingDisabledException exc) {
            showEnableGoogleVoiceTyping();
        }
    }

    // this method is for the use of user will write text in box and library will speak that word,
    // for accessing this method set the visibility to visible of edittext and speak button at bottom
    // of screen
    private void onSpeakClick() {
        if (textToSpeech.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.input_something, Toast.LENGTH_LONG).show();
            return;
        }

        Speech.getInstance().say(textToSpeech.getText().toString().trim(), new TextToSpeechCallback() {
            @Override
            public void onStart() {
                Toast.makeText(MainActivity.this, "TTS onStart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompleted() {
                Toast.makeText(MainActivity.this, "TTS onCompleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError() {
                Toast.makeText(MainActivity.this, "TTS onError", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStartOfSpeech() {
    }

    @Override
    public void onSpeechRmsChanged(float value) {
        //Log.d(getClass().getSimpleName(), "Speech recognition rms is now " + value +  "dB");
    }

    @Override
    public void onSpeechResult(String result) {

        button.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);


        if (result.isEmpty()) {
            Speech.getInstance().say(getString(R.string.repeat));

        } else {
            text.setText(result);

            // this ".say" method line allows the app to speak in repeat as user speaks
//            Speech.getInstance().say(result);

//            if (App.getStringFromSharedPreferences("voice_text").contains(result)) {
//                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
//            }
        }
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {
        text.setText("");
        for (String partial : results) {
            text.append(partial + " ");
        }
    }

    private void showSpeechNotSupportedDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        SpeechUtil.redirectUserToGoogleAppOnPlayStore(MainActivity.this);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.speech_not_available)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener)
                .show();
    }


    private void showEnableGoogleVoiceTyping() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.enable_google_voice_typing)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                })
                .show();
    }

    public static void preventStatusBarExpansion(Context context) {
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager manager = ((WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));

        Activity activity = (Activity)context;

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = LAYOUT_FLAG;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                // this is to enable the notification to recieve touch events
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                // Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        int resId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int result = 0;
        if (resId > 0) {
            result = activity.getResources().getDimensionPixelSize(resId);
        }

        localLayoutParams.height = result;

        localLayoutParams.format = PixelFormat.TRANSPARENT;

        customViewGroup view = new customViewGroup(context);

        manager.addView(view, localLayoutParams);
    }

    @TargetApi(23)
    public void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 101);
    }

    public static class customViewGroup extends ViewGroup {

        public customViewGroup(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.v("customViewGroup", "**********Intercepted");
            return true;
        }
    }
}