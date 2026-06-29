package com.Saalai.SalaiMusicApp;


import android.util.Base64;

public class CodeEncoder {

    public static String encode(String input) {

        String base64 = Base64.encodeToString(
                input.getBytes(),
                Base64.NO_WRAP
        );

        StringBuilder binary = new StringBuilder();

        for (char c : base64.toCharArray()) {
            String bin = String.format("%8s",
                    Integer.toBinaryString(c)).replace(' ', '0');
            binary.append(bin);
        }

        return "11111111" + binary.toString() + "00000000";
        // start pattern + data + end pattern
    }
}
