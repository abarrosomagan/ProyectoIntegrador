package com.sazon.proyectointegrador.util;

import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Picker de foto de perfil. Para mantener el proyecto en opciones gratuitas,
 * comprime la imagen y guarda el resultado en /users/{uid}.avatarUrl.
 *
 * Uso:
 *   AvatarHelper helper = AvatarHelper.attach(activity, uri -> { ... });
 *   helper.launchPicker();
 */
public class AvatarHelper {

    public interface OnAvatarReady {
        /** url puede ser null si la subida falló o el usuario canceló. */
        void onResult(@Nullable String avatarUrl);
    }

    private final AppCompatActivity activity;
    private final OnAvatarReady callback;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker;

    private AvatarHelper(AppCompatActivity activity, OnAvatarReady callback) {
        this.activity = activity;
        this.callback = callback;

        // El picker hay que registrarlo antes de STARTED, por eso `attach` se
        // llama desde onCreate (a través de los controllers).
        this.picker = activity.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) {
                        if (callback != null) callback.onResult(null);
                        return;
                    }
                    saveAvatar(uri);
                });
    }

    public static AvatarHelper attach(@NonNull AppCompatActivity activity,
                                      @NonNull OnAvatarReady callback) {
        return new AvatarHelper(activity, callback);
    }

    public void launchPicker() {
        picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void saveAvatar(@NonNull Uri uri) {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            Toast.makeText(activity, "Sesión expirada", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onResult(null);
            return;
        }

        String imageValue = RecipeImageHelper.toFirestoreImage(
                activity.getContentResolver(), uri);
        if (imageValue == null) {
            Toast.makeText(activity, "La foto es demasiado grande",
                    Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onResult(null);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", imageValue);
        SessionManager.updateUserDoc(uid, updates,
                ok -> {
                    Toast.makeText(activity, "Foto de perfil actualizada",
                            Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onResult(imageValue);
                },
                err -> {
                    Toast.makeText(activity, "No se pudo guardar la foto",
                            Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onResult(null);
                });
    }
}
