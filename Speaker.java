package com.kalom.unipismartalert;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;

import java.util.Locale;

class Speaker {

    private TextToSpeech tts;
    private TextToSpeech.OnInitListener initListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.ENGLISH);
        }
    };

    Speaker(Context context) {
        tts = new TextToSpeech(context, initListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void speak(String s) {
        tts.speak(s, TextToSpeech.QUEUE_ADD, null, null);
    }
}
