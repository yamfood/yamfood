alter table products
    alter column name type varchar(200) using name->'ru';