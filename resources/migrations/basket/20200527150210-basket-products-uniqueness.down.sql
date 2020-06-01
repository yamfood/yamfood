alter table basket_products
    add constraint basket_products_basket_id_product_id_key unique (basket_id, product_id);