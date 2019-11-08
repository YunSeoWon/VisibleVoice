package com.example.visiblevoice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.visiblevoice.Activities.FileDownloadActivity;

import com.example.visiblevoice.Activities.FileListActivity;
import com.example.visiblevoice.Data.AppDataInfo;
import com.example.visiblevoice.db.AppDatabase;
import com.example.visiblevoice.db.CurrentDownloadDAO;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.Map;

public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    private String username;
    private SharedPreferences userData;
    private CurrentDownloadDAO currentDownloadDAO;
    private static final String TAG = "MyFirebaseMsgService";
    private SharedPreferences down;
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            /*if (*//* Check if data needs to be processed by long running job *//* true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }*/

        }

        // Check if message contains a notification payload.

        try{
            Log.d(TAG,"body 출력 : "+ remoteMessage.getData());
            sendNotification(remoteMessage.getData());
        }
        catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        userData = getSharedPreferences(AppDataInfo.Login.key, AppCompatActivity.MODE_PRIVATE);
        username = userData.getString(AppDataInfo.Login.userID, null);
        //TODO FCM 올시 json png m4a 디비에 넣기
        currentDownloadDAO = Room.databaseBuilder(getApplicationContext(), AppDatabase.class,"db-record" )
                .allowMainThreadQueries()   //Allows room to do operation on main thread
                .build()
                .getCurrentDownloadDAO();
       /* down = getSharedPreferences(AppDataInfo.File.key, AppCompatActivity.MODE_PRIVATE);
        SharedPreferences.Editor download = down.edit();
        download.putString(AppDataInfo.File.json, remoteMessage.getData().get("json"));
        download.putString(AppDataInfo.File.png, remoteMessage.getData().get("png"));
        download.commit();*/

       //TODO 수정한 내용 이후 music FCM 수정해야 제대로 돌아감.
        Log.d("currentDownloadDAO","넣기전 db record 수 : "+currentDownloadDAO.getNumberRecord());
        com.example.visiblevoice.models.CurrentDownload currentDownload = new com.example.visiblevoice.models.CurrentDownload();
        String jsonFileName = remoteMessage.getData().get("json");
        String musicFileNmae = remoteMessage.getData().get("music");//TODO 나중에 음성파일 추가하면 수정해야됨
        String pngFileName = remoteMessage.getData().get("png");
        String fileName = jsonFileName.substring(0,jsonFileName.length()-5);

        currentDownload.setFileName(fileName);
        currentDownload.setAudioPath(getFilesDir().getAbsolutePath() + "/" + username + "/" + musicFileNmae);
        currentDownload.setWordCloudPath(getFilesDir().getAbsolutePath() + "/" + username + "/" + pngFileName);
        currentDownload.setJsonPath(getFilesDir().getAbsolutePath() + "/" + username + "/"+jsonFileName);
        Log.d("currentDownloadDAO",currentDownload.getFileName()+"");
        Log.d("currentDownloadDAO",currentDownload.getAudioPath()+"");
        Log.d("currentDownloadDAO",currentDownload.getJsonPath()+"");
        Log.d("currentDownloadDAO",currentDownload.getWordCloudPath()+"");
        try{
            Log.d("currentDownloadDAO",currentDownloadDAO.getRecordJsonFileName(fileName));
        }catch (NullPointerException ne){
            ne.printStackTrace();
        }

        if(currentDownloadDAO.getRecordMusicFileName(currentDownload.fileName) == null)
            currentDownloadDAO.insert(currentDownload);
        Log.d("currentDownloadDAO",remoteMessage+"");
        Log.d("currentDownloadDAO",currentDownload.fileName+"");
        Log.d("currentDownloadDAO","넣은후 db record 수 : "+currentDownloadDAO.getNumberRecord());

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(Map<String,String > messageBody) {
        Log.d(TAG,"sendnotification body 출력 : "+messageBody);
        Intent intent = new Intent(this, FileDownloadActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.vv_logo)
                        .setContentTitle(getString(R.string.download_complete_title))
                        .setContentText(getString(R.string.download_complete_context))
                        .setVibrate(new long[]{1000, 1000})
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

}