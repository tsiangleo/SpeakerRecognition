package com.github.tsiangleo.sr.client.util;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by tsiang on 2017/2/17.
 */

public class ImgUtil {

    public static Bitmap getBitmap(int[][] pixel){
        int rows = pixel.length;    //height
        int cols = pixel[0].length; //width

        Bitmap bitmap = Bitmap.createBitmap (cols,rows, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int d = pixel[i][j];
                //（24-31 位表示 alpha，16-23 位表示红色，8-15 位表示绿色，0-7 位表示蓝色）。
                // ARGB
                int argb = Color.argb(255,d,d,d);
                bitmap.setPixel(j,i,argb);
            }
        }
        return bitmap;
    }
}
