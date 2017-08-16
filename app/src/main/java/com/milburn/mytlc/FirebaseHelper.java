package com.milburn.mytlc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

public class FirebaseHelper {
    private Context context;
    private SharedPreferences sharedPreferences;
    private String issueText;
    private View v;

    public FirebaseHelper(Context con) {
        context = con;
        sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void uploadFiles(List<String[]> htmlList, List<Shift> shiftList, String issue) {
        int i = 0;
        FileOutputStream outputStream;
        for (String[] s : htmlList) {
            try {
                File file = new File(context.getFilesDir(), s[0]);
                outputStream = context.openFileOutput(s[0], Context.MODE_PRIVATE);
                outputStream.write(s[1].getBytes());
                outputStream.close();

                Uri path = Uri.fromFile(file);
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageReference = storage.getReference(getUUID() + "/" + s[0]);
                UploadTask uploadTask = storageReference.putFile(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String issueName = "Issue.txt";
        File file = new File(context.getFilesDir(), issueName);
        try {
            outputStream = context.openFileOutput(issueName, Context.MODE_PRIVATE);
            outputStream.write(issue.getBytes());
            outputStream.write("\n\n---------------------\n\n".getBytes());
            outputStream.write(shiftList.toArray().toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri path = Uri.fromFile(file);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference(getUUID() + "/" + issueName);
        UploadTask uploadTask = storageReference.putFile(path);

        Toast.makeText(context, "Issue sent", Toast.LENGTH_LONG);
    }

    public void sendIssue(final List<String[]> htmlList, final List<Shift> shiftList) {
        v = LayoutInflater.from(context).inflate(R.layout.dialog_issue, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(v);
        builder.setTitle("Issue report");
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                issueText = v.findViewById(R.id.textview_issue).toString();
                uploadFiles(htmlList, shiftList, issueText);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //
            }
        });
        builder.create();
        builder.show();
    }

    private String getUUID() {
        String uuid;

        if (sharedPreferences.contains("UUID")) {
            uuid = sharedPreferences.getString("UUID", "null");
        } else {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit()
                    .putString("UUID", uuid)
                    .apply();
        }
        return uuid;
    }

    public void setReport(Boolean bool) {
        sharedPreferences.edit()
                .putBoolean("report", bool)
                .apply();
    }

    public Boolean getReport() {
        return sharedPreferences.getBoolean("report", false);
    }
}
