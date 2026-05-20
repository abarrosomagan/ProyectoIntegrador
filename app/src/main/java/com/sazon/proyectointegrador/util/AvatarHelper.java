package com.sazon.proyectointegrador.util;

import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Picker de foto de perfil + subida a Firebase Storage en /avatars/{uid}.jpg
 * Tras subirla, escribe el avatarUrl en /users/{uid}.
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
                    uploadAvatar(uri);
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

    private void uploadAvatar(@NonNull Uri uri) {
        String uid = SessionManager.currentUid();
        if (uid == null) {
            Toast.makeText(activity, "Sesión expirada", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onResult(null);
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("avatars/" + uid + ".jpg");

        Toast.makeText(activity, "Subiendo foto...", Toast.LENGTH_SHORT).show();

        ref.putFile(uri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            String url = downloadUri.toString();
                            // Guardamos la url en el doc del usuario
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("avatarUrl", url);
                            SessionManager.updateUserDoc(uid, updates,
                                    ok -> {
                                        if (callback != null) callback.onResult(url);
                                    },
                                    err -> {
                                        Toast.makeText(activity,
                                                "Subida ok pero no se guardó en el perfil",
                                                Toast.LENGTH_SHORT).show();
                                        if (callback != null) callback.onResult(url);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(activity, "No se pudo obtener la URL",
                                    Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onResult(null);
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Error subiendo la foto",
                            Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onResult(null);
                });
    }
}
