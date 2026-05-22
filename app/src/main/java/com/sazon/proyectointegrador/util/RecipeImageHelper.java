package com.sazon.proyectointegrador.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public final class RecipeImageHelper {

    private static final String DATA_PREFIX = "data:image/jpeg;base64,";
    private static final int MAX_SIDE = 900;
    private static final int JPEG_QUALITY = 62;
    private static final int MAX_DATA_CHARS = 850_000;

    private RecipeImageHelper() {
    }

    @Nullable
    public static String toFirestoreImage(@NonNull ContentResolver resolver,
                                           @NonNull Uri uri) {
        try {
            Bitmap bitmap = decodeScaled(resolver, uri);
            if (bitmap == null) return null;

            int quality = JPEG_QUALITY;
            String encoded = encode(bitmap, quality);
            while (encoded.length() > MAX_DATA_CHARS && quality > 35) {
                quality -= 7;
                encoded = encode(bitmap, quality);
            }
            return encoded.length() <= MAX_DATA_CHARS ? encoded : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void loadInto(@NonNull ImageView target, @Nullable String imageValue) {
        if (imageValue == null || imageValue.trim().isEmpty()) {
            target.setImageDrawable(null);
            return;
        }

        if (imageValue.startsWith(DATA_PREFIX)) {
            byte[] bytes = Base64.decode(imageValue.substring(DATA_PREFIX.length()), Base64.DEFAULT);
            Glide.with(target.getContext())
                    .asBitmap()
                    .load(bytes)
                    .centerCrop()
                    .into(target);
            return;
        }

        Glide.with(target.getContext())
                .load(imageValue)
                .centerCrop()
                .into(target);
    }

    @Nullable
    private static Bitmap decodeScaled(@NonNull ContentResolver resolver,
                                       @NonNull Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);

        try (InputStream input = resolver.openInputStream(uri)) {
            Bitmap original = BitmapFactory.decodeStream(input, null, opts);
            if (original == null) return null;
            return scaleDown(original);
        }
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        while ((width / sample) > MAX_SIDE * 2 || (height / sample) > MAX_SIDE * 2) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap scaleDown(@NonNull Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longest = Math.max(width, height);
        if (longest <= MAX_SIDE) return bitmap;

        float ratio = MAX_SIDE / (float) longest;
        int scaledWidth = Math.max(1, Math.round(width * ratio));
        int scaledHeight = Math.max(1, Math.round(height * ratio));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
        if (scaled != bitmap) bitmap.recycle();
        return scaled;
    }

    private static String encode(@NonNull Bitmap bitmap, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        return DATA_PREFIX + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }
}
