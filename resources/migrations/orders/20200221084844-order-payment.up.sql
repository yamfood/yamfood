alter table orders
    add payment varchar(100) default 'cash' not null;