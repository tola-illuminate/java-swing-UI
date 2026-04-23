package model;

public class Task {

    private String id;
    private String title;
    private String assignee;
    private String priority;
    private String status;
    private String progress;

    // constructor task
    public Task(String id, String title, String assignee,
                String priority, String status, String progress) {
        this.id = id;
        this.title = title;
        this.assignee = assignee;
        this.priority = priority;
        this.status = status;
        this.progress = progress;
    }

    // Getters & Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAssignee() { return assignee; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getProgress() { return progress; }

}


