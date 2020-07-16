package com.util;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class TranslateText {
	public static String translate(String text) {
		Translate translate = TranslateOptions.getDefaultInstance().getService();
		
		Translation translation = translate.translate(
				text,
				TranslateOption.sourceLanguage("ja"),
				TranslateOption.targetLanguage("ko"));
		
		String result = translation.getTranslatedText();
		
		return result;
	}
}
