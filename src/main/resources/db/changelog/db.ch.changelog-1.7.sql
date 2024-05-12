--liquibase formatted sql
--changeset vitaxa:delete-category-stats-table
DROP TABLE IF EXISTS uzum.category_daily_stats;
DROP TABLE IF EXISTS uzum.category_daily_stats_mv;

--changeset vitaxa:add-uzum-product-daily-category-projection
ALTER TABLE uzum.uzum_product_daily_sales ADD PROJECTION IF NOT EXISTS product_daily_sales_category_id_projection (
SELECT
    *
ORDER BY category_id, date
    );
