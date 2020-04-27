create table sms
(
    id         serial,
    phone      bigint                   not null,
    text       varchar(200)             not null,
    created_at timestamp with time zone not null default current_timestamp,
    is_sent    bool                     not null default false,
    error      varchar(200)             not null default '',

    primary key (id)
)