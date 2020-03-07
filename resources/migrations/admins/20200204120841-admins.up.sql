create table "admins"
(
    id       serial       not null,
    login    varchar(200) not null,
    password varchar(200) not null,
    payload  jsonb        not null default '{}',
    token    varchar(200) unique,

    primary key (id)
)