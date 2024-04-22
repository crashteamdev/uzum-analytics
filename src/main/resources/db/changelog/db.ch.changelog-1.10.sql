--liquibase formatted sql
--changeset vitaxa:add-uzum-product-daily-seller-link-projection
ALTER TABLE uzum.uzum_product_daily_sales ADD PROJECTION IF NOT EXISTS product_daily_sales_seller_link_projection (
SELECT
    *
ORDER BY seller_link, date
    );
