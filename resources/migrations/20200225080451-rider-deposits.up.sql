create table "rider_deposits"
(
    id         serial    not null,
    rider_id   int       not null references riders (id),
    admin_id   int       not null references admins (id),
    amount     int       not null,
    created_at timestamp not null default (now() at time zone 'utc'),
    payload    jsonb     not null default '{}',

    primary key (id)
)