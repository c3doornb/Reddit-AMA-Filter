package com.robotdestroyer.amasorter;

import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.Key;

public class ContentActivity extends AppCompatActivity {

    AmaPage viewedPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        //Retrieves the AmaPage from the original activity
        viewedPost = (AmaPage) getIntent().getSerializableExtra("myPage");

        //Sets the layout object variables
        LinearLayout layout = findViewById(R.id.scrollLayout);
        TextView tvText = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);

        //Clears the scroll layout of all previous comment/reply entries. Leaves the first as it is used for the description
        for (int i = 1; i < layout.getChildCount(); i++) {
            layout.removeViewAt(i);
        }

        //Sets title and description text as well as any attributes handled in java. Also adds the date to the end of the description
        tvText.setText(viewedPost.title);
        tvDescription.setText(viewedPost.description + "\n\n" + viewedPost.date.toString());
        tvDescription.setPadding(10, 10, 10, 120);


        //If the original poster username is deleted, we show a warning that all replies from deleted usernames will appear
        if (viewedPost.amaAuthor.equals("[deleted]")) {
            TextView tvWarning = new TextView(this);
            tvWarning.setText("***THE AUTHOR OF THIS POST HAS BEEN DELETED. REPLIES WILL BE FROM ALL DELETED USERS, NOT NECESSARILY THE ORIGINAL POSTER.***");
            tvWarning.setTextColor(Color.parseColor("#B22222"));
            tvWarning.setPadding(10, 10, 10, 120);
            tvWarning.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.addView(tvWarning, 1);

            tvDescription.setPadding(10, 10, 10, 20);
        }


        //Iterates through all comments to be displayed in the AmaPage
        for (int i = 0; i < viewedPost.commentData.size(); i++) {

            //Makes new TextView objects to display a specific comment/reply pair
            TextView newComment = new TextView(this);
            TextView newReply = new TextView(this);

            //Sets the comment text and attributes, and replaces double line breaks with single line breaks
            newComment.setText(viewedPost.commentData.get(i).getComment().replaceAll("\\n\\n", "\n"));
            newComment.setPadding(20, 10, 20, 20);

            //Sets the reply text and attributes
            newReply.setText(viewedPost.commentData.get(i).getReply());
            newReply.setPadding(50, 10,50, 120);
            newReply.setTextColor(Color.BLUE);

            //Adds the comment and reply respectively to the scroll layout
            layout.addView(newComment);
            layout.addView(newReply);
        }

    }
}
