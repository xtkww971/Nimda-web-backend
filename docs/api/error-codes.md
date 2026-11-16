# 오류 코드

내부 코드는 응답 본문에 포함되지 않습니다. 오류 메시지는 상황에 따라 기본 메시지와 다를 수 있습니다.

| 이름 | 내부 코드 | HTTP | 기본 메시지 |
| --- | --- | --- | --- |
| `INVALID_INPUT` | CO001 | 400 | 잘못된 요청입니다. |
| `ACCESS_DENIED` | CO002 | 403 | 접근 권한이 없습니다. |
| `INTERNAL_SERVER_ERROR` | CO003 | 500 | 서버 오류가 발생했습니다. |
| `VERSION_MISMATCH` | CO004 | 426 | 앱 버전이 일치하지 않습니다. 최신 버전으로 업데이트 해주세요. |
| `UPLOAD_FILE_NOT_FOUND` | UF001 | 404 | 업로드할 파일이 없습니다. |
| `FILE_FORM_NOT_ALLOW` | UF002 | 400 | 허용되지 않는 파일 형식입니다 |
| `FILE_ROLE_NOT_EXIST` | UF003 | 403 | 삭제 권한이 없는 파일이 포함되어 있습니다 |
| `S3_KEY_INNEED` | UF004 | 400 | S3 key가 필요합니다. |
| `ATTACHMENT_NOT_FOUND` | UF005 | 404 | 파일이 존재하지 않습니다. |
| `FILE_IDS_REQUIRED` | UF006 | 400 | fileIds가 필요합니다. |
| `FILE_STORAGE_UNAVAILABLE` | UF007 | 503 | 파일 업로드 서비스를 사용할 수 없습니다. |
| `IMAGE_PROCESSING_FAILED` | UF008 | 500 | 이미지 처리에 실패했습니다. |
| `ATTACHMENT_LINK_FORBIDDEN` | UF009 | 403 | 첨부 소유자만 게시글에 연결할 수 있습니다. |
| `ATTACHMENT_ALREADY_LINKED` | UF010 | 400 | 이미 다른 게시글에 연결된 첨부입니다. |
| `ATTACHMENT_CATEGORY_MISMATCH` | UF011 | 400 | 첨부 카테고리가 게시글 카테고리와 일치하지 않습니다. |
| `ATTACHMENT_REMOVE_FORBIDDEN` | UF012 | 403 | 다른 사용자가 올린 첨부는 제거할 수 없습니다. |
| `INVALID_THUMBNAIL` | UF013 | 400 | 썸네일은 첨부된 이미지 중에서 선택해야 합니다. |
| `INVALID_FILE_NAME` | UF014 | 400 | 잘못된 파일명입니다. |
| `INVALID_FILE_PATH` | UF015 | 403 | 잘못된 파일 경로입니다. |
| `FILE_NOT_FOUND` | UF016 | 404 | 파일을 찾을 수 없습니다. |
| `FILE_READ_FORBIDDEN` | UF017 | 403 | 파일에 접근할 권한이 없습니다. |
| `ALREADY_CHECKED_IN` | AT001 | 400 | 이미 오늘 출석을 완료했습니다. |
| `ATTENDANCE_NOT_FOUND` | AT002 | 404 | 출석 정보가 없습니다. |
| `BOARD_NOT_FOUND` | BO001 | 404 | 게시글을 찾을 수 없습니다. |
| `BOARD_ACCESS_DENIED` | BO002 | 403 | 접근 권한이 없습니다. |
| `BOARD_UPDATE_FORBIDDEN` | BO003 | 403 | 게시글을 수정할 권한이 없습니다. |
| `BOARD_DELETE_FORBIDDEN` | BO004 | 403 | 게시글을 삭제할 권한이 없습니다. |
| `BOARD_PIN_FORBIDDEN` | BO005 | 403 | 게시글을 고정/해제할 권한이 없습니다. 관리자만 가능합니다. |
| `NEWS_UPDATE_FORBIDDEN` | BO006 | 403 | 새 소식 게시판은 관리자만 수정할 수 있습니다. |
| `SHOP_WRITE_FORBIDDEN` | BO007 | 403 | 상품 등록은 관리자만 가능합니다. |
| `SHOP_UPDATE_FORBIDDEN` | BO008 | 403 | 상품 수정은 관리자만 가능합니다. |
| `INVALID_ITEM_PRICE` | BO009 | 400 | 상품 가격을 1 NC 이상으로 입력해주세요. |
| `INVALID_ITEM_TYPE` | BO010 | 400 | 지원하지 않는 상품 종류입니다. |
| `SHOP_BADGE_REQUIRED` | BO011 | 400 | 배지 상품은 배지를 선택해야 합니다. |
| `SHOP_BADGE_NOT_FOUND` | BO012 | 404 | 존재하지 않는 배지입니다. |
| `SHOP_BADGE_INACTIVE` | BO013 | 400 | 비활성화된 배지는 상품으로 등록할 수 없습니다. |
| `SHOP_THUMBNAIL_REQUIRED` | BO014 | 400 | 마일리지 상점 상품은 썸네일 이미지가 필요합니다. |
| `DELETE_TARGET_REQUIRED` | BO015 | 400 | 삭제할 게시글을 선택해주세요. |
| `CATEGORY_NOT_FOUND` | CT001 | 404 | 카테고리를 찾을 수 없습니다. |
| `INVALID_SLUG` | CT002 | 400 | 유효하지 않은 카테고리 slug입니다. |
| `CATEGORY_PARAM_REQUIRED` | CT003 | 400 | categoryId 또는 slug 파라미터가 필요합니다. |
| `ADMIN_REQUIRED` | CT004 | 403 | 관리자 권한이 필요합니다. |
| `SELF_PARENT_NOT_ALLOWED` | CT005 | 400 | 자기 자신을 부모로 설정할 수 없습니다. |
| `CIRCULAR_REFERENCE` | CT006 | 400 | 순환 참조가 발생합니다. 해당 카테고리를 부모로 설정할 수 없습니다. |
| `HAS_CHILD_CATEGORY` | CT007 | 400 | 하위 카테고리가 존재하여 삭제할 수 없습니다. 먼저 하위 카테고리를 삭제하거나 비활성화하세요. |
| `HAS_POSTS` | CT008 | 400 | 해당 카테고리에 게시글이 존재하여 삭제할 수 없습니다. |
| `SLUG_ALREADY_EXIST` | CT009 | 400 | 이미 사용 중인 슬러그입니다. |
| `PARENT_NOT_FOUND` | CT010 | 404 | 부모 카테고리를 찾을 수 없습니다. |
| `INACTIVE_PARENT` | CT011 | 400 | 비활성화된 카테고리는 부모로 설정할 수 없습니다. |
| `MAX_DEPTH_EXCEEDED` | CT012 | 400 | 카테고리는 최대 2단계까지만 설정 가능합니다. |
| `COMMENT_NOT_FOUND` | CM001 | 404 | 댓글을 찾을 수 없습니다. |
| `PARENT_COMMENT_NOT_FOUND` | CM002 | 404 | 부모 댓글을 찾을 수 없습니다. |
| `REPLY_DEPTH_EXCEEDED` | CM003 | 400 | 대댓글에는 답글을 달 수 없습니다. |
| `PARENT_COMMENT_DELETED` | CM004 | 400 | 삭제된 댓글에는 답글을 달 수 없습니다. |
| `DELETED_COMMENT_STATUS_CHANGE` | CM005 | 400 | 삭제된 댓글은 상태를 변경할 수 없습니다. |
| `COMMENT_UPDATE_FORBIDDEN` | CM006 | 403 | 댓글 수정 권한이 없습니다. |
| `DELETED_COMMENT_UPDATE` | CM007 | 400 | 삭제된 댓글은 수정할 수 없습니다. |
| `COMMENT_DELETE_FORBIDDEN` | CM008 | 403 | 댓글 삭제 권한이 없습니다. |
| `COMMENT_ALREADY_DELETED` | CM009 | 400 | 이미 삭제된 댓글입니다. |
| `NOTIFICATION_NOT_FOUND` | NT001 | 404 | 알림이 존재하지 않습니다. |
| `BALANCE_NOT_FOUND` | PT001 | 404 | 포인트 계좌가 존재하지 않습니다. |
| `BALANCE_NOT_FOUND_PENDING` | PT002 | 404 | 포인트 계좌가 존재하지 않습니다! 승인 대기 중일 수 있습니다. |
| `ITEM_NOT_PURCHASABLE` | PT003 | 400 | 구매할 수 없는 상품입니다. |
| `NOT_SHOP_ITEM` | PT004 | 400 | 마일리지 구매 상품이 아닙니다. |
| `ITEM_PRICE_NOT_SET` | PT005 | 400 | 상품 가격이 설정되지 않았습니다. |
| `ITEM_BADGE_NOT_LINKED` | PT006 | 400 | 상품에 연결된 배지가 없습니다. |
| `BADGE_ALREADY_OWNED` | PT007 | 400 | 이미 보유한 배지입니다. |
| `INVALID_SPEND_AMOUNT` | PT008 | 400 | 차감할 마일리지가 올바르지 않습니다. |
| `INSUFFICIENT_BALANCE` | PT009 | 400 | 보유 마일리지가 부족합니다. |
| `DECORATION_NOT_FOUND` | PD001 | 404 | 존재하지 않는 프로필 장식입니다. |
| `BADGE_NOT_FOUND` | PD002 | 404 | 존재하지 않는 프로필 배지입니다. |
| `DECORATION_NOT_USABLE` | PD003 | 400 | 사용할 수 없는 프로필 장식입니다. |
| `BADGE_NOT_USABLE` | PD004 | 400 | 사용할 수 없는 프로필 배지입니다. |
| `DECORATION_NOT_OWNED` | PD005 | 403 | 보유한 프로필 장식만 사용할 수 있습니다. |
| `BADGE_KEY_REQUIRED` | PD006 | 400 | 배지 키를 입력해주세요. |
| `BADGE_KEY_INVALID` | PD007 | 400 | 배지 키는 영문 소문자, 숫자, 하이픈만 사용할 수 있습니다. |
| `BADGE_LABEL_REQUIRED` | PD008 | 400 | 배지 이름을 입력해주세요. |
| `BADGE_IMAGE_REQUIRED` | PD009 | 400 | 배지 이미지 경로가 필요합니다. |
| `BADGE_KEY_ALREADY_EXIST` | PD010 | 400 | 이미 존재하는 배지 키입니다. |
| `STUDENT_NUM_REQUIRED` | PD011 | 400 | studentNum 값이 필요합니다. |
| `DECORATION_ID_REQUIRED` | PD012 | 400 | decorationId 값이 필요합니다. |
| `STUDENT_NUM_NOT_FOUND` | PD013 | 404 | 존재하지 않는 학번입니다. |
| `IMAGE_SERVICE_UNAVAILABLE` | PD014 | 503 | 배지 이미지 서비스를 사용할 수 없습니다. |
| `TAG_NOT_FOUND` | TG001 | 404 | 태그가 존재하지 않습니다. |
| `TAG_NOT_IN_CATEGORY` | TG002 | 400 | 이 태그는 해당 카테고리에 속하지 않습니다. |
| `ADMIN_USER_NOT_FOUND` | US001 | 404 | User not found |
| `USER_PENDING_FAILED` | US002 | 400 | User pending failed |
| `USER_CONVERT_ROLE_FAILED` | US003 | 400 | User role convert failed |
| `USER_CONVERT_STATUS_FAILED` | US004 | 400 | User status convert failed |
| `USER_ROLE_NOT_FOUND` | US005 | 500 | User role not found |
| `USER_GRANT_ROLE_FAILED` | US006 | 400 | User role grant failed |
| `USER_REVOKE_ROLE_FAILED` | US007 | 400 | User role revoke failed |
| `AUTHORITY_NOT_EXIST` | US008 | 400 | Authority not found |
| `CANNOT_AUTHORIZED` | AE001 | 401 | 인증이 필요합니다. |
| `AUTH_USER_NOT_FOUND` | AE002 | 404 | 해당 유저 정보를 찾을 수 없습니다. |
| `STATUS_PENDING` | AE003 | 403 | 승인 대기 중인 계정입니다. 관리자 승인 후 로그인할 수 있습니다. |
| `STATUS_REJECTED` | AE004 | 403 | 승인이 거부된 계정입니다. |
| `STATUS_NOT_APPROVED` | AE005 | 403 | 승인되지 않은 계정입니다. |
| `INVALID_CREDENTIALS` | AE006 | 401 | Invalid user ID or password |
| `USER_ALREADY_EXIST` | UE001 | 400 | 해당 정보를 가진 유저가 이미 존재합니다. |
| `USER_NOT_FOUND` | UE002 | 404 | 사용자를 찾을 수 없습니다. |
| `NICKNAME_ALREADY_EXIST` | UE003 | 400 | 이미 사용 중인 닉네임입니다. |
| `PROFILE_IMAGE_KEY_REQUIRED` | UE004 | 400 | 프로필 이미지 키가 필요합니다. |
| `RECOVERY_SESSION_EXPIRED` | UR001 | 403 | 인증 세션이 만료되었습니다. |
| `AUTH_CODE_MISMATCH` | UR002 | 401 | 인증에 실패했습니다. |
| `EMAIL_NOT_VERIFIED` | UR003 | 403 | 이메일 인증이 완료되지 않은 요청입니다. |
| `CHALLENGE_NOT_FOUND` | CH001 | 404 | 존재하지 않는 문제입니다. |
| `CHALLENGE_CODE_DUPLICATED` | CH002 | 409 | 이미 존재하는 문제 코드입니다. |
| `INVALID_CHALLENGE_RULE` | CH003 | 400 | 허용되지 않는 문제 유형 조합입니다. |
| `INVALID_POINT_RANGE` | CH004 | 400 | 최소 점수는 최대 점수보다 클 수 없습니다. |
| `STATIC_FLAG_REQUIRED` | CH005 | 400 | STATIC 문제는 플래그가 필요합니다. |
| `DOWNLOAD_NOT_SUPPORTED` | CH006 | 400 | 인스턴스 문제는 첨부파일 다운로드를 지원하지 않습니다. |
| `CHALLENGE_ATTACHMENT_NOT_FOUND` | CH007 | 404 | 첨부파일이 없는 문제입니다. |
| `DOWNLOAD_FORBIDDEN` | CH008 | 403 | 본인이 요청한 다운로드가 아닙니다. |
| `CONTEST_NOT_FOUND` | CN001 | 404 | 존재하지 않는 대회입니다. |
| `UNFINALIZED_CONTEST_EXISTS` | CN002 | 409 | 결과가 확정되지 않은 대회가 있습니다. 종료 후 결과가 확정되면 새 대회를 만들 수 있습니다. |
| `CONTEST_STARTS_BEFORE_PREVIOUS` | CN003 | 400 | 새 대회는 이전 대회보다 늦게 시작해야 합니다. |
| `CONTEST_NOT_ENDED` | CN004 | 409 | 아직 종료되지 않은 대회입니다. |
| `CONTEST_ENDED` | CN005 | 403 | 대회가 종료되었습니다. |
| `INVALID_CONTEST_RANGE` | CN006 | 400 | 종료 시각은 시작 시각보다 늦어야 합니다. |
| `CONTEST_RESULT_NOT_FOUND` | CN007 | 404 | 결과가 확정된 대회가 아닙니다. |
| `GROUP_NOT_FOUND` | GR001 | 404 | 존재하지 않는 그룹입니다. |
| `GROUP_NAME_DUPLICATED` | GR002 | 409 | 이미 사용 중인 그룹 이름입니다. |
| `GROUP_MEMBER_NOT_FOUND` | GR003 | 404 | 그룹 멤버가 아닙니다. |
| `ALREADY_GROUP_MEMBER` | GR004 | 409 | 이미 그룹에 가입되어 있습니다. |
| `GROUP_CAPACITY_EXCEEDED` | GR005 | 400 | 그룹 정원이 초과되었습니다. |
| `GROUP_LEADER_ONLY` | GR006 | 403 | 그룹장만 수행할 수 있습니다. |
| `LEADER_TRANSFER_FORBIDDEN` | GR007 | 403 | 그룹장만 위임할 수 있습니다. |
| `LEADER_TRANSFER_TO_SELF` | GR008 | 400 | 본인에게 위임할 수 없습니다. |
| `LEADER_CANNOT_LEAVE` | GR009 | 400 | 그룹장은 위임 후 탈퇴할 수 있습니다. |
| `CANNOT_KICK_SELF` | GR010 | 400 | 본인을 강퇴할 수 없습니다. |
| `JOIN_REQUEST_NOT_FOUND` | GR011 | 404 | 존재하지 않는 요청입니다. |
| `JOIN_REQUEST_ALREADY_PROCESSED` | GR012 | 400 | 이미 처리된 요청입니다. |
| `JOIN_REQUEST_ALREADY_PENDING` | GR013 | 409 | 이미 진행 중인 요청이 존재합니다. |
| `JOIN_REQUEST_CANCEL_FORBIDDEN` | GR014 | 403 | 본인의 요청만 취소할 수 있습니다. |
| `JOIN_REQUEST_APPLY_LEADER_ONLY` | GR015 | 403 | 그룹장만 신청을 수락/거절할 수 있습니다. |
| `JOIN_REQUEST_INVITEE_ONLY` | GR016 | 403 | 초대받은 본인만 수락/거절할 수 있습니다. |
| `TEAM_NOT_FOUND` | TM001 | 404 | 존재하지 않는 팀입니다. |
| `TEAM_MEMBER_NOT_FOUND` | TM002 | 404 | 해당 유저는 이 팀의 멤버가 아닙니다. |
| `TEAM_USER_NOT_FOUND` | TM003 | 404 | 존재하지 않는 유저입니다. |
| `TEAM_USER_NOT_APPROVED` | TM004 | 400 | 승인되지 않은 유저는 팀에 넣을 수 없습니다. |
| `TEAM_USER_ALREADY_ASSIGNED` | TM005 | 409 | 이미 다른 팀에 속한 유저가 있습니다. |
| `TEAM_NAME_DUPLICATED` | TM006 | 409 | 이미 사용 중인 팀 이름입니다. |
| `TEAM_MEMBERS_OVER_CAPACITY` | TM007 | 400 | 멤버 수가 최대 인원을 넘습니다. |
| `TEAM_CAPACITY_BELOW_MEMBERS` | TM008 | 400 | 현재 멤버 수보다 최대 인원을 작게 할 수 없습니다. |
| `TEAM_FULL` | TM009 | 400 | 팀 정원을 넘습니다. |
| `NO_TEAM` | TM010 | 403 | 팀에 소속되어 있지 않습니다. 운영진이 팀을 배정하면 문제가 공개됩니다. |
| `INSTANCE_NOT_SUPPORTED` | IN001 | 400 | 해당 문제는 인스턴스 기능을 지원하지 않습니다. |
| `CTF_SERVER_UNAVAILABLE` | IN002 | 503 | CTF 서버가 아직 작동중이지 않습니다. |
| `INSTANCE_FORBIDDEN` | IN003 | 403 | 본인 인스턴스가 아닙니다. |
| `TOO_MANY_SUBMISSIONS` | SV001 | 429 | 제출이 너무 많습니다. 잠시 후 다시 시도해 주세요. |
| `SUBMISSION_FORBIDDEN` | SV002 | 403 | 본인의 제출이 아닙니다. |
