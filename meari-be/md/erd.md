CREATE TABLE "content" (
"content_id"	BIGSERIAL		NOT NULL,
"theme_id"	BIGINT		NOT NULL,
"title"	VARCHAR(100)		NOT NULL,
"video_url"	VARCHAR(2048)		NOT NULL,
"thumbnail_url"	VARCHAR(2048)	DEFAULT NULL	NULL,
"max_people"	INTEGER		NOT NULL,
"total_duration"	DECIMAL(10,3)		NOT NULL
);

CREATE TABLE "kopic_report" (
"kopic_report_id"	BIGSERIAL		NOT NULL,
"member_id"	BIGINT		NOT NULL,
"theme_id"	BIGINT		NOT NULL,
"total_score"	INTEGER		NULL,
"detailed_analysis"	JSONB		NOT NULL,
"status"	VARCHAR(50)		NOT NULL
);

CREATE TABLE "theme" (
"theme_id"	BIGSERIAL		NOT NULL,
"name"	VARCHAR(50)		NOT NULL,
"description"	VARCHAR(500)		NOT NULL,
"theme_url"	VARCHAR(2048)		NOT NULL
);

CREATE TABLE "member_room" (
"member_room_id"	BIGSERIAL		NOT NULL,
"member_id"	BIGINT		NOT NULL,
"room_id"	BIGINT		NOT NULL
);

CREATE TABLE "shadowing_report" (
"shadowing_report_id"	BIGSERIAL		NOT NULL,
"member_id"	BIGINT		NOT NULL,
"room_id"	BIGINT		NOT NULL,
"role_id"	BIGINT		NOT NULL,
"content_id"	BIGINT		NOT NULL,
"audio_url"	VARCHAR(2048)		NOT NULL,
"accuracy"	INTEGER	DEFAULT NULL	NULL,
"intonation"	INTEGER	DEFAULT NULL	NULL,
"detailed_analysis"	JSONB	DEFAULT NULL	NULL,
"status"	VARCHAR(20)		NOT NULL
);

COMMENT ON COLUMN "shadowing_report"."status" IS 'PROCESSING, COMPLETED';

CREATE TABLE "word" (
"word_id"	BIGSERIAL		NOT NULL,
"word_kr"	VARCHAR(100)		NOT NULL,
"definition_kr"	VARCHAR(500)		NOT NULL,
"word_vn"	VARCHAR(100)		NOT NULL,
"definition_vn"	VARCHAR(500)		NOT NULL
);

CREATE TABLE "sentence" (
"sentence_id"	BIGSERIAL		NOT NULL,
"content_id"	BIGINT		NOT NULL,
"role_id"	BIGINT		NOT NULL,
"sequence"	INTEGER		NOT NULL,
"start_time"	DECIMAL(10,3)		NOT NULL,
"end_time"	DECIMAL(10,3)		NOT NULL,
"text_ko"	VARCHAR(1000)		NOT NULL,
"text_vn"	VARCHAR(1000)		NOT NULL
);

CREATE TABLE "room" (
"room_id"	BIGSERIAL		NOT NULL,
"owner_id"	BIGINT		NOT NULL,
"title"	VARCHAR(100)		NOT NULL,
"is_active"	VARCHAR(20)		NOT NULL,
"password"	VARCHAR(20)		NULL
);

COMMENT ON COLUMN "room"."is_active" IS '대기 중, 학습 중, 종료됨';

CREATE TABLE "kopic_sentence" (
"kopic_sentence_id"	BIGSERIAL		NOT NULL,
"theme_id"	BIGINT		NOT NULL,
"text_ko"	VARCHAR(1000)		NOT NULL,
"kopic_sentence_url"	VARCHAR(2048)		NOT NULL
);

CREATE TABLE "member" (
"member_id"	BIGSERIAL		NOT NULL,
"email"	VARCHAR(255)		NOT NULL,
"password"	VARCHAR(255)		NOT NULL,
"nickname"	VARCHAR(100)		NOT NULL,
"profile_url"	VARCHAR(2048)		NULL,
"native_language"	VARCHAR(10)	DEFAULT 'KR'	NOT NULL,
"sex"	VARCHAR(1)		NOT NULL
);

COMMENT ON COLUMN "member"."native_language" IS 'ISO 국가 코드';

CREATE TABLE "sentence_word" (
"sentence_word_id"	BIGSERIAL		NOT NULL,
"word_id"	BIGINT		NOT NULL,
"sentence_id"	BIGINT		NOT NULL,
"order"	INTEGER		NOT NULL
);

CREATE TABLE "role" (
"role_id"	BIGSERIAL		NOT NULL,
"content_id"	BIGINT		NOT NULL,
"name"	VARCHAR(50)		NOT NULL
);

ALTER TABLE "content" ADD CONSTRAINT "PK_CONTENT" PRIMARY KEY (
"content_id"
);

ALTER TABLE "kopic_report" ADD CONSTRAINT "PK_KOPIC_REPORT" PRIMARY KEY (
"kopic_report_id"
);

ALTER TABLE "theme" ADD CONSTRAINT "PK_THEME" PRIMARY KEY (
"theme_id"
);

ALTER TABLE "member_room" ADD CONSTRAINT "PK_MEMBER_ROOM" PRIMARY KEY (
"member_room_id"
);

ALTER TABLE "shadowing_report" ADD CONSTRAINT "PK_SHADOWING_REPORT" PRIMARY KEY (
"shadowing_report_id"
);

ALTER TABLE "word" ADD CONSTRAINT "PK_WORD" PRIMARY KEY (
"word_id"
);

ALTER TABLE "sentence" ADD CONSTRAINT "PK_SENTENCE" PRIMARY KEY (
"sentence_id"
);

ALTER TABLE "room" ADD CONSTRAINT "PK_ROOM" PRIMARY KEY (
"room_id"
);

ALTER TABLE "kopic_sentence" ADD CONSTRAINT "PK_KOPIC_SENTENCE" PRIMARY KEY (
"kopic_sentence_id"
);

ALTER TABLE "member" ADD CONSTRAINT "PK_MEMBER" PRIMARY KEY (
"member_id"
);

ALTER TABLE "sentence_word" ADD CONSTRAINT "PK_SENTENCE_WORD" PRIMARY KEY (
"sentence_word_id"
);

ALTER TABLE "role" ADD CONSTRAINT "PK_ROLE" PRIMARY KEY (
"role_id"
);

ALTER TABLE "content" ADD CONSTRAINT "FK_theme_TO_content_1" FOREIGN KEY (
"theme_id"
)
REFERENCES "theme" (
"theme_id"
);

ALTER TABLE "kopic_report" ADD CONSTRAINT "FK_member_TO_kopic_report_1" FOREIGN KEY (
"member_id"
)
REFERENCES "member" (
"member_id"
);

ALTER TABLE "kopic_report" ADD CONSTRAINT "FK_theme_TO_kopic_report_1" FOREIGN KEY (
"theme_id"
)
REFERENCES "theme" (
"theme_id"
);

ALTER TABLE "member_room" ADD CONSTRAINT "FK_member_TO_member_room_1" FOREIGN KEY (
"member_id"
)
REFERENCES "member" (
"member_id"
);

ALTER TABLE "member_room" ADD CONSTRAINT "FK_room_TO_member_room_1" FOREIGN KEY (
"room_id"
)
REFERENCES "room" (
"room_id"
);

ALTER TABLE "shadowing_report" ADD CONSTRAINT "FK_member_TO_shadowing_report_1" FOREIGN KEY (
"member_id"
)
REFERENCES "member" (
"member_id"
);

ALTER TABLE "shadowing_report" ADD CONSTRAINT "FK_room_TO_shadowing_report_1" FOREIGN KEY (
"room_id"
)
REFERENCES "room" (
"room_id"
);

ALTER TABLE "shadowing_report" ADD CONSTRAINT "FK_role_TO_shadowing_report_1" FOREIGN KEY (
"role_id"
)
REFERENCES "role" (
"role_id"
);

ALTER TABLE "shadowing_report" ADD CONSTRAINT "FK_content_TO_shadowing_report_1" FOREIGN KEY (
"content_id"
)
REFERENCES "content" (
"content_id"
);

ALTER TABLE "sentence" ADD CONSTRAINT "FK_content_TO_sentence_1" FOREIGN KEY (
"content_id"
)
REFERENCES "content" (
"content_id"
);

ALTER TABLE "sentence" ADD CONSTRAINT "FK_role_TO_sentence_1" FOREIGN KEY (
"role_id"
)
REFERENCES "role" (
"role_id"
);

ALTER TABLE "room" ADD CONSTRAINT "FK_member_TO_room_1" FOREIGN KEY (
"owner_id"
)
REFERENCES "member" (
"member_id"
);

ALTER TABLE "kopic_sentence" ADD CONSTRAINT "FK_theme_TO_kopic_sentence_1" FOREIGN KEY (
"theme_id"
)
REFERENCES "theme" (
"theme_id"
);

ALTER TABLE "sentence_word" ADD CONSTRAINT "FK_word_TO_sentence_word_1" FOREIGN KEY (
"word_id"
)
REFERENCES "word" (
"word_id"
);

ALTER TABLE "sentence_word" ADD CONSTRAINT "FK_sentence_TO_sentence_word_1" FOREIGN KEY (
"sentence_id"
)
REFERENCES "sentence" (
"sentence_id"
);

ALTER TABLE "role" ADD CONSTRAINT "FK_content_TO_role_1" FOREIGN KEY (
"content_id"
)
REFERENCES "content" (
"content_id"
);

