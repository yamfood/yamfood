alter table clients
    add bot_id int references bots (id);
