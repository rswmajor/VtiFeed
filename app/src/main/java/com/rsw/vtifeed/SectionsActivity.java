package com.rsw.vtifeed;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SectionsActivity extends AppCompatActivity {

    TextView tvStatus;
    ListView lvSections;
    static RepositoryFeed feed;
    ArrayList<String> sectionsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sections);

        initUI();

        baseInit();

        fillListSections();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_sections, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search:
                showSearchDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initUI(){

        tvStatus = (TextView) findViewById(R.id.tvStatusSections);

        lvSections = (ListView) findViewById(R.id.lvListSections);
        lvSections.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                viewPackagesSection(sectionsList.get((int) id));
            }
        });


    }

    private void baseInit(){
        //feed = (RepositoryFeed)getIntent().getSerializableExtra("RepositoryFeed");
        sectionsList = feed.getSections();

        setTitle(feed.getNameRepository());
        tvStatus.setText("Packages: " + String.valueOf(feed.getPackages().size()));
    }

    private void fillListSections(){

        final int FOLDER_ICON = R.mipmap.ic_folder_open_black_48dp;

        final String ATTR_SECTION = "text";
        final String ATTR_COUNT = "count";
        final String ATTR_ICON = "image";

        final int itemCount = sectionsList.size();

        ArrayList<Map<String, Object>> data = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            String nameSection = sectionsList.get(i);
            String packCount = "Packages: " + String.valueOf(feed.getPackages(nameSection).size());
            Map<String, Object> m = new HashMap<>();

            m.put(ATTR_SECTION, nameSection);
            m.put(ATTR_ICON, FOLDER_ICON);
            m.put(ATTR_COUNT, packCount);

            data.add(m);
        }

        String[] from = {ATTR_SECTION, ATTR_ICON, ATTR_COUNT};
        int[] to = {R.id.tvItemNameSection, R.id.ivItemIconSection, R.id.tvItemSectionPackageCount};

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, data, R.layout.item_list_section, from, to);
        lvSections.setAdapter(simpleAdapter);

    }

    private void viewPackagesSection(String section) {
        viewPackages(feed.getPackages(section), section);
    }

    private void viewPackages(ArrayList<PackageFeed> packages, String titleText){
        PackagesActivity.packagesList = packages;
        Intent intent = new Intent(SectionsActivity.this, PackagesActivity.class);
        intent.putExtra("section", titleText);
        startActivity(intent);
    }

    private void searchPackages(String text){
        String searchText = text.trim().toLowerCase();
        ArrayList<PackageFeed> resultList = new ArrayList<>();
        for (PackageFeed pkg : feed.getPackages()){
            if (pkg.name.toLowerCase().contains(searchText) | pkg.description.toLowerCase().contains(searchText)){
                resultList.add(pkg);
            }
        }
        if (resultList.size() == 0) {
            Toast.makeText(this, "Packages not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String textTitle = "Search for: '" + searchText + "'";
        viewPackages(resultList, textTitle);
    }

    private void showSearchDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);// | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mSearchText = input.getText().toString();
                searchPackages(mSearchText);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
