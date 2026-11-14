package com.nimda.cite.common.util;

public class UrlNames {
    //AdminUserController
    public static final String adminUserUrl = "/api/admin/users";
    public static final String getAllUsersUrl = "";
    public static final String getPendingUserUrl = "/pending";
    public static final String approveUserUrl = "/{id}/approve";
    public static final String rejectUserUrl = "/{id}/reject";
    public static final String getAvailableRolesUrl = "/roles";
    public static final String grantRoleUrl = "/{id}/role";
    public static final String revokeRoleUrl = "/{id}/role/remove";
    public static final String getUserByIdUrl = "/{id}";
    public static final String deleteTagUrl = "/tag-delete/{tag}";
    public static final String getUserSuggestionsUrl = "/suggestions";

    //AuthController
    public static final String authUrl = "/api/auth";
    public static final String loginUrl = "/login";
    public static final String logoutUrl = "/logout";
    public static final String registerUrl = "/register";
    public static final String getCurrentUserUrl = "/me";
    public static final String updateProfileUrl = "/me";
    public static final String updateProfileImageUrl = "/profile-image";
    public static final String updateProfileDecorationUrl = "/profile-decoration";
    public static final String emailHideUrl = "/email-hide";

    //UserRecoveryController
    public static final String passwordChangeUrl = "api/cite/passwordChange";
    public static final String checkUserInfoUrl = "/info-check";
    public static final String sendAuthMailUrl = "/send-authMail";
    public static final String checkAuthCodeUrl = "/check-authcode";
    public static final String changePasswordUrl = "/change-password";

    //AlarmController
    public static final String alarmUrl = "/api/alarm";
    public static final String subscribeUrl = "/subscribe";

    //AttachmentController
    public static final String attachmentUrl = "/api/cite/attachments";
    public static final String uploadAttachmentUrl = "/upload";
    public static final String getAttachmentUrl = "/{id}";
    public static final String downloadAttachmentUrl = "/{id}/download";
    public static final String attachmentDownloadLinkUrl = "/{id}/download-url";
    public static final String myAttachmentsUrl = "/my";
    public static final String deleteAttachmentUrl = "";
    public static final String createPresignedUploadUrl = "/presigned";
    public static final String registerAttachmentUrl = "/register";

    //AttendanceController
    public static final String attendanceUrl = "/api/cite/attendance";
    public static final String checkInUrl = "/checkIn";
    public static final String getTodayVisitorsUrl = "/today";
    public static final String getConsecutiveRankUrl = "/rank/consecutive";
    public static final String getTotalRankUrl = "/rank/total";
    public static final String getMyAttendanceUrl = "/me";
    public static final String getMyAttendanceLogsUrl = "/me/logs";
    public static final String getMyTotalAttendanceCountUrl = "";

    //AdminBoardController
    public static final String adminBoardUrl = "/api/admin/boards";
    public static final String getTagStatsUrl = "/tag-stats";
    public static final String deactivateByTagUrl = "/deactivate-by-tag";
    public static final String activateByTagUrl = "/activate-by-tag";

    //BoardController
    public static final String boardUrl = "/api/cite/board";
    public static final String getPostsByCategoryUrl = "";
    public static final String getPinnedPostsUrl = "/pinned";
    public static final String getPopularPostsUrl = "/popular";
    public static final String writeBoardUrl = "";
    public static final String viewBoardUrl = "/{id}";
    public static final String updateBoardUrl = "/{id}";
    public static final String deleteBoardUrl = "/{id}";
    public static final String togglePinUrl = "/{id}/pin";
    public static final String getMyBoardStatsUrl = "/my/board-count";
    public static final String getBoardsByNicknameUrl = "/user/{nickname}";
    public static final String getMyBoardsUrl = "/my/boards";
    public static final String deleteMyBoardsUrl = "/my/boards";
    public static final String getMyCommentedBoardsUrl = "/my/commented-boards";
    public static final String getRecentBoardsUrl = "/recent-boards";

    //CategoryController
    public static final String categoryUrl = "/api/cite/category";
    public static final String getAllCategoriesUrl = "";
    public static final String getAllCategoriesAdminUrl = "/all";
    public static final String getCategoryBySlugUrl = "/slug/{slug}";
    public static final String createCategoryUrl = "";
    public static final String updateCategoryUrl = "/{id}";
    public static final String deleteCategoryUrl = "/{id}";
    public static final String updateSortOrdersUrl = "/sort-order";
    public static final String enableShopUrl = "/enable-shop/{id}";

    //FileDownloadController
    public static final String fileDownloadUrl = "/api/download";
    public static final String downloadFileUrl = "/{fileName:.+}";

    //CommentController
    public static final String commentUrl = "/api";
    public static final String createCommentUrl = "/board/{boardId}/comments";
    public static final String getCommentsUrl = "/board/{boardId}/comments";
    public static final String getMyCommentsUrl = "/my-page/comments";
    public static final String updateCommentUrl = "/comments/{commentId}";
    public static final String hideCommentUrl = "/comments/{commentId}/status";
    public static final String deleteCommentUrl = "/comments/{commentId}";
    public static final String getMyCommentCountUrl = "/comments/my/count";
    public static final String deleteMyCommentsUrl = "/my-page/comments";
    public static final String getCommentsByNicknameUrl = "/comments/user/{nickname}";

    //BoardLikeController
    public static final String boardLikeUrl = "/api/like/board";
    public static final String getLikedBoardsByNicknameUrl = "/user/{nickname}/liked";
    public static final String getLikeStatusUrl = "/{boardId}/likeStatus";
    public static final String getBoardLikeCountUrl = "/{boardId}/likeCount";
    public static final String toggleBoardLikeUrl = "/{boardId}";
    public static final String getTotalLikesReceivedUrl = "/totalLikes";
    public static final String getLikeBoardsUrl = "/pushedLikes";
    public static final String getPushedLikesCountUrl = "/pushedLikes/count";

    //CommentLikeController
    public static final String commentLikeUrl = "/api/like/comment";
    public static final String toggleCommentLikeUrl = "/{commentId}";
    public static final String getCommentLikeCountUrl = "/{commentId}/likeCount";

    //NotificationController
    public static final String notificationUrl = "/api/notifications";
    public static final String getNotificationsUrl = "";
    public static final String getUnReadNotificationsUrl = "/unRead";
    public static final String markAsReadUrl = "/{notificationId}/read";
    public static final String markAllAsReadUrl = "/readAll";
    public static final String hasUnreadNotificationUrl = "/hasUnread";
    public static final String deleteNotificationUrl = "/{notificationId}";

    //PointController
    public static final String pointUrl = "/api/cite/point";
    public static final String getBalanceUrl = "";
    public static final String totalBalanceUrl = "/allBalance";
    public static final String viewPointDetailUrl = "/pointDetails";
    public static final String updateUserBalanceManualUrl = "";
    public static final String updateUserBalanceManualBulkUrl = "/bulk";
    public static final String getBalanceByNicknameUrl = "/user/{nickname}";
    public static final String getPointDetailsByNicknameUrl = "/user/{nickname}/details";

    //PointUsageController
    public static final String pointUsageBoardUrl = "/api/cite/board";
    public static final String purchaseUrl = "/{id}/purchase";

    //ProfileDecorationController
    public static final String profileDecorationBaseUrl = "/api/cite";
    public static final String getActiveDecorationsUrl = "/profile-decorations";
    public static final String getDecorationImageUrl = "/profile-decorations/{key}/image";
    public static final String getMyDecorationsUrl = "/profile-decorations/me";
    public static final String getAdminDecorationsUrl = "/admin/profile-decorations";
    public static final String createDecorationUrl = "/admin/profile-decorations";
    public static final String grantDecorationUrl = "/admin/profile-decorations/ownership/grant";
    public static final String revokeDecorationUrl = "/admin/profile-decorations/ownership/revoke";
    public static final String deleteDecorationUrl = "/admin/profile-decorations/{id}";

    //TagController
    public static final String tagUrl = "/api/cite/tag";
    public static final String addTagUrl = "/add-tag";
    public static final String getAllTagsUrl = "";
    public static final String searchTagUrl = "/{categoryId}";
    public static final String deleteTagByIdUrl = "/{tagId}";
    public static final String updateTagNameUrl = "/{tagId}";

    //UsersController
    public static final String usersUrl = "/api/users";
    public static final String getUserByNicknameUrl = "/nickname/{nickname}";

    //GroupController
    public static final String groupUrl = "/api/groups";
    public static final String createGroupUrl = "";
    public static final String getGroupsUrl = "";
    public static final String getGroupUrl = "/{groupId}";
    public static final String updateGroupUrl = "/{groupId}";
    public static final String deleteGroupUrl = "/{groupId}";

    //GroupJoinRequestController
    public static final String groupJoinRequestUrl = "/api/groups/{groupId}/join-requests";
    public static final String applyToGroupUrl = "/apply";
    public static final String inviteToGroupUrl = "/invite";
    public static final String getPendingRequestsUrl = "";
    public static final String acceptJoinRequestUrl = "/{requestId}/accept";
    public static final String rejectJoinRequestUrl = "/{requestId}/reject";
    public static final String cancelJoinRequestUrl = "/{requestId}";

    //GroupMemberController
    public static final String groupMemberUrl = "/api/groups/{groupId}/members";
    public static final String getMembersUrl = "";
    public static final String leaveGroupUrl = "/me";
    public static final String kickMemberUrl = "/{targetUserId}";
    public static final String transferLeadershipUrl = "/{targetUserId}/leader";

    //ProblemController
    public static final String problemUrl = "api/judge/problem";
    public static final String viewProblemListUrl = "";
    public static final String viewProblemHtmlUrl = "/{id}/html";
    public static final String viewProblemDetailsUrl = "/{id}";
    public static final String createProblemUrl = "";
    public static final String searchByCodeUrl = "/search-by-code";
    public static final String searchByTitleUrl = "/search-by-title";
    public static final String toggleIsPublicUrl = "/toggle-public/{id}";
    public static final String deleteProblemUrl = "/{id}";

    //SubmissionController
    public static final String submissionUrl = "api/judge/submission";
    public static final String submitRequestUrl = "";
    public static final String updateResultUrl = "/result";
    public static final String viewMySubmitUrl = "/my/status/{problemId}";
    public static final String viewMySourceCodeUrl = "/detail/{submissionId}";
    public static final String viewMyStatusUrl = "/my/status";
    public static final String viewOtherStatusUrl = "/{userId}/status";

    //ChallengeController (CTF)
    public static final String challengeUrl = "/api/ctf/challenge";
    public static final String createChallengeUrl = "";
    public static final String toggleChallengePublicUrl = "/toggle-public/{id}";
    public static final String deleteChallengeUrl = "/{id}";

    //ChallengeQueryController (CTF)
    public static final String challengesUrl = "/api/ctf/challenges";
    public static final String getChallengesUrl = "";
    public static final String getChallengeDetailUrl = "/{code}";

    //ChallengeDownloadController (CTF)
    public static final String requestChallengeDownloadUrl = "/{code}/download";
    public static final String getChallengeDownloadStatusUrl = "/download/{requestId}";

    //FlagSubmissionController (CTF)
    public static final String submitFlagUrl = "/{challengeId}";
    public static final String getSubmissionResultUrl = "/submission/{submissionId}";

    //AdminContestController (CTF)
    public static final String adminContestUrl = "/api/admin/ctf/contests";
    public static final String getAdminContestsUrl = "";
    public static final String createContestUrl = "";
    public static final String finalizeContestUrl = "/{contestId}/finalize";

    //AdminScheduleController (CTF)
    public static final String adminScheduleUrl = "/api/admin/ctf/schedule";
    public static final String getAdminScheduleUrl = "";
    public static final String updateScheduleUrl = "";

    //ContestController (CTF)
    public static final String contestUrl = "/api/ctf/contests";
    public static final String getContestsUrl = "";
    public static final String getContestDetailUrl = "/{contestId}";

    //ScheduleController (CTF)
    public static final String scheduleUrl = "/api/ctf/schedule";
    public static final String getScheduleUrl = "";

    //AdminTeamController (CTF)
    public static final String adminTeamUrl = "/api/admin/ctf/teams";
    public static final String getTeamsUrl = "";
    public static final String getUnassignedUsersUrl = "/unassigned";
    public static final String getTeamUrl = "/{teamId}";
    public static final String createTeamUrl = "";
    public static final String updateTeamUrl = "/{teamId}";
    public static final String deleteTeamUrl = "/{teamId}";
    public static final String addTeamMembersUrl = "/{teamId}/members";
    public static final String removeTeamMemberUrl = "/{teamId}/members/{userId}";
    public static final String changeTeamLeaderUrl = "/{teamId}/leader";

    //CtfTeamController (CTF)
    public static final String ctfTeamUrl = "/api/ctf/teams";
    public static final String getMyTeamUrl = "/me";

    //CtfServerStatusController (CTF)
    public static final String ctfServerStatusUrl = "/api/ctf/server-status";
    public static final String getCtfServerStatusUrl = "";

    //InstanceController (CTF)
    public static final String instanceUrl = "/api/ctf/instance";
    public static final String createInstanceUrl = "/{code}";
    public static final String getMyInstanceUrl = "/{code}";
    public static final String getInstanceStatusUrl = "/status/{uuid}";

    //ScoreboardController (CTF)
    public static final String scoreboardUrl = "/api/ctf/scoreboard";
    public static final String getScoreboardUrl = "";
    public static final String getMyTeamScoreUrl = "/me";
}
