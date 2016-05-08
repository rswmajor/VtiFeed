package com.rsw.vtifeed;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sergey Rihmayer on 28.03.2016.
 */
public class RepositoryFeed implements Serializable {

    public boolean isLoaded;

    public RepositoryFeed(String line) {
        String regexp = "^src/gz\\s+(\\S+)\\s+(http://\\S+)$";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            //Log.d("MyLOG", matcher.group(1) + matcher.group(2));
            nameRepository = matcher.group(1);
            urlRepository = matcher.group(2);
        }
        else{
            nameRepository = "";
            urlRepository = "";
        }
        packages = new ArrayList<>();
        isLoaded = false;
    }

    public String getNameRepository() {
        return nameRepository;
    }

    public String getUrlRepository() {
        return urlRepository;
    }

    public ArrayList<PackageFeed> getPackages() {
        return packages;
    }

    public void setPackages(ArrayList<PackageFeed> packages) {
        this.packages = packages;
    }

    public ArrayList<PackageFeed> getPackages(String section){
        String nameSection = section.equals(NONAME) ? "" : section;
        ArrayList<PackageFeed> packInSection = new ArrayList<>();
        for (PackageFeed item : packages) {
            if (nameSection.equals(item.section)){
                packInSection.add(item);
            }
        }
        return  packInSection;
    }

    public ArrayList<String> getSections(){
        ArrayList<String> sections = new ArrayList<>();
        for (PackageFeed item : packages) {
            String nameSection = item.section.length()>0 ? item.section : NONAME;
            if (!containsSection(nameSection, sections)) {
                sections.add(nameSection);
                //Log.d("MyLOG", "Section: " + nameSection);
            }
        }
        return sections;
    }


    private String nameRepository, urlRepository;
    private ArrayList<PackageFeed> packages;
    private final String NONAME = "noname";

    private boolean containsSection(String section, ArrayList<String> sections){
        for (String item : sections) {
            if (item.equals(section)){
                return true;
            }
        }
        return false;
    }

}
