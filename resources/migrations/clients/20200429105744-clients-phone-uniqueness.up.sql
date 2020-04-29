alter table clients
    drop constraint clients_phone_key,
    add constraint clients_phone_bot_id_key unique (phone, bot_id);