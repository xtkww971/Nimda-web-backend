-- Local development schema, generated from all 22 current JPA entities.
-- MySQL 8.0 / Hibernate 6.3.1. Use on an EMPTY local database only.
-- Docker executes this file once when the nimda-local MySQL volume is created.
-- Historical V2..V33 migrations are NOT replayed; the local profile disables Flyway.
-- Includes point_history because its entity still exists, even though V10 dropped it.


    create table attachments (
        board_id bigint,
        category_id bigint,
        created_at datetime(6) not null,
        file_size bigint,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        user_id bigint,
        extension varchar(255),
        filepath varchar(255) not null,
        origin_filename varchar(255) not null,
        stored_filename varchar(255) not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table attendance (
        consecutive_count integer,
        last_date date,
        total_count integer,
        user_id bigint not null,
        primary key (user_id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table attendance_log (
        attendance_date datetime(6),
        id bigint not null auto_increment,
        user_id bigint,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table authority (
        id bigint not null auto_increment,
        authority_name varchar(20) not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table board (
        pinned bit not null,
        post_view integer not null,
        category_id bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        item_price bigint not null,
        profile_decoration_id bigint,
        tag_id bigint,
        thumbnail_attachment_id bigint,
        updated_at datetime(6) not null,
        user_id bigint not null,
        title varchar(200) not null,
        filepath varchar(500),
        content MEDIUMTEXT not null,
        filename varchar(255),
        item_type enum ('GENERAL','BADGE') not null,
        status enum ('ACTIVE','HIDDEN','DELETED') not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table board_like (
        author_id bigint not null,
        board_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        liker_id bigint,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table category (
        is_active bit not null,
        post_count integer not null,
        shop_enabled bit not null,
        sort_order integer not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        parent_id bigint,
        updated_at datetime(6) not null,
        name varchar(50) not null,
        slug varchar(50) not null,
        redirect_url varchar(500),
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table challenges (
        is_public bit not null,
        points integer not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        updated_at datetime(6),
        code varchar(50) not null,
        flag_hash varchar(64),
        title varchar(100) not null,
        attachment_key varchar(255),
        description TEXT not null,
        category enum ('REVERSING','FORENSICS','CRYPTO','WEB','PWN','MISC') not null,
        flag_type enum ('STATIC','DYNAMIC') not null,
        isolation_type enum ('NONE','SHARED','PER_USER') not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table comment_like (
        comment_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table comments (
        board_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        parent_comment_id bigint,
        updated_at datetime(6),
        user_id bigint not null,
        context TEXT not null,
        status enum ('PUBLIC','HIDDEN','DELETED'),
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table group_join_requests (
        created_at datetime(6) not null,
        group_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        user_id bigint not null,
        join_type enum ('APPLY','INVITE') not null,
        status enum ('PENDING','ACCEPTED','REJECTED','CANCELED') not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table group_members (
        created_at datetime(6),
        group_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        user_id bigint not null,
        role enum ('LEADER','MEMBER') not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table `groups` (
        capacity integer not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        name varchar(50) not null,
        description varchar(200),
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table notification (
        is_read bit not null,
        created_at datetime(6) not null,
        expired_at datetime(6),
        id bigint not null auto_increment,
        recipient_id bigint not null,
        related_entity_id bigint,
        sender_id bigint,
        updated_at datetime(6) not null,
        related_url varchar(500),
        message varchar(255) not null,
        notification_type VARCHAR(50) not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table point_details (
        amount bigint not null,
        created_at datetime(6),
        expired_at datetime(6) not null,
        id bigint not null auto_increment,
        remaining_amount bigint not null,
        user_balance_id bigint not null,
        description varchar(255) not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table point_history (
        id bigint not null auto_increment,
        point_detail_id bigint,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table profile_decorations (
        active bit not null,
        purchase_required bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        required_role varchar(50),
        decoration_key varchar(100) not null,
        label varchar(100) not null,
        file_path varchar(500) not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table solves (
        awarded_points integer not null,
        challenge_id bigint not null,
        id bigint not null auto_increment,
        solved_at datetime(6),
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table tags (
        sort_value integer,
        category_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        tag_name varchar(255),
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table user_authority (
        authority_id bigint not null,
        user_id bigint not null,
        primary key (authority_id, user_id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table user_balance (
        total_amount bigint not null,
        updated_at datetime(6) not null,
        user_id bigint not null,
        primary key (user_id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table user_profile_decorations (
        acquired_at datetime(6),
        id bigint not null auto_increment,
        profile_decoration_id bigint not null,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    create table users (
        email_hide bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6) not null,
        student_num varchar(9) not null,
        name varchar(10) not null,
        birth varchar(20),
        major varchar(20) not null,
        nickname varchar(20) not null,
        user_id varchar(20) not null,
        email varchar(40) not null,
        boj_id varchar(50),
        profile_decoration varchar(100),
        password varchar(255) not null,
        profile_image varchar(255),
        status enum ('PENDING','APPROVED','REJECTED') not null,
        primary key (id)
    ) engine=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    alter table authority 
       add constraint UK_6ct98mcqw43jw46da6tbapvie unique (authority_name);

    alter table board_like 
       add constraint UK19t9rs8pr99ol17bl9q42kmnx unique (liker_id, board_id);

    alter table category 
       add constraint UK_hqknmjh5423vchi4xkyhxlhg2 unique (slug);

    alter table challenges 
       add constraint UK_2ooow5gju78uo6t0op939vwn3 unique (code);

    alter table comment_like 
       add constraint UKo8urnlomri10dub10y8ket23c unique (user_id, comment_id);

    alter table group_join_requests 
       add constraint uk_group_join_request unique (group_id, user_id);

    alter table group_members 
       add constraint uk_group_member unique (group_id, user_id);

    alter table profile_decorations 
       add constraint UK_1n1o0khm65wx4jh40oqedkvtc unique (decoration_key);

    alter table solves 
       add constraint uk_solves_challenge_user unique (challenge_id, user_id);

    alter table user_profile_decorations 
       add constraint UKim5kifvr5qopcgrrqw3vyd3qq unique (user_id, profile_decoration_id);

    alter table users 
       add constraint UK_2ty1xmrrgtn89xt7kyxx6ta7h unique (nickname);

    alter table users 
       add constraint UK_6efs5vmce86ymf5q7lmvn2uuf unique (user_id);

    alter table users 
       add constraint UK_6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table attendance 
       add constraint FKjcaqd29v2qy723owsdah2t8vx 
       foreign key (user_id) 
       references users (id);

    alter table attendance_log 
       add constraint FKfm0a1i3jrk15odgowo4loaw26 
       foreign key (user_id) 
       references users (id);

    alter table board 
       add constraint FK5vlh90qyii65ixwsbnafd55ud 
       foreign key (user_id) 
       references users (id);

    alter table board 
       add constraint FK60jn05fpinj6cjhqcmtkacu2g 
       foreign key (category_id) 
       references category (id);

    alter table board 
       add constraint FKf7yso1uexiq7lggs9fkr3evba 
       foreign key (profile_decoration_id) 
       references profile_decorations (id);

    alter table board 
       add constraint FKnthewaap66yq6gp1svxwyxn1s 
       foreign key (tag_id) 
       references tags (id);

    alter table board_like 
       add constraint FKjxh1p5to2bkik97gqdqu70iy7 
       foreign key (author_id) 
       references users (id);

    alter table board_like 
       add constraint FKk7rxm8vl1ptqqhwdj2sjmlpvq 
       foreign key (board_id) 
       references board (id);

    alter table board_like 
       add constraint FK7ub67la3m4l4bg6pgu30qx0n5 
       foreign key (liker_id) 
       references users (id);

    alter table comment_like 
       add constraint FKl1mu2ryf4k4ps93b5b52q2m8i 
       foreign key (comment_id) 
       references comments (id);

    alter table comment_like 
       add constraint FKl5wrmp8eoy5uegdo3473jqqi 
       foreign key (user_id) 
       references users (id);

    alter table comments 
       add constraint FK8omq0tc18jd43bu5tjh6jvraq 
       foreign key (user_id) 
       references users (id);

    alter table comments 
       add constraint FK2sbm05xp09r2igj2t4j2so05l 
       foreign key (board_id) 
       references board (id);

    alter table comments 
       add constraint FK7h839m3lkvhbyv3bcdv7sm4fj 
       foreign key (parent_comment_id) 
       references comments (id);

    alter table group_join_requests 
       add constraint FKlvbk73i4g0va06atf9xylhuct 
       foreign key (group_id) 
       references `groups` (id);

    alter table group_join_requests 
       add constraint FK64quii1nqqd8afxdy6aysqprr 
       foreign key (user_id) 
       references users (id);

    alter table group_members 
       add constraint FKrpgq4bl4kui39wk9mlkl26ib 
       foreign key (group_id) 
       references `groups` (id);

    alter table group_members 
       add constraint FKnr9qg33qt2ovmv29g4vc3gtdx 
       foreign key (user_id) 
       references users (id);

    alter table notification 
       add constraint FKfcyn9rsga73dqnorl7owfyl4a 
       foreign key (recipient_id) 
       references users (id);

    alter table notification 
       add constraint FKrg0atx075rr68et2rqrh34qwj 
       foreign key (sender_id) 
       references users (id);

    alter table point_details 
       add constraint FKl55g3lsyt0pv4rq87bbxnrtfo 
       foreign key (user_balance_id) 
       references user_balance (user_id);

    alter table point_history 
       add constraint FK749nie4rh0qrufpf1c3g88ux8 
       foreign key (point_detail_id) 
       references point_details (id);

    alter table tags 
       add constraint FK9lv5crmxwwvfyw4eq2x8mkwe9 
       foreign key (category_id) 
       references category (id);

    alter table user_authority 
       add constraint FKgvxjs381k6f48d5d2yi11uh89 
       foreign key (authority_id) 
       references authority (id);

    alter table user_authority 
       add constraint FKhi46vu7680y1hwvmnnuh4cybx 
       foreign key (user_id) 
       references users (id);

    alter table user_balance 
       add constraint FKdc2arkxowqwkfyhymlokgj7k0 
       foreign key (user_id) 
       references users (id);

    alter table user_profile_decorations 
       add constraint FK3sj7p2aevoeyq0au9i5f7q2fd 
       foreign key (profile_decoration_id) 
       references profile_decorations (id);

    alter table user_profile_decorations 
       add constraint FKm4y2a1i7hvpmdik14u2dhjnyh 
       foreign key (user_id) 
       references users (id);

-- Supplemental indexes and category FK from the existing migration definitions.
ALTER TABLE category ADD CONSTRAINT fk_local_category_parent
    FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL;
CREATE INDEX idx_challenges_category ON challenges(category);
CREATE INDEX idx_challenges_is_public ON challenges(is_public);
CREATE INDEX idx_solves_user_id ON solves(user_id);
CREATE INDEX idx_solves_challenge_id ON solves(challenge_id);
CREATE INDEX idx_group_members_group_id ON group_members(group_id);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);
CREATE INDEX idx_group_join_requests_group_id ON group_join_requests(group_id);
CREATE INDEX idx_group_join_requests_user_id ON group_join_requests(user_id);
CREATE INDEX idx_attachments_board_id ON attachments(board_id);
CREATE INDEX idx_attachments_user_id ON attachments(user_id);
