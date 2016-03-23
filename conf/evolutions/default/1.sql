# --- !Ups

create table "scheme" (
    "empref" VARCHAR NOT NULL PRIMARY KEY,
    "termination_date" DATE NULL
);

INSERT INTO "scheme" ("empref") VALUES ('123/AB12345');
INSERT INTO "scheme" ("empref") VALUES ('123/BC12345');
INSERT INTO "scheme" ("empref") VALUES ('321/ZX54321');
INSERT INTO "scheme" ("empref") VALUES ('222/MM22222');

CREATE TABLE "gateway_user" (
    "id"       INTEGER NOT NULL PRIMARY KEY,
    "username" VARCHAR NOT NULL,
    "password" VARCHAR NOT NULL
);

CREATE TABLE "gateway_enrolment" (
    "gateway_user_id" INTEGER NOT NULL REFERENCES "gateway_user",
    "empref"          VARCHAR NOT NULL REFERENCES "scheme"
);

INSERT INTO "gateway_user" ("id", "username", "password") VALUES (1, 'gateway1', 'password');
INSERT INTO "gateway_user" ("id", "username", "password") VALUES (2, 'gateway2', 'password');

INSERT INTO "gateway_enrolment" ("gateway_user_id", "empref") VALUES (1, '123/AB12345');
INSERT INTO "gateway_enrolment" ("gateway_user_id", "empref") VALUES (1, '123/BC12345');
INSERT INTO "gateway_enrolment" ("gateway_user_id", "empref") VALUES (1, '321/ZX54321');
INSERT INTO "gateway_enrolment" ("gateway_user_id", "empref") VALUES (2, '222/MM22222');


create table "client" (
    "id" VARCHAR NOT NULL PRIMARY KEY,
    "secret" VARCHAR NULL,
    "redirect_uri" VARCHAR NULL,
    "scope" VARCHAR NULL,
    "grant_type" VARCHAR NOT NULL
);

INSERT INTO "client" ("id", "secret", "redirect_uri", "scope", "grant_type") VALUES ('client1', 'secret1', '', '', 'authorization_code');

CREATE TABLE "auth_codes" (
    "authorization_code" VARCHAR NOT NULL PRIMARY KEY,
    "gateway_user_id" INTEGER NOT NULL REFERENCES "gateway_user",
    "redirect_uri" VARCHAR NULL,
    "created_at" DATE NOT NULL,
    "scope" VARCHAR NULL,
    "client_id" VARCHAR NULL,
    "expires_in" INTEGER NOT NULL
);

INSERT INTO "auth_codes" ("authorization_code", "gateway_user_id", "redirect_uri", "created_at", "scope", "client_id", "expires_in")
    VALUES ('doug_code', 1, 'http://localhost:9000/', now(), '', 'client1', 100000);

CREATE TABLE "access_token" (
    "access_token" VARCHAR NOT NULL PRIMARY KEY,
    "refresh_token" VARCHAR NULL,
    "gateway_user_id" INTEGER NOT NULL REFERENCES "gateway_user",
    "scope" VARCHAR NULL,
    "expires_in" INTEGER NOT NULL,
    "created_at" DATE NOT NULL,
    "client_id" VARCHAR NULL
);

# --- !Downs


DROP TABLE "access_token";
DROP TABLE "auth_codes";
DROP TABLE "client";

DROP TABLE "gateway_enrolment";
DROP TABLE "gateway_user";
DROP TABLE "scheme";
