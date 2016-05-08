package com.rsw.vtifeed;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PackagesActivity extends AppCompatActivity {

    TextView tvStatus;
    ListView lvPackages;

    static ArrayList<PackageFeed> packagesList;

    private final int MENU_COPY_LINK = 1;
    private final int MENU_OPEN_LINK = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packages);

        initUI();

        baseInit();

        fillListPackages();
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()){
            case R.id.lvListPackages:
                //group, id, position, text
                menu.add(0, MENU_COPY_LINK, 1, "Copy link to clipboard");
                menu.add(0, MENU_OPEN_LINK, 2, "Open link with system");
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Log.d("MyLOG", "acmi.position " + acmi.position);
        switch (item.getItemId()) {
            case MENU_COPY_LINK:
                ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("link", packagesList.get(acmi.position).getDownloadLink());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(PackagesActivity.this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
                break;
            case MENU_OPEN_LINK:
                String url = packagesList.get(acmi.position).getDownloadLink();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void initUI() {
        tvStatus = (TextView) findViewById(R.id.tvStatusPackages);
        lvPackages = (ListView) findViewById(R.id.lvListPackages);

        lvPackages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                registerForContextMenu(lvPackages);
                view.showContextMenu();
                unregisterForContextMenu(lvPackages);
            }
        });
        //lvPackages.setOnCreateContextMenuListener(this);
    }

    private void baseInit() {
        String nameSection = getIntent().getStringExtra("section");
        setTitle( nameSection);
        tvStatus.setText("Packages: " + String.valueOf(packagesList.size()));
    }

    private void fillListPackages() {

        //final int BTN_ICON = R.mipmap.ic_file_download_black_48dp;

        final String ATTR_NAME = "name";
        final String ATTR_VERSION = "version";
        final String ATTR_DEPENDS = "depends";
        final String ATTR_SECTION = "section";
        final String ATTR_ARCH = "arch";
        final String ATTR_MAINT = "maint";
        final String ATTR_SIZE = "size";
        final String ATTR_DESCR = "descr";
        //final String ATTR_ICON = "icon";

        final int itemCount = packagesList.size();

        ArrayList<Map<String, Object>> data = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            PackageFeed pack = packagesList.get(i);
            String packSize = pack.size + " bytes (" + fileSizeToString(Long.parseLong(pack.size), false) + ")";
            Map<String, Object> m = new HashMap<>();

            m.put(ATTR_NAME, pack.name);
            m.put(ATTR_VERSION, "Version: " + pack.versionP);
            m.put(ATTR_DEPENDS, "Depends: " + pack.depends);
            m.put(ATTR_SECTION, "Section: " + pack.section);
            m.put(ATTR_ARCH, "Architecture: " + pack.architecture);
            m.put(ATTR_MAINT, "Maintainer: " + pack.maintainer);
            m.put(ATTR_SIZE, "Size: " + packSize);
            m.put(ATTR_DESCR, "Description: " + pack.description);

            data.add(m);
        }

        String[] from = {ATTR_NAME, ATTR_VERSION, ATTR_DEPENDS, ATTR_SECTION, ATTR_ARCH, ATTR_MAINT, ATTR_SIZE, ATTR_DESCR};
        int[] to = {R.id.tvPackageName, R.id.tvVersion, R.id.tvDepends, R.id.tvSectionName , R.id.tvArchitecture,
                    R.id.tvMaintainer, R.id.tvSize, R.id.tvDescription};

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, data, R.layout.item_list_packages, from, to);
        lvPackages.setAdapter(simpleAdapter);
    }

    public String fileSizeToString(long bytes, boolean si)
    {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
