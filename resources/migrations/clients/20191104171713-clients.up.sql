create table "clients"
(
    id         serial,
    tid        bigint unique              not null,
    name       varchar(200) default ''    not null,
    phone      bigint unique,
    payload    jsonb        default '{}'  not null,
    is_blocked bool         default false not null,

    primary key (id)
)