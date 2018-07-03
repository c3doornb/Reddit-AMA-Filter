package com.robotdestroyer.amasorter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AmaPage implements Serializable {

    //Variable declaration
    public String title, description, amaAuthor;
    Date date;
    public List<Comment> commentData = new ArrayList<>();


    //Constructor
    public AmaPage(String title, String description, String amaAuthor, Date date) {
        this.title = title;
        this.description = description;
        this.amaAuthor = amaAuthor;
        this.date = date;
    }


    //Method used to fill the commentData list
    public void AddComments(List<String> comments, List<String> replies) {
        for (int i = 0; i < comments.size(); i++) {
            commentData.add(new Comment(comments.get(i), replies.get(i)));
        }
    }


    //Class which holds a comment and reply pair
    public class Comment implements Serializable {
        private String comment;
        private String reply;

        public Comment(String text, String reply) {
            this.comment = text;
            this.reply = reply;
        }

        public String getComment() {
            return comment;
        }

        public String getReply() {
            return reply;
        }
    }
}
