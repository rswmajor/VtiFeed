package com.rsw.vtifeed;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class MainActivity extends AppCompatActivity {

    ArrayList<RepositoryFeed> repositoryList;

    TextView tvStatus;
    ListView lvListRepo;
    AdapterView.OnItemClickListener clickListener;
    ProgressDialog mProgressDialog;

    private final String FILE_CACHE_EXT = ".pkglist";
    private final String PATH_OPKG = "opkg";
    private final String FILE_CONF_EXT = ".conf";

    private final int MENU_UPDATE_CACHE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()){
            case R.id.lvListRepo:
                //group, id, position, text
                menu.add(0, MENU_UPDATE_CACHE, 1, "Update cache and open");
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_UPDATE_CACHE:
                AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
                downloadPackageList(repositoryList.get(acmi.position));
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void initUI() {
        tvStatus = (TextView) findViewById(R.id.tvStatusRepo);
        lvListRepo = (ListView) findViewById(R.id.lvListRepo);
        clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RepositoryFeed feed = repositoryList.get((int)id);
                if (feed.isLoaded) {
                    viewSectionsFeed(feed);
                }else {
                    downloadPackageList(feed);
                }
            }
        };
        lvListRepo.setOnItemClickListener(clickListener);

        lvListRepo.setOnCreateContextMenuListener(this);

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Download list packages");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
    }

    private void viewSectionsFeed(RepositoryFeed feed) {
        SectionsActivity.feed = feed;
        Intent intent = new Intent(MainActivity.this, SectionsActivity.class);
        //intent.putExtra("RepositoryFeed", feed);
        startActivity(intent);
    }
    //comment
    private void start() {
        repositoryList = searchUrlRepositoryInSdcard();

        if (repositoryList == null || repositoryList.isEmpty()) {
            tvStatus.setText("Url feed not found. Please check exists *.conf files in folder 'opkg' from external sdcard.");
            return;
        }

        tvStatus.setText(String.format("Feeds: %d", repositoryList.size()));

        loadCachedFiles();

        fillListRepo();


    }

    private void fillListRepo() {

        final int FOLDER_ICON = R.mipmap.ic_public_black_48dp;

        final String ATTR_REPOS = "text";
        final String ATTR_COUNT = "count";
        final String ATTR_ICON = "image";

        final int itemCount = repositoryList.size();

        ArrayList<Map<String, Object>> data = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            RepositoryFeed feed = repositoryList.get(i);
            String packCount = feed.isLoaded ? "Packages: " + String.valueOf(feed.getPackages().size()) : "";
            Map<String, Object> m = new HashMap<>();

            m.put(ATTR_REPOS, feed.getNameRepository());
            m.put(ATTR_ICON, FOLDER_ICON);
            m.put(ATTR_COUNT, packCount);

            data.add(m);
        }

        String[] from = {ATTR_REPOS, ATTR_ICON, ATTR_COUNT};
        int[] to = {R.id.tvItemNameFeed, R.id.ivItemIconFeed, R.id.tvItemPackageCount};

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, data, R.layout.item_list_repo, from, to);
        lvListRepo.setAdapter(simpleAdapter);

    }

    private ArrayList<RepositoryFeed> searchUrlRepositoryInSdcard() {

        File sdcard = Environment.getExternalStorageDirectory();

        File folder = new File(sdcard, PATH_OPKG);

        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(FILE_CONF_EXT);
            }
        });

        if (files == null) {
            return null;
        }

        ArrayList<RepositoryFeed> repositoryFeedArrayList = new ArrayList<>();

        for (File fileEntry : files) {
            if (fileEntry.exists()) {
                ArrayList<RepositoryFeed> tempList = parseUrlRepo(fileEntry);

                if (!tempList.isEmpty()) {
                    repositoryFeedArrayList.addAll(tempList);
                }

            }
        }

        return repositoryFeedArrayList;
    }

    private ArrayList<RepositoryFeed> parseUrlRepo(File fileOpkgConf) {

        BufferedReader reader = null;
        ArrayList<RepositoryFeed> tempList = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(fileOpkgConf));
            String line;
            while ((line = reader.readLine()) != null) {
                RepositoryFeed feed = new RepositoryFeed(line);
                if (feed.getUrlRepository().length() != 0 & feed.getNameRepository().length() != 0) {
                    tempList.add(feed);
                }

            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempList;
    }

    private void downloadPackageList(RepositoryFeed feed) {
        feed.getPackages().clear();

        final String remoteFile = "Packages.gz";
        final String urlPackages = feed.getUrlRepository() + "/" + remoteFile;

        final DownloadTask downloadTask = new DownloadTask(MainActivity.this, feed);
        Log.d("MYLOG", "Download " + urlPackages);
        downloadTask.execute(urlPackages);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });

    }

    private void parseTextPackages(String text, RepositoryFeed feed){
        String[] arrTextPackage = text.split("\n\n\n");
        Log.d("MyLOG", "arrTextPackage.length = " + String.valueOf(arrTextPackage.length));
        ArrayList<PackageFeed> packageList = feed.getPackages();
        for (int i = 0; i < arrTextPackage.length; i++){
            PackageFeed pf = new PackageFeed(arrTextPackage[i]);
            if (pf.filename.length()>0){
                pf.feed = feed;
                packageList.add(pf);
            }
        }
        feed.isLoaded = true;
    }

    private void loadCachedFiles(){

        for (RepositoryFeed feed : repositoryList){
            String fileName = feed.getNameRepository() + FILE_CACHE_EXT;
            File file = new File(getCacheDir(), fileName);
            if (file.exists()) {
                InputStream instream = null;
                try {
                    instream = new BufferedInputStream(new FileInputStream(file));
                    ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
                    byte buff[] = new byte[4096];
                    int count;
                    while ((count = instream.read(buff)) != -1) {
                        tmpStream.write(buff, 0, count);
                    }
                    parseTextPackages(new String(tmpStream.toByteArray()), feed);

                }catch (FileNotFoundException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (instream != null) {
                            instream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private RepositoryFeed feed;
        //private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context, RepositoryFeed feed) {
            this.context = context;
            this.feed = feed;
        }

        @Override
        protected String doInBackground(String... sUrl) {

            InputStream input = null;
            ByteArrayOutputStream inbuffer;
            GZIPInputStream gzin = null;
            FileOutputStream outstream = null;
            String filename = feed.getNameRepository() + FILE_CACHE_EXT;

            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                inbuffer = new ByteArrayOutputStream();

                byte data[] = new byte[4096];
                int total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength == 0) // only if total length is known
                        fileLength = total;
                    publishProgress(total, fileLength);
                    inbuffer.write(data, 0, count);
                }

                //decompress
                ByteArrayInputStream bain = new ByteArrayInputStream(inbuffer.toByteArray());
                gzin  = new GZIPInputStream(bain);

                ByteArrayOutputStream tmpBuf = new ByteArrayOutputStream();

                while ((count = gzin.read(data)) != -1) {
                    tmpBuf.write(data, 0, count);
                }

                byte[] decompressedBytes = tmpBuf.toByteArray();

                //cache list packages to internal memory
                outstream = new FileOutputStream(new File(getCacheDir(), filename));

                outstream.write(decompressedBytes);

                return new String(decompressedBytes);

            } catch (Exception e) {
                //return e.toString();
                Log.d("MYLOG", e.getMessage());
                return null;
            } finally {
                try {
                    if (gzin != null) {
                        gzin.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                    if (outstream != null){
                        outstream.close();
                    }
                } catch (IOException ignored) {
                    Log.d("MYLOG", ignored.getMessage());
                }

                if (connection != null)
                    connection.disconnect();
            }

        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            //PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                   // getClass().getName());
            //mWakeLock.acquire();
            //<uses-permission android:name="android.permission.WAKE_LOCK" />
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMax(1);
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(progress[1]);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            //mWakeLock.release();
            mProgressDialog.dismiss();
            if (result == null)
                Toast.makeText(context, "Error download list packages", Toast.LENGTH_LONG).show();
            else {
                Toast.makeText(context, "List packages downloaded", Toast.LENGTH_SHORT).show();
                Log.d("MyLOG", String.valueOf(result.length()));
                //Log.d("MyLOG", result);
                parseTextPackages(result, feed);
                fillListRepo();
                viewSectionsFeed(feed);
            }
        }

    }
}
