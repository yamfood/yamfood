alter table order_products
    add constraint order_products_order_id_product_id_key unique (order_id, product_id);