package com.nimda.cite.common.util;

public class TableNames {
    //user
    public static final String usersTable = "users";
    public static final String authorityTable = "authority";
    public static final String userAuthorityTable = "user_authority";

    //attachment
    public static final String attachmentsTable = "attachments";

    //attendance
    public static final String attendanceTable = "attendance";
    public static final String attendanceLogTable = "attendance_log";

    //board
    public static final String boardTable = "board";
    public static final String categoryTable = "category";

    //comment
    public static final String commentsTable = "comments";

    //like
    public static final String boardLikeTable = "board_like";
    public static final String commentLikeTable = "comment_like";

    //notification
    public static final String notificationTable = "notification";

    //point
    public static final String pointHistoryTable = "point_history";
    public static final String pointDetailsTable = "point_details";
    public static final String userBalanceTable = "user_balance";

    //profile decoration
    public static final String profileDecorationsTable = "profile_decorations";
    public static final String userProfileDecorationsTable = "user_profile_decorations";

    //tag
    public static final String tagsTable = "tags";

    //judgeServer - group
    public static final String groupsTable = "`groups`"; // MySQL 예약어라 백틱 필요
    public static final String groupJoinRequestsTable = "group_join_requests";
    public static final String groupMembersTable = "group_members";

    //judgeServer - problem
    public static final String problemsTable = "problems";

    //judgeServer - submission
    public static final String submissionsTable = "submissions";
}
