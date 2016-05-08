package com.rsw.vtifeed;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by Irina on 28.03.2016.
 */
public class PackageFeed implements Serializable {

    public String name, versionP, depends, section, architecture, maintainer;
    public String mD5Sum, size, filename, sourceP, description;
    public RepositoryFeed feed;

    public PackageFeed(String text) {
        name = "";
        versionP = "";
        depends = "";
        section = "";
        architecture = "";
        maintainer = "";
        mD5Sum = "";
        size = "";
        filename = "";
        sourceP = "";
        description = "";
        feed = null;

        parseTextFeed(text);

        //Log.d("MyLOG", "Name - " + name);
/*        Log.d("MyLOG", "versionP - " + versionP);
        Log.d("MyLOG", "depends - " + depends);
        Log.d("MyLOG", "section - " + section);
        Log.d("MyLOG", "architecture - " + architecture);
        Log.d("MyLOG", "maintainer - " + maintainer);
        Log.d("MyLOG", "mD5Sum - " + mD5Sum);
        Log.d("MyLOG", "size - " + size);
        Log.d("MyLOG", "filename - " + filename);
        Log.d("MyLOG", "sourceP - " + sourceP);
        Log.d("MyLOG", "description - " + description);*/

    }

    public String getDownloadLink(){
        if (feed == null) {
            return "";
        }
        return feed.getUrlRepository() + "/" + filename;
    }

    private void parseTextFeed(String text){
        String lines[] = text.split("\n");
        if (lines == null){
            return;
        }

        for (int i = 0; i < lines.length; i++){
            String pair[] = lines[i].split(":", 2);
            if (pair.length > 1){
                String key = pair[0].trim();
                String val = pair[1].trim();
                if (key.equals("Package") ){
                    name = val;
                }else if(key.equals("Version")){
                    versionP = val;
                }else if(key.equals("Depends")){
                    depends = val;
                }else if(key.equals("Section")){
                    section = val;
                }else if(key.equals("Architecture")){
                    architecture = val;
                }else if(key.equals("Maintainer")){
                    maintainer = val;
                }else if(key.equals("MD5Sum")){
                    mD5Sum = val;
                }else if(key.equals("Size")){
                    size = val;
                }else if(key.equals("Filename")){
                    filename = val;
                }else if(key.equals("SourceP")){
                    sourceP = val;
                    Log.d("MyLOG", "NOT EMPTY SOURCE: " + val);
                }else if(key.equals("Description")){
                    description = val;
                    if ((i+1) < lines.length && lines[i+1].startsWith(" ") ) {
                        description += "\n" + lines[i+1];
                    }
                }

            }
        }
    }
}
