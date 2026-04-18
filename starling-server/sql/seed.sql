-- Default user (password: admin)
INSERT INTO users (username, password, figure, sex, motto, credits, sso_ticket)
VALUES ('admin', 'admin', 'hd-180-1.ch-210-66.lg-270-82.sh-290-91.hr-828-61', 'M', 'Hello Habbo!', 10000, 'starling-sso-ticket');

-- Navigator categories
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (1, 'Public Rooms', 0, 0, 300);
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (2, 'Guest Rooms', 0, 0, 600);
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (3, 'Lobbies', 1, 0, 100);
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (4, 'Pools', 1, 0, 100);
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (5, 'Category 1', 2, 0, 100);
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (6, 'Category 2', 2, 0, 100);
INSERT INTO navigator_categories (id, name, parent_id, node_type, max_users) VALUES (7, 'Category 3', 2, 0, 100);
