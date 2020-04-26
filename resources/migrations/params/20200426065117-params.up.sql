create table params
(
    id    serial,
    name  varchar(200) not null,
    key   varchar(200) not null unique,
    docs  text         not null default '',
    value varchar      not null,

    primary key (id)
);