ALTER TABLE assets
    ADD COLUMN source VARCHAR(20) NULL AFTER type;

-- 현재 서비스에서 수동 입력으로 생성되는 자산 타입은 두 개뿐이다.
UPDATE assets
SET source = 'MANUAL'
WHERE type IN ('REAL_ESTATE', 'AUTOMOBILE');

UPDATE assets
SET source = 'MYDATA'
WHERE source IS NULL;

ALTER TABLE assets
    MODIFY COLUMN source VARCHAR(20) NOT NULL;

CREATE INDEX idx_assets_user_source
    ON assets (user_id, source);
