create table modifiers
(
    id    uuid  default uuid_generate_v4(),
    name  jsonb default '{}',
    price int   default 0,

    primary key (id)
)