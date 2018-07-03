package com.robotdestroyer.amasorter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/* This Class is the activity that appears when the app is first loaded. It has a text edit field to enter
 * a name, and a button to start the search on google for any AMA's on Reddit associated with the name.
 * Once the search is complete, a scroll list is filled with buttons. The buttons each open a Content Activity
 * With their respective AMAPage data. */

public class MainActivity extends AppCompatActivity {
    //Interactive layout objects
    Button getLinkBtn;
    EditText searchNameText;

    //Name typed into the search field
    String celebName;

    //List of links to ama pages that will be shown. Filled after google search is performed
    List<String> correctLinks = new ArrayList<>();
    List<Button> linkButtons = new ArrayList<>();

    //Handler for secondary thread to add links to the layout
    Handler addLinksHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sets the layout object variables
        searchNameText = findViewById(R.id.searchNameTxt);
        getLinkBtn = findViewById(R.id.getLinkBtn);

        //When the button is pressed, it will hide the keyboard and start a new thread in the LinkButton class
        getLinkBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(searchNameText.getWindowToken(),0);
                new LinkButton().execute();
            }
        });
    }


    //This class is used when the button is pressed to get all links associated with the search text
    private class LinkButton extends AsyncTask<Void, Void, Void> {

        //Starts a new thread and calls GetLinks
        @Override
        protected Void doInBackground(Void... voids) {
            GetLinks();
            return null;
        }


        //Searches google for the search text and "AMA reddit" Then calls methods to handle the search results.
        void GetLinks() {

            //Gets the string to be searched, does returns if it is empty
            celebName = searchNameText.getText().toString();
            if (celebName == "") {
                return;
            }

            //Gets rid of old results
            linkButtons.clear();
            correctLinks.clear();

            //Gets the url for the google search. Searches for the user input search text, and adds "ama reddit". Also tries to limit AMA requests.
            String request = "https://www.google.com/search?q=" + celebName + " ama reddit -\"[AMA Request]\"" + "&num=20";


            //Attempts to get a doc containing the info after the google search
            try {
                Document doc = Jsoup
                        .connect(request)
                        .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")
                        .timeout(20000).get();

                //Gets the first several links from the google search
                Elements links = doc.select("a[href]");


                //Iterates through the link results and checks if the url contains reddit.com
                for (Element link : links) {
                    String url = link.attr("href").toLowerCase();
                    if (!correctLinks.contains(url) && url.contains("reddit.com/r/")) {

                        //If the link is to reddit, we add the link to the correctLinks list. The list is to easily check for duplicate urls.
                        correctLinks.add(url);

                        //If the link is to reddit, we make a new button with the MakeNewLinkButton method and add it to the linkButtons list
                        Button newButton = MakeNewLinkButton(url, link);
                        if (newButton != null)
                            linkButtons.add(newButton);
                    }
                }

                //Adds each button in the linkButtons list to the screen. These are the buttons that will take us to different AMA pages
                //Must use a handler because layout can't be edited from a secondary thread
                final LinearLayout layout = findViewById(R.id.scrollLayout);
                for (final Button button : linkButtons) {
                    addLinksHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (linkButtons.indexOf(button) == 0)
                                layout.removeAllViews();
                            if (button.getText().length() > 3)
                                layout.addView(button);
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        //This method makes a new button that is used to take us to its specific AMA page
        Button MakeNewLinkButton(String url, Element link) {

            //Get the text to be displayed on the button.
            String linkTitle = "";
            for (Node child : link.childNodes()) {
                linkTitle += Html.fromHtml(child.toString());
            }

            //Some links are duplicates, this checks for those
            if (linkTitle.equals("Cached") || linkTitle.equals("Similar"))
                return null;

            //Makes the new button and sets its attributes
            Button newButton = new Button(MainActivity.this);
            newButton.setText(linkTitle);
            newButton.setBackgroundColor(Color.parseColor("#E5E7E9"));
            newButton.setPadding(10, 0,10,0);
            final String myUrl = url;

            //When this button is pressed it will start a new thread with the ClickLink class, and pass to it the button's specific url
            newButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ClickLink cL = new ClickLink();
                    cL.clickUrl = myUrl;
                    cL.execute();
                }
            });

            //Returns the new button
            return newButton;
        }


    }


    //When button to a specific AMA page is clicked, an instance of this class is created and doInBackground is executed, calling the ClickOnLink method
    private class ClickLink extends AsyncTask<Void, Void, Void> {
        String clickUrl;
        @Override
        protected Void doInBackground(Void... voids) {
            ClickOnLink(clickUrl);
            return null;
        }

        //Gets an instance of the AmaPage class with the specific url's info and calls the ShowData method with that instance
        void ClickOnLink(String url) {

            //Makes sure the url is not empty, and removes a strange string that appears at the beginning of all urls retrieved using jsoup.
            //Then adds .json to retrieve the .json file that reddit provides for its posts.
            if (url != "") {
                url = url.replace("/url?q=", "");
                url = url + ".json?";
                ShowData(RequestAMAPage(url));
            }
        }
    }


    //Returns an instance of the AmaPage class with the needed info we intend to display
    AmaPage RequestAMAPage (String urlString) {

        AmaPage viewedPost = null;
        HttpURLConnection request = null;
        URL url = null;

        //Makes a URL with the string passed to this method
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        //Parses the JSON file we get from the reddit
        try {
            //Connects to the url
            request = (HttpURLConnection) url.openConnection();
            request.setRequestMethod("GET");
            request.connect();

            //We retrieve a JsonArray
            JsonParser jp = new JsonParser(); //from gson
            JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonArray rootArr = root.getAsJsonArray(); //May be an array, may be an object.

            //We make a new JsonObject that holds all of the post information
            JsonObject postObj = rootArr.get(0).getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray().get(0).getAsJsonObject().get("data").getAsJsonObject();

            //We create the AmaPage instance and pass variables to its constructor. Variables are: Post title, post description, original poster username, and the date the post was created
            viewedPost = new AmaPage(postObj.get("title").getAsString(), postObj.get("selftext").getAsString(), postObj.get("author").getAsString(), new Date(postObj.get("created_utc").getAsLong() * 1000));

            //We make a new JsonArray that holds all of the base comments from the post. (Initial comments replying directly to the post)
            JsonArray baseComments = rootArr.get(1).getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray();

            //We define two new lists that will hold the comments we want to display
            List<String> commentsToDisplay = new ArrayList<>();
            List<String> postAuthorReplies = new ArrayList<>();

            //Iterating through all of the base comments, we will check if they contain a reply with an author that matches the original poster.
            for (JsonElement comment : baseComments) {

                //Checks if there are any replies to the comment. We move on if so
                if (comment.getAsJsonObject().get("data").getAsJsonObject().get("replies") != null && comment.getAsJsonObject().get("data").getAsJsonObject().get("replies").toString().length() > 2) {

                    //Make a new JsonArray to simplify our code. Set it to the comment's replies
                    JsonArray replies = comment.getAsJsonObject().get("data").getAsJsonObject().get("replies").getAsJsonObject().get("data").getAsJsonObject().get("children").getAsJsonArray();

                    //Iterate through the replies
                    for (int i = 0; i < replies.size(); i++) {

                        //Make a new JsonObject to simplify and refer to this specific reply in the iteration
                        JsonObject reply = replies.get(i).getAsJsonObject().get("data").getAsJsonObject();

                        //Checks if the reply is empty, or if it has no author
                        if (reply != null && reply.get("author") != null
                                && reply.toString().length() > 2
                                && reply.get("author").toString().length() > 2) {

                            //Finally checks if the reply's author is equal to our original poster. If so, we have found a comment/reply pair to display
                            if (reply.get("author").getAsString().equals(viewedPost.amaAuthor)) {

                                //Adds the comment and the reply to their respective lists
                                commentsToDisplay.add(comment.getAsJsonObject().get("data").getAsJsonObject().get("body").getAsString().trim());
                                postAuthorReplies.add(reply.get("body").getAsString().trim());

                                //breaks from the reply iterations (only displaying one reply), and will continue to iterate though the rest of the comments
                                break;
                            }

                        }
                    }
                }
            }

            //Calls the method in the AmaPage to add all comments and replies that we plan on displaying
            viewedPost.AddComments(commentsToDisplay, postAuthorReplies);

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            request.disconnect();
        }

        //Returns the AmaPage that now contains all of the info we intend to display
        return viewedPost;
    }


    //Opens the activity, ContentActivity, to display all of the info from the AmaPage
    void ShowData (AmaPage page) {
        if (page == null)
            return;

        //Creates an intent to pass the AmaPage to the new Activity
        Intent openPageActivity = new Intent(MainActivity.this, ContentActivity.class);
        openPageActivity.putExtra("myPage", page);

        startActivity(openPageActivity);
    }
}
