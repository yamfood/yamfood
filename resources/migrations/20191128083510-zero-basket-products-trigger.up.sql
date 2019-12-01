CREATE TRIGGER zero_basket_products_trigger
  AFTER UPDATE
  ON "basket_products"
  FOR EACH ROW
  WHEN (NEW.count = 0)
EXECUTE PROCEDURE delete_basket_product();