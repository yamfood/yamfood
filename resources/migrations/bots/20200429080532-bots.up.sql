create table bots
(
    id        serial,
    token     varchar(200)       not null,
    name      varchar(200)       not null,
    payload   jsonb default '{}' not null,
    is_active bool  default true not null,

    primary key (id)
)