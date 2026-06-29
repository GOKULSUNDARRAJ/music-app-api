package com.Saalai.SalaiMusicApp;


import android.util.Base64;

public class CodeDecoder {

    public static String decodeBinary(String binary) {

        if (!binary.contains("11111111")) return null;

        int start = binary.indexOf("11111111") + 8;
        int end = binary.lastIndexOf("00000000");

        if (end <= start) return null;

        String clean = binary.substring(start, end);

        StringBuilder base64 = new StringBuilder();

        for (int i = 0; i + 8 <= clean.length(); i += 8) {
            String byteStr = clean.substring(i, i + 8);
            int charCode = Integer.parseInt(byteStr, 2);
            base64.append((char) charCode);
        }

        try {
            byte[] decoded = Base64.decode(base64.toString(), Base64.NO_WRAP);
            return new String(decoded);
        } catch (Exception e) {
            return null;
        }
    }
}