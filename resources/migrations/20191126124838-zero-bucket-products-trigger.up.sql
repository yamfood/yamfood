CREATE TRIGGER zero_bucket_products_trigger
  AFTER UPDATE ON "bucket_products"
  FOR EACH ROW
  WHEN (NEW.count = 0)
EXECUTE PROCEDURE delete_bucket_product();