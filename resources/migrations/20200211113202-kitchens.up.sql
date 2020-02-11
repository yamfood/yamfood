create table "kitchens"
(
    id       serial       not null,
    name     varchar(200) not null,
    location Point        not null,
    payload  jsonb        not null default '{}',

    primary key (id)
)