-- Supprimer les anciennes données de contenu pour éviter les conflits d'ID
DELETE FROM CONTENT_BODIES;
DELETE FROM CONTENT_TITLES;
DELETE FROM CONTENT;

-- Réinitialiser les compteurs d'ID pour la table CONTENT
ALTER TABLE CONTENT ALTER COLUMN ID RESTART WITH 1;


-- Insérer des pages de contenu par défaut
INSERT INTO CONTENT (id, slug) VALUES (1, 'terms-and-conditions');
INSERT INTO CONTENT_TITLES (content_id, language_code, title) VALUES (1, 'en', 'Terms and Conditions');
INSERT INTO CONTENT_TITLES (content_id, language_code, title) VALUES (1, 'fr', 'Conditions Générales de Vente');
INSERT INTO CONTENT_TITLES (content_id, language_code, title) VALUES (1, 'ar', 'الشروط والأحكام');
INSERT INTO CONTENT_BODIES (content_id, language_code, body) VALUES (1, 'en', 'Content for terms and conditions goes here...');
INSERT INTO CONTENT_BODIES (content_id, language_code, body) VALUES (1, 'fr', 'Le contenu des conditions générales de vente va ici...');
INSERT INTO CONTENT_BODIES (content_id, language_code, body) VALUES (1, 'ar', 'محتوى الشروط والأحكام يوضع هنا...');

INSERT INTO CONTENT (id, slug) VALUES (2, 'privacy-policy');
INSERT INTO CONTENT_TITLES (content_id, language_code, title) VALUES (2, 'en', 'Privacy Policy');
INSERT INTO CONTENT_TITLES (content_id, language_code, title) VALUES (2, 'fr', 'Politique de Confidentialité');
INSERT INTO CONTENT_TITLES (content_id, language_code, title) VALUES (2, 'ar', 'سياسة الخصوصية');
INSERT INTO CONTENT_BODIES (content_id, language_code, body) VALUES (2, 'en', 'Content for privacy policy goes here...');
INSERT INTO CONTENT_BODIES (content_id, language_code, body) VALUES (2, 'fr', 'Le contenu de la politique de confidentialité va ici...');
INSERT INTO CONTENT_BODIES (content_id, language_code, body) VALUES (2, 'ar', 'محتوى سياسة الخصوصية يوضع هنا...');
-- ===============================================
-- PARAMÈTRES DU SITE
-- ===============================================
DELETE FROM SETTINGS;
INSERT INTO SETTINGS (setting_key, setting_value) VALUES
                                                      ('site_logo_url', '/logo.png'), -- << CORRECTION ICI
                                                      ('site_color_primary', '#a855f7'), -- J'en profite pour mettre la couleur violette de votre logo
                                                      ('social_facebook_url', 'https://facebook.com'),
                                                      ('social_twitter_url', 'https://twitter.com'),
                                                      ('social_instagram_url', 'https://instagram.com');

